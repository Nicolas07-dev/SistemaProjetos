
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

const titulos = {
    projetos: 'Gerenciamento de Projetos',
    dashboard: 'Dashboard de Projetos',
    categorias: 'Categorias de Projetos',
    relatorios: 'Relatórios de Projetos',
    configuracoes: 'Configurações do Sistema'
};

function hojeLocal() {
    const agora = new Date();
    const ano = agora.getFullYear();
    const mes = String(agora.getMonth() + 1).padStart(2, '0');
    const dia = String(agora.getDate()).padStart(2, '0');
    return `${ano}-${mes}-${dia}`;
}

function dataValida(data) {
    if (!/^\d{4}-\d{2}-\d{2}$/.test(data || '')) {
        return false;
    }

    const [ano, mes, dia] = data.split('-').map(Number);
    const d = new Date(0);
    d.setFullYear(ano, mes - 1, dia);

    return d.getFullYear() === ano &&
           d.getMonth() === mes - 1 &&
           d.getDate() === dia;
}

function mostrarData(data) {
    if (!data) return 'Não informada';
    if (!dataValida(data)) return String(data);

    const [ano, mes, dia] = data.split('-');
    return `${dia}/${mes}/${ano}`;
}

function aviso(texto) {
    $('mensagem').textContent = texto;
}

async function autenticar(usuario, senha) {
    const resposta = await fetch(API + '/auth/login', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ usuario, senha })
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

        if (!resposta.ok) throw new Error();

        const dados = await resposta.json();
        exibirSistema(dados.usuario);
    } catch {
        exibirLogin();
    }
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
        throw new Error('Sessão expirada. Faça login novamente.');
    }

    if (!resposta.ok) {
        let erro = `Erro HTTP ${resposta.status}`;

        try {
            const dados = await resposta.json();
            erro = dados.erro || dados.mensagem || erro;
        } catch {
        }

        throw new Error(erro);
    }

    if (resposta.status === 204) return null;

    const texto = await resposta.text();
    return texto ? JSON.parse(texto) : null;
}

function bloquear(valor) {
    ocupado = valor;

    document.querySelectorAll('button').forEach(b => {
        if (b.id !== 'theme-toggle' &&
            b.id !== 'theme-settings') {
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

        
        projetos = Array.isArray(p)
            ? p.map(projeto => ({
                ...projeto,
                status: String(projeto.status || '').trim().toLowerCase() ===
                    'em desenvolvimento'
                    ? 'Em Desenvolvimento'
                    : projeto.status
            }))
            : [];

        categorias = Array.isArray(c) ? c : [];

        atualizarCategoriasSelect();
        renderizar();

        aviso('Dados atualizados.');
    } catch (erro) {
        aviso('Não foi possível conectar à API: ' + erro.message);
        console.error(erro);
    }
}

function atualizarCategoriasSelect() {
    ['categoria', 'filtro-categoria', 'rel-categoria']
        .forEach(id => {
            const select = $(id);
            const valor = select.value;

            select.replaceChildren();

            if (id !== 'categoria') {
                select.add(new Option('Todas as categorias', ''));
            }

            categorias.forEach(c => {
                select.add(new Option(c, c));
            });

            if (categorias.includes(valor)) {
                select.value = valor;
            }
        });
}


function projetoAtrasado(p) {
    if (!p.prazo || !dataValida(p.prazo)) {
        return false;
    }

    if (p.status === 'Concluído' || p.status === 'Cancelado') {
        return false;
    }

    return p.prazo < hojeLocal();
}




function stats() {
    const contar = status =>
        projetos.filter(p => p.status === status).length;

    const planejados = contar('Planejado');
    const andamento = contar('Em Desenvolvimento');

    $('stat-total').textContent = projetos.length;
    $('stat-concluidos').textContent = contar('Concluído');
    $('stat-ativos').textContent = planejados + andamento;
    $('stat-cancelados').textContent = contar('Cancelado');
}


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

    const classes = {
        'Planejado': 'planejado',
        'Em Desenvolvimento': 'andamento',
        'Concluído': 'concluido',
        'Cancelado': 'cancelado'
    };

    s.className = 'status ' + (classes[status] || 'planejado');
    return s;
}

function filtrados() {
    const busca = $('busca').value
        .trim()
        .toLocaleLowerCase('pt-BR');

    const cat = $('filtro-categoria').value;
    const status = $('filtro-status').value;

    return projetos.filter(p => {
        const nome = String(p.nome || '')
            .toLocaleLowerCase('pt-BR');

        return nome.includes(busca) &&
               (!cat || p.categoria === cat) &&
               (!status || p.status === status);
    });
}

function renderProjetos() {
    const dados = filtrados();
    const paginas = Math.max(1, Math.ceil(dados.length / TAMANHO));

    pagina = Math.min(pagina, paginas - 1);

    const corpo = $('projetos-body');
    corpo.replaceChildren();

    dados.slice(
        pagina * TAMANHO,
        (pagina + 1) * TAMANHO
    ).forEach(p => {
        const tr = document.createElement('tr');


        celula(tr, p.id);
        celula(tr, p.nome);
        celula(tr, p.descricao);
        celula(tr, p.categoria);

        celula(tr, '').append(etiqueta(p.status));

        celula(tr, mostrarData(p.dataCriacao));
        celula(tr, mostrarData(p.prazo));

        const acoes = celula(tr, '');
        acoes.className = 'actions';

        acoes.append(
            botao('✎ Editar', 'icon-btn', () => editar(p)),
            botao('✕ Excluir', 'icon-btn delete', () => excluir(p))
        );


        corpo.append(tr);
    });

    if (!dados.length) {
        const tr = document.createElement('tr');
        const td = celula(tr, 'Nenhum projeto encontrado.');
        td.colSpan = 8;
        td.className = 'empty';
        corpo.append(tr);
    }

    $('contagem').textContent =
        `${dados.length} projeto(s) encontrado(s)`;

    $('pagina-label').textContent =
        `${pagina + 1} / ${paginas}`;

    $('anterior').disabled = pagina === 0;
    $('proximo').disabled = pagina >= paginas - 1;
}

function limpar() {
    edicao = null;

    $('form-projeto').reset();
    $('dataCriacao').value = hojeLocal();
    $('dataCriacao').readOnly = true;
    $('dataCriacao').title =
        'Preenchida automaticamente no cadastro';

    $('prazo').value = '';
    $('form-title').textContent = 'Cadastro de Projeto';
    $('salvar').textContent = '＋ Cadastrar';
    $('excluir-selecionado').hidden = true;
}

function editar(p) {
    edicao = p.id;

    $('nome').value = p.nome || '';
    $('descricao').value = p.descricao || '';
    $('categoria').value = p.categoria || '';
    $('status').value = p.status || 'Planejado';

    $('dataCriacao').value = p.dataCriacao || '';
    $('dataCriacao').readOnly = Boolean(p.dataCriacao);

    $('dataCriacao').title = p.dataCriacao
        ? 'A data de criação original não pode ser alterada'
        : 'Projeto antigo: informe a data de criação';

    $('prazo').value = p.prazo || '';

    $('form-title').textContent = 'Editar Projeto';
    $('salvar').textContent = '✓ Salvar alterações';
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
            { method: 'DELETE' }
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

$('form-projeto').addEventListener('submit', async e => {
    e.preventDefault();

    if (ocupado) return;

    const p = {
        nome: $('nome').value.trim(),
        descricao: $('descricao').value.trim(),
        categoria: $('categoria').value,
        status: $('status').value,
        dataCriacao: $('dataCriacao').value,
        prazo: $('prazo').value
    };

    if (!p.nome || !p.descricao || !p.categoria) {
        alert('Preencha todos os campos.');
        return;
    }

    if (p.nome.length < 3 || p.nome.length > 100 ||
        p.descricao.length < 10 || p.descricao.length > 500) {
        alert(
            'Nome: 3 a 100 caracteres. ' +
            'Descrição: 10 a 500 caracteres.'
        );
        return;
    }

    if (!dataValida(p.dataCriacao) || !dataValida(p.prazo)) {
        alert('Informe datas válidas para criação e prazo.');
        return;
    }

    if (p.prazo < p.dataCriacao) {
        alert('O prazo não pode ser anterior à data de criação.');
        return;
    }

    const editando = edicao !== null;

    bloquear(true);

    try {
        const caminho = editando
            ? '/projetos/' + encodeURIComponent(edicao)
            : '/projetos';

        await requisicao(caminho, {
            method: editando ? 'PUT' : 'POST',
            body: JSON.stringify(p)
        });

        limpar();
        await atualizar();

        aviso(editando
            ? 'Projeto atualizado com sucesso.'
            : 'Projeto cadastrado com sucesso.'
        );
    } catch (erro) {
        alert(erro.message);
    } finally {
        bloquear(false);
        renderProjetos();
    }
});

function renderCategorias() {
    const corpo = $('categorias-body');
    corpo.replaceChildren();

    categorias.forEach(cat => {
        const tr = document.createElement('tr');

        celula(tr, cat);

        celula(
            tr,
            projetos.filter(p => p.categoria === cat).length
        );

        const acoes = celula(tr, '');
        acoes.className = 'actions';

        acoes.append(
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
        const td = celula(tr, 'Nenhuma categoria cadastrada.');
        td.colSpan = 3;
        td.className = 'empty';
        corpo.append(tr);
    }
}

async function criarCategoria() {
    const nome = prompt('Nome da nova categoria:');

    if (nome === null || !nome.trim()) return;

    try {
        await requisicao('/categorias', {
            method: 'POST',
            body: JSON.stringify({ nome: nome.trim() })
        });

        await atualizar();
    } catch (erro) {
        alert(erro.message);
    }
}

async function renomear(antiga) {
    const nome = prompt('Novo nome da categoria:', antiga);

    if (nome === null ||
        !nome.trim() ||
        nome.trim() === antiga) {
        return;
    }

    try {
        await requisicao(
            '/categorias/' + encodeURIComponent(antiga),
            {
                method: 'PUT',
                body: JSON.stringify({ nome: nome.trim() })
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
            { method: 'DELETE' }
        );

        await atualizar();
    } catch (erro) {
        alert(erro.message);
    }
}

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

        barra.style.width = (valor / max * 100) + '%';

        trilha.append(barra);
        linha.append(cab, trilha);
        el.append(linha);
    });
}


function graficoPizzaStatus() {
    const elemento = $('grafico-status');
    elemento.replaceChildren();

    const status = [
        'Planejado',
        'Em Desenvolvimento',
        'Concluído',
        'Cancelado'
    ];

    const cores = [
        '#8b5cf6',
        '#f59e0b',
        '#22c55e',
        '#ef4444'
    ];

    const dados = status.map((nome, i) => ({
        nome,
        quantidade: projetos.filter(p => p.status === nome).length,
        cor: cores[i]
    }));

    const total = dados.reduce((soma, d) => soma + d.quantidade, 0);

    if (total === 0) {
        elemento.textContent = 'Nenhum projeto para exibir.';
        return;
    }

    let inicio = 0;

    const partes = dados
        .filter(d => d.quantidade > 0)
        .map(d => {
            const fim = inicio + (d.quantidade / total) * 100;
            const parte = `${d.cor} ${inicio}% ${fim}%`;
            inicio = fim;
            return parte;
        });

    const pizzaArea = document.createElement('div');
    pizzaArea.className = 'pizza-area';

    const pizza = document.createElement('div');
    pizza.className = 'pizza';
    pizza.style.background =
        `conic-gradient(${partes.join(', ')})`;

    pizza.setAttribute('role', 'img');
    pizza.setAttribute('aria-label',
        'Distribuição dos projetos por status');

    const centro = document.createElement('div');
    centro.className = 'pizza-centro';
    const totalCentro = document.createElement('strong');
    totalCentro.textContent = total;
    const legendaCentro = document.createElement('span');
    legendaCentro.textContent = 'projetos';
    centro.append(totalCentro, legendaCentro);
    pizzaArea.append(pizza, centro);

    const legenda = document.createElement('div');
    legenda.className = 'pizza-legenda';

    dados.forEach(d => {
        const linha = document.createElement('div');
        linha.className = 'pizza-legenda-item';

        const cor = document.createElement('span');
        cor.className = 'pizza-cor';
        cor.style.backgroundColor = d.cor;

        const percentual = ((d.quantidade / total) * 100)
            .toFixed(1);

        const texto = document.createElement('span');
        texto.textContent =
            `${d.nome}: ${d.quantidade} (${percentual}%)`;

        linha.append(cor, texto);
        legenda.append(linha);
    });

    elemento.append(pizzaArea, legenda);
}

function graficoEvolucaoMensal() {
    const elemento = $('grafico-evolucao');
    elemento.replaceChildren();

    const datas = projetos
        .filter(p => dataValida(p.dataCriacao))
        .map(p => p.dataCriacao.slice(0, 7));

    if (datas.length === 0) {
        elemento.textContent =
            'Nenhum projeto com data de criação válida.';
        return;
    }

    const meses = [...new Set(datas)].sort();
    const primeiro = meses[0];
    const ultimo = meses[meses.length - 1];

    const [anoInicial, mesInicial] = primeiro.split('-').map(Number);
    const [anoFinal, mesFinal] = ultimo.split('-').map(Number);

    const pontos = [];

    for (let ano = anoInicial; ano <= anoFinal; ano++) {
        const inicio = ano === anoInicial ? mesInicial : 1;
        const fim = ano === anoFinal ? mesFinal : 12;

        for (let mes = inicio; mes <= fim; mes++) {
            const chave = `${ano}-${String(mes).padStart(2, '0')}`;

            pontos.push({
                mes: chave,
                total: datas.filter(d => d <= chave).length
            });
        }
    }

    const largura = 700;
    const altura = 280;
    const margem = 40;
    const areaLargura = largura - margem * 2;
    const areaAltura = altura - margem * 2;
    const maximo = Math.max(1, ...pontos.map(p => p.total));

    const ns = 'http://www.w3.org/2000/svg';

    const criarSvg = (tipo, atributos) => {
        const el = document.createElementNS(ns, tipo);

        Object.entries(atributos).forEach(([chave, valor]) => {
            el.setAttribute(chave, valor);
        });

        return el;
    };

    const svg = criarSvg('svg', {
        viewBox: `0 0 ${largura} ${altura}`,
        role: 'img',
        'aria-label': 'Evolução mensal dos projetos'
    });

    const coordenadas = pontos.map((p, i) => {
        const x = margem +
            (pontos.length === 1
                ? areaLargura / 2
                : (i / (pontos.length - 1)) * areaLargura);

        const y = altura - margem -
            (p.total / maximo) * areaAltura;

        return { x, y, ...p };
    });

    for (let i = 0; i <= 4; i++) {
        const valor = (maximo / 4) * i;
        const y = altura - margem - (i / 4) * areaAltura;

        svg.append(criarSvg('line', {
            x1: margem,
            y1: y,
            x2: largura - margem,
            y2: y,
            stroke: '#94a3b8',
            'stroke-opacity': '0.25'
        }));

        const texto = criarSvg('text', {
            x: margem - 10,
            y: y + 4,
            'text-anchor': 'end',
            fill: '#64748b',
            'font-size': '12'
        });

        texto.textContent = Number(valor.toFixed(1));
        svg.append(texto);
    }

    const linha = criarSvg('polyline', {
        points: coordenadas.map(p => `${p.x},${p.y}`).join(' '),
        fill: 'none',
        stroke: '#2563eb',
        'stroke-width': '3',
        'stroke-linecap': 'round',
        'stroke-linejoin': 'round'
    });

    svg.append(linha);

    const intervalo = Math.max(1, Math.ceil(pontos.length / 8));

    coordenadas.forEach((p, i) => {
        const circulo = criarSvg('circle', {
            cx: p.x,
            cy: p.y,
            r: 4,
            fill: '#2563eb'
        });

        const titulo = criarSvg('title', {});
        titulo.textContent = `${p.mes}: ${p.total} projeto(s)`;

        circulo.append(titulo);
        svg.append(circulo);

        if (i % intervalo === 0 || i === pontos.length - 1) {
            const texto = criarSvg('text', {
                x: p.x,
                y: altura - 12,
                'text-anchor': 'middle',
                fill: '#64748b',
                'font-size': '11'
            });

            const [ano, mes] = p.mes.split('-');
            texto.textContent = `${mes}/${ano.slice(2)}`;

            svg.append(texto);
        }
    });

    elemento.append(svg);
}

function graficoCumprimentoPrazos() {
    const elemento = $('grafico-prazos');
    elemento.replaceChildren();

    // Consideramos apenas projetos em aberto com prazo válido.
    const elegiveis = projetos.filter(p =>
        dataValida(p.prazo) &&
        p.status !== 'Concluído' &&
        p.status !== 'Cancelado'
    );

    const atrasados = elegiveis.filter(projetoAtrasado).length;
    const noPrazo = elegiveis.length - atrasados;
    const total = elegiveis.length;

    if (total === 0) {
        elemento.textContent =
            'Nenhum projeto em aberto com prazo válido.';
        return;
    }

    const percentual = (atrasados / total) * 100;

    const rosca = document.createElement('div');
    rosca.className = 'rosca';

    rosca.style.background = `conic-gradient(
        #ef4444 0% ${percentual}%,
        #22c55e ${percentual}% 100%
    )`;

    const centro = document.createElement('div');
    centro.className = 'rosca-centro';

    const numero = document.createElement('strong');
    numero.textContent = total;

    const descricao = document.createElement('small');
    descricao.textContent = 'Em aberto';

    centro.append(numero, descricao);
    rosca.append(centro);

    const legenda = document.createElement('div');
    legenda.className = 'pizza-legenda';

    [
        {
            nome: 'No prazo',
            valor: noPrazo,
            cor: '#22c55e'
        },
        {
            nome: 'Atrasados',
            valor: atrasados,
            cor: '#ef4444'
        }
    ].forEach(d => {
        const linha = document.createElement('div');
        linha.className = 'pizza-legenda-item';

        const cor = document.createElement('span');
        cor.className = 'pizza-cor';
        cor.style.backgroundColor = d.cor;

        const texto = document.createElement('span');
        const porcentagem = ((d.valor / total) * 100).toFixed(1);

        texto.textContent =
            `${d.nome}: ${d.valor} (${porcentagem}%)`;

        linha.append(cor, texto);
        legenda.append(linha);
    });

    elemento.append(rosca, legenda);
}

function renderDashboard() {
    const status = [
        'Planejado',
        'Em Desenvolvimento',
        'Concluído',
        'Cancelado'
    ];

    graficoPizzaStatus();

    grafico(
        'grafico-categorias',
        categorias.map(c => [
            c,
            projetos.filter(p => p.categoria === c).length
        ])
    );

    graficoEvolucaoMensal();
    graficoCumprimentoPrazos();
}

function dadosRelatorio() {
    const cat = $('rel-categoria').value;
    const status = $('rel-status').value;

    return projetos.filter(p =>
        (!cat || p.categoria === cat) &&
        (!status || p.status === status)
    );
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
            p.status,
            mostrarData(p.dataCriacao),
            mostrarData(p.prazo)
        ].forEach(valor => celula(tr, valor));

        corpo.append(tr);
    });

    const contar = status =>
        dados.filter(p => p.status === status).length;

    $('rel-resumo').textContent =
        `Total: ${dados.length} | ` +
        `Planejados: ${contar('Planejado')} | ` +
        `Em Desenvolvimento: ${contar('Em Desenvolvimento')} | ` +
        `Concluídos: ${contar('Concluído')} | ` +
        `Cancelados: ${contar('Cancelado')}`;
}

function exportarCsv() {
    const linhas = [
        [
            'ID',
            'Nome',
            'Descrição',
            'Categoria',
            'Status',
            'Data de criação',
            'Prazo'
        ],
        ...dadosRelatorio().map(p => [
            p.id,
            p.nome,
            p.descricao,
            p.categoria,
            p.status,
            p.dataCriacao || '',
            p.prazo || ''
        ])
    ];

    const csv = '\ufeff' + linhas.map(linha =>
        linha.map(valor =>
            '"' + String(valor ?? '').replaceAll('"', '""') + '"'
        ).join(';')
    ).join('\r\n');

    const url = URL.createObjectURL(
        new Blob([csv], {
            type: 'text/csv;charset=utf-8'
        })
    );

    const a = document.createElement('a');
    a.href = url;
    a.download = 'relatorio_projetos.csv';
    a.click();

    setTimeout(() => URL.revokeObjectURL(url), 1000);
}

function imprimir() {
    const dados = dadosRelatorio();
    const janela = window.open('', '_blank');

    if (!janela) {
        alert('Permita pop-ups para imprimir o relatório.');
        return;
    }

    const escape = valor =>
        String(valor ?? '').replace(/[&<>"']/g, c => ({
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#39;'
        }[c]));

    const linhas = dados.map(p => {
        const valores = [
            p.id,
            p.nome,
            p.descricao,
            p.categoria,
            p.status,
            mostrarData(p.dataCriacao),
            mostrarData(p.prazo)
        ];

        return '<tr>' +
            valores.map(v => '<td>' + escape(v) + '</td>').join('') +
            '</tr>';
    }).join('');

    janela.document.write(
        '<!doctype html>' +
        '<html lang="pt-BR">' +
        '<head><meta charset="utf-8">' +
        '<title>Relatório de Projetos</title>' +
        '<style>' +
        'body{font:13px Arial;padding:30px}' +
        'table{border-collapse:collapse;width:100%}' +
        'th,td{border:1px solid #bbb;padding:8px;text-align:left}' +
        'h1{color:#17365b}' +
        '</style></head><body>' +
        '<h1>Relatório de Projetos</h1>' +
        '<p>' + escape(new Date().toLocaleString('pt-BR')) + '</p>' +
        '<table><thead><tr>' +
        '<th>ID</th>' +
        '<th>Nome</th>' +
        '<th>Descrição</th>' +
        '<th>Categoria</th>' +
        '<th>Status</th>' +
        '<th>Data de criação</th>' +
        '<th>Prazo</th>' +
        '</tr></thead><tbody>' +
        linhas +
        '</tbody></table></body></html>'
    );

    janela.document.close();
    janela.focus();
    janela.print();
}

function renderizar() {
    stats();
    renderProjetos();
    renderCategorias();
    renderDashboard();
    renderRelatorio();
}

function abrir(nome) {
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

function aplicarTema(escuro) {
    document.body.classList.toggle('dark', escuro);

    localStorage.setItem(
        'projetos-tema',
        escuro ? 'escuro' : 'claro'
    );

    $('theme-toggle').textContent =
        escuro ? '☀ Tema claro' : '☾ Tema escuro';

    if ($('theme-select')) {
        $('theme-select').value = escuro ? 'Escuro' : 'Claro';
    }
}

function tema() {
    aplicarTema(!document.body.classList.contains('dark'));
}

aplicarTema(localStorage.getItem('projetos-tema') === 'escuro');

document.querySelectorAll('#nav button').forEach(b => {
    b.addEventListener('click', () => abrir(b.dataset.page));
});

$('theme-toggle').addEventListener('click', tema);
$('save-theme').addEventListener('click', () => {
    aplicarTema($('theme-select').value === 'Escuro');
    $('settings-feedback').textContent = 'Preferências salvas com sucesso!';
});

$('restore-theme').addEventListener('click', () => {
    aplicarTema(false);
    $('settings-feedback').textContent = 'Configurações restauradas para o tema claro.';
});
$('limpar').addEventListener('click', limpar);
$('atualizar').addEventListener('click', atualizar);

$('excluir-selecionado').addEventListener('click', () => {
    const p = projetos.find(p => p.id === edicao);
    if (p) excluir(p);
});

$('nova-categoria').addEventListener('click', criarCategoria);
$('exportar-csv').addEventListener('click', exportarCsv);
$('imprimir-relatorio').addEventListener('click', imprimir);

$('rel-categoria').addEventListener('change', renderRelatorio);
$('rel-status').addEventListener('change', renderRelatorio);

['busca', 'filtro-categoria', 'filtro-status'].forEach(id => {
    $(id).addEventListener(
        id === 'busca' ? 'input' : 'change',
        () => {
            pagina = 0;
            renderProjetos();
        }
    );
});

$('anterior').addEventListener('click', () => {
    pagina = Math.max(0, pagina - 1);
    renderProjetos();
});

$('proximo').addEventListener('click', () => {
    pagina++;
    renderProjetos();
});

$('login-form').addEventListener('submit', async e => {
    e.preventDefault();

    const b = $('login-botao');
    b.disabled = true;
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
        b.disabled = false;
    }
});

$('sair').addEventListener('click', async () => {
    if (!confirm('Deseja sair da sua conta?')) return;

    const token = tokenSessao;

    try {
        await fetch(API + '/auth/logout', {
            method: 'POST',
            headers: {
                Authorization: 'Bearer ' + token
            }
        });
    } catch (erro) {
        console.error('Erro ao encerrar sessão:', erro);
    } finally {
        exibirLogin();
    }
});

function relogio() {
    $('relogio').textContent =
        new Date().toLocaleString('pt-BR', {
            dateStyle: 'short',
            timeStyle: 'short'
        });
}

relogio();
setInterval(relogio, 30000);

limpar();
verificarSessao();
