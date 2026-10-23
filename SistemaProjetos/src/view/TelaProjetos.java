package view;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;
import java.util.Objects;
import java.util.prefs.Preferences;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import security.AutenticacaoAdmin;
import client.ProjetoApiClient;
import client.CategoriaApiClient;
import model.Projeto;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;

public class TelaProjetos extends JFrame {
    private static final Color AZUL = new Color(16, 35, 67);
    private static final Color AZUL_CLARO = new Color(43, 105, 213);
    private static final Color FUNDO = new Color(245, 247, 251);
    private static final Color TEXTO = new Color(33, 51, 76);
    private static final Preferences PREFERENCIAS = Preferences.userNodeForPackage(TelaProjetos.class);
    private static final String CHAVE_TEMA = "temaEscuro";
    private boolean temaEscuro = false;
    private JButton menuConfiguracoes;
    private JButton botaoSair;
    private JPanel painelConfiguracoes;
    private JComboBox<String> escolhaTema;
    private JButton botaoSalvarPreferencias, botaoRestaurarPadrao;
    private JLabel situacaoPreferencias;
    private JButton botaoTema;
    private JPanel painelLateral, painelTopo, painelConteudo;
    private CardLayout navegacaoTelas;
    private JPanel painelTelas;
    private JPanel painelDashboard;
    private JButton menuDashboard, menuProjetos, menuRelatorios, menuCategorias;
    private JPanel painelCategorias;
    private JTable tabelaCategorias;
    private DefaultTableModel modeloCategorias;
    private JButton botaoNovaCategoria, botaoEditarCategoria, botaoExcluirCategoria, botaoRecarregarCategorias;
    private final CategoriaApiClient apiCategorias = new CategoriaApiClient();
    private List<String> categoriasCarregadas = new ArrayList<>();
    private JPanel painelRelatorios;
    private JComboBox<String> relCategoria, relStatus;
    private DefaultTableModel modeloRelatorio;
    private JLabel resumoRelatorio;
    private JButton botaoCsv, botaoPdf;
    private JLabel tituloTopo;
    private GraficoBarras graficoStatus, graficoCategorias;
    private JLabel resumoDashboard;
    private final List<JPanel> cartoesTema = new ArrayList<>();
    private final List<JLabel> rotulosTema = new ArrayList<>();
    private final List<JLabel> rotulosErro = new ArrayList<>();
    private final List<JLabel> valoresEstatisticos = new ArrayList<>();
    private JTextField campoNome, campoDescricao;
    private JComboBox<String> comboCategoria, comboStatus;
    private JTable tabela;
    private DefaultTableModel modelo;
    private JButton botaoCadastrar, botaoAlterar, botaoExcluir, botaoLimpar, botaoAtualizar;
    private JTextField campoBusca;
    private JComboBox<String> filtroCategoria, filtroStatus;
    private TableRowSorter<DefaultTableModel> ordenador;
    private List<Projeto> projetosCarregados = new ArrayList<>();
    private JLabel mensagem;
    private JButton botaoAnterior, botaoProxima;
    private JLabel indicadorPagina;
    private static final int TAMANHO_PAGINA = 10;
    private int paginaAtual = 0;
    private int totalFiltrados = 0;
    private JLabel erroNome, erroDescricao, erroStatus;
    private boolean validacaoAtivada = false;
    private JLabel totalValor, concluidosValor, andamentoValor, planejadosValor;
    private final ProjetoApiClient api;
    private boolean ocupado;
    private int idSelecionado = -1;
    public TelaProjetos() {
        setTitle("Sistema de Projetos");
        setSize(1440, 820);
        setMinimumSize(new Dimension(1100, 650));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        api = new ProjetoApiClient();
        criarComponentes();
        criarEventos();

        // Aplica o tema persistido depois que todos os componentes existem.
        if (PREFERENCIAS.getBoolean(CHAVE_TEMA, false)) aplicarTema(true);
        carregarTabela();
    }

    private JLabel titulo(String texto, int tamanho) {
        JLabel label = new JLabel(texto);
        label.setFont(new Font("SansSerif", Font.BOLD, tamanho));
        label.setForeground(TEXTO);
        rotulosTema.add(label);
        return label;
    }

    private JPanel cartao() {
        JPanel painel = new JPanel(new BorderLayout(0, 12));
        painel.setBackground(Color.WHITE);
        painel.putClientProperty("FlatLaf.style", "arc: 16");
        cartoesTema.add(painel);
        painel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(227, 233, 242)),
            BorderFactory.createEmptyBorder(18, 20, 18, 20)));
        return painel;
    }

    private JButton itemMenu(String texto, boolean ativo) {
        String icone = texto.equals("Dashboard") ? "◈" : texto.equals("Projetos") ? "▣"
            : texto.equals("Categorias") ? "▤" : texto.equals("Relatórios") ? "▥" : "⚙";
        JButton botao = new JButton(icone + "    " + texto);
        botao.setHorizontalAlignment(SwingConstants.LEFT);
        botao.setFocusPainted(false);
        botao.setForeground(Color.WHITE);
        botao.setBackground(ativo ? AZUL_CLARO : AZUL);
        botao.setBorder(BorderFactory.createEmptyBorder(13, 16, 13, 10));
        botao.setFont(new Font("SansSerif", Font.BOLD, 13));
        botao.putClientProperty("JButton.buttonType", "roundRect");
        botao.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

        // Itens não implementados não navegam para telas inexistentes.
        if (!ativo && !texto.equals("Dashboard") && !texto.equals("Relatórios") && !texto.equals("Categorias") && !texto.equals("Configurações")) {
            botao.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Esta seção será implementada nas próximas etapas."));
        }

        return botao;
    }

    private JPanel cardEstatistico(String nome, Color cor, java.util.function.Consumer<JLabel> guardar) {
        JPanel painel = new JPanel(new BorderLayout(0, 8));
        painel.setBackground(Color.WHITE);
        cartoesTema.add(painel);
        painel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(227, 233, 242)),
            BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        JLabel tituloCard = new JLabel(nome);
        tituloCard.setForeground(TEXTO);
        rotulosTema.add(tituloCard);
        tituloCard.setFont(new Font("SansSerif", Font.BOLD, 12));
        JLabel valor = new JLabel("0");
        valor.setFont(new Font("SansSerif", Font.BOLD, 32));
        valor.setForeground(cor);
        valoresEstatisticos.add(valor);
        painel.add(tituloCard, BorderLayout.NORTH);
        painel.add(valor, BorderLayout.CENTER);
        guardar.accept(valor);
        return painel;
    }

    private void atualizarEstatisticas(List<Projeto> projetos) {
        int concluidos = 0, andamento = 0, planejados = 0;
        for (Projeto projeto : projetos) {
            String status = Objects.toString(projeto.getStatus(), "").trim();
            if (status.equalsIgnoreCase("Concluído")) concluidos++;
            else if (status.equalsIgnoreCase("Em desenvolvimento")) andamento++;
            else if (status.equalsIgnoreCase("Planejado")) planejados++;
        }

        totalValor.setText(String.valueOf(projetos.size()));
        concluidosValor.setText(String.valueOf(concluidos));
        andamentoValor.setText(String.valueOf(andamento));
        planejadosValor.setText(String.valueOf(planejados));
    }

    // Renderiza a coluna Status como uma etiqueta colorida, sem alterar os dados.
    private void personalizarTabela() {
        tabela.setRowHeight(42);
        tabela.setShowVerticalLines(false);
        tabela.setShowHorizontalLines(true);
        tabela.setGridColor(temaEscuro ? new Color(55, 65, 82) : new Color(231, 237, 245));
        tabela.setSelectionBackground(temaEscuro ? new Color(48, 76, 110) : new Color(216, 231, 250));
        tabela.setSelectionForeground(temaEscuro ? Color.WHITE : TEXTO);
        tabela.setIntercellSpacing(new Dimension(0, 1));
        JTableHeader cabecalho = tabela.getTableHeader();
        cabecalho.setPreferredSize(new Dimension(0, 42));
        cabecalho.setBackground(temaEscuro ? new Color(43, 56, 74) : new Color(231, 239, 249));
        cabecalho.setForeground(temaEscuro ? new Color(226, 235, 249) : AZUL);
        cabecalho.setFont(new Font("SansSerif", Font.BOLD, 12));
        DefaultTableCellRenderer rendererPadrao = new DefaultTableCellRenderer() {

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean selecionado, boolean foco, int linha, int coluna) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, selecionado, foco, linha, coluna);
                label.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                label.setFont(new Font("SansSerif", Font.PLAIN, 12));
                if (!selecionado) {
                    label.setBackground(corLinha(linha));
                    label.setForeground(temaEscuro ? new Color(225, 232, 242) : TEXTO);
                }

                return label;
            }

        };
        for (int i = 0; i < tabela.getColumnCount(); i++) {
            tabela.getColumnModel().getColumn(i).setCellRenderer(rendererPadrao);
        }

        tabela.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean selecionado, boolean foco, int linha, int coluna) {
                String status = Objects.toString(value, "").trim();
                Color fundo = temaEscuro ? new Color(45, 75, 115) : new Color(231, 239, 249);
                Color corTexto = temaEscuro ? new Color(190, 218, 255) : new Color(49, 93, 178);
                if (status.equalsIgnoreCase("Concluído")) {
                    fundo = temaEscuro ? new Color(32, 85, 65) : new Color(220, 252, 231);
                    corTexto = temaEscuro ? new Color(160, 239, 193) : new Color(22, 122, 71);
                } else if (status.equalsIgnoreCase("Em desenvolvimento")) {
                    fundo = temaEscuro ? new Color(94, 70, 32) : new Color(254, 243, 199);
                    corTexto = temaEscuro ? new Color(255, 216, 142) : new Color(156, 101, 7);
                }

                JPanel painel = new JPanel(new GridBagLayout());
                painel.setOpaque(true);
                painel.setBackground(selecionado ? table.getSelectionBackground()
                    : (corLinha(linha)));
                JLabel etiqueta = new JLabel(status);
                etiqueta.setOpaque(true);
                etiqueta.setBackground(fundo);
                etiqueta.setForeground(corTexto);
                etiqueta.setFont(new Font("SansSerif", Font.BOLD, 11));
                etiqueta.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                painel.add(etiqueta);
                return painel;
            }

        });
    }

    private void criarComponentes() {
        campoNome = new JTextField(20);
        campoDescricao = new JTextField(20);
        comboCategoria = new JComboBox<>(new String[]{"Web", "Software", "Mobile", "Outro"});
        comboCategoria.setEditable(false);
        comboStatus = new JComboBox<>(new String[]{"Planejado", "Em desenvolvimento", "Concluído"});
        botaoCadastrar = new JButton("▣  Salvar");
        botaoAlterar = new JButton("✎  Alterar");
        botaoExcluir = new JButton("▤  Excluir");
        botaoLimpar = new JButton("⌁  Limpar");
        botaoAtualizar = new JButton("Atualizar");
        botaoCadastrar.setBackground(AZUL_CLARO);
        botaoCadastrar.setForeground(Color.WHITE);
        botaoExcluir.setBackground(new Color(183, 52, 62));
        botaoExcluir.setForeground(Color.WHITE);
        modelo = new DefaultTableModel(new String[]{"ID", "Nome", "Descrição", "Categoria", "Status"}, 0) {

            @Override public boolean isCellEditable(int linha, int coluna) { return false; }

        };
        tabela = new JTable(modelo);
        ordenador = new TableRowSorter<>(modelo);
        tabela.setRowSorter(ordenador);
        tabela.setFillsViewportHeight(true);
        tabela.setRowHeight(30);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.getTableHeader().setReorderingAllowed(false);
        tabela.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        personalizarTabela();
        JPanel lateral = new JPanel();
        painelLateral = lateral;
        lateral.setLayout(new BoxLayout(lateral, BoxLayout.Y_AXIS));
        lateral.setBackground(AZUL);
        lateral.setPreferredSize(new Dimension(218, 0));
        lateral.setBorder(BorderFactory.createEmptyBorder(24, 12, 16, 12));
        JLabel logo = new JLabel("◈  Sistema de Projetos");
        logo.setForeground(Color.WHITE);
        logo.setFont(new Font("SansSerif", Font.BOLD, 14));
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        lateral.add(logo);
        lateral.add(Box.createVerticalStrut(42));
        for (String item : new String[]{"Dashboard", "Projetos", "Categorias", "Relatórios", "Configurações"}) {
            JButton menu = itemMenu(item, item.equals("Projetos"));
            if (item.equals("Dashboard")) menuDashboard = menu;
            if (item.equals("Projetos")) menuProjetos = menu;
            if (item.equals("Relatórios")) menuRelatorios = menu;
            if (item.equals("Categorias")) menuCategorias = menu;
            if (item.equals("Configurações")) menuConfiguracoes = menu;
            menu.setAlignmentX(Component.LEFT_ALIGNMENT);
            lateral.add(menu);
            lateral.add(Box.createVerticalStrut(5));
        }

        lateral.add(Box.createVerticalGlue());
        JLabel rodapeMenu = new JLabel("  ●  Sistema ativo  •  v1.0");
        rodapeMenu.setForeground(new Color(161, 183, 219));
        rodapeMenu.setFont(new Font("SansSerif", Font.PLAIN, 11));
        rodapeMenu.setAlignmentX(Component.LEFT_ALIGNMENT);
        lateral.add(rodapeMenu);
        lateral.add(Box.createVerticalStrut(12));
        JLabel perfil = new JLabel("  ◉  Administrador");
        perfil.setForeground(Color.WHITE);
        perfil.setFont(new Font("SansSerif", Font.BOLD, 13));
        perfil.setAlignmentX(Component.LEFT_ALIGNMENT);
        lateral.add(perfil);
        lateral.add(Box.createVerticalStrut(10));
        botaoSair = new JButton("↪  Sair da conta");
        botaoSair.setAlignmentX(Component.LEFT_ALIGNMENT);
        botaoSair.setHorizontalAlignment(SwingConstants.LEFT);
        botaoSair.setForeground(Color.WHITE);
        botaoSair.setBackground(new Color(149, 52, 65));
        botaoSair.setFocusPainted(false);
        botaoSair.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        lateral.add(botaoSair);
        JPanel topo = new JPanel(new BorderLayout());
        painelTopo = topo;
        topo.setBackground(Color.WHITE);
        topo.setBorder(BorderFactory.createEmptyBorder(18, 26, 18, 26));
        tituloTopo = titulo("Visão geral  /  Projetos", 21);
        topo.add(tituloTopo, BorderLayout.WEST);
        JLabel usuario = new JLabel("◉  Usuário");
        usuario.setForeground(TEXTO);
        rotulosTema.add(usuario);
        botaoTema = new JButton("🌙 Tema escuro");
        botaoTema.setToolTipText("Alternar entre tema claro e escuro");
        botaoTema.putClientProperty("JButton.buttonType", "roundRect");
        JPanel acoesTopo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        acoesTopo.setOpaque(false);
        acoesTopo.add(botaoTema);
        acoesTopo.add(usuario);
        topo.add(acoesTopo, BorderLayout.EAST);
        JPanel formulario = cartao();
        formulario.add(titulo("▣  Cadastro de Projeto", 17), BorderLayout.NORTH);
        JPanel campos = new JPanel(new GridBagLayout());
        campos.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 5, 6, 5);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 0;
        g.gridx = 0; g.gridy = 0; campos.add(new JLabel("Nome:"), g);
        g.gridx = 1; g.weightx = 1; campos.add(campoNome, g);
        g.gridx = 0; g.gridy = 1; g.weightx = 0; campos.add(new JLabel("Descrição:"), g);
        g.gridx = 1; g.weightx = 1; campos.add(campoDescricao, g);
        g.gridx = 0; g.gridy = 2; g.weightx = 0; campos.add(new JLabel("Categoria:"), g);
        g.gridx = 1; g.weightx = 1; campos.add(comboCategoria, g);
        g.gridx = 0; g.gridy = 3; g.weightx = 0; campos.add(new JLabel("Status:"), g);
        g.gridx = 1; g.weightx = 1; campos.add(comboStatus, g);

        // Mensagens de erro logo abaixo dos respectivos campos
        erroNome = new JLabel(" ");
        erroDescricao = new JLabel(" ");
        erroStatus = new JLabel(" ");
        for (JLabel aviso : new JLabel[]{erroNome, erroDescricao, erroStatus}) {
            aviso.setForeground(new Color(183, 52, 62));
            rotulosErro.add(aviso);
            aviso.setFont(new Font("SansSerif", Font.PLAIN, 11));
        }

        g.gridx = 1; g.gridy = 4; g.weightx = 1;

        // Reorganiza os campos em linhas com avisos de validação
        campos.removeAll();
        adicionarCampo(campos, 0, "Nome do Projeto *", campoNome, erroNome);
        adicionarCampo(campos, 2, "Descrição", campoDescricao, erroDescricao);
        adicionarCampo(campos, 4, "Categoria:", comboCategoria, null);
        adicionarCampo(campos, 5, "Status:", comboStatus, erroStatus);
        formulario.add(campos, BorderLayout.NORTH);
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botoes.setOpaque(false);
        for (JButton b : new JButton[]{botaoCadastrar, botaoAlterar, botaoExcluir, botaoLimpar}) botoes.add(b);
        for (JButton b : new JButton[]{botaoCadastrar, botaoAlterar, botaoExcluir, botaoLimpar}) {
            b.putClientProperty("JButton.buttonType", "roundRect");
            b.setFont(new Font("SansSerif", Font.BOLD, 12));
            b.setMargin(new Insets(10, 13, 10, 13));
        }
        formulario.add(botoes, BorderLayout.SOUTH);
        JPanel listagem = cartao();
        JPanel tituloLista = new JPanel(new BorderLayout());
        tituloLista.setOpaque(false);
        tituloLista.add(titulo("▦  Lista de Projetos", 17), BorderLayout.WEST);
        tituloLista.add(botaoAtualizar, BorderLayout.EAST);

        // Filtros aplicados localmente aos dados recebidos da API
        campoBusca = new JTextField(20);
        campoBusca.putClientProperty("JTextField.placeholderText", "Pesquisar por nome, categoria ou status...");
        filtroCategoria = new JComboBox<>(new String[]{"Todas", "Web", "Software", "Mobile", "Outro"});
        filtroStatus = new JComboBox<>(new String[]{"Todos", "Planejado", "Em desenvolvimento", "Concluído"});
        JPanel filtros = new JPanel(new GridLayout(1, 3, 8, 0));
        filtros.setOpaque(false);
        filtros.add(campoBusca);
        filtros.add(filtroCategoria);
        filtros.add(filtroStatus);
        for (JComponent campo : new JComponent[]{campoNome, campoDescricao, campoBusca,
                comboCategoria, comboStatus, filtroCategoria, filtroStatus}) {
            campo.putClientProperty("JComponent.roundRect", true);
            campo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        }
        campoNome.putClientProperty("JTextField.placeholderText", "Digite o nome do projeto");
        campoDescricao.putClientProperty("JTextField.placeholderText", "Descreva o objetivo do projeto");
        JPanel cabecalhoLista = new JPanel(new BorderLayout(0, 12));
        cabecalhoLista.setOpaque(false);
        cabecalhoLista.add(tituloLista, BorderLayout.NORTH);
        cabecalhoLista.add(filtros, BorderLayout.SOUTH);
        listagem.add(cabecalhoLista, BorderLayout.NORTH);
        JScrollPane rolagemTabela = new JScrollPane(tabela);
        rolagemTabela.setBorder(BorderFactory.createEmptyBorder());
        rolagemTabela.getViewport().setBackground(Color.WHITE);
        listagem.add(rolagemTabela, BorderLayout.CENTER);
        botaoAtualizar.putClientProperty("JButton.buttonType", "roundRect");
        botaoAnterior = new JButton("◀ Anterior");
        botaoProxima = new JButton("Próxima ▶");
        indicadorPagina = new JLabel("Página 1 de 1", SwingConstants.CENTER);
        JPanel navegacao = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        navegacao.setOpaque(false);
        navegacao.add(botaoAnterior);
        navegacao.add(indicadorPagina);
        navegacao.add(botaoProxima);
        listagem.add(navegacao, BorderLayout.SOUTH);
        mensagem = new JLabel(" ");
        mensagem.setForeground(TEXTO);
        rotulosTema.add(mensagem);
        JPanel conteudo = new JPanel(new BorderLayout(0, 16));
        painelConteudo = conteudo;
        conteudo.setBackground(FUNDO);
        conteudo.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
        JPanel estatisticas = new JPanel(new GridLayout(1, 4, 16, 0));
        estatisticas.setOpaque(false);
        estatisticas.add(cardEstatistico("📁  Total de projetos", AZUL_CLARO, l -> totalValor = l));
        estatisticas.add(cardEstatistico("✔  Concluídos", new Color(30, 135, 80), l -> concluidosValor = l));
        estatisticas.add(cardEstatistico("◷  Em andamento", new Color(193, 126, 24), l -> andamentoValor = l));
        estatisticas.add(cardEstatistico("▥  Planejados", new Color(111, 88, 171), l -> planejadosValor = l));
        // Layout de referência: indicadores no topo, formulário e lista lado a lado.
        JPanel areaTrabalho = new JPanel(new GridBagLayout());
        areaTrabalho.setOpaque(false);
        GridBagConstraints coluna = new GridBagConstraints();
        coluna.gridy = 0;
        coluna.fill = GridBagConstraints.BOTH;
        coluna.weighty = 1.0;
        coluna.insets = new Insets(0, 0, 0, 16);
        coluna.gridx = 0;
        coluna.weightx = 0.42;
        areaTrabalho.add(formulario, coluna);
        coluna.gridx = 1;
        coluna.weightx = 0.58;
        coluna.insets = new Insets(0, 0, 0, 0);
        areaTrabalho.add(listagem, coluna);
        conteudo.add(estatisticas, BorderLayout.NORTH);
        conteudo.add(areaTrabalho, BorderLayout.CENTER);
        conteudo.add(mensagem, BorderLayout.SOUTH);
        JPanel principal = new JPanel(new BorderLayout());
        principal.add(topo, BorderLayout.NORTH);
        navegacaoTelas = new CardLayout();
        painelTelas = new JPanel(navegacaoTelas);
        painelTelas.add(conteudo, "Projetos");
        painelDashboard = criarDashboard();
        painelTelas.add(painelDashboard, "Dashboard");
        painelRelatorios = criarRelatorios();
        painelTelas.add(painelRelatorios, "Relatórios");
        painelCategorias = criarCategorias();
        painelTelas.add(painelCategorias, "Categorias");
        painelConfiguracoes = criarConfiguracoes();
        painelTelas.add(painelConfiguracoes, "Configurações");
        principal.add(painelTelas, BorderLayout.CENTER);
        setContentPane(new JPanel(new BorderLayout()));
        add(lateral, BorderLayout.WEST);
        add(principal, BorderLayout.CENTER);
    }

    private void mostrarTela(String tela) {
        navegacaoTelas.show(painelTelas, tela);
        tituloTopo.setText(tela.equals("Dashboard") ? "Dashboard de Projetos"
            : tela.equals("Relatórios") ? "Relatórios de Projetos" : tela.equals("Categorias") ? "Categorias de Projetos" : tela.equals("Configurações") ? "Configurações do Sistema" : "Sistema de Projetos");
        Color inativo = temaEscuro ? new Color(15, 24, 39) : AZUL;
        menuDashboard.setBackground(tela.equals("Dashboard") ? AZUL_CLARO : inativo);
        menuProjetos.setBackground(tela.equals("Projetos") ? AZUL_CLARO : inativo);
        menuRelatorios.setBackground(tela.equals("Relatórios") ? AZUL_CLARO : inativo);
        menuCategorias.setBackground(tela.equals("Categorias") ? AZUL_CLARO : inativo);
        menuConfiguracoes.setBackground(tela.equals("Configurações") ? AZUL_CLARO : inativo);
        if (tela.equals("Dashboard")) atualizarDashboard();
        if (tela.equals("Relatórios")) atualizarRelatorio();
        if (tela.equals("Categorias")) carregarCategorias();
        if (tela.equals("Configurações")) atualizarConfiguracoes();
    }

    private JPanel criarConfiguracoes() {
        JPanel pagina = new JPanel(new BorderLayout(0, 16));
        pagina.setBackground(FUNDO);
        pagina.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));
        JPanel corpo = new JPanel();
        corpo.setOpaque(false);
        corpo.setLayout(new BoxLayout(corpo, BoxLayout.Y_AXIS));
        JPanel aparencia = cartao();
        aparencia.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
        aparencia.add(titulo("Preferências de aparência", 19), BorderLayout.NORTH);
        JPanel controles = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        controles.setOpaque(false);
        JLabel rotulo = titulo("Tema do sistema:", 13);
        escolhaTema = new JComboBox<>(new String[]{"Claro", "Escuro"});
        botaoSalvarPreferencias = new JButton("Salvar preferências");
        controles.add(rotulo);
        controles.add(escolhaTema);
        controles.add(botaoSalvarPreferencias);
        aparencia.add(controles, BorderLayout.CENTER);
        situacaoPreferencias = titulo("", 12);
        aparencia.add(situacaoPreferencias, BorderLayout.SOUTH);
        corpo.add(aparencia);
        corpo.add(Box.createVerticalStrut(16));
        JPanel restauracao = cartao();
        restauracao.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        restauracao.add(titulo("Restaurar configurações", 18), BorderLayout.NORTH);
        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        acoes.setOpaque(false);
        botaoRestaurarPadrao = new JButton("Restaurar padrão (tema claro)");
        acoes.add(botaoRestaurarPadrao);
        restauracao.add(acoes, BorderLayout.CENTER);
        corpo.add(restauracao);
        corpo.add(Box.createVerticalStrut(16));
        JPanel informacoes = cartao();
        informacoes.setMaximumSize(new Dimension(Integer.MAX_VALUE, 170));
        informacoes.add(titulo("Sobre o sistema", 18), BorderLayout.NORTH);
        JPanel detalhes = new JPanel(new GridLayout(3, 1, 0, 8));
        detalhes.setOpaque(false);
        detalhes.add(titulo("Sistema de Projetos | Versão 1.0", 13));
        detalhes.add(titulo("Tecnologias: Java, Swing, FlatLaf e API REST", 13));
        detalhes.add(titulo("Recursos: projetos, categorias, dashboard e relatórios", 13));
        informacoes.add(detalhes, BorderLayout.CENTER);
        corpo.add(informacoes);
        pagina.add(new JScrollPane(corpo, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
        return pagina;
    }

    private void atualizarConfiguracoes() {
        escolhaTema.setSelectedItem(temaEscuro ? "Escuro" : "Claro");
        situacaoPreferencias.setText("Tema atual: " + (temaEscuro ? "Escuro" : "Claro")
            + " | Preferência salva no computador.");
    }

    private void salvarPreferencias() {
        boolean escuro = "Escuro".equals(escolhaTema.getSelectedItem());
        try {
            PREFERENCIAS.putBoolean(CHAVE_TEMA, escuro);
            PREFERENCIAS.flush();
            aplicarTema(escuro);
            atualizarConfiguracoes();
            JOptionPane.showMessageDialog(this, "Preferências salvas com sucesso!");
        } catch (java.util.prefs.BackingStoreException | SecurityException ex) {
            JOptionPane.showMessageDialog(this, "Não foi possível salvar as preferências: " + ex.getMessage(),
                "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void restaurarPadrao() {
        int resposta = JOptionPane.showConfirmDialog(this,
            "Restaurar o tema claro como padrão?\nOs projetos e categorias não serão alterados.",
            "Confirmar restauração", JOptionPane.YES_NO_OPTION);
        if (resposta != JOptionPane.YES_OPTION) return;
        escolhaTema.setSelectedItem("Claro");
        salvarPreferencias();
    }

    private JPanel criarCategorias() {
        JPanel pagina = new JPanel(new BorderLayout(0, 14));
        pagina.setBackground(FUNDO);
        pagina.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));
        JPanel cabecalho = cartao();
        cabecalho.add(titulo("Gerenciamento de categorias", 20), BorderLayout.NORTH);
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        botoes.setOpaque(false);
        botaoNovaCategoria = new JButton("+ Nova categoria");
        botaoEditarCategoria = new JButton("Editar selecionada");
        botaoExcluirCategoria = new JButton("Excluir selecionada");
        botaoRecarregarCategorias = new JButton("Atualizar");
        for (JButton b : new JButton[]{botaoNovaCategoria, botaoEditarCategoria, botaoExcluirCategoria, botaoRecarregarCategorias}) botoes.add(b);
        cabecalho.add(botoes, BorderLayout.CENTER);
        pagina.add(cabecalho, BorderLayout.NORTH);
        JPanel lista = cartao();
        modeloCategorias = new DefaultTableModel(new String[]{"Categoria", "Projetos vinculados"}, 0) {
            @Override public boolean isCellEditable(int linha, int coluna) { return false; }
        };
        tabelaCategorias = new JTable(modeloCategorias);
        tabelaCategorias.setRowHeight(34);
        tabelaCategorias.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabelaCategorias.setAutoCreateRowSorter(true);
        lista.add(new JScrollPane(tabelaCategorias), BorderLayout.CENTER);
        pagina.add(lista, BorderLayout.CENTER);
        return pagina;
    }

    private void carregarCategorias() {
        executar("Carregando categorias...", () -> apiCategorias.listar(), lista -> {
            categoriasCarregadas = new ArrayList<>(lista);
            atualizarCombosCategorias();
            atualizarTabelaCategorias();
            atualizarDashboard();
        });
    }

    private void atualizarCombosCategorias() {
        String selecionada = Objects.toString(comboCategoria.getSelectedItem(), "");
        String filtro = Objects.toString(filtroCategoria.getSelectedItem(), "Todas");
        String rel = Objects.toString(relCategoria.getSelectedItem(), "Todas");
        comboCategoria.removeAllItems();
        filtroCategoria.removeAllItems(); filtroCategoria.addItem("Todas");
        relCategoria.removeAllItems(); relCategoria.addItem("Todas");
        for (String nome : categoriasCarregadas) {
            comboCategoria.addItem(nome);
            filtroCategoria.addItem(nome);
            relCategoria.addItem(nome);
        }
        if (categoriasCarregadas.contains(selecionada)) comboCategoria.setSelectedItem(selecionada);
        if (categoriasCarregadas.contains(filtro)) filtroCategoria.setSelectedItem(filtro);
        if (categoriasCarregadas.contains(rel)) relCategoria.setSelectedItem(rel);
        reiniciarPaginacao();
        atualizarRelatorio();
    }

    private void atualizarTabelaCategorias() {
        if (modeloCategorias == null) return;
        modeloCategorias.setRowCount(0);
        for (String categoria : categoriasCarregadas) {
            int quantidade = 0;
            for (Projeto projeto : projetosCarregados)
                if (categoria.equalsIgnoreCase(Objects.toString(projeto.getCategoria(), ""))) quantidade++;
            modeloCategorias.addRow(new Object[]{categoria, quantidade});
        }
    }

    private String categoriaSelecionada() {
        int linha = tabelaCategorias.getSelectedRow();
        if (linha < 0) { JOptionPane.showMessageDialog(this, "Selecione uma categoria na tabela."); return null; }
        return Objects.toString(modeloCategorias.getValueAt(tabelaCategorias.convertRowIndexToModel(linha), 0), "");
    }

    private void editarCategoria(boolean edicao) {
        String antiga = edicao ? categoriaSelecionada() : null;
        if (edicao && antiga == null) return;
        String nome = JOptionPane.showInputDialog(this, edicao ? "Novo nome da categoria:" : "Nome da nova categoria:", edicao ? antiga : "");
        if (nome == null) return;
        nome = nome.trim();
        if (nome.isEmpty() || nome.length() > 60 || nome.contains("\n") || nome.contains("\r")) {
            JOptionPane.showMessageDialog(this, "Informe um nome válido (até 60 caracteres)."); return;
        }
        final String novo = nome, anterior = antiga;
        executar(edicao ? "Alterando categoria..." : "Cadastrando categoria...",
            () -> { if (edicao) apiCategorias.renomear(anterior, novo); else apiCategorias.cadastrar(novo); return true; },
            ok -> carregarTabela());
    }

    private void excluirCategoria() {
        String nome = categoriaSelecionada();
        if (nome == null) return;
        if (JOptionPane.showConfirmDialog(this, "Excluir a categoria '" + nome + "'?\nCategorias em uso não podem ser excluídas.",
                "Confirmar exclusão", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        executar("Excluindo categoria...", () -> { apiCategorias.excluir(nome); return true; },
            ok -> carregarCategorias());
    }

    private JPanel criarRelatorios() {
        JPanel pagina = new JPanel(new BorderLayout(0, 14));
        pagina.setBackground(FUNDO);
        pagina.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));
        JPanel cabecalho = cartao();
        cabecalho.add(titulo("Relatório de projetos", 20), BorderLayout.NORTH);
        JPanel controles = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        controles.setOpaque(false);
        relCategoria = new JComboBox<>(new String[]{"Todas", "Web", "Software", "Mobile", "Outro"});
        relStatus = new JComboBox<>(new String[]{"Todos", "Planejado", "Em desenvolvimento", "Concluído"});
        botaoCsv = new JButton("Exportar CSV");
        botaoPdf = new JButton("Exportar PDF");
        controles.add(new JLabel("Categoria:")); controles.add(relCategoria);
        controles.add(new JLabel("Status:")); controles.add(relStatus);
        controles.add(botaoCsv); controles.add(botaoPdf);
        cabecalho.add(controles, BorderLayout.CENTER);
        pagina.add(cabecalho, BorderLayout.NORTH);
        JPanel lista = cartao();
        modeloRelatorio = new DefaultTableModel(new String[]{"ID", "Nome", "Descrição", "Categoria", "Status"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tabelaRelatorio = new JTable(modeloRelatorio);
        tabelaRelatorio.setRowHeight(30);
        tabelaRelatorio.setAutoCreateRowSorter(true);
        lista.add(new JScrollPane(tabelaRelatorio), BorderLayout.CENTER);
        pagina.add(lista, BorderLayout.CENTER);
        JPanel rodape = cartao();
        resumoRelatorio = titulo("Nenhum projeto encontrado.", 13);
        rodape.add(resumoRelatorio, BorderLayout.CENTER);
        pagina.add(rodape, BorderLayout.SOUTH);
        return pagina;
    }

    private List<Projeto> projetosDoRelatorio() {
        List<Projeto> filtrados = new ArrayList<>();
        String categoria = Objects.toString(relCategoria.getSelectedItem(), "Todas");
        String status = Objects.toString(relStatus.getSelectedItem(), "Todos");
        for (Projeto p : projetosCarregados) {
            if ((categoria.equals("Todas") || categoria.equalsIgnoreCase(Objects.toString(p.getCategoria(), "")))
                && (status.equals("Todos") || status.equalsIgnoreCase(Objects.toString(p.getStatus(), ""))))
                filtrados.add(p);
        }
        return filtrados;
    }

    private void atualizarRelatorio() {
        if (modeloRelatorio == null) return;
        List<Projeto> lista = projetosDoRelatorio();
        modeloRelatorio.setRowCount(0);
        int planejados = 0, andamento = 0, concluidos = 0;
        for (Projeto p : lista) {
            modeloRelatorio.addRow(new Object[]{p.getId(), p.getNome(), p.getDescricao(), p.getCategoria(), p.getStatus()});
            if ("Planejado".equalsIgnoreCase(p.getStatus())) planejados++;
            else if ("Em desenvolvimento".equalsIgnoreCase(p.getStatus())) andamento++;
            else if ("Concluído".equalsIgnoreCase(p.getStatus())) concluidos++;
        }
        resumoRelatorio.setText("Total: " + lista.size() + "   |   Planejados: " + planejados
            + "   |   Em desenvolvimento: " + andamento + "   |   Concluídos: " + concluidos);
    }

    private void exportarRelatorio(boolean pdf) {
        List<Projeto> lista = projetosDoRelatorio();
        JFileChooser seletor = new JFileChooser();
        seletor.setSelectedFile(new File(pdf ? "relatorio_projetos.pdf" : "relatorio_projetos.csv"));
        if (seletor.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File arquivo = seletor.getSelectedFile();
        String extensao = pdf ? ".pdf" : ".csv";
        if (!arquivo.getName().toLowerCase(java.util.Locale.ROOT).endsWith(extensao))
            arquivo = new File(arquivo.getAbsolutePath() + extensao);
        if (arquivo.exists() && JOptionPane.showConfirmDialog(this, "Substituir o arquivo existente?",
            "Confirmar", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            if (pdf) gerarPdf(arquivo, lista);
            else gerarCsv(arquivo, lista);
            JOptionPane.showMessageDialog(this, "Relatório exportado com sucesso!\n" + arquivo.getAbsolutePath());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao exportar: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String csvCampo(Object valor) {
        String texto = Objects.toString(valor, "");
        return "\"" + texto.replace("\"", "\"\"") + "\"";
    }

    private void gerarCsv(File arquivo, List<Projeto> lista) throws IOException {
        try (java.io.Writer out = new java.io.OutputStreamWriter(new FileOutputStream(arquivo), StandardCharsets.UTF_8)) {
            out.write('\ufeff'); // BOM: Excel reconhece UTF-8
            out.write("ID;Nome;Descrição;Categoria;Status\r\n");
            for (Projeto p : lista) {
                out.write(p.getId() + ";" + csvCampo(p.getNome()) + ";" + csvCampo(p.getDescricao())
                    + ";" + csvCampo(p.getCategoria()) + ";" + csvCampo(p.getStatus()) + "\r\n");
            }
        }
    }

    // Gerador PDF simples e independente de bibliotecas adicionais.
    // Usa Helvetica/WinAnsi e quebra linhas para evitar corte horizontal.
    private void gerarPdf(File arquivo, List<Projeto> lista) throws IOException {
        List<List<String>> paginas = new ArrayList<>();
        List<String> atual = new ArrayList<>();
        paginas.add(atual);
        String data = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        atual.add("SISTEMA DE PROJETOS - RELATORIO");
        atual.add("Gerado em: " + data);
        atual.add("Categoria: " + relCategoria.getSelectedItem() + "  |  Status: " + relStatus.getSelectedItem());
        atual.add("Total de projetos: " + lista.size());
        int[] contagem = new int[3];
        for (Projeto p : lista) {
            if ("Planejado".equalsIgnoreCase(p.getStatus())) contagem[0]++;
            else if ("Em desenvolvimento".equalsIgnoreCase(p.getStatus())) contagem[1]++;
            else if ("Concluído".equalsIgnoreCase(p.getStatus())) contagem[2]++;
        }
        atual.add("Planejados: " + contagem[0] + " | Em desenvolvimento: " + contagem[1]
            + " | Concluidos: " + contagem[2]);
        atual.add("---------------------------------------------------------------");
        for (Projeto p : lista) {
            List<String> bloco = new ArrayList<>();
            bloco.add("ID: " + p.getId() + "  |  " + Objects.toString(p.getNome(), ""));
            bloco.add("Categoria: " + p.getCategoria() + "  |  Status: " + p.getStatus());
            String descricao = Objects.toString(p.getDescricao(), "").replace('\n', ' ').replace('\r', ' ');
            while (!descricao.isEmpty()) {
                int limite = Math.min(95, descricao.length());
                int corte = limite;
                if (limite < descricao.length()) {
                    int espaco = descricao.lastIndexOf(' ', limite);
                    if (espaco > 35) corte = espaco;
                }
                bloco.add("Descricao: " + descricao.substring(0, corte).trim());
                descricao = descricao.substring(corte).trim();
            }
            bloco.add(" ");
            if (atual.size() + bloco.size() > 55) {
                atual = new ArrayList<>(); paginas.add(atual);
                atual.add("RELATORIO DE PROJETOS - CONTINUACAO");
                atual.add("---------------------------------------------------------------");
            }
            atual.addAll(bloco);
        }
        if (lista.isEmpty()) atual.add("Nenhum projeto encontrado para os filtros selecionados.");
        escreverPdf(arquivo, paginas);
    }

    private String pdfTexto(String texto) {
        // Helvetica WinAnsi suporta os caracteres ocidentais comuns.
        return texto.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
            .replace("\t", " ");
    }

    private void escreverPdf(File arquivo, List<List<String>> paginas) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();
        offsets.add(0);
        bytes.write("%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII));
        int numeroPaginas = paginas.size();
        int fonteId = 3 + numeroPaginas * 2;
        int totalObjetos = fonteId;
        List<byte[]> objetos = new ArrayList<>();
        objetos.add("<< /Type /Catalog /Pages 2 0 R >>".getBytes(StandardCharsets.US_ASCII));
        StringBuilder filhos = new StringBuilder();
        for (int i = 0; i < numeroPaginas; i++) filhos.append(3 + i * 2).append(" 0 R ");
        objetos.add(("<< /Type /Pages /Kids [" + filhos + "] /Count " + numeroPaginas + " >>").getBytes(StandardCharsets.US_ASCII));
        for (int i = 0; i < numeroPaginas; i++) {
            int conteudoId = 4 + i * 2;
            objetos.add(("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 "
                + fonteId + " 0 R >> >> /Contents " + conteudoId + " 0 R >>").getBytes(StandardCharsets.US_ASCII));
            StringBuilder conteudo = new StringBuilder("BT /F1 10 Tf 45 800 Td 14 TL\n");
            for (String linha : paginas.get(i)) conteudo.append("(").append(pdfTexto(linha)).append(") Tj T*\n");
            conteudo.append("ET\n");
            byte[] stream = conteudo.toString().getBytes(java.nio.charset.Charset.forName("windows-1252"));
            ByteArrayOutputStream obj = new ByteArrayOutputStream();
            obj.write(("<< /Length " + stream.length + " >>\nstream\n").getBytes(StandardCharsets.US_ASCII));
            obj.write(stream);
            obj.write("\nendstream".getBytes(StandardCharsets.US_ASCII));
            objetos.add(obj.toByteArray());
        }
        objetos.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>".getBytes(StandardCharsets.US_ASCII));
        for (int i = 0; i < totalObjetos; i++) {
            offsets.add(bytes.size());
            bytes.write(((i + 1) + " 0 obj\n").getBytes(StandardCharsets.US_ASCII));
            bytes.write(objetos.get(i));
            bytes.write("\nendobj\n".getBytes(StandardCharsets.US_ASCII));
        }
        int inicioXref = bytes.size();
        bytes.write(("xref\n0 " + (totalObjetos + 1) + "\n0000000000 65535 f \n").getBytes(StandardCharsets.US_ASCII));
        for (int i = 1; i <= totalObjetos; i++)
            bytes.write(String.format(java.util.Locale.ROOT, "%010d 00000 n \n", offsets.get(i)).getBytes(StandardCharsets.US_ASCII));
        bytes.write(("trailer\n<< /Size " + (totalObjetos + 1) + " /Root 1 0 R >>\nstartxref\n"
            + inicioXref + "\n%%EOF\n").getBytes(StandardCharsets.US_ASCII));
        Files.write(arquivo.toPath(), bytes.toByteArray());
    }

    private JPanel criarDashboard() {
        JPanel dashboard = new JPanel(new BorderLayout(0, 16));
        dashboard.setBackground(FUNDO);
        dashboard.setBorder(BorderFactory.createEmptyBorder(20, 22, 20, 22));
        dashboard.add(titulo("Visão geral dos projetos", 22), BorderLayout.NORTH);
        JPanel graficos = new JPanel(new GridLayout(1, 2, 16, 0));
        graficos.setOpaque(false);
        graficoStatus = new GraficoBarras("Projetos por status",
            new String[]{"Planejado", "Em desenvolvimento", "Concluído"},
            new Color[]{new Color(79, 132, 216), new Color(217, 155, 57), new Color(49, 159, 105)});
        graficoCategorias = new GraficoBarras("Projetos por categoria",
            new String[]{"Web", "Software", "Mobile", "Outro"},
            new Color[]{new Color(64, 128, 207), new Color(112, 91, 196),
                        new Color(46, 158, 169), new Color(198, 118, 80)});
        graficos.add(graficoStatus);
        graficos.add(graficoCategorias);
        dashboard.add(graficos, BorderLayout.CENTER);
        JPanel rodape = cartao();
        resumoDashboard = titulo("Nenhum projeto cadastrado.", 14);
        rodape.add(resumoDashboard, BorderLayout.CENTER);
        dashboard.add(rodape, BorderLayout.SOUTH);
        return dashboard;
    }

    private void atualizarDashboard() {
        if (graficoStatus == null) return;
        int[] status = new int[3];
        int[] categorias = new int[categoriasCarregadas.size()];
        for (Projeto p : projetosCarregados) {
            String st = Objects.toString(p.getStatus(), "").trim();
            if (st.equalsIgnoreCase("Planejado")) status[0]++;
            else if (st.equalsIgnoreCase("Em desenvolvimento")) status[1]++;
            else if (st.equalsIgnoreCase("Concluído")) status[2]++;
            String cat = Objects.toString(p.getCategoria(), "").trim();
            for (int i = 0; i < categoriasCarregadas.size(); i++) {
                if (cat.equalsIgnoreCase(categoriasCarregadas.get(i))) { categorias[i]++; break; }
            }
        }
        graficoStatus.definirValores(status);
        graficoCategorias.definirCategorias(categoriasCarregadas.toArray(new String[0]), categorias);
        resumoDashboard.setText("Total: " + projetosCarregados.size()
            + "   |   Planejados: " + status[0]
            + "   |   Em desenvolvimento: " + status[1]
            + "   |   Concluídos: " + status[2]);
    }

    private class GraficoBarras extends JPanel {
        private final String tituloGrafico;
        private String[] nomes;
        private Color[] cores;
        private int[] valores;
        GraficoBarras(String tituloGrafico, String[] nomes, Color[] cores) {
            this.tituloGrafico = tituloGrafico;
            this.nomes = nomes;
            this.cores = cores;
            this.valores = new int[nomes.length];
            setPreferredSize(new Dimension(300, 300));
            setBorder(BorderFactory.createEmptyBorder(20, 18, 20, 18));
        }

        void definirCategorias(String[] novosNomes, int[] novosValores) {
            nomes = novosNomes.clone();
            Color[] paleta = {new Color(64,128,207), new Color(112,91,196), new Color(46,158,169), new Color(198,118,80), new Color(80,150,95), new Color(190,110,160)};
            cores = new Color[nomes.length];
            for (int i = 0; i < cores.length; i++) cores[i] = paleta[i % paleta.length];
            definirValores(novosValores);
        }
        void definirValores(int[] novosValores) {
            valores = novosValores.clone();
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color fundo = temaEscuro ? new Color(35, 44, 59) : Color.WHITE;
                Color texto = temaEscuro ? new Color(225, 232, 242) : TEXTO;
                g2.setColor(fundo);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(texto);
                g2.setFont(new Font("SansSerif", Font.BOLD, 16));
                g2.drawString(tituloGrafico, 18, 30);
                int max = 1;
                for (int v : valores) max = Math.max(max, v);
                int inicioY = 64;
                int larguraDisponivel = Math.max(40, getWidth() - 180);
                int alturaLinha = Math.max(42, (getHeight() - inicioY - 20) / Math.max(1, nomes.length));
                for (int i = 0; i < nomes.length; i++) {
                    int y = inicioY + i * alturaLinha;
                    g2.setColor(texto);
                    g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
                    String nome = nomes[i];
                    if (nome.equals("Em desenvolvimento")) nome = "Em desenvolv.";
                    g2.drawString(nome, 18, y + 16);
                    int largura = (int) Math.round((double) valores[i] / max * larguraDisponivel);
                    g2.setColor(cores[i]);
                    g2.fillRoundRect(145, y, Math.max(0, largura), 22, 8, 8);
                    g2.setColor(texto);
                    g2.drawString(String.valueOf(valores[i]), 151 + largura, y + 16);
                }
            } finally { g2.dispose(); }
        }
    }

    private Color corLinha(int linha) {
        if (temaEscuro) return linha % 2 == 0 ? new Color(36, 45, 59) : new Color(42, 52, 67);
        return linha % 2 == 0 ? Color.WHITE : new Color(247, 250, 254);
    }

    private void alternarTema() {
        boolean novoTema = !temaEscuro;
        try {
            PREFERENCIAS.putBoolean(CHAVE_TEMA, novoTema);
            PREFERENCIAS.flush();
            aplicarTema(novoTema);
        } catch (java.util.prefs.BackingStoreException | SecurityException ex) {
            JOptionPane.showMessageDialog(this, "Não foi possível salvar o tema: " + ex.getMessage(),
                "Aviso", JOptionPane.WARNING_MESSAGE);
            aplicarTema(novoTema);
        }
    }

    private void aplicarTema(boolean escuro) {
        temaEscuro = escuro;
        if (temaEscuro) FlatDarkLaf.setup();
        else FlatLightLaf.setup();
        FlatLaf.updateUI();
        Color fundoJanela = temaEscuro ? new Color(25, 32, 44) : FUNDO;
        Color fundoCartao = temaEscuro ? new Color(35, 44, 59) : Color.WHITE;
        Color corTexto = temaEscuro ? new Color(225, 232, 242) : TEXTO;
        Color borda = temaEscuro ? new Color(65, 78, 97) : new Color(222, 230, 240);
        painelConteudo.setBackground(fundoJanela);
        painelDashboard.setBackground(fundoJanela);
        painelRelatorios.setBackground(fundoJanela);
        painelCategorias.setBackground(fundoJanela);
        painelConfiguracoes.setBackground(fundoJanela);
        painelTopo.setBackground(fundoCartao);
        painelLateral.setBackground(temaEscuro ? new Color(15, 24, 39) : AZUL);
        for (JPanel painel : cartoesTema) {
            painel.setBackground(fundoCartao);
            if (painel.getBorder() != null) {

                // Preserva o espaçamento original dos cartões.
                if (painel.getLayout() instanceof BorderLayout) {
                    boolean estatistico = valoresEstatisticos.stream().anyMatch(painel::isAncestorOf);
                    painel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(borda),
                        estatistico ? BorderFactory.createEmptyBorder(14, 16, 14, 16)
                                   : BorderFactory.createEmptyBorder(18, 20, 18, 20)));
                }

            }

        }

        for (JLabel rotulo : rotulosTema) rotulo.setForeground(corTexto);
        for (JLabel erro : rotulosErro)
            erro.setForeground(temaEscuro ? new Color(255, 150, 158) : new Color(183, 52, 62));

        // Cores de destaque legíveis em ambos os temas.
        Color[] coresValores = temaEscuro
            ? new Color[]{new Color(130, 189, 255), new Color(112, 220, 158),
                          new Color(255, 200, 114), new Color(197, 172, 255)}

            : new Color[]{AZUL_CLARO, new Color(30, 135, 80),
                          new Color(193, 126, 24), new Color(111, 88, 171)};
        for (int i = 0; i < valoresEstatisticos.size(); i++)
            valoresEstatisticos.get(i).setForeground(coresValores[i]);
        botaoTema.setText(temaEscuro ? "☀ Tema claro" : "🌙 Tema escuro");
        botaoCadastrar.setBackground(temaEscuro ? new Color(46, 105, 184) : AZUL_CLARO);
        botaoCadastrar.setForeground(Color.WHITE);
        botaoExcluir.setBackground(temaEscuro ? new Color(166, 57, 68) : new Color(183, 52, 62));
        botaoExcluir.setForeground(Color.WHITE);
        personalizarTabela();
        graficoStatus.repaint();
        graficoCategorias.repaint();
        atualizarConfiguracoes();
        revalidate();
        repaint();
    }

    private void criarEventos() {
        botaoTema.addActionListener(e -> alternarTema());
        menuDashboard.addActionListener(e -> mostrarTela("Dashboard"));
        menuProjetos.addActionListener(e -> mostrarTela("Projetos"));
        menuRelatorios.addActionListener(e -> mostrarTela("Relatórios"));
        menuCategorias.addActionListener(e -> mostrarTela("Categorias"));
        menuConfiguracoes.addActionListener(e -> mostrarTela("Configurações"));
        botaoSair.addActionListener(e -> {
            int resposta = JOptionPane.showConfirmDialog(this,
                "Deseja sair da conta de administrador?", "Encerrar sessão",
                JOptionPane.YES_NO_OPTION);
            if (resposta == JOptionPane.YES_OPTION) {
                dispose();
                new LoginAdministrador().setVisible(true);
            }
        });
        botaoSalvarPreferencias.addActionListener(e -> salvarPreferencias());
        botaoRestaurarPadrao.addActionListener(e -> restaurarPadrao());
        botaoNovaCategoria.addActionListener(e -> editarCategoria(false));
        botaoEditarCategoria.addActionListener(e -> editarCategoria(true));
        botaoExcluirCategoria.addActionListener(e -> excluirCategoria());
        botaoRecarregarCategorias.addActionListener(e -> carregarCategorias());
        relCategoria.addActionListener(e -> atualizarRelatorio());
        relStatus.addActionListener(e -> atualizarRelatorio());
        botaoCsv.addActionListener(e -> exportarRelatorio(false));
        botaoPdf.addActionListener(e -> exportarRelatorio(true));
        botaoAtualizar.addActionListener(e -> carregarTabela());
        botaoAnterior.addActionListener(e -> {
            if (paginaAtual > 0) {
                paginaAtual--;
                aplicarFiltros();
            }

        });
        botaoProxima.addActionListener(e -> {
            if ((paginaAtual + 1) * TAMANHO_PAGINA < totalFiltrados) {
                paginaAtual++;
                aplicarFiltros();
            }

        });
        DocumentListener validarTexto = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { validarSeAtivo(); }

            public void removeUpdate(DocumentEvent e) { validarSeAtivo(); }

            public void changedUpdate(DocumentEvent e) { validarSeAtivo(); }

        };
        campoNome.getDocument().addDocumentListener(validarTexto);
        campoDescricao.getDocument().addDocumentListener(validarTexto);
        comboStatus.addActionListener(e -> validarSeAtivo());
        campoBusca.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { reiniciarPaginacao(); }

            public void removeUpdate(DocumentEvent e) { reiniciarPaginacao(); }

            public void changedUpdate(DocumentEvent e) { reiniciarPaginacao(); }

        });
        filtroCategoria.addActionListener(e -> reiniciarPaginacao());
        filtroStatus.addActionListener(e -> reiniciarPaginacao());
        botaoLimpar.addActionListener(e -> limparFormulario());
        botaoCadastrar.addActionListener(e -> cadastrar());
        botaoAlterar.addActionListener(e -> alterar());
        botaoExcluir.addActionListener(e -> excluir());
        tabela.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) selecionarProjeto();
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
        validacaoAtivada = false;
        limparErros();
    }

    private void carregarTabela() {
        executar("Carregando projetos...", () -> api.listar(), projetos -> {
            atualizarTabela(projetos);
            aplicarFiltros();
            SwingUtilities.invokeLater(() -> carregarCategorias());
        });
    }

    private void reiniciarPaginacao() {
        paginaAtual = 0;
        aplicarFiltros();
    }

    private void aplicarFiltros() {
        String nome = campoBusca.getText().trim().toLowerCase(java.util.Locale.ROOT);
        String categoria = Objects.toString(filtroCategoria.getSelectedItem(), "Todas");
        String status = Objects.toString(filtroStatus.getSelectedItem(), "Todos");

        // A ordem original dos dados determina as páginas; a ordenação por

        // clique no cabeçalho continua disponível dentro da página exibida.
        java.util.List<Integer> linhasEncontradas = new ArrayList<>();
        for (int i = 0; i < modelo.getRowCount(); i++) {
            String nomeLinha = Objects.toString(modelo.getValueAt(i, 1), "")
                .toLowerCase(java.util.Locale.ROOT);
            String categoriaLinha = Objects.toString(modelo.getValueAt(i, 3), "");
            String statusLinha = Objects.toString(modelo.getValueAt(i, 4), "");
            if (nomeLinha.contains(nome)
                && (categoria.equals("Todas") || categoriaLinha.equalsIgnoreCase(categoria))
                && (status.equals("Todos") || statusLinha.equalsIgnoreCase(status))) {
                linhasEncontradas.add(i);
            }

        }

        totalFiltrados = linhasEncontradas.size();
        int totalPaginas = Math.max(1, (totalFiltrados + TAMANHO_PAGINA - 1) / TAMANHO_PAGINA);
        paginaAtual = Math.min(paginaAtual, totalPaginas - 1);
        int inicio = paginaAtual * TAMANHO_PAGINA;
        int fim = Math.min(inicio + TAMANHO_PAGINA, totalFiltrados);
        java.util.Set<Integer> linhasDaPagina = new java.util.HashSet<>(
            linhasEncontradas.subList(inicio, fim));
        ordenador.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {

            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                return linhasDaPagina.contains(entry.getIdentifier());
            }

        });
        indicadorPagina.setText("Página " + (paginaAtual + 1) + " de " + totalPaginas);
        botaoAnterior.setEnabled(paginaAtual > 0);
        botaoProxima.setEnabled(paginaAtual + 1 < totalPaginas);
        String intervalo = totalFiltrados == 0 ? "0" : (inicio + 1) + "–" + fim;
        mensagem.setText("Exibindo " + intervalo + " de " + totalFiltrados
            + " projetos encontrados (total: " + projetosCarregados.size() + ")");
    }

    private void atualizarTabela(List<Projeto> projetos) {
        limparFormulario();
        modelo.setRowCount(0);
        projetosCarregados = new ArrayList<>(projetos);
        paginaAtual = 0;
        atualizarEstatisticas(projetos);
        atualizarDashboard();
        atualizarRelatorio();
        atualizarTabelaCategorias();
        for (Projeto p : projetos) {
            modelo.addRow(new Object[]{p.getId(), p.getNome(), p.getDescricao(), p.getCategoria(), p.getStatus()});
        }
        atualizarTabelaCategorias();
    }

    private <T> void executar(String texto, Callable<T> operacao, Consumer<T> sucesso) {
        if (ocupado) return;
        definirOcupado(true);
        mensagem.setText(texto);
        new SwingWorker<T, Void>() {

            @Override protected T doInBackground() throws Exception { return operacao.call(); }

            @Override protected void done() {
                definirOcupado(false);
                try { sucesso.accept(get()); }

                catch (InterruptedException e) { Thread.currentThread().interrupt(); mostrarErro(e); }

                catch (ExecutionException e) { mostrarErro(e.getCause()); }

                catch (RuntimeException e) { mostrarErro(e); }

            }

        }.execute();
    }

    private void definirOcupado(boolean valor) {
        ocupado = valor;
        for (JButton b : new JButton[]{botaoCadastrar, botaoAlterar, botaoExcluir, botaoLimpar, botaoAtualizar}) b.setEnabled(!valor);
        campoNome.setEnabled(!valor);
        campoDescricao.setEnabled(!valor);
        comboCategoria.setEnabled(!valor);
        comboStatus.setEnabled(!valor);
        tabela.setEnabled(!valor);
    }

    private void mostrarErro(Throwable erro) {
        String detalhe = erro.getMessage();
        if (detalhe == null || detalhe.isBlank()) detalhe = erro.getClass().getSimpleName();
        mensagem.setText("Falha na operação. Verifique se a API está executando.");
        JOptionPane.showMessageDialog(this, "Erro na comunicação com a API:\n" + detalhe, "Erro", JOptionPane.ERROR_MESSAGE);
    }

    private void selecionarProjeto() {
        int linha = tabela.getSelectedRow();
        if (linha == -1) return;
        linha = tabela.convertRowIndexToModel(linha);
        idSelecionado = Integer.parseInt(modelo.getValueAt(linha, 0).toString());
        campoNome.setText(Objects.toString(modelo.getValueAt(linha, 1), ""));
        campoDescricao.setText(Objects.toString(modelo.getValueAt(linha, 2), ""));
        comboCategoria.setSelectedItem(Objects.toString(modelo.getValueAt(linha, 3), ""));
        comboStatus.setSelectedItem(Objects.toString(modelo.getValueAt(linha, 4), ""));
    }

    private void adicionarCampo(JPanel painel, int linha, String rotulo,
                               JComponent componente, JLabel aviso) {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 5, 4, 5);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridx = 0; g.gridy = linha; g.weightx = 0;
        painel.add(new JLabel(rotulo), g);
        g.gridx = 1; g.weightx = 1;
        painel.add(componente, g);
        if (aviso != null) {
            g.gridy = linha + 1;
            g.insets = new Insets(0, 5, 3, 5);
            painel.add(aviso, g);
        }

    }

    private void limparErros() {
        for (JComponent campo : new JComponent[]{campoNome, campoDescricao, comboStatus}) {
            campo.putClientProperty("JComponent.outline", null);
        }

        erroNome.setText(" ");
        erroDescricao.setText(" ");
        erroStatus.setText(" ");
    }

    private void validarSeAtivo() {
        if (validacaoAtivada) validarCampos();
    }

    private boolean validarCampos() {
        String nome = campoNome.getText().trim();
        String descricao = campoDescricao.getText().trim();
        String status = Objects.toString(comboStatus.getSelectedItem(), "");
        boolean nomeValido = !nome.isEmpty();
        boolean descricaoValida = descricao.length() >= 10;
        boolean statusValido = status.equals("Planejado")
            || status.equals("Em desenvolvimento") || status.equals("Concluído");
        campoNome.putClientProperty("JComponent.outline", nomeValido ? null : "error");
        campoDescricao.putClientProperty("JComponent.outline", descricaoValida ? null : "error");
        comboStatus.putClientProperty("JComponent.outline", statusValido ? null : "error");
        erroNome.setText(nomeValido ? " " : "Informe o nome do projeto.");
        erroDescricao.setText(descricaoValida ? " " : "A descrição deve ter pelo menos 10 caracteres.");
        erroStatus.setText(statusValido ? " " : "Selecione um status válido.");
        return nomeValido && descricaoValida && statusValido;
    }

    private Projeto lerFormulario(int id) {
        validacaoAtivada = true;
        if (!validarCampos()) {
            if (campoNome.getText().trim().isEmpty()) campoNome.requestFocus();
            else if (campoDescricao.getText().trim().length() < 10) campoDescricao.requestFocus();
            else comboStatus.requestFocus();
            return null;
        }

        return new Projeto(id, campoNome.getText().trim(), campoDescricao.getText().trim(),
            Objects.toString(comboCategoria.getSelectedItem(), ""),
            Objects.toString(comboStatus.getSelectedItem(), ""));
    }

    private void cadastrar() {
        if (ocupado) return;
        Projeto projeto = lerFormulario(0);
        if (projeto == null) return;
        executar("Cadastrando projeto...", () -> api.cadastrar(projeto), cadastrado -> {
            JOptionPane.showMessageDialog(this, "Projeto cadastrado com sucesso!\nID: " + cadastrado.getId());
            limparFormulario();
            carregarTabela();
        });
    }

    private void alterar() {
        if (ocupado) return;
        if (idSelecionado == -1) {
            JOptionPane.showMessageDialog(this, "Selecione um projeto na tabela para alterar.");
            return;
        }

        Projeto projeto = lerFormulario(idSelecionado);
        if (projeto == null) return;
        executar("Alterando projeto...", () -> api.alterar(projeto), alterado -> {
            JOptionPane.showMessageDialog(this, alterado == null ? "Projeto não encontrado." : "Projeto alterado com sucesso!");
            limparFormulario();
            carregarTabela();
        });
    }

    private void excluir() {
        if (ocupado) return;
        if (idSelecionado == -1) {
            JOptionPane.showMessageDialog(this, "Selecione um projeto na tabela para excluir.");
            return;
        }

        final int id = idSelecionado;
        int resposta = JOptionPane.showConfirmDialog(this,
            "Tem certeza que deseja excluir o projeto?\nID: " + id,
            "Confirmar exclusão", JOptionPane.YES_NO_OPTION);
        if (resposta != JOptionPane.YES_OPTION) return;
        executar("Excluindo projeto...", () -> api.excluir(id), excluiu -> {
            JOptionPane.showMessageDialog(this, excluiu ? "Projeto excluído com sucesso!" : "Projeto não encontrado.");
            limparFormulario();
            carregarTabela();
        });
    }

    public static void main(String[] args) {
        if (PREFERENCIAS.getBoolean(CHAVE_TEMA, false)) FlatDarkLaf.setup();
        else FlatLightLaf.setup();
        SwingUtilities.invokeLater(() -> new LoginAdministrador().setVisible(true));
    }

}
