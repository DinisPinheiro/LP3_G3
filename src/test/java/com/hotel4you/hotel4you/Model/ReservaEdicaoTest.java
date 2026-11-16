package com.hotel4you.hotel4you.Model;

import com.hotel4you.hotel4you.Model.enums.TipoCobranca;
import com.hotel4you.hotel4you.Model.enums.TipoQuarto;
import com.hotel4you.hotel4you.Model.enums.TipoServico;
import com.hotel4you.hotel4you.Model.enums.Vista;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes da edição de serviços por estado da reserva.
 */
class ReservaEdicaoTest {

    private Cliente cliente;
    private Quarto quarto;
    private Servico servico;
    private Reserva reserva;

    @BeforeEach
    void preparar() {

        cliente = new Cliente(1, "Maria Silva", "PT123456", "910000000");
        quarto = new Quarto(1, 202, 2, "", TipoQuarto.DUPLO, new BigDecimal("80.00"), Vista.MAR, true);
        servico = new Servico(1, TipoServico.EXTRA, "Lavandaria", "Lavandaria", "", new BigDecimal("10.00"), TipoCobranca.POR_UNIDADE, true);
        reserva = new Reserva(1, cliente, quarto, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 4));
    }

    private ConsumoServico criarConsumo() {

        return new ConsumoServico(
                servico, 2, LocalDate.of(2026, 12, 2));
    }

    /**
     * Reserva pendente não permite edição durante a estadia.
     */
    @Test
    void reservaPendenteNaoPermiteEditar() {

        assertFalse(reserva.podeEditarServicos());
        assertThrows(IllegalStateException.class, () -> reserva.adicionarServicoEdicao(criarConsumo()));
    }

    /**
     * Reserva com check-in permite acrescentar serviços.
     */
    @Test
    void reservaComCheckInPermiteAdicionarServico() {

        CartaoMagnetico cartao = new CartaoMagnetico(12345, false);
        reserva.fazerCheckIn(cartao, LocalDateTime.of(2026, 12, 1, 14, 0));
        assertTrue(reserva.podeEditarServicos());
        BigDecimal totalAnterior = reserva.calcularTotal();
        reserva.adicionarServicoEdicao(criarConsumo());
        assertEquals(1, reserva.getConsumos().size());
        assertEquals(0, totalAnterior.add(new BigDecimal("20.00")).compareTo(reserva.calcularTotal()));
    }

    /**
     * Remover um serviço atualiza o valor total.
     */
    @Test
    void removerServicoAtualizaTotal() {

        CartaoMagnetico cartao = new CartaoMagnetico(12345, false);
        reserva.fazerCheckIn(cartao);
        BigDecimal totalInicial = reserva.calcularTotal();
        reserva.adicionarServicoEdicao(criarConsumo());
        reserva.removerServicoEdicao(0);
        assertEquals(0, reserva.getConsumos().size());
        assertEquals(0, totalInicial.compareTo(reserva.calcularTotal()));
    }

    /**
     * Uma reserva cancelada não pode ser editada.
     */
    @Test
    void reservaCanceladaNaoPermiteEditar() {

        reserva.cancelar();
        assertFalse(reserva.podeEditarServicos());
        assertThrows(IllegalStateException.class, () -> reserva.adicionarServicoEdicao(criarConsumo()));
        assertThrows(IllegalStateException.class, () -> reserva.removerServicoEdicao(0));
    }

    /**
     * Uma reserva com check-out não pode ser editada.
     */
    @Test
    void reservaComCheckOutNaoPermiteEditar() {

        CartaoMagnetico cartao = new CartaoMagnetico(12345, false);
        reserva.fazerCheckIn(cartao);
        reserva.fazerCheckOut();
        assertFalse(reserva.podeEditarServicos());
        assertThrows(IllegalStateException.class, () -> reserva.adicionarServicoEdicao(criarConsumo()));
    }

    /**
     * Não é possível remover um serviço inexistente.
     */
    @Test
    void naoPermiteRemoverServicoInexistente() {

        CartaoMagnetico cartao = new CartaoMagnetico(12345, false);
        reserva.fazerCheckIn(cartao);
        assertThrows(IllegalArgumentException.class, () -> reserva.removerServicoEdicao(0));
    }
}