package br.com.pfc.biblioteca.service;

import br.com.pfc.biblioteca.dto.*;
import br.com.pfc.biblioteca.enums.TipoUsuario;
import br.com.pfc.biblioteca.infra.exception.ConflitoException;
import br.com.pfc.biblioteca.infra.exception.CredenciaisInvalidasException;
import br.com.pfc.biblioteca.infra.exception.RecursoNaoEncontradoException;
import br.com.pfc.biblioteca.infra.exception.RegraDeNegocioException;
import br.com.pfc.biblioteca.infra.security.JwtService;
import br.com.pfc.biblioteca.entity.Livro;
import br.com.pfc.biblioteca.entity.Usuario;
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

    public UsuarioService(UsuarioRepository repository, LivroRepository livroRepository, PasswordEncoder passwordEncoder, JwtService jwtService, EmailService emailService) {
        this.repository = repository;
        this.livroRepository = livroRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
    }


    private List<UsuarioDTO> converteDados(List<Usuario> usuario){
        return usuario.stream()
                .map(u -> new UsuarioDTO(
                        u.getId(), u.getNome(),
                        u.getEmail(), u.getSenha(), u.getTipo()))
                .collect(Collectors.toList());
    }

    public Usuario cadastrarUsuario(UsuarioRequest request) {

        boolean emailExiste = repository.findByEmailAndAtivoTrue(request.email()).isPresent();
        if(emailExiste){
            throw new ConflitoException("Já existe um usuário ativo com esse email");
        }

        if(request.tipo() == TipoUsuario.FUNCIONARIO){
            String codigoCorreto = System.getenv("CODIGO_FUNCIONARIO");

            if(request.codigoAcesso() == null || !request.codigoAcesso().equals(codigoCorreto)){
                throw new RegraDeNegocioException("Código inválido para cadastro de funcionário");
            }
        }

        Usuario usuario = new Usuario(request);
        usuario.setSenha(passwordEncoder.encode(request.senha()));
        return repository.save(usuario);
    }

    public List<UsuarioDTO> obterUsuarios() {
        return converteDados(repository.findByAtivoTrue());
    }

    public Usuario atualizarUsuario(Long id, UsuarioRequest request) {
        Usuario usuario = repository.findById(id).orElse(null);
        if(request.nome() != null){
            usuario.setNome(request.nome());
        }
        if(request.email() != null){
            usuario.setEmail(request.email());
        }
        if(request.senha() != null){
            usuario.setSenha(request.senha());
        }
        return repository.save(usuario);
    }

    public Usuario excluirUsuario(Long id) {
        String emailLogado = SecurityContextHolder.getContext().getAuthentication().getName();

        Usuario usuario = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
        if(!usuario.getEmail().equals(emailLogado)){
            throw new RegraDeNegocioException("Você só pode excluir sua própria conta");
        }

        usuario.setNome("Usuário removido");
        usuario.setEmail("removido-" + usuario.getId() + "@anonimizado.local");
        usuario.setSenha(null);
        usuario.setAtivo(false);
        return repository.save(usuario);
    }
    @Transactional
    public void adicionarFavorito(Long usuarioId, Long livroId){
        Usuario usuario = repository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));

        Livro livro = livroRepository.findById(livroId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Livro não encontrado!"));

        boolean jaFavoritado = usuario.getFavoritos().stream()
                .anyMatch(l -> l.getId().equals(livro.getId()));

        if (!jaFavoritado) {
            usuario.getFavoritos().add(livro);
            repository.save(usuario);
        }
    }

    @Transactional
    public void removerFavorito(Long usuarioId, Long livroId) {
        Usuario usuario = repository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
        usuario.getFavoritos().removeIf(l -> l.getId().equals(livroId));
        repository.save(usuario);

    }

    public List<LivroDTO> listarFavoritos(Long usuarioId) {
        Usuario usuario = repository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));

        return usuario.getFavoritos().stream()
                .map(LivroDTO::new)
                .collect(Collectors.toList());
    }

    public void login(LoginRequest request){
        Usuario usuario = repository.findByEmailAndAtivoTrue(request.email())
                .orElseThrow(() -> new CredenciaisInvalidasException("Email ou senha incorretos"));

        if(usuario.getTempoBloqueio() != null && usuario.getTempoBloqueio().isAfter(LocalDateTime.now())){
            throw new RegraDeNegocioException("Conta bloqueada, tente novamente mais tarde");
        }

        if(!passwordEncoder.matches(request.senha(), usuario.getSenha())){
            usuario.setTentativasFalhas(usuario.getTentativasFalhas() + 1);
            if(usuario.getTentativasFalhas() >= 3){
                usuario.setTempoBloqueio(LocalDateTime.now().plusMinutes(15));
            }
            repository.save(usuario);
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
    }

    private String gerarCodigo(){
        return String.valueOf((int) (Math.random() * 900000) + 100000);
    }

    public LoginResponse confirmar2FA(Confirmar2FARequest request){
        Usuario usuario = repository.findByEmailAndAtivoTrue(request.email())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));

        if(usuario.getCodigo2FA() == null
                || !usuario.getCodigo2FA().equals(request.codigo())
                || usuario.getExpiracao2FA().isBefore(LocalDateTime.now())){
            throw new RegraDeNegocioException("Código invalido ou expirado!");
        }

        usuario.setCodigo2FA(null);
        usuario.setExpiracao2FA(null);
        repository.save(usuario);

        String token = jwtService.gerarToken(usuario);
        return new LoginResponse(new UsuarioDTO(usuario), token);
    }

    public void recuperacaoSenha(SolicitarRecuperacaoRequest request){
        Usuario usuario = repository.findByEmailAndAtivoTrue(request.email())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));

        String codigo = gerarCodigo();
        usuario.setCodigoRecuperacao(codigo);
        usuario.setExpiracaoCodigo(LocalDateTime.now().plusMinutes(15));
        repository.save(usuario);
        emailService.enviarEmail(usuario.getEmail(),
                "Recuperação de Conta - Biblioteca Pallanthir",
                "Recebemos sua solicitação para recuperação de conta, seu código de acesso é: \n" +
                codigo + "\nO código tem o prazo de válidade de 15 minutos!");
    }

    public void redefinirSenha(RedefinirSenhaRequest request){
        Usuario usuario = repository.findByEmailAndAtivoTrue(request.email())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));

        if(usuario.getCodigoRecuperacao() == null
        || !usuario.getCodigoRecuperacao().equals(request.codigo())
        || usuario.getExpiracaoCodigo().isBefore(LocalDateTime.now())){
            throw new RegraDeNegocioException("Código de recuperção invalido ou expirado!");
        }

        usuario.setSenha(passwordEncoder.encode(request.novaSenha()));
        usuario.setCodigoRecuperacao(null);
        usuario.setExpiracaoCodigo(null);
        repository.save(usuario);
    }
}
