package api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import model.Projeto;
import service.ProjetoService;
import util.JsonUtil;

public class Api {

    private static ProjetoService service;

    public static void main(String[] args) throws Exception {

        service = new ProjetoService();
        service.carregar();

        HttpServer server = HttpServer.create(
                new InetSocketAddress(7070),
                0);

        // GET /
        server.createContext("/", exchange -> {

            try {

                configurarCors(exchange);

                if (responderOptions(exchange)) {
                    return;
                }

                if (!exchange.getRequestURI().getPath().equals("/")) {

                    enviarJson(
                            exchange,
                            404,
                            JsonUtil.erro("Rota não encontrada"));

                    return;
                }

                enviar(
                        exchange,
                        200,
                        "text/plain",
                        "API Sistema de Projetos");

            } finally {

                exchange.close();
            }
        });

        // CRUD /api/projetos
        server.createContext("/api/projetos", exchange -> {

            try {

                configurarCors(exchange);

                if (responderOptions(exchange)) {
                    return;
                }

                tratarProjetos(exchange);

            } catch (IllegalArgumentException e) {

                enviarJson(
                        exchange,
                        400,
                        JsonUtil.erro(e.getMessage()));

            } catch (Exception e) {

                e.printStackTrace();

                enviarJson(
                        exchange,
                        500,
                        JsonUtil.erro("Erro interno do servidor"));

            } finally {

                exchange.close();
            }
        });

        server.start();

        System.out.println(
                "Servidor iniciado em http://localhost:7070");
    }

    private static void tratarProjetos(HttpExchange exchange)
            throws Exception {

        String metodo = exchange.getRequestMethod();

        String caminho = exchange.getRequestURI().getPath();

        if (!caminho.equals("/api/projetos")
                && !caminho.startsWith("/api/projetos/")) {

            enviarJson(
                    exchange,
                    404,
                    JsonUtil.erro("Rota não encontrada"));

            return;
        }

        String[] partes = caminho.split("/");

        if (partes.length > 4) {

            enviarJson(
                    exchange,
                    404,
                    JsonUtil.erro("Rota não encontrada"));

            return;
        }

        boolean temId = partes.length == 4;
        int id = 0;

        if (temId) {

            try {

                id = Integer.parseInt(partes[3]);

            } catch (NumberFormatException e) {

                enviarJson(
                        exchange,
                        400,
                        JsonUtil.erro("ID inválido"));

                return;
            }
        }

        switch (metodo) {

            case "GET" ->
                get(exchange, temId, id);

            case "POST" ->
                post(exchange, temId);

            case "PUT" ->
                put(exchange, temId, id);

            case "DELETE" ->
                delete(exchange, temId, id);

            default ->
                exchange.sendResponseHeaders(405, -1);
        }
    }

    // GET /api/projetos
    // GET /api/projetos/{id}
    private static void get(
            HttpExchange exchange,
            boolean temId,
            int id) throws IOException {

        if (!temId) {

            enviarJson(
                    exchange,
                    200,
                    JsonUtil.listaParaJson(service.listar()));

            return;
        }

        Projeto projeto = service.buscarPorId(id);

        if (projeto == null) {

            enviarJson(
                    exchange,
                    404,
                    JsonUtil.erro("Projeto não encontrado"));

            return;
        }

        enviarJson(
                exchange,
                200,
                JsonUtil.paraJson(projeto));
    }

    // POST /api/projetos
    private static void post(
            HttpExchange exchange,
            boolean temId) throws Exception {

        if (temId) {

            exchange.sendResponseHeaders(405, -1);
            return;
        }

        Projeto projeto = JsonUtil.jsonParaProjeto(
                lerCorpo(exchange));

        if (projeto.getNome() == null
                || projeto.getNome().isBlank()) {

            enviarJson(
                    exchange,
                    400,
                    JsonUtil.erro("Nome obrigatório"));

            return;
        }

        if (projeto.getId() <= 0) {
            projeto.setId(proximoId());
        }

        if (!service.adicionar(projeto)) {

            enviarJson(
                    exchange,
                    409,
                    JsonUtil.erro("Já existe um projeto com esse ID"));

            return;
        }

        service.salvar();

        enviarJson(
                exchange,
                201,
                JsonUtil.paraJson(projeto));
    }

    // PUT /api/projetos/{id}
    private static void put(
            HttpExchange exchange,
            boolean temId,
            int id) throws Exception {

        if (!temId) {

            enviarJson(
                    exchange,
                    400,
                    JsonUtil.erro("Informe o ID na URL"));

            return;
        }

        Projeto projeto = JsonUtil.jsonParaProjeto(
                lerCorpo(exchange));

        // O ID utilizado é o da URL.
        projeto.setId(id);

        if (projeto.getNome() == null
                || projeto.getNome().isBlank()) {

            enviarJson(
                    exchange,
                    400,
                    JsonUtil.erro("Nome obrigatório"));

            return;
        }

        if (!service.alterar(projeto)) {

            enviarJson(
                    exchange,
                    404,
                    JsonUtil.erro("Projeto não encontrado"));

            return;
        }

        service.salvar();

        enviarJson(
                exchange,
                200,
                JsonUtil.paraJson(service.buscarPorId(id)));
    }

    // DELETE /api/projetos/{id}
    private static void delete(
            HttpExchange exchange,
            boolean temId,
            int id) throws Exception {

        if (!temId) {

            enviarJson(
                    exchange,
                    400,
                    JsonUtil.erro("Informe o ID na URL"));

            return;
        }

        if (!service.removerPorId(id)) {

            enviarJson(
                    exchange,
                    404,
                    JsonUtil.erro("Projeto não encontrado"));

            return;
        }

        service.salvar();

        exchange.sendResponseHeaders(204, -1);
    }

    // ---------- CORS e OPTIONS ----------

    private static void configurarCors(HttpExchange exchange) {

        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Origin",
                "*");

        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Methods",
                "GET, POST, PUT, DELETE, OPTIONS");

        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Headers",
                "Content-Type");
    }

    private static boolean responderOptions(HttpExchange exchange)
            throws IOException {

        if (exchange.getRequestMethod().equals("OPTIONS")) {

            exchange.sendResponseHeaders(204, -1);

            return true;
        }

        return false;
    }

    // ---------- Auxiliares ----------

    private static int proximoId() {

        int proximo = 1;

        for (Projeto projeto : service.listar()) {

            if (projeto.getId() >= proximo) {
                proximo = projeto.getId() + 1;
            }
        }

        return proximo;
    }

    private static String lerCorpo(HttpExchange exchange)
            throws IOException {

        return new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8);
    }

    private static void enviarJson(
            HttpExchange exchange,
            int status,
            String json) throws IOException {

        enviar(
                exchange,
                status,
                "application/json",
                json);
    }

    private static void enviar(
            HttpExchange exchange,
            int status,
            String tipo,
            String corpo) throws IOException {

        byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type",
                tipo + "; charset=UTF-8");

        exchange.sendResponseHeaders(status, bytes.length);

        exchange.getResponseBody().write(bytes);
    }
}