package br.com.pfc.biblioteca.service;

import br.com.pfc.biblioteca.dto.dashboard.DashboardDTO;
import br.com.pfc.biblioteca.dto.dashboard.EstoqueResumoDTO;
import br.com.pfc.biblioteca.dto.livro.MateriaDTO;
import br.com.pfc.biblioteca.dto.reserva.ReservasResumoDTO;
import br.com.pfc.biblioteca.enums.Materia;
import br.com.pfc.biblioteca.enums.StatusPedido;
import br.com.pfc.biblioteca.enums.StatusReserva;
import br.com.pfc.biblioteca.repository.LivroRepository;
import br.com.pfc.biblioteca.repository.PedidoRepository;
import br.com.pfc.biblioteca.repository.ReservaRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {
    private final LivroRepository livroRepository;
    private final ReservaRepository reservaRepository;
    private final PedidoRepository pedidoRepository;

    public DashboardService(LivroRepository livroRepository, ReservaRepository reservaRepository, PedidoRepository pedidoRepository) {
        this.livroRepository = livroRepository;
        this.reservaRepository = reservaRepository;
        this.pedidoRepository = pedidoRepository;
    }

    @Transactional(readOnly = true)
    public DashboardDTO montar(){
        EstoqueResumoDTO estoque = new EstoqueResumoDTO(livroRepository.countByAtivoTrue(),
                livroRepository.somarEstoqueReserva(),
                livroRepository.somarEstoqueCompra(),
                livroRepository.contarEsgotadosParaReserva());

        ReservasResumoDTO reservas = new ReservasResumoDTO(
                reservaRepository.countByStatus(StatusReserva.RESERVADO),
                reservaRepository.countByStatus(StatusReserva.ENTREGUE),
                reservaRepository.countByStatus(StatusReserva.DEVOLVIDO),
                reservaRepository.countByStatus(StatusReserva.CANCELADO));

        PageRequest top5 = PageRequest.of(0, 5);
        return new DashboardDTO(estoque, reservas,
                paraDTO(reservaRepository.materiasMaisReservadas(StatusReserva.DEVOLVIDO, top5)),
                paraDTO(pedidoRepository.materiasMaisCompradas(StatusPedido.APROVADO, top5)));
    }

    private List<MateriaDTO> paraDTO(List<Object[]> linhas) {
        return linhas.stream().map(l -> {
            Materia m = (Materia) l[0];
            return new MateriaDTO(m.name(), m.getMateriaLivro(), ((Number) l[1]).longValue());
        }).collect(Collectors.toList());
    }
}
