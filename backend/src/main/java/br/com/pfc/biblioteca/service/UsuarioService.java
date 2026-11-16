package br.com.pfc.biblioteca.service;

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

    public UsuarioService(UsuarioRepository repository, LivroRepository livroRepository, ReservaRepository reservaRepository,
                          PasswordEncoder passwordEncoder, JwtService jwtService, EmailService emailService,
                          LogAuditoriaService logService, PedidoRepository pedidoRepository, MultaRepository multaRepository,
                          AvisoDisponibilidadeService avisoService) {
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
                        u.getEmail(), u.getTipo()))
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
        usuario.setVersaoTermos(versaoValida(request.versaoTermos()));
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
        usuario.setVersaoTermos(versaoValida(request.versao()));
        usuario.setDataAceiteTermos(LocalDateTime.now());
        Usuario salvo = repository.save(usuario);
        registrarAceiteDosTermos(salvo, "após atualização dos documentos");
        return new TermosDTO(salvo.getVersaoTermos(), salvo.getDataAceiteTermos());
    }

    private String versaoValida(String versao) {
        if (versao == null || versao.isBlank()) {
            return null;
        }
        String limpa = versao.trim();
        if (!limpa.matches("^[A-Za-z0-9_-]{1,64}$")) {
            throw new RegraDeNegocioException("Versão dos termos inválida");
        }
        return limpa;
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
        if(request.email() != null && !request.email().isBlank() && !request.email().equals(usuario.getEmail())){
            if(repository.findByEmailAndAtivoTrue(request.email()).isPresent()){
                throw new ConflitoException("Já existe um usuário ativo com esse email");
            }
            usuario.setEmail(request.email());
        }
        Usuario salvo = repository.save(usuario);
        logService.registrarLog(salvo.getId(), salvo.getEmail(), "ATUALIZACAO_USUARIO", "Dados do usuário atualizados");
        return salvo;
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
        if (alvo.getTipo() == TipoUsuario.GERENTE && repository.countByTipoAndAtivoTrue(TipoUsuario.GERENTE) <= 1) {
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
        repository.save(usuario);

        emailService.enviarCodigo(usuario.getEmail(), "Código de login - Biblioteca Pallanthir",
                "Confirme seu login",
                "Use o código abaixo para concluir a entrada na sua conta.",
                codigo, 10);

        logService.registrarLog(usuario.getId(), usuario.getEmail(), "SUCESSO_LOGIN", "Código 2FA enviado");
    }

    private String gerarCodigo(){
        return String.valueOf((int) (Math.random() * 900000) + 100000);
    }

    public LoginResponse renovarToken(){
        Usuario usuario = usuarioLogado();
        return new LoginResponse(new UsuarioDTO(usuario), jwtService.gerarToken(usuario));
    }

    public LoginResponse confirmar2FA(Confirmar2FARequest request){
        Usuario usuario = repository.findByEmailAndAtivoTrue(request.email())
                .orElseThrow(() -> {
                    logService.registrarLog(null, request.email(), "FALHA_2FA", "Email não encontrado");
                    return new RecursoNaoEncontradoException("Usuário não encontrado");
                });

        if(usuario.getCodigo2FA() == null
                || !usuario.getCodigo2FA().equals(request.codigo())
                || usuario.getExpiracao2FA().isBefore(LocalDateTime.now())){
            logService.registrarLog(usuario.getId(), usuario.getEmail(), "FALHA_2FA", "Código 2FA inválido ou expirado");
            throw new RegraDeNegocioException("Código invalido ou expirado!");
        }

        usuario.setCodigo2FA(null);
        usuario.setExpiracao2FA(null);
        repository.save(usuario);

        String token = jwtService.gerarToken(usuario);
        logService.registrarLog(usuario.getId(), usuario.getEmail(), "SUCESSO_2FA", "Login concluído, token gerado");
        return new LoginResponse(new UsuarioDTO(usuario), token);
    }

    public void recuperacaoSenha(SolicitarRecuperacaoRequest request){
        Usuario usuario = repository.findByEmailAndAtivoTrue(request.email())
                .orElseThrow(() -> {
                    logService.registrarLog(null, request.email(), "FALHA_RECUPERACAO_SENHA", "Email não encontrado");
                    return new RecursoNaoEncontradoException("Usuário não encontrado");
                });

        String codigo = gerarCodigo();
        usuario.setCodigoRecuperacao(codigo);
        usuario.setExpiracaoCodigo(LocalDateTime.now().plusMinutes(15));
        repository.save(usuario);
        emailService.enviarCodigo(usuario.getEmail(), "Recuperação de Conta - Biblioteca Pallanthir",
                "Recuperação de conta",
                "Recebemos uma solicitação para redefinir a senha da sua conta. Use o código abaixo para continuar.",
                codigo, 15);
        logService.registrarLog(usuario.getId(), usuario.getEmail(), "SOLICITACAO_RECUPERACAO_SENHA", "Código de recuperação enviado");
    }

    public void redefinirSenha(RedefinirSenhaRequest request){
        Usuario usuario = repository.findByEmailAndAtivoTrue(request.email())
                .orElseThrow(() -> {
                    logService.registrarLog(null, request.email(), "FALHA_REDEFINICAO_SENHA", "Email não encontrado");
                    return new RecursoNaoEncontradoException("Usuário não encontrado");
                });

        if(usuario.getCodigoRecuperacao() == null
        || !usuario.getCodigoRecuperacao().equals(request.codigo())
        || usuario.getExpiracaoCodigo().isBefore(LocalDateTime.now())){
            logService.registrarLog(usuario.getId(), usuario.getEmail(), "FALHA_REDEFINICAO_SENHA", "Código de recuperação inválido ou expirado");
            throw new RegraDeNegocioException("Código de recuperção invalido ou expirado!");
        }

        usuario.setSenha(passwordEncoder.encode(request.novaSenha()));
        usuario.setCodigoRecuperacao(null);
        usuario.setExpiracaoCodigo(null);
        repository.save(usuario);
        logService.registrarLog(usuario.getId(), usuario.getEmail(), "REDEFINICAO_SENHA", "Senha redefinida com sucesso");
    }
}
