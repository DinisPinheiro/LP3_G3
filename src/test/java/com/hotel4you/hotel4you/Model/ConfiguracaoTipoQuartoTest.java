package com.hotel4you.hotel4you.Model;

import com.hotel4you.hotel4you.Model.enums.TipoQuarto;
import com.hotel4you.hotel4you.Model.enums.Vista;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * S6-09: herança da configuração do tipo de quarto (preço base e equipamentos) e exceções por quarto.
 */
class ConfiguracaoTipoQuartoTest {

    private Equipamento toalhas;
    private Equipamento secador;
    private Equipamento cofre;
    private Artigo televisao;
    private Artigo champo;
    private ConfiguracaoTipoQuarto duplo;

    @BeforeEach
    void preparar() {
        toalhas = new Equipamento(1, "Toalhas");
        secador = new Equipamento(2, "Secador");
        cofre = new Equipamento(3, "Cofre");
        televisao = new Artigo(1, "Televisão", "un", 5);
        champo = new Artigo(2, "Champô", "un", 20);
        duplo = new ConfiguracaoTipoQuarto(TipoQuarto.DUPLO, new BigDecimal("80.00"),
                List.of(toalhas, secador), List.of(new ProdutoQuarto(televisao, 1)));
    }

    private Quarto quartoDuplo(int numero) {
        return new Quarto(numero, numero, 2, "", TipoQuarto.DUPLO, BigDecimal.ZERO, Vista.NENHUMA, true);
    }

    private static void assertValor(String esperado, BigDecimal obtido) {
        assertEquals(0, new BigDecimal(esperado).compareTo(obtido), "esperado " + esperado + ", obtido " + obtido);
    }

    // ---- A configuração do tipo ----

    @Test
    void configuracaoValidaOPrecoERetiraRepetidos() {
        assertThrows(IllegalArgumentException.class,
                () -> new ConfiguracaoTipoQuarto(TipoQuarto.DUPLO, new BigDecimal("-1"), List.of(), List.of()));
        assertThrows(NullPointerException.class,
                () -> new ConfiguracaoTipoQuarto(null, BigDecimal.TEN, List.of(), List.of()));

        ConfiguracaoTipoQuarto cfg = new ConfiguracaoTipoQuarto(TipoQuarto.FAMILIA, BigDecimal.TEN,
                List.of(toalhas, toalhas), List.of(new ProdutoQuarto(champo, 1), new ProdutoQuarto(champo, 3)));

        assertEquals(1, cfg.getEquipamentos().size());
        assertEquals(1, cfg.getProdutos().size());
        assertEquals(3, cfg.getProdutos().get(0).getQuantidade(), "o mesmo produto fica com a última quantidade");
    }

    @Test
    void configuracaoNaoDeixaMudarAsListasPorFora() {
        assertThrows(UnsupportedOperationException.class, () -> duplo.getEquipamentos().add(cofre));
        assertThrows(UnsupportedOperationException.class,
                () -> duplo.getProdutos().add(new ProdutoQuarto(champo, 1)));
    }

    // ---- Herança ----

    @Test
    void quartoComValoresPropriosTemExcecoes() {
        Quarto quarto = quartoDuplo(205);

        assertTrue(quarto.isPrecoPersonalizado());
        assertTrue(quarto.isEquipamentosPersonalizados());
    }

    @Test
    void quartoHerdaOPrecoEOsEquipamentosDoTipo() {
        Quarto quarto = quartoDuplo(205);

        quarto.herdarPreco(duplo);
        quarto.herdarEquipamentos(duplo);

        assertFalse(quarto.isPrecoPersonalizado());
        assertFalse(quarto.isEquipamentosPersonalizados());
        assertValor("80.00", quarto.getPrecoBase());
        assertEquals(2, quarto.getEquipamentos().size());
        assertEquals(1, quarto.getProdutos().size());
        assertEquals("Televisão", quarto.getProdutos().get(0).getArtigo().getNome());
    }

    @Test
    void quartoHerdadoTemOPrecoPorNoiteDoTipoMaisAVista() {
        Quarto quarto = new Quarto(1, 205, 2, "", TipoQuarto.DUPLO, BigDecimal.ZERO, Vista.MAR, true);
        quarto.setSuplementoVista(new BigDecimal("20.00"));

        quarto.herdarPreco(duplo);

        assertValor("100.00", quarto.precoNoite());
    }

    @Test
    void excecaoDePrecoNaoMudaOsEquipamentosHerdados() {
        Quarto quarto = quartoDuplo(205);
        quarto.herdarPreco(duplo);
        quarto.herdarEquipamentos(duplo);

        quarto.personalizarPreco(new BigDecimal("95.00"));

        assertTrue(quarto.isPrecoPersonalizado());
        assertFalse(quarto.isEquipamentosPersonalizados());
        assertValor("95.00", quarto.getPrecoBase());
        assertEquals(2, quarto.getEquipamentos().size());
    }

    @Test
    void excecaoDeEquipamentosNaoMudaOPrecoHerdado() {
        Quarto quarto = quartoDuplo(205);
        quarto.herdarPreco(duplo);
        quarto.herdarEquipamentos(duplo);

        quarto.personalizarEquipamentos(List.of(cofre), List.of(new ProdutoQuarto(champo, 2)));

        assertTrue(quarto.isEquipamentosPersonalizados());
        assertFalse(quarto.isPrecoPersonalizado());
        assertEquals(List.of(cofre), quarto.getEquipamentos());
        assertEquals(1, quarto.getProdutos().size());
        assertEquals("Champô", quarto.getProdutos().get(0).getArtigo().getNome());
        assertValor("80.00", quarto.getPrecoBase());
    }

    @Test
    void voltarAHerdarDesfazAExcecao() {
        Quarto quarto = quartoDuplo(205);
        quarto.personalizarPreco(new BigDecimal("95.00"));
        quarto.personalizarEquipamentos(List.of(cofre), List.of());

        quarto.herdarPreco(duplo);
        quarto.herdarEquipamentos(duplo);

        assertFalse(quarto.isPrecoPersonalizado());
        assertFalse(quarto.isEquipamentosPersonalizados());
        assertValor("80.00", quarto.getPrecoBase());
        assertEquals(2, quarto.getEquipamentos().size());
    }

    @Test
    void naoSePodeHerdarDeOutroTipo() {
        Quarto individual = new Quarto(1, 201, 2, "", TipoQuarto.INDIVIDUAL, BigDecimal.ZERO, Vista.NENHUMA, true);

        assertThrows(IllegalArgumentException.class, () -> individual.herdarPreco(duplo));
        assertThrows(IllegalArgumentException.class, () -> individual.herdarEquipamentos(duplo));
        assertThrows(IllegalArgumentException.class, () -> individual.aplicarConfiguracao(duplo));
    }

    // ---- Aplicar a configuração (quando o gestor a muda) ----

    @Test
    void aplicarConfiguracaoSoMudaOQueOQuartoHerda() {
        Quarto herda = quartoDuplo(205);
        herda.herdarPreco(duplo);
        herda.herdarEquipamentos(duplo);
        Quarto excecao = quartoDuplo(206);
        excecao.personalizarPreco(new BigDecimal("95.00"));
        excecao.personalizarEquipamentos(List.of(cofre), List.of());
        Quarto soPreco = quartoDuplo(207);
        soPreco.herdarPreco(duplo);
        soPreco.personalizarEquipamentos(List.of(cofre), List.of());

        ConfiguracaoTipoQuarto nova = new ConfiguracaoTipoQuarto(TipoQuarto.DUPLO, new BigDecimal("90.00"),
                List.of(toalhas), List.of());

        assertTrue(herda.aplicarConfiguracao(nova));
        assertTrue(soPreco.aplicarConfiguracao(nova));
        assertFalse(excecao.aplicarConfiguracao(nova), "um quarto só com exceções não muda");

        assertValor("90.00", herda.getPrecoBase());
        assertEquals(List.of(toalhas), herda.getEquipamentos());
        assertTrue(herda.getProdutos().isEmpty());

        assertValor("95.00", excecao.getPrecoBase());
        assertEquals(List.of(cofre), excecao.getEquipamentos());

        assertValor("90.00", soPreco.getPrecoBase());
        assertEquals(List.of(cofre), soPreco.getEquipamentos());
    }

    @Test
    void aplicarConfiguracaoIgualNaoMudaNada() {
        Quarto quarto = quartoDuplo(205);
        quarto.herdarPreco(duplo);
        quarto.herdarEquipamentos(duplo);

        assertFalse(quarto.aplicarConfiguracao(duplo));
    }

    @Test
    void mudarAQuantidadeDeUmProdutoDoTipoContaComoMudanca() {
        Quarto quarto = quartoDuplo(205);
        quarto.herdarPreco(duplo);
        quarto.herdarEquipamentos(duplo);

        ConfiguracaoTipoQuarto nova = new ConfiguracaoTipoQuarto(TipoQuarto.DUPLO, new BigDecimal("80.00"),
                List.of(toalhas, secador), List.of(new ProdutoQuarto(televisao, 2)));

        assertTrue(quarto.aplicarConfiguracao(nova));
        assertEquals(2, quarto.getProdutos().get(0).getQuantidade());
    }
}
