const API_URL = (
  process.env.REACT_APP_API_URL || "http://localhost:8080"
).replace(/\/$/, "");

let tokenAtual = null;
let aoExpirarSessao = null;

export const definirToken = (token) => {
  tokenAtual = token || null;
};

export const definirAoExpirarSessao = (callback) => {
  aoExpirarSessao = callback;
};

export const lerDadosDoToken = (token) => {
  try {
    const conteudo = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    return JSON.parse(atob(conteudo));
  } catch (e) {
    return null;
  }
};

export const tokenExpirado = (token) => {
  const dados = token ? lerDadosDoToken(token) : null;
  return !dados || !dados.exp || dados.exp * 1000 <= Date.now();
};

async function request(caminho, opcoes = {}) {
  if (tokenAtual && tokenExpirado(tokenAtual)) {
    tokenAtual = null;
    if (aoExpirarSessao) {
      aoExpirarSessao();
    }
    const expirada = new Error("Sua sessão expirou. Entre novamente.");
    expirada.status = 401;
    throw expirada;
  }

  const cabecalhos = { "Content-Type": "application/json" };
  if (tokenAtual) {
    cabecalhos.Authorization = `Bearer ${tokenAtual}`;
  }

  const resposta = await fetch(`${API_URL}${caminho}`, {
    ...opcoes,
    headers: { ...cabecalhos, ...(opcoes.headers || {}) },
  });

  if (!resposta.ok) {
    const corpo = await resposta.text();
    let mensagem =
      resposta.status === 403
        ? "Você não tem permissão para fazer isso."
        : `Erro ${resposta.status} ao chamar ${caminho}`;
    try {
      const dados = JSON.parse(corpo);
      if (dados && (dados.mensagem || dados.message)) {
        mensagem = dados.mensagem || dados.message;
      }
    } catch (e) {
      if (corpo) {
        mensagem = corpo;
      }
    }
    const erro = new Error(mensagem);
    erro.status = resposta.status;
    throw erro;
  }

  if (resposta.status === 204) {
    return null;
  }

  const texto = await resposta.text();
  return texto ? JSON.parse(texto) : null;
}

export const livrosApi = {
  listar: () => request("/livros"),
  listarMaterias: () => request("/livros/materias"),
  buscarPorTitulo: (titulo) =>
    request(`/livros/titulo/${encodeURIComponent(titulo)}`),
  filtrarPorMateria: (materia) =>
    request(`/livros/${encodeURIComponent(materia)}`),
  cadastrar: (titulo, materia, preco, estoque) =>
    request("/livros/cadastro", {
      method: "POST",
      body: JSON.stringify({ titulo, materia, preco, estoque }),
    }),
  atualizar: (id, dados) =>
    request(`/livros/atualizar/${id}`, {
      method: "PUT",
      body: JSON.stringify(dados),
    }),
  deletar: (id, confirmado) =>
    request(`/livros/deletar/${id}?confirmado=${confirmado ? "true" : "false"}`, {
      method: "DELETE",
    }),
};

export const usuariosApi = {
  listar: () => request("/usuarios"),
  login: (email, senha) =>
    request("/usuarios/login", {
      method: "POST",
      body: JSON.stringify({ email, senha }),
    }),
  verificar2FA: (email, codigo) =>
    request("/usuarios/verificar-2fa", {
      method: "POST",
      body: JSON.stringify({ email, codigo }),
    }),
  renovarToken: () => request("/usuarios/renovar-token", { method: "POST" }),
  recuperarSenha: (email) =>
    request("/usuarios/recuperar-senha", {
      method: "POST",
      body: JSON.stringify({ email }),
    }),
  redefinirSenha: (email, codigo, novaSenha) =>
    request("/usuarios/redefinir-senha", {
      method: "POST",
      body: JSON.stringify({ email, codigo, novaSenha }),
    }),
  cadastrar: (dados) =>
    request("/usuarios/cadastro", {
      method: "POST",
      body: JSON.stringify(dados),
    }),
  atualizar: (id, dados) =>
    request(`/usuarios/atualizar/${id}`, {
      method: "PUT",
      body: JSON.stringify(dados),
    }),
  deletar: (id) => request(`/usuarios/deletar/${id}`, { method: "DELETE" }),

  listarFavoritos: (usuarioId) => request(`/usuarios/${usuarioId}/favoritos`),
  favoritar: (usuarioId, livroId) =>
    request(`/usuarios/${usuarioId}/favoritar/${livroId}`, { method: "POST" }),
  desfavoritar: (usuarioId, livroId) =>
    request(`/usuarios/${usuarioId}/favoritar/${livroId}`, { method: "DELETE" }),
};

export const reservasApi = {
  listarPorUsuario: (usuarioId) => request(`/reservas/usuario/${usuarioId}`),
  reservar: (usuarioId, livroId) =>
    request(`/reservas/${usuarioId}/${livroId}`, { method: "POST" }),
  cancelar: (reservaId) =>
    request(`/reservas/${reservaId}/cancelar`, { method: "PUT" }),
  devolver: (reservaId) =>
    request(`/reservas/${reservaId}/devolver`, { method: "PUT" }),
};

export const logsApi = {
  listar: () => request("/logs"),
  listarPorUsuario: (usuarioId) => request(`/logs/usuarios/${usuarioId}`),
};

export { API_URL };
