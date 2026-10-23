package view;

import java.awt.*;
import java.util.Arrays;
import javax.swing.*;
import com.formdev.flatlaf.FlatLightLaf;
import security.AutenticacaoAdmin;

/** Tela de entrada; no primeiro acesso permite criar o administrador local. */
public class LoginAdministrador extends JFrame {
    private final JTextField campoUsuario = new JTextField(20);
    private final JPasswordField campoSenha = new JPasswordField(20);
    private final JPasswordField campoConfirmacao = new JPasswordField(20);
    private final boolean primeiroAcesso;
    private int falhas;

    public LoginAdministrador() {
        primeiroAcesso = !AutenticacaoAdmin.configurado();
        setTitle(primeiroAcesso ? "Criar administrador" : "Login - Sistema de Projetos");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(460, primeiroAcesso ? 510 : 425);
        setMinimumSize(getSize());
        setLocationRelativeTo(null);
        JPanel pagina = new JPanel(new GridBagLayout());
        pagina.setBackground(new Color(240, 244, 249));
        JPanel cartao = new JPanel();
        cartao.setLayout(new BoxLayout(cartao, BoxLayout.Y_AXIS));
        cartao.setBackground(Color.WHITE);
        cartao.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 230, 242)),
            BorderFactory.createEmptyBorder(28, 35, 28, 35)));
        JLabel titulo = new JLabel("Sistema de Projetos");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 23));
        titulo.setForeground(new Color(17, 43, 78));
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        cartao.add(titulo);
        cartao.add(Box.createVerticalStrut(8));
        JLabel subtitulo = new JLabel(primeiroAcesso ? "Configure seu administrador" : "Entre com sua conta de administrador");
        subtitulo.setForeground(new Color(90, 105, 126));
        subtitulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        cartao.add(subtitulo);
        cartao.add(Box.createVerticalStrut(24));
        campoUsuario.setText(primeiroAcesso ? "admin" : AutenticacaoAdmin.usuario());
        adicionarCampo(cartao, "Usuário", campoUsuario);
        adicionarCampo(cartao, "Senha", campoSenha);
        if (primeiroAcesso) adicionarCampo(cartao, "Confirmar senha", campoConfirmacao);
        JButton entrar = new JButton(primeiroAcesso ? "Criar administrador e entrar" : "Entrar");
        entrar.setBackground(new Color(38, 96, 165));
        entrar.setForeground(Color.WHITE);
        entrar.setFont(new Font("SansSerif", Font.BOLD, 14));
        entrar.setAlignmentX(Component.LEFT_ALIGNMENT);
        entrar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        entrar.addActionListener(e -> autenticar());
        cartao.add(Box.createVerticalStrut(8));
        cartao.add(entrar);
        getRootPane().setDefaultButton(entrar);
        pagina.add(cartao);
        setContentPane(pagina);
    }

    private void adicionarCampo(JPanel painel, String rotulo, JComponent campo) {
        JLabel label = new JLabel(rotulo);
        label.setFont(new Font("SansSerif", Font.BOLD, 12));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        painel.add(label);
        painel.add(Box.createVerticalStrut(6));
        campo.setAlignmentX(Component.LEFT_ALIGNMENT);
        campo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 39));
        painel.add(campo);
        painel.add(Box.createVerticalStrut(15));
    }

    private void autenticar() {
        char[] senha = campoSenha.getPassword();
        char[] confirmacao = primeiroAcesso ? campoConfirmacao.getPassword() : new char[0];
        try {
            if (primeiroAcesso) {
                if (!Arrays.equals(senha, confirmacao)) {
                    JOptionPane.showMessageDialog(this, "As senhas não coincidem.", "Atenção", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                AutenticacaoAdmin.cadastrarInicial(campoUsuario.getText(), senha);
            } else if (!AutenticacaoAdmin.validar(campoUsuario.getText(), senha)) {
                falhas++;
                JOptionPane.showMessageDialog(this, "Usuário ou senha incorretos.", "Acesso negado", JOptionPane.ERROR_MESSAGE);
                campoSenha.setText("");
                if (falhas >= 5) {
                    JOptionPane.showMessageDialog(this, "Muitas tentativas incorretas. Abra a tela novamente para tentar de novo.");
                    dispose();
                }
                return;
            }
            dispose();
            new TelaProjetos().setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Não foi possível entrar", JOptionPane.ERROR_MESSAGE);
        } finally {
            Arrays.fill(senha, '\0');
            Arrays.fill(confirmacao, '\0');
        }
    }
}
