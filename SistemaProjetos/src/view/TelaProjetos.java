package view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

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

    private ProjetoApiClient api;

    /*
     * Guarda o ID do projeto que está sendo alterado.
     *
     * Quando for um novo cadastro, fica -1.
     */
    private int idSelecionado = -1;

    public TelaProjetos() {

        setTitle("Sistema de Projetos");

        setSize(1000, 600);

        setMinimumSize(new Dimension(800, 500));

        setLocationRelativeTo(null);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        /*
         * CLIENTE DA API
         */
        api = new ProjetoApiClient();

        criarComponentes();

        criarEventos();

        carregarTabela();
    }

    private void criarComponentes() {

        /*
         * CAMPOS
         */
        campoNome = new JTextField(20);

        campoDescricao = new JTextField(20);

        /*
         * CATEGORIA
         */
        comboCategoria = new JComboBox<>();

        comboCategoria.addItem("Web");
        comboCategoria.addItem("Software");
        comboCategoria.addItem("Mobile");
        comboCategoria.addItem("Outro");

        /*
         * STATUS
         */
        comboStatus = new JComboBox<>();

        comboStatus.addItem("Planejado");
        comboStatus.addItem("Em desenvolvimento");
        comboStatus.addItem("Concluído");

        /*
         * BOTÕES
         */
        botaoCadastrar = new JButton("Cadastrar");

        botaoAlterar = new JButton("Alterar");

        botaoExcluir = new JButton("Excluir");

        botaoLimpar = new JButton("Limpar");

        /*
         * TABELA
         */
        modelo = new DefaultTableModel();

        modelo.addColumn("ID");
        modelo.addColumn("Nome");
        modelo.addColumn("Descrição");
        modelo.addColumn("Categoria");
        modelo.addColumn("Status");

        tabela = new JTable(modelo);

        tabela.setFillsViewportHeight(true);

        /*
         * Permite selecionar apenas uma linha por vez.
         */
        tabela.setSelectionMode(
            ListSelectionModel.SINGLE_SELECTION
        );

        /*
         * PAINEL DO FORMULÁRIO
         */
        JPanel painelFormulario = new JPanel(
            new GridBagLayout()
        );

        painelFormulario.setBorder(
            BorderFactory.createTitledBorder(
                "Dados do Projeto"
            )
        );

        GridBagConstraints gbc = new GridBagConstraints();

        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.fill = GridBagConstraints.HORIZONTAL;

        /*
         * NOME
         */
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;

        painelFormulario.add(
            new JLabel("Nome:"),
            gbc
        );

        gbc.gridx = 1;
        gbc.weightx = 1;

        painelFormulario.add(
            campoNome,
            gbc
        );

        /*
         * DESCRIÇÃO
         */
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;

        painelFormulario.add(
            new JLabel("Descrição:"),
            gbc
        );

        gbc.gridx = 1;
        gbc.weightx = 1;

        painelFormulario.add(
            campoDescricao,
            gbc
        );

        /*
         * CATEGORIA
         */
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;

        painelFormulario.add(
            new JLabel("Categoria:"),
            gbc
        );

        gbc.gridx = 1;
        gbc.weightx = 1;

        painelFormulario.add(
            comboCategoria,
            gbc
        );

        /*
         * STATUS
         */
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;

        painelFormulario.add(
            new JLabel("Status:"),
            gbc
        );

        gbc.gridx = 1;
        gbc.weightx = 1;

        painelFormulario.add(
            comboStatus,
            gbc
        );

        /*
         * PAINEL DOS BOTÕES
         */
        JPanel painelBotoes = new JPanel();

        painelBotoes.add(botaoCadastrar);

        painelBotoes.add(botaoAlterar);

        painelBotoes.add(botaoExcluir);

        painelBotoes.add(botaoLimpar);

        /*
         * PAINEL SUPERIOR
         */
        JPanel painelSuperior = new JPanel();

        painelSuperior.setLayout(
            new BoxLayout(
                painelSuperior,
                BoxLayout.Y_AXIS
            )
        );

        painelSuperior.add(painelFormulario);

        painelSuperior.add(painelBotoes);

        /*
         * PAINEL PRINCIPAL
         */
        JPanel painelPrincipal = new JPanel(
            new BorderLayout(10, 10)
        );

        painelPrincipal.setBorder(
            BorderFactory.createEmptyBorder(
                10,
                10,
                10,
                10
            )
        );

        /*
         * FORMULÁRIO NO TOPO
         */
        painelPrincipal.add(
            painelSuperior,
            BorderLayout.NORTH
        );

        /*
         * TABELA NO CENTRO
         */
        JScrollPane scrollTabela = new JScrollPane(
            tabela
        );

        scrollTabela.setBorder(
            BorderFactory.createTitledBorder(
                "Projetos cadastrados"
            )
        );

        painelPrincipal.add(
            scrollTabela,
            BorderLayout.CENTER
        );

        /*
         * ADICIONA O PAINEL PRINCIPAL
         */
        setLayout(
            new BorderLayout()
        );

        add(
            painelPrincipal,
            BorderLayout.CENTER
        );
    }

    private void criarEventos() {

        /*
         * BOTÃO LIMPAR
         */
        botaoLimpar.addActionListener(
            e -> limparFormulario()
        );

        /*
         * BOTÃO CADASTRAR
         */
        botaoCadastrar.addActionListener(
            e -> cadastrar()
        );

        /*
         * BOTÃO ALTERAR
         */
        botaoAlterar.addActionListener(
            e -> alterar()
        );

        /*
         * BOTÃO EXCLUIR
         */
        botaoExcluir.addActionListener(
            e -> excluir()
        );

        /*
         * SELEÇÃO DA TABELA
         */
        tabela.getSelectionModel().addListSelectionListener(
            e -> selecionarProjeto()
        );
    }

    /*
     * LIMPA O FORMULÁRIO
     */
    private void limparFormulario() {

        campoNome.setText("");

        campoDescricao.setText("");

        comboCategoria.setSelectedIndex(0);

        comboStatus.setSelectedIndex(0);

        /*
         * Nenhum projeto selecionado.
         */
        idSelecionado = -1;

        tabela.clearSelection();

        campoNome.requestFocus();
    }

    /*
     * CARREGA OS PROJETOS ATRAVÉS DA API
     */
    private void carregarTabela() {

        try {

            modelo.setRowCount(0);

            for (Projeto projeto : api.listar()) {

                modelo.addRow(
                    new Object[]{
                        projeto.getId(),
                        projeto.getNome(),
                        projeto.getDescricao(),
                        projeto.getCategoria(),
                        projeto.getStatus()
                    }
                );
            }

            modelo.fireTableDataChanged();

        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                this,
                "Erro ao carregar os projetos pela API:\n"
                + e.getMessage(),
                "Erro",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    /*
     * SELECIONA UM PROJETO DA TABELA
     */
    private void selecionarProjeto() {

        int linha = tabela.getSelectedRow();

        /*
         * Nenhuma linha selecionada.
         */
        if (linha == -1) {
            return;
        }

        /*
         * Pega o ID da primeira coluna.
         */
        idSelecionado = Integer.parseInt(
            modelo.getValueAt(linha, 0).toString()
        );

        /*
         * Preenche os campos.
         */
        campoNome.setText(
            modelo.getValueAt(linha, 1).toString()
        );

        campoDescricao.setText(
            modelo.getValueAt(linha, 2).toString()
        );

        comboCategoria.setSelectedItem(
            modelo.getValueAt(linha, 3).toString()
        );

        comboStatus.setSelectedItem(
            modelo.getValueAt(linha, 4).toString()
        );
    }

    /*
     * CADASTRAR
     *
     * POST /api/projetos
     */
    private void cadastrar() {

        String nome = campoNome.getText();

        String descricao = campoDescricao.getText();

        String categoria =
            comboCategoria.getSelectedItem().toString();

        String status =
            comboStatus.getSelectedItem().toString();

        /*
         * VERIFICA O NOME
         */
        if (nome.isBlank()) {

            JOptionPane.showMessageDialog(
                this,
                "Informe o nome."
            );

            return;
        }

        /*
         * Cria o projeto sem ID.
         *
         * A API gera o próximo ID.
         */
        Projeto projeto = new Projeto(
            nome,
            descricao,
            categoria,
            status
        );

        try {

            Projeto cadastrado =
                api.cadastrar(projeto);

            JOptionPane.showMessageDialog(
                this,
                "Projeto cadastrado com sucesso!\n"
                + "ID: " + cadastrado.getId()
            );

            limparFormulario();

            carregarTabela();

        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                this,
                "Erro ao cadastrar projeto pela API:\n"
                + e.getMessage(),
                "Erro",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    /*
     * ALTERAR
     *
     * PUT /api/projetos/{id}
     */
    private void alterar() {

        /*
         * Verifica se existe projeto selecionado.
         */
        if (idSelecionado == -1) {

            JOptionPane.showMessageDialog(
                this,
                "Selecione um projeto na tabela para alterar.",
                "Aviso",
                JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        String nome = campoNome.getText();

        String descricao = campoDescricao.getText();

        String categoria =
            comboCategoria.getSelectedItem().toString();

        String status =
            comboStatus.getSelectedItem().toString();

        /*
         * Verifica o nome.
         */
        if (nome.isBlank()) {

            JOptionPane.showMessageDialog(
                this,
                "Informe o nome."
            );

            return;
        }

        /*
         * Cria o projeto com o mesmo ID.
         */
        Projeto projeto = new Projeto(
            idSelecionado,
            nome,
            descricao,
            categoria,
            status
        );

        try {

            Projeto alterado =
                api.alterar(projeto);

            if (alterado == null) {

                JOptionPane.showMessageDialog(
                    this,
                    "Projeto não encontrado.",
                    "Aviso",
                    JOptionPane.WARNING_MESSAGE
                );

                return;
            }

            JOptionPane.showMessageDialog(
                this,
                "Projeto alterado com sucesso!"
            );

            limparFormulario();

            carregarTabela();

        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                this,
                "Erro ao alterar projeto pela API:\n"
                + e.getMessage(),
                "Erro",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    /*
     * EXCLUIR
     *
     * DELETE /api/projetos/{id}
     */
    private void excluir() {

        /*
         * Verifica se existe projeto selecionado.
         */
        if (idSelecionado == -1) {

            JOptionPane.showMessageDialog(
                this,
                "Selecione um projeto na tabela para excluir.",
                "Aviso",
                JOptionPane.WARNING_MESSAGE
            );

            return;
        }

        /*
         * Confirmação.
         */
        int resposta = JOptionPane.showConfirmDialog(
            this,
            "Tem certeza que deseja excluir o projeto?\n"
            + "ID: " + idSelecionado,
            "Confirmar exclusão",
            JOptionPane.YES_NO_OPTION
        );

        if (resposta != JOptionPane.YES_OPTION) {
            return;
        }

        try {

            boolean excluiu =
                api.excluir(idSelecionado);

            if (excluiu) {

                JOptionPane.showMessageDialog(
                    this,
                    "Projeto excluído com sucesso!"
                );

                limparFormulario();

                carregarTabela();

            } else {

                JOptionPane.showMessageDialog(
                    this,
                    "Projeto não encontrado.",
                    "Aviso",
                    JOptionPane.WARNING_MESSAGE
                );
            }

        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                this,
                "Erro ao excluir projeto pela API:\n"
                + e.getMessage(),
                "Erro",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            TelaProjetos tela =
                new TelaProjetos();

            tela.setVisible(true);
        });
    }
}