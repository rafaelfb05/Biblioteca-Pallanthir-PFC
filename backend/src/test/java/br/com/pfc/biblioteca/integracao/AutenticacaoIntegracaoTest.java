package br.com.pfc.biblioteca.integracao;

import br.com.pfc.biblioteca.entity.jpa.Livro;
import br.com.pfc.biblioteca.entity.jpa.Usuario;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class AutenticacaoIntegracaoTest extends TesteIntegracaoBase {

    private Map<String, Object> cadastro(String email, String senha, String tipo, Boolean aceitouTermos, String codigo) {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("nome", "Pessoa de Teste");
        corpo.put("email", email);
        corpo.put("senha", senha);
        corpo.put("tipo", tipo);
        corpo.put("aceitouTermos", aceitouTermos);
        corpo.put("codigoAcesso", codigo);
        corpo.put("versaoTermos", documentoService.versaoVigenteDosTermos());
        return corpo;
    }

    private String loginECapturarCodigo(String email, String senha) {
        Resposta resposta = post("/usuarios/login", null, Map.of("email", email, "senha", senha));
        assertThat(resposta.status()).isEqualTo(200);
        ArgumentCaptor<String> codigo = ArgumentCaptor.forClass(String.class);
        verify(emailService).enviarCodigo(eq(email), anyString(), anyString(), anyString(), codigo.capture(), eq(10));
        return codigo.getValue();
    }

    @Test
    void cadastroDeEstudanteGuardaSenhaComoHashERegistraAceite() {
        String email = emailUnico();

        Resposta resposta = post("/usuarios/cadastro", null, cadastro(email, SENHA, "ESTUDANTE", true, null));

        assertThat(resposta.status()).isEqualTo(200);
        assertThat(resposta.corpo().path("tipo").asText()).isEqualTo("ESTUDANTE");
        Usuario salvo = usuarioRepository.findByEmailAndAtivoTrue(email).orElseThrow();
        assertThat(salvo.getSenha()).isNotEqualTo(SENHA);
        assertThat(passwordEncoder.matches(SENHA, salvo.getSenha())).isTrue();
        assertThat(salvo.getDataAceiteTermos()).isNotNull();
    }

    @Test
    void cadastroGuardaAVersaoDosTermosERegistraOAceiteNosLogs() {
        String email = emailUnico();
        Map<String, Object> corpo = cadastro(email, SENHA, "ESTUDANTE", true, null);
        String vigente = documentoService.versaoVigenteDosTermos();

        Resposta resposta = post("/usuarios/cadastro", null, corpo);

        assertThat(resposta.status()).isEqualTo(200);
        Usuario salvo = usuarioRepository.findByEmailAndAtivoTrue(email).orElseThrow();
        assertThat(salvo.getVersaoTermos()).isEqualTo(vigente);
        verify(logService).registrarLog(eq(salvo.getId()), eq(email), eq("ACEITE_TERMOS"), contains("versão " + vigente));

        Map<String, Object> desatualizado = cadastro(emailUnico(), SENHA, "ESTUDANTE", true, null);
        desatualizado.put("versaoTermos", "versao-antiga");
        assertThat(post("/usuarios/cadastro", null, desatualizado).status()).isEqualTo(409);
    }

    @Test
    void usuarioAceitaANovaVersaoDosTermos() {
        Usuario estudante = criarEstudante();
        estudante.setVersaoTermos(null);
        usuarioRepository.save(estudante);
        String token = token(estudante);
        String vigente = documentoService.versaoVigenteDosTermos();

        assertThat(get("/usuarios/termos", token).corpo().path("versaoAceita").isNull()).isTrue();

        Resposta aceite = post("/usuarios/termos/aceite", token, Map.of("versao", vigente));

        assertThat(aceite.status()).isEqualTo(200);
        assertThat(aceite.corpo().path("versaoAceita").asText()).isEqualTo(vigente);
        assertThat(get("/usuarios/termos", token).corpo().path("versaoAceita").asText()).isEqualTo(vigente);
        Usuario salvo = usuarioRepository.findById(estudante.getId()).orElseThrow();
        assertThat(salvo.getVersaoTermos()).isEqualTo(vigente);
        assertThat(salvo.getDataAceiteTermos()).isNotNull();
        verify(logService).registrarLog(eq(estudante.getId()), eq(estudante.getEmail()), eq("ACEITE_TERMOS"), contains("após atualização"));
    }

    @Test
    void aceiteDosTermosRecusaVersaoInvalidaEExigeLogin() {
        String token = token(criarFuncionario());

        assertThat(post("/usuarios/termos/aceite", token, Map.of("versao", "versão <script>")).status()).isEqualTo(409);
        assertThat(post("/usuarios/termos/aceite", token, Map.of("versao", "versao-antiga")).status()).isEqualTo(409);
        assertThat(post("/usuarios/termos/aceite", token, Map.of("versao", "")).status()).isEqualTo(400);
        assertThat(post("/usuarios/termos/aceite", null, Map.of("versao", "v1")).status()).isIn(401, 403);
    }

    @Test
    void cadastroRecusaSenhaFraca() {
        Resposta resposta = post("/usuarios/cadastro", null, cadastro(emailUnico(), "abc12345", "ESTUDANTE", true, null));

        assertThat(resposta.status()).isEqualTo(400);
    }

    @Test
    void cadastroExigeAceiteDosTermos() {
        String email = emailUnico();

        Resposta resposta = post("/usuarios/cadastro", null, cadastro(email, SENHA, "ESTUDANTE", false, null));

        assertThat(resposta.status()).isEqualTo(400);
        assertThat(resposta.mensagem()).contains("Termos de Uso");
        assertThat(usuarioRepository.findByEmailAndAtivoTrue(email)).isEmpty();
    }

    @Test
    void cadastroRecusaEmailJaUsado() {
        String email = criarEstudante().getEmail();

        Resposta resposta = post("/usuarios/cadastro", null, cadastro(email, SENHA, "ESTUDANTE", true, null));

        assertThat(resposta.status()).isEqualTo(409);
    }

    @Test
    void cadastroDeFuncionarioRecusaCodigoInvalido() {
        String email = emailUnico();

        Resposta resposta = post("/usuarios/cadastro", null, cadastro(email, SENHA, "FUNCIONARIO", true, "codigo-errado"));

        assertThat(resposta.status()).isEqualTo(400);
        assertThat(usuarioRepository.findByEmailAndAtivoTrue(email)).isEmpty();
    }

    @Test
    void cadastroDeFuncionarioAceitaCodigoCorreto() {
        String codigo = System.getenv("CODIGO_FUNCIONARIO");
        assumeTrue(codigo != null && !codigo.isBlank());

        Resposta resposta = post("/usuarios/cadastro", null, cadastro(emailUnico(), SENHA, "FUNCIONARIO", true, codigo));

        assertThat(resposta.status()).isEqualTo(200);
        assertThat(resposta.corpo().path("tipo").asText()).isEqualTo("FUNCIONARIO");
    }

    @Test
    void loginEnviaCodigoEVerificacaoDevolveTokenValido() {
        Usuario usuario = criarEstudante();

        String codigo = loginECapturarCodigo(usuario.getEmail(), SENHA);
        Resposta verificacao = post("/usuarios/verificar-2fa", null, Map.of("email", usuario.getEmail(), "codigo", codigo));

        assertThat(codigo).matches("\\d{6}");
        assertThat(verificacao.status()).isEqualTo(200);
        String token = verificacao.corpo().path("token").asText();
        assertThat(token).isNotBlank();
        assertThat(get("/reservas/usuario/" + usuario.getId(), token).status()).isEqualTo(200);
    }

    @Test
    void codigoDeVerificacaoSoPodeSerUsadoUmaVez() {
        Usuario usuario = criarEstudante();
        String codigo = loginECapturarCodigo(usuario.getEmail(), SENHA);
        Map<String, String> corpo = Map.of("email", usuario.getEmail(), "codigo", codigo);

        assertThat(post("/usuarios/verificar-2fa", null, corpo).status()).isEqualTo(200);
        assertThat(post("/usuarios/verificar-2fa", null, corpo).status()).isEqualTo(400);
    }

    @Test
    void codigoDeVerificacaoIncorretoERecusado() {
        Usuario usuario = criarEstudante();
        String codigo = loginECapturarCodigo(usuario.getEmail(), SENHA);
        String errado = codigo.equals("123456") ? "654321" : "123456";

        Resposta resposta = post("/usuarios/verificar-2fa", null, Map.of("email", usuario.getEmail(), "codigo", errado));

        assertThat(resposta.status()).isEqualTo(400);
    }

    @Test
    void codigoDeVerificacaoExpiradoERecusado() {
        Usuario usuario = criarEstudante();
        String codigo = loginECapturarCodigo(usuario.getEmail(), SENHA);
        Usuario salvo = usuarioRepository.findById(usuario.getId()).orElseThrow();
        salvo.setExpiracao2FA(LocalDateTime.now().minusMinutes(1));
        usuarioRepository.save(salvo);

        Resposta resposta = post("/usuarios/verificar-2fa", null, Map.of("email", usuario.getEmail(), "codigo", codigo));

        assertThat(resposta.status()).isEqualTo(400);
    }

    @Test
    void falhaDeLoginUsaAMesmaMensagemParaEmailESenha() {
        Usuario usuario = criarEstudante();

        Resposta emailInexistente = post("/usuarios/login", null, Map.of("email", emailUnico(), "senha", SENHA));
        Resposta senhaErrada = post("/usuarios/login", null, Map.of("email", usuario.getEmail(), "senha", "Errada@123"));

        assertThat(emailInexistente.status()).isEqualTo(401);
        assertThat(senhaErrada.status()).isEqualTo(401);
        assertThat(emailInexistente.mensagem()).isEqualTo(senhaErrada.mensagem());
    }

    @Test
    void contaEBloqueadaAposTresSenhasErradas() {
        Usuario usuario = criarEstudante();
        Map<String, String> senhaErrada = Map.of("email", usuario.getEmail(), "senha", "Errada@123");

        for (int i = 0; i < 3; i++) {
            assertThat(post("/usuarios/login", null, senhaErrada).status()).isEqualTo(401);
        }
        Resposta comSenhaCorreta = post("/usuarios/login", null, Map.of("email", usuario.getEmail(), "senha", SENHA));

        assertThat(comSenhaCorreta.status()).isEqualTo(400);
        assertThat(comSenhaCorreta.mensagem()).contains("bloqueada");
        verify(emailService, never()).enviarCodigo(eq(usuario.getEmail()), anyString(), anyString(), anyString(), anyString(), eq(10));
    }

    @Test
    void loginCorretoZeraAsTentativasErradas() {
        Usuario usuario = criarEstudante();
        Map<String, String> senhaErrada = Map.of("email", usuario.getEmail(), "senha", "Errada@123");
        post("/usuarios/login", null, senhaErrada);
        post("/usuarios/login", null, senhaErrada);

        loginECapturarCodigo(usuario.getEmail(), SENHA);

        assertThat(usuarioRepository.findById(usuario.getId()).orElseThrow().getTentativasFalhas()).isZero();
    }

    @Test
    void recuperacaoDeSenhaTrocaASenhaComCodigoDeUsoUnico() {
        Usuario usuario = criarEstudante();
        assertThat(post("/usuarios/recuperar-senha", null, Map.of("email", usuario.getEmail())).status()).isEqualTo(200);
        ArgumentCaptor<String> codigo = ArgumentCaptor.forClass(String.class);
        verify(emailService).enviarCodigo(eq(usuario.getEmail()), anyString(), anyString(), anyString(), codigo.capture(), eq(15));
        Map<String, String> redefinicao = Map.of("email", usuario.getEmail(), "codigo", codigo.getValue(), "novaSenha", "NovaSenha@456");

        assertThat(post("/usuarios/redefinir-senha", null, redefinicao).status()).isEqualTo(200);
        assertThat(post("/usuarios/login", null, Map.of("email", usuario.getEmail(), "senha", SENHA)).status()).isEqualTo(401);
        assertThat(post("/usuarios/login", null, Map.of("email", usuario.getEmail(), "senha", "NovaSenha@456")).status()).isEqualTo(200);
        assertThat(post("/usuarios/redefinir-senha", null, redefinicao).status()).isEqualTo(400);
    }

    @Test
    void rotaProtegidaRecusaRequisicaoSemTokenOuComTokenAdulterado() {
        Usuario usuario = criarEstudante();
        String token = token(usuario);
        String adulterado = token.substring(0, token.length() - 4) + "abcd";

        assertThat(get("/reservas/usuario/" + usuario.getId(), null).status()).isIn(401, 403);
        assertThat(get("/reservas/usuario/" + usuario.getId(), adulterado).status()).isIn(401, 403);
    }

    @Test
    void renovacaoDeTokenDevolveNovoToken() {
        Usuario usuario = criarEstudante();

        Resposta resposta = post("/usuarios/renovar-token", token(usuario), null);

        assertThat(resposta.status()).isEqualTo(200);
        assertThat(resposta.corpo().path("token").asText()).isNotBlank();
        assertThat(resposta.corpo().path("usuario").path("email").asText()).isEqualTo(usuario.getEmail());
    }

    @Test
    void usuarioNaoAlteraAContaDeOutraPessoa() {
        Usuario usuario = criarEstudante();
        Usuario outro = criarEstudante();

        Resposta resposta = put("/usuarios/atualizar/" + outro.getId(), token(usuario), Map.of("nome", "Invasor"));

        assertThat(resposta.status()).isEqualTo(400);
        assertThat(usuarioRepository.findById(outro.getId()).orElseThrow().getNome()).isNotEqualTo("Invasor");
    }

    @Test
    void exclusaoDeContaAnonimizaOsDados() {
        Usuario usuario = criarEstudante();
        String email = usuario.getEmail();

        Resposta resposta = delete("/usuarios/deletar/" + usuario.getId(), token(usuario));

        assertThat(resposta.status()).isEqualTo(204);
        Usuario anonimizado = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertThat(anonimizado.getNome()).isEqualTo("Usuário removido");
        assertThat(anonimizado.getEmail()).isNotEqualTo(email).endsWith("@anonimizado.local");
        assertThat(anonimizado.isAtivo()).isFalse();
        assertThat(post("/usuarios/login", null, Map.of("email", email, "senha", SENHA)).status()).isEqualTo(401);
    }

    @Test
    void exclusaoDeContaEBloqueadaComReservaEmAberto() {
        Usuario usuario = criarEstudante();
        Livro livro = criarLivro(30, 1, 0);
        assertThat(post("/reservas/" + usuario.getId() + "/" + livro.getId(), token(usuario), null).status()).isEqualTo(200);

        Resposta resposta = delete("/usuarios/deletar/" + usuario.getId(), token(usuario));

        assertThat(resposta.status()).isEqualTo(409);
        assertThat(usuarioRepository.findById(usuario.getId()).orElseThrow().isAtivo()).isTrue();
    }

    @Test
    void codigoDeRecuperacaoEInvalidadoDepoisDeCincoErros() {
        Usuario usuario = criarEstudante();
        post("/usuarios/recuperar-senha", null, Map.of("email", usuario.getEmail()));
        ArgumentCaptor<String> codigo = ArgumentCaptor.forClass(String.class);
        verify(emailService).enviarCodigo(eq(usuario.getEmail()), anyString(), anyString(), anyString(), codigo.capture(), eq(15));
        String errado = codigo.getValue().equals("123456") ? "654321" : "123456";

        for (int i = 0; i < 4; i++) {
            Resposta resposta = post("/usuarios/redefinir-senha", null, Map.of("email", usuario.getEmail(), "codigo", errado, "novaSenha", "NovaSenha@456"));
            assertThat(resposta.mensagem()).isEqualTo("Código inválido ou expirado");
        }
        Resposta quinta = post("/usuarios/redefinir-senha", null, Map.of("email", usuario.getEmail(), "codigo", errado, "novaSenha", "NovaSenha@456"));
        assertThat(quinta.mensagem()).contains("Muitas tentativas");

        Resposta comCodigoCerto = post("/usuarios/redefinir-senha", null, Map.of("email", usuario.getEmail(), "codigo", codigo.getValue(), "novaSenha", "NovaSenha@456"));
        assertThat(comCodigoCerto.status()).isEqualTo(400);
        assertThat(passwordEncoder.matches(SENHA, usuarioRepository.findById(usuario.getId()).orElseThrow().getSenha())).isTrue();
    }

    @Test
    void recuperacaoNaoRevelaSeOEmailExisteENemReenviaEmMenosDeUmMinuto() {
        String inexistente = emailUnico();
        assertThat(post("/usuarios/recuperar-senha", null, Map.of("email", inexistente)).status()).isEqualTo(200);
        verify(emailService, never()).enviarCodigo(eq(inexistente), anyString(), anyString(), anyString(), anyString(), eq(15));
        Resposta redefinir = post("/usuarios/redefinir-senha", null, Map.of("email", inexistente, "codigo", "123456", "novaSenha", "NovaSenha@456"));
        assertThat(redefinir.status()).isEqualTo(400);
        assertThat(redefinir.mensagem()).isEqualTo("Código inválido ou expirado");

        Usuario usuario = criarEstudante();
        assertThat(post("/usuarios/recuperar-senha", null, Map.of("email", usuario.getEmail())).status()).isEqualTo(200);
        assertThat(post("/usuarios/recuperar-senha", null, Map.of("email", usuario.getEmail())).status()).isEqualTo(200);
        verify(emailService, times(1)).enviarCodigo(eq(usuario.getEmail()), anyString(), anyString(), anyString(), anyString(), eq(15));
    }

    @Test
    void codigoDeVerificacaoEInvalidadoDepoisDeCincoErros() {
        Usuario usuario = criarEstudante();
        String codigo = loginECapturarCodigo(usuario.getEmail(), SENHA);
        String errado = codigo.equals("123456") ? "654321" : "123456";

        for (int i = 0; i < 5; i++) {
            post("/usuarios/verificar-2fa", null, Map.of("email", usuario.getEmail(), "codigo", errado));
        }
        Resposta comCodigoCerto = post("/usuarios/verificar-2fa", null, Map.of("email", usuario.getEmail(), "codigo", codigo));

        assertThat(comCodigoCerto.status()).isEqualTo(400);
        Resposta emailInexistente = post("/usuarios/verificar-2fa", null, Map.of("email", emailUnico(), "codigo", "123456"));
        assertThat(emailInexistente.status()).isEqualTo(400);
        assertThat(emailInexistente.mensagem()).isEqualTo("Código inválido ou expirado");
    }

    @Test
    void trocaDeEmailSoAconteceDepoisDeConfirmarOCodigoEnviadoAoNovoEmail() {
        Usuario usuario = criarEstudante();
        String token = token(usuario);
        String novo = emailUnico();

        Resposta pedido = put("/usuarios/atualizar/" + usuario.getId(), token, Map.of("nome", usuario.getNome(), "email", novo));
        assertThat(pedido.status()).isEqualTo(200);
        assertThat(pedido.corpo().path("email").asText()).isEqualTo(usuario.getEmail());
        assertThat(pedido.corpo().path("emailPendente").asText()).isEqualTo(novo);
        ArgumentCaptor<String> codigo = ArgumentCaptor.forClass(String.class);
        verify(emailService).enviarCodigo(eq(novo), anyString(), anyString(), anyString(), codigo.capture(), eq(15));
        assertThat(usuarioRepository.findById(usuario.getId()).orElseThrow().getEmail()).isEqualTo(usuario.getEmail());

        String errado = codigo.getValue().equals("123456") ? "654321" : "123456";
        assertThat(post("/usuarios/confirmar-email", token, Map.of("codigo", errado)).status()).isEqualTo(400);

        Resposta confirmado = post("/usuarios/confirmar-email", token, Map.of("codigo", codigo.getValue()));
        assertThat(confirmado.status()).isEqualTo(200);
        assertThat(confirmado.corpo().path("email").asText()).isEqualTo(novo);
        assertThat(usuarioRepository.findById(usuario.getId()).orElseThrow().getEmail()).isEqualTo(novo);
        assertThat(get("/usuarios/termos", token).status()).isIn(401, 403);
    }

    @Test
    void tokenDeContaExcluidaOuComPerfilAlteradoDeixaDeValerNaHora() {
        Usuario funcionario = criarFuncionario();
        String tokenFuncionario = token(funcionario);
        assertThat(get("/reservas/em-aberto", tokenFuncionario).status()).isEqualTo(200);

        funcionario.setTipo(br.com.pfc.biblioteca.enums.TipoUsuario.ESTUDANTE);
        usuarioRepository.save(funcionario);
        assertThat(get("/reservas/em-aberto", tokenFuncionario).status()).isEqualTo(403);

        Usuario estudante = criarEstudante();
        String tokenEstudante = token(estudante);
        assertThat(delete("/usuarios/deletar/" + estudante.getId(), tokenEstudante).status()).isEqualTo(204);
        assertThat(get("/reservas/usuario/" + estudante.getId(), tokenEstudante).status()).isIn(401, 403);
    }
}

