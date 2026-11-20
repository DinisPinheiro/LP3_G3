package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.Model.CartaoMagnetico;
import com.hotel4you.hotel4you.Model.Cliente;
import com.hotel4you.hotel4you.Model.Quarto;
import com.hotel4you.hotel4you.Model.Reserva;
import com.hotel4you.hotel4you.Model.enums.EstadoReserva;
import com.hotel4you.hotel4you.Model.enums.TipoQuarto;
import com.hotel4you.hotel4you.Model.enums.Vista;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes da disponibilidade de quartos no DAO de reservas: sobreposição de períodos, estados que
 * ocupam o quarto, listagens por cliente e por estado (com paginação).
 */
class ReservaDAOMemoriaDisponibilidadeTest {

    private static final LocalDate BASE = LocalDate.of(2027, 3, 10);

    private ReservaDAOMemoria dao;
    private Cliente maria;
    private Cliente rui;
    private Quarto quarto202;
    private Quarto quarto203;

    @BeforeEach
    void preparar() {
        dao = new ReservaDAOMemoria();
        maria = new Cliente(1, "Maria Silva", "PT123456", "910000000");
        rui = new Cliente(2, "Rui Costa", "PT654321", "920000000");
        quarto202 = new Quarto(1, 202, 2, "", TipoQuarto.DUPLO, new BigDecimal("80.00"), Vista.MAR, true);
        quarto203 = new Quarto(2, 203, 2, "", TipoQuarto.INDIVIDUAL, new BigDecimal("50.00"), Vista.NENHUMA, true);
    }

    private static LocalDate dia(int deslocamento) {
        return BASE.plusDays(deslocamento);
    }

    private Reserva guardar(Cliente cliente, Quarto quarto, int entrada, int saida) {
        return dao.inserir(new Reserva(0, cliente, quarto, dia(entrada), dia(saida)));
    }

    // ---- Sobreposição de períodos -----------------------------------------------------------

    @Test
    void quartoSemReservasEstaLivre() {
        assertFalse(dao.existeSobreposicao(1, dia(0), dia(3)));
    }

    @Test
    void mesmoPeriodoEstaOcupado() {
        guardar(maria, quarto202, 0, 3);

        assertTrue(dao.existeSobreposicao(1, dia(0), dia(3)));
    }

    @Test
    void periodosQueSeCruzamEstaoOcupados() {
        guardar(maria, quarto202, 2, 6);

        assertTrue(dao.existeSobreposicao(1, dia(0), dia(3)), "termina dentro da reserva");
        assertTrue(dao.existeSobreposicao(1, dia(5), dia(9)), "começa dentro da reserva");
        assertTrue(dao.existeSobreposicao(1, dia(3), dia(5)), "totalmente dentro da reserva");
        assertTrue(dao.existeSobreposicao(1, dia(0), dia(9)), "engloba a reserva");
        assertTrue(dao.existeSobreposicao(1, dia(5), dia(6)), "só a última noite");
    }

    @Test
    void entrarNoDiaDeSaidaDeOutraReservaEPermitido() {
        guardar(maria, quarto202, 2, 6);

        assertFalse(dao.existeSobreposicao(1, dia(6), dia(8)), "começa no dia de saída");
        assertFalse(dao.existeSobreposicao(1, dia(0), dia(2)), "termina no dia de entrada");
    }

    @Test
    void periodosDistantesEstaoLivres() {
        guardar(maria, quarto202, 2, 6);

        assertFalse(dao.existeSobreposicao(1, dia(10), dia(12)));
        assertFalse(dao.existeSobreposicao(1, dia(-5), dia(-1)));
    }

    @Test
    void outroQuartoNoMesmoPeriodoEstaLivre() {
        guardar(maria, quarto202, 0, 3);

        assertFalse(dao.existeSobreposicao(2, dia(0), dia(3)));
        assertFalse(dao.existeSobreposicao(99, dia(0), dia(3)), "quarto sem reservas");
    }

    // ---- Estados que ocupam o quarto --------------------------------------------------------

    @Test
    void reservaPendenteOcupaOQuarto() {
        Reserva reserva = guardar(maria, quarto202, 0, 3);

        assertEquals(EstadoReserva.PENDENTE, reserva.getEstado());
        assertTrue(dao.existeSobreposicao(1, dia(1), dia(2)));
    }

    @Test
    void reservaComCheckInOcupaOQuarto() {
        Reserva reserva = guardar(maria, quarto202, 0, 3);
        reserva.fazerCheckIn(new CartaoMagnetico(1001, false));
        dao.atualizar(reserva);

        assertTrue(dao.existeSobreposicao(1, dia(1), dia(2)));
    }

    @Test
    void reservaCanceladaLibertaOQuarto() {
        Reserva reserva = guardar(maria, quarto202, 0, 3);
        reserva.cancelar();
        dao.atualizar(reserva);

        assertFalse(dao.existeSobreposicao(1, dia(0), dia(3)));
    }

    @Test
    void reservaComCheckOutLibertaOQuarto() {
        Reserva reserva = guardar(maria, quarto202, 0, 3);
        reserva.fazerCheckIn(new CartaoMagnetico(1001, false));
        reserva.fazerCheckOut();
        dao.atualizar(reserva);

        assertFalse(dao.existeSobreposicao(1, dia(0), dia(3)));
    }

    // ---- Inserir respeita a disponibilidade -------------------------------------------------

    @Test
    void naoSeInsereReservaNumQuartoOcupadoENadaFicaGuardado() {
        guardar(maria, quarto202, 0, 3);

        DAOException erro = assertThrows(DAOException.class, () -> guardar(rui, quarto202, 2, 5));

        assertTrue(erro.getMessage().contains("já está reservado"));
        assertEquals(1, dao.listarTodos().size());
    }

    @Test
    void duasReservasSeguidasNoMesmoQuartoSaoAceites() {
        guardar(maria, quarto202, 0, 3);

        Reserva seguinte = guardar(rui, quarto202, 3, 5);

        assertEquals(2, seguinte.getId());
        assertEquals(2, dao.listarTodos().size());
    }

    @Test
    void depoisDeCancelarOQuartoPodeSerReservadoDeNovo() {
        Reserva primeira = guardar(maria, quarto202, 0, 3);
        primeira.cancelar();
        dao.atualizar(primeira);

        Reserva nova = guardar(rui, quarto202, 0, 3);

        assertEquals(EstadoReserva.PENDENTE, nova.getEstado());
        assertEquals(2, dao.listarTodos().size(), "a cancelada continua no histórico");
    }

    @Test
    void depoisDeApagarOQuartoFicaLivre() {
        Reserva reserva = guardar(maria, quarto202, 0, 3);
        dao.remover(reserva.getId());

        assertFalse(dao.existeSobreposicao(1, dia(0), dia(3)));
    }

    // ---- Listagens --------------------------------------------------------------------------

    @Test
    void listarPorClienteDevolveSoAsReservasDesseCliente() {
        Reserva daMaria = guardar(maria, quarto202, 0, 3);
        guardar(rui, quarto203, 0, 3);

        assertEquals(List.of(daMaria), dao.listarPorCliente(1));
        assertEquals(1, dao.listarPorCliente(2).size());
        assertTrue(dao.listarPorCliente(99).isEmpty());
    }

    @Test
    void listarPorEstadosFiltraEOrdenaDaEntradaMaisRecenteParaAMaisAntiga() {
        Reserva antiga = guardar(maria, quarto202, 0, 2);
        Reserva recente = guardar(maria, quarto202, 10, 12);
        Reserva meio = guardar(rui, quarto203, 5, 7);
        Reserva cancelada = guardar(rui, quarto203, 20, 22);
        cancelada.cancelar();
        dao.atualizar(cancelada);

        assertEquals(List.of(recente, meio, antiga), dao.listarPorEstados(List.of(EstadoReserva.PENDENTE), 1, 10));
        assertEquals(List.of(cancelada), dao.listarPorEstados(List.of(EstadoReserva.CANCELADA), 1, 10));
        assertEquals(4, dao.listarPorEstados(List.of(EstadoReserva.PENDENTE, EstadoReserva.CANCELADA), 1, 10).size());
        assertTrue(dao.listarPorEstados(List.of(EstadoReserva.CHECK_OUT), 1, 10).isEmpty());
    }

    @Test
    void listarPorEstadosPagina() {
        for (int i = 0; i < 5; i++) {
            guardar(maria, quarto202, i * 3, i * 3 + 2);
        }
        List<EstadoReserva> pendentes = List.of(EstadoReserva.PENDENTE);

        assertEquals(2, dao.listarPorEstados(pendentes, 1, 2).size());
        assertEquals(2, dao.listarPorEstados(pendentes, 2, 2).size());
        assertEquals(1, dao.listarPorEstados(pendentes, 3, 2).size());
        assertTrue(dao.listarPorEstados(pendentes, 4, 2).isEmpty(), "página depois da última");
    }

    @Test
    void contarPorEstadosContaSoOsEstadosPedidos() {
        guardar(maria, quarto202, 0, 2);
        guardar(maria, quarto202, 5, 7);
        Reserva cancelada = guardar(rui, quarto203, 0, 2);
        cancelada.cancelar();
        dao.atualizar(cancelada);

        assertEquals(2, dao.contarPorEstados(List.of(EstadoReserva.PENDENTE)));
        assertEquals(1, dao.contarPorEstados(List.of(EstadoReserva.CANCELADA)));
        assertEquals(3, dao.contarPorEstados(List.of(EstadoReserva.PENDENTE, EstadoReserva.CANCELADA)));
        assertEquals(0, dao.contarPorEstados(List.of(EstadoReserva.CHECK_IN)));
    }

    @Test
    void listarPorEstadosRejeitaEstadosEPaginacaoInvalidos() {
        List<EstadoReserva> pendentes = List.of(EstadoReserva.PENDENTE);

        assertThrows(IllegalArgumentException.class, () -> dao.listarPorEstados(null, 1, 10));
        assertThrows(IllegalArgumentException.class, () -> dao.listarPorEstados(List.of(), 1, 10));
        assertThrows(IllegalArgumentException.class, () -> dao.listarPorEstados(pendentes, 0, 10));
        assertThrows(IllegalArgumentException.class, () -> dao.listarPorEstados(pendentes, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> dao.listarPorEstados(pendentes, 1, 101));
        assertThrows(IllegalArgumentException.class, () -> dao.contarPorEstados(null));
        assertThrows(IllegalArgumentException.class, () -> dao.contarPorEstados(List.of()));
    }
}