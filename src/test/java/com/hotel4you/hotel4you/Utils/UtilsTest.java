package com.hotel4you.hotel4you.Utils;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilsTest {

    @Test
    void estaPreenchidoRejeitaNuloVazioEEspacos() {
        assertFalse(Utils.estaPreenchido(null));
        assertFalse(Utils.estaPreenchido(""));
        assertFalse(Utils.estaPreenchido("   "));
        assertTrue(Utils.estaPreenchido(" Ana "));
    }

    @Test
    void eCancelamentoReconheceSoOZero() {
        assertTrue(Utils.eCancelamento("0"));
        assertTrue(Utils.eCancelamento(" 0 "));
        assertFalse(Utils.eCancelamento("00"));
        assertFalse(Utils.eCancelamento("10"));
        assertFalse(Utils.eCancelamento(""));
        assertFalse(Utils.eCancelamento(null));
    }

    @Test
    void converterInteiroAceitaInteirosEIgnoraEspacos() {
        assertEquals(7, Utils.converterInteiro(" 7 ").orElseThrow());
        assertEquals(-3, Utils.converterInteiro("-3").orElseThrow());
    }

    @Test
    void converterInteiroRejeitaTextoInvalido() {
        assertTrue(Utils.converterInteiro("abc").isEmpty());
        assertTrue(Utils.converterInteiro("7,5").isEmpty());
        assertTrue(Utils.converterInteiro("").isEmpty());
        assertTrue(Utils.converterInteiro(null).isEmpty());
    }

    @Test
    void converterInteiroComIntervaloRespeitaOsLimites() {
        assertEquals(3, Utils.converterInteiro(" 3 ", 1, 5).orElseThrow());
        assertEquals(1, Utils.converterInteiro("1", 1, 5).orElseThrow());
        assertEquals(5, Utils.converterInteiro("5", 1, 5).orElseThrow());
        assertTrue(Utils.converterInteiro("0", 1, 5).isEmpty());
        assertTrue(Utils.converterInteiro("6", 1, 5).isEmpty());
        assertTrue(Utils.converterInteiro("abc", 1, 5).isEmpty());
    }

    @Test
    void converterDecimalAceitaVirgulaEPonto() {
        assertEquals(new BigDecimal("60.50"), Utils.converterDecimal("60,50").orElseThrow());
        assertEquals(new BigDecimal("60.5"), Utils.converterDecimal("60.5").orElseThrow());
        assertEquals(new BigDecimal("7"), Utils.converterDecimal(" 7 ").orElseThrow());
        assertEquals(new BigDecimal("-3.5"), Utils.converterDecimal("-3,5").orElseThrow());
    }

    @Test
    void converterDecimalRejeitaTextoInvalido() {
        assertTrue(Utils.converterDecimal("abc").isEmpty());
        assertTrue(Utils.converterDecimal("1e3").isEmpty());
        assertTrue(Utils.converterDecimal("1.000,50").isEmpty());
        assertTrue(Utils.converterDecimal("6,").isEmpty());
        assertTrue(Utils.converterDecimal(",5").isEmpty());
        assertTrue(Utils.converterDecimal("").isEmpty());
        assertTrue(Utils.converterDecimal(null).isEmpty());
    }

    @Test
    void converterDataAceitaDataValida() {
        assertEquals(LocalDate.of(2026, 3, 15), Utils.converterData("15/03/2026").orElseThrow());
        assertEquals(LocalDate.of(2028, 2, 29), Utils.converterData("29/02/2028").orElseThrow());
    }

    @Test
    void converterDataRejeitaDatasInvalidas() {
        assertTrue(Utils.converterData("31/02/2026").isEmpty());
        assertTrue(Utils.converterData("29/02/2027").isEmpty());
        assertTrue(Utils.converterData("1/1/2026").isEmpty());
        assertTrue(Utils.converterData("2026-03-15").isEmpty());
        assertTrue(Utils.converterData("abc").isEmpty());
        assertTrue(Utils.converterData("").isEmpty());
        assertTrue(Utils.converterData(null).isEmpty());
    }

    @Test
    void formatarPrecoUsaVirgulaDecimal() {
        assertTrue(Utils.formatarPreco(new BigDecimal("80")).contains("80,00"));
    }
}
