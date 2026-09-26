package view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import model.Projeto;
import service.ProjetoService;

public class TelaProjetos extends JFrame {

    private JTextField campoNome;

    private JTextField campoDescricao;

    private JComboBox<String> comboCategoria;

    private JComboBox<String> comboStatus;

    private JTable tabela;

    private DefaultTableModel modelo;

    private JButton botaoCadastrar;

    private JButton botaoLimpar;

    private ProjetoService service;


    public TelaProjetos() {

        setTitle("Sistema de Projetos");

        setSize(1000, 600);

        setMinimumSize(new Dimension(800, 500));

        setLocationRelativeTo(null);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        service = new ProjetoService();

        try {

            service.carregar();

        } catch (Exception e) {

            System.err.println(
                "Aviso: Arquivo de dados inicial não carregado: "
                + e.getMessage()
            );
        }

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

        painelBotoes.add(botaoLimpar);


        /*
         * PAINEL SUPERIOR
         *
         * Junta formulário + botões
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
         * ADICIONA O PAINEL PRINCIPAL NA JANELA
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

        botaoLimpar.addActionListener(
            e -> limparFormulario()
        );

        botaoCadastrar.addActionListener(
            e -> cadastrar()
        );
    }


    private void limparFormulario() {

        campoNome.setText("");

        campoDescricao.setText("");

        comboCategoria.setSelectedIndex(0);

        comboStatus.setSelectedIndex(0);

        campoNome.requestFocus();
    }


    private void carregarTabela() {

        modelo.setRowCount(0);

        for (Projeto projeto : service.listar()) {

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
    }


    private void cadastrar() {

        String nome = campoNome.getText();

        String descricao = campoDescricao.getText();

        String categoria =
            comboCategoria.getSelectedItem().toString();

        String status =
            comboStatus.getSelectedItem().toString();


        if (nome.isBlank()) {

            JOptionPane.showMessageDialog(
                this,
                "Informe o nome."
            );

            return;
        }


        /*
         * GERA O PRÓXIMO ID
         */

        int proximoId = 1;

        for (Projeto p : service.listar()) {

            if (p.getId() >= proximoId) {

                proximoId = p.getId() + 1;
            }
        }


        /*
         * CRIA O PROJETO
         */

        Projeto projeto = new Projeto(
            proximoId,
            nome,
            descricao,
            categoria,
            status
        );


        /*
         * SALVA
         */

        try {

            boolean adicionou =
                service.adicionar(projeto);

            if (adicionou) {

                service.salvar();

                JOptionPane.showMessageDialog(
                    this,
                    "Projeto cadastrado com sucesso!"
                );

                limparFormulario();

                carregarTabela();

            } else {

                JOptionPane.showMessageDialog(
                    this,
                    "Não foi possível adicionar o projeto. "
                    + "Verifique os dados inseridos.",
                    "Aviso",
                    JOptionPane.WARNING_MESSAGE
                );
            }

        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                this,
                "Erro ao gravar as informações "
                + "no arquivo CSV: "
                + e.getMessage(),
                "Erro de Salvamento",
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