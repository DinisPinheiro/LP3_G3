package com.hotel4you.hotel4you.Model;

import com.hotel4you.hotel4you.Model.enums.EstadoReserva;
import com.hotel4you.hotel4you.Model.enums.TipoCobranca;
import com.hotel4you.hotel4you.Model.enums.TipoQuarto;
import com.hotel4you.hotel4you.Model.enums.TipoServico;
import com.hotel4you.hotel4you.Model.enums.Vista;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Testes unitários da S5-09: os preços (quarto, vista e extras) ficam guardados na reserva quando
 * ela é criada. Alterar um valor depois só se aplica às reservas seguintes.
 */
class ReservaPrecosGuardadosTest {

    private final LocalDate entrada = LocalDate.of(2026, 12, 1);
    private final LocalDate saida = LocalDate.of(2026, 12, 4);   // 3 noites

    private Cliente cliente;
    private Quarto quarto;
    private Servico limpeza;

    @BeforeEach
    void preparar() {
        cliente = new Cliente(1, "Maria Silva", "PT123456", "910000000");
        // 80 (preço base) + 20 (vista mar) = 100 por noite; 3 noites = 300
        quarto = new Quarto(1, 202, 2, "", TipoQuarto.DUPLO, new BigDecimal("80.00"), Vista.MAR, true);
        limpeza = new Servico(1, TipoServico.EXTRA, "Limpeza extra", "", "", new BigDecimal("10.00"),
                TipoCobranca.POR_UNIDADE, true);
    }

    private Reserva novaReserva(int id) {
        return new Reserva(id, cliente, quarto, entrada, saida);
    }

    private Reserva novaReservaComLimpezas(int id, int quantidade) {
        return new Reserva(id, cliente, quarto, entrada, saida,
                List.of(new ConsumoServico(limpeza, quantidade, entrada)), List.of());
    }

    private static void assertValor(String esperado, BigDecimal obtido) {
        assertEquals(0, new BigDecimal(esperado).compareTo(obtido), "esperado " + esperado + ", obtido " + obtido);
    }

    // ---- Vista ----

    @Test
    void aReservaGuardaOPrecoBaseEOValorDaVistaDaCriacao() {
        Reserva reserva = novaReserva(1);

        assertValor("80.00", reserva.getPrecoBaseNoite());
        assertValor("20.00", reserva.getValorVistaNoite());
        assertValor("100.00", reserva.getPrecoNoite());
    }

    @Test
    void alterarOValorDaVistaNaoAlteraUmaReservaJaCriada() {
        Reserva antes = novaReserva(1);

        quarto.setSuplementoVista(new BigDecimal("35.00"));   // o gestor sobe a vista mar de 20 para 35

        assertValor("20.00", antes.getValorVistaNoite());
        assertValor("300.00", antes.calcularTotal());
    }

    @Test
    void reservaCriadaDepoisDaAlteracaoUsaOValorNovoDaVista() {
        Reserva antes = novaReserva(1);
        quarto.setSuplementoVista(new BigDecimal("35.00"));

        Reserva depois = novaReserva(2);

        assertValor("300.00", antes.calcularTotal());    // 3 x (80 + 20)
        assertValor("345.00", depois.calcularTotal());   // 3 x (80 + 35)
        assertValor("35.00", depois.getValorVistaNoite());
    }

    @Test
    void alterarOPrecoBaseDoQuartoNaoAlteraUmaReservaJaCriada() {
        Reserva antes = novaReserva(1);

        quarto.setPrecoBase(new BigDecimal("90.00"));
        Reserva depois = novaReserva(2);

        assertValor("300.00", antes.calcularTotal());    // 3 x (80 + 20)
        assertValor("330.00", depois.calcularTotal());   // 3 x (90 + 20)
    }

    @Test
    void reservaLidaDaBaseDeDadosUsaOsPrecosGuardadosENaoOsAtuaisDoQuarto() {
        // Na base de dados ficou 80 + 20; entretanto a vista do quarto passou para 35.
        quarto.setSuplementoVista(new BigDecimal("35.00"));

        Reserva lida = new Reserva(7, cliente, quarto, entrada, saida, new BigDecimal("80.00"),
                new BigDecimal("20.00"), EstadoReserva.PENDENTE, null, null, null, List.of(), List.of());

        assertValor("300.00", lida.calcularTotal());
    }

    // ---- Extras ----

    @Test
    void alterarOValorDeUmExtraNaoAlteraUmaReservaJaCriada() {
        Reserva antes = novaReservaComLimpezas(1, 2);

        limpeza.setValor(new BigDecimal("15.00"));   // o gestor sobe a limpeza de 10 para 15

        assertValor("10.00", antes.getConsumos().get(0).getValorUnitario());
        assertValor("320.00", antes.calcularTotal());   // 300 + 2 x 10
    }

    @Test
    void reservaCriadaDepoisDaAlteracaoUsaOValorNovoDoExtra() {
        Reserva antes = novaReservaComLimpezas(1, 2);
        limpeza.setValor(new BigDecimal("15.00"));

        Reserva depois = novaReservaComLimpezas(2, 2);

        assertValor("320.00", antes.calcularTotal());    // 300 + 2 x 10
        assertValor("330.00", depois.calcularTotal());   // 300 + 2 x 15
    }

    // ---- Extras por noite ou por unidade ----

    @Test
    void extraPorUnidadeNaoDependeDasNoites() {
        ConsumoServico linha = new ConsumoServico(limpeza, 2, entrada);

        assertValor("20.00", linha.total(1));
        assertValor("20.00", linha.total(3));
    }

    @Test
    void extraPorNoiteEMultiplicadoPelasNoites() {
        limpeza.setCobranca(TipoCobranca.POR_NOITE);
        ConsumoServico linha = new ConsumoServico(limpeza, 2, entrada);

        assertValor("20.00", linha.total(1));
        assertValor("60.00", linha.total(3));
    }

    @Test
    void mudarAFormaDeCobrancaDoExtraNaoAlteraUmaReservaJaCriada() {
        Reserva antes = novaReservaComLimpezas(1, 2);

        limpeza.setCobranca(TipoCobranca.POR_NOITE);
        Reserva depois = novaReservaComLimpezas(2, 2);

        assertEquals(TipoCobranca.POR_UNIDADE, antes.getConsumos().get(0).getCobranca());
        assertValor("320.00", antes.calcularTotal());    // 300 + 2 x 10
        assertValor("360.00", depois.calcularTotal());   // 300 + 2 x 10 x 3 noites
    }
}
