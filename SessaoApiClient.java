
package client;

public final class SessaoApiClient {

    private static String token;

    private SessaoApiClient() {
    }

    public static void definirToken(String novoToken) {
        token = novoToken;
    }

    public static String obterToken() {
        return token;
    }

    public static boolean ativa() {
        return token != null && !token.isBlank();
    }

    public static void encerrar() {
        token = null;
    }
}
