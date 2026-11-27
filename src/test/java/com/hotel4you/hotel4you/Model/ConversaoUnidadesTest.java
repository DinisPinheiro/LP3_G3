package com.hotel4you.hotel4you.Model;

import com.hotel4you.hotel4you.Model.encomenda.ImpostoLinha;
import com.hotel4you.hotel4you.Model.encomenda.LinhaEncomenda;
import com.hotel4you.hotel4you.Model.encomenda.Montante;
import com.hotel4you.hotel4you.Model.encomenda.PrecoUnitario;
import com.hotel4you.hotel4you.Model.encomenda.Quantidade;
import com.hotel4you.hotel4you.Model.enums.UnidadeMedida;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * S7-02: conversões de caixas para a unidade principal (regras em {@link ConversaoUnidades} e {@link Artigo}).
 */
class ConversaoUnidadesTest {

    /** Linha com a quantidade principal e, opcionalmente, uma quantidade informativa. */
    private static LinhaEncomenda linha(int valor, UnidadeMedida unidade, Quantidade... informativas) {
        Montante zero = new Montante(BigDecimal.ZERO, "EUR");
        return new LinhaEncomenda(1, "P1", "", "Toalha de banho", new Quantidade("Count", valor, unidade),
                List.of(informativas), new PrecoUnitario(new Montante(BigDecimal.ONE, "EUR"), 1, unidade),
                new ImpostoLinha("", 0, zero, "VAT", 23, zero, ""), zero);
    }

    private static Quantidade informativa(int valor, UnidadeMedida unidade) {
        return new Quantidade("Count", valor, unidade);
    }

    // ---- Conversão do ficheiro ----

    @Test
    void conversaoVemDoFicheiroQuandoTrazCaixasEUnidades() {
        LinhaEncomenda emCaixas = linha(50, UnidadeMedida.CAIXA, informativa(500, UnidadeMedida.UNIDADE));
        LinhaEncomenda emUnidades = linha(200, UnidadeMedida.UNIDADE, informativa(10, UnidadeMedida.CAIXA));

        assertEquals(10, ConversaoUnidades.conversaoDoFicheiro(emCaixas).getAsInt());
        assertEquals(20, ConversaoUnidades.conversaoDoFicheiro(emUnidades).getAsInt());
        assertFalse(ConversaoUnidades.conversaoDoFicheiro(linha(50, UnidadeMedida.CAIXA)).isPresent());
    }

    @Test
    void conversaoDoFicheiroTemDeSerExata() {
        LinhaEncomenda linha = linha(3, UnidadeMedida.CAIXA, informativa(10, UnidadeMedida.UNIDADE));

        assertThrows(IllegalArgumentException.class, () -> ConversaoUnidades.conversaoDoFicheiro(linha));
    }

    @Test
    void aConversaoDoFicheiroTemPrioridadeSobreADefinidaEAGuardada() {
        LinhaEncomenda comFicheiro = linha(5, UnidadeMedida.CAIXA, informativa(60, UnidadeMedida.UNIDADE));
        LinhaEncomenda semFicheiro = linha(5, UnidadeMedida.CAIXA);

        assertEquals(12, ConversaoUnidades.escolherConversao(comFicheiro, 8, 6));
        assertEquals(8, ConversaoUnidades.escolherConversao(semFicheiro, 8, 6), "a definida agora");
        assertEquals(6, ConversaoUnidades.escolherConversao(semFicheiro, null, 6), "a guardada no produto");
        assertNull(ConversaoUnidades.escolherConversao(semFicheiro, null, null));
    }

    // ---- Quantidade na unidade principal ----

    @Test
    void caixasPassamAUnidadesComAConversao() {
        LinhaEncomenda linha = linha(3, UnidadeMedida.CAIXA);

        assertEquals(30, ConversaoUnidades.quantidadeNaUnidadePrincipal(linha, 10));
        assertEquals("un", ConversaoUnidades.unidadePrincipal(linha));
    }

    @Test
    void unidadesEQuilosFicamComoVem() {
        assertEquals(200, ConversaoUnidades.quantidadeNaUnidadePrincipal(linha(200, UnidadeMedida.UNIDADE), null));
        assertEquals(12, ConversaoUnidades.quantidadeNaUnidadePrincipal(linha(12, UnidadeMedida.QUILOGRAMA), null));
        assertEquals("un", ConversaoUnidades.unidadePrincipal(linha(200, UnidadeMedida.UNIDADE)));
        assertEquals("kg", ConversaoUnidades.unidadePrincipal(linha(12, UnidadeMedida.QUILOGRAMA)));
    }

    @Test
    void caixasSemConversaoPedemAoUtilizadorParaADefinir() {
        ConversaoEmFaltaException erro = assertThrows(ConversaoEmFaltaException.class,
                () -> ConversaoUnidades.quantidadeNaUnidadePrincipal(linha(3, UnidadeMedida.CAIXA), null));

        assertEquals(1, erro.getNumeroLinha());
        assertEquals("Toalha de banho", erro.getProduto());
        assertTrue(erro.getMessage().contains("Linha 1"));
    }

    // ---- Artigo: conversão e produtos antigos em caixas ----

    @Test
    void artigoConverteCaixasParaUnidadesEMostraAsDuas() {
        Artigo toalhas = new Artigo(1, "Toalha de banho", "un", 25);
        toalhas.definirConversaoCaixa(10);

        assertEquals(30, toalhas.caixasParaUnidades(3));
        assertEquals("1 cx = 10 un", toalhas.descreverConversao());
        assertEquals("25 un (2 cx + 5 un)", toalhas.descreverQuantidade(25));
        assertEquals("20 un (2 cx)", toalhas.descreverQuantidade(20));
    }

    @Test
    void artigoSemConversaoNaoConverteCaixas() {
        Artigo toalhas = new Artigo(1, "Toalha de banho", "un", 25);

        assertFalse(toalhas.temConversaoCaixa());
        assertEquals("", toalhas.descreverConversao());
        assertEquals("25 un", toalhas.descreverQuantidade(25));
        assertThrows(IllegalStateException.class, () -> toalhas.caixasParaUnidades(1));
    }

    @Test
    void conversaoTemLimites() {
        Artigo toalhas = new Artigo(1, "Toalha de banho", "un", 0);

        assertThrows(IllegalArgumentException.class, () -> toalhas.definirConversaoCaixa(0));
        assertThrows(IllegalArgumentException.class,
                () -> toalhas.definirConversaoCaixa(Artigo.MAX_UNIDADES_POR_CAIXA + 1));
    }

    @Test
    void produtoAntigoEmCaixasPassaAUnidades() {
        Artigo toalhas = new Artigo(1, "Toalha de banho", "cx", 50);
        toalhas.setQuantidadeReservada(2);
        assertTrue(toalhas.estaEmCaixas());
        assertThrows(IllegalArgumentException.class, toalhas::exigirUnidadePrincipal,
                "em caixas não pode ser usado nos quartos e nas reservas");

        toalhas.converterCaixasParaUnidades(10);

        assertFalse(toalhas.estaEmCaixas());
        assertEquals("un", toalhas.getUnidade());
        assertEquals(500, toalhas.getQuantidadeAtual());
        assertEquals(20, toalhas.getQuantidadeReservada());
        assertEquals(10, toalhas.getUnidadesPorCaixa());
        toalhas.exigirUnidadePrincipal();
        assertThrows(IllegalStateException.class, () -> toalhas.converterCaixasParaUnidades(10), "já está em unidades");
    }
}
