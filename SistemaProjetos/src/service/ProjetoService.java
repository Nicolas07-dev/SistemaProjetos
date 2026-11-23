package service;

import dao.ProjetoCSV;
import java.util.ArrayList;
import java.util.List;
import util.SistemaLogs;
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

    public void validarCategoria(Projeto projeto, CategoriaService categorias) {
        if (projeto.getCategoria() == null ||
                projeto.getCategoria().trim().isEmpty()) {
            throw new IllegalArgumentException("Categoria obrigatória");
        }

        String categoria = projeto.getCategoria().trim();
        String cadastrada = categorias.nomeCadastrado(categoria);

        if (cadastrada == null) {
            throw new IllegalArgumentException(
                    "Categoria não cadastrada: " + categoria);
        }

        // Grava o nome exatamente como está cadastrado (ex.: "web" -> "Web").
        projeto.setCategoria(cadastrada);
    }

    public void validar(Projeto projeto) {
        if (projeto == null) {
            throw new IllegalArgumentException("Projeto obrigatório");
        }
        if (projeto.getId() <= 0) {
            throw new IllegalArgumentException("O ID deve ser um número positivo");
        }
        if (projeto.getNome() == null || projeto.getNome().trim().isEmpty()) {
            throw new IllegalArgumentException("Nome obrigatório");
        }

        String nome = projeto.getNome().trim();

        if (nome.length() < 3 || nome.length() > 100) {
            throw new IllegalArgumentException(
                    "O nome deve ter entre 3 e 100 caracteres!");
        }

        projeto.setNome(nome);

        if (projeto.getDescricao() == null || projeto.getDescricao().trim().length() < 10) {
            throw new IllegalArgumentException("A descrição deve ter pelo menos 10 caracteres");
        }

        String descricao = projeto.getDescricao().trim();

        if (descricao.length() < 10 || descricao.length() > 500) {
            throw new IllegalArgumentException(
                    "A descrição deve ter entre 10 e 500 caracteres! ");
        }

        projeto.setDescricao(descricao);

        String status = projeto.getStatus();

        if (status == null) {
            throw new IllegalArgumentException("Status obrigatório");
        }

        status = status.trim();

        String statusValido = null;

        for (String opcao : new String[] {
                "Planejado", "Em Desenvolvimento", "Concluído", "Cancelado" }) {
            if (opcao.equalsIgnoreCase(status)) {
                statusValido = opcao;
                break;
            }
        }

        if (statusValido == null) {
            throw new IllegalArgumentException(
                    "Status inválido. Use Planejado, Em Desenvolvimento, Concluído ou Cancelado");
        }

        projeto.setStatus(statusValido);

        if (projeto.getDataCriacao() == null) {
            throw new IllegalArgumentException(
                    "Data de criação obrigatória");
        }

        if (projeto.getPrazo() == null) {
            throw new IllegalArgumentException(
                    "Prazo obrigatório");
        }

        if (projeto.getPrazo().isBefore(projeto.getDataCriacao())) {
            throw new IllegalArgumentException(
                    "O prazo não pode ser anterior à data de criação");
        }

    }

    public boolean adicionar(Projeto projeto) {
        validar(projeto);
        if (buscarPorId(projeto.getId()) != null) {
            return false;
        }
        projetos.add(projeto);

        SistemaLogs.audit(
                "PROJETO " + projeto.getId() + " CRIADO");

        return true;
    }

    public boolean alterar(Projeto projetoAtualizado) {
        Projeto projeto = buscarPorId(projetoAtualizado.getId());

        if (projeto == null) {
            return false;
        }

        if (projeto.getDataCriacao() != null) {
            projetoAtualizado.setDataCriacao(projeto.getDataCriacao());
        }

        validar(projetoAtualizado);

        projeto.setNome(projetoAtualizado.getNome());
        projeto.setDescricao(projetoAtualizado.getDescricao());
        projeto.setCategoria(projetoAtualizado.getCategoria());
        projeto.setStatus(projetoAtualizado.getStatus());
        projeto.setDataCriacao(projetoAtualizado.getDataCriacao());
        projeto.setPrazo(projetoAtualizado.getPrazo());

        SistemaLogs.audit(
                "PROJETO " + projeto.getId() + " ALTERADO");

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

            SistemaLogs.audit(
                    "PROJETO " + projeto.getId() + " EXCLUIDO");

            return true;
        }
        return false;
    }
}
