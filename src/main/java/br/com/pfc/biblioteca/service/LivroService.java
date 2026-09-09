package br.com.pfc.biblioteca.service;

import br.com.pfc.biblioteca.dto.DadosLivro;
import br.com.pfc.biblioteca.model.Livro;
import br.com.pfc.biblioteca.repository.LivroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LivroService {

    @Autowired
    private LivroRepository repository;
    private ConsumoApi consumo = new ConsumoApi();
    private ConverteDados conversor = new ConverteDados();
    private final String ENDERECO = "https://www.googleapis.com/books/v1/volumes?q=";
    private final String API_KEY = System.getenv("BOOK_API");


    public Livro salvarLivro(String nomeLivro) {
        var json = consumo.obterDados(ENDERECO + nomeLivro.replace(" ", "+") + "&key=" + API_KEY);
        DadosLivro.DadosBusca dadosBusca = conversor.obterDados(json, DadosLivro.DadosBusca.class);

        if(dadosBusca.items() == null || dadosBusca.items().isEmpty()){
            throw new RuntimeException("Livro não encontrado na API");
        }
        DadosLivro dados = dadosBusca.items().get(0).volumeInfo();
        Livro livro = new Livro(dados);

        return repository.save(livro);
    }
}
