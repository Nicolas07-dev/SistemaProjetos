package service;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import model.Projeto;

public class CategoriaService {
    private final Path arquivo = Paths.get("dados", "categorias.txt");
    private final List<String> categorias = new ArrayList<>();
    public synchronized void carregar(List<Projeto> projetos) throws Exception {
        categorias.clear();
        if (Files.exists(arquivo)) {
            for (String linha : Files.readAllLines(arquivo, StandardCharsets.UTF_8)) {
                if (!linha.trim().isEmpty() && !contem(linha.trim())) categorias.add(linha.trim());
            }
        } else {
            categorias.addAll(Arrays.asList("Web", "Software", "Mobile", "Outro"));
        }
        // Compatibilidade com projetos criados antes da gestão de categorias.
        for (Projeto projeto : projetos) {
            String nome = projeto.getCategoria();
            if (nome != null && !nome.trim().isEmpty() && !contem(nome.trim())) categorias.add(nome.trim());
        }
        salvar();
    }
    public synchronized List<String> listar() { return new ArrayList<>(categorias); }
    public synchronized boolean contem(String nome) {
        for (String categoria : categorias) if (categoria.equalsIgnoreCase(nome)) return true;
        return false;
    }
    public static String validarNome(String nome) {
        if (nome == null || nome.trim().isEmpty()) throw new IllegalArgumentException("Nome da categoria obrigatório");
        nome = nome.trim();
        if (nome.length() > 60 || nome.indexOf('\n') >= 0 || nome.indexOf('\r') >= 0)
            throw new IllegalArgumentException("Nome da categoria inválido (máximo 60 caracteres)");
        return nome;
    }
    public synchronized void cadastrar(String nome) throws Exception {
        nome = validarNome(nome);
        if (contem(nome)) throw new IllegalStateException("Categoria já cadastrada");
        categorias.add(nome);
        salvar();
    }
    public synchronized void renomear(String anterior, String novo, List<Projeto> projetos, ProjetoService service) throws Exception {
        anterior = validarNome(anterior); novo = validarNome(novo);
        int indice = indice(anterior);
        if (indice < 0) throw new NoSuchElementException("Categoria não encontrada");
        if (!anterior.equalsIgnoreCase(novo) && contem(novo)) throw new IllegalStateException("Categoria já cadastrada");
        String nomeAntigo = categorias.get(indice);
        categorias.set(indice, novo);
        for (Projeto projeto : projetos) if (nomeAntigo.equalsIgnoreCase(projeto.getCategoria())) projeto.setCategoria(novo);
        service.salvar();
        salvar();
    }
    public synchronized void excluir(String nome, List<Projeto> projetos) throws Exception {
        int indice = indice(validarNome(nome));
        if (indice < 0) throw new NoSuchElementException("Categoria não encontrada");
        for (Projeto projeto : projetos) if (nome.equalsIgnoreCase(projeto.getCategoria()))
            throw new IllegalStateException("Não é possível excluir uma categoria utilizada por projetos");
        categorias.remove(indice);
        salvar();
    }
    private int indice(String nome) {
        for (int i = 0; i < categorias.size(); i++) if (categorias.get(i).equalsIgnoreCase(nome)) return i;
        return -1;
    }
    private void salvar() throws Exception {
        Files.createDirectories(arquivo.getParent());
        Files.write(arquivo, categorias, StandardCharsets.UTF_8);
    }
}
