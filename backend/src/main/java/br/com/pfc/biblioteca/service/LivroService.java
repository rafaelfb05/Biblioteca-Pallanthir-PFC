package br.com.pfc.biblioteca.service;

import br.com.pfc.biblioteca.dto.AtualizarLivroRequest;
import br.com.pfc.biblioteca.dto.DadosLivro;
import br.com.pfc.biblioteca.dto.LivroDTO;
import br.com.pfc.biblioteca.dto.MateriaDTO;
import br.com.pfc.biblioteca.enums.Materia;
import br.com.pfc.biblioteca.model.Livro;
import br.com.pfc.biblioteca.repository.LivroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LivroService {

    @Autowired
    private LivroRepository repository;
    private ConsumoApi consumo = new ConsumoApi();
    private ConverteDados conversor = new ConverteDados();
    private final String ENDERECO = "https://www.googleapis.com/books/v1/volumes?q=";
    private final String API_KEY = System.getenv("BOOK_API");


    public Livro salvarLivro(String titulo, Materia materia, Double preco, Integer estoque) {
        var json = consumo.obterDados(ENDERECO + titulo.replace(" ", "+") + "&key=" + API_KEY);
        DadosLivro.DadosBusca dadosBusca = conversor.obterDados(json, DadosLivro.DadosBusca.class);

        if (dadosBusca.items() == null || dadosBusca.items().isEmpty()) {
            throw new RuntimeException("Livro não encontrado na API");
        }
        DadosLivro dados = dadosBusca.items().get(0).volumeInfo();
        Livro livro = new Livro(dados);
        livro.setMateria(materia);
        livro.setPreco(preco);
        livro.setEstoque(estoque);
        return repository.save(livro);
    }

    public List<LivroDTO> obterTodosOsLivros() {
        return converterDados(repository.findAll());

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

    public void deletarLivro(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("livro não encontrado");
        }
        repository.deleteById(id);
    }

    public List<MateriaDTO> listarMaterias() {
        return Arrays.stream(Materia.values())
                .map(m -> new MateriaDTO(m.name(), m.getMateriaLivro(), repository.countByMateria(m)))
                .collect(Collectors.toList());
    }

    public List<LivroDTO> filtarPorMateria(Materia materia) {
        return converterDados(repository.findByMateria(materia));
    }

    public Optional<LivroDTO> buscarPorNome(String titulo) {
        return repository.findByTituloContainingIgnoreCase(titulo)
                .map(LivroDTO::new);

    }
}