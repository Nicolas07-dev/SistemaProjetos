const API_URL = "http://localhost:7070/api/projetos";

const listaProjetos = document.getElementById("listaProjetos");
const mensagem = document.getElementById("mensagem");
const formProjeto = document.getElementById("formProjeto");

const tituloFormulario = document.getElementById("tituloFormulario");
const btnSalvar = document.getElementById("btnSalvar");
const btnCancelar = document.getElementById("btnCancelar");

// null significa que o formulário está em modo de cadastro.
let idEmEdicao = null;

let ocupado = false;

function definirOcupado(valor) {
    ocupado = valor;

    document.querySelectorAll("input, textarea, select, button")
        .forEach(elemento => {
            elemento.disabled = valor;
        });
}

async function listarProjetos() {
    mensagem.textContent = "Carregando projetos...";

    try {
        const resposta = await fetch(API_URL);

        if (!resposta.ok) {
            throw new Error("Erro ao consultar a API.");
        }

        const projetos = await resposta.json();

        exibirProjetos(projetos);

        mensagem.textContent = "Projetos carregados com sucesso.";

    } catch (erro) {
        mensagem.textContent =
            "Não foi possível carregar os projetos. " +
            "Recarregue a página para tentar novamente.";

        console.error(erro);
    }
}

function exibirProjetos(projetos) {
    listaProjetos.replaceChildren();

    if (projetos.length === 0) {
        const mensagemVazia = document.createElement("p");

        mensagemVazia.textContent = "Nenhum projeto cadastrado.";

        listaProjetos.appendChild(mensagemVazia);

        return;
    }

    projetos.forEach(projeto => {
        const card = document.createElement("article");
        card.classList.add("projeto");

        const titulo = document.createElement("h3");
        titulo.textContent = projeto.nome;

        const descricao = document.createElement("p");
        descricao.textContent = projeto.descricao;

        const categoria = document.createElement("p");
        categoria.textContent = "Categoria: " + projeto.categoria;

        const status = document.createElement("p");
        status.textContent = "Status: " + projeto.status;

        const botaoEditar = document.createElement("button");
        botaoEditar.type = "button";
        botaoEditar.textContent = "Editar";
        botaoEditar.disabled = ocupado;

        botaoEditar.addEventListener(
            "click",
            () => editarProjeto(projeto)
        );

        const botaoExcluir = document.createElement("button");
        botaoExcluir.type = "button";
        botaoExcluir.textContent = "Excluir";
        botaoExcluir.disabled = ocupado;

        botaoExcluir.addEventListener(
            "click",
            () => excluirProjeto(projeto.id)
        );

        card.append(
            titulo,
            descricao,
            categoria,
            status,
            botaoEditar,
            botaoExcluir
        );

        listaProjetos.appendChild(card);
    });
}

// Preenche o formulário com o projeto selecionado.
function editarProjeto(projeto) {
    if (ocupado) {
        return;
    }

    idEmEdicao = projeto.id;

    document.getElementById("nome").value = projeto.nome ?? "";
    document.getElementById("descricao").value = projeto.descricao ?? "";
    document.getElementById("categoria").value = projeto.categoria ?? "";

    const campoStatus = document.getElementById("status");
    const valorStatus = projeto.status ?? "Planejado";

    // Preserva status antigos que não estejam nas opções.
    if (![...campoStatus.options].some(
        opcao => opcao.value === valorStatus
    )) {
        campoStatus.add(new Option(valorStatus, valorStatus));
    }

    campoStatus.value = valorStatus;

    tituloFormulario.textContent = "Editar projeto";
    btnSalvar.textContent = "Salvar alterações";
    btnCancelar.hidden = false;

    document.getElementById("nome").focus();

    formProjeto.scrollIntoView({
        behavior: "smooth",
        block: "start"
    });
}

// Volta ao modo de cadastro.
function cancelarEdicao() {
    idEmEdicao = null;

    formProjeto.reset();

    tituloFormulario.textContent = "Cadastrar projeto";
    btnSalvar.textContent = "Cadastrar projeto";
    btnCancelar.hidden = true;
}

// Cadastra com POST ou altera com PUT.
formProjeto.addEventListener("submit", async evento => {
    evento.preventDefault();

    if (ocupado) {
        return;
    }

    const projeto = {
        nome: document.getElementById("nome").value.trim(),
        descricao: document.getElementById("descricao").value,
        categoria: document.getElementById("categoria").value,
        status: document.getElementById("status").value
    };

    if (!projeto.nome) {
        alert("Informe o nome.");
        return;
    }

    const editando = idEmEdicao !== null;

    const url = editando
        ? `${API_URL}/${idEmEdicao}`
        : API_URL;

    definirOcupado(true);

    try {
        const resposta = await fetch(url, {
            method: editando ? "PUT" : "POST",

            headers: {
                "Content-Type": "application/json"
            },

            body: JSON.stringify(projeto)
        });

        if (!resposta.ok) {
            let detalhe = `Erro HTTP ${resposta.status}.`;

            try {
                const dados = await resposta.json();
                detalhe = dados.erro || detalhe;

            } catch (erro) {
                // Mantém o código HTTP.
            }

            throw new Error(detalhe);
        }

        alert(
            editando
                ? "Projeto alterado com sucesso!"
                : "Projeto cadastrado com sucesso!"
        );

        cancelarEdicao();

        await listarProjetos();

    } catch (erro) {
        alert(
            `Não foi possível confirmar a operação. ${erro.message}`
        );

        console.error(erro);

    } finally {
        definirOcupado(false);
    }
});

async function excluirProjeto(id) {
    if (ocupado) {
        return;
    }

    if (!confirm("Deseja realmente excluir este projeto?")) {
        return;
    }

    definirOcupado(true);

    try {
        const resposta = await fetch(
            `${API_URL}/${id}`,
            { method: "DELETE" }
        );

        if (!resposta.ok) {
            throw new Error(
                `Erro ao excluir o projeto. HTTP ${resposta.status}.`
            );
        }

        if (idEmEdicao === id) {
            cancelarEdicao();
        }

        alert("Projeto excluído com sucesso!");

        await listarProjetos();

    } catch (erro) {
        alert(
            `Não foi possível confirmar a exclusão. ${erro.message}`
        );

        console.error(erro);

    } finally {
        definirOcupado(false);
    }
}

btnCancelar.addEventListener("click", () => {
    if (!ocupado) {
        cancelarEdicao();
    }
});

listarProjetos();