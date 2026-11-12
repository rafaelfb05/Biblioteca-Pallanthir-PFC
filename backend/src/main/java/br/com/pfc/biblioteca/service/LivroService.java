package br.com.pfc.biblioteca.service;

import java.util.UUID;
import br.com.pfc.biblioteca.dto.livro.AtualizarLivroRequest;
import br.com.pfc.biblioteca.dto.livro.DadosLivro;
import br.com.pfc.biblioteca.dto.livro.LivroDTO;
import br.com.pfc.biblioteca.dto.livro.MateriaDTO;
import br.com.pfc.biblioteca.enums.Materia;
import br.com.pfc.biblioteca.enums.StatusReserva;
import br.com.pfc.biblioteca.infra.exception.ConflitoException;
import br.com.pfc.biblioteca.infra.exception.RecursoNaoEncontradoException;
import br.com.pfc.biblioteca.infra.exception.RegraDeNegocioException;
import br.com.pfc.biblioteca.entity.jpa.Livro;
import br.com.pfc.biblioteca.repository.LivroRepository;
import br.com.pfc.biblioteca.repository.ReservaRepository;
import br.com.pfc.biblioteca.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LivroService {

    private final LivroRepository repository;
    private final ReservaRepository reservaRepository;
    private final UsuarioRepository usuarioRepository;
    private final LogAuditoriaService logService;
    private ConsumoApi consumo = new ConsumoApi();
    private ConverteDados conversor = new ConverteDados();
    private final String ENDERECO = "https://www.googleapis.com/books/v1/volumes?q=";
    private final String API_KEY = System.getenv("BOOK_API");

    public LivroService(LivroRepository repository, ReservaRepository reservaRepository, UsuarioRepository usuarioRepository, LogAuditoriaService logService) {
        this.repository = repository;
        this.reservaRepository = reservaRepository;
        this.usuarioRepository = usuarioRepository;
        this.logService = logService;
    }


    public Livro salvarLivro(String titulo, Materia materia, Double preco, Integer estoqueReserva, Integer estoqueCompra) {
        String json;
        try {
            json = consumo.obterDados(ENDERECO + URLEncoder.encode(titulo.trim(), StandardCharsets.UTF_8)
                    + "&key=" + URLEncoder.encode(String.valueOf(API_KEY), StandardCharsets.UTF_8));
        } catch (RegraDeNegocioException e) {
            logService.registrarLogUsuarioLogado("FALHA_CADASTRO_LIVRO", "Livro \"" + titulo + "\": " + e.getMessage());
            throw e;
        }
        DadosLivro.DadosBusca dadosBusca = conversor.obterDados(json, DadosLivro.DadosBusca.class);

        if (dadosBusca.items() == null || dadosBusca.items().isEmpty()) {
            logService.registrarLogUsuarioLogado("FALHA_CADASTRO_LIVRO", "Livro \"" + titulo + "\" não encontrado na API do Google Books");
            throw new RecursoNaoEncontradoException("Livro não encontrado na API do Google Books");
        }
        DadosLivro dados = dadosBusca.items().get(0).volumeInfo();
        Livro livro = new Livro(dados);
        livro.setMateria(materia);
        livro.setPreco(preco);
        livro.setEstoqueReserva(estoqueReserva);
        livro.setEstoqueCompra(estoqueCompra);
        Livro salvo = repository.save(livro);
        logService.registrarLogUsuarioLogado("CADASTRO_LIVRO", "Livro " + salvo.getId() + " (" + salvo.getTitulo() + ") cadastrado");
        return salvo;
    }

    public List<LivroDTO> obterTodosOsLivros() {
        return converterDados(repository.findByAtivoTrue());

    }

    private List<LivroDTO> converterDados(List<Livro> livro) {
        return livro.stream()
                .map(LivroDTO::new)
                .collect(Collectors.toList());
    }

    public Livro atualizarLivro(UUID id, AtualizarLivroRequest request) {
        Livro livro = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Livro não encontrado"));

        if (request.titulo() != null) {
            livro.setTitulo(request.titulo());
        }
        if (request.autores() != null) {
            livro.setAutores(request.autores());
        }
        if (request.anoLancamento() != null) {
            livro.setAnoLancamento(request.anoLancamento());
        }
        if (request.numeroPagina() != null) {
            livro.setNumeroPagina(request.numeroPagina());
        }
        if (request.materia() != null) {
            livro.setMateria(request.materia());
        }
        if (request.preco() != null){
            livro.setPreco(request.preco());
        }
        if (request.estoqueReserva() != null) {
            livro.setEstoqueReserva(request.estoqueReserva());
        }
        if (request.estoqueCompra() != null) {
            livro.setEstoqueCompra(request.estoqueCompra());
        }
        if (request.isbn10() != null) {
            livro.setIsbn10(normalizarIsbn(request.isbn10(), "^\\d{9}[\\dX]$", "ISBN-10 inválido: informe 10 caracteres (9 números e um número ou X no final)"));
        }
        if (request.isbn13() != null) {
            livro.setIsbn13(normalizarIsbn(request.isbn13(), "^\\d{13}$", "ISBN-13 inválido: informe 13 números"));
        }
        Livro salvo = repository.save(livro);
        logService.registrarLogUsuarioLogado("ATUALIZACAO_LIVRO", "Livro " + salvo.getId() + " (" + salvo.getTitulo() + ") atualizado");
        return salvo;
    }

    private String normalizarIsbn(String valor, String formato, String mensagem) {
        String limpo = valor.replaceAll("[\\s-]", "").toUpperCase();
        if (limpo.isEmpty()) {
            return null;
        }
        if (!limpo.matches(formato)) {
            throw new RegraDeNegocioException(mensagem);
        }
        return limpo;
    }

    @Transactional
    public void deletarLivro(UUID id, boolean confirmado) {
        Livro livro = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Livro não encontrado"));

        if (!confirmado && reservaRepository.existsByLivroIdAndStatusIn(id, List.of(StatusReserva.RESERVADO, StatusReserva.ENTREGUE))) {
            throw new ConflitoException("Este livro possui reservas em aberto.");
        }

        usuarioRepository.buscarPorLivroFavoritado(id).forEach(usuario -> {
            usuario.getFavoritos().removeIf(favorito -> favorito.getId().equals(id));
            usuarioRepository.save(usuario);
        });

        livro.setAtivo(false);
        repository.save(livro);
        logService.registrarLogUsuarioLogado("EXCLUSAO_LIVRO", "Livro " + livro.getId() + " (" + livro.getTitulo() + ") excluído");
    }

    public List<MateriaDTO> listarMaterias() {
        return Arrays.stream(Materia.values())
                .map(m -> new MateriaDTO(m.name(), m.getMateriaLivro(), repository.countByMateria(m)))
                .collect(Collectors.toList());
    }

    public List<LivroDTO> filtarPorMateria(Materia materia) {
        return converterDados(repository.findByMateriaAndAtivoTrue(materia));
    }

    public Optional<LivroDTO> buscarPorNome(String titulo) {
        return repository.findByTituloContainingIgnoreCaseAndAtivoTrue(titulo)
                .map(LivroDTO::new);

    }
}