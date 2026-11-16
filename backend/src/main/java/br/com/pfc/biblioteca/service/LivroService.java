package br.com.pfc.biblioteca.service;

import java.util.Set;
import java.util.Locale;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.text.Normalizer;
import java.net.URI;
import br.com.pfc.biblioteca.dto.livro.BuscaOpenLibrary;
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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final String CAPAS_OPEN_LIBRARY = "https://covers.openlibrary.org/b/isbn/";
    private static final String BUSCA_OPEN_LIBRARY = "https://openlibrary.org/search.json?fields=title,author_name,cover_i&limit=10&title=";
    private static final String CAPAS_POR_ID = "https://covers.openlibrary.org/b/id/";
    private static final int TAMANHO_MAXIMO_LINK_CAPA = 255;

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
        livroJaCadastrado(livro).ifPresent(existente -> {
            logService.registrarLogUsuarioLogado("FALHA_CADASTRO_LIVRO",
                    "Livro \"" + titulo + "\" já está no acervo como " + existente.getId());
            throw new ConflitoException("\"" + existente.getTitulo() + "\" já está no acervo. "
                    + "Para ter mais exemplares, edite o estoque do livro que já existe.");
        });
        livro.setCapaUrl(buscarCapa(livro).orElse(null));
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
        String isbn10Anterior = livro.getIsbn10();
        String isbn13Anterior = livro.getIsbn13();
        if (request.isbn10() != null) {
            livro.setIsbn10(normalizarIsbn(request.isbn10(), "^\\d{9}[\\dX]$", "ISBN-10 inválido: informe 10 caracteres (9 números e um número ou X no final)"));
        }
        if (request.isbn13() != null) {
            livro.setIsbn13(normalizarIsbn(request.isbn13(), "^\\d{13}$", "ISBN-13 inválido: informe 13 números"));
        }
        boolean isbnMudou = !Objects.equals(isbn10Anterior, livro.getIsbn10()) || !Objects.equals(isbn13Anterior, livro.getIsbn13());
        if (isbnMudou) {
            livroComOMesmoIsbn(livro)
                    .filter(outro -> !outro.getId().equals(livro.getId()))
                    .ifPresent(outro -> {
                        throw new ConflitoException("Esse ISBN já pertence a \"" + outro.getTitulo() + "\", que está no acervo");
                    });
        }
        if (request.capaUrl() != null) {
            String link = request.capaUrl().trim();
            livro.setCapaUrl(link.isEmpty() ? buscarCapa(livro).orElse(null) : validarLinkDaCapa(link));
        } else if (isbnMudou) {
            livro.setCapaUrl(buscarCapa(livro).orElse(null));
        }
        Livro salvo = repository.save(livro);
        logService.registrarLogUsuarioLogado("ATUALIZACAO_LIVRO", "Livro " + salvo.getId() + " (" + salvo.getTitulo() + ") atualizado");
        return salvo;
    }

    private Optional<Livro> livroComOMesmoIsbn(Livro livro) {
        Optional<Livro> pelo13 = livro.getIsbn13() == null
                ? Optional.empty()
                : repository.findFirstByIsbn13AndAtivoTrue(livro.getIsbn13());
        if (pelo13.isPresent() || livro.getIsbn10() == null) {
            return pelo13;
        }
        return repository.findFirstByIsbn10AndAtivoTrue(livro.getIsbn10());
    }

    private Optional<Livro> livroJaCadastrado(Livro livro) {
        Optional<Livro> peloIsbn = livroComOMesmoIsbn(livro);
        if (peloIsbn.isPresent() || livro.getIsbn13() != null || livro.getIsbn10() != null) {
            return peloIsbn;
        }
        Set<String> titulo = palavras(livro.getTitulo());
        Set<String> autores = palavras(livro.getAutores() == null ? "" : String.join(" ", livro.getAutores()));
        return repository.findByAtivoTrue().stream()
                .filter(outro -> palavras(outro.getTitulo()).equals(titulo))
                .filter(outro -> palavras(outro.getAutores() == null ? "" : String.join(" ", outro.getAutores())).equals(autores))
                .findFirst();
    }

    private Optional<String> buscarCapa(Livro livro) {
        return capaPeloIsbn(livro).or(() -> capaPeloTitulo(livro));
    }

    private Optional<String> capaPeloTitulo(Livro livro) {
        if (livro.getTitulo() == null || livro.getTitulo().isBlank()) {
            return Optional.empty();
        }
        try {
            String json = consumo.obterDados(BUSCA_OPEN_LIBRARY + URLEncoder.encode(livro.getTitulo().trim(), StandardCharsets.UTF_8));
            BuscaOpenLibrary busca = conversor.obterDados(json, BuscaOpenLibrary.class);
            if (busca.docs() == null) {
                return Optional.empty();
            }
            Set<String> palavrasDoTitulo = palavras(livro.getTitulo());
            List<String> sobrenomes = sobrenomesDosAutores(livro.getAutores());
            return busca.docs().stream()
                    .filter(resultado -> resultado.capaId() != null && resultado.capaId() > 0)
                    .filter(resultado -> mesmoTitulo(palavrasDoTitulo, resultado.titulo()))
                    .filter(resultado -> sobrenomes.isEmpty() || mesmoAutor(sobrenomes, resultado.autores()))
                    .map(resultado -> CAPAS_POR_ID + resultado.capaId() + "-L.jpg")
                    .findFirst();
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private Set<String> palavras(String texto) {
        String semAcento = Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return Arrays.stream(semAcento.split("[^a-z0-9]+"))
                .filter(palavra -> !palavra.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean mesmoTitulo(Set<String> palavrasDoTitulo, String outroTitulo) {
        Set<String> outras = palavras(outroTitulo);
        return !palavrasDoTitulo.isEmpty() && palavrasDoTitulo.equals(outras);
    }

    private List<String> sobrenomesDosAutores(List<String> autores) {
        if (autores == null) {
            return List.of();
        }
        return autores.stream()
                .filter(autor -> autor != null && !autor.equalsIgnoreCase("Autor desconhecido"))
                .map(autor -> new ArrayList<>(palavras(autor)))
                .filter(partes -> !partes.isEmpty())
                .map(partes -> partes.get(partes.size() - 1))
                .map(sobrenome -> sobrenome.substring(0, Math.min(5, sobrenome.length())))
                .toList();
    }

    private boolean mesmoAutor(List<String> sobrenomes, List<String> autores) {
        if (autores == null) {
            return false;
        }
        return autores.stream()
                .flatMap(autor -> palavras(autor).stream())
                .anyMatch(palavra -> sobrenomes.stream().anyMatch(palavra::startsWith));
    }

    private String validarLinkDaCapa(String link) {
        if (link.length() > TAMANHO_MAXIMO_LINK_CAPA) {
            throw new RegraDeNegocioException("O link da capa pode ter no máximo " + TAMANHO_MAXIMO_LINK_CAPA + " caracteres");
        }
        URI endereco;
        try {
            endereco = URI.create(link);
        } catch (IllegalArgumentException e) {
            throw new RegraDeNegocioException("O link da capa não é um endereço válido");
        }
        if (!"https".equalsIgnoreCase(endereco.getScheme()) || endereco.getHost() == null) {
            throw new RegraDeNegocioException("O link da capa precisa começar com https://");
        }
        if (!consumo.imagemExiste(link)) {
            throw new RegraDeNegocioException("O link não abre uma imagem. Clique com o botão direito na capa, "
                    + "escolha \"Copiar endereço da imagem\" e cole aqui");
        }
        return link;
    }

    private Optional<String> capaPeloIsbn(Livro livro) {
        return Stream.of(livro.getIsbn13(), livro.getIsbn10())
                .filter(isbn -> isbn != null && !isbn.isBlank())
                .filter(isbn -> consumo.imagemExiste(CAPAS_OPEN_LIBRARY + isbn + "-L.jpg?default=false"))
                .map(isbn -> CAPAS_OPEN_LIBRARY + isbn + "-L.jpg")
                .findFirst();
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