package view;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;

import util.SistemaLogs;

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
import client.AutenticacaoApiClient;
import client.CategoriaApiClient;
import model.Projeto;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;

public class TelaProjetos extends JFrame {
    private static final Color AZUL = new Color(15, 23, 42);
    private static final Color AZUL_CLARO = new Color(37, 99, 235);
    private static final Color MENU_ATIVO = new Color(30, 58, 122);
    private static final Color MENU_TEXTO = new Color(182, 195, 217);
    private static final Color FUNDO = new Color(244, 246, 251);
    private static final Color TEXTO = new Color(30, 41, 59);
    private static final Color TEXTO_SUAVE = new Color(100, 116, 139);
    private static final Color TEXTO_SUAVE_ESCURO = new Color(148, 163, 184);
    private static final Color BORDA_CLARA = new Color(226, 232, 240);
    private static final Color VERMELHO = new Color(220, 38, 38);
    private static final Color VERDE = new Color(22, 163, 74);

    static {
        // Aparência global dos componentes (apenas visual)
        UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));
        UIManager.put("Button.arc", 12);
        UIManager.put("Component.arc", 12);
        UIManager.put("TextComponent.arc", 12);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.trackArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("ScrollBar.showButtons", false);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("TableHeader.bottomSeparatorColor", new Color(0, 0, 0, 0));
        UIManager.put("TableHeader.separatorColor", new Color(0, 0, 0, 0));
    }

    private static Font fonte(int estilo, float tamanho) {
        Font base = UIManager.getFont("defaultFont");
        if (base == null)
            base = new Font("SansSerif", Font.PLAIN, 13);
        return base.deriveFont(estilo, tamanho);
    }

    /** Cartão com cantos arredondados, borda fina e (opcional) faixa de destaque lateral. */
    private static class Cartao extends JPanel {
        private Color corBorda = BORDA_CLARA;
        private JLabel fonteAcento;

        Cartao(LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }

        void definirBorda(Color cor) {
            corBorda = cor;
            repaint();
        }

        void definirAcento(JLabel rotulo) {
            fonteAcento = rotulo;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth() - 1, h = getHeight() - 1;
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, w, h, 16, 16);
            g2.setColor(corBorda);
            g2.drawRoundRect(0, 0, w, h, 16, 16);
            if (fonteAcento != null) {
                g2.setColor(fonteAcento.getForeground());
                g2.fillRoundRect(0, 16, 4, h - 31, 4, 4);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private void botaoPerigo(JButton b) {
        botaoPrimario(b);
        b.setBackground(VERMELHO);
        b.setForeground(Color.WHITE);
        b.putClientProperty("FlatLaf.style", "arc: 12; borderWidth: 0; focusWidth: 0; "
                + "hoverBackground: #B91C1C; pressedBackground: #991B1B");
    }

    private void botaoPrimario(JButton b) {
        b.setBackground(AZUL_CLARO);
        b.setForeground(Color.WHITE);
        b.putClientProperty("FlatLaf.style", "arc: 12; borderWidth: 0; focusWidth: 0; "
                + "hoverBackground: #1D4ED8; pressedBackground: #1E40AF");
        b.setFont(fonte(Font.BOLD, 12.5f));
        b.setMargin(new Insets(9, 16, 9, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void botaoSecundario(JButton b) {
        b.putClientProperty("FlatLaf.style", "arc: 12; focusWidth: 1");
        b.setFont(fonte(Font.BOLD, 12.5f));
        b.setMargin(new Insets(9, 16, 9, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
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
    private JPanel painelEstatisticas, painelAreaTrabalho, painelFormulario, painelAreaPrincipal;
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
    private GraficoPizza graficoStatus;
    private GraficoBarras graficoCategorias;
    private GraficoLinha graficoEvolucao;
    private GraficoRosca graficoPrazos;
    private JLabel resumoDashboard;
    private final List<JPanel> cartoesTema = new ArrayList<>();
    private final List<JLabel> rotulosTema = new ArrayList<>();
    private final List<JLabel> rotulosErro = new ArrayList<>();
    private final List<JLabel> valoresEstatisticos = new ArrayList<>();
    private final List<JLabel> rotulosSuaves = new ArrayList<>();
    private final List<JTable> tabelasSecundarias = new ArrayList<>();
    private int linhaFormulario;
    private JTextField campoNome, campoDescricao;
    private JTextField campoDataCriacao, campoPrazo;
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
    private JLabel totalValor, concluidosValor, ativosValor, canceladosValor;
    private final ProjetoApiClient api;
    private boolean ocupado;
    private int idSelecionado = -1;
    private JLabel totalDashboard;
    private JLabel concluidosDashboard;
    private JLabel ativosDashboard;
    private JLabel canceladosDashboard;

    public TelaProjetos() {
        setTitle("Sistema de Projetos");
        setSize(1440, 820);
        setMinimumSize(new Dimension(980, 620));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        api = new ProjetoApiClient();
        criarComponentes();
        criarEventos();
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                ajustarResponsividade();
            }
        });
        ajustarResponsividade();

        if (PREFERENCIAS.getBoolean(CHAVE_TEMA, false))
            aplicarTema(true);
        carregarTabela();
    }

    private JLabel titulo(String texto, int tamanho) {
        JLabel label = new JLabel(texto);
        label.setFont(fonte(Font.BOLD, tamanho));
        label.setForeground(TEXTO);
        rotulosTema.add(label);
        return label;
    }

    /** Título + subtítulo usados no topo dos cartões. */
    private JComponent cabecalhoCartao(String texto, String descricao) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel t = titulo(texto, 17);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel d = new JLabel(descricao);
        d.setFont(fonte(Font.PLAIN, 12.5f));
        d.setForeground(TEXTO_SUAVE);
        d.setAlignmentX(Component.LEFT_ALIGNMENT);
        rotulosSuaves.add(d);
        p.add(t);
        p.add(Box.createVerticalStrut(4));
        p.add(d);
        return p;
    }

    private javax.swing.border.Border bordaTopo(Color linha) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, linha),
                BorderFactory.createEmptyBorder(20, 32, 20, 32));
    }

    private JPanel cartao() {
        Cartao painel = new Cartao(new BorderLayout(0, 14));
        painel.setBackground(Color.WHITE);
        cartoesTema.add(painel);
        painel.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24));
        return painel;
    }

    private JButton itemMenu(String texto, boolean ativo) {
        String icone = texto.equals("Dashboard") ? "◈"
                : texto.equals("Projetos") ? "▣"
                        : texto.equals("Categorias") ? "▤" : texto.equals("Relatórios") ? "▥" : "⚙";
        JButton botao = new JButton(icone + "    " + texto);
        botao.setHorizontalAlignment(SwingConstants.LEFT);
        botao.setFocusPainted(false);
        botao.setForeground(ativo ? Color.WHITE : MENU_TEXTO);
        botao.setBackground(ativo ? MENU_ATIVO : AZUL);
        botao.setMargin(new Insets(12, 16, 12, 10));
        botao.setFont(fonte(Font.BOLD, 13f));
        botao.putClientProperty("FlatLaf.style", "arc: 12; borderWidth: 0; focusWidth: 0; hoverBackground: #1b2942; pressedBackground: #243b61");
        botao.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        botao.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        if (!ativo && !texto.equals("Dashboard") && !texto.equals("Relatórios") && !texto.equals("Categorias")
                && !texto.equals("Configurações")) {
            botao.addActionListener(e -> JOptionPane.showMessageDialog(this,
                    "Esta seção será implementada nas próximas etapas."));
        }

        return botao;
    }

    private int tipoIcone;

    /** Quadrado colorido com ícone desenhado, igual aos cards da versão web. */
    private static class IconeEstatistica extends JComponent {
        private final int tipo;
        private final JLabel cor;

        IconeEstatistica(int tipo, JLabel cor) {
            this.tipo = tipo;
            this.cor = cor;
            setPreferredSize(new Dimension(50, 50));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color c = cor.getForeground();
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 38));
            g2.fillRoundRect(0, 0, 50, 50, 14, 14);
            g2.setColor(c);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (tipo) {
                case 0: // pasta
                    g2.drawRoundRect(14, 18, 22, 16, 4, 4);
                    g2.drawLine(14, 18, 14, 15);
                    g2.drawLine(14, 15, 21, 15);
                    g2.drawLine(21, 15, 24, 18);
                    break;
                case 1: // check
                    g2.drawOval(14, 14, 22, 22);
                    g2.drawPolyline(new int[] { 20, 24, 31 }, new int[] { 25, 29, 21 }, 3);
                    break;
                case 2: // relógio
                    g2.drawOval(14, 14, 22, 22);
                    g2.drawPolyline(new int[] { 25, 25, 30 }, new int[] { 19, 25, 28 }, 3);
                    break;
                default: // x
                    g2.drawOval(14, 14, 22, 22);
                    g2.drawLine(20, 20, 30, 30);
                    g2.drawLine(30, 20, 20, 30);
            }
            g2.dispose();
        }
    }

    private JPanel cardEstatistico(String nome, Color cor, java.util.function.Consumer<JLabel> guardar) {
        Cartao painel = new Cartao(new BorderLayout(16, 0));
        painel.setBackground(Color.WHITE);
        cartoesTema.add(painel);
        painel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JLabel tituloCard = new JLabel(nome.replaceAll("^[^\\p{L}]+", ""));
        tituloCard.setForeground(TEXTO_SUAVE);
        rotulosSuaves.add(tituloCard);
        tituloCard.setFont(fonte(Font.PLAIN, 12.5f));
        JLabel valor = new JLabel("0");
        valor.setFont(fonte(Font.BOLD, 30f));
        valor.setForeground(cor);
        valoresEstatisticos.add(valor);
        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        tituloCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        valor.setAlignmentX(Component.LEFT_ALIGNMENT);
        textos.add(tituloCard);
        textos.add(valor);
        JPanel esquerda = new JPanel(new GridBagLayout());
        esquerda.setOpaque(false);
        esquerda.add(new IconeEstatistica((tipoIcone++) % 4, valor));
        painel.add(esquerda, BorderLayout.WEST);
        painel.add(textos, BorderLayout.CENTER);
        guardar.accept(valor);
        return painel;
    }

    private void atualizarEstatisticas(List<Projeto> projetos) {
        int concluidos = 0;
        int ativos = 0;
        int cancelados = 0;

        for (Projeto projeto : projetos) {
            String status = Objects.toString(
                    projeto.getStatus(), "").trim();

            if (status.equalsIgnoreCase("Concluído")) {
                concluidos++;
            } else if (status.equalsIgnoreCase("Planejado") ||
                    status.equalsIgnoreCase("Em Desenvolvimento")) {
                ativos++;
            } else if (status.equalsIgnoreCase("Cancelado")) {
                cancelados++;
            }
        }

        totalValor.setText(String.valueOf(projetos.size()));
        concluidosValor.setText(String.valueOf(concluidos));
        ativosValor.setText(String.valueOf(ativos));
        canceladosValor.setText(String.valueOf(cancelados));

        if (totalDashboard != null) {
            totalDashboard.setText(String.valueOf(projetos.size()));
            concluidosDashboard.setText(String.valueOf(concluidos));
            ativosDashboard.setText(String.valueOf(ativos));
            canceladosDashboard.setText(String.valueOf(cancelados));
        }
    }

    private DefaultTableCellRenderer rendererPadrao() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean selecionado, boolean foco, int linha, int coluna) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        table, value, selecionado, foco, linha, coluna);
                label.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, table.getGridColor()),
                        BorderFactory.createEmptyBorder(0, 14, 0, 6)));
                label.setFont(fonte(Font.PLAIN, 12.5f));
                if (!selecionado) {
                    label.setBackground(corLinha(linha));
                    label.setForeground(temaEscuro ? new Color(225, 232, 242) : TEXTO);
                }
                return label;
            }
        };
    }

    /** Cabeçalho alinhado à esquerda, com o mesmo recuo das células. */
    private static class RendererCabecalho implements javax.swing.table.TableCellRenderer {
        private final javax.swing.table.TableCellRenderer base;

        RendererCabecalho(javax.swing.table.TableCellRenderer base) {
            this.base = base;
        }

        @Override
        public Component getTableCellRendererComponent(JTable tabela, Object valor, boolean selecionado,
                boolean foco, int linha, int coluna) {
            Component c = base.getTableCellRendererComponent(tabela, valor, selecionado, foco, linha, coluna);
            if (c instanceof JLabel) {
                JLabel l = (JLabel) c;
                l.setHorizontalAlignment(SwingConstants.LEFT);
                l.setText(Objects.toString(valor, "").toUpperCase());
                l.setFont(l.getFont().deriveFont(Font.BOLD, 11f));
                l.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 10));
            }
            return c;
        }
    }

    /** Estilo visual comum a todas as tabelas do sistema. */
    private void estilizarTabela(JTable t) {
        t.setRowHeight(40);
        t.setBackground(temaEscuro ? new Color(26, 35, 54) : Color.WHITE);
        t.setShowVerticalLines(false);
        t.setShowHorizontalLines(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setGridColor(temaEscuro ? new Color(55, 65, 82) : new Color(238, 242, 247));
        t.setSelectionBackground(temaEscuro ? new Color(48, 76, 110) : new Color(219, 234, 254));
        t.setSelectionForeground(temaEscuro ? Color.WHITE : TEXTO);
        t.setFillsViewportHeight(true);
        JTableHeader cabecalho = t.getTableHeader();
        cabecalho.setPreferredSize(new Dimension(0, 40));
        cabecalho.setBackground(temaEscuro ? new Color(43, 56, 74) : new Color(241, 245, 249));
        cabecalho.setForeground(temaEscuro ? new Color(203, 213, 225) : new Color(71, 85, 105));
        cabecalho.setFont(fonte(Font.BOLD, 12f));
        cabecalho.setReorderingAllowed(false);
        if (!(cabecalho.getDefaultRenderer() instanceof RendererCabecalho))
            cabecalho.setDefaultRenderer(new RendererCabecalho(cabecalho.getDefaultRenderer()));
        t.putClientProperty("FlatLaf.style", "showHorizontalLines: false; "
                + "cellFocusColor: #00000000; selectionInactiveBackground: "
                + (temaEscuro ? "#304C6E" : "#DBEAFE") + "; selectionInactiveForeground: "
                + (temaEscuro ? "#FFFFFF" : "#1E293B"));
        DefaultTableCellRenderer renderer = rendererPadrao();
        for (int i = 0; i < t.getColumnCount(); i++)
            t.getColumnModel().getColumn(i).setCellRenderer(renderer);
        JScrollPane rolagem = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, t);
        if (rolagem != null) {
            rolagem.getViewport().setBackground(temaEscuro ? new Color(26, 35, 54) : Color.WHITE);
            rolagem.setBorder(BorderFactory.createLineBorder(
                    temaEscuro ? new Color(45, 59, 82) : BORDA_CLARA));
        }
    }

    private void personalizarTabela() {
        estilizarTabela(tabela);
        int[] larguras = { 44, 84, 80, 88, 152, 120, 84 };
        for (int i = 0; i < larguras.length && i < tabela.getColumnCount(); i++)
            tabela.getColumnModel().getColumn(i).setPreferredWidth(larguras[i]);
        tabela.getColumnModel().getColumn(0).setMaxWidth(60);

        tabela.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean selecionado, boolean foco, int linha, int coluna) {
                String status = Objects.toString(value, "").trim();
                Color fundo = temaEscuro ? new Color(52, 42, 92) : new Color(237, 233, 254);
                Color corTexto = temaEscuro ? new Color(196, 181, 253) : new Color(109, 40, 217);
                if (status.equalsIgnoreCase("Concluído")) {
                    fundo = temaEscuro ? new Color(32, 85, 65) : new Color(220, 252, 231);
                    corTexto = temaEscuro ? new Color(160, 239, 193) : new Color(21, 128, 61);
                } else if (status.equalsIgnoreCase("Em desenvolvimento")) {
                    fundo = temaEscuro ? new Color(94, 70, 32) : new Color(254, 243, 199);
                    corTexto = temaEscuro ? new Color(255, 216, 142) : new Color(161, 98, 7);
                } else if (status.equalsIgnoreCase("Cancelado")) {
                    fundo = temaEscuro ? new Color(99, 40, 48) : new Color(254, 226, 226);
                    corTexto = temaEscuro ? new Color(255, 170, 176) : new Color(185, 28, 28);
                }

                JPanel painel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                painel.setOpaque(true);
                painel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, table.getGridColor()),
                        BorderFactory.createEmptyBorder(8, 14, 0, 0)));
                painel.setBackground(selecionado ? table.getSelectionBackground()
                        : (corLinha(linha)));
                final Color fundoPilula = fundo;
                JLabel etiqueta = new JLabel(status) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(fundoPilula);
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                        g2.setColor(getForeground());
                        g2.fillOval(9, getHeight() / 2 - 3, 6, 6);
                        g2.dispose();
                        super.paintComponent(g);
                    }
                };
                etiqueta.setOpaque(false);
                etiqueta.setForeground(corTexto);
                etiqueta.setFont(fonte(Font.BOLD, 11f));
                etiqueta.setBorder(BorderFactory.createEmptyBorder(4, 20, 4, 10));
                painel.add(etiqueta);
                return painel;
            }

        });
    }

    private void criarComponentes() {

        campoNome = new JTextField(20);
        campoDescricao = new JTextField(20);

        campoDataCriacao = new JTextField(20);

        campoDataCriacao.setText(java.time.LocalDate.now().toString());

        campoDataCriacao.setEditable(false);

        campoPrazo = new JTextField(20);

        campoDataCriacao.putClientProperty(
                "JTextField.placeholderText", "AAAA-MM-DD");

        campoPrazo.putClientProperty(
                "JTextField.placeholderText", "AAAA-MM-DD");

        comboCategoria = new JComboBox<>(new String[] { "Web", "Software", "Mobile", "Outro" });
        comboCategoria.setEditable(false);
        comboStatus = new JComboBox<>(new String[] { "Planejado", "Em Desenvolvimento", "Concluído", "Cancelado" });
        botaoCadastrar = new JButton("Salvar");
        botaoAlterar = new JButton("Alterar");
        botaoExcluir = new JButton("Excluir");
        botaoLimpar = new JButton("Limpar");
        botaoAtualizar = new JButton("Atualizar");
        botaoCadastrar.setBackground(AZUL_CLARO);
        botaoCadastrar.setForeground(Color.WHITE);
        botaoExcluir.setBackground(VERMELHO);
        botaoExcluir.setForeground(Color.WHITE);
        modelo = new DefaultTableModel(
                new String[] { "ID", "Nome", "Descrição", "Categoria", "Status", "Data de criação", "Prazo" }, 0) {

            @Override
            public boolean isCellEditable(int linha, int coluna) {
                return false;
            }

        };
        tabela = new JTable(modelo);
        ordenador = new TableRowSorter<>(modelo);
        tabela.setRowSorter(ordenador);
        tabela.setFillsViewportHeight(true);
        tabela.setRowHeight(30);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.getTableHeader().setReorderingAllowed(false);
        tabela.getTableHeader().setFont(fonte(Font.BOLD, 12f));
        personalizarTabela();
        JPanel lateral = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0,
                        temaEscuro ? new Color(10, 17, 32) : new Color(15, 23, 42),
                        0, getHeight(), temaEscuro ? new Color(13, 23, 43) : new Color(17, 28, 54)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        painelLateral = lateral;
        lateral.setLayout(new BoxLayout(lateral, BoxLayout.Y_AXIS));
        lateral.setOpaque(false);
        lateral.setBackground(AZUL);
        lateral.setPreferredSize(new Dimension(252, 0));
        lateral.setBorder(BorderFactory.createEmptyBorder(26, 16, 18, 16));

        JPanel marca = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        marca.setOpaque(false);
        marca.setAlignmentX(Component.LEFT_ALIGNMENT);
        marca.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        JComponent icone = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(96, 165, 250), 36, 36, AZUL_CLARO));
                g2.fillRoundRect(0, 0, 36, 36, 11, 11);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(8, 8, 8, 8, 3, 3);
                g2.setColor(new Color(255, 255, 255, 190));
                g2.fillRoundRect(20, 8, 8, 8, 3, 3);
                g2.fillRoundRect(8, 20, 8, 8, 3, 3);
                g2.setColor(new Color(255, 255, 255, 120));
                g2.fillRoundRect(20, 20, 8, 8, 3, 3);
                g2.dispose();
            }
        };
        icone.setPreferredSize(new Dimension(36, 36));
        JLabel logo = new JLabel("<html><b>Sistema de</b><br><b>Projetos</b></html>");
        logo.setForeground(Color.WHITE);
        logo.setFont(fonte(Font.BOLD, 14f));
        marca.add(icone);
        marca.add(logo);
        lateral.add(marca);
        lateral.add(Box.createVerticalStrut(34));

        JLabel secao = new JLabel("  MENU");
        secao.setForeground(new Color(100, 116, 139));
        secao.setFont(fonte(Font.BOLD, 10.5f));
        secao.setAlignmentX(Component.LEFT_ALIGNMENT);
        lateral.add(secao);
        lateral.add(Box.createVerticalStrut(10));
        for (String item : new String[] { "Projetos", "Dashboard", "Categorias", "Relatórios", "Configurações" }) {
            JButton menu = itemMenu(item, item.equals("Projetos"));
            if (item.equals("Dashboard"))
                menuDashboard = menu;
            if (item.equals("Projetos"))
                menuProjetos = menu;
            if (item.equals("Relatórios"))
                menuRelatorios = menu;
            if (item.equals("Categorias"))
                menuCategorias = menu;
            if (item.equals("Configurações"))
                menuConfiguracoes = menu;
            menu.setAlignmentX(Component.LEFT_ALIGNMENT);
            lateral.add(menu);
            lateral.add(Box.createVerticalStrut(6));
        }

        lateral.add(Box.createVerticalGlue());
        JLabel rodapeMenu = new JLabel("<html><font color='#4ADE80'>●</font>&nbsp; Sistema ativo &nbsp;•&nbsp; v1.0</html>");
        rodapeMenu.setForeground(new Color(148, 163, 184));
        rodapeMenu.setFont(fonte(Font.PLAIN, 11.5f));
        rodapeMenu.setAlignmentX(Component.LEFT_ALIGNMENT);
        rodapeMenu.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));
        lateral.add(rodapeMenu);
        lateral.add(Box.createVerticalStrut(14));

        JPanel cartaoPerfil = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 14));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
            }
        };
        cartaoPerfil.setOpaque(false);
        cartaoPerfil.setAlignmentX(Component.LEFT_ALIGNMENT);
        cartaoPerfil.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        JComponent avatar = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(AZUL_CLARO);
                g2.fillOval(0, 0, 34, 34);
                g2.setColor(Color.WHITE);
                g2.setFont(fonte(Font.BOLD, 14f));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("A", (34 - fm.stringWidth("A")) / 2, (34 - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        avatar.setPreferredSize(new Dimension(34, 34));
        JLabel perfil = new JLabel("<html><b>Administrador</b><br><span style='font-size:10px;color:#94A3B8'>Acesso total</span></html>");
        perfil.setForeground(Color.WHITE);
        perfil.setFont(fonte(Font.PLAIN, 13f));
        cartaoPerfil.add(avatar);
        cartaoPerfil.add(perfil);
        lateral.add(cartaoPerfil);
        lateral.add(Box.createVerticalStrut(10));
        botaoSair = new JButton("↪  Sair da conta");
        botaoSair.setAlignmentX(Component.LEFT_ALIGNMENT);
        botaoSair.setHorizontalAlignment(SwingConstants.CENTER);
        botaoSair.setForeground(new Color(254, 202, 202));
        botaoSair.setBackground(new Color(127, 29, 29, 150));
        botaoSair.setFont(fonte(Font.BOLD, 12.5f));
        botaoSair.setFocusPainted(false);
        botaoSair.putClientProperty("FlatLaf.style", "arc: 12; borderWidth: 0; focusWidth: 0; "
                + "hoverBackground: #991B1B; pressedBackground: #7F1D1D");
        botaoSair.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        botaoSair.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        lateral.add(botaoSair);
        JPanel topo = new JPanel(new BorderLayout());
        painelTopo = topo;
        topo.setBackground(Color.WHITE);
        topo.setBorder(bordaTopo(BORDA_CLARA));
        tituloTopo = titulo("Gerenciamento de Projetos", 22);
        JLabel subtituloTopo = new JLabel("Organize e acompanhe seus projetos em um só lugar");
        subtituloTopo.setFont(fonte(Font.PLAIN, 12.5f));
        subtituloTopo.setForeground(TEXTO_SUAVE);
        rotulosSuaves.add(subtituloTopo);
        JPanel blocoTitulo = new JPanel();
        blocoTitulo.setOpaque(false);
        blocoTitulo.setLayout(new BoxLayout(blocoTitulo, BoxLayout.Y_AXIS));
        tituloTopo.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtituloTopo.setAlignmentX(Component.LEFT_ALIGNMENT);
        blocoTitulo.add(tituloTopo);
        blocoTitulo.add(Box.createVerticalStrut(3));
        blocoTitulo.add(subtituloTopo);
        topo.add(blocoTitulo, BorderLayout.WEST);
        JLabel usuario = new JLabel();
        usuario.setForeground(TEXTO_SUAVE);
        usuario.setFont(fonte(Font.PLAIN, 12.5f));
        usuario.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        Runnable relogio = () -> usuario.setText(java.time.LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm")));
        relogio.run();
        new javax.swing.Timer(30000, e -> relogio.run()).start();
        rotulosSuaves.add(usuario);
        botaoTema = new JButton("🌙 Tema escuro");
        botaoTema.setToolTipText("Alternar entre tema claro e escuro");
        botaoSecundario(botaoTema);
        botaoTema.setPreferredSize(new Dimension(140, 38));
        JPanel acoesTopo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
        acoesTopo.setOpaque(false);
        acoesTopo.add(usuario);
        acoesTopo.add(botaoTema);
        topo.add(acoesTopo, BorderLayout.EAST);
        JPanel formulario = cartao();
        formulario.add(cabecalhoCartao("Cadastro de Projeto", "Preencha os dados abaixo para registrar um projeto"),
                BorderLayout.NORTH);
        JPanel campos = new JPanel(new GridBagLayout());
        campos.setOpaque(false);

        erroNome = new JLabel(" ");
        erroDescricao = new JLabel(" ");
        erroStatus = new JLabel(" ");
        for (JLabel aviso : new JLabel[] { erroNome, erroDescricao, erroStatus }) {
            aviso.setForeground(new Color(183, 52, 62));
            rotulosErro.add(aviso);
            aviso.setFont(fonte(Font.PLAIN, 11f));
        }

        linhaFormulario = 0;
        adicionarCampo(campos, 0, "Nome do Projeto *", campoNome, erroNome);
        adicionarCampo(campos, 2, "Descrição", campoDescricao, erroDescricao);
        adicionarCampo(campos, 4, "Categoria", comboCategoria, null);
        adicionarCampo(campos, 5, "Status", comboStatus, erroStatus);
        adicionarCampo(campos, 7, "Data de criação", campoDataCriacao, null);
        adicionarCampo(campos, 8, "Prazo", campoPrazo, null);
        JPanel blocoForm = new JPanel(new BorderLayout(0, 8));
        blocoForm.setOpaque(false);
        blocoForm.add(campos, BorderLayout.NORTH);
        formulario.add(blocoForm, BorderLayout.CENTER);
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        botoes.setOpaque(false);
        for (JButton b : new JButton[] { botaoCadastrar, botaoAlterar, botaoExcluir, botaoLimpar })
            botoes.add(b);
        botaoPrimario(botaoCadastrar);
        botaoPerigo(botaoExcluir);
        botaoSecundario(botaoAlterar);
        botaoSecundario(botaoLimpar);
        botaoSecundario(botaoAtualizar);
        formulario.add(botoes, BorderLayout.SOUTH);
        JPanel listagem = cartao();
        JPanel tituloLista = new JPanel(new BorderLayout());
        tituloLista.setOpaque(false);
        tituloLista.add(cabecalhoCartao("Lista de Projetos", "Consulte, filtre e selecione um projeto para editar"),
                BorderLayout.WEST);
        tituloLista.add(botaoAtualizar, BorderLayout.EAST);

        campoBusca = new JTextField(20);
        campoBusca.putClientProperty("JTextField.placeholderText", "Pesquisar por nome, categoria ou status...");
        filtroCategoria = new JComboBox<>(new String[] { "Todas", "Web", "Software", "Mobile", "Outro" });
        filtroStatus = new JComboBox<>(
                new String[] { "Todos", "Planejado", "Em Desenvolvimento", "Concluído", "Cancelado" });
        JPanel filtros = new JPanel(new GridBagLayout());
        filtros.setOpaque(false);
        GridBagConstraints gf = new GridBagConstraints();
        gf.fill = GridBagConstraints.HORIZONTAL;
        gf.ipady = 5;
        gf.gridy = 0;
        gf.gridx = 0;
        gf.weightx = 2.2;
        gf.insets = new Insets(0, 0, 0, 8);
        filtros.add(campoBusca, gf);
        gf.gridx = 1;
        gf.weightx = 1;
        filtros.add(filtroCategoria, gf);
        gf.gridx = 2;
        gf.insets = new Insets(0, 0, 0, 0);
        filtros.add(filtroStatus, gf);
        for (JComponent campo : new JComponent[] { campoNome, campoDescricao, campoBusca,
                comboCategoria, comboStatus, filtroCategoria, filtroStatus }) {
            campo.setFont(fonte(Font.PLAIN, 13f));
        }
        for (JComponent campo : new JComponent[] { campoNome, campoDescricao, campoBusca, campoDataCriacao,
                campoPrazo, comboCategoria, comboStatus, filtroCategoria, filtroStatus })
            campo.putClientProperty("FlatLaf.style", "arc: 12");
        campoNome.putClientProperty("JTextField.placeholderText", "Digite o nome do projeto");
        campoDescricao.putClientProperty("JTextField.placeholderText", "Descreva o objetivo do projeto");
        JPanel cabecalhoLista = new JPanel(new BorderLayout(0, 12));
        cabecalhoLista.setOpaque(false);
        cabecalhoLista.add(tituloLista, BorderLayout.NORTH);
        cabecalhoLista.add(filtros, BorderLayout.SOUTH);
        listagem.add(cabecalhoLista, BorderLayout.NORTH);
        JScrollPane rolagemTabela = new JScrollPane(tabela);
        rolagemTabela.getVerticalScrollBar().setUnitIncrement(16);
        listagem.add(rolagemTabela, BorderLayout.CENTER);
        personalizarTabela();
        botaoAnterior = new JButton("◀  Anterior");
        botaoProxima = new JButton("Próxima  ▶");
        botaoSecundario(botaoAnterior);
        botaoSecundario(botaoProxima);
        indicadorPagina = new JLabel("Página 1 de 1", SwingConstants.CENTER);
        JPanel navegacao = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        navegacao.setOpaque(false);
        navegacao.add(botaoAnterior);
        navegacao.add(indicadorPagina);
        navegacao.add(botaoProxima);
        listagem.add(navegacao, BorderLayout.SOUTH);
        mensagem = new JLabel(" ");
        mensagem.setForeground(TEXTO_SUAVE);
        mensagem.setFont(fonte(Font.PLAIN, 12f));
        rotulosSuaves.add(mensagem);
        JPanel conteudo = new JPanel(new BorderLayout(0, 18));
        painelConteudo = conteudo;
        conteudo.setBackground(FUNDO);
        conteudo.setBorder(BorderFactory.createEmptyBorder(28, 32, 36, 32));

        JPanel estatisticas = new JPanel(new GridLayout(1, 4, 18, 0));
        painelEstatisticas = estatisticas;
        estatisticas.setOpaque(false);

        estatisticas.add(cardEstatistico(
                "Total de projetos",
                AZUL_CLARO,
                l -> totalValor = l));

        estatisticas.add(cardEstatistico(
                "Projetos concluídos",
                VERDE,
                l -> concluidosValor = l));

        estatisticas.add(cardEstatistico(
                "Projetos ativos",
                new Color(217, 119, 6),
                l -> ativosValor = l));

        estatisticas.add(cardEstatistico(
                "Projetos cancelados",
                VERMELHO,
                l -> canceladosValor = l));

        JPanel areaTrabalho = new JPanel(new GridBagLayout());
        painelAreaTrabalho = areaTrabalho;
        painelFormulario = formulario;
        areaTrabalho.setOpaque(false);
        GridBagConstraints coluna = new GridBagConstraints();
        coluna.gridy = 0;
        coluna.fill = GridBagConstraints.BOTH;
        coluna.weighty = 1.0;
        coluna.insets = new Insets(0, 0, 0, 18);
        coluna.gridx = 0;
        coluna.weightx = 0;
        formulario.setPreferredSize(new Dimension(360, formulario.getPreferredSize().height));
        areaTrabalho.add(formulario, coluna);
        coluna.gridx = 1;
        coluna.weightx = 1;
        coluna.insets = new Insets(0, 0, 0, 0);
        areaTrabalho.add(listagem, coluna);
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
        JPanel areaPrincipal = new JPanel(new BorderLayout(0, 18));
        painelAreaPrincipal = areaPrincipal;
        areaPrincipal.setBackground(FUNDO);
        painelTelas.setBackground(FUNDO);
        estatisticas.setBorder(BorderFactory.createEmptyBorder(20, 28, 0, 28));
        areaPrincipal.add(estatisticas, BorderLayout.NORTH);
        areaPrincipal.add(painelTelas, BorderLayout.CENTER);
        principal.add(areaPrincipal, BorderLayout.CENTER);
        setContentPane(new JPanel(new BorderLayout()));
        add(lateral, BorderLayout.WEST);
        add(principal, BorderLayout.CENTER);
    }

    /** Ajusta proporções e distribuição dos componentes conforme o tamanho da janela. */
    private void ajustarResponsividade() {
        int largura = getContentPane().getWidth();
        if (largura <= 0)
            largura = getWidth();
        boolean compacto = largura < 1250;

        if (painelLateral != null) {
            int larguraMenu = largura < 1120 ? 218 : 252;
            painelLateral.setPreferredSize(new Dimension(larguraMenu, 0));
            painelLateral.setBorder(BorderFactory.createEmptyBorder(
                    compacto ? 20 : 26, compacto ? 12 : 16, 18, compacto ? 12 : 16));
        }
        if (painelConteudo != null) {
            painelConteudo.setBorder(BorderFactory.createEmptyBorder(
                    compacto ? 18 : 28, compacto ? 18 : 32,
                    compacto ? 22 : 36, compacto ? 18 : 32));
        }
        if (painelEstatisticas != null) {
            painelEstatisticas.setLayout(new GridLayout(compacto ? 2 : 1, compacto ? 2 : 4,
                    compacto ? 12 : 18, compacto ? 12 : 0));
        }
        if (painelFormulario != null) {
            int larguraFormulario = largura < 1120 ? 320 : compacto ? 350 : 410;
            painelFormulario.setPreferredSize(new Dimension(larguraFormulario,
                    painelFormulario.getPreferredSize().height));
        }
        if (painelAreaTrabalho != null) {
            painelAreaTrabalho.revalidate();
        }
        if (painelLateral != null)
            painelLateral.revalidate();
        if (painelConteudo != null)
            painelConteudo.revalidate();
    }

    private void mostrarTela(String tela) {
        navegacaoTelas.show(painelTelas, tela);
        tituloTopo.setText(tela.equals("Dashboard") ? "Dashboard de Projetos"
                : tela.equals("Relatórios") ? "Relatórios de Projetos"
                        : tela.equals("Categorias") ? "Categorias de Projetos"
                                : tela.equals("Configurações") ? "Configurações do Sistema" : "Gerenciamento de Projetos");
        Color inativo = temaEscuro ? new Color(10, 17, 32) : AZUL;
        for (JButton item : new JButton[] { menuDashboard, menuProjetos, menuRelatorios, menuCategorias, menuConfiguracoes }) {
            boolean selecionado = item == menuDashboard && tela.equals("Dashboard")
                    || item == menuProjetos && tela.equals("Projetos")
                    || item == menuRelatorios && tela.equals("Relatórios")
                    || item == menuCategorias && tela.equals("Categorias")
                    || item == menuConfiguracoes && tela.equals("Configurações");
            item.setBackground(selecionado ? MENU_ATIVO : inativo);
            item.setForeground(selecionado ? Color.WHITE : MENU_TEXTO);
        }
        if (tela.equals("Dashboard"))
            atualizarDashboard();
        if (tela.equals("Relatórios"))
            atualizarRelatorio();
        if (tela.equals("Categorias"))
            carregarCategorias();
        if (tela.equals("Configurações"))
            atualizarConfiguracoes();
    }

    private JPanel criarConfiguracoes() {
        JPanel pagina = new JPanel(new BorderLayout(0, 16));
        pagina.setBackground(FUNDO);
        pagina.setBorder(BorderFactory.createEmptyBorder(28, 32, 36, 32));
        JPanel corpo = new JPanel();
        corpo.setOpaque(false);
        corpo.setLayout(new BoxLayout(corpo, BoxLayout.Y_AXIS));
        JPanel aparencia = cartao();
        aparencia.setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));
        aparencia.add(cabecalhoCartao("Preferências de aparência", "Escolha o tema da interface"), BorderLayout.NORTH);
        JPanel controles = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        controles.setBorder(BorderFactory.createEmptyBorder(0, -12, 0, 0));
        controles.setOpaque(false);
        JLabel rotulo = titulo("Tema do sistema:", 13);
        escolhaTema = new JComboBox<>(new String[] { "Claro", "Escuro" });
        botaoSalvarPreferencias = new JButton("Salvar preferências");
        botaoPrimario(botaoSalvarPreferencias);
        controles.add(rotulo);
        controles.add(escolhaTema);
        controles.add(botaoSalvarPreferencias);
        aparencia.add(controles, BorderLayout.CENTER);
        situacaoPreferencias = new JLabel(" ");
        situacaoPreferencias.setFont(fonte(Font.PLAIN, 12f));
        situacaoPreferencias.setForeground(TEXTO_SUAVE);
        rotulosSuaves.add(situacaoPreferencias);
        aparencia.add(situacaoPreferencias, BorderLayout.SOUTH);
        corpo.add(aparencia);
        corpo.add(Box.createVerticalStrut(16));
        JPanel restauracao = cartao();
        restauracao.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        restauracao.add(cabecalhoCartao("Restaurar configurações", "Volte ao tema padrão sem afetar seus dados"), BorderLayout.NORTH);
        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        acoes.setBorder(BorderFactory.createEmptyBorder(0, -12, 0, 0));
        acoes.setOpaque(false);
        botaoRestaurarPadrao = new JButton("Restaurar padrão (tema claro)");
        botaoSecundario(botaoRestaurarPadrao);
        acoes.add(botaoRestaurarPadrao);
        restauracao.add(acoes, BorderLayout.CENTER);
        corpo.add(restauracao);
        corpo.add(Box.createVerticalStrut(16));
        JPanel informacoes = cartao();
        informacoes.setMaximumSize(new Dimension(Integer.MAX_VALUE, 190));
        informacoes.add(cabecalhoCartao("Sobre o sistema", "Informações da aplicação"), BorderLayout.NORTH);
        JPanel detalhes = new JPanel(new GridLayout(3, 1, 0, 8));
        detalhes.setOpaque(false);
        detalhes.add(titulo("Sistema de Projetos | Versão 1.0", 13));
        detalhes.add(titulo("Tecnologias: Java, Swing, FlatLaf e API REST", 13));
        detalhes.add(titulo("Recursos: projetos, categorias, dashboard e relatórios", 13));
        informacoes.add(detalhes, BorderLayout.CENTER);
        corpo.add(informacoes);
        JScrollPane rolagemConfig = new JScrollPane(corpo, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        rolagemConfig.setBorder(BorderFactory.createEmptyBorder());
        rolagemConfig.setOpaque(false);
        rolagemConfig.getViewport().setOpaque(false);
        pagina.add(rolagemConfig, BorderLayout.CENTER);
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
        if (resposta != JOptionPane.YES_OPTION)
            return;
        escolhaTema.setSelectedItem("Claro");
        salvarPreferencias();
    }

    private JPanel criarCategorias() {
        JPanel pagina = new JPanel(new BorderLayout(0, 14));
        pagina.setBackground(FUNDO);
        pagina.setBorder(BorderFactory.createEmptyBorder(28, 32, 36, 32));
        JPanel cabecalho = cartao();
        cabecalho.setLayout(new BorderLayout(16, 0));
        cabecalho.add(cabecalhoCartao("Gerenciamento de categorias",
                "Cadastre, renomeie e organize as categorias dos projetos"), BorderLayout.WEST);
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        botoes.setOpaque(false);
        botaoNovaCategoria = new JButton("+  Nova categoria");
        botaoEditarCategoria = new JButton("Editar selecionada");
        botaoExcluirCategoria = new JButton("Excluir selecionada");
        botaoRecarregarCategorias = new JButton("Atualizar");
        botaoPrimario(botaoNovaCategoria);
        botaoPerigo(botaoExcluirCategoria);
        botaoSecundario(botaoEditarCategoria);
        botaoSecundario(botaoRecarregarCategorias);
        for (JButton b : new JButton[] { botaoNovaCategoria, botaoEditarCategoria, botaoExcluirCategoria,
                botaoRecarregarCategorias })
            botoes.add(b);
        cabecalho.add(botoes, BorderLayout.EAST);
        pagina.add(cabecalho, BorderLayout.NORTH);
        JPanel lista = cartao();
        modeloCategorias = new DefaultTableModel(new String[] { "Categoria", "Projetos vinculados" }, 0) {
            @Override
            public boolean isCellEditable(int linha, int coluna) {
                return false;
            }
        };
        tabelaCategorias = new JTable(modeloCategorias);
        tabelaCategorias.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabelaCategorias.setAutoCreateRowSorter(true);
        lista.add(new JScrollPane(tabelaCategorias), BorderLayout.CENTER);
        tabelasSecundarias.add(tabelaCategorias);
        estilizarTabela(tabelaCategorias);
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
        filtroCategoria.removeAllItems();
        filtroCategoria.addItem("Todas");
        relCategoria.removeAllItems();
        relCategoria.addItem("Todas");
        for (String nome : categoriasCarregadas) {
            comboCategoria.addItem(nome);
            filtroCategoria.addItem(nome);
            relCategoria.addItem(nome);
        }
        if (categoriasCarregadas.contains(selecionada))
            comboCategoria.setSelectedItem(selecionada);
        if (categoriasCarregadas.contains(filtro))
            filtroCategoria.setSelectedItem(filtro);
        if (categoriasCarregadas.contains(rel))
            relCategoria.setSelectedItem(rel);
        reiniciarPaginacao();
        atualizarRelatorio();
    }

    private void atualizarTabelaCategorias() {
        if (modeloCategorias == null)
            return;
        modeloCategorias.setRowCount(0);
        for (String categoria : categoriasCarregadas) {
            int quantidade = 0;
            for (Projeto projeto : projetosCarregados)
                if (categoria.equalsIgnoreCase(Objects.toString(projeto.getCategoria(), "")))
                    quantidade++;
            modeloCategorias.addRow(new Object[] { categoria, quantidade });
        }
    }

    private String categoriaSelecionada() {
        int linha = tabelaCategorias.getSelectedRow();
        if (linha < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma categoria na tabela.");
            return null;
        }
        return Objects.toString(modeloCategorias.getValueAt(tabelaCategorias.convertRowIndexToModel(linha), 0), "");
    }

    private void editarCategoria(boolean edicao) {
        String antiga = edicao ? categoriaSelecionada() : null;
        if (edicao && antiga == null)
            return;
        String nome = JOptionPane.showInputDialog(this, edicao ? "Novo nome da categoria:" : "Nome da nova categoria:",
                edicao ? antiga : "");
        if (nome == null)
            return;
        nome = nome.trim();
        if (nome.isEmpty() || nome.length() > 60 || nome.contains("\n") || nome.contains("\r")) {
            JOptionPane.showMessageDialog(this, "Informe um nome válido (até 60 caracteres).");
            return;
        }
        final String novo = nome, anterior = antiga;
        executar(edicao ? "Alterando categoria..." : "Cadastrando categoria...",
                () -> {
                    if (edicao)
                        apiCategorias.renomear(anterior, novo);
                    else
                        apiCategorias.cadastrar(novo);
                    return true;
                },
                ok -> carregarTabela());
    }

    private void excluirCategoria() {
        String nome = categoriaSelecionada();
        if (nome == null)
            return;
        if (JOptionPane.showConfirmDialog(this,
                "Excluir a categoria '" + nome + "'?\nCategorias em uso não podem ser excluídas.",
                "Confirmar exclusão", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
            return;
        executar("Excluindo categoria...", () -> {
            apiCategorias.excluir(nome);
            return true;
        },
                ok -> carregarCategorias());
    }

    private JPanel criarRelatorios() {
        JPanel pagina = new JPanel(new BorderLayout(0, 14));
        pagina.setBackground(FUNDO);
        pagina.setBorder(BorderFactory.createEmptyBorder(28, 32, 36, 32));
        JPanel cabecalho = cartao();
        cabecalho.setLayout(new BorderLayout(16, 0));
        cabecalho.add(cabecalhoCartao("Relatório de projetos",
                "Filtre os dados e exporte o resultado"), BorderLayout.WEST);
        JPanel controles = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        controles.setOpaque(false);
        relCategoria = new JComboBox<>(new String[] { "Todas", "Web", "Software", "Mobile", "Outro" });
        relStatus = new JComboBox<>(
                new String[] { "Todos", "Planejado", "Em Desenvolvimento", "Concluído", "Cancelado" });
        botaoCsv = new JButton("Exportar CSV");
        botaoPdf = new JButton("Exportar PDF");
        botaoPrimario(botaoCsv);
        botaoSecundario(botaoPdf);
        JLabel rotCat = new JLabel("Categoria");
        JLabel rotSta = new JLabel("Status");
        for (JLabel r : new JLabel[] { rotCat, rotSta }) {
            r.setFont(fonte(Font.BOLD, 12.5f));
            rotulosTema.add(r);
        }
        controles.add(rotCat);
        controles.add(relCategoria);
        controles.add(rotSta);
        controles.add(relStatus);
        controles.add(botaoCsv);
        controles.add(botaoPdf);
        cabecalho.add(controles, BorderLayout.EAST);
        pagina.add(cabecalho, BorderLayout.NORTH);
        JPanel lista = cartao();
        modeloRelatorio = new DefaultTableModel(new String[] { "ID", "Nome", "Descrição", "Categoria", "Status" }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable tabelaRelatorio = new JTable(modeloRelatorio);
        tabelaRelatorio.setAutoCreateRowSorter(true);
        lista.add(new JScrollPane(tabelaRelatorio), BorderLayout.CENTER);
        tabelasSecundarias.add(tabelaRelatorio);
        estilizarTabela(tabelaRelatorio);
        tabelaRelatorio.getColumnModel().getColumn(0).setMaxWidth(70);
        pagina.add(lista, BorderLayout.CENTER);
        JPanel rodape = cartao();
        resumoRelatorio = titulo("Nenhum projeto encontrado.", 13);
        rodape.setBorder(BorderFactory.createEmptyBorder(14, 24, 14, 24));
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
        if (modeloRelatorio == null)
            return;
        List<Projeto> lista = projetosDoRelatorio();
        modeloRelatorio.setRowCount(0);
        int planejados = 0, andamento = 0, concluidos = 0;
        for (Projeto p : lista) {
            modeloRelatorio
                    .addRow(new Object[] { p.getId(), p.getNome(), p.getDescricao(), p.getCategoria(), p.getStatus() });
            if ("Planejado".equalsIgnoreCase(p.getStatus()))
                planejados++;
            else if ("Em desenvolvimento".equalsIgnoreCase(p.getStatus()))
                andamento++;
            else if ("Concluído".equalsIgnoreCase(p.getStatus()))
                concluidos++;
        }
        resumoRelatorio.setText("Total: " + lista.size() + "   |   Planejados: " + planejados
                + "   |   Em desenvolvimento: " + andamento + "   |   Concluídos: " + concluidos);
    }

    private void exportarRelatorio(boolean pdf) {
        List<Projeto> lista = projetosDoRelatorio();
        JFileChooser seletor = new JFileChooser();
        seletor.setSelectedFile(new File(pdf ? "relatorio_projetos.pdf" : "relatorio_projetos.csv"));
        if (seletor.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
            return;
        File arquivo = seletor.getSelectedFile();
        String extensao = pdf ? ".pdf" : ".csv";
        if (!arquivo.getName().toLowerCase(java.util.Locale.ROOT).endsWith(extensao))
            arquivo = new File(arquivo.getAbsolutePath() + extensao);
        if (arquivo.exists() && JOptionPane.showConfirmDialog(this, "Substituir o arquivo existente?",
                "Confirmar", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
            return;
        try {
            if (pdf)
                gerarPdf(arquivo, lista);
            else
                gerarCsv(arquivo, lista);
            JOptionPane.showMessageDialog(this, "Relatório exportado com sucesso!\n" + arquivo.getAbsolutePath());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao exportar: " + ex.getMessage(), "Erro",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private String csvCampo(Object valor) {
        String texto = Objects.toString(valor, "");
        return "\"" + texto.replace("\"", "\"\"") + "\"";
    }

    private void gerarCsv(File arquivo, List<Projeto> lista) throws IOException {
        try (java.io.Writer out = new java.io.OutputStreamWriter(new FileOutputStream(arquivo),
                StandardCharsets.UTF_8)) {
            out.write('\ufeff'); // BOM: Excel reconhece UTF-8
            out.write("ID;Nome;Descrição;Categoria;Status\r\n");
            for (Projeto p : lista) {
                out.write(p.getId() + ";" + csvCampo(p.getNome()) + ";" + csvCampo(p.getDescricao())
                        + ";" + csvCampo(p.getCategoria()) + ";" + csvCampo(p.getStatus()) + "\r\n");
            }
        }
    }

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
            if ("Planejado".equalsIgnoreCase(p.getStatus()))
                contagem[0]++;
            else if ("Em desenvolvimento".equalsIgnoreCase(p.getStatus()))
                contagem[1]++;
            else if ("Concluído".equalsIgnoreCase(p.getStatus()))
                contagem[2]++;
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
                    if (espaco > 35)
                        corte = espaco;
                }
                bloco.add("Descricao: " + descricao.substring(0, corte).trim());
                descricao = descricao.substring(corte).trim();
            }
            bloco.add(" ");
            if (atual.size() + bloco.size() > 55) {
                atual = new ArrayList<>();
                paginas.add(atual);
                atual.add("RELATORIO DE PROJETOS - CONTINUACAO");
                atual.add("---------------------------------------------------------------");
            }
            atual.addAll(bloco);
        }
        if (lista.isEmpty())
            atual.add("Nenhum projeto encontrado para os filtros selecionados.");
        escreverPdf(arquivo, paginas);
    }

    private String pdfTexto(String texto) {

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
        for (int i = 0; i < numeroPaginas; i++)
            filhos.append(3 + i * 2).append(" 0 R ");
        objetos.add(("<< /Type /Pages /Kids [" + filhos + "] /Count " + numeroPaginas + " >>")
                .getBytes(StandardCharsets.US_ASCII));
        for (int i = 0; i < numeroPaginas; i++) {
            int conteudoId = 4 + i * 2;
            objetos.add(("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 "
                    + fonteId + " 0 R >> >> /Contents " + conteudoId + " 0 R >>").getBytes(StandardCharsets.US_ASCII));
            StringBuilder conteudo = new StringBuilder("BT /F1 10 Tf 45 800 Td 14 TL\n");
            for (String linha : paginas.get(i))
                conteudo.append("(").append(pdfTexto(linha)).append(") Tj T*\n");
            conteudo.append("ET\n");
            byte[] stream = conteudo.toString().getBytes(java.nio.charset.Charset.forName("windows-1252"));
            ByteArrayOutputStream obj = new ByteArrayOutputStream();
            obj.write(("<< /Length " + stream.length + " >>\nstream\n").getBytes(StandardCharsets.US_ASCII));
            obj.write(stream);
            obj.write("\nendstream".getBytes(StandardCharsets.US_ASCII));
            objetos.add(obj.toByteArray());
        }
        objetos.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>"
                .getBytes(StandardCharsets.US_ASCII));
        for (int i = 0; i < totalObjetos; i++) {
            offsets.add(bytes.size());
            bytes.write(((i + 1) + " 0 obj\n").getBytes(StandardCharsets.US_ASCII));
            bytes.write(objetos.get(i));
            bytes.write("\nendobj\n".getBytes(StandardCharsets.US_ASCII));
        }
        int inicioXref = bytes.size();
        bytes.write(("xref\n0 " + (totalObjetos + 1) + "\n0000000000 65535 f \n").getBytes(StandardCharsets.US_ASCII));
        for (int i = 1; i <= totalObjetos; i++)
            bytes.write(String.format(java.util.Locale.ROOT, "%010d 00000 n \n", offsets.get(i))
                    .getBytes(StandardCharsets.US_ASCII));
        bytes.write(("trailer\n<< /Size " + (totalObjetos + 1) + " /Root 1 0 R >>\nstartxref\n"
                + inicioXref + "\n%%EOF\n").getBytes(StandardCharsets.US_ASCII));
        Files.write(arquivo.toPath(), bytes.toByteArray());
    }

    private JPanel criarDashboard() {
        JPanel dashboard = new JPanel(new BorderLayout(0, 16));
        dashboard.setBackground(FUNDO);
        dashboard.setBorder(BorderFactory.createEmptyBorder(28, 32, 36, 32));

        JPanel cabecalhoDashboard = new JPanel(new BorderLayout(0, 16));
        cabecalhoDashboard.setOpaque(false);

        cabecalhoDashboard.add(
                titulo("Visão geral dos projetos", 20),
                BorderLayout.NORTH);

        dashboard.add(cabecalhoDashboard, BorderLayout.NORTH);
        JPanel graficos = new JPanel(new GridLayout(1, 2, 16, 0));
        graficos.setOpaque(false);

        graficoStatus = new GraficoPizza(
                "Status dos Projetos",
                new String[] {
                        "Planejado",
                        "Em Desenvolvimento",
                        "Concluído",
                        "Cancelado"
                },
                new Color[] {
                        new Color(139, 92, 246),
                        new Color(245, 158, 11),
                        new Color(34, 197, 94),
                        new Color(239, 68, 68)
                });

        graficoCategorias = new GraficoBarras("Projetos por categoria",
                new String[] { "Web", "Software", "Mobile", "Outro" },
                new Color[] { new Color(64, 128, 207), new Color(112, 91, 196),
                        new Color(46, 158, 169), new Color(198, 118, 80) });
        graficos.setLayout(new GridLayout(2, 2, 16, 16));

        graficos.add(graficoStatus);
        graficos.add(graficoCategorias);

        graficoEvolucao = new GraficoLinha();
        graficos.add(graficoEvolucao);

        graficoPrazos = new GraficoRosca();
        graficos.add(graficoPrazos);
        dashboard.add(graficos, BorderLayout.CENTER);
        JPanel rodape = cartao();
        resumoDashboard = titulo("Nenhum projeto cadastrado.", 13);
        rodape.setBorder(BorderFactory.createEmptyBorder(14, 24, 14, 24));
        rodape.add(resumoDashboard, BorderLayout.CENTER);
        dashboard.add(rodape, BorderLayout.SOUTH);
        return dashboard;
    }

    private void atualizarDashboard() {
        if (graficoStatus == null)
            return;
        int[] status = new int[4];
        int[] categorias = new int[categoriasCarregadas.size()];
        for (Projeto p : projetosCarregados) {
            String st = Objects.toString(p.getStatus(), "").trim();
            if (st.equalsIgnoreCase("Planejado"))
                status[0]++;
            else if (st.equalsIgnoreCase("Em desenvolvimento"))
                status[1]++;
            else if (st.equalsIgnoreCase("Concluído"))
                status[2]++;
            else if (st.equalsIgnoreCase("Cancelado"))
                status[3]++;
            String cat = Objects.toString(p.getCategoria(), "").trim();
            for (int i = 0; i < categoriasCarregadas.size(); i++) {
                if (cat.equalsIgnoreCase(categoriasCarregadas.get(i))) {
                    categorias[i]++;
                    break;
                }
            }
        }
        graficoStatus.definirValores(status);
        graficoCategorias.definirCategorias(categoriasCarregadas.toArray(new String[0]), categorias);
        resumoDashboard.setText("Total: " + projetosCarregados.size()
                + "   |   Planejados: " + status[0]
                + "   |   Em desenvolvimento: " + status[1]
                + "   |   Concluídos: " + status[2]);

        atualizarEvolucaoMensal();
        atualizarCumprimentoPrazos();
    }

    private void fundoGrafico(Graphics2D g2, int largura, int altura, Color fundo) {
        g2.setColor(fundo);
        g2.fillRoundRect(0, 0, largura - 1, altura - 1, 18, 18);
        g2.setColor(temaEscuro ? new Color(45, 59, 82) : BORDA_CLARA);
        g2.drawRoundRect(0, 0, largura - 1, altura - 1, 18, 18);
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
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(20, 18, 20, 18));
        }

        void definirCategorias(String[] novosNomes, int[] novosValores) {
            nomes = novosNomes.clone();
            Color[] paleta = { new Color(64, 128, 207), new Color(112, 91, 196), new Color(46, 158, 169),
                    new Color(198, 118, 80), new Color(80, 150, 95), new Color(190, 110, 160) };
            cores = new Color[nomes.length];
            for (int i = 0; i < cores.length; i++)
                cores[i] = paleta[i % paleta.length];
            definirValores(novosValores);
        }

        void definirValores(int[] novosValores) {
            valores = novosValores.clone();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color fundo = temaEscuro ? new Color(26, 35, 54) : Color.WHITE;
                Color texto = temaEscuro ? new Color(225, 232, 242) : TEXTO;
                g2.setColor(fundo);
                fundoGrafico(g2, getWidth(), getHeight(), fundo);
                g2.setColor(texto);
                g2.setFont(fonte(Font.BOLD, 16f));
                g2.drawString(tituloGrafico, 18, 30);
                int max = 1;
                for (int v : valores)
                    max = Math.max(max, v);
                int inicioY = 64;
                int larguraDisponivel = Math.max(40, getWidth() - 180);
                int alturaLinha = Math.max(42, (getHeight() - inicioY - 20) / Math.max(1, nomes.length));
                for (int i = 0; i < nomes.length; i++) {
                    int y = inicioY + i * alturaLinha;
                    g2.setColor(texto);
                    g2.setFont(fonte(Font.PLAIN, 12f));
                    String nome = nomes[i];
                    if (nome.equals("Em desenvolvimento"))
                        nome = "Em desenvolv.";
                    g2.drawString(nome, 18, y + 16);
                    int largura = (int) Math.round((double) valores[i] / max * larguraDisponivel);
                    g2.setColor(temaEscuro ? new Color(51, 65, 85) : new Color(241, 245, 249));
                    g2.fillRoundRect(145, y, larguraDisponivel, 22, 22, 22);
                    g2.setColor(cores[i]);
                    if (largura > 0)
                        g2.fillRoundRect(145, y, Math.max(22, largura), 22, 22, 22);
                    g2.setColor(texto);
                    g2.drawString(String.valueOf(valores[i]), 151 + Math.max(largura, valores[i] > 0 ? 22 : 0), y + 16);
                }
            } finally {
                g2.dispose();
            }
        }
    }

    private class GraficoPizza extends JPanel {

        private final String tituloGrafico;
        private final String[] nomes;
        private final Color[] cores;
        private int[] valores;

        GraficoPizza(String tituloGrafico, String[] nomes, Color[] cores) {
            this.tituloGrafico = tituloGrafico;
            this.nomes = nomes;
            this.cores = cores;
            this.valores = new int[nomes.length];

            setPreferredSize(new Dimension(300, 300));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(20, 18, 20, 18));
        }

        void definirValores(int[] novosValores) {
            valores = novosValores.clone();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();

            try {
                g2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                Color fundo = temaEscuro
                        ? new Color(26, 35, 54)
                        : Color.WHITE;

                Color texto = temaEscuro
                        ? new Color(225, 232, 242)
                        : TEXTO;

                g2.setColor(fundo);
                fundoGrafico(g2, getWidth(), getHeight(), fundo);

                g2.setColor(texto);
                g2.setFont(fonte(Font.BOLD, 16f));
                g2.drawString(tituloGrafico, 18, 30);

                int total = 0;
                for (int valor : valores) {
                    total += valor;
                }

                if (total == 0) {
                    g2.setFont(fonte(Font.PLAIN, 13f));
                    g2.drawString("Nenhum projeto cadastrado.", 18, 70);
                    return;
                }

                int diametro = Math.min(
                        Math.min(getWidth() - 190, getHeight() - 100),
                        220);

                diametro = Math.max(60, diametro);

                int x = 25;
                int y = Math.max(55, (getHeight() - diametro) / 2);

                int inicio = 90;
                int acumulado = 0;

                for (int i = 0; i < valores.length; i++) {

                    if (valores[i] <= 0) {
                        continue;
                    }

                    int novoAcumulado = 0;

                    for (int j = 0; j <= i; j++) {
                        novoAcumulado += valores[j];
                    }

                    int anguloFinal = (int) Math.round(
                            novoAcumulado * 360.0 / total);

                    int angulo = anguloFinal - acumulado;

                    g2.setColor(cores[i]);

                    g2.fillArc(
                            x,
                            y,
                            diametro,
                            diametro,
                            inicio,
                            -angulo);

                    inicio -= angulo;
                    acumulado = anguloFinal;
                }

                int furo = (int) (diametro * 0.58);
                g2.setColor(fundo);
                g2.fillOval(x + (diametro - furo) / 2, y + (diametro - furo) / 2, furo, furo);
                g2.setColor(texto);
                g2.setFont(fonte(Font.BOLD, 24f));
                FontMetrics fmTotal = g2.getFontMetrics();
                String textoTotal = String.valueOf(total);
                g2.drawString(textoTotal, x + (diametro - fmTotal.stringWidth(textoTotal)) / 2,
                        y + diametro / 2 + 2);
                g2.setFont(fonte(Font.PLAIN, 11f));
                fmTotal = g2.getFontMetrics();
                g2.drawString("projetos", x + (diametro - fmTotal.stringWidth("projetos")) / 2,
                        y + diametro / 2 + 18);

                int legendaX = x + diametro + 20;
                int legendaY = 85;

                g2.setFont(fonte(Font.PLAIN, 12f));

                for (int i = 0; i < nomes.length; i++) {
                    int yLegenda = legendaY + i * 34;

                    g2.setColor(cores[i]);
                    g2.fillRoundRect(
                            legendaX, yLegenda - 10,
                            12, 12, 4, 4);

                    g2.setColor(texto);

                    double percentual = valores[i] * 100.0 / total;

                    g2.drawString(
                            nomes[i] + " (" +
                                    String.format("%.1f%%", percentual) + ")",
                            legendaX + 18,
                            yLegenda);
                }

            } finally {
                g2.dispose();
            }
        }
    }

    private class GraficoLinha extends JPanel {

        private String[] meses = {};
        private int[] valores = {};

        GraficoLinha() {
            setPreferredSize(new Dimension(350, 260));
            setOpaque(false);
        }

        void definirDados(String[] meses, int[] valores) {
            this.meses = meses.clone();
            this.valores = valores.clone();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();

            try {
                g2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                Color fundo = temaEscuro
                        ? new Color(26, 35, 54)
                        : Color.WHITE;

                Color texto = temaEscuro
                        ? new Color(225, 232, 242)
                        : TEXTO;

                g2.setColor(fundo);
                fundoGrafico(g2, getWidth(), getHeight(), fundo);

                g2.setColor(texto);
                g2.setFont(fonte(Font.BOLD, 16f));
                g2.drawString("Evolução Mensal", 18, 30);

                if (valores.length == 0) {
                    g2.setFont(fonte(Font.PLAIN, 13f));
                    g2.drawString("Sem dados para exibir.", 18, 70);
                    return;
                }

                int esquerda = 55;
                int direita = getWidth() - 25;
                int topo = 65;
                int base = getHeight() - 45;

                int largura = direita - esquerda;
                int altura = base - topo;

                if (largura <= 0 || altura <= 0)
                    return;

                int max = 1;
                for (int valor : valores)
                    max = Math.max(max, valor);

                g2.setColor(temaEscuro
                        ? new Color(80, 92, 110)
                        : new Color(220, 228, 240));

                for (int i = 0; i <= 4; i++) {
                    int y = base - i * altura / 4;
                    g2.drawLine(esquerda, y, direita, y);

                    g2.setColor(texto);
                    g2.setFont(fonte(Font.PLAIN, 11f));
                    g2.drawString(
                            String.valueOf((int) Math.round(max * i / 4.0)),
                            15, y + 4);

                    g2.setColor(temaEscuro
                            ? new Color(80, 92, 110)
                            : new Color(220, 228, 240));
                }

                if (valores.length > 1) {
                    java.awt.geom.Path2D.Double area = new java.awt.geom.Path2D.Double();
                    area.moveTo(esquerda, base);
                    for (int i = 0; i < valores.length; i++) {
                        int ax = esquerda + i * largura / (valores.length - 1);
                        int ay = base - (int) Math.round(valores[i] * altura / (double) max);
                        area.lineTo(ax, ay);
                    }
                    area.lineTo(esquerda + largura, base);
                    area.closePath();
                    g2.setPaint(new GradientPaint(0, topo, new Color(37, 99, 235, 70),
                            0, base, new Color(37, 99, 235, 0)));
                    g2.fill(area);
                }

                int anteriorX = -1;
                int anteriorY = -1;

                for (int i = 0; i < valores.length; i++) {
                    int x = valores.length == 1
                            ? esquerda + largura / 2
                            : esquerda + i * largura / (valores.length - 1);

                    int y = base - (int) Math.round(
                            valores[i] * altura / (double) max);

                    g2.setColor(new Color(37, 99, 235));
                    g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                    if (anteriorX >= 0)
                        g2.drawLine(anteriorX, anteriorY, x, y);

                    g2.setColor(fundo);
                    g2.fillOval(x - 6, y - 6, 12, 12);
                    g2.setColor(new Color(37, 99, 235));
                    g2.fillOval(x - 4, y - 4, 8, 8);

                    g2.setColor(texto);
                    g2.setFont(fonte(Font.PLAIN, 10f));

                    String mes = meses[i];
                    int larguraTexto = g2.getFontMetrics().stringWidth(mes);

                    g2.drawString(
                            mes, x - larguraTexto / 2, base + 20);

                    anteriorX = x;
                    anteriorY = y;
                }

            } finally {
                g2.dispose();
            }
        }
    }

    private class GraficoRosca extends JPanel {

        private int noPrazo = 0;
        private int atrasados = 0;

        GraficoRosca() {
            setPreferredSize(new Dimension(350, 260));
            setOpaque(false);
        }

        void definirDados(int noPrazo, int atrasados) {
            this.noPrazo = noPrazo;
            this.atrasados = atrasados;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();

            try {
                g2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                Color fundo = temaEscuro
                        ? new Color(26, 35, 54)
                        : Color.WHITE;

                Color texto = temaEscuro
                        ? new Color(225, 232, 242)
                        : TEXTO;

                Color verde = new Color(34, 197, 94);
                Color vermelho = new Color(239, 68, 68);

                g2.setColor(fundo);
                fundoGrafico(g2, getWidth(), getHeight(), fundo);

                g2.setColor(texto);
                g2.setFont(fonte(Font.BOLD, 16f));
                g2.drawString("Cumprimento de Prazos", 18, 30);

                int total = noPrazo + atrasados;

                if (total == 0) {
                    g2.setFont(fonte(Font.PLAIN, 13f));
                    g2.drawString(
                            "Nenhum projeto em aberto com prazo válido.",
                            18, 75);
                    return;
                }

                int diametro = Math.min(
                        Math.min(getWidth() - 190, getHeight() - 100),
                        200);

                diametro = Math.max(60, diametro);

                int x = 25;
                int y = Math.max(55, (getHeight() - diametro) / 2);

                int anguloAtrasados = (int) Math.round(
                        atrasados * 360.0 / total);

                g2.setColor(verde);
                g2.fillArc(x, y, diametro, diametro, 90, 360);

                g2.setColor(vermelho);
                g2.fillArc(
                        x, y, diametro, diametro,
                        90, -anguloAtrasados);

                int espessura = Math.max(16, diametro / 5);
                int interno = diametro - espessura * 2;

                g2.setColor(fundo);
                g2.fillOval(
                        x + espessura,
                        y + espessura,
                        interno,
                        interno);

                g2.setColor(texto);
                g2.setFont(fonte(Font.BOLD, 26f));

                String numero = String.valueOf(total);
                FontMetrics fm = g2.getFontMetrics();

                g2.drawString(
                        numero,
                        x + (diametro - fm.stringWidth(numero)) / 2,
                        y + diametro / 2);

                g2.setFont(fonte(Font.PLAIN, 12f));

                String centro = "Em aberto";
                fm = g2.getFontMetrics();

                g2.drawString(
                        centro,
                        x + (diametro - fm.stringWidth(centro)) / 2,
                        y + diametro / 2 + 20);

                int legendaX = x + diametro + 20;
                int legendaY = 90;

                String[] nomes = { "No prazo", "Atrasados" };
                int[] valores = { noPrazo, atrasados };
                Color[] cores = { verde, vermelho };

                for (int i = 0; i < nomes.length; i++) {
                    int ly = legendaY + i * 42;

                    g2.setColor(cores[i]);
                    g2.fillRoundRect(
                            legendaX, ly - 10, 12, 12, 4, 4);

                    double percentual = valores[i] * 100.0 / total;

                    g2.setColor(texto);
                    g2.setFont(fonte(Font.PLAIN, 12f));

                    g2.drawString(
                            String.format(
                                    java.util.Locale.forLanguageTag("pt-BR"),
                                    "%s: %d (%.1f%%)",
                                    nomes[i], valores[i], percentual),
                            legendaX + 18,
                            ly);
                }

            } finally {
                g2.dispose();
            }
        }
    }

    private void atualizarCumprimentoPrazos() {

        int noPrazo = 0;
        int atrasados = 0;

        java.time.LocalDate hoje = java.time.LocalDate.now();

        for (Projeto p : projetosCarregados) {

            String status = Objects.toString(
                    p.getStatus(), "").trim();

            if (status.equalsIgnoreCase("Concluído") ||
                    status.equalsIgnoreCase("Cancelado")) {
                continue;
            }

            try {
                String prazoTexto = Objects.toString(
                        p.getPrazo(), "").trim();

                if (prazoTexto.isEmpty()) {
                    continue;
                }

                java.time.LocalDate prazo = java.time.LocalDate.parse(prazoTexto);

                if (prazo.isBefore(hoje)) {
                    atrasados++;
                } else {
                    noPrazo++;
                }

            } catch (java.time.format.DateTimeParseException ex) {
                // Ignora projetos com prazo inválido.
            }
        }

        graficoPrazos.definirDados(noPrazo, atrasados);
    }

    private void atualizarEvolucaoMensal() {

        java.util.TreeMap<java.time.YearMonth, Integer> contagem = new java.util.TreeMap<>();

        for (Projeto p : projetosCarregados) {
            try {

                java.time.LocalDate data = p.getDataCriacao();

                java.time.YearMonth mes = java.time.YearMonth.from(data);

                contagem.merge(mes, 1, Integer::sum);

            } catch (Exception ex) {
                // Ignora projetos sem data válida.
            }
        }

        if (contagem.isEmpty()) {
            graficoEvolucao.definirDados(
                    new String[0], new int[0]);
            return;
        }

        java.time.YearMonth primeiro = contagem.firstKey();
        java.time.YearMonth ultimo = contagem.lastKey();

        java.util.List<String> meses = new ArrayList<>();
        java.util.List<Integer> valores = new ArrayList<>();

        int acumulado = 0;

        for (java.time.YearMonth mes = primeiro; !mes.isAfter(ultimo); mes = mes.plusMonths(1)) {

            acumulado += contagem.getOrDefault(mes, 0);

            meses.add(mes.format(
                    java.time.format.DateTimeFormatter
                            .ofPattern("MM/yy")));

            valores.add(acumulado);
        }

        graficoEvolucao.definirDados(
                meses.toArray(new String[0]),
                valores.stream().mapToInt(Integer::intValue).toArray());
    }

    private Color corLinha(int linha) {
        return temaEscuro ? new Color(26, 35, 54) : Color.WHITE;
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
        if (temaEscuro)
            FlatDarkLaf.setup();
        else
            FlatLightLaf.setup();
        FlatLaf.updateUI();
        Color fundoJanela = temaEscuro ? new Color(15, 23, 42) : FUNDO;
        Color fundoCartao = temaEscuro ? new Color(26, 35, 54) : Color.WHITE;
        Color corTexto = temaEscuro ? new Color(225, 232, 242) : TEXTO;
        Color borda = temaEscuro ? new Color(45, 59, 82) : new Color(222, 230, 240);
        painelConteudo.setBackground(fundoJanela);
        if (painelAreaPrincipal != null)
            painelAreaPrincipal.setBackground(fundoJanela);
        if (painelTelas != null)
            painelTelas.setBackground(fundoJanela);
        if (painelEstatisticas != null)
            painelEstatisticas.setBackground(fundoJanela);
        painelDashboard.setBackground(fundoJanela);
        painelRelatorios.setBackground(fundoJanela);
        painelCategorias.setBackground(fundoJanela);
        painelConfiguracoes.setBackground(fundoJanela);
        painelTopo.setBackground(fundoCartao);
        painelLateral.setBackground(temaEscuro ? new Color(10, 17, 32) : AZUL);
        painelLateral.repaint();
        for (JPanel painel : cartoesTema) {
            painel.setBackground(fundoCartao);
            if (painel instanceof Cartao)
                ((Cartao) painel).definirBorda(borda);
        }
        painelTopo.setBorder(bordaTopo(borda));

        for (JLabel rotulo : rotulosTema)
            rotulo.setForeground(corTexto);
        for (JLabel suave : rotulosSuaves)
            suave.setForeground(temaEscuro ? TEXTO_SUAVE_ESCURO : TEXTO_SUAVE);
        for (JLabel erro : rotulosErro)
            erro.setForeground(temaEscuro ? new Color(255, 150, 158) : new Color(183, 52, 62));

        Color[] coresValores = temaEscuro
                ? new Color[] { new Color(130, 189, 255), new Color(112, 220, 158),
                        new Color(255, 200, 114), new Color(255, 150, 158) }

                : new Color[] { AZUL_CLARO, VERDE,
                        new Color(217, 119, 6), VERMELHO };
        for (int i = 0; i < valoresEstatisticos.size(); i++)
            valoresEstatisticos.get(i).setForeground(
                    coresValores[i % coresValores.length]);
        botaoTema.setText(temaEscuro ? "☀ Tema claro" : "🌙 Tema escuro");
        botaoCadastrar.setBackground(temaEscuro ? new Color(46, 105, 184) : AZUL_CLARO);
        botaoCadastrar.setForeground(Color.WHITE);
        botaoExcluir.setBackground(temaEscuro ? new Color(185, 28, 28) : VERMELHO);
        botaoExcluir.setForeground(Color.WHITE);
        for (JButton primario : new JButton[] { botaoNovaCategoria, botaoCsv, botaoSalvarPreferencias }) {
            primario.setBackground(temaEscuro ? new Color(46, 105, 184) : AZUL_CLARO);
            primario.setForeground(Color.WHITE);
        }
        botaoExcluirCategoria.setBackground(temaEscuro ? new Color(185, 28, 28) : VERMELHO);
        botaoExcluirCategoria.setForeground(Color.WHITE);
        botaoSair.setBackground(new Color(127, 29, 29, 150));
        botaoSair.setForeground(new Color(254, 202, 202));
        personalizarTabela();
        for (JTable secundaria : tabelasSecundarias)
            estilizarTabela(secundaria);
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

                try {
                    new AutenticacaoApiClient().logout();
                } catch (Exception ex) {
                    // API indisponível: a sessão local já foi encerrada,
                    // então volta para o login mesmo assim.
                }

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
            public void insertUpdate(DocumentEvent e) {
                validarSeAtivo();
            }

            public void removeUpdate(DocumentEvent e) {
                validarSeAtivo();
            }

            public void changedUpdate(DocumentEvent e) {
                validarSeAtivo();
            }

        };
        campoNome.getDocument().addDocumentListener(validarTexto);
        campoDescricao.getDocument().addDocumentListener(validarTexto);
        comboStatus.addActionListener(e -> validarSeAtivo());
        campoBusca.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                reiniciarPaginacao();
            }

            public void removeUpdate(DocumentEvent e) {
                reiniciarPaginacao();
            }

            public void changedUpdate(DocumentEvent e) {
                reiniciarPaginacao();
            }

        });
        filtroCategoria.addActionListener(e -> reiniciarPaginacao());
        filtroStatus.addActionListener(e -> reiniciarPaginacao());
        botaoLimpar.addActionListener(e -> limparFormulario());
        botaoCadastrar.addActionListener(e -> cadastrar());
        botaoAlterar.addActionListener(e -> alterar());
        botaoExcluir.addActionListener(e -> excluir());
        tabela.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                selecionarProjeto();
        });
    }

    private void limparFormulario() {

        campoNome.setText("");
        campoDescricao.setText("");

        campoDataCriacao.setText(java.time.LocalDate.now().toString());

        campoDataCriacao.setEditable(false);

        campoPrazo.setText("");

        if (comboCategoria.getItemCount() > 0) {
            comboCategoria.setSelectedIndex(0);
        }

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

            modelo.addRow(new Object[] {
                    p.getId(),
                    p.getNome(),
                    p.getDescricao(),
                    p.getCategoria(),
                    p.getStatus(),
                    p.getDataCriacao() != null
                            ? p.getDataCriacao().toString()
                            : "Não informada",
                    p.getPrazo() != null
                            ? p.getPrazo().toString()
                            : "Não informado"
            });

        }
        atualizarTabelaCategorias();
    }

    private <T> void executar(String texto, Callable<T> operacao, Consumer<T> sucesso) {
        if (ocupado)
            return;
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
                }

                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    mostrarErro(e);
                }

                catch (ExecutionException e) {
                    mostrarErro(e.getCause());
                }

                catch (RuntimeException e) {
                    mostrarErro(e);
                }

            }

        }.execute();
    }

    private void definirOcupado(boolean valor) {
        ocupado = valor;
        for (JButton b : new JButton[] { botaoCadastrar, botaoAlterar, botaoExcluir, botaoLimpar, botaoAtualizar })
            b.setEnabled(!valor);
        campoNome.setEnabled(!valor);
        campoDescricao.setEnabled(!valor);
        comboCategoria.setEnabled(!valor);
        comboStatus.setEnabled(!valor);
        tabela.setEnabled(!valor);
    }

    private void mostrarErro(Throwable erro) {
        String detalhe = erro.getMessage();
        if (detalhe == null || detalhe.isBlank())
            detalhe = erro.getClass().getSimpleName();
        mensagem.setText("Falha na operação. Verifique se a API está executando.");
        JOptionPane.showMessageDialog(this, "Erro na comunicação com a API:\n" + detalhe, "Erro",
                JOptionPane.ERROR_MESSAGE);
    }

    private void selecionarProjeto() {
        int linha = tabela.getSelectedRow();
        if (linha == -1)
            return;
        linha = tabela.convertRowIndexToModel(linha);
        idSelecionado = Integer.parseInt(modelo.getValueAt(linha, 0).toString());
        campoNome.setText(Objects.toString(modelo.getValueAt(linha, 1), ""));
        campoDescricao.setText(Objects.toString(modelo.getValueAt(linha, 2), ""));
        comboCategoria.setSelectedItem(Objects.toString(modelo.getValueAt(linha, 3), ""));
        comboStatus.setSelectedItem(Objects.toString(modelo.getValueAt(linha, 4), ""));

        String dataCriacao = Objects.toString(
                modelo.getValueAt(linha, 5), "");

        String prazo = Objects.toString(
                modelo.getValueAt(linha, 6), "");

        campoDataCriacao.setText(
                dataCriacao.equals("Não informada") ? "" : dataCriacao);

        campoDataCriacao.setEditable(dataCriacao.equals("Não informada"));

        if (dataCriacao.equals("Não informada")) {
            campoDataCriacao.setToolTipText(
                    "Projeto antigo: informe a data de criação no formato AAAA-MM-DD.");
        } else {
            campoDataCriacao.setToolTipText(
                    "A data de criação não pode ser alterada.");
        }

        campoPrazo.setText(
                prazo.equals("Não informado") ? "" : prazo);

    }

    private void adicionarCampo(JPanel painel, int linha, String rotulo,
            JComponent componente, JLabel aviso) {
        // Rótulo acima do campo, ocupando toda a largura do formulário.
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridx = 0;
        g.weightx = 1;
        g.gridy = linhaFormulario++;
        g.insets = new Insets(8, 0, 5, 0);
        JLabel etiqueta = new JLabel(rotulo.replace(":", ""));
        etiqueta.setFont(fonte(Font.BOLD, 12.5f));
        etiqueta.setForeground(TEXTO);
        rotulosTema.add(etiqueta);
        painel.add(etiqueta, g);
        g.gridy = linhaFormulario++;
        g.insets = new Insets(0, 0, 0, 0);
        g.ipady = 7;
        painel.add(componente, g);
        g.ipady = 0;
        g.gridy = linhaFormulario++;
        g.insets = new Insets(2, 2, 0, 0);
        if (aviso != null) {
            painel.add(aviso, g);
        }
    }

    private void limparErros() {
        for (JComponent campo : new JComponent[] { campoNome, campoDescricao, comboStatus }) {
            campo.putClientProperty("JComponent.outline", null);
        }

        erroNome.setText(" ");
        erroDescricao.setText(" ");
        erroStatus.setText(" ");
    }

    private void validarSeAtivo() {
        if (validacaoAtivada)
            validarCampos();
    }

    private boolean validarCampos() {
        String nome = campoNome.getText().trim();
        String descricao = campoDescricao.getText().trim();
        String status = Objects.toString(comboStatus.getSelectedItem(), "");
        boolean nomeValido = !nome.isEmpty();
        boolean descricaoValida = descricao.length() >= 10;
        boolean statusValido = status.equals("Planejado")
                || status.equals("Em Desenvolvimento") || status.equals("Concluído") || status.equals("Cancelado");
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
            if (campoNome.getText().trim().isEmpty())
                campoNome.requestFocus();
            else if (campoDescricao.getText().trim().length() < 10)
                campoDescricao.requestFocus();
            else
                comboStatus.requestFocus();
            return null;
        }

        try {
            java.time.LocalDate dataCriacao = java.time.LocalDate.parse(
                    campoDataCriacao.getText().trim());

            java.time.LocalDate prazo = java.time.LocalDate.parse(
                    campoPrazo.getText().trim());

            if (prazo.isBefore(dataCriacao)) {
                JOptionPane.showMessageDialog(this,
                        "O prazo não pode ser anterior à data de criação.");
                return null;
            }

            return new Projeto(
                    id,
                    campoNome.getText().trim(),
                    campoDescricao.getText().trim(),
                    Objects.toString(comboCategoria.getSelectedItem(), ""),
                    Objects.toString(comboStatus.getSelectedItem(), ""),
                    dataCriacao,
                    prazo);

        } catch (java.time.format.DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this,
                    "Informe datas válidas no formato AAAA-MM-DD.\n"
                            + "Exemplo: 2026-12-15.");
            return null;
        }

    }

    private void cadastrar() {
        if (ocupado)
            return;
        Projeto projeto = lerFormulario(0);
        if (projeto == null)
            return;
        executar("Cadastrando projeto...", () -> api.cadastrar(projeto), cadastrado -> {
            JOptionPane.showMessageDialog(this, "Projeto cadastrado com sucesso!\nID: " + cadastrado.getId());
            limparFormulario();
            carregarTabela();
        });
    }

    private void alterar() {
        if (ocupado)
            return;
        if (idSelecionado == -1) {
            JOptionPane.showMessageDialog(this, "Selecione um projeto na tabela para alterar.");
            return;
        }

        Projeto projeto = lerFormulario(idSelecionado);
        if (projeto == null)
            return;
        executar("Alterando projeto...", () -> api.alterar(projeto), alterado -> {
            JOptionPane.showMessageDialog(this,
                    alterado == null ? "Projeto não encontrado." : "Projeto alterado com sucesso!");
            limparFormulario();
            carregarTabela();
        });
    }

    private void excluir() {
        if (ocupado)
            return;
        if (idSelecionado == -1) {
            JOptionPane.showMessageDialog(this, "Selecione um projeto na tabela para excluir.");
            return;
        }

        final int id = idSelecionado;
        int resposta = JOptionPane.showConfirmDialog(this,
                "Tem certeza que deseja excluir o projeto?\nID: " + id,
                "Confirmar exclusão", JOptionPane.YES_NO_OPTION);
        if (resposta != JOptionPane.YES_OPTION)
            return;
        executar("Excluindo projeto...", () -> api.excluir(id), excluiu -> {
            JOptionPane.showMessageDialog(this, excluiu ? "Projeto excluído com sucesso!" : "Projeto não encontrado.");
            limparFormulario();
            carregarTabela();
        });
    }

    public static void main(String[] args) {
        if (PREFERENCIAS.getBoolean(CHAVE_TEMA, false))
            FlatDarkLaf.setup();
        else
            FlatLightLaf.setup();
        SwingUtilities.invokeLater(() -> new LoginAdministrador().setVisible(true));
    }

}
