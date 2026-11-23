
package util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SistemaLogs {

    private static final Path PASTA_LOGS = Paths.get("logs");

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static final Object BLOQUEIO = new Object();

    private static void registrar(String arquivo, String nivel, String mensagem) {

        try {
            Files.createDirectories(PASTA_LOGS);

            Path caminho = PASTA_LOGS.resolve(arquivo);

            String registro = "[" +
                    LocalDateTime.now().format(FORMATO) +
                    "] " + nivel + " - " + mensagem +
                    System.lineSeparator();

            synchronized (BLOQUEIO) {
                Files.writeString(
                        caminho,
                        registro,
                        java.nio.file.StandardOpenOption.CREATE,
                        java.nio.file.StandardOpenOption.APPEND);
            }

        } catch (IOException e) {
            System.err.println("Falha ao registrar log: " + e.getMessage());
        }
    }

    public static void application(String mensagem) {
        registrar("application.log", "INFO", mensagem);
    }

    public static void access(String mensagem) {
        registrar("access.log", "INFO", mensagem);
    }

    public static void audit(String mensagem) {
        registrar("audit.log", "INFO", mensagem);
    }

    public static void error(String mensagem) {
        registrar("error.log", "ERROR", mensagem);
    }

    public static void api(String mensagem) {
        registrar("api.log", "INFO", mensagem);
    }
}
