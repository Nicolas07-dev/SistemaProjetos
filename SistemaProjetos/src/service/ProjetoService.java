package service;
import dao.ProjetoCSV;
import java.util.ArrayList;
import java.util.List;
import model.Projeto;

public class ProjetoService {
    private List<Projeto> projetos;
    private ProjetoCSV dao;
    public ProjetoService() {
        projetos = new ArrayList<>();
        dao = new ProjetoCSV();
    }

    public void carregar() throws Exception {
        projetos = dao.listar();
    }

    public void salvar() throws Exception {
        dao.salvar(projetos);
    }

    // Regras centralizadas para POST e PUT (e para outros clientes do service).
    public void validar(Projeto projeto) {
        if (projeto == null) {
            throw new IllegalArgumentException("Projeto obrigatório");
        }
        if (projeto.getNome() == null || projeto.getNome().trim().isEmpty()) {
            throw new IllegalArgumentException("Nome obrigatório");
        }
        if (projeto.getDescricao() == null || projeto.getDescricao().trim().length() < 10) {
            throw new IllegalArgumentException("A descrição deve ter pelo menos 10 caracteres");
        }
        String status = projeto.getStatus();
        if (status == null || !(status.equals("Planejado")
                || status.equals("Em desenvolvimento")
                || status.equals("Concluído"))) {
            throw new IllegalArgumentException("Status inválido. Use Planejado, Em desenvolvimento ou Concluído");
        }
    }

    public boolean adicionar(Projeto projeto) {
        validar(projeto);
        if (buscarPorId(projeto.getId()) != null) {
            return false;
        }
        projetos.add(projeto);
        return true;
    }

    public boolean alterar(Projeto projetoAtualizado) {
        validar(projetoAtualizado);
        Projeto projeto = buscarPorId(projetoAtualizado.getId());
        if (projeto == null) {
            return false;
        }
        projeto.setNome(projetoAtualizado.getNome());
        projeto.setDescricao(projetoAtualizado.getDescricao());
        projeto.setCategoria(projetoAtualizado.getCategoria());
        projeto.setStatus(projetoAtualizado.getStatus());
        return true;
    }

    public List<Projeto> listar() {
        return projetos;
    }

    public Projeto buscarPorId(int id) {
        for (Projeto projeto : projetos) {
            if (projeto.getId() == id) {
                return projeto;
            }
        }
        return null;
    }

    public List<Projeto> buscarPorCategoria(String categoria) {
        List<Projeto> resultado = new ArrayList<>();
        for (Projeto projeto : projetos) {
            if (projeto.getCategoria() != null && projeto.getCategoria().equalsIgnoreCase(categoria)) {
                resultado.add(projeto);
            }
        }
        return resultado;
    }

    public List<Projeto> buscarPorStatus(String status) {
        List<Projeto> resultado = new ArrayList<>();
        for (Projeto projeto : projetos) {
            if (projeto.getStatus() != null && projeto.getStatus().equalsIgnoreCase(status)) {
                resultado.add(projeto);
            }
        }
        return resultado;
    }

    public boolean removerPorId(int id) {
        Projeto projeto = buscarPorId(id);
        if (projeto != null) {
            projetos.remove(projeto);
            return true;
        }
        return false;
    }
}
