package view;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Arrays;
import javax.swing.*;
import com.formdev.flatlaf.FlatLightLaf;
import security.AutenticacaoAdmin;
import util.SistemaLogs;
import client.AutenticacaoApiClient;

public class LoginAdministrador extends JFrame {
    private static final Color AZUL_ESCURO = new Color(11, 22, 48);
    private static final Color AZUL_MEIO = new Color(20, 48, 107);
    private static final Color AZUL_PRIMARIO = new Color(37, 99, 235);
    private static final Color TEXTO_CLARO = new Color(30, 41, 59);
    private static final Color TEXTO_SUAVE_CLARO = new Color(100, 116, 139);
    // true quando o tema escuro do FlatLaf está ativo (o login acompanha o tema salvo)
    private final boolean escuro = com.formdev.flatlaf.FlatLaf.isLafDark();
    private final Color TEXTO = escuro ? new Color(226, 232, 240) : TEXTO_CLARO;
    private final Color TEXTO_SUAVE = escuro ? new Color(148, 163, 184) : TEXTO_SUAVE_CLARO;
    private final Color FUNDO_FORM = escuro ? new Color(15, 23, 42) : Color.WHITE;

    static {
        UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));
        UIManager.put("Button.arc", 12);
        UIManager.put("Component.arc", 12);
        UIManager.put("TextComponent.arc", 12);
    }

    private final JTextField campoUsuario = new JTextField(20);
    private final JPasswordField campoSenha = new JPasswordField(20);
    private final JPasswordField campoConfirmacao = new JPasswordField(20);
    private final boolean primeiroAcesso;
    private int falhas;

    private static Font fonte(int estilo, float tamanho) {
        Font base = UIManager.getFont("defaultFont");
        if (base == null)
            base = new Font("SansSerif", Font.PLAIN, 13);
        return base.deriveFont(estilo, tamanho);
    }

    public LoginAdministrador() {
        primeiroAcesso = !AutenticacaoAdmin.configurado();
        setTitle(primeiroAcesso ? "Criar administrador" : "Login - Sistema de Projetos");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, primeiroAcesso ? 620 : 560);
        setMinimumSize(getSize());
        setLocationRelativeTo(null);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(criarPainelMarca(), BorderLayout.WEST);
        raiz.add(criarPainelFormulario(), BorderLayout.CENTER);
        setContentPane(raiz);
    }

    /** Lado esquerdo: identidade visual do sistema. */
    private JPanel criarPainelMarca() {
        JPanel marca = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new LinearGradientPaint(0, 0, getWidth(), getHeight(),
                        new float[] { 0f, .6f, 1f },
                        new Color[] { AZUL_ESCURO, AZUL_MEIO, new Color(30, 74, 154) }));
                g2.fillRect(0, 0, getWidth(), getHeight());
                // formas decorativas
                g2.setColor(new Color(255, 255, 255, 14));
                g2.fill(new Ellipse2D.Double(getWidth() - 170, -90, 280, 280));
                g2.setColor(new Color(255, 255, 255, 10));
                g2.fill(new Ellipse2D.Double(-110, getHeight() - 190, 320, 320));
                g2.setColor(new Color(59, 130, 246, 40));
                g2.fill(new Ellipse2D.Double(getWidth() - 120, getHeight() - 150, 170, 170));
                g2.dispose();
            }
        };
        marca.setLayout(new BoxLayout(marca, BoxLayout.Y_AXIS));
        marca.setPreferredSize(new Dimension(414, 0));
        marca.setBorder(BorderFactory.createEmptyBorder(56, 64, 40, 64));

        marca.add(new IconeLogo(58));
        marca.add(Box.createVerticalStrut(28));

        JLabel nome = new JLabel("Sistema de Projetos");
        nome.setFont(fonte(Font.BOLD, 34f));
        nome.setForeground(Color.WHITE);
        nome.setAlignmentX(Component.LEFT_ALIGNMENT);
        marca.add(nome);
        marca.add(Box.createVerticalStrut(10));

        JLabel frase = new JLabel("<html>Organize, acompanhe e entregue<br>seus projetos em um só lugar.</html>");
        frase.setFont(fonte(Font.PLAIN, 16f));
        frase.setForeground(new Color(191, 207, 232));
        frase.setAlignmentX(Component.LEFT_ALIGNMENT);
        marca.add(frase);
        marca.add(Box.createVerticalStrut(36));

        for (String item : new String[] { "Cadastro e acompanhamento de projetos",
                "Dashboard com indicadores em tempo real", "Relatórios exportáveis em CSV e PDF" }) {
            marca.add(linhaBeneficio(item));
            marca.add(Box.createVerticalStrut(14));
        }

        marca.add(Box.createVerticalGlue());
        JLabel rodape = new JLabel("Acesso restrito ao administrador");
        rodape.setFont(fonte(Font.PLAIN, 12f));
        rodape.setForeground(new Color(140, 163, 201));
        rodape.setAlignmentX(Component.LEFT_ALIGNMENT);
        marca.add(rodape);
        return marca;
    }

    private JComponent linhaBeneficio(String texto) {
        JPanel linha = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        linha.setOpaque(false);
        linha.setAlignmentX(Component.LEFT_ALIGNMENT);
        linha.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        JComponent marcador = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(59, 130, 246, 90));
                g2.fillOval(0, 2, 20, 20);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawPolyline(new int[] { 6, 9, 14 }, new int[] { 12, 15, 9 }, 3);
                g2.dispose();
            }
        };
        marcador.setPreferredSize(new Dimension(20, 24));
        JLabel rotulo = new JLabel("  " + texto);
        rotulo.setFont(fonte(Font.PLAIN, 13.5f));
        rotulo.setForeground(new Color(226, 234, 248));
        linha.add(marcador);
        linha.add(rotulo);
        return linha;
    }

    /** Lado direito: formulário de acesso. */
    private JPanel criarPainelFormulario() {
        JPanel lado = new JPanel(new GridBagLayout());
        lado.setBackground(FUNDO_FORM);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setPreferredSize(new Dimension(390, primeiroAcesso ? 480 : 410));

        JLabel titulo = new JLabel(primeiroAcesso ? "Criar administrador" : "Bem-vindo de volta");
        titulo.setFont(fonte(Font.BOLD, 28f));
        titulo.setForeground(TEXTO);
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(titulo);
        form.add(Box.createVerticalStrut(8));

        JLabel subtitulo = new JLabel(
                primeiroAcesso ? "Defina o usuário e a senha do administrador" : "Entre com sua conta de administrador");
        subtitulo.setFont(fonte(Font.PLAIN, 14.5f));
        subtitulo.setForeground(TEXTO_SUAVE);
        subtitulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(subtitulo);
        form.add(Box.createVerticalStrut(30));

        campoUsuario.setText(primeiroAcesso ? "admin" : AutenticacaoAdmin.usuario());
        campoUsuario.putClientProperty("JTextField.placeholderText", "Seu usuário");
        campoSenha.putClientProperty("JTextField.placeholderText", "Sua senha");
        campoConfirmacao.putClientProperty("JTextField.placeholderText", "Repita a senha");
        campoSenha.putClientProperty("JPasswordField.showRevealButton", true);
        campoConfirmacao.putClientProperty("JPasswordField.showRevealButton", true);

        adicionarCampo(form, "Usuário", campoUsuario);
        adicionarCampo(form, "Senha", campoSenha);
        if (primeiroAcesso)
            adicionarCampo(form, "Confirmar senha", campoConfirmacao);

        JButton entrar = new JButton(primeiroAcesso ? "Criar administrador e entrar" : "Entrar");
        entrar.setBackground(AZUL_PRIMARIO);
        entrar.setForeground(Color.WHITE);
        entrar.setFont(fonte(Font.BOLD, 14f));
        entrar.setFocusPainted(false);
        entrar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        entrar.putClientProperty("FlatLaf.style",
                "arc: 14; borderWidth: 0; focusWidth: 0; hoverBackground: #1D4ED8; pressedBackground: #1E40AF");
        entrar.setAlignmentX(Component.LEFT_ALIGNMENT);
        entrar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        entrar.setPreferredSize(new Dimension(390, 48));
        entrar.addActionListener(e -> autenticar());
        form.add(Box.createVerticalStrut(10));
        form.add(entrar);
        getRootPane().setDefaultButton(entrar);

        form.add(Box.createVerticalGlue());
        lado.add(form);
        return lado;
    }

    private void adicionarCampo(JPanel painel, String rotulo, JComponent campo) {
        JLabel label = new JLabel(rotulo);
        label.setFont(fonte(Font.BOLD, 12.5f));
        label.setForeground(TEXTO);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        painel.add(label);
        painel.add(Box.createVerticalStrut(7));
        campo.setAlignmentX(Component.LEFT_ALIGNMENT);
        campo.setFont(fonte(Font.PLAIN, 14f));
        campo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        campo.setPreferredSize(new Dimension(390, 46));
        campo.putClientProperty("FlatLaf.style",
                "arc: 12; margin: 4,12,4,12; borderColor: " + (escuro ? "#3D4F6B" : "#CBD5E1")
                        + (escuro ? "; background: #1E293B" : "")
                        + "; focusedBorderColor: #3B82F6; focusWidth: 1");
        painel.add(campo);
        painel.add(Box.createVerticalStrut(18));
    }

    /** Ícone da marca desenhado em Java2D (4 quadrados arredondados). */
    private static class IconeLogo extends JComponent {
        private final int tamanho;

        IconeLogo(int tamanho) {
            this.tamanho = tamanho;
            setPreferredSize(new Dimension(tamanho, tamanho));
            setMaximumSize(new Dimension(tamanho, tamanho));
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, new Color(96, 165, 250), tamanho, tamanho, AZUL_PRIMARIO));
            g2.fill(new RoundRectangle2D.Double(0, 0, tamanho, tamanho, 16, 16));
            g2.setColor(Color.WHITE);
            int m = tamanho / 5, c = (tamanho - m * 2 - 6) / 2;
            g2.fill(new RoundRectangle2D.Double(m, m, c, c, 5, 5));
            g2.setColor(new Color(255, 255, 255, 190));
            g2.fill(new RoundRectangle2D.Double(m + c + 6, m, c, c, 5, 5));
            g2.fill(new RoundRectangle2D.Double(m, m + c + 6, c, c, 5, 5));
            g2.setColor(new Color(255, 255, 255, 120));
            g2.fill(new RoundRectangle2D.Double(m + c + 6, m + c + 6, c, c, 5, 5));
            g2.dispose();
        }
    }

    private void autenticar() {
        char[] senha = campoSenha.getPassword();
        char[] confirmacao = primeiroAcesso ? campoConfirmacao.getPassword() : new char[0];
        try {
            if (primeiroAcesso) {
                if (!Arrays.equals(senha, confirmacao)) {
                    JOptionPane.showMessageDialog(this, "As senhas não coincidem.", "Atenção",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }
                AutenticacaoAdmin.cadastrarInicial(campoUsuario.getText(), senha);
            } else if (!AutenticacaoAdmin.validar(campoUsuario.getText(), senha)) {
                falhas++;
                JOptionPane.showMessageDialog(this, "Usuário ou senha incorretos.", "Acesso negado",
                        JOptionPane.ERROR_MESSAGE);
                campoSenha.setText("");
                if (falhas >= 5) {
                    JOptionPane.showMessageDialog(this,
                            "Muitas tentativas incorretas. Abra a tela novamente para tentar de novo.");
                    dispose();
                }
                return;
            }

            new AutenticacaoApiClient().login(
                    campoUsuario.getText().trim(), senha);

            dispose();
            new TelaProjetos().setVisible(true);
        } catch (Exception ex) {
            String detalhe = ex.getMessage();
            if (ex instanceof java.net.ConnectException || ex.getCause() instanceof java.net.ConnectException) {
                detalhe = "Não foi possível conectar à API.\nInicie a Api.java (porta 7070) e tente novamente.";
            } else if (detalhe == null || detalhe.isBlank()) {
                detalhe = ex.getClass().getSimpleName();
            }
            JOptionPane.showMessageDialog(this, detalhe, "Não foi possível entrar", JOptionPane.ERROR_MESSAGE);
        } finally {
            Arrays.fill(senha, '\0');
            Arrays.fill(confirmacao, '\0');
        }
    }

    public static void main(String[] args) {
        if (java.util.prefs.Preferences.userNodeForPackage(TelaProjetos.class).getBoolean("temaEscuro", false))
            com.formdev.flatlaf.FlatDarkLaf.setup();
        else
            FlatLightLaf.setup();
        UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));

        SwingUtilities.invokeLater(() -> {
            new LoginAdministrador().setVisible(true);
        });
    }
}
