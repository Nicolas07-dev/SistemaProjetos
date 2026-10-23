package dao;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import model.Projeto;

public class ProjetoCSV {
    private final Path caminho = Path.of("dados/projetos.csv");
    private static final String CABECALHO = "id;nome;descricao;categoria;status";
    public ProjetoCSV() {
    }

    public void salvar(List<Projeto> projetos) throws Exception {

        // Cria a pasta dados caso ela ainda não exista.
        Files.createDirectories(caminho.getParent());
        StringBuilder csv = new StringBuilder(CABECALHO);
        csv.append('\n');
        for (Projeto projeto : projetos) {
            csv.append(projeto.getId()).append(';')
                    .append(formatarCampo(projeto.getNome())).append(';')
                    .append(formatarCampo(projeto.getDescricao())).append(';')
                    .append(formatarCampo(projeto.getCategoria())).append(';')
                    .append(formatarCampo(projeto.getStatus())).append('\n');
        }

        Files.writeString(
                caminho,
                csv.toString(),
                StandardCharsets.UTF_8);
    }

    public List<Projeto> listar() throws Exception {
        List<Projeto> projetos = new ArrayList<>();
        if (!Files.exists(caminho)) {
            return projetos;
        }

        String conteudo = Files.readString(
                caminho,
                StandardCharsets.UTF_8);

        // Remove a marca BOM, caso exista no início do arquivo.
        if (conteudo.startsWith("\uFEFF")) {
            conteudo = conteudo.substring(1);
        }

        if (conteudo.isBlank()) {
            return projetos;
        }

        List<List<String>> registros = lerRegistros(conteudo);
        if (registros.isEmpty()) {
            return projetos;
        }

        if (!String.join(";", registros.get(0)).equals(CABECALHO)) {
            throw new IllegalArgumentException(
                    "Cabeçalho inválido em " + caminho);
        }

        // O primeiro registro é o cabeçalho.
        for (int i = 1; i < registros.size(); i++) {
            List<String> campos = registros.get(i);

            // Ignora linhas vazias.
            if (campos.size() == 1 && campos.get(0).isBlank()) {
                continue;
            }

            if (campos.size() != 5) {
                throw new IllegalArgumentException(
                        "Registro " + (i + 1)
                                + " do CSV deve ter 5 campos.");
            }

            int id;
            try {
                id = Integer.parseInt(campos.get(0).trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "ID inválido no registro " + (i + 1)
                                + " do CSV.",
                        e);
            }

            Projeto projeto = new Projeto(
                    id,
                    campos.get(1),
                    campos.get(2),
                    campos.get(3),
                    campos.get(4));
            projetos.add(projeto);
        }

        return projetos;
    }

    /*
     * Coloca o campo entre aspas quando ele possui
     * ponto e vírgula, aspas ou quebras de linha.
     *
     * Aspas dentro do texto são duplicadas no CSV.
     */
    private String formatarCampo(String valor) {
        if (valor == null) {
            return "";
        }

        if (valor.contains(";")
                || valor.contains("\"")
                || valor.contains("\n")
                || valor.contains("\r")) {
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }

        return valor;
    }

    /*
     * Lê os registros respeitando campos entre aspas.
     * Uma quebra de linha dentro de aspas faz parte do texto.
     */
    private List<List<String>> lerRegistros(String texto) {
        List<List<String>> registros = new ArrayList<>();
        List<String> campos = new ArrayList<>();
        StringBuilder campo = new StringBuilder();
        boolean entreAspas = false;
        boolean aspasFechadas = false;
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (entreAspas) {
                if (c == '"') {

                    // Duas aspas representam uma aspa no texto.
                    if (i + 1 < texto.length()
                            && texto.charAt(i + 1) == '"') {
                        campo.append('"');
                        i++;
                    } else {
                        entreAspas = false;
                        aspasFechadas = true;
                    }

                } else {
                    campo.append(c);
                }

            } else if (c == ';' || c == '\n' || c == '\r') {
                campos.add(campo.toString());
                campo.setLength(0);
                aspasFechadas = false;
                if (c != ';') {
                    registros.add(campos);
                    campos = new ArrayList<>();

                    // Trata a quebra de linha do Windows: \r\n.
                    if (c == '\r'
                            && i + 1 < texto.length()
                            && texto.charAt(i + 1) == '\n') {
                        i++;
                    }
                }

            } else if (c == '"'
                    && campo.length() == 0
                    && !aspasFechadas) {
                entreAspas = true;
            } else {
                if (aspasFechadas) {
                    throw new IllegalArgumentException(
                            "CSV inválido: texto após o fechamento de aspas.");
                }

                campo.append(c);
            }
        }

        if (entreAspas) {
            throw new IllegalArgumentException(
                    "CSV inválido: aspas não fechadas.");
        }

        // Adiciona o último registro se não houver quebra de linha final.
        if (campo.length() > 0
                || !campos.isEmpty()
                || aspasFechadas) {
            campos.add(campo.toString());
            registros.add(campos);
        }

        return registros;
    }
}
