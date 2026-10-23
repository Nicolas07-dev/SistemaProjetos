
'use strict';

const API = 'http://localhost:7070/api';

const $ = id => document.getElementById(id);

let tokenSessao = sessionStorage.getItem('projetos-token') || '';

let projetos = [];
let categorias = [];
let edicao = null;
let pagina = 0;
let ocupado = false;

const TAMANHO = 10;

const estado = {
    pagina: 'projetos'
};

const titulos = {
    projetos: 'Gerenciamento de Projetos',
    dashboard: 'Dashboard de Projetos',
    categorias: 'Categorias de Projetos',
    relatorios: 'Relatórios de Projetos',
    configuracoes: 'Configurações do Sistema'
};

// ======================================================
// AUTENTICAÇÃO
// ======================================================

async function autenticar(usuario, senha) {
    const resposta = await fetch(API + '/auth/login', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            usuario,
            senha
        })
    });

    const dados = await resposta.json();

    if (!resposta.ok) {
        throw new Error(dados.erro || 'Credenciais inválidas.');
    }

    return dados;
}

function exibirLogin() {
    tokenSessao = '';

    sessionStorage.removeItem('projetos-token');

    $('app-principal').hidden = true;
    $('login-screen').hidden = false;
    $('login-senha').value = '';
}

function exibirSistema(usuario) {
    $('login-screen').hidden = true;
    $('app-principal').hidden = false;
    $('usuario-logado').textContent = usuario;
    $('login-erro').textContent = '';

    atualizar();
}

async function verificarSessao() {
    if (!tokenSessao) {
        exibirLogin();
        return;
    }

    try {
        const resposta = await fetch(API + '/auth/session', {
            headers: {
                Authorization: 'Bearer ' + tokenSessao
            }
        });

        if (!resposta.ok) {
            throw new Error();
        }

        const dados = await resposta.json();

        exibirSistema(dados.usuario);

    } catch {
        exibirLogin();
    }
}

// ======================================================
// COMUNICAÇÃO COM A API
// ======================================================

function aviso(texto) {
    $('mensagem').textContent = texto;
}

async function requisicao(caminho, opcoes = {}) {
    const resposta = await fetch(API + caminho, {
        ...opcoes,
        headers: {
            'Content-Type': 'application/json',
            Authorization: 'Bearer ' + tokenSessao,
            ...(opcoes.headers || {})
        }
    });

    if (resposta.status === 401) {
        exibirLogin();

        throw new Error(
            'Sessão expirada. Faça login novamente.'
        );
    }

    if (!resposta.ok) {
        let erro = `Erro HTTP ${resposta.status}`;

        try {
            const dados = await resposta.json();

            erro = dados.erro || dados.mensagem || erro;

        } catch {
            // Mantém a mensagem HTTP original.
        }

        throw new Error(erro);
    }

    if (resposta.status === 204) {
        return null;
    }

    const texto = await resposta.text();

    return texto ? JSON.parse(texto) : null;
}

function bloquear(valor) {
    ocupado = valor;

    document.querySelectorAll('button').forEach(b => {
        if (
            b.id !== 'theme-toggle' &&
            b.id !== 'theme-settings'
        ) {
            b.disabled = valor;
        }
    });
}

async function atualizar() {
    try {
        aviso('Carregando dados...');

        const [p, c] = await Promise.all([
            requisicao('/projetos'),
            requisicao('/categorias')
        ]);

        projetos = Array.isArray(p) ? p : [];
        categorias = Array.isArray(c) ? c : [];

        atualizarCategoriasSelect();
        renderizar();

        aviso('Dados atualizados.');

    } catch (erro) {
        aviso(
            'Não foi possível conectar à API: ' + erro.message
        );

        console.error(erro);
    }
}

// ======================================================
// CATEGORIAS NOS SELECTS
// ======================================================

function atualizarCategoriasSelect() {
    const ids = [
        'categoria',
        'filtro-categoria',
        'rel-categoria'
    ];

    ids.forEach(id => {
        const select = $(id);
        const valor = select.value;

        select.replaceChildren();

        if (id !== 'categoria') {
            select.add(
                new Option('Todas as categorias', '')
            );
        }

        categorias.forEach(c => {
            select.add(new Option(c, c));
        });

        if (categorias.includes(valor)) {
            select.value = valor;
        }
    });
}

// ======================================================
// ESTATÍSTICAS
// ======================================================

function stats() {
    const total = projetos.length;

    const contar = status => {
        return projetos.filter(
            p => p.status === status
        ).length;
    };

    $('stat-total').textContent = total;

    $('stat-concluidos').textContent =
        contar('Concluído');

    $('stat-andamento').textContent =
        contar('Em desenvolvimento');

    $('stat-planejados').textContent =
        contar('Planejado');
}

// ======================================================
// ELEMENTOS DA TABELA
// ======================================================

function celula(tr, valor) {
    const td = document.createElement('td');

    td.textContent = String(valor ?? '');

    tr.append(td);

    return td;
}

function botao(texto, classe, acao) {
    const b = document.createElement('button');

    b.type = 'button';
    b.textContent = texto;
    b.className = classe;

    b.addEventListener('click', acao);

    return b;
}

function etiqueta(status) {
    const s = document.createElement('span');

    s.textContent = status || '—';

    s.className =
        'status ' +
        (
            status === 'Concluído'
                ? 'concluido'
                : status === 'Em desenvolvimento'
                    ? 'andamento'
                    : 'planejado'
        );

    return s;
}

// ======================================================
// FILTROS DOS PROJETOS
// ======================================================

function filtrados() {
    const busca = $('busca')
        .value
        .trim()
        .toLocaleLowerCase('pt-BR');

    const cat = $('filtro-categoria').value;
    const status = $('filtro-status').value;

    return projetos.filter(p => {
        const nome = String(p.nome || '')
            .toLocaleLowerCase('pt-BR');

        return (
            nome.includes(busca) &&
            (!cat || p.categoria === cat) &&
            (!status || p.status === status)
        );
    });
}

// ======================================================
// RENDERIZAÇÃO DOS PROJETOS
// ======================================================

function renderProjetos() {
    const dados = filtrados();

    const paginas = Math.max(
        1,
        Math.ceil(dados.length / TAMANHO)
    );

    pagina = Math.min(pagina, paginas - 1);

    const corpo = $('projetos-body');

    corpo.replaceChildren();

    dados
        .slice(
            pagina * TAMANHO,
            (pagina + 1) * TAMANHO
        )
        .forEach(p => {
            const tr = document.createElement('tr');

            celula(tr, p.id);
            celula(tr, p.nome);
            celula(tr, p.categoria);

            const td = celula(tr, '');

            td.append(etiqueta(p.status));

            const a = celula(tr, '');

            a.className = 'actions';

            a.append(
                botao(
                    '✎ Editar',
                    'icon-btn',
                    () => editar(p)
                ),
                botao(
                    '✕ Excluir',
                    'icon-btn delete',
                    () => excluir(p)
                )
            );

            corpo.append(tr);
        });

    if (!dados.length) {
        const tr = document.createElement('tr');

        const td = celula(
            tr,
            'Nenhum projeto encontrado.'
        );

        td.colSpan = 5;
        td.className = 'empty';

        corpo.append(tr);
    }

    $('contagem').textContent =
        `${dados.length} projeto(s) encontrado(s)`;

    $('pagina-label').textContent =
        `${pagina + 1} / ${paginas}`;

    $('anterior').disabled = pagina === 0;

    $('proximo').disabled =
        pagina >= paginas - 1;
}

// ======================================================
// FORMULÁRIO DE PROJETOS
// ======================================================

function limpar() {
    edicao = null;

    $('form-projeto').reset();

    $('form-title').textContent =
        'Cadastro de Projeto';

    $('salvar').textContent =
        '＋ Cadastrar';

    $('excluir-selecionado').hidden = true;
}

function editar(p) {
    edicao = p.id;

    $('nome').value = p.nome || '';
    $('descricao').value = p.descricao || '';
    $('categoria').value = p.categoria || '';

    $('status').value =
        p.status || 'Planejado';

    $('form-title').textContent =
        'Editar Projeto';

    $('salvar').textContent =
        '✓ Salvar alterações';

    $('excluir-selecionado').hidden = false;

    abrir('projetos');

    $('nome').focus();
}

async function excluir(p) {
    if (!confirm(`Excluir o projeto "${p.nome}"?`)) {
        return;
    }

    bloquear(true);

    try {
        await requisicao(
            '/projetos/' + encodeURIComponent(p.id),
            {
                method: 'DELETE'
            }
        );

        limpar();

        await atualizar();

        aviso('Projeto excluído com sucesso.');

    } catch (erro) {
        alert(erro.message);

    } finally {
        bloquear(false);
        renderProjetos();
    }
}

// ======================================================
// CADASTRAR E EDITAR PROJETOS
// ======================================================

$('form-projeto').addEventListener(
    'submit',
    async e => {
        e.preventDefault();

        if (ocupado) {
            return;
        }

        const p = {
            nome: $('nome').value.trim(),
            descricao: $('descricao').value.trim(),
            categoria: $('categoria').value,
            status: $('status').value
        };

        if (!p.nome || !p.descricao || !p.categoria) {
            alert('Preencha todos os campos.');
            return;
        }

        const editando = edicao !== null;

        bloquear(true);

        try {
            await requisicao(
                '/projetos' +
                    (
                        editando
                            ? '/' + encodeURIComponent(edicao)
                            : ''
                    ),
                {
                    method: editando ? 'PUT' : 'POST',
                    body: JSON.stringify(p)
                }
            );

            limpar();

            await atualizar();

            aviso(
                editando
                    ? 'Projeto atualizado com sucesso.'
                    : 'Projeto cadastrado com sucesso.'
            );

        } catch (erro) {
            alert(erro.message);

        } finally {
            bloquear(false);
            renderProjetos();
        }
    }
);

// ======================================================
// GERENCIAMENTO DE CATEGORIAS
// ======================================================

function renderCategorias() {
    const corpo = $('categorias-body');

    corpo.replaceChildren();

    categorias.forEach(cat => {
        const tr = document.createElement('tr');

        celula(tr, cat);

        celula(
            tr,
            projetos.filter(
                p => p.categoria === cat
            ).length
        );

        const a = celula(tr, '');

        a.className = 'actions';

        a.append(
            botao(
                '✎ Renomear',
                'icon-btn',
                () => renomear(cat)
            ),
            botao(
                '✕ Excluir',
                'icon-btn delete',
                () => excluirCategoria(cat)
            )
        );

        corpo.append(tr);
    });

    if (!categorias.length) {
        const tr = document.createElement('tr');

        const td = celula(
            tr,
            'Nenhuma categoria cadastrada.'
        );

        td.colSpan = 3;
        td.className = 'empty';

        corpo.append(tr);
    }
}

async function criarCategoria() {
    const nome = prompt('Nome da nova categoria:');

    if (nome === null || !nome.trim()) {
        return;
    }

    try {
        await requisicao('/categorias', {
            method: 'POST',
            body: JSON.stringify({
                nome: nome.trim()
            })
        });

        await atualizar();

    } catch (erro) {
        alert(erro.message);
    }
}

async function renomear(antiga) {
    const nome = prompt(
        'Novo nome da categoria:',
        antiga
    );

    if (
        nome === null ||
        !nome.trim() ||
        nome.trim() === antiga
    ) {
        return;
    }

    try {
        await requisicao(
            '/categorias/' + encodeURIComponent(antiga),
            {
                method: 'PUT',
                body: JSON.stringify({
                    nome: nome.trim()
                })
            }
        );

        await atualizar();

    } catch (erro) {
        alert(erro.message);
    }
}

async function excluirCategoria(nome) {
    if (!confirm(`Excluir a categoria "${nome}"?`)) {
        return;
    }

    try {
        await requisicao(
            '/categorias/' + encodeURIComponent(nome),
            {
                method: 'DELETE'
            }
        );

        await atualizar();

    } catch (erro) {
        alert(erro.message);
    }
}

// ======================================================
// GRÁFICOS
// ======================================================

function grafico(id, dados) {
    const el = $(id);

    el.replaceChildren();

    const max = Math.max(
        1,
        ...dados.map(d => d[1])
    );

    dados.forEach(([nome, valor]) => {
        const linha = document.createElement('div');
        const cab = document.createElement('div');
        const rot = document.createElement('span');
        const numero = document.createElement('strong');
        const trilha = document.createElement('div');
        const barra = document.createElement('div');

        cab.className = 'bar-label';

        rot.textContent = nome;
        numero.textContent = valor;

        cab.append(rot, numero);

        trilha.className = 'track';

        barra.className = 'fill';

        barra.style.width =
            (valor / max * 100) + '%';

        trilha.append(barra);

        linha.append(cab, trilha);

        el.append(linha);
    });
}

function renderDashboard() {
    grafico(
        'grafico-status',
        [
            'Planejado',
            'Em desenvolvimento',
            'Concluído'
        ].map(s => [
            s,
            projetos.filter(
                p => p.status === s
            ).length
        ])
    );

    grafico(
        'grafico-categorias',
        categorias.map(c => [
            c,
            projetos.filter(
                p => p.categoria === c
            ).length
        ])
    );
}

// ======================================================
// RELATÓRIOS
// ======================================================

function dadosRelatorio() {
    const cat = $('rel-categoria').value;
    const status = $('rel-status').value;

    return projetos.filter(p => {
        return (
            (!cat || p.categoria === cat) &&
            (!status || p.status === status)
        );
    });
}

function renderRelatorio() {
    const dados = dadosRelatorio();

    const corpo = $('relatorio-body');

    corpo.replaceChildren();

    dados.forEach(p => {
        const tr = document.createElement('tr');

        [
            p.id,
            p.nome,
            p.descricao,
            p.categoria,
            p.status
        ].forEach(v => celula(tr, v));

        corpo.append(tr);
    });

    const planejados = dados.filter(
        p => p.status === 'Planejado'
    ).length;

    const andamento = dados.filter(
        p => p.status === 'Em desenvolvimento'
    ).length;

    const concluidos = dados.filter(
        p => p.status === 'Concluído'
    ).length;

    $('rel-resumo').textContent =
        `Total: ${dados.length} | ` +
        `Planejados: ${planejados} | ` +
        `Em desenvolvimento: ${andamento} | ` +
        `Concluídos: ${concluidos}`;
}

// ======================================================
// EXPORTAÇÃO CSV
// ======================================================

function exportarCsv() {
    const linhas = [
        [
            'ID',
            'Nome',
            'Descrição',
            'Categoria',
            'Status'
        ],
        ...dadosRelatorio().map(p => [
            p.id,
            p.nome,
            p.descricao,
            p.categoria,
            p.status
        ])
    ];

    const csv =
        '\ufeff' +
        linhas
            .map(l =>
                l
                    .map(v =>
                        '"' +
                        String(v ?? '').replaceAll('"', '""') +
                        '"'
                    )
                    .join(';')
            )
            .join('\r\n');

    const url = URL.createObjectURL(
        new Blob(
            [csv],
            {
                type: 'text/csv;charset=utf-8'
            }
        )
    );

    const a = document.createElement('a');

    a.href = url;
    a.download = 'relatorio_projetos.csv';

    a.click();

    setTimeout(() => {
        URL.revokeObjectURL(url);
    }, 1000);
}

// ======================================================
// IMPRESSÃO DE RELATÓRIOS
// ======================================================

function imprimir() {
    const dados = dadosRelatorio();

    const janela = window.open('', '_blank');

    if (!janela) {
        alert('Permita pop-ups para imprimir o relatório.');
        return;
    }

    const escape = s => {
        return String(s ?? '').replace(
            /[&<>"']/g,
            c => ({
                '&': '&amp;',
                '<': '&lt;',
                '>': '&gt;',
                '"': '&quot;',
                "'": '&#39;'
            }[c])
        );
    };

    const linhas = dados.map(p => {
        return (
            '<tr>' +
            [
                p.id,
                p.nome,
                p.descricao,
                p.categoria,
                p.status
            ]
                .map(v => '<td>' + escape(v) + '</td>')
                .join('') +
            '</tr>'
        );
    }).join('');

    janela.document.write(
        '<!doctype html>' +
        '<html lang="pt-BR">' +
        '<meta charset="utf-8">' +
        '<title>Relatório de Projetos</title>' +
        '<style>' +
        'body{font:13px Arial;padding:30px}' +
        'table{border-collapse:collapse;width:100%}' +
        'th,td{border:1px solid #bbb;padding:8px;text-align:left}' +
        'h1{color:#17365b}' +
        '</style>' +
        '<h1>Relatório de Projetos</h1>' +
        '<p>' +
        escape(new Date().toLocaleString('pt-BR')) +
        '</p>' +
        '<table>' +
        '<thead>' +
        '<tr>' +
        '<th>ID</th>' +
        '<th>Nome</th>' +
        '<th>Descrição</th>' +
        '<th>Categoria</th>' +
        '<th>Status</th>' +
        '</tr>' +
        '</thead>' +
        '<tbody>' +
        linhas +
        '</tbody>' +
        '</table>' +
        '</html>'
    );

    janela.document.close();
    janela.focus();
    janela.print();
}

// ======================================================
// RENDERIZAÇÃO GERAL
// ======================================================

function renderizar() {
    stats();
    renderProjetos();
    renderCategorias();
    renderDashboard();
    renderRelatorio();
}

// ======================================================
// NAVEGAÇÃO ENTRE TELAS
// ======================================================

function abrir(nome) {
    estado.pagina = nome;

    document.querySelectorAll('.page').forEach(el => {
        el.classList.toggle(
            'active',
            el.id === 'page-' + nome
        );
    });

    document.querySelectorAll('#nav button').forEach(b => {
        b.classList.toggle(
            'active',
            b.dataset.page === nome
        );
    });

    $('page-title').textContent = titulos[nome];
}

// ======================================================
// TEMA CLARO E ESCURO
// ======================================================

function tema() {
    const escuro = document.body.classList.toggle('dark');

    localStorage.setItem(
        'projetos-tema',
        escuro ? 'escuro' : 'claro'
    );

    $('theme-toggle').textContent =
        escuro ? '☀ Tema claro' : '☾ Tema escuro';
}

if (localStorage.getItem('projetos-tema') === 'escuro') {
    tema();
}

// ======================================================
// EVENTOS DE NAVEGAÇÃO
// ======================================================

document.querySelectorAll('#nav button').forEach(b => {
    b.addEventListener('click', () => {
        abrir(b.dataset.page);
    });
});

// ======================================================
// EVENTOS DOS BOTÕES
// ======================================================

$('theme-toggle').addEventListener('click', tema);

$('theme-settings').addEventListener('click', tema);

$('limpar').addEventListener('click', limpar);

$('atualizar').addEventListener('click', atualizar);

$('excluir-selecionado').addEventListener('click', () => {
    const p = projetos.find(
        p => p.id === edicao
    );

    if (p) {
        excluir(p);
    }
});

$('nova-categoria').addEventListener(
    'click',
    criarCategoria
);

$('exportar-csv').addEventListener(
    'click',
    exportarCsv
);

$('imprimir-relatorio').addEventListener(
    'click',
    imprimir
);

$('rel-categoria').addEventListener(
    'change',
    renderRelatorio
);

$('rel-status').addEventListener(
    'change',
    renderRelatorio
);

// ======================================================
// EVENTOS DOS FILTROS
// ======================================================

[
    'busca',
    'filtro-categoria',
    'filtro-status'
].forEach(id => {
    $(id).addEventListener(
        id === 'busca' ? 'input' : 'change',
        () => {
            pagina = 0;
            renderProjetos();
        }
    );
});

// ======================================================
// PAGINAÇÃO
// ======================================================

$('anterior').addEventListener('click', () => {
    pagina = Math.max(0, pagina - 1);

    renderProjetos();
});

$('proximo').addEventListener('click', () => {
    pagina++;

    renderProjetos();
});

// ======================================================
// EVENTO DO LOGIN
// ======================================================

$('login-form').addEventListener(
    'submit',
    async e => {
        e.preventDefault();

        const botao = $('login-botao');

        botao.disabled = true;

        $('login-erro').textContent = '';

        try {
            const dados = await autenticar(
                $('login-usuario').value.trim(),
                $('login-senha').value
            );

            tokenSessao = dados.token;

            sessionStorage.setItem(
                'projetos-token',
                tokenSessao
            );

            exibirSistema(dados.usuario);

        } catch (erro) {
            $('login-erro').textContent =
                erro.message.includes('Failed to fetch')
                    ? 'Não foi possível conectar à API. Inicie o servidor Java.'
                    : erro.message;

        } finally {
            botao.disabled = false;
        }
    }
);

// ======================================================
// EVENTO DE LOGOUT
// ======================================================

$('sair').addEventListener('click', async () => {
    if (!confirm('Deseja sair da sua conta?')) {
        return;
    }

    const token = tokenSessao;

    exibirLogin();

    try {
        await fetch(API + '/auth/logout', {
            method: 'POST',
            headers: {
                Authorization: 'Bearer ' + token
            }
        });

    } catch {
        // A sessão local já foi encerrada.
    }
});

// ======================================================
// RELÓGIO
// ======================================================

function relogio() {
    $('relogio').textContent =
        new Date().toLocaleString('pt-BR', {
            dateStyle: 'short',
            timeStyle: 'short'
        });
}

relogio();

setInterval(relogio, 30000);

// ======================================================
// VERIFICAR SESSÃO AO INICIAR
// ======================================================

verificarSessao();
