package com.hotel4you.hotel4you.Model;

import com.hotel4you.hotel4you.Model.enums.EstadoReserva;
import com.hotel4you.hotel4you.Model.enums.TipoQuarto;
import com.hotel4you.hotel4you.Model.enums.Vista;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes unitários das transições de estado da {@link Reserva}:
 * PENDENTE → CHECK_IN → CHECK_OUT, ou PENDENTE → CANCELADA. Tudo o resto é rejeitado.
 */
class ReservaTest {

    private Reserva reserva;
    private CartaoMagnetico cartao;
    private final LocalDateTime agora = LocalDateTime.of(2026, 12, 1, 15, 30);

    @BeforeEach
    void preparar() {
        Cliente cliente = new Cliente(1, "Maria Silva", "PT123456", "910000000");
        Quarto quarto = new Quarto(1, 202, 2, "", TipoQuarto.DUPLO, new BigDecimal("80.00"), Vista.MAR, true);
        reserva = new Reserva(1, cliente, quarto, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 4));
        cartao = new CartaoMagnetico(1001, false);
    }

    private void fazerCheckIn() {
        reserva.fazerCheckIn(cartao, agora);
    }

    // ---- Estado inicial ----

    @Test
    void reservaNovaFicaPendenteSemCartaoNemDatasDeCheckInOuOut() {
        assertEquals(EstadoReserva.PENDENTE, reserva.getEstado());
        assertNull(reserva.getCartao());
        assertNull(reserva.getDataCheckIn());
        assertNull(reserva.getDataCheckOut());
    }

    // ---- Check-in ----

    @Test
    void checkInMudaOEstadoRegistaOMomentoEGuardaOCartao() {
        fazerCheckIn();

        assertEquals(EstadoReserva.CHECK_IN, reserva.getEstado());
        assertEquals(agora, reserva.getDataCheckIn());
        assertSame(cartao, reserva.getCartao());
        assertTrue(cartao.isAtivo());
    }

    @Test
    void checkInSoAceitaReservasPendentes() {
        fazerCheckIn();
        assertThrows(IllegalStateException.class, () -> reserva.fazerCheckIn(new CartaoMagnetico(1002, false), agora));

        reserva.fazerCheckOut(agora.plusDays(3));
        assertThrows(IllegalStateException.class, () -> reserva.fazerCheckIn(new CartaoMagnetico(1003, false), agora));
    }

    @Test
    void checkInDeReservaCanceladaELancaExcecao() {
        reserva.cancelar();

        assertThrows(IllegalStateException.class, () -> reserva.fazerCheckIn(cartao, agora));
        assertEquals(EstadoReserva.CANCELADA, reserva.getEstado());
        assertNull(reserva.getCartao());
    }

    @Test
    void checkInRejeitaCartaoNulo() {
        assertThrows(NullPointerException.class, () -> reserva.fazerCheckIn(null, agora));

        assertEquals(EstadoReserva.PENDENTE, reserva.getEstado());
    }

    @Test
    void checkInRejeitaOCartaoMestre() {
        CartaoMagnetico mestre = new CartaoMagnetico(1, true);

        assertThrows(IllegalArgumentException.class, () -> reserva.fazerCheckIn(mestre, agora));

        assertEquals(EstadoReserva.PENDENTE, reserva.getEstado());
        assertNull(reserva.getCartao());
    }

    @Test
    void checkInRejeitaUmCartaoDesativado() {
        cartao.desativar();

        assertThrows(IllegalArgumentException.class, () -> reserva.fazerCheckIn(cartao, agora));

        assertEquals(EstadoReserva.PENDENTE, reserva.getEstado());
        assertNull(reserva.getCartao());
    }

    // ---- Check-out ----

    @Test
    void checkOutMudaOEstadoRegistaASaidaEDesativaOCartao() {
        fazerCheckIn();
        LocalDateTime saida = agora.plusDays(3);

        reserva.fazerCheckOut(saida);

        assertEquals(EstadoReserva.CHECK_OUT, reserva.getEstado());
        assertEquals(saida, reserva.getDataCheckOut());
        assertFalse(cartao.isAtivo());
        assertFalse(cartao.temAcessoAoPiso(2));
    }

    @Test
    void checkOutSemCheckInLancaExcecao() {
        assertThrows(IllegalStateException.class, () -> reserva.fazerCheckOut(agora));

        assertEquals(EstadoReserva.PENDENTE, reserva.getEstado());
        assertNull(reserva.getDataCheckOut());
    }

    @Test
    void checkOutDuasVezesLancaExcecao() {
        fazerCheckIn();
        reserva.fazerCheckOut(agora.plusDays(1));

        assertThrows(IllegalStateException.class, () -> reserva.fazerCheckOut(agora.plusDays(2)));
        assertEquals(agora.plusDays(1), reserva.getDataCheckOut());
    }

    @Test
    void checkOutDeReservaCanceladaLancaExcecao() {
        reserva.cancelar();

        assertThrows(IllegalStateException.class, () -> reserva.fazerCheckOut(agora));
        assertEquals(EstadoReserva.CANCELADA, reserva.getEstado());
    }

    // ---- Cancelamento ----

    @Test
    void cancelarMudaUmaReservaPendenteParaCancelada() {
        reserva.cancelar();

        assertEquals(EstadoReserva.CANCELADA, reserva.getEstado());
        assertEquals(0, BigDecimal.ZERO.compareTo(reserva.calcularTotal()));
    }

    @Test
    void cancelarSoEPossivelEmReservasPendentes() {
        reserva.cancelar();
        assertThrows(IllegalStateException.class, () -> reserva.cancelar());

        preparar();
        fazerCheckIn();
        assertThrows(IllegalStateException.class, () -> reserva.cancelar());

        reserva.fazerCheckOut(agora.plusDays(3));
        assertThrows(IllegalStateException.class, () -> reserva.cancelar());
    }

    // ---- Ciclo completo e regras associadas ----

    @Test
    void cicloCompletoPendenteCheckInCheckOut() {
        assertEquals(EstadoReserva.PENDENTE, reserva.getEstado());

        fazerCheckIn();
        assertEquals(EstadoReserva.CHECK_IN, reserva.getEstado());
        assertTrue(cartao.temAcessoAoPiso(2));

        reserva.fazerCheckOut(agora.plusDays(3));
        assertEquals(EstadoReserva.CHECK_OUT, reserva.getEstado());
        assertFalse(cartao.temAcessoAoPiso(2));
    }

    @Test
    void soSePodeApagarUmaReservaSemCheckIn() {
        assertTrue(reserva.podeSerApagada());

        reserva.cancelar();
        assertTrue(reserva.podeSerApagada());

        preparar();
        fazerCheckIn();
        assertFalse(reserva.podeSerApagada());

        reserva.fazerCheckOut(agora.plusDays(3));
        assertFalse(reserva.podeSerApagada());
    }

    @Test
    void naoSeRegistamConsumosAntesDoCheckInNemDepoisDoCheckOut() {
        ConsumoServico consumo = null; // o estado é verificado antes de olhar para o consumo

        assertThrows(IllegalStateException.class, () -> reserva.adicionarConsumo(consumo));

        fazerCheckIn();
        reserva.fazerCheckOut(agora.plusDays(3));
        assertThrows(IllegalStateException.class, () -> reserva.adicionarConsumo(consumo));
    }
}