package com.hotel4you.hotel4you.Model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes unitários do cartão magnético.
 */
class CartaoMagneticoTest {

    @Test
    void cartaoNovoFicaAtivoESemId() {
        CartaoMagnetico cartao = new CartaoMagnetico(1001, false);

        assertEquals(0, cartao.getId());
        assertEquals(1001, cartao.getNumero());
        assertFalse(cartao.isMestre());
        assertTrue(cartao.isAtivo());
    }

    @Test
    void numeroTemDeSerPositivo() {
        assertThrows(IllegalArgumentException.class, () -> new CartaoMagnetico(0, false));
        assertThrows(IllegalArgumentException.class, () -> new CartaoMagnetico(-5, false));
    }

    @Test
    void desativarTiraOEstadoAtivo() {
        CartaoMagnetico cartao = new CartaoMagnetico(1001, false);

        cartao.desativar();

        assertFalse(cartao.isAtivo());
    }

    @Test
    void cartaoNormalAtivoAbreOsPisosDeDormidaEDeServicos() {
        CartaoMagnetico cartao = new CartaoMagnetico(1001, false);

        assertFalse(cartao.temAcessoAoPiso(-1));
        assertFalse(cartao.temAcessoAoPiso(0));
        assertTrue(cartao.temAcessoAoPiso(1));
        assertTrue(cartao.temAcessoAoPiso(2));
        assertTrue(cartao.temAcessoAoPiso(3));
        assertFalse(cartao.temAcessoAoPiso(9));
    }

    @Test
    void cartaoMestreAtivoAbreTodosOsPisos() {
        CartaoMagnetico mestre = new CartaoMagnetico(1, true);

        for (int piso = -1; piso <= 3; piso++) {
            assertTrue(mestre.temAcessoAoPiso(piso), "piso " + piso);
        }
    }

    @Test
    void cartaoDesativadoJaNaoAbreNenhumPiso() {
        CartaoMagnetico cartao = new CartaoMagnetico(1001, false);
        cartao.desativar();

        for (int piso = -1; piso <= 3; piso++) {
            assertFalse(cartao.temAcessoAoPiso(piso), "piso " + piso);
        }
    }

    @Test
    void cartaoReconstruidoGuardaTodoOEstado() {
        CartaoMagnetico cartao = new CartaoMagnetico(7, 1002, false, false);

        assertEquals(7, cartao.getId());
        assertEquals(1002, cartao.getNumero());
        assertFalse(cartao.isAtivo());
    }

    @Test
    void toStringMostraONumeroEOEstado() {
        CartaoMagnetico cartao = new CartaoMagnetico(1001, false);
        assertEquals("Cartão 1001", cartao.toString());

        cartao.desativar();
        assertEquals("Cartão 1001 (desativado)", cartao.toString());
        assertEquals("Cartão 1 (mestre)", new CartaoMagnetico(1, true).toString());
    }
}
