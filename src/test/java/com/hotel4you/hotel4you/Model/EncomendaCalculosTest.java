package com.hotel4you.hotel4you.Model;

import com.hotel4you.hotel4you.Model.encomenda.EncomendaFornecedor;
import com.hotel4you.hotel4you.Model.encomenda.FornecedorEncomenda;
import com.hotel4you.hotel4you.Model.encomenda.ImpostoLinha;
import com.hotel4you.hotel4you.Model.encomenda.LinhaEncomenda;
import com.hotel4you.hotel4you.Model.encomenda.Montante;
import com.hotel4you.hotel4you.Model.encomenda.PrecoUnitario;
import com.hotel4you.hotel4you.Model.encomenda.Quantidade;
import com.hotel4you.hotel4you.Model.encomenda.TotaisEncomenda;
import com.hotel4you.hotel4you.Model.enums.UnidadeMedida;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** S5-04: testes dos cálculos de linhas (valor sem taxa, taxa, total, caixas e unidades) e dos totais da encomenda. */
class EncomendaCalculosTest {

    private static Montante eur(String valor) {
        return new Montante(new BigDecimal(valor), "EUR");
    }

    /**
     * Linha como vem no XML: os valores {@code xmlSemTaxa}, {@code xmlTaxa} e {@code xmlTotal} são os do ficheiro
     * (os testes comparam-nos com os calculados).
     */
    private static LinhaEncomenda linha(int numero, Quantidade quantidade, List<Quantidade> informativas,
                                        String preco, int porQuantidade, UnidadeMedida unidadePreco, int taxa,
                                        String xmlSemTaxa, String xmlTaxa, String xmlTotal) {
        return new LinhaEncomenda(numero, "COD" + numero, "Supplier", "Produto " + numero, quantidade, informativas,
                new PrecoUnitario(eur(preco), porQuantidade, unidadePreco),
                new ImpostoLinha("Tax", 1, eur(xmlSemTaxa), "VAT", taxa, eur(xmlTaxa), "PT"),
                eur(xmlTotal));
    }

    private static Quantidade qtd(String tipo, int valor, UnidadeMedida unidade) {
        return new Quantidade(tipo, valor, unidade);
    }

    /** Toalhas: 50 caixas (500 unidades) a 2,51 EUR por caixa, IVA 23%. */
    private static LinhaEncomenda toalhas() {
        return linha(1, qtd("Count", 50, UnidadeMedida.CAIXA),
                List.of(qtd("NetWeight", 200, UnidadeMedida.QUILOGRAMA), qtd("Count", 500, UnidadeMedida.UNIDADE)),
                "2.51", 1, UnidadeMedida.CAIXA, 23, "125.50", "28.87", "154.37");
    }

    /** Champô: 200 unidades (10 caixas) a 3,49 EUR por unidade, IVA 23%. */
    private static LinhaEncomenda champo() {
        return linha(2, qtd("Count", 200, UnidadeMedida.UNIDADE),
                List.of(qtd("NetWeight", 20, UnidadeMedida.QUILOGRAMA), qtd("Count", 10, UnidadeMedida.CAIXA)),
                "3.49", 1, UnidadeMedida.UNIDADE, 23, "698.00", "160.54", "858.54");
    }

    /** Secador: 50 unidades a 29,99 EUR por unidade, sem caixas. */
    private static LinhaEncomenda secador() {
        return linha(3, qtd("Count", 50, UnidadeMedida.UNIDADE),
                List.of(qtd("NetWeight", 38, UnidadeMedida.QUILOGRAMA)),
                "29.99", 1, UnidadeMedida.UNIDADE, 23, "1499.50", "344.89", "1844.39");
    }

    private static EncomendaFornecedor encomenda(TotaisEncomenda resumoXml, LinhaEncomenda... linhas) {
        return new EncomendaFornecedor("OC1", LocalDate.of(2026, 10, 5),
                new FornecedorEncomenda("11PT920", "ProductHero", "Rua", "1", "Porto", "4000-032", "PT", "Portugal"),
                List.of(linhas), resumoXml);
    }

    // ---- linha: valor sem taxa, taxa e total ---------------------------------------------------

    @Test
    void linhaEmCaixasMultiplicaOPrecoPelasCaixas() {
        LinhaEncomenda linha = toalhas();
        assertEquals(new BigDecimal("125.50"), linha.subtotalCalculado());
        assertEquals(new BigDecimal("28.87"), linha.taxaCalculada(), "28,865 arredonda meio para cima");
        assertEquals(new BigDecimal("154.37"), linha.totalCalculado());
    }

    @Test
    void linhaEmUnidadesMultiplicaOPrecoPelasUnidades() {
        LinhaEncomenda linha = champo();
        assertEquals(new BigDecimal("698.00"), linha.subtotalCalculado());
        assertEquals(new BigDecimal("160.54"), linha.taxaCalculada());
        assertEquals(new BigDecimal("858.54"), linha.totalCalculado());
    }

    @Test
    void precoPorVariasUnidadesDivideAQuantidade() {
        // 5,00 EUR por cada 10 unidades; 25 unidades custam 12,50; taxa 6% = 0,75
        LinhaEncomenda linha = linha(1, qtd("Count", 25, UnidadeMedida.UNIDADE), List.of(), "5.00", 10,
                UnidadeMedida.UNIDADE, 6, "12.50", "0.75", "13.25");
        assertEquals(new BigDecimal("12.50"), linha.subtotalCalculado());
        assertEquals(new BigDecimal("0.75"), linha.taxaCalculada());
        assertEquals(new BigDecimal("13.25"), linha.totalCalculado());
    }

    @Test
    void precoEmUnidadeDiferenteDaEncomendadaUsaAQuantidadeInformativa() {
        // Encomendado em caixas (50), mas o preço é por unidade (500 unidades a 0,25).
        LinhaEncomenda linha = linha(1, qtd("Count", 50, UnidadeMedida.CAIXA),
                List.of(qtd("Count", 500, UnidadeMedida.UNIDADE)), "0.25", 1, UnidadeMedida.UNIDADE, 23,
                "125.00", "28.75", "153.75");
        assertTrue(linha.subtotalCalculavel());
        assertEquals(new BigDecimal("125.00"), linha.subtotalCalculado());
        assertEquals(new BigDecimal("153.75"), linha.totalCalculado());
    }

    @Test
    void semQuantidadeNaUnidadeDoPrecoUsaOValorSemTaxaDoXML() {
        LinhaEncomenda linha = linha(1, qtd("Count", 50, UnidadeMedida.CAIXA), List.of(), "2.51", 1,
                UnidadeMedida.UNIDADE, 23, "125.50", "28.87", "154.37");
        assertFalse(linha.subtotalCalculavel());
        assertEquals(new BigDecimal("125.50"), linha.subtotalCalculado());
        assertEquals(new BigDecimal("154.37"), linha.totalCalculado());
        assertTrue(encomenda(null, linha).avisos().get(0).startsWith("Linha 1: o preço é por un"));
    }

    @Test
    void taxaArredondaMeioParaCima() {
        // 0,05 x 10% = 0,005 -> 0,01
        LinhaEncomenda linha = linha(1, qtd("Count", 1, UnidadeMedida.UNIDADE), List.of(), "0.05", 1,
                UnidadeMedida.UNIDADE, 10, "0.05", "0.01", "0.06");
        assertEquals(new BigDecimal("0.01"), linha.taxaCalculada());
    }

    @Test
    void taxaZeroNaoAcrescentaNada() {
        LinhaEncomenda linha = linha(1, qtd("Count", 3, UnidadeMedida.UNIDADE), List.of(), "1.10", 1,
                UnidadeMedida.UNIDADE, 0, "3.30", "0.00", "3.30");
        assertEquals(new BigDecimal("0.00"), linha.taxaCalculada());
        assertEquals(new BigDecimal("3.30"), linha.totalCalculado());
    }

    // ---- linha: caixas e unidades --------------------------------------------------------------

    @Test
    void linhaGuardaCaixasEUnidades() {
        assertEquals(50, toalhas().caixas().getAsInt());
        assertEquals(500, toalhas().unidades().getAsInt());
        assertEquals(10, champo().caixas().getAsInt());
        assertEquals(200, champo().unidades().getAsInt());
    }

    @Test
    void linhaSemCaixasNaoInventaCaixas() {
        assertTrue(secador().caixas().isEmpty());
        assertEquals(50, secador().unidades().getAsInt());
    }

    // ---- encomenda: totais ---------------------------------------------------------------------

    @Test
    void totaisSomamLinhasTaxasETotalComTaxas() {
        TotaisEncomenda totais = encomenda(null, toalhas(), champo(), secador()).totais();
        assertEquals(new BigDecimal("2323.00"), totais.totalLinhas());
        assertEquals(new BigDecimal("534.30"), totais.totalTaxas());
        assertEquals(new BigDecimal("2857.30"), totais.totalComTaxas());
        assertEquals("EUR", totais.moeda());
    }

    @Test
    void totalComTaxasEIgualAoTotalDasLinhasMaisAsTaxas() {
        TotaisEncomenda totais = encomenda(null, toalhas(), champo(), secador()).totais();
        assertEquals(0, totais.totalComTaxas().compareTo(totais.totalLinhas().add(totais.totalTaxas())));
    }

    @Test
    void totaisNaoPerdemCentimos() {
        // 0,1 + 0,2 com double dava 0,30000000000000004; com BigDecimal é exato
        LinhaEncomenda a = linha(1, qtd("Count", 1, UnidadeMedida.UNIDADE), List.of(), "0.10", 1,
                UnidadeMedida.UNIDADE, 0, "0.10", "0.00", "0.10");
        LinhaEncomenda b = linha(2, qtd("Count", 1, UnidadeMedida.UNIDADE), List.of(), "0.20", 1,
                UnidadeMedida.UNIDADE, 0, "0.20", "0.00", "0.20");
        assertEquals(new BigDecimal("0.30"), encomenda(null, a, b).totais().totalLinhas());
    }

    @Test
    void moedasDiferentesNaMesmaEncomendaSaoRejeitadas() {
        LinhaEncomenda dolares = new LinhaEncomenda(2, "X", "Supplier", "Produto 2",
                qtd("Count", 1, UnidadeMedida.UNIDADE), List.of(),
                new PrecoUnitario(new Montante(new BigDecimal("1.00"), "USD"), 1, UnidadeMedida.UNIDADE),
                new ImpostoLinha("Tax", 1, new Montante(new BigDecimal("1.00"), "USD"), "VAT", 0,
                        new Montante(BigDecimal.ZERO, "USD"), "PT"),
                new Montante(new BigDecimal("1.00"), "USD"));
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> encomenda(null, toalhas(), dolares).totais());
        assertTrue(erro.getMessage().startsWith("Linha 2"));
    }

    // ---- avisos: valores do XML contra os calculados -------------------------------------------

    @Test
    void xmlCertoNaoTemAvisos() {
        TotaisEncomenda resumo = new TotaisEncomenda(new BigDecimal("2323.00"), new BigDecimal("534.30"),
                new BigDecimal("2857.30"), "EUR");
        assertTrue(encomenda(resumo, toalhas(), champo(), secador()).avisos().isEmpty());
    }

    @Test
    void impostoDoXMLErradoGeraAvisoMasUsaOCalculado() {
        // o fornecedor escreveu 160,51 de imposto em vez de 160,54
        LinhaEncomenda errada = linha(2, qtd("Count", 200, UnidadeMedida.UNIDADE), List.of(), "3.49", 1,
                UnidadeMedida.UNIDADE, 23, "698.00", "160.51", "858.54");
        List<String> avisos = encomenda(null, errada).avisos();
        assertEquals(1, avisos.size());
        assertTrue(avisos.get(0).startsWith("Linha 2: imposto no XML (160.51)"));
        assertEquals(new BigDecimal("160.54"), errada.taxaCalculada());
    }

    @Test
    void resumoDoXMLErradoGeraAviso() {
        TotaisEncomenda resumo = new TotaisEncomenda(new BigDecimal("2323.00"), new BigDecimal("534.30"),
                new BigDecimal("2900.00"), "EUR");
        List<String> avisos = encomenda(resumo, toalhas(), champo(), secador()).avisos();
        assertEquals(1, avisos.size());
        assertTrue(avisos.get(0).startsWith("Encomenda: total com taxas no XML (2900.00)"));
    }
}
