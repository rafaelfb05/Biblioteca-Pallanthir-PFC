package br.com.pfc.biblioteca.service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.MessageDigest;
import br.com.pfc.biblioteca.dto.auth.*;
import br.com.pfc.biblioteca.dto.livro.LivroDTO;
import br.com.pfc.biblioteca.dto.usuario.AceiteTermosRequest;
import br.com.pfc.biblioteca.dto.usuario.TermosDTO;
import br.com.pfc.biblioteca.dto.usuario.UsuarioDTO;
import br.com.pfc.biblioteca.dto.usuario.UsuarioRequest;
import br.com.pfc.biblioteca.enums.StatusMulta;
import br.com.pfc.biblioteca.enums.StatusPedido;
import br.com.pfc.biblioteca.enums.StatusReserva;
import br.com.pfc.biblioteca.enums.TipoUsuario;
import br.com.pfc.biblioteca.infra.exception.ConflitoException;
import br.com.pfc.biblioteca.infra.exception.CredenciaisInvalidasException;
import br.com.pfc.biblioteca.infra.exception.RecursoNaoEncontradoException;
import br.com.pfc.biblioteca.infra.exception.RegraDeNegocioException;
import br.com.pfc.biblioteca.infra.security.JwtService;
import br.com.pfc.biblioteca.entity.jpa.Livro;
import br.com.pfc.biblioteca.entity.jpa.Usuario;
import br.com.pfc.biblioteca.repository.LivroRepository;
import br.com.pfc.biblioteca.repository.MultaRepository;
import br.com.pfc.biblioteca.repository.PedidoRepository;
import br.com.pfc.biblioteca.repository.ReservaRepository;
import br.com.pfc.biblioteca.repository.UsuarioRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    private final UsuarioRepository repository;
    private final LivroRepository livroRepository;
    private final ReservaRepository reservaRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final LogAuditoriaService logService;
    private final PedidoRepository pedidoRepository;
    private final MultaRepository multaRepository;
    private final AvisoDisponibilidadeService avisoService;
    private final DocumentoService documentoService;
    private static final SecureRandom GERADOR = new SecureRandom();
    private static final int TENTATIVAS_MAXIMAS_CODIGO = 5;
    private static final String CODIGO_INVALIDO = "Código inválido ou expirado";

    public UsuarioService(UsuarioRepository repository, LivroRepository livroRepository, ReservaRepository reservaRepository,
                          PasswordEncoder passwordEncoder, JwtService jwtService, EmailService emailService,
                          LogAuditoriaService logService, PedidoRepository pedidoRepository, MultaRepository multaRepository,
                          AvisoDisponibilidadeService avisoService, DocumentoService documentoService) {
        this.documentoService = documentoService;
        this.multaRepository = multaRepository;
        this.avisoService = avisoService;
        this.repository = repository;
        this.livroRepository = livroRepository;
        this.reservaRepository = reservaRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.logService = logService;
        this.pedidoRepository = pedidoRepository;
    }


    private List<UsuarioDTO> converteDados(List<Usuario> usuario){
        return usuario.stream()
                .map(u -> new UsuarioDTO(
                        u.getId(), u.getNome(),
                        u.getEmail(), u.getTipo(), null))
                .collect(Collectors.toList());
    }

    public Usuario cadastrarUsuario(UsuarioRequest request) {

        boolean emailExiste = repository.findByEmailAndAtivoTrue(request.email()).isPresent();
        if(emailExiste){
            logService.registrarLog(null, request.email(), "FALHA_CADASTRO", "Email já cadastrado");
            throw new ConflitoException("Já existe um usuário ativo com esse email");
        }

        if(!Boolean.TRUE.equals(request.aceitouTermos())){
            logService.registrarLog(null, request.email(), "FALHA_CADASTRO", "Termos de Uso e Política de Privacidade não aceitos");
            throw new RegraDeNegocioException("É necessário aceitar os Termos de Uso e a Política de Privacidade");
        }

        if(request.tipo() == TipoUsuario.GERENTE){
            logService.registrarLog(null, request.email(), "FALHA_CADASTRO", "Tentativa de cadastro como gerente");
            throw new RegraDeNegocioException("Não é possível criar uma conta de gerente pelo cadastro");
        }

        if(request.tipo() == TipoUsuario.FUNCIONARIO){
            String codigoCorreto = System.getenv("CODIGO_FUNCIONARIO");

            if(request.codigoAcesso() == null || !request.codigoAcesso().equals(codigoCorreto)){
                logService.registrarLog(null, request.email(), "FALHA_CADASTRO", "Código de funcionário inválido");
                throw new RegraDeNegocioException("Código inválido para cadastro de funcionário");
            }
        }

        Usuario usuario = new Usuario(request);
        usuario.setSenha(passwordEncoder.encode(request.senha()));
        usuario.setDataAceiteTermos(LocalDateTime.now());
        usuario.setVersaoTermos(versaoVigente(request.versaoTermos(), request.email()));
        Usuario salvo = repository.save(usuario);
        logService.registrarLog(salvo.getId(), salvo.getEmail(), "CADASTRO_USUARIO", "Usuário cadastrado como " + salvo.getTipo());
        registrarAceiteDosTermos(salvo, "no cadastro");
        return salvo;
    }

    public TermosDTO statusDosTermos() {
        Usuario usuario = usuarioLogado();
        return new TermosDTO(usuario.getVersaoTermos(), usuario.getDataAceiteTermos());
    }

    @Transactional
    public TermosDTO aceitarTermos(AceiteTermosRequest request) {
        Usuario usuario = usuarioLogado();
        usuario.setVersaoTermos(versaoVigente(request.versao(), usuario.getEmail()));
        usuario.setDataAceiteTermos(LocalDateTime.now());
        Usuario salvo = repository.save(usuario);
        registrarAceiteDosTermos(salvo, "após atualização dos documentos");
        return new TermosDTO(salvo.getVersaoTermos(), salvo.getDataAceiteTermos());
    }

    private String versaoVigente(String versao, String email) {
        String vigente = documentoService.versaoVigenteDosTermos();
        if (versao == null || !vigente.equals(versao.trim())) {
            logService.registrarLog(null, email, "FALHA_ACEITE_TERMOS", "Versão dos termos diferente da versão vigente");
            throw new ConflitoException("Os Termos de Uso foram atualizados. Recarregue a página e leia a nova versão.");
        }
        return vigente;
    }

    private void registrarAceiteDosTermos(Usuario usuario, String momento) {
        logService.registrarLog(usuario.getId(), usuario.getEmail(), "ACEITE_TERMOS",
                "Aceitou os Termos de Uso e a Política de Privacidade " + momento
                        + (usuario.getVersaoTermos() == null ? "" : " (versão " + usuario.getVersaoTermos() + ")"));
    }

    public List<UsuarioDTO> obterUsuarios() {
        return converteDados(repository.findByAtivoTrue());
    }

    private Usuario usuarioLogado(){
        String emailLogado = SecurityContextHolder.getContext().getAuthentication().getName();
        return repository.findByEmailAndAtivoTrue(emailLogado)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
    }

    private void garantirPropriaConta(UUID usuarioId, String acao){
        Usuario logado = usuarioLogado();
        if(!logado.getId().equals(usuarioId)){
            logService.registrarLog(logado.getId(), logado.getEmail(), "ACESSO_NEGADO", "Tentativa de " + acao + " do usuário " + usuarioId);
            throw new RegraDeNegocioException("Você só pode alterar a sua própria conta");
        }
    }

    public Usuario atualizarUsuario(UUID id, UsuarioRequest request) {
        garantirPropriaConta(id, "atualizar a conta");
        Usuario usuario = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
        if(request.nome() != null && !request.nome().isBlank()){
            usuario.setNome(request.nome());
        }
        String novoEmail = request.email() == null ? "" : request.email().trim();
        if(!novoEmail.isEmpty() && !novoEmail.equalsIgnoreCase(usuario.getEmail())){
            if(!novoEmail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")){
                throw new RegraDeNegocioException("Informe um e-mail válido");
            }
            if(repository.findByEmailAndAtivoTrue(novoEmail).isPresent()){
                throw new ConflitoException("Já existe um usuário ativo com esse email");
            }
            String codigo = gerarCodigo();
            usuario.setNovoEmailPendente(novoEmail);
            usuario.setCodigoNovoEmail(codigo);
            usuario.setExpiracaoCodigoNovoEmail(LocalDateTime.now().plusMinutes(15));
            usuario.setTentativasNovoEmail(0);
            emailService.enviarCodigo(novoEmail, "Confirme seu novo e-mail - Biblioteca Pallanthir",
                    "Confirme seu novo e-mail",
                    "Use o código abaixo para confirmar a troca do e-mail da sua conta na Biblioteca Pallanthir.",
                    codigo, 15);
            logService.registrarLog(usuario.getId(), usuario.getEmail(), "SOLICITACAO_TROCA_EMAIL", "Código de confirmação enviado ao novo e-mail");
        }
        Usuario salvo = repository.save(usuario);
        logService.registrarLog(salvo.getId(), salvo.getEmail(), "ATUALIZACAO_USUARIO", "Dados do usuário atualizados");
        return salvo;
    }

    @Transactional
    public Usuario confirmarNovoEmail(String codigo) {
        Usuario usuario = usuarioLogado();
        if (usuario.getNovoEmailPendente() == null) {
            throw new RegraDeNegocioException("Não há troca de e-mail pendente");
        }
        if (!codigoConfere(usuario.getCodigoNovoEmail(), usuario.getExpiracaoCodigoNovoEmail(), codigo)) {
            int tentativas = usuario.getTentativasNovoEmail() + 1;
            usuario.setTentativasNovoEmail(tentativas);
            boolean esgotou = tentativas >= TENTATIVAS_MAXIMAS_CODIGO;
            if (esgotou) {
                limparTrocaDeEmail(usuario);
            }
            repository.save(usuario);
            logService.registrarLog(usuario.getId(), usuario.getEmail(), "FALHA_TROCA_EMAIL", "Código de confirmação incorreto ou expirado");
            throw new RegraDeNegocioException(esgotou
                    ? "Muitas tentativas com o código errado. Peça a troca do e-mail novamente."
                    : CODIGO_INVALIDO);
        }
        String novoEmail = usuario.getNovoEmailPendente();
        if (repository.findByEmailAndAtivoTrue(novoEmail).isPresent()) {
            limparTrocaDeEmail(usuario);
            repository.save(usuario);
            throw new ConflitoException("Já existe um usuário ativo com esse email");
        }
        String emailAnterior = usuario.getEmail();
        usuario.setEmail(novoEmail);
        limparTrocaDeEmail(usuario);
        Usuario salvo = repository.save(usuario);
        logService.registrarLog(salvo.getId(), salvo.getEmail(), "TROCA_EMAIL", "E-mail alterado de " + emailAnterior);
        return salvo;
    }

    private void limparTrocaDeEmail(Usuario usuario) {
        usuario.setNovoEmailPendente(null);
        usuario.setCodigoNovoEmail(null);
        usuario.setExpiracaoCodigoNovoEmail(null);
        usuario.setTentativasNovoEmail(0);
    }

    private boolean codigoConfere(String esperado, LocalDateTime expiracao, String informado) {
        return esperado != null && informado != null && expiracao != null
                && expiracao.isAfter(LocalDateTime.now())
                && MessageDigest.isEqual(esperado.getBytes(StandardCharsets.UTF_8), informado.trim().getBytes(StandardCharsets.UTF_8));
    }

    @Transactional
    public Usuario excluirUsuario(UUID id) {
        String emailLogado = SecurityContextHolder.getContext().getAuthentication().getName();

        Usuario usuario = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
        if(!usuario.getEmail().equals(emailLogado)){
            logService.registrarLogUsuarioLogado("ACESSO_NEGADO", "Tentativa de excluir a conta do usuário " + id);
            throw new RegraDeNegocioException("Você só pode excluir sua própria conta");
        }

        if (usuario.getTipo() == TipoUsuario.GERENTE && repository.travarAtivosPorTipo(TipoUsuario.GERENTE).size() <= 1) {
            logService.registrarLog(usuario.getId(), usuario.getEmail(), "FALHA_EXCLUSAO_USUARIO", "Último gerente ativo tentou excluir a própria conta");
            throw new ConflitoException("Você é o único gerente do sistema. Promova outro funcionário a gerente na página Equipe antes de excluir a sua conta.");
        }

        if(reservaRepository.existsByUsuarioIdAndStatusIn(id, List.of(StatusReserva.RESERVADO, StatusReserva.ENTREGUE))){
            logService.registrarLog(usuario.getId(), usuario.getEmail(), "FALHA_EXCLUSAO_USUARIO", "Conta possui reservas em aberto ou livros não devolvidos");
            throw new ConflitoException("Não é possível excluir a conta enquanto houver livros reservados ou não devolvidos. Cancele as reservas ou devolva os livros na biblioteca.");
        }

        boolean temPedidoEmAberto = pedidoRepository.existsByUsuarioIdAndStatusIn(
                usuario.getId(),
                List.of(StatusPedido.AGUARDANDO_PAGAMENTO, StatusPedido.APROVADO));

        if (multaRepository.existsByUsuarioIdAndStatus(usuario.getId(), StatusMulta.PENDENTE)) {
            throw new ConflitoException("Você tem uma multa pendente. Pague a multa antes de excluir a conta.");
        }

        if (temPedidoEmAberto) {
            throw new ConflitoException(
                    "Você tem pedidos em andamento (aguardando pagamento ou retirada). "
                            + "Conclua a retirada antes de excluir a conta.");
        }

        Usuario salvo = anonimizar(usuario);
        logService.registrarLog(salvo.getId(), salvo.getEmail(), "EXCLUSAO_USUARIO", "Conta excluída e dados anonimizados");
        return salvo;
    }

    private Usuario anonimizar(Usuario usuario) {
        avisoService.cancelarTodosDoUsuario(usuario.getId());
        usuario.setNome("Usuário removido");
        usuario.setEmail("removido-" + usuario.getId() + "@anonimizado.local");
        usuario.setSenha(passwordEncoder.encode(UUID.randomUUID().toString()));
        usuario.setCodigo2FA(null);
        usuario.setExpiracao2FA(null);
        usuario.setCodigoRecuperacao(null);
        usuario.setExpiracaoCodigo(null);
        usuario.getFavoritos().clear();
        usuario.setAtivo(false);
        return repository.save(usuario);
    }

    private Usuario membroDaEquipe(UUID id, Usuario gerente, String acao) {
        Usuario alvo = repository.findById(id)
                .filter(Usuario::isAtivo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
        if (alvo.getId().equals(gerente.getId())) {
            logService.registrarLog(gerente.getId(), gerente.getEmail(), "ACESSO_NEGADO", "Tentativa de " + acao + " da própria conta");
            throw new RegraDeNegocioException("Você não pode " + acao + " da sua própria conta");
        }
        if (alvo.getTipo() == TipoUsuario.ESTUDANTE) {
            throw new RegraDeNegocioException("Esta ação só vale para funcionários e gerentes");
        }
        return alvo;
    }

    @Transactional
    public Usuario alterarPerfil(UUID id, TipoUsuario novoTipo) {
        Usuario gerente = usuarioLogado();
        Usuario alvo = membroDaEquipe(id, gerente, "alterar o perfil");
        if (novoTipo != TipoUsuario.FUNCIONARIO && novoTipo != TipoUsuario.GERENTE) {
            throw new RegraDeNegocioException("O perfil deve ser Funcionário ou Gerente");
        }
        if (alvo.getTipo() == novoTipo) {
            return alvo;
        }
        if (alvo.getTipo() == TipoUsuario.GERENTE && repository.travarAtivosPorTipo(TipoUsuario.GERENTE).size() <= 1) {
            throw new ConflitoException("O sistema precisa ter pelo menos um gerente");
        }
        TipoUsuario anterior = alvo.getTipo();
        alvo.setTipo(novoTipo);
        Usuario salvo = repository.save(alvo);
        logService.registrarLog(gerente.getId(), gerente.getEmail(), "ALTERACAO_PERFIL",
                "Perfil de " + salvo.getEmail() + " alterado de " + anterior + " para " + novoTipo);
        return salvo;
    }

    @Transactional
    public void excluirFuncionario(UUID id) {
        Usuario gerente = usuarioLogado();
        Usuario alvo = membroDaEquipe(id, gerente, "excluir");
        if (alvo.getTipo() != TipoUsuario.FUNCIONARIO) {
            throw new RegraDeNegocioException("Para excluir um gerente, altere o perfil dele para Funcionário antes");
        }
        String email = alvo.getEmail();
        anonimizar(alvo);
        logService.registrarLog(gerente.getId(), gerente.getEmail(), "EXCLUSAO_FUNCIONARIO",
                "Conta do funcionário " + email + " excluída e dados anonimizados");
    }
    @Transactional
    public void adicionarFavorito(UUID usuarioId, UUID livroId){
        garantirPropriaConta(usuarioId, "favoritar livros");
        Usuario usuario = repository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));

        Livro livro = livroRepository.findById(livroId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Livro não encontrado!"));

        boolean jaFavoritado = usuario.getFavoritos().stream()
                .anyMatch(l -> l.getId().equals(livro.getId()));

        if (!jaFavoritado) {
            usuario.getFavoritos().add(livro);
            repository.save(usuario);
            logService.registrarLog(usuario.getId(), usuario.getEmail(), "FAVORITAR_LIVRO", "Livro " + livro.getId() + " adicionado aos favoritos");
        }
    }

    @Transactional
    public void removerFavorito(UUID usuarioId, UUID livroId) {
        garantirPropriaConta(usuarioId, "remover favoritos");
        Usuario usuario = repository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
        usuario.getFavoritos().removeIf(l -> l.getId().equals(livroId));
        repository.save(usuario);
        logService.registrarLog(usuario.getId(), usuario.getEmail(), "DESFAVORITAR_LIVRO", "Livro " + livroId + " removido dos favoritos");

    }

    public List<LivroDTO> listarFavoritos(UUID usuarioId) {
        garantirPropriaConta(usuarioId, "listar os favoritos");
        Usuario usuario = repository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));

        return usuario.getFavoritos().stream()
                .map(LivroDTO::new)
                .collect(Collectors.toList());
    }

    public void login(LoginRequest request){
        Usuario usuario = repository.findByEmailAndAtivoTrue(request.email())
                .orElseThrow(() -> {
                    logService.registrarLog(null, request.email(), "FALHA_LOGIN", "Email não encontrado");
                    return new CredenciaisInvalidasException("Email ou senha incorretos");
                });

        if(usuario.getTempoBloqueio() != null && usuario.getTempoBloqueio().isAfter(LocalDateTime.now())){
            logService.registrarLog(usuario.getId(), usuario.getEmail(), "LOGIN_BLOQUEADO", "Bloqueio contra força bruta ativado");
            throw new RegraDeNegocioException("Conta bloqueada, tente novamente mais tarde");
        }

        if(!passwordEncoder.matches(request.senha(), usuario.getSenha())){
            usuario.setTentativasFalhas(usuario.getTentativasFalhas() + 1);
            if(usuario.getTentativasFalhas() >= 3){
                usuario.setTempoBloqueio(LocalDateTime.now().plusMinutes(15));
            }
            repository.save(usuario);
            logService.registrarLog(usuario.getId(), usuario.getEmail(), "FALHA_LOGIN", "Senha incorreta");
            throw new CredenciaisInvalidasException("Email ou senha incorretos");
        }

        usuario.setTentativasFalhas(0);
        usuario.setTempoBloqueio(null);
        String codigo = gerarCodigo();
        usuario.setCodigo2FA(codigo);
        usuario.setExpiracao2FA(LocalDateTime.now().plusMinutes(10));
        usuario.setTentativas2FA(0);
        repository.save(usuario);

        emailService.enviarCodigo(usuario.getEmail(), "Código de login - Biblioteca Pallanthir",
                "Confirme seu login",
                "Use o código abaixo para concluir a entrada na sua conta.",
                codigo, 10);

        logService.registrarLog(usuario.getId(), usuario.getEmail(), "SUCESSO_LOGIN", "Código 2FA enviado");
    }

    private String gerarCodigo(){
        return String.valueOf(100000 + GERADOR.nextInt(900000));
    }

    public LoginResponse renovarToken(){
        Usuario usuario = usuarioLogado();
        return new LoginResponse(new UsuarioDTO(usuario), jwtService.gerarToken(usuario));
    }

    public LoginResponse confirmar2FA(Confirmar2FARequest request){
        Usuario usuario = repository.findByEmailAndAtivoTrue(request.email()).orElse(null);
        if (usuario == null) {
            logService.registrarLog(null, request.email(), "FALHA_2FA", "Email não encontrado");
            throw new RegraDeNegocioException(CODIGO_INVALIDO);
        }

        if (!codigoConfere(usuario.getCodigo2FA(), usuario.getExpiracao2FA(), request.codigo())) {
            int tentativas = usuario.getTentativas2FA() + 1;
            usuario.setTentativas2FA(tentativas);
            boolean esgotou = usuario.getCodigo2FA() != null && tentativas >= TENTATIVAS_MAXIMAS_CODIGO;
            if (esgotou) {
                usuario.setCodigo2FA(null);
                usuario.setExpiracao2FA(null);
            }
            repository.save(usuario);
            logService.registrarLog(usuario.getId(), usuario.getEmail(), "FALHA_2FA",
                    esgotou ? "Código 2FA invalidado após " + tentativas + " tentativas erradas" : "Código 2FA inválido ou expirado");
            throw new RegraDeNegocioException(esgotou
                    ? "Muitas tentativas com o código errado. Faça login novamente para receber um novo código."
                    : CODIGO_INVALIDO);
        }

        usuario.setCodigo2FA(null);
        usuario.setExpiracao2FA(null);
        usuario.setTentativas2FA(0);
        repository.save(usuario);

        String token = jwtService.gerarToken(usuario);
        logService.registrarLog(usuario.getId(), usuario.getEmail(), "SUCESSO_2FA", "Login concluído, token gerado");
        return new LoginResponse(new UsuarioDTO(usuario), token);
    }

    public void recuperacaoSenha(SolicitarRecuperacaoRequest request){
        Usuario usuario = repository.findByEmailAndAtivoTrue(request.email()).orElse(null);
        if (usuario == null) {
            logService.registrarLog(null, request.email(), "FALHA_RECUPERACAO_SENHA", "Email não encontrado");
            return;
        }
        LocalDateTime agora = LocalDateTime.now();
        if (usuario.getDataCodigoRecuperacao() != null && usuario.getDataCodigoRecuperacao().isAfter(agora.minusMinutes(1))) {
            logService.registrarLog(usuario.getId(), usuario.getEmail(), "RECUPERACAO_SENHA_REPETIDA", "Novo pedido em menos de 1 minuto, código não reenviado");
            return;
        }

        String codigo = gerarCodigo();
        usuario.setCodigoRecuperacao(codigo);
        usuario.setExpiracaoCodigo(agora.plusMinutes(15));
        usuario.setDataCodigoRecuperacao(agora);
        usuario.setTentativasRecuperacao(0);
        repository.save(usuario);
        emailService.enviarCodigo(usuario.getEmail(), "Recuperação de Conta - Biblioteca Pallanthir",
                "Recuperação de conta",
                "Recebemos uma solicitação para redefinir a senha da sua conta. Use o código abaixo para continuar.",
                codigo, 15);
        logService.registrarLog(usuario.getId(), usuario.getEmail(), "SOLICITACAO_RECUPERACAO_SENHA", "Código de recuperação enviado");
    }

    public void redefinirSenha(RedefinirSenhaRequest request){
        Usuario usuario = repository.findByEmailAndAtivoTrue(request.email()).orElse(null);
        if (usuario == null) {
            logService.registrarLog(null, request.email(), "FALHA_REDEFINICAO_SENHA", "Email não encontrado");
            throw new RegraDeNegocioException(CODIGO_INVALIDO);
        }

        if (!codigoConfere(usuario.getCodigoRecuperacao(), usuario.getExpiracaoCodigo(), request.codigo())) {
            int tentativas = usuario.getTentativasRecuperacao() + 1;
            usuario.setTentativasRecuperacao(tentativas);
            boolean esgotou = usuario.getCodigoRecuperacao() != null && tentativas >= TENTATIVAS_MAXIMAS_CODIGO;
            if (esgotou) {
                usuario.setCodigoRecuperacao(null);
                usuario.setExpiracaoCodigo(null);
            }
            repository.save(usuario);
            logService.registrarLog(usuario.getId(), usuario.getEmail(), "FALHA_REDEFINICAO_SENHA",
                    esgotou ? "Código de recuperação invalidado após " + tentativas + " tentativas erradas" : "Código de recuperação inválido ou expirado");
            throw new RegraDeNegocioException(esgotou
                    ? "Muitas tentativas com o código errado. Peça um novo código de recuperação."
                    : CODIGO_INVALIDO);
        }

        usuario.setSenha(passwordEncoder.encode(request.novaSenha()));
        usuario.setCodigoRecuperacao(null);
        usuario.setExpiracaoCodigo(null);
        usuario.setTentativasRecuperacao(0);
        usuario.setDataCodigoRecuperacao(null);
        repository.save(usuario);
        logService.registrarLog(usuario.getId(), usuario.getEmail(), "REDEFINICAO_SENHA", "Senha redefinida com sucesso");
    }
}
