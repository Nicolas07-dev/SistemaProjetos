package client;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class CategoriaApiClient {
    private static final String URL = "http://localhost:7070/api/categorias";
    private final HttpClient client = HttpClient.newHttpClient();
    private static String esc(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\""); }
    private static String corpo(String nome) { return "{\"nome\":\"" + esc(nome) + "\"}"; }
    private static String uri(String nome) { return URL + "/" + URLEncoder.encode(nome, StandardCharsets.UTF_8).replace("+", "%20"); }
    private String requisitar(String url, String metodo, String json) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url)).header("Content-Type", "application/json; charset=UTF-8");
        if ("GET".equals(metodo)) b.GET();
        else if ("DELETE".equals(metodo)) b.DELETE();
        else b.method(metodo, HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
        HttpResponse<String> resposta = client.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resposta.statusCode() < 200 || resposta.statusCode() >= 300)
            throw new Exception("HTTP " + resposta.statusCode() + ": " + resposta.body());
        return resposta.body();
    }
    public List<String> listar() throws Exception {
        String json = requisitar(URL, "GET", null).trim();
        List<String> lista = new ArrayList<>();
        if (!json.startsWith("[") || !json.endsWith("]")) throw new Exception("Resposta de categorias inválida");
        int i = 1;
        while (i < json.length() - 1) {
            char c = json.charAt(i);
            if (c == ',' || Character.isWhitespace(c)) { i++; continue; }
            if (c != '"') throw new Exception("JSON de categorias inválido");
            StringBuilder nome = new StringBuilder(); i++;
            boolean fechado = false;
            while (i < json.length()) {
                c = json.charAt(i++);
                if (c == '"') { fechado = true; break; }
                if (c == '\\') {
                    if (i >= json.length()) throw new Exception("Escape JSON inválido");
                    c = json.charAt(i++);
                    if (c == 'n') c = '\n'; else if (c == 'r') c = '\r'; else if (c == 't') c = '\t';
                    else if (c == 'u') { if (i + 4 > json.length()) throw new Exception("Unicode JSON inválido"); c = (char) Integer.parseInt(json.substring(i, i + 4), 16); i += 4; }
                }
                nome.append(c);
            }
            if (!fechado) throw new Exception("String JSON inválida");
            lista.add(nome.toString());
        }
        return lista;
    }
    public void cadastrar(String nome) throws Exception { requisitar(URL, "POST", corpo(nome)); }
    public void renomear(String anterior, String novo) throws Exception { requisitar(uri(anterior), "PUT", corpo(novo)); }
    public void excluir(String nome) throws Exception { requisitar(uri(nome), "DELETE", null); }
}
