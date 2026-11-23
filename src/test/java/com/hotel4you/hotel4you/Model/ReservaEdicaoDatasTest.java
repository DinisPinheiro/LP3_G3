package com.hotel4you.hotel4you.Model;

import com.hotel4you.hotel4you.Model.enums.TipoCobranca;
import com.hotel4you.hotel4you.Model.enums.TipoQuarto;
import com.hotel4you.hotel4you.Model.enums.TipoServico;
import com.hotel4you.hotel4you.Model.enums.Vista;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes da edição das datas de uma reserva (S6-08): datas livres e ocupadas, recálculo do total e
 * estados em que a edição é permitida.
 */
class ReservaEdicaoDatasTest {

    private static final LocalDate ENTRADA = LocalDate.of(2027, 3, 10);
    private static final LocalDate SAIDA = LocalDate.of(2027, 3, 13);   // 3 noites

    private Quarto quarto;
    private Reserva reserva;
    /** Períodos ocupados por outras reservas: {entrada, saída}. */
    private final List<LocalDate[]> ocupados = new ArrayList<>();

    /** Disponibilidade de teste: ocupado se o período se cruzar com um dos {@link #ocupados}. */
    private final Reserva.Disponibilidade disponibilidade = (idQuarto, entrada, saida, ignorada) -> {
        for (LocalDate[] periodo : ocupados) {
            if (periodo[0].isBefore(saida) && periodo[1].isAfter(entrada)) {
                return true;
            }
        }
        return false;
    };

    @BeforeEach
    void preparar() {
        Cliente cliente = new Cliente(1, "Maria Silva", "PT123456", "910000000");
        // 80 (base) + 20 (vista mar) = 100 por noite
        quarto = new Quarto(1, 202, 2, "", TipoQuarto.DUPLO, new BigDecimal("80.00"), Vista.MAR, true);
        quarto.setSuplementoVista(new BigDecimal("20.00"));
        reserva = new Reserva(1, cliente, quarto, ENTRADA, SAIDA);
    }

    private static void assertValor(String esperado, BigDecimal obtido) {
        assertEquals(0, new BigDecimal(esperado).compareTo(obtido), "esperado " + esperado + ", obtido " + obtido);
    }

    @Test
    void alteraDatasQuandoOQuartoEstaLivre() {
        reserva.alterarDatas(ENTRADA.plusDays(7), SAIDA.plusDays(9), disponibilidade);

        assertEquals(ENTRADA.plusDays(7), reserva.getDataEntrada());
        assertEquals(SAIDA.plusDays(9), reserva.getDataSaida());
    }

    @Test
    void recusaDatasSeOQuartoEstiverOcupado() {
        ocupados.add(new LocalDate[]{ENTRADA.plusDays(20), ENTRADA.plusDays(25)});

        assertThrows(IllegalArgumentException.class,
                () -> reserva.alterarDatas(ENTRADA.plusDays(18), ENTRADA.plusDays(22), disponibilidade));

        assertEquals(ENTRADA, reserva.getDataEntrada(), "as datas não mudam");
        assertEquals(SAIDA, reserva.getDataSaida());
        assertValor("300.00", reserva.calcularTotal());
    }

    @Test
    void periodoColadoAOutraReservaEPermitido() {
        ocupados.add(new LocalDate[]{ENTRADA.plusDays(10), ENTRADA.plusDays(14)});

        reserva.alterarDatas(ENTRADA.plusDays(14), ENTRADA.plusDays(16), disponibilidade);
        reserva.alterarDatas(ENTRADA.plusDays(6), ENTRADA.plusDays(10), disponibilidade);

        assertEquals(ENTRADA.plusDays(10), reserva.getDataSaida());
    }

    @Test
    void ovalorTotalERecalculadoComOsNovosDias() {
        assertValor("300.00", reserva.calcularTotal());

        reserva.alterarDatas(ENTRADA, ENTRADA.plusDays(5), disponibilidade);
        assertEquals(5, reserva.getNoites());
        assertValor("500.00", reserva.calcularTotal());

        reserva.alterarDatas(ENTRADA, ENTRADA.plusDays(1), disponibilidade);
        assertValor("100.00", reserva.calcularTotal());
    }

    @Test
    void totalRecalculadoIncluiServicosCobradosPorNoite() {
        Servico pequenoAlmoco = new Servico(1, TipoServico.SERVICO, "Pequeno-almoço", "", "",
                new BigDecimal("8.00"), TipoCobranca.POR_NOITE, true);
        Reserva comServico = new Reserva(2, reserva.getCliente(), quarto, ENTRADA, SAIDA,
                List.of(new ConsumoServico(pequenoAlmoco, 1, ENTRADA)), List.of());
        assertValor("324.00", comServico.calcularTotal());   // 3 x 100 + 3 x 8

        comServico.alterarDatas(ENTRADA, ENTRADA.plusDays(4), disponibilidade);

        assertValor("432.00", comServico.calcularTotal());   // 4 x 100 + 4 x 8
    }

    @Test
    void mantemOsPrecosGuardadosNaCriacao() {
        quarto.setPrecoBase(new BigDecimal("200.00"));   // o gestor altera o preço depois da reserva

        reserva.alterarDatas(ENTRADA, ENTRADA.plusDays(2), disponibilidade);

        assertValor("200.00", reserva.calcularTotal());   // 2 x (80 + 20), não 2 x 220
    }

    @Test
    void validaAsDatas() {
        assertThrows(IllegalArgumentException.class,
                () -> reserva.alterarDatas(ENTRADA, ENTRADA, disponibilidade), "saída igual à entrada");
        assertThrows(IllegalArgumentException.class,
                () -> reserva.alterarDatas(ENTRADA, ENTRADA.minusDays(1), disponibilidade), "saída antes da entrada");
        assertThrows(NullPointerException.class, () -> reserva.alterarDatas(null, SAIDA, disponibilidade));
        assertThrows(NullPointerException.class, () -> reserva.alterarDatas(ENTRADA, null, disponibilidade));
        assertEquals(ENTRADA, reserva.getDataEntrada());
        assertEquals(SAIDA, reserva.getDataSaida());
    }

    @Test
    void aDisponibilidadeRecebeOQuartoEAReservaAIgnorar() {
        int[] recebido = new int[2];
        reserva.alterarDatas(ENTRADA, SAIDA.plusDays(1), (idQuarto, entrada, saida, ignorada) -> {
            recebido[0] = idQuarto;
            recebido[1] = ignorada;
            return false;
        });

        assertEquals(quarto.getId(), recebido[0]);
        assertEquals(reserva.getId(), recebido[1]);
    }

    @Test
    void reservaPendenteEAtivaPodeSerEditada() {
        assertTrue(reserva.podeEditarDatas());
    }

    @Test
    void depoisDoCheckInSoSePodeAlterarASaida() {
        reserva.fazerCheckIn(new CartaoMagnetico(12345, false));
        assertTrue(reserva.podeEditarDatas());

        reserva.alterarDatas(ENTRADA, SAIDA.plusDays(2), disponibilidade);
        assertEquals(SAIDA.plusDays(2), reserva.getDataSaida());
        assertValor("500.00", reserva.calcularTotal());

        assertThrows(IllegalArgumentException.class,
                () -> reserva.alterarDatas(ENTRADA.plusDays(1), SAIDA, disponibilidade));
        assertEquals(ENTRADA, reserva.getDataEntrada());
    }

    @Test
    void reservaCanceladaOuComCheckOutNaoPodeSerEditada() {
        Reserva cancelada = new Reserva(2, reserva.getCliente(), quarto, ENTRADA, SAIDA);
        cancelada.cancelar();
        assertFalse(cancelada.podeEditarDatas());
        assertThrows(IllegalStateException.class,
                () -> cancelada.alterarDatas(ENTRADA, SAIDA.plusDays(1), disponibilidade));

        reserva.fazerCheckIn(new CartaoMagnetico(12345, false));
        reserva.fazerCheckOut();
        assertFalse(reserva.podeEditarDatas());
        assertThrows(IllegalStateException.class,
                () -> reserva.alterarDatas(ENTRADA, SAIDA.plusDays(1), disponibilidade));
        assertEquals(SAIDA, reserva.getDataSaida());
    }

    @Test
    void sobreposicaoDePeriodos() {
        assertTrue(reserva.sobrepoe(ENTRADA.minusDays(1), ENTRADA.plusDays(1)));
        assertTrue(reserva.sobrepoe(ENTRADA, SAIDA));
        assertFalse(reserva.sobrepoe(SAIDA, SAIDA.plusDays(2)), "entrar no dia de saída é permitido");
        assertFalse(reserva.sobrepoe(ENTRADA.minusDays(2), ENTRADA), "sair no dia de entrada é permitido");
    }
}
