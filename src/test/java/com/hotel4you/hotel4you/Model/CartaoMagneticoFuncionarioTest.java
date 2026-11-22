package com.hotel4you.hotel4you.Model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes unitários do dono de um cartão (cartão de funcionário), S6-06.
 */
class CartaoMagneticoFuncionarioTest {

    @Test
    void cartaoDeFuncionarioFicaLigadoAoUtilizadorEAtivo() {
        CartaoMagnetico cartao = CartaoMagnetico.paraFuncionario(201, 7);

        assertTrue(cartao.isDeFuncionario());
        assertEquals(7, cartao.getIdUtilizador());
        assertTrue(cartao.isAtivo());
        assertFalse(cartao.isMestre());
        assertEquals(0, cartao.getId());
    }

    @Test
    void cartaoDeClienteNaoTemDono() {
        CartaoMagnetico cartao = new CartaoMagnetico(1001, false);

        assertFalse(cartao.isDeFuncionario());
        assertNull(cartao.getIdUtilizador());
    }

    @Test
    void cartaoMestreNaoPodeTerDono() {
        assertThrows(IllegalArgumentException.class, () -> new CartaoMagnetico(1, 1, true, true, 5));
    }

    @Test
    void donoTemDeSerUmIdValido() {
        assertThrows(IllegalArgumentException.class, () -> CartaoMagnetico.paraFuncionario(201, 0));
        assertThrows(IllegalArgumentException.class, () -> CartaoMagnetico.paraFuncionario(201, -3));
    }

    @Test
    void toStringIndicaQueEDeFuncionario() {
        assertEquals("Cartão 201 (funcionário)", CartaoMagnetico.paraFuncionario(201, 7).toString());
    }
}