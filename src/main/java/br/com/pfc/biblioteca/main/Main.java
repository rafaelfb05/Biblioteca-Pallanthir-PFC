package br.com.pfc.biblioteca.main;

import br.com.pfc.biblioteca.dto.DadosLivro;
import br.com.pfc.biblioteca.model.Livro;
import br.com.pfc.biblioteca.service.ConsumoApi;
import br.com.pfc.biblioteca.service.ConverteDados;

import java.util.Scanner;

public class Main {
      private Scanner scanner = new Scanner(System.in);
      private ConsumoApi consumo = new ConsumoApi();
      private ConverteDados conversor = new ConverteDados();
      private final String ENDERECO = "https://www.googleapis.com/books/v1/volumes?q=";
      private final String API_KEY = System.getenv("BOOK_API");

      public void exibeMenu() {
            int opcao = -1;
            String mensagem = """
                    --------BEM VINDO AO PALLANTHIR---------
                    
                    digite:
                    -1 para pesquisar um livro
                    """;



            while(opcao != 0){
                  System.out.println(mensagem);
                  opcao = scanner.nextInt();
                  scanner.nextLine();

                  switch (opcao) {
                        case 1:
                              pesquisarLivrosNaApi();
                  }
            }
      }

      private void pesquisarLivrosNaApi() {
            DadosLivro dados = getDadosLivro();
            Livro livro = new Livro(dados);
            System.out.println(dados);
      }

      private DadosLivro getDadosLivro() {
            System.out.println("Digite o nome do livro: ");
            var nomeLivro = scanner.nextLine();
            var json = consumo.obterDados(ENDERECO + nomeLivro.replace(" ", "+" + API_KEY));
            DadosLivro dados = conversor.obterDados(json, DadosLivro.class);
            return dados;
      }

}
