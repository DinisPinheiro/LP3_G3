package com.hotel4you.hotel4you.Model;

import com.hotel4you.hotel4you.Model.enums.EstadoReserva;
import com.hotel4you.hotel4you.Model.enums.EstadoImportacao;
import com.hotel4you.hotel4you.Model.enums.TipoQuarto;
import com.hotel4you.hotel4you.Model.enums.Vista;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes dos estados: nomes gravados na base de dados (têm de coincidir com os CHECK dos scripts SQL)
 * e todas as transições permitidas e proibidas de uma reserva.
 */
class EstadoReservaTest {

    private static Reserva novaReserva() {
        Cliente cliente = new Cliente(1, "Maria Silva", "PT123456", "910000000");
        Quarto quarto = new Quarto(1, 202, 2, "", TipoQuarto.DUPLO, new BigDecimal("80.00"), Vista.MAR, true);
        return new Reserva(1, cliente, quarto, LocalDate.now().plusDays(5), LocalDate.now().plusDays(8));
    }

    private static Reserva comCheckIn() {
        Reserva reserva = novaReserva();
        reserva.fazerCheckIn(new CartaoMagnetico(1001, false));
        return reserva;
    }

    private static Reserva comCheckOut() {
        Reserva reserva = comCheckIn();
        reserva.fazerCheckOut();
        return reserva;
    }

    private static Reserva cancelada() {
        Reserva reserva = novaReserva();
        reserva.cancelar();
        return reserva;
    }

    @Test
    void osQuatroEstadosDeReservaTemOsNomesGuardadosNaBaseDeDados() {
        assertEquals(4, EstadoReserva.values().length);
        assertEquals(EstadoReserva.PENDENTE, EstadoReserva.valueOf("PENDENTE"));
        assertEquals(EstadoReserva.CHECK_IN, EstadoReserva.valueOf("CHECK_IN"));
        assertEquals(EstadoReserva.CHECK_OUT, EstadoReserva.valueOf("CHECK_OUT"));
        assertEquals(EstadoReserva.CANCELADA, EstadoReserva.valueOf("CANCELADA"));
    }

    @Test
    void osEstadosDeImportacaoTemOsNomesGuardadosNaBaseDeDados() {
        assertEquals(3, EstadoImportacao.values().length);
        assertEquals(EstadoImportacao.PENDENTE, EstadoImportacao.valueOf("PENDENTE"));
        assertEquals(EstadoImportacao.APROVADO, EstadoImportacao.valueOf("APROVADO"));
        assertEquals(EstadoImportacao.REJEITADO, EstadoImportacao.valueOf("REJEITADO"));
    }

    @Test
    void osEstadosDeImportacaoMostramONomeLegivel() {
        assertEquals("Pendente", EstadoImportacao.PENDENTE.getNome());
        assertEquals("Aprovado", EstadoImportacao.APROVADO.toString());
        assertEquals("Rejeitado", EstadoImportacao.REJEITADO.toString());
    }

    @Test
    void estadoDesconhecidoNaoExiste() {
        assertThrows(IllegalArgumentException.class, () -> EstadoReserva.valueOf("ARQUIVADA"));
        assertThrows(IllegalArgumentException.class, () -> EstadoImportacao.valueOf("ANULADO"));
    }

    @Test
    void reservaNovaComecaPendente() {
        assertEquals(EstadoReserva.PENDENTE, novaReserva().getEstado());
    }

    @Test
    void deCadaEstadoSoSePodeIrParaOsEstadosSeguintesPermitidos() {
        // PENDENTE: check-in e cancelar sim, check-out não.
        assertEquals(EstadoReserva.CHECK_IN, comCheckIn().getEstado());
        assertEquals(EstadoReserva.CANCELADA, cancelada().getEstado());
        assertThrows(IllegalStateException.class, () -> novaReserva().fazerCheckOut());

        // CHECK_IN: só check-out.
        assertThrows(IllegalStateException.class, () -> comCheckIn().fazerCheckIn(new CartaoMagnetico(1002, false)));
        assertThrows(IllegalStateException.class, () -> comCheckIn().cancelar());
        assertEquals(EstadoReserva.CHECK_OUT, comCheckOut().getEstado());

        // CHECK_OUT: estado final.
        assertThrows(IllegalStateException.class, () -> comCheckOut().fazerCheckIn(new CartaoMagnetico(1003, false)));
        assertThrows(IllegalStateException.class, () -> comCheckOut().fazerCheckOut());
        assertThrows(IllegalStateException.class, () -> comCheckOut().cancelar());

        // CANCELADA: estado final.
        assertThrows(IllegalStateException.class, () -> cancelada().fazerCheckIn(new CartaoMagnetico(1004, false)));
        assertThrows(IllegalStateException.class, () -> cancelada().fazerCheckOut());
        assertThrows(IllegalStateException.class, () -> cancelada().cancelar());
    }

    @Test
    void umaTransicaoProibidaNaoAlteraOEstado() {
        Reserva reserva = comCheckIn();

        assertThrows(IllegalStateException.class, reserva::cancelar);

        assertEquals(EstadoReserva.CHECK_IN, reserva.getEstado());
        assertNotNull(reserva.getCartao());
    }

    @Test
    void soAsReservasSemCheckInPodemSerApagadasEmQualquerEstado() {
        assertTrue(novaReserva().podeSerApagada());
        assertTrue(cancelada().podeSerApagada());
        assertFalse(comCheckIn().podeSerApagada());
        assertFalse(comCheckOut().podeSerApagada());
    }
}