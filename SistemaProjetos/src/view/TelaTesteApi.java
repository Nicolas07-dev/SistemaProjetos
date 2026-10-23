package view;
import client.ProjetoApiClient;
import model.Projeto;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.*;

public class TelaTesteApi extends JFrame {
    private final ProjetoApiClient api = new ProjetoApiClient();
    private final JButton listar = new JButton("Listar");
    private final JButton cadastrar = new JButton("Cadastrar");
    private final JButton alterar = new JButton("Alterar");
    private final JButton excluir = new JButton("Excluir");
    private final JTextArea resultado = new JTextArea();
    private final JLabel mensagem = new JLabel(
            "Inicie a API antes de usar os botões.");
    private boolean ocupado;
    public TelaTesteApi() {
        setTitle("Teste da API");
        setSize(700, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JPanel botoes = new JPanel(
                new GridLayout(4, 1, 8, 8));
        botoes.add(listar);
        botoes.add(cadastrar);
        botoes.add(alterar);
        botoes.add(excluir);
        resultado.setEditable(false);
        resultado.setLineWrap(true);
        resultado.setWrapStyleWord(true);
        JPanel painel = new JPanel(
                new BorderLayout(12, 12));
        painel.setBorder(
                BorderFactory.createEmptyBorder(12, 12, 12, 12));
        painel.add(botoes, BorderLayout.WEST);
        painel.add(
                new JScrollPane(resultado),
                BorderLayout.CENTER);
        painel.add(mensagem, BorderLayout.SOUTH);
        setContentPane(painel);
        listar.addActionListener(e -> listarProjetos());
        cadastrar.addActionListener(e -> cadastrarProjeto());
        alterar.addActionListener(e -> alterarProjeto());
        excluir.addActionListener(e -> excluirProjeto());
    }

    private void listarProjetos() {
        executar(
                "GET - Listando projetos...",
                () -> api.listar(),
                projetos -> {
                    resultado.setText("GET /api/projetos\n\n");
                    if (projetos.isEmpty()) {
                        resultado.append("Nenhum projeto cadastrado.\n");
                    }

                    for (Projeto projeto : projetos) {
                        exibirProjeto(projeto);
                    }

                    mensagem.setText(
                            "Consulta concluída: "
                                    + projetos.size() + " projeto(s).");
                });
    }

    private void cadastrarProjeto() {
        if (ocupado) {
            return;
        }

        Projeto projeto = formulario(null);
        if (projeto == null) {
            return;
        }

        executar(
                "POST - Cadastrando projeto...",
                () -> api.cadastrar(projeto),
                criado -> {
                    resultado.setText(
                            "POST /api/projetos - Cadastro concluído\n\n");
                    exibirProjeto(criado);
                    mensagem.setText(
                            "Projeto cadastrado. ID: " + criado.getId());
                });
    }

    private void alterarProjeto() {
        if (ocupado) {
            return;
        }

        Integer id = pedirId();
        if (id == null) {
            return;
        }

        // Busca os dados atuais antes de abrir o formulário.
        executar(
                "GET - Buscando projeto...",
                () -> api.buscarPorId(id),
                existente -> {
                    if (existente == null) {
                        avisarNaoEncontrado();
                        return;
                    }

                    Projeto projeto = formulario(existente);
                    if (projeto == null) {
                        mensagem.setText("Alteração cancelada.");
                        return;
                    }

                    executar(
                            "PUT - Alterando projeto...",
                            () -> api.alterar(projeto),
                            atualizado -> {
                                if (atualizado == null) {
                                    avisarNaoEncontrado();
                                    return;
                                }

                                resultado.setText(
                                        "PUT /api/projetos/" + id
                                                + " - Alteração concluída\n\n");
                                exibirProjeto(atualizado);
                                mensagem.setText(
                                        "Projeto alterado com sucesso.");
                            });
                });
    }

    private void excluirProjeto() {
        if (ocupado) {
            return;
        }

        Integer id = pedirId();
        if (id == null) {
            return;
        }

        int confirmacao = JOptionPane.showConfirmDialog(
                this,
                "Excluir o projeto de ID " + id + "?",
                "Confirmar exclusão",
                JOptionPane.YES_NO_OPTION);
        if (confirmacao != JOptionPane.YES_OPTION) {
            return;
        }

        executar(
                "DELETE - Excluindo projeto...",
                () -> api.excluir(id),
                removido -> {
                    if (!removido) {
                        avisarNaoEncontrado();
                        return;
                    }

                    resultado.setText(
                            "DELETE /api/projetos/" + id
                                    + "\nProjeto excluído com sucesso.\n");
                    mensagem.setText(
                            "Exclusão concluída. Clique em Listar "
                                    + "para consultar os registros restantes.");
                });
    }

    private Integer pedirId() {
        String texto = JOptionPane.showInputDialog(
                this,
                "Informe o ID do projeto:");
        if (texto == null) {
            return null;
        }

        try {
            int id = Integer.parseInt(texto.trim());
            if (id <= 0) {
                throw new NumberFormatException();
            }

            return id;
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Informe um ID inteiro maior que zero.");
            return null;
        }
    }

    /*
     * Abre um formulário para cadastrar ou alterar.
     * Se existente for null, é um novo cadastro.
     */
    private Projeto formulario(Projeto existente) {
        JTextField nome = new JTextField(
                existente == null ? "" : texto(existente.getNome()));
        JTextField descricao = new JTextField(
                existente == null ? "" : texto(existente.getDescricao()));
        JTextField categoria = new JTextField(
                existente == null ? "Web" : texto(existente.getCategoria()));
        JComboBox<String> status = new JComboBox<>(
                new String[] {
                        "Planejado",
                        "Em desenvolvimento",
                        "Concluído"
                });
        if (existente != null) {
            String atual = texto(existente.getStatus());
            boolean encontrado = false;
            for (int i = 0; i < status.getItemCount(); i++) {
                if (status.getItemAt(i).equals(atual)) {
                    encontrado = true;
                }
            }

            if (!encontrado) {
                status.addItem(atual);
            }

            status.setSelectedItem(atual);
        }

        JPanel painel = new JPanel(
                new GridLayout(4, 2, 8, 8));
        painel.add(new JLabel("Nome:"));
        painel.add(nome);
        painel.add(new JLabel("Descrição:"));
        painel.add(descricao);
        painel.add(new JLabel("Categoria:"));
        painel.add(categoria);
        painel.add(new JLabel("Status:"));
        painel.add(status);
        while (true) {
            int resposta = JOptionPane.showConfirmDialog(
                    this,
                    painel,
                    existente == null
                            ? "Cadastrar projeto"
                            : "Alterar projeto - ID " + existente.getId(),
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);
            if (resposta != JOptionPane.OK_OPTION) {
                return null;
            }

            if (nome.getText().isBlank()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Informe o nome.");
                continue;
            }

            return new Projeto(
                    existente == null ? 0 : existente.getId(),
                    nome.getText().trim(),
                    descricao.getText(),
                    categoria.getText(),
                    status.getSelectedItem().toString());
        }
    }

    private String texto(String valor) {
        return valor == null ? "" : valor;
    }

    private void exibirProjeto(Projeto projeto) {
        resultado.append(
                "ID: " + projeto.getId() + "\n"
                        + "Nome: " + texto(projeto.getNome()) + "\n"
                        + "Descrição: " + texto(projeto.getDescricao()) + "\n"
                        + "Categoria: " + texto(projeto.getCategoria()) + "\n"
                        + "Status: " + texto(projeto.getStatus()) + "\n\n");
    }

    private void avisarNaoEncontrado() {
        mensagem.setText("Projeto não encontrado.");
        JOptionPane.showMessageDialog(
                this,
                "Projeto não encontrado.");
    }

    private void definirOcupado(boolean valor) {
        ocupado = valor;
        listar.setEnabled(!valor);
        cadastrar.setEnabled(!valor);
        alterar.setEnabled(!valor);
        excluir.setEnabled(!valor);
    }

    // Executa as requisições sem congelar a janela.
    private <T> void executar(
            String texto,
            Callable<T> operacao,
            Consumer<T> sucesso) {
        if (ocupado) {
            return;
        }

        definirOcupado(true);
        mensagem.setText(texto);
        new SwingWorker<T, Void>() {

            @Override
            protected T doInBackground() throws Exception {
                return operacao.call();
            }

            @Override
            protected void done() {
                definirOcupado(false);
                try {
                    sucesso.accept(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    mostrarErro(e);
                } catch (ExecutionException e) {
                    mostrarErro(e.getCause());
                } catch (RuntimeException e) {
                    mostrarErro(e);
                }
            }

        }.execute();
    }

    private void mostrarErro(Throwable erro) {
        String detalhe = erro.getMessage();
        if (detalhe == null || detalhe.isBlank()) {
            detalhe = erro.getClass().getSimpleName();
        }

        mensagem.setText(
                "Falha na operação. Verifique se a API está executando.");
        resultado.setText("Erro: " + detalhe);
        JOptionPane.showMessageDialog(
                this,
                detalhe,
                "Erro na API",
                JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(
                () -> new TelaTesteApi().setVisible(true));
    }
}
