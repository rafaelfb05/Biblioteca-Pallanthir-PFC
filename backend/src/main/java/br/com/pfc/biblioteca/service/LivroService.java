package br.com.pfc.biblioteca.service;

import br.com.pfc.biblioteca.dto.AtualizarLivroRequest;
import br.com.pfc.biblioteca.dto.DadosLivro;
import br.com.pfc.biblioteca.dto.LivroDTO;
import br.com.pfc.biblioteca.dto.MateriaDTO;
import br.com.pfc.biblioteca.enums.Materia;
import br.com.pfc.biblioteca.enums.StatusReserva;
import br.com.pfc.biblioteca.infra.exception.ConflitoException;
import br.com.pfc.biblioteca.infra.exception.RecursoNaoEncontradoException;
import br.com.pfc.biblioteca.entity.Livro;
import br.com.pfc.biblioteca.repository.LivroRepository;
import br.com.pfc.biblioteca.repository.ReservaRepository;
import br.com.pfc.biblioteca.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LivroService {

    private final LivroRepository repository;
    private final ReservaRepository reservaRepository;
    private final UsuarioRepository usuarioRepository;
    private ConsumoApi consumo = new ConsumoApi();
    private ConverteDados conversor = new ConverteDados();
    private final String ENDERECO = "https://www.googleapis.com/books/v1/volumes?q=";
    private final String API_KEY = System.getenv("BOOK_API");

    public LivroService(LivroRepository repository, ReservaRepository reservaRepository, UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.reservaRepository = reservaRepository;
        this.usuarioRepository = usuarioRepository;
    }


    public Livro salvarLivro(String titulo, Materia materia, Double preco, Integer estoque) {
        var json = consumo.obterDados(ENDERECO + titulo.replace(" ", "+") + "&key=" + API_KEY);
        DadosLivro.DadosBusca dadosBusca = conversor.obterDados(json, DadosLivro.DadosBusca.class);

        if (dadosBusca.items() == null || dadosBusca.items().isEmpty()) {
            throw new RecursoNaoEncontradoException("Livro não encontrado na API do Google Books");
        }
        DadosLivro dados = dadosBusca.items().get(0).volumeInfo();
        Livro livro = new Livro(dados);
        livro.setMateria(materia);
        livro.setPreco(preco);
        livro.setEstoque(estoque);
        return repository.save(livro);
    }

    public List<LivroDTO> obterTodosOsLivros() {
        return converterDados(repository.findByAtivoTrue());

    }

    private List<LivroDTO> converterDados(List<Livro> livro) {
        return livro.stream()
                .map(s -> new LivroDTO(s.getId(), s.getTitulo(), s.getAutores(),
                        s.getAnoLancamento(), s.getNumeroPagina(),
                        s.getCapaUrl(), s.getMateria(), s.getPreco(),
                        s.getEstoque(), s.getAvaliacao()))
                .collect(Collectors.toList());
    }

    public Livro atualizarLivro(Long id, AtualizarLivroRequest request) {
        Livro livro = repository.findById(id).orElse(null);

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
        if (request.avaliacao() != null) {
            livro.setAvaliacao(request.avaliacao());
        }
        if (request.materia() != null) {
            livro.setMateria(request.materia());
        }
        if (request.preco() != null){
            livro.setPreco(request.preco());
        }
        if (request.estoque() != null) {
            livro.setEstoque(request.estoque());
        }
        return repository.save(livro);
    }

    @Transactional
    public void deletarLivro(Long id, boolean confirmado) {
        Livro livro = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Livro não encontrado"));

        if (!confirmado && reservaRepository.existsByLivroIdAndStatus(id, StatusReserva.RESERVADO)) {
            throw new ConflitoException("Este livro possui reservas em aberto.");
        }

        usuarioRepository.buscarPorLivroFavoritado(id).forEach(usuario -> {
            usuario.getFavoritos().removeIf(favorito -> favorito.getId().equals(id));
            usuarioRepository.save(usuario);
        });

        livro.setAtivo(false);
        repository.save(livro);
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