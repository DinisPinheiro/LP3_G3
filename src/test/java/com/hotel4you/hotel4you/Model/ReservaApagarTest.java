package com.hotel4you.hotel4you.Model;

import com.hotel4you.hotel4you.Model.enums.EstadoReserva;
import com.hotel4you.hotel4you.Model.enums.TipoQuarto;
import com.hotel4you.hotel4you.Model.enums.Vista;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes da regra do model "só se pode apagar uma reserva que nunca teve check-in"
 * ({@link Reserva#podeSerApagada()}), com reservas sem check-in e com check-in.
 * Depende do check-in (S4-01), que é quem regista o momento do check-in na reserva.
 */
class ReservaApagarTest {

    private Cliente cliente;
    private Quarto quarto;
    private Reserva reserva;
    private final LocalDate entrada = LocalDate.of(2026, 12, 1);
    private final LocalDate saida = LocalDate.of(2026, 12, 4);
    private final LocalDateTime agora = LocalDateTime.of(2026, 12, 1, 15, 30);

    @BeforeEach
    void preparar() {
        cliente = new Cliente(1, "Maria Silva", "PT123456", "910000000");
        quarto = new Quarto(1, 202, 2, "", TipoQuarto.DUPLO, new BigDecimal("80.00"), Vista.MAR, true);
        reserva = new Reserva(1, cliente, quarto, entrada, saida);
    }

    // ---- Reservas SEM check-in: podem ser apagadas ----

    @Test
    void reservaPendenteSemCheckInPodeSerApagada() {
        assertTrue(reserva.podeSerApagada());
    }

    @Test
    void reservaCanceladaSemCheckInPodeSerApagada() {
        reserva.cancelar();

        assertTrue(reserva.podeSerApagada());
    }

    @Test
    void reservaLidaDaBaseDeDadosSemMomentoDeCheckInPodeSerApagada() {
        Reserva lida = new Reserva(2, cliente, quarto, entrada, saida, EstadoReserva.PENDENTE, null,
                null, null, List.of());

        assertTrue(lida.podeSerApagada());
    }

    // ---- Reservas COM check-in: nunca podem ser apagadas ----

    @Test
    void reservaComCheckInNaoPodeSerApagada() {
        reserva.fazerCheckIn(new CartaoMagnetico(1001, false), agora);

        assertFalse(reserva.podeSerApagada());
    }

    @Test
    void reservaComCheckOutNaoPodeSerApagada() {
        reserva.fazerCheckIn(new CartaoMagnetico(1001, false), agora);
        reserva.fazerCheckOut(agora.plusDays(3));

        assertFalse(reserva.podeSerApagada());
    }

    @Test
    void reservaLidaDaBaseDeDadosComMomentoDeCheckInNaoPodeSerApagada() {
        Reserva lida = new Reserva(3, cliente, quarto, entrada, saida, EstadoReserva.CHECK_IN,
                new CartaoMagnetico(5, 1001, false, true), agora, null, List.of());

        assertFalse(lida.podeSerApagada());
    }

    @Test
    void aRegraDependeDoCheckInTerSidoFeitoENaoSoDoEstado() {
        // Reserva que já teve check-in e depois foi marcada com outro estado: continua protegida.
        Reserva lida = new Reserva(4, cliente, quarto, entrada, saida, EstadoReserva.CANCELADA, null,
                agora, null, List.of());

        assertFalse(lida.podeSerApagada());
    }
}
