package br.com.pfc.biblioteca.integracao;

import br.com.pfc.biblioteca.entity.jpa.AvisoDisponibilidade;
import br.com.pfc.biblioteca.entity.jpa.Livro;
import br.com.pfc.biblioteca.entity.jpa.Usuario;
import br.com.pfc.biblioteca.enums.StatusAviso;
import br.com.pfc.biblioteca.enums.TipoEstoque;
import br.com.pfc.biblioteca.repository.AvisoDisponibilidadeRepository;
import br.com.pfc.biblioteca.schedule.AvisosDeDisponibilidadeJob;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class AvisoDisponibilidadeIntegracaoTest extends TesteIntegracaoBase {

    @Autowired
    private AvisoDisponibilidadeRepository avisoRepository;

    @Autowired
    private AvisosDeDisponibilidadeJob job;

    private Resposta pedirAviso(String token, Livro livro, String tipo) {
        return post("/avisos", token, Map.of("livroId", livro.getId(), "tipo", tipo));
    }

    private AvisoDisponibilidade aviso(Resposta resposta) {
        return avisoRepository.findById(UUID.fromString(resposta.corpo().path("id").asText())).orElseThrow();
    }

    private void reabastecer(Livro livro, int reserva, int compra) {
        Livro atual = recarregar(livro);
        atual.setEstoqueReserva(reserva);
        atual.setEstoqueCompra(compra);
        livroRepository.save(atual);
    }

    @Test
    void estudantePedeAvisoDeLivroEsgotadoUmaVezPorEstoque() {
        Usuario estudante = criarEstudante();
        String token = token(estudante);
        Livro livro = criarLivro(40, 0, 0);

        Resposta primeiro = pedirAviso(token, livro, "RESERVA");
        assertThat(primeiro.status()).isEqualTo(200);
        assertThat(primeiro.corpo().path("status").asText()).isEqualTo("PENDENTE");
        assertThat(pedirAviso(token, livro, "RESERVA").corpo().path("id").asText())
                .isEqualTo(primeiro.corpo().path("id").asText());
        assertThat(pedirAviso(token, livro, "COMPRA").status()).isEqualTo(200);

        JsonNode meus = get("/avisos/meus", token).corpo();
        assertThat(meus).hasSize(2);
        assertThat(meus.get(0).path("tituloLivro").asText()).isEqualTo(livro.getTitulo());

        assertThat(pedirAviso(token(criarFuncionario()), livro, "RESERVA").status()).isEqualTo(403);
        assertThat(pedirAviso(null, livro, "RESERVA").status()).isIn(401, 403);
    }

    @Test
    void naoPedeAvisoDeEstoqueQueJaExisteNemDeLivroQueNaoEstaAVenda() {
        String token = token(criarEstudante());

        Resposta comEstoque = pedirAviso(token, criarLivro(40, 3, 0), "RESERVA");
        assertThat(comEstoque.status()).isEqualTo(409);
        assertThat(comEstoque.mensagem()).contains("já tem exemplares para reserva");

        assertThat(pedirAviso(token, criarLivro(0, 0, 0), "COMPRA").status()).isEqualTo(400);
    }

    @Test
    void quandoOEstoqueVoltaOEstudanteRecebeUmUnicoEmail() {
        Usuario estudante = criarEstudante();
        Livro livro = criarLivro(40, 0, 0);
        AvisoDisponibilidade avisoReserva = aviso(pedirAviso(token(estudante), livro, "RESERVA"));
        AvisoDisponibilidade avisoCompra = aviso(pedirAviso(token(estudante), livro, "COMPRA"));

        job.enviarAvisos();
        verify(emailService, never()).enviarAvisoDisponibilidade(eq(estudante.getEmail()), any(), any(), any());

        reabastecer(livro, 2, 0);
        job.enviarAvisos();
        job.enviarAvisos();

        verify(emailService, times(1)).enviarAvisoDisponibilidade(eq(estudante.getEmail()), eq(estudante.getNome()),
                any(Livro.class), eq(TipoEstoque.RESERVA));
        AvisoDisponibilidade enviado = avisoRepository.findById(avisoReserva.getId()).orElseThrow();
        assertThat(enviado.getStatus()).isEqualTo(StatusAviso.ENVIADO);
        assertThat(enviado.getDataEnvio()).isNotNull();
        assertThat(avisoRepository.findById(avisoCompra.getId()).orElseThrow().getStatus()).isEqualTo(StatusAviso.PENDENTE);
        assertThat(get("/avisos/meus", token(estudante)).corpo()).hasSize(1);
    }

    @Test
    void avisoCanceladoNaoEEnviado() {
        Usuario estudante = criarEstudante();
        Livro livro = criarLivro(40, 0, 0);
        AvisoDisponibilidade aviso = aviso(pedirAviso(token(estudante), livro, "RESERVA"));

        assertThat(delete("/avisos/" + aviso.getId(), token(criarEstudante())).status()).isEqualTo(400);
        assertThat(delete("/avisos/" + aviso.getId(), token(estudante)).status()).isEqualTo(204);

        reabastecer(livro, 1, 0);
        job.enviarAvisos();

        verify(emailService, never()).enviarAvisoDisponibilidade(eq(estudante.getEmail()), any(), any(), any());
        assertThat(avisoRepository.findById(aviso.getId()).orElseThrow().getStatus()).isEqualTo(StatusAviso.CANCELADO);
    }

    @Test
    void falhaNoEnvioTentaDeNovoNaProximaVerificacao() {
        Usuario estudante = criarEstudante();
        Livro livro = criarLivro(40, 0, 0);
        AvisoDisponibilidade aviso = aviso(pedirAviso(token(estudante), livro, "RESERVA"));
        doThrow(new RuntimeException("Brevo fora do ar"))
                .when(emailService).enviarAvisoDisponibilidade(eq(estudante.getEmail()), any(), any(), any());

        reabastecer(livro, 1, 0);
        job.enviarAvisos();

        AvisoDisponibilidade depois = avisoRepository.findById(aviso.getId()).orElseThrow();
        assertThat(depois.getStatus()).isEqualTo(StatusAviso.PENDENTE);
        assertThat(depois.getTentativas()).isEqualTo(1);
    }

    @Test
    void excluirAContaCancelaOsAvisos() {
        Usuario estudante = criarEstudante();
        AvisoDisponibilidade aviso = aviso(pedirAviso(token(estudante), criarLivro(40, 0, 0), "RESERVA"));

        assertThat(delete("/usuarios/deletar/" + estudante.getId(), token(estudante)).status()).isEqualTo(204);

        assertThat(avisoRepository.findById(aviso.getId()).orElseThrow().getStatus()).isEqualTo(StatusAviso.CANCELADO);
    }
}
