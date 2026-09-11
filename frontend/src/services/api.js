const API_URL = (
  process.env.REACT_APP_API_URL || "http://localhost:8080"
).replace(/\/$/, "");

async function request(caminho, opcoes = {}) {
  const resposta = await fetch(`${API_URL}${caminho}`, {
    headers: { "Content-Type": "application/json" },
    ...opcoes,
  });

  if (!resposta.ok) {
    throw new Error(`Erro ${resposta.status} ao chamar ${caminho}`);
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
  cadastrar: (titulo, materia, preco) =>
    request("/livros/cadastro", {
      method: "POST",
      body: JSON.stringify({ titulo, materia, preco }),
    }),
  atualizar: (id, dados) =>
    request(`/livros/atualizar/${id}`, {
      method: "PUT",
      body: JSON.stringify(dados),
    }),
  deletar: (id) => request(`/livros/deletar/${id}`, { method: "DELETE" }),
};

export const usuariosApi = {
  listar: () => request("/usuarios"),
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

export { API_URL };
