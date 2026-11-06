package br.com.pfc.biblioteca.service;

import br.com.pfc.biblioteca.dto.carrinho.AdicionarCarrinhoRequest;
import br.com.pfc.biblioteca.dto.carrinho.CarrinhoDTO;
import br.com.pfc.biblioteca.dto.carrinho.ItemCarrinhoDTO;
import br.com.pfc.biblioteca.entity.jpa.ItemCarrinho;
import br.com.pfc.biblioteca.entity.jpa.Livro;
import br.com.pfc.biblioteca.entity.jpa.Usuario;
import br.com.pfc.biblioteca.infra.exception.ConflitoException;
import br.com.pfc.biblioteca.infra.exception.RecursoNaoEncontradoException;
import br.com.pfc.biblioteca.infra.exception.RegraDeNegocioException;
import br.com.pfc.biblioteca.repository.ItemCarrinhoRepository;
import br.com.pfc.biblioteca.repository.LivroRepository;
import br.com.pfc.biblioteca.repository.UsuarioRepository;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CarrinhoService {

    private final ItemCarrinhoRepository repository;
    private final LivroRepository livroRepository;
    private final UsuarioRepository usuarioRepository;

    public CarrinhoService(ItemCarrinhoRepository repository, LivroRepository livroRepository, UsuarioRepository usuarioRepository) {
        this.repository = repository;
        this.livroRepository = livroRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public CarrinhoDTO adicionar(AdicionarCarrinhoRequest request){
        Usuario usuario = usuarioLogado();

        Livro livro = livroRepository.findById(request.livroId())
                .filter(Livro::isAtivo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Livro não encontrado"));

        if (livro.getPreco() <= 0){
            throw new RegraDeNegocioException("Este livro não está disponível para compra");
        }

        int estoque = livro.getEstoqueCompra() == null ? 0 : livro.getEstoqueCompra();
        if(request.quantidade() > estoque){
            throw new ConflitoException("O estoque de " + livro.getTitulo() + " está em falta");
        }

        ItemCarrinho item = repository.findByUsuarioIdAndLivroId(usuario.getId(), livro.getId())
                .orElse(new ItemCarrinho(usuario, livro));

        item.setQuantidade(request.quantidade());
        item.setAtivo(true);
        repository.save(item);

        return montar(usuario.getId());
    }

    @Transactional
    public CarrinhoDTO remover(Long livroId){
        Usuario usuario = usuarioLogado();
        ItemCarrinho item = repository.findByUsuarioIdAndLivroId(usuario.getId(), livroId)
                .filter(ItemCarrinho::isAtivo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item não encontrado no carrinho"));
        item.setAtivo(false);
        repository.save(item);
        return montar(usuario.getId());
    }

    @Transactional(readOnly = true)
    public CarrinhoDTO obter(){
        return montar(usuarioLogado().getId());
    }

    private CarrinhoDTO montar(Long usuarioId){
        List<ItemCarrinhoDTO> itens = repository.findByUsuarioIdAndAtivoTrue(usuarioId).stream()
                .map(i -> {
                    BigDecimal preco = BigDecimal.valueOf(i.getLivro().getPreco()).setScale(2, RoundingMode.HALF_UP);
                    return new ItemCarrinhoDTO(i.getLivro().getId(), i.getLivro().getTitulo(),
                            i.getLivro().getCapaUrl(), preco, i.getQuantidade(),
                            preco.multiply(BigDecimal.valueOf(i.getQuantidade())));
                })
                .collect(Collectors.toList());

        BigDecimal total = itens.stream().map(ItemCarrinhoDTO::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CarrinhoDTO(itens, total);
    }

    private Usuario usuarioLogado(){
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmailAndAtivoTrue(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
    }
}
