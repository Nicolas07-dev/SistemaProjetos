
package client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class AutenticacaoApiClient {

    private static final String URL = "http://localhost:7070/api/auth/login";

    private final HttpClient client = HttpClient.newHttpClient();

    public void login(String usuario, char[] senha) throws Exception {

        String senhaTexto = new String(senha);

        String json = "{\"usuario\":\"" + escapar(usuario)
                + "\",\"senha\":\"" + escapar(senhaTexto) + "\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(
                        json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() != 200) {
            throw new Exception(
                    "Falha na autenticação da API. HTTP "
                            + response.statusCode() + ": "
                            + response.body());
        }

        String token = extrairCampo(response.body(), "token");

        if (token == null || token.isBlank()) {
            throw new Exception("A API não retornou um token válido.");
        }

        SessaoApiClient.definirToken(token);
    }

    public void logout() throws Exception {

        String token = SessaoApiClient.obterToken();

        if (token == null || token.isBlank()) {
            SessaoApiClient.encerrar();
            return;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:7070/api/auth/logout"))
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response;

        try {
            response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(
                            StandardCharsets.UTF_8));
        } finally {
            // Encerra a sessão local mesmo se a API estiver fora do ar.
            SessaoApiClient.encerrar();
        }

        if (response.statusCode() != 200) {
            throw new Exception(
                    "Erro ao encerrar sessão. HTTP "
                            + response.statusCode());
        }
    }

    private String escapar(String texto) {
        return texto.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String extrairCampo(String json, String campo) {
        String chave = "\"" + campo + "\"";
        int inicio = json.indexOf(chave);

        if (inicio < 0) {
            return null;
        }

        inicio = json.indexOf(':', inicio + chave.length());

        if (inicio < 0) {
            return null;
        }

        inicio++;

        while (inicio < json.length()
                && Character.isWhitespace(json.charAt(inicio))) {
            inicio++;
        }

        if (inicio >= json.length() || json.charAt(inicio) != '"') {
            return null;
        }

        int fim = json.indexOf('"', inicio + 1);

        if (fim < 0) {
            return null;
        }

        return json.substring(inicio + 1, fim);
    }
}
