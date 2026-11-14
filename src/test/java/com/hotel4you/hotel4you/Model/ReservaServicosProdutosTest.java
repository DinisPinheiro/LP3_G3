package com.hotel4you.hotel4you.Model;

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
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Testes unitários da S5-06: serviços, extras e produtos na reserva, com quantidades.
 * O total (preço base do quarto + vista + extras) é calculado pelo model.
 */
class ReservaServicosProdutosTest {

    private final LocalDate entrada = LocalDate.of(2026, 12, 1);
    private final LocalDate saida = LocalDate.of(2026, 12, 4);   // 3 noites

    private Cliente cliente;
    private Quarto quarto;
    private Servico limpeza;
    private Servico agua;
    private Servico pequenoAlmoco;
    private Artigo toalhas;

    @BeforeEach
    void preparar() {
        cliente = new Cliente(1, "Maria Silva", "PT123456", "910000000");
        // 80 (preço base) + 20 (vista mar) = 100 por noite; 3 noites = 300
        quarto = new Quarto(1, 202, 2, "", TipoQuarto.DUPLO, new BigDecimal("80.00"), Vista.MAR, true);
        limpeza = servico(1, "Limpeza extra", "10.00", TipoCobranca.POR_UNIDADE);
        agua = servico(2, "Água", "1.50", TipoCobranca.POR_UNIDADE);
        pequenoAlmoco = servico(3, "Pequeno-almoço", "8.00", TipoCobranca.POR_NOITE);
        toalhas = new Artigo(1, "Toalhas", "un", 50);
    }

    private static Servico servico(int id, String nome, String valor, TipoCobranca cobranca) {
        return new Servico(id, TipoServico.EXTRA, nome, "", "", new BigDecimal(valor), cobranca, true);
    }

    private ConsumoServico linha(Servico servico, int quantidade) {
        return new ConsumoServico(servico, quantidade, entrada);
    }

    private Reserva reservaCom(List<ConsumoServico> servicos, List<ProdutoReserva> produtos) {
        return new Reserva(1, cliente, quarto, entrada, saida, servicos, produtos);
    }

    private static void assertValor(String esperado, BigDecimal obtido) {
        assertEquals(0, new BigDecimal(esperado).compareTo(obtido), "esperado " + esperado + ", obtido " + obtido);
    }

    // ---- Total com serviços e extras ----

    @Test
    void semExtrasOTotalEPrecoBaseMaisVistaVezesAsNoites() {
        assertValor("300.00", new Reserva(1, cliente, quarto, entrada, saida).calcularTotal());
    }

    @Test
    void duasLimpezasSomamDuasVezesOValorDaLimpeza() {
        Reserva reserva = reservaCom(List.of(linha(limpeza, 2)), List.of());

        assertValor("320.00", reserva.calcularTotal());   // 300 + 2 x 10
    }

    @Test
    void totalComVariosServicosEQuantidadesDiferentes() {
        Reserva reserva = reservaCom(List.of(linha(limpeza, 2), linha(agua, 3)), List.of());

        assertValor("324.50", reserva.calcularTotal());   // 300 + 2 x 10 + 3 x 1,50
    }

    @Test
    void servicoPorNoiteEMultiplicadoPelaQuantidadeEPelasNoites() {
        Reserva reserva = reservaCom(List.of(linha(pequenoAlmoco, 2)), List.of());

        assertValor("348.00", reserva.calcularTotal());   // 300 + 2 x 8 x 3 noites
    }

    @Test
    void totalJuntaServicosPorUnidadeEPorNoite() {
        Reserva reserva = reservaCom(List.of(linha(limpeza, 1), linha(pequenoAlmoco, 1), linha(agua, 4)), List.of());

        assertValor("340.00", reserva.calcularTotal());   // 300 + 10 + 8 x 3 + 4 x 1,50
    }

    @Test
    void aEstimativaUsaAMesmaContaDaReserva() {
        List<ConsumoServico> servicos = List.of(linha(limpeza, 2), linha(pequenoAlmoco, 2));

        BigDecimal estimativa = Reserva.calcularTotal(quarto.precoNoite(), 3, servicos);

        assertValor("368.00", estimativa);   // 300 + 2 x 10 + 2 x 8 x 3
        assertValor("368.00", reservaCom(servicos, List.of()).calcularTotal());
    }

    @Test
    void reservaCanceladaNaoTemCustoMesmoComExtras() {
        Reserva reserva = reservaCom(List.of(linha(limpeza, 2)), List.of(new ProdutoReserva(toalhas, 4)));

        reserva.cancelar();

        assertValor("0", reserva.calcularTotal());
    }

    @Test
    void quantidadeTemDeSerPeloMenosUm() {
        assertThrows(IllegalArgumentException.class, () -> linha(limpeza, 0));
        assertThrows(IllegalArgumentException.class, () -> linha(limpeza, -2));
        assertThrows(IllegalArgumentException.class, () -> new ProdutoReserva(toalhas, 0));
        assertThrows(IllegalArgumentException.class, () -> new ProdutoReserva(toalhas, -1));
    }

    // ---- Produtos do stock ----

    @Test
    void produtosFicamNaReservaMasNaoEntramNoTotal() {
        Reserva reserva = reservaCom(List.of(), List.of(new ProdutoReserva(toalhas, 4)));

        assertEquals(1, reserva.getProdutos().size());
        assertEquals(4, reserva.getProdutos().get(0).getQuantidade());
        assertValor("300.00", reserva.calcularTotal());
    }

    @Test
    void quantidadeUsadaSomaAsLinhasDoMesmoArtigo() {
        Artigo champo = new Artigo(2, "Champô", "un", 30);
        Reserva reserva = reservaCom(List.of(),
                List.of(new ProdutoReserva(toalhas, 4), new ProdutoReserva(champo, 1), new ProdutoReserva(toalhas, 2)));

        assertEquals(6, reserva.quantidadeUsada(toalhas));
        assertEquals(1, reserva.quantidadeUsada(champo));
        assertEquals(0, reserva.quantidadeUsada(new Artigo(3, "Lençóis", "un", 10)));
    }

    @Test
    void reservaCanceladaNaoUsaStock() {
        Reserva reserva = reservaCom(List.of(), List.of(new ProdutoReserva(toalhas, 4)));

        reserva.cancelar();

        assertEquals(0, reserva.quantidadeUsada(toalhas));
    }

    @Test
    void stockDisponivelEOStockDoHotelMenosOUsadoEmReservas() {
        toalhas.setQuantidadeReservada(8);

        assertEquals(50, toalhas.getQuantidadeAtual());      // o stock do hotel não muda
        assertEquals(8, toalhas.getQuantidadeReservada());
        assertEquals(42, toalhas.getQuantidadeDisponivel());
    }

    @Test
    void stockDisponivelNuncaENegativo() {
        toalhas.setQuantidadeReservada(60);

        assertEquals(0, toalhas.getQuantidadeDisponivel());
        assertThrows(IllegalArgumentException.class, () -> toalhas.setQuantidadeReservada(-1));
    }
}
