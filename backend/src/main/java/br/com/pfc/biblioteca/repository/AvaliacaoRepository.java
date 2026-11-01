package br.com.pfc.biblioteca.repository;

import br.com.pfc.biblioteca.dto.RankingLivroDTO;
import br.com.pfc.biblioteca.entity.jpa.Avaliacao;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;


import java.util.List;
import java.util.Optional;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    Optional<Avaliacao> findByUsuarioIdAndLivroId(Long usuarioId, Long livroId);

    List<Avaliacao> findByLivroIdAndAtivoTrueOrderByDataAvaliacaoDesc(Long livroId);

    long countByLivroIdAndAtivoTrue(Long livroId);

    @Query("SELECT AVG(a.nota) FROM Avaliacao a WHERE a.livro.id = :livroId AND a.ativo = true")
    Double calcularMedia(long livroId);

    @Query("""
        SELECT new br.com.pfc.biblioteca.dto.RankingLivroDTO(
            l.id, l.titulo, l.capaUrl, AVG(a.nota), COUNT(a))
        FROM Avaliacao a JOIN a.livro l
        WHERE a.ativo = true AND l.ativo = true
        GROUP BY l.id, l.titulo, l.capaUrl
        HAVING COUNT(a) >= :minimo
        ORDER BY AVG(a.nota) DESC, COUNT(a) DESC
        """)
    List<RankingLivroDTO> buscarRanking(Long minimo, Pageable pageable);
}
