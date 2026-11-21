package com.hotel4you.hotel4you.Model;

import com.hotel4you.hotel4you.Model.enums.Perfil;
import com.hotel4you.hotel4you.Model.enums.TipoMovimento;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * S6-04: stock real (o total do hotel) e stock disponível (o real menos o que está em uso nas reservas),
 * saída de stock com justificação e diferença entre produtos consumíveis e não consumíveis.
 */
class StockRealDisponivelTest {

    private final Utilizador gestor = Utilizador.novo("gestor", "gestor123", "Gestor", Perfil.GESTOR);

    private static Artigo televisoes(int real) {
        return new Artigo(1, "Televisão LG 32", "un", real);
    }

    private static Artigo champo(int real) {
        Artigo artigo = new Artigo(2, "Champô 200ml", "un", real);
        artigo.setConsumivel(true);
        return artigo;
    }

    private MovimentoStock saida(Artigo artigo, int quantidade, String justificacao) {
        return new MovimentoStock(TipoMovimento.SAIDA, quantidade, LocalDateTime.now(), gestor, artigo, justificacao);
    }

    // ---- Stock real e disponível ----

    @Test
    void stockDisponivelDescontaOQueEstaEmUsoNasReservas() {
        Artigo tv = televisoes(20);
        tv.darBaixa(5);

        assertEquals(20, tv.getQuantidadeAtual(), "o stock real não muda quando a reserva usa o produto");
        assertEquals(5, tv.getQuantidadeReservada());
        assertEquals(15, tv.getQuantidadeDisponivel());
    }

    @Test
    void naoSeDaBaixaDeMaisDoQueODisponivel() {
        Artigo tv = televisoes(3);
        tv.darBaixa(2);

        IllegalStateException erro = assertThrows(IllegalStateException.class, () -> tv.darBaixa(2));
        assertTrue(erro.getMessage().contains("disponível"));
        assertEquals(2, tv.getQuantidadeReservada(), "a baixa falhada não altera o stock");
    }

    @Test
    void devolverVoltaAoDisponivelSemMexerNoReal() {
        Artigo tv = televisoes(10);
        tv.darBaixa(4);

        tv.devolver(3);

        assertEquals(10, tv.getQuantidadeAtual());
        assertEquals(9, tv.getQuantidadeDisponivel());
        assertThrows(IllegalArgumentException.class, () -> tv.devolver(2), "só há 1 em uso");
    }

    // ---- Consumíveis e não consumíveis ----

    @Test
    void naoConsumivelRegressaAoStockNoCheckOut() {
        Artigo tv = televisoes(10);
        tv.darBaixa(2);

        boolean gasto = tv.reporNoCheckOut(2);

        assertFalse(gasto);
        assertEquals(10, tv.getQuantidadeAtual(), "o stock real fica igual");
        assertEquals(10, tv.getQuantidadeDisponivel(), "volta tudo ao disponível");
    }

    @Test
    void consumivelDaSaidaDefinitivaNoCheckOut() {
        Artigo champo = champo(50);
        champo.darBaixa(2);
        assertEquals(48, champo.getQuantidadeDisponivel());

        boolean gasto = champo.reporNoCheckOut(2);

        assertTrue(gasto);
        assertEquals(48, champo.getQuantidadeAtual(), "o champô gasto sai do stock real");
        assertEquals(48, champo.getQuantidadeDisponivel(), "e não volta ao disponível");
        assertEquals(0, champo.getQuantidadeReservada());
    }

    @Test
    void consumivelNuncaDeixaOStockRealNegativo() {
        Artigo champo = champo(1);
        champo.setQuantidadeReservada(3);

        champo.reporNoCheckOut(3);

        assertEquals(0, champo.getQuantidadeAtual());
        assertEquals(0, champo.getQuantidadeDisponivel());
    }

    // ---- Saída de stock com justificação ----

    @Test
    void saidaDescontaAoStockRealEAoDisponivel() {
        Artigo tv = televisoes(20);
        tv.darBaixa(5);

        tv.aplicar(saida(tv, 1, "Televisão avariada no quarto 205"));

        assertEquals(19, tv.getQuantidadeAtual());
        assertEquals(14, tv.getQuantidadeDisponivel());
    }

    @Test
    void saidaGuardaProdutoQuantidadeEJustificacao() {
        Artigo tv = televisoes(20);

        MovimentoStock movimento = saida(tv, 2, "  Televisão avariada  ");

        assertEquals(TipoMovimento.SAIDA, movimento.getTipo());
        assertEquals(tv, movimento.getArtigo());
        assertEquals(2, movimento.getQuantidade());
        assertEquals("Televisão avariada", movimento.getJustificacao());
        assertEquals(gestor, movimento.getUtilizador());
    }

    @Test
    void saidaSemJustificacaoNaoEAceite() {
        Artigo tv = televisoes(20);

        assertThrows(IllegalArgumentException.class, () -> saida(tv, 1, "  "));
        assertThrows(IllegalArgumentException.class, () -> saida(tv, 1, null));
        assertThrows(IllegalArgumentException.class, () -> saida(tv, 1, "x".repeat(MovimentoStock.MAX_JUSTIFICACAO + 1)));
    }

    @Test
    void entradaNaoPrecisaDeJustificacao() {
        MovimentoStock entrada = new MovimentoStock(TipoMovimento.ENTRADA, 5, LocalDateTime.now(), gestor, televisoes(0));

        assertEquals("", entrada.getJustificacao());
    }

    @Test
    void saidaNaoDeixaOStockRealNegativo() {
        Artigo tv = televisoes(2);

        assertThrows(IllegalStateException.class, () -> tv.aplicar(saida(tv, 3, "Avaria")));
        assertEquals(2, tv.getQuantidadeAtual());
    }

    @Test
    void saidaDeQuantidadeZeroNaoEAceite() {
        assertThrows(IllegalArgumentException.class, () -> saida(televisoes(5), 0, "Avaria"));
    }
}
