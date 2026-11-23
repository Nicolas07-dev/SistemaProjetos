package dao;

import java.time.LocalDate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import model.Projeto;

public class ProjetoCSV {
    private final Path caminho = Path.of("dados/projetos.csv");
    private static final String CABECALHO_ANTIGO = "id;nome;descricao;categoria;status";
    private static final String CABECALHO = "id;nome;descricao;categoria;status;dataCriacao;prazo";

    public ProjetoCSV() {
    }

    public void salvar(List<Projeto> projetos) throws Exception {

        Files.createDirectories(caminho.getParent());
        StringBuilder csv = new StringBuilder(CABECALHO);
        csv.append('\n');
        for (Projeto projeto : projetos) {
            csv.append(projeto.getId()).append(';')
                    .append(formatarCampo(projeto.getNome())).append(';')
                    .append(formatarCampo(projeto.getDescricao())).append(';')

                    .append(formatarCampo(projeto.getCategoria())).append(';')
                    .append(formatarCampo(projeto.getStatus())).append(';')
                    .append(projeto.getDataCriacao() == null
                            ? ""
                            : projeto.getDataCriacao().toString())
                    .append(';')
                    .append(projeto.getPrazo() == null
                            ? ""
                            : projeto.getPrazo().toString())
                    .append('\n');

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

        String cabecalhoLido = String.join(";", registros.get(0));

        boolean formatoAntigo = cabecalhoLido.equals(CABECALHO_ANTIGO);
        boolean formatoNovo = cabecalhoLido.equals(CABECALHO);

        if (!formatoAntigo && !formatoNovo) {
            throw new IllegalArgumentException(
                    "Cabeçalho inválido em " + caminho);
        }

        for (int i = 1; i < registros.size(); i++) {
            List<String> campos = registros.get(i);

            if (campos.size() == 1 && campos.get(0).isBlank()) {
                continue;
            }

            int quantidadeEsperada = formatoAntigo ? 5 : 7;

            if (campos.size() != quantidadeEsperada) {
                throw new IllegalArgumentException(
                        "Registro " + (i + 1)
                                + " do CSV deve ter "
                                + quantidadeEsperada + " campos.");
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

            Projeto projeto;

            if (formatoAntigo) {
                projeto = new Projeto(
                        id,
                        campos.get(1),
                        campos.get(2),
                        campos.get(3),
                        campos.get(4));

            } else {
                LocalDate dataCriacao;
                LocalDate prazo;

                try {
                    dataCriacao = campos.get(5).isBlank()
                            ? null
                            : LocalDate.parse(campos.get(5).trim());

                    prazo = campos.get(6).isBlank()
                            ? null
                            : LocalDate.parse(campos.get(6).trim());

                } catch (java.time.format.DateTimeParseException e) {
                    throw new IllegalArgumentException(
                            "Data inválida no registro " + (i + 1)
                                    + " do CSV.",
                            e);
                }

                projeto = new Projeto(
                        id,
                        campos.get(1),
                        campos.get(2),
                        campos.get(3),
                        campos.get(4),
                        dataCriacao,
                        prazo);
            }

            projetos.add(projeto);

        }

        return projetos;
    }

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

        if (campo.length() > 0
                || !campos.isEmpty()
                || aspasFechadas) {
            campos.add(campo.toString());
            registros.add(campos);
        }

        return registros;
    }
}
