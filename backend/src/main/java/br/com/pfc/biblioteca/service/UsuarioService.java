package br.com.pfc.biblioteca.service;

import br.com.pfc.biblioteca.dto.*;
import br.com.pfc.biblioteca.enums.TipoUsuario;
import br.com.pfc.biblioteca.infra.exception.ConflitoException;
import br.com.pfc.biblioteca.infra.exception.CredenciaisInvalidasException;
import br.com.pfc.biblioteca.infra.exception.RecursoNaoEncontradoException;
import br.com.pfc.biblioteca.infra.exception.RegraDeNegocioException;
import br.com.pfc.biblioteca.infra.security.JwtService;
import br.com.pfc.biblioteca.entity.jpa.Livro;
import br.com.pfc.biblioteca.entity.jpa.Usuario;
import br.com.pfc.biblioteca.repository.LivroRepository;
import br.com.pfc.biblioteca.repository.UsuarioRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    private final UsuarioRepository repository;
    private final LivroRepository livroRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final LogAuditoriaService logService;

    public UsuarioService(UsuarioRepository repository, LivroRepository livroRepository, PasswordEncoder passwordEncoder, JwtService jwtService, EmailService emailService, LogAuditoriaService logService) {
        this.repository = repository;
        this.livroRepository = livroRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.logService = logService;
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

        if(request.tipo() == TipoUsuario.FUNCIONARIO){
            String codigoCorreto = System.getenv("CODIGO_FUNCIONARIO");

            if(request.codigoAcesso() == null || !request.codigoAcesso().equals(codigoCorreto)){
                logService.registrarLog(null, request.email(), "FALHA_CADASTRO", "Código de funcionário inválido");
                throw new RegraDeNegocioException("Código inválido para cadastro de funcionário");
            }
        }

        Usuario usuario = new Usuario(request);
        usuario.setSenha(passwordEncoder.encode(request.senha()));
        Usuario salvo = repository.save(usuario);
        logService.registrarLog(salvo.getId(), salvo.getEmail(), "CADASTRO_USUARIO", "Usuário cadastrado como " + salvo.getTipo());
        return salvo;
    }

    public List<UsuarioDTO> obterUsuarios() {
        return converteDados(repository.findByAtivoTrue());
    }

    private Usuario usuarioLogado(){
        String emailLogado = SecurityContextHolder.getContext().getAuthentication().getName();
        return repository.findByEmailAndAtivoTrue(emailLogado)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
    }

    private void garantirPropriaConta(Long usuarioId, String acao){
        Usuario logado = usuarioLogado();
        if(!logado.getId().equals(usuarioId)){
            logService.registrarLog(logado.getId(), logado.getEmail(), "ACESSO_NEGADO", "Tentativa de " + acao + " do usuário " + usuarioId);
            throw new RegraDeNegocioException("Você só pode alterar a sua própria conta");
        }
    }

    public Usuario atualizarUsuario(Long id, UsuarioRequest request) {
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

    public Usuario excluirUsuario(Long id) {
        String emailLogado = SecurityContextHolder.getContext().getAuthentication().getName();

        Usuario usuario = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
        if(!usuario.getEmail().equals(emailLogado)){
            logService.registrarLogUsuarioLogado("ACESSO_NEGADO", "Tentativa de excluir a conta do usuário " + id);
            throw new RegraDeNegocioException("Você só pode excluir sua própria conta");
        }

        usuario.setNome("Usuário removido");
        usuario.setEmail("removido-" + usuario.getId() + "@anonimizado.local");
        usuario.setSenha(null);
        usuario.setAtivo(false);
        Usuario salvo = repository.save(usuario);
        logService.registrarLog(salvo.getId(), salvo.getEmail(), "EXCLUSAO_USUARIO", "Conta excluída e dados anonimizados");
        return salvo;
    }
    @Transactional
    public void adicionarFavorito(Long usuarioId, Long livroId){
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
    public void removerFavorito(Long usuarioId, Long livroId) {
        garantirPropriaConta(usuarioId, "remover favoritos");
        Usuario usuario = repository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
        usuario.getFavoritos().removeIf(l -> l.getId().equals(livroId));
        repository.save(usuario);
        logService.registrarLog(usuario.getId(), usuario.getEmail(), "DESFAVORITAR_LIVRO", "Livro " + livroId + " removido dos favoritos");

    }

    public List<LivroDTO> listarFavoritos(Long usuarioId) {
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
            throw new CredenciaisInvalidasException("Email ou senha incorretos!");
        }

        usuario.setTentativasFalhas(0);
        usuario.setTempoBloqueio(null);
        String codigo = gerarCodigo();
        usuario.setCodigo2FA(codigo);
        usuario.setExpiracao2FA(LocalDateTime.now().plusMinutes(10));
        repository.save(usuario);

        emailService.enviarEmail(usuario.getEmail(), "Autenticação de 2 fatores",
                "Seu código para login é:\n" + codigo +
                        "\nO código tem o prazo de validade de 10 minutos!");

        logService.registrarLog(usuario.getId(), usuario.getEmail(), "SUCESSO_LOGIN", "Código 2FA enviado");
    }

    private String gerarCodigo(){
        return String.valueOf((int) (Math.random() * 900000) + 100000);
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
        emailService.enviarEmail(usuario.getEmail(),
                "Recuperação de Conta - Biblioteca Pallanthir",
                "Recebemos sua solicitação para recuperação de conta, seu código de acesso é: \n" +
                codigo + "\nO código tem o prazo de válidade de 15 minutos!");
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
