package client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import model.Projeto;
import util.JsonUtil;

public class ProjetoApiClient {

    private static final String BASE_URL = "http://localhost:7070/api/projetos";

    private final HttpClient client;

    public ProjetoApiClient() {
        client = HttpClient.newHttpClient();
    }

    // =========================
    // GET - Listar projetos
    // =========================

    public List<Projeto> listar() throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .GET()
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new Exception(
                    "Erro ao listar projetos. Código: "
                    + response.statusCode()
            );
        }

        return jsonParaLista(response.body());
    }

    // =========================
    // GET - Buscar por ID
    // =========================

    public Projeto buscarPorId(int id) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/" + id))
                .GET()
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() == 404) {
            return null;
        }

        if (response.statusCode() != 200) {
            throw new Exception(
                    "Erro ao buscar projeto. Código: "
                    + response.statusCode()
            );
        }

        return JsonUtil.jsonParaProjeto(response.body());
    }

    // =========================
    // POST - Cadastrar projeto
    // =========================

    public Projeto cadastrar(Projeto projeto) throws Exception {

        String json = JsonUtil.paraJson(projeto);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 201) {
            throw new Exception(
                    "Erro ao cadastrar projeto. Código: "
                    + response.statusCode()
                    + "\n"
                    + response.body()
            );
        }

        return JsonUtil.jsonParaProjeto(response.body());
    }

    // =========================
    // PUT - Alterar projeto
    // =========================

    public Projeto alterar(Projeto projeto) throws Exception {

        String json = JsonUtil.paraJson(projeto);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/" + projeto.getId()))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() == 404) {
            return null;
        }

        if (response.statusCode() != 200) {
            throw new Exception(
                    "Erro ao alterar projeto. Código: "
                    + response.statusCode()
                    + "\n"
                    + response.body()
            );
        }

        return JsonUtil.jsonParaProjeto(response.body());
    }

    // =========================
    // DELETE - Excluir projeto
    // =========================

    public boolean excluir(int id) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/" + id))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() == 404) {
            return false;
        }

        /*
        * 204 = No Content
        * É uma resposta de sucesso para o DELETE.
        */
        if (response.statusCode() != 204) {
            throw new Exception(
                    "Erro ao excluir projeto. Código: "
                    + response.statusCode()
            );
        }

        return true;
    }

    // =========================
    // JSON → Lista de Projetos
    // =========================

    private List<Projeto> jsonParaLista(String json) {

        List<Projeto> projetos = new ArrayList<>();

        json = json.trim();

        if (json.equals("[]")) {
            return projetos;
        }

        if (!json.startsWith("[") || !json.endsWith("]")) {
            throw new IllegalArgumentException(
                    "JSON de lista inválido"
            );
        }

        String conteudo = json.substring(1, json.length() - 1);

        List<String> objetos = separarObjetos(conteudo);

        for (String objeto : objetos) {
            projetos.add(JsonUtil.jsonParaProjeto(objeto));
        }

        return projetos;
    }

    // =========================
    // Separa os objetos JSON
    // =========================

    private List<String> separarObjetos(String json) {

        List<String> objetos = new ArrayList<>();

        boolean dentroString = false;
        boolean escape = false;
        int nivel = 0;
        int inicio = -1;

        for (int i = 0; i < json.length(); i++) {

            char c = json.charAt(i);

            if (escape) {
                escape = false;
                continue;
            }

            if (c == '\\') {
                escape = true;
                continue;
            }

            if (c == '"') {
                dentroString = !dentroString;
                continue;
            }

            if (dentroString) {
                continue;
            }

            if (c == '{') {

                if (nivel == 0) {
                    inicio = i;
                }

                nivel++;

            } else if (c == '}') {

                nivel--;

                if (nivel == 0 && inicio >= 0) {
                    objetos.add(
                            json.substring(inicio, i + 1)
                    );

                    inicio = -1;
                }
            }
        }

        return objetos;
    }
}