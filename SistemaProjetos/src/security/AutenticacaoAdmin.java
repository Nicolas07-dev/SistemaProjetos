package security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Credenciais locais para o único administrador deste aplicativo Swing. */
public final class AutenticacaoAdmin {
    private static final Preferences PREFS = Preferences.userNodeForPackage(AutenticacaoAdmin.class);
    private static final int ITERACOES = 210_000;
    private static final int BITS = 256;
    private AutenticacaoAdmin() {}

    public static boolean configurado() {
        return !PREFS.get("hash", "").isEmpty() && !PREFS.get("salt", "").isEmpty();
    }

    public static String usuario() { return PREFS.get("usuario", "admin"); }

    public static synchronized void cadastrarInicial(String usuario, char[] senha) throws Exception {
        if (configurado()) throw new IllegalStateException("Administrador já cadastrado.");
        String nome = usuario == null ? "" : usuario.trim();
        if (nome.length() < 3 || nome.length() > 40 || nome.contains(" "))
            throw new IllegalArgumentException("Usuário deve ter de 3 a 40 caracteres, sem espaços.");
        if (senha == null || senha.length < 8)
            throw new IllegalArgumentException("A senha deve ter pelo menos 8 caracteres.");
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        byte[] hash = derivar(senha, salt);
        PREFS.put("usuario", nome);
        PREFS.put("salt", Base64.getEncoder().encodeToString(salt));
        PREFS.put("hash", Base64.getEncoder().encodeToString(hash));
        PREFS.flush();
        Arrays.fill(hash, (byte) 0);
    }

    public static boolean validar(String usuario, char[] senha) {
        if (!configurado() || usuario == null || senha == null || !usuario().equals(usuario.trim())) return false;
        try {
            byte[] salt = Base64.getDecoder().decode(PREFS.get("salt", ""));
            byte[] esperado = Base64.getDecoder().decode(PREFS.get("hash", ""));
            byte[] calculado = derivar(senha, salt);
            boolean valido = MessageDigest.isEqual(esperado, calculado);
            Arrays.fill(calculado, (byte) 0);
            return valido;
        } catch (Exception ex) { return false; }
    }

    private static byte[] derivar(char[] senha, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(senha, salt, ITERACOES, BITS);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        finally { spec.clearPassword(); }
    }
}
