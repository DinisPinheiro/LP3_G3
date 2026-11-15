package com.hotel4you.hotel4you.Model;

import com.hotel4you.hotel4you.Model.enums.Perfil;
import com.hotel4you.hotel4you.Model.enums.TipoMovimento;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** S5-04: o stock do hotel fica separado do stock disponível (que desconta o usado em reservas). */
class ArtigoStockDisponivelTest {

    private static Artigo toalhas(int stock) {
        return new Artigo(1, "Toalhas", "un", stock);
    }

    @Test
    void semReservasODisponivelEIgualAoStockDoHotel() {
        Artigo artigo = toalhas(50);
        assertEquals(50, artigo.getQuantidadeAtual());
        assertEquals(50, artigo.getQuantidadeDisponivel());
    }

    @Test
    void disponivelDescontaOUsadoEmReservasSemMexerNoStockDoHotel() {
        Artigo artigo = toalhas(50);
        artigo.setQuantidadeReservada(12);
        assertEquals(50, artigo.getQuantidadeAtual(), "o stock do hotel não muda");
        assertEquals(38, artigo.getQuantidadeDisponivel());
    }

    @Test
    void disponivelNuncaFicaNegativo() {
        Artigo artigo = toalhas(10);
        artigo.setQuantidadeReservada(15);
        assertEquals(0, artigo.getQuantidadeDisponivel());
    }

    @Test
    void entradaAumentaOsDoisValores() {
        Artigo artigo = toalhas(10);
        artigo.setQuantidadeReservada(4);
        Utilizador gestor = Utilizador.novo("gestor", "gestor123", "Gestor", Perfil.GESTOR);
        artigo.aplicar(new MovimentoStock(TipoMovimento.ENTRADA, 20, LocalDateTime.now(), gestor, artigo));
        assertEquals(30, artigo.getQuantidadeAtual());
        assertEquals(26, artigo.getQuantidadeDisponivel());
    }

    @Test
    void reservadoNegativoERejeitado() {
        assertThrows(IllegalArgumentException.class, () -> toalhas(5).setQuantidadeReservada(-1));
    }

    @Test
    void artigoGuardaUltimoPrecoUnidadeETaxa() {
        Artigo artigo = toalhas(5);
        assertNull(artigo.getPrecoUnitario());
        assertNull(artigo.getTaxaPercentagem());
        assertEquals("", artigo.getUnidadePreco());
        artigo.setUltimoPreco(new BigDecimal("2.51"), "cx", 23);
        assertEquals(new BigDecimal("2.51"), artigo.getPrecoUnitario());
        assertEquals("cx", artigo.getUnidadePreco());
        assertEquals(23, artigo.getTaxaPercentagem());
        assertEquals(5, artigo.getQuantidadeAtual(), "guardar o preço não mexe no stock");
    }

    @Test
    void precoNegativoETaxaForaDeIntervaloSaoRejeitados() {
        Artigo artigo = toalhas(5);
        assertThrows(IllegalArgumentException.class, () -> artigo.setUltimoPreco(new BigDecimal("-1"), "un", 23));
        assertThrows(IllegalArgumentException.class, () -> artigo.setUltimoPreco(BigDecimal.ONE, "un", 101));
        assertThrows(IllegalArgumentException.class, () -> artigo.setUltimoPreco(BigDecimal.ONE, "un", -1));
    }
}
