package com.hotel4you.hotel4you.Model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes unitários dos locais do hotel.
 */
class LocalHotelTest {

    @Test
    void localComPisoGuardaCodigoEmMaiusculasENome() {
        LocalHotel piscina = new LocalHotel(3, " piscina ", "Piscina", 1);

        assertEquals("PISCINA", piscina.getCodigo());
        assertEquals("Piscina", piscina.getNome());
        assertEquals(1, piscina.getPiso());
        assertFalse(piscina.isQuarto());
    }

    @Test
    void quartoNaoTemPisoProprio() {
        LocalHotel quarto = new LocalHotel(1, LocalHotel.QUARTO, "Quarto", 2);

        assertTrue(quarto.isQuarto());
        assertNull(quarto.getPiso());
    }

    @Test
    void localQueNaoEQuartoPrecisaDeUmPisoValido() {
        assertThrows(IllegalArgumentException.class, () -> new LocalHotel(0, "BAR", "Bar", null));
        assertThrows(IllegalArgumentException.class, () -> new LocalHotel(0, "BAR", "Bar", 7));
    }

    @Test
    void codigoENomeSaoObrigatorios() {
        assertThrows(IllegalArgumentException.class, () -> new LocalHotel(0, " ", "Bar", 0));
        assertThrows(IllegalArgumentException.class, () -> new LocalHotel(0, "BAR", null, 0));
    }

    @Test
    void doisLocaisComOMesmoCodigoSaoIguais() {
        assertEquals(new LocalHotel(1, "BAR", "Bar", 0), new LocalHotel(9, "bar", "Outro nome", 0));
    }

    @Test
    void toStringMostraONome() {
        assertEquals("Receção", new LocalHotel(5, LocalHotel.RECECAO, "Receção", 0).toString());
    }
}