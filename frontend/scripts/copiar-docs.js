const fs = require("fs");
const path = require("path");

const origem = path.resolve(__dirname, "..", "..", "docs");
const destino = path.resolve(__dirname, "..", "public", "docs");
const documentos = ["TERMOS_DE_USO.md", "POLITICA_DE_PRIVACIDADE.md"];

fs.mkdirSync(destino, { recursive: true });

documentos.forEach((documento) => {
  const arquivo = path.join(origem, documento);
  if (fs.existsSync(arquivo)) {
    fs.copyFileSync(arquivo, path.join(destino, documento));
  }
});
