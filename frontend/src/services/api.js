// URL do back-end. Em desenvolvimento vem do .env.development (localhost:8080)
// e na Vercel vem da variavel de ambiente REACT_APP_API_URL.
const API_URL = (process.env.REACT_APP_API_URL || "http://localhost:8080").replace(/\/$/, "");

async function request(caminho, opcoes = {}) {
  const resposta = await fetch(`${API_URL}${caminho}`, {
    headers: { "Content-Type": "application/json" },
    ...opcoes,
  });

  if (!resposta.ok) {
    throw new Error(`Erro ${resposta.status} ao chamar ${caminho}`);
  }

  // DELETE responde 204 sem corpo
  if (resposta.status === 204) {
    return null;
  }

  return resposta.json();
}

export const livrosApi = {
  listar: () => request("/livros"),
  buscarPorTitulo: (titulo) => request(`/livros/titulo/${encodeURIComponent(titulo)}`),
  filtrarPorMateria: (materia) => request(`/livros/${encodeURIComponent(materia)}`),
  cadastrar: (titulo, materia) =>
    request("/livros/cadastro", {
      method: "POST",
      body: JSON.stringify({ titulo, materia }),
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
