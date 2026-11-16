import { documentosApi } from "./api";

const resumo = (texto, semente) => {
  let h1 = 0xdeadbeef ^ semente;
  let h2 = 0x41c6ce57 ^ semente;
  for (let i = 0; i < texto.length; i++) {
    const codigo = texto.charCodeAt(i);
    h1 = Math.imul(h1 ^ codigo, 2654435761);
    h2 = Math.imul(h2 ^ codigo, 1597334677);
  }
  h1 =
    Math.imul(h1 ^ (h1 >>> 16), 2246822507) ^
    Math.imul(h2 ^ (h2 >>> 13), 3266489909);
  h2 =
    Math.imul(h2 ^ (h2 >>> 16), 2246822507) ^
    Math.imul(h1 ^ (h1 >>> 13), 3266489909);
  return (4294967296 * (2097151 & h2) + (h1 >>> 0))
    .toString(16)
    .padStart(14, "0");
};

export const versaoDosTermos = (...textos) => {
  const conteudo = textos
    .map((texto) => String(texto).replace(/\r\n/g, "\n").trim())
    .join("\n\u0000\n");
  return resumo(conteudo, 1) + resumo(conteudo, 2);
};

export const carregarVersaoDosTermos = async () => {
  const documentos = (await documentosApi.listar()) || [];
  const conteudo = (tipo) => {
    const documento = documentos.find((item) => item.tipo === tipo);
    if (!documento) {
      throw new Error("Documento não encontrado: " + tipo);
    }
    return documento.conteudo;
  };
  return versaoDosTermos(conteudo("TERMOS_DE_USO"), conteudo("POLITICA_DE_PRIVACIDADE"));
};

export const carregarPoliticaDeReservas = async () => {
  const documento = await documentosApi.obter("POLITICA_DE_RESERVAS");
  if (!documento || !documento.conteudo) {
    throw new Error("Não foi possível carregar a política de reservas");
  }
  return { texto: documento.conteudo, versao: versaoDosTermos(documento.conteudo) };
};
