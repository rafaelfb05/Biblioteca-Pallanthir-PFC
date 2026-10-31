package br.com.pfc.biblioteca.service;

import br.com.pfc.biblioteca.dto.AvaliacaoDTO;
import br.com.pfc.biblioteca.dto.AvaliacaoRequest;
import br.com.pfc.biblioteca.dto.RankingLivroDTO;
import br.com.pfc.biblioteca.dto.ResumoAvaliacoesDTO;
import br.com.pfc.biblioteca.entity.jpa.Avaliacao;
import br.com.pfc.biblioteca.entity.jpa.Livro;
import br.com.pfc.biblioteca.entity.jpa.Usuario;
import br.com.pfc.biblioteca.infra.exception.RecursoNaoEncontradoException;
import br.com.pfc.biblioteca.infra.exception.RegraDeNegocioException;
import br.com.pfc.biblioteca.repository.AvaliacaoRepository;
import br.com.pfc.biblioteca.repository.LivroRepository;
import br.com.pfc.biblioteca.repository.UsuarioRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AvaliacaoService {

    private static final long MINIMO_AVALIACOES = 3;
    private final AvaliacaoRepository repository;
    private final LivroRepository livroRepository;
    private final UsuarioRepository usuarioRepository;
    private final LogAuditoriaService logService;


    public AvaliacaoService(AvaliacaoRepository repository, LivroRepository livroRepository, UsuarioRepository usuarioRepository, LogAuditoriaService logService) {
        this.repository = repository;
        this.livroRepository = livroRepository;
        this.usuarioRepository = usuarioRepository;
        this.logService = logService;
    }

    public AvaliacaoDTO avaliarLivro(Long livroId, AvaliacaoRequest request){
        Usuario usuario = getUsuarioLogado();

        Livro livro = livroRepository.findById(livroId)
                .filter(Livro::isAtivo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Livro não encontrado"));

        Avaliacao avaliacao = repository.findByUsuarioIdAndLivroId(usuario.getId(), livroId)
                .orElse(new Avaliacao(usuario, livro, request.nota(), request.comentario()));

        avaliacao.setNota(request.nota());
        avaliacao.setComentario(request.comentario());
        avaliacao.setDataAvaliacao(LocalDateTime.now());
        avaliacao.setAtivo(true);

        Avaliacao salva = repository.save(avaliacao);
        logService.registrarLog(usuario.getId(), usuario.getEmail(), "AVALIACAO_REGISTRADA",
                "Livro id " + livroId + ", nota " + request.nota());
        return new AvaliacaoDTO(salva);
    }

    public ResumoAvaliacoesDTO obterResumo(Long livroId){
        Double media = repository.calcularMedia(livroId);
        Double mediaArredondada = media == null ? 0.0 : Math.round(media * 10) / 10.0;

        List<AvaliacaoDTO> lista = repository
                .findByLivroIdAndAtivoTrueOrderByDataAvaliacaoDesc(livroId)
                .stream()
                .map(AvaliacaoDTO::new)
                .collect(Collectors.toList());

        return new ResumoAvaliacoesDTO(mediaArredondada, lista.size(), lista);
    }

    public void removerAvaliacao(Long avaliacaoId){
        Usuario usuario = getUsuarioLogado();

        Avaliacao avaliacao = repository.findById(avaliacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Avaliação não encontrada"));

        if(!avaliacao.getUsuario().getId().equals(usuario.getId())){
            throw new RegraDeNegocioException("Você só pode remover a própria avaliação");
        }

        avaliacao.setAtivo(false);
        repository.save(avaliacao);
        logService.registrarLog(usuario.getId(), usuario.getEmail(), "AVALIACAO_REMOVIDA",
                "avaliacao id " + avaliacaoId);
    }

    public List<RankingLivroDTO> obterRanking(){
        return repository.buscarRanking(MINIMO_AVALIACOES, PageRequest.of(0, 10))
                .stream()
                .map(r -> new RankingLivroDTO(
                        r.id(), r.titulo(), r.capaUrl(), Math.round(r.media() * 10) / 10.0, r.total()))
                .collect(Collectors.toList());
    }

    private Usuario getUsuarioLogado(){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmailAndAtivoTrue(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
    }
}
