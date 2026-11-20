package com.hotel4you.hotel4you.Utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testes das constantes da estrutura física do hotel (enunciado, secção 2). */
class ConstantesTest {

    @Test
    void osPisosValidosVaoDoMenos1Ao3() {
        for (int piso = -1; piso <= 3; piso++) {
            assertTrue(Constantes.pisoValido(piso), "piso " + piso);
        }
        assertFalse(Constantes.pisoValido(-2));
        assertFalse(Constantes.pisoValido(4));
        assertFalse(Constantes.pisoValido(Integer.MIN_VALUE));
        assertFalse(Constantes.pisoValido(Integer.MAX_VALUE));
    }

    @Test
    void soOsPisos2e3SaoDeDormidas() {
        assertTrue(Constantes.pisoDeDormidas(2));
        assertTrue(Constantes.pisoDeDormidas(3));
        assertFalse(Constantes.pisoDeDormidas(-1));
        assertFalse(Constantes.pisoDeDormidas(0));
        assertFalse(Constantes.pisoDeDormidas(1));
        assertFalse(Constantes.pisoDeDormidas(4));
    }

    @Test
    void oHotelTem20QuartosRepartidosPorTresTipos() {
        assertEquals(7, Constantes.TOTAL_INDIVIDUAIS);
        assertEquals(10, Constantes.TOTAL_DUPLOS);
        assertEquals(3, Constantes.TOTAL_FAMILIA);
        assertEquals(20, Constantes.TOTAL_QUARTOS);
    }

    @Test
    void osDoisPisosDeDormidasTemOTotalDeQuartos() {
        int pisosDeDormidas = Constantes.ULTIMO_PISO_DORMIDAS - Constantes.PRIMEIRO_PISO_DORMIDAS + 1;
        assertEquals(Constantes.TOTAL_QUARTOS, pisosDeDormidas * Constantes.QUARTOS_POR_PISO);
    }
}