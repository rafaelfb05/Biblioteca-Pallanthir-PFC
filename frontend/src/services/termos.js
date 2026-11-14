const ARQUIVOS_DOS_TERMOS = ["TERMOS_DE_USO.md", "POLITICA_DE_PRIVACIDADE.md"];

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
  const textos = await Promise.all(
    ARQUIVOS_DOS_TERMOS.map(async (arquivo) => {
      const resposta = await fetch(process.env.PUBLIC_URL + "/docs/" + arquivo);
      if (!resposta.ok) {
        throw new Error("Não foi possível carregar " + arquivo);
      }
      return resposta.text();
    })
  );
  return versaoDosTermos(...textos);
};
