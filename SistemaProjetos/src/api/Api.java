package api;

import com.sun.net.httpserver.HttpExchange;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import security.AutenticacaoAdmin;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Arrays;

import java.net.InetSocketAddress;

import java.nio.charset.StandardCharsets;

import model.Projeto;

import service.ProjetoService;
import service.CategoriaService;
import java.net.URLDecoder;
import java.util.NoSuchElementException;

import util.JsonUtil;

public class Api {

    private static final Map<String, Long> SESSOES_WEB = new ConcurrentHashMap<>();
    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final long VALIDADE = 8L * 60 * 60 * 1000;
    private static ProjetoService service;
    private static CategoriaService categorias;

    public static void main(String[] args) throws Exception {

        service = new ProjetoService();

        service.carregar();
        categorias = new CategoriaService();
        categorias.carregar(service.listar());

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

        // Login web usando as mesmas credenciais do aplicativo Swing.
        server.createContext("/api/auth", exchange -> {
            try {
                configurarCors(exchange);
                if (responderOptions(exchange)) return;
                String caminho = exchange.getRequestURI().getPath();
                String metodo = exchange.getRequestMethod();
                if ("/api/auth/login".equals(caminho) && "POST".equals(metodo)) {
                    String corpo = lerCorpo(exchange);
                    String usuario = extrairCampoJson(corpo, "usuario");
                    char[] senha = extrairCampoJson(corpo, "senha").toCharArray();
                    boolean valido;
                    try { valido = AutenticacaoAdmin.validar(usuario, senha); }
                    finally { Arrays.fill(senha, '\0'); }
                    if (!valido) { enviarJson(exchange, 401, JsonUtil.erro("Usuário ou senha inválidos.")); return; }
                    byte[] bytes = new byte[32]; ALEATORIO.nextBytes(bytes);
                    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
                    SESSOES_WEB.put(token, System.currentTimeMillis() + VALIDADE);
                    enviarJson(exchange, 200, "{\"token\":" + jsonString(token) + ",\"usuario\":" + jsonString(AutenticacaoAdmin.usuario()) + "}");
                } else if ("/api/auth/session".equals(caminho) && "GET".equals(metodo)) {
                    if (!sessaoWebValida(exchange)) { enviarJson(exchange, 401, JsonUtil.erro("Sessão expirada.")); return; }
                    enviarJson(exchange, 200, "{\"usuario\":" + jsonString(AutenticacaoAdmin.usuario()) + "}");
                } else if ("/api/auth/logout".equals(caminho) && "POST".equals(metodo)) {
                    String token = tokenWeb(exchange);
                    if (token != null) SESSOES_WEB.remove(token);
                    enviarJson(exchange, 200, "{\"ok\":true}");
                } else { enviarJson(exchange, 404, JsonUtil.erro("Rota não encontrada")); }
            } catch (Exception ex) {
                enviarJson(exchange, 400, JsonUtil.erro("Requisição inválida."));
            } finally { exchange.close(); }
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

        server.createContext("/api/categorias", exchange -> {
            try {
                configurarCors(exchange);
                if (responderOptions(exchange)) return;
                tratarCategorias(exchange);
            } catch (IllegalArgumentException e) {
                enviarJson(exchange, 400, JsonUtil.erro(e.getMessage()));
            } catch (NoSuchElementException e) {
                enviarJson(exchange, 404, JsonUtil.erro(e.getMessage()));
            } catch (IllegalStateException e) {
                enviarJson(exchange, 409, JsonUtil.erro(e.getMessage()));
            } catch (Exception e) {
                e.printStackTrace();
                enviarJson(exchange, 500, JsonUtil.erro("Erro interno do servidor"));
            } finally { exchange.close(); }
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
        service.validar(projeto);
        if (!categorias.contem(projeto.getCategoria())) throw new IllegalArgumentException("Categoria não cadastrada");

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
        service.validar(projeto);
        if (!categorias.contem(projeto.getCategoria())) throw new IllegalArgumentException("Categoria não cadastrada");

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

    private static String jsonString(String texto) {
        return "\"" + texto.replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }
    private static String lerNomeCategoria(HttpExchange exchange) throws IOException {
        String json = lerCorpo(exchange).trim();
        // Corpo aceito: {"nome":"..."}; processa escapes comuns de JSON.
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"nome\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(json);
        if (!m.find()) throw new IllegalArgumentException("Informe o nome da categoria em JSON");
        String raw = m.group(1);
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '\\' && ++i < raw.length()) {
                c = raw.charAt(i);
                if (c == 'n') c = '\n'; else if (c == 'r') c = '\r'; else if (c == 't') c = '\t';
                else if (c == 'u') { if (i + 4 >= raw.length()) throw new IllegalArgumentException("Unicode inválido"); c = (char)Integer.parseInt(raw.substring(i+1,i+5),16); i+=4; }
            }
            s.append(c);
        }
        return CategoriaService.validarNome(s.toString());
    }
    private static void tratarCategorias(HttpExchange exchange) throws Exception {
        String caminho = exchange.getRequestURI().getRawPath();
        boolean raiz = caminho.equals("/api/categorias") || caminho.equals("/api/categorias/");
        if (!raiz && !caminho.startsWith("/api/categorias/")) {
            enviarJson(exchange, 404, JsonUtil.erro("Rota não encontrada")); return;
        }
        String nome = raiz ? null : URLDecoder.decode(caminho.substring("/api/categorias/".length()).replace("+", "%2B"), StandardCharsets.UTF_8);
        if (nome != null && (nome.isBlank() || nome.contains("/"))) throw new IllegalArgumentException("Categoria inválida");
        String metodo = exchange.getRequestMethod();
        if ("GET".equals(metodo) && raiz) {
            StringBuilder json = new StringBuilder("[");
            for (String categoria : categorias.listar()) {
                if (json.length() > 1) json.append(',');
                json.append(jsonString(categoria));
            }
            enviarJson(exchange, 200, json.append(']').toString());
        } else if ("POST".equals(metodo) && raiz) {
            String nova = lerNomeCategoria(exchange);
            categorias.cadastrar(nova);
            enviarJson(exchange, 201, "{\"nome\":" + jsonString(nova) + "}");
        } else if ("PUT".equals(metodo) && !raiz) {
            String nova = lerNomeCategoria(exchange);
            categorias.renomear(nome, nova, service.listar(), service);
            enviarJson(exchange, 200, "{\"nome\":" + jsonString(nova) + "}");
        } else if ("DELETE".equals(metodo) && !raiz) {
            categorias.excluir(nome, service.listar());
            exchange.sendResponseHeaders(204, -1);
        } else exchange.sendResponseHeaders(405, -1);
    }

    private static String tokenWeb(HttpExchange exchange) {
        String valor = exchange.getRequestHeaders().getFirst("Authorization");
        return valor != null && valor.startsWith("Bearer ") ? valor.substring(7) : null;
    }

    private static boolean sessaoWebValida(HttpExchange exchange) {
        String token = tokenWeb(exchange);
        if (token == null) return false;
        Long expira = SESSOES_WEB.get(token);
        if (expira == null) return false;
        if (System.currentTimeMillis() >= expira) { SESSOES_WEB.remove(token); return false; }
        return true;
    }

    private static String extrairCampoJson(String json, String campo) {
        String chave = "\"" + campo + "\"";
        int pos = json.indexOf(chave);
        if (pos < 0) return "";
        pos = json.indexOf(':', pos + chave.length());
        if (pos < 0) return "";
        pos++;
        while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) pos++;
        if (pos >= json.length() || json.charAt(pos++) != '"') return "";
        StringBuilder resultado = new StringBuilder();
        boolean escape = false;
        for (; pos < json.length(); pos++) {
            char c = json.charAt(pos);
            if (escape) {
                switch (c) {
                    case 'n': resultado.append('\n'); break;
                    case 'r': resultado.append('\r'); break;
                    case 't': resultado.append('\t'); break;
                    default: resultado.append(c);
                }
                escape = false;
            } else if (c == '\\') escape = true;
            else if (c == '"') return resultado.toString();
            else resultado.append(c);
        }
        return "";
    }

    private static void configurarCors(HttpExchange exchange) {

        exchange.getResponseHeaders().set(

                "Access-Control-Allow-Origin",

                "*");

        exchange.getResponseHeaders().set(

                "Access-Control-Allow-Methods",

                "GET, POST, PUT, DELETE, OPTIONS");

        exchange.getResponseHeaders().set(

                "Access-Control-Allow-Headers",

                "Content-Type, Authorization");

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