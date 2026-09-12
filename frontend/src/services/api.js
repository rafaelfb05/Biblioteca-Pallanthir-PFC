const API_URL = (
  process.env.REACT_APP_API_URL || "http://localhost:8080"
).replace(/\/$/, "");

async function request(caminho, opcoes = {}) {
  const resposta = await fetch(`${API_URL}${caminho}`, {
    headers: { "Content-Type": "application/json" },
    ...opcoes,
  });

  if (!resposta.ok) {
    const corpo = await resposta.text();
    let mensagem = `Erro ${resposta.status} ao chamar ${caminho}`;
    try {
      const dados = JSON.parse(corpo);
      if (dados && dados.message) {
        mensagem = dados.message;
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

export { API_URL };
