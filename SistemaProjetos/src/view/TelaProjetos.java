package view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import client.ProjetoApiClient;
import model.Projeto;

public class TelaProjetos extends JFrame {

    private JTextField campoNome;
    private JTextField campoDescricao;

    private JComboBox<String> comboCategoria;
    private JComboBox<String> comboStatus;

    private JTable tabela;
    private DefaultTableModel modelo;

    private JButton botaoCadastrar;
    private JButton botaoAlterar;
    private JButton botaoExcluir;
    private JButton botaoLimpar;
    private JButton botaoAtualizar;

    private JLabel mensagem;

    private ProjetoApiClient api;

    private boolean ocupado;

    // -1 significa que nenhum projeto está selecionado.
    private int idSelecionado = -1;

    public TelaProjetos() {

        setTitle("Sistema de Projetos");
        setSize(1000, 600);
        setMinimumSize(new Dimension(800, 500));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        api = new ProjetoApiClient();

        criarComponentes();
        criarEventos();
        carregarTabela();
    }

    private void criarComponentes() {

        // CAMPOS
        campoNome = new JTextField(20);
        campoDescricao = new JTextField(20);

        // CATEGORIAS
        comboCategoria = new JComboBox<>();

        comboCategoria.addItem("Web");
        comboCategoria.addItem("Software");
        comboCategoria.addItem("Mobile");
        comboCategoria.addItem("Outro");

        comboCategoria.setEditable(true);

        // STATUS
        comboStatus = new JComboBox<>();

        comboStatus.addItem("Planejado");
        comboStatus.addItem("Em desenvolvimento");
        comboStatus.addItem("Concluído");

        // BOTÕES
        botaoCadastrar = new JButton("Cadastrar");
        botaoAlterar = new JButton("Alterar");
        botaoExcluir = new JButton("Excluir");
        botaoLimpar = new JButton("Limpar");
        botaoAtualizar = new JButton("Atualizar");

        mensagem = new JLabel(" ");

        // TABELA
        modelo = new DefaultTableModel() {

            @Override
            public boolean isCellEditable(int linha, int coluna) {
                return false;
            }
        };

        modelo.addColumn("ID");
        modelo.addColumn("Nome");
        modelo.addColumn("Descrição");
        modelo.addColumn("Categoria");
        modelo.addColumn("Status");

        tabela = new JTable(modelo);

        tabela.setFillsViewportHeight(true);

        tabela.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);

        // PAINEL DO FORMULÁRIO
        JPanel painelFormulario = new JPanel(
                new GridBagLayout());

        painelFormulario.setBorder(
                BorderFactory.createTitledBorder(
                        "Dados do Projeto"));

        GridBagConstraints gbc = new GridBagConstraints();

        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // NOME
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;

        painelFormulario.add(
                new JLabel("Nome:"),
                gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;

        painelFormulario.add(
                campoNome,
                gbc);

        // DESCRIÇÃO
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;

        painelFormulario.add(
                new JLabel("Descrição:"),
                gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;

        painelFormulario.add(
                campoDescricao,
                gbc);

        // CATEGORIA
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;

        painelFormulario.add(
                new JLabel("Categoria:"),
                gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;

        painelFormulario.add(
                comboCategoria,
                gbc);

        // STATUS
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;

        painelFormulario.add(
                new JLabel("Status:"),
                gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;

        painelFormulario.add(
                comboStatus,
                gbc);

        // PAINEL DOS BOTÕES
        JPanel painelBotoes = new JPanel();

        painelBotoes.add(botaoCadastrar);
        painelBotoes.add(botaoAlterar);
        painelBotoes.add(botaoExcluir);
        painelBotoes.add(botaoLimpar);
        painelBotoes.add(botaoAtualizar);

        // PAINEL SUPERIOR
        JPanel painelSuperior = new JPanel();

        painelSuperior.setLayout(
                new BoxLayout(
                        painelSuperior,
                        BoxLayout.Y_AXIS));

        painelSuperior.add(painelFormulario);
        painelSuperior.add(painelBotoes);

        // PAINEL PRINCIPAL
        JPanel painelPrincipal = new JPanel(
                new BorderLayout(10, 10));

        painelPrincipal.setBorder(
                BorderFactory.createEmptyBorder(
                        10, 10, 10, 10));

        painelPrincipal.add(
                painelSuperior,
                BorderLayout.NORTH);

        // TABELA COM ROLAGEM
        JScrollPane scrollTabela = new JScrollPane(tabela);

        scrollTabela.setBorder(
                BorderFactory.createTitledBorder(
                        "Projetos cadastrados"));

        painelPrincipal.add(
                scrollTabela,
                BorderLayout.CENTER);

        painelPrincipal.add(
                mensagem,
                BorderLayout.SOUTH);

        setLayout(new BorderLayout());

        add(
                painelPrincipal,
                BorderLayout.CENTER);
    }

    private void criarEventos() {

        botaoAtualizar.addActionListener(
                e -> carregarTabela());

        botaoLimpar.addActionListener(
                e -> limparFormulario());

        botaoCadastrar.addActionListener(
                e -> cadastrar());

        botaoAlterar.addActionListener(
                e -> alterar());

        botaoExcluir.addActionListener(
                e -> excluir());

        tabela.getSelectionModel().addListSelectionListener(
                e -> {
                    if (!e.getValueIsAdjusting()) {
                        selecionarProjeto();
                    }
                });
    }

    private void limparFormulario() {

        campoNome.setText("");
        campoDescricao.setText("");

        comboCategoria.setSelectedIndex(0);
        comboStatus.setSelectedIndex(0);

        idSelecionado = -1;

        tabela.clearSelection();

        campoNome.requestFocus();
    }

    // GET /api/projetos
    private void carregarTabela() {

        executar(
                "Carregando projetos...",
                () -> api.listar(),
                projetos -> {
                    atualizarTabela(projetos);

                    mensagem.setText(
                            "Projetos carregados: " + projetos.size());
                });
    }

    private void atualizarTabela(List<Projeto> projetos) {

        limparFormulario();

        modelo.setRowCount(0);

        for (Projeto projeto : projetos) {

            modelo.addRow(
                    new Object[] {
                            projeto.getId(),
                            projeto.getNome(),
                            projeto.getDescricao(),
                            projeto.getCategoria(),
                            projeto.getStatus()
                    });
        }
    }

    /*
     * Executa a comunicação HTTP em segundo plano.
     *
     * doInBackground(): realiza a requisição.
     * done(): atualiza a interface após a resposta.
     */
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

    private void definirOcupado(boolean valor) {

        ocupado = valor;

        botaoCadastrar.setEnabled(!valor);
        botaoAlterar.setEnabled(!valor);
        botaoExcluir.setEnabled(!valor);
        botaoLimpar.setEnabled(!valor);
        botaoAtualizar.setEnabled(!valor);

        campoNome.setEnabled(!valor);
        campoDescricao.setEnabled(!valor);

        comboCategoria.setEnabled(!valor);
        comboStatus.setEnabled(!valor);

        tabela.setEnabled(!valor);
    }

    private void mostrarErro(Throwable erro) {

        String detalhe = erro.getMessage();

        if (detalhe == null || detalhe.isBlank()) {
            detalhe = erro.getClass().getSimpleName();
        }

        mensagem.setText(
                "Falha na operação. Verifique se a API está executando.");

        JOptionPane.showMessageDialog(
                this,
                "Erro na comunicação com a API:\n" + detalhe,
                "Erro",
                JOptionPane.ERROR_MESSAGE);
    }

    private void selecionarProjeto() {

        int linha = tabela.getSelectedRow();

        if (linha == -1) {
            return;
        }

        idSelecionado = Integer.parseInt(
                modelo.getValueAt(linha, 0).toString());

        campoNome.setText(
                java.util.Objects.toString(
                        modelo.getValueAt(linha, 1),
                        ""));

        campoDescricao.setText(
                java.util.Objects.toString(
                        modelo.getValueAt(linha, 2),
                        ""));

        comboCategoria.setSelectedItem(
                java.util.Objects.toString(
                        modelo.getValueAt(linha, 3),
                        ""));

        comboStatus.setSelectedItem(
                java.util.Objects.toString(
                        modelo.getValueAt(linha, 4),
                        ""));
    }

    private Projeto lerFormulario(int id) {

        String nome = campoNome.getText().trim();

        if (nome.isBlank()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Informe o nome.");

            return null;
        }

        return new Projeto(
                id,
                nome,
                campoDescricao.getText(),
                comboCategoria.getSelectedItem().toString(),
                comboStatus.getSelectedItem().toString());
    }

    // POST /api/projetos
    private void cadastrar() {

        if (ocupado) {
            return;
        }

        Projeto projeto = lerFormulario(0);

        if (projeto == null) {
            return;
        }

        executar(
                "Cadastrando projeto...",
                () -> api.cadastrar(projeto),
                cadastrado -> {

                    JOptionPane.showMessageDialog(
                            this,
                            "Projeto cadastrado com sucesso!\n"
                                    + "ID: " + cadastrado.getId());

                    limparFormulario();
                    carregarTabela();
                });
    }

    // PUT /api/projetos/{id}
    private void alterar() {

        if (ocupado) {
            return;
        }

        if (idSelecionado == -1) {

            JOptionPane.showMessageDialog(
                    this,
                    "Selecione um projeto na tabela para alterar.");

            return;
        }

        Projeto projeto = lerFormulario(idSelecionado);

        if (projeto == null) {
            return;
        }

        executar(
                "Alterando projeto...",
                () -> api.alterar(projeto),
                alterado -> {

                    JOptionPane.showMessageDialog(
                            this,
                            alterado == null
                                    ? "Projeto não encontrado."
                                    : "Projeto alterado com sucesso!");

                    limparFormulario();
                    carregarTabela();
                });
    }

    // DELETE /api/projetos/{id}
    private void excluir() {

        if (ocupado) {
            return;
        }

        if (idSelecionado == -1) {

            JOptionPane.showMessageDialog(
                    this,
                    "Selecione um projeto na tabela para excluir.");

            return;
        }

        final int id = idSelecionado;

        int resposta = JOptionPane.showConfirmDialog(
                this,
                "Tem certeza que deseja excluir o projeto?\n"
                        + "ID: " + id,
                "Confirmar exclusão",
                JOptionPane.YES_NO_OPTION);

        if (resposta != JOptionPane.YES_OPTION) {
            return;
        }

        executar(
                "Excluindo projeto...",
                () -> api.excluir(id),
                excluiu -> {

                    JOptionPane.showMessageDialog(
                            this,
                            excluiu
                                    ? "Projeto excluído com sucesso!"
                                    : "Projeto não encontrado.");

                    limparFormulario();
                    carregarTabela();
                });
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            TelaProjetos tela = new TelaProjetos();
            tela.setVisible(true);
        });
    }
}