package com.hotel4you.hotel4you.Utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilsTest {

    private void teclado(String texto) {
        Utils.setScanner(new Scanner(texto));
    }

    @AfterEach
    void repor() {
        Utils.setScanner(new Scanner(System.in));
    }

    @Test
    void lerCampoIgnoraVaziosERemoveEspacos() {
        teclado("\n   \n  Ana  \n");
        assertEquals("Ana", Utils.lerCampo("Nome: "));
    }

    @Test
    void lerCampoZeroCancela() {
        teclado("0\n");
        assertThrows(OperacaoCanceladaException.class, () -> Utils.lerCampo("Nome: "));
    }

    @Test
    void lerInteiroRepeteAteSerValido() {
        teclado("abc\n9\n-1\n3\n");
        assertEquals(3, Utils.lerInteiro("Opção: ", 1, 5));
    }

    @Test
    void lerInteiroAceitaZeroQuandoEstaNoIntervalo() {
        teclado("0\n");
        assertEquals(0, Utils.lerInteiro("Opção: ", 0, 5));
    }

    @Test
    void lerInteiroOuCancelarZeroCancela() {
        teclado("0\n");
        assertThrows(OperacaoCanceladaException.class, () -> Utils.lerInteiroOuCancelar("Quartos: ", 1, 5));
    }

    @Test
    void lerDataRejeitaInvalidasEAceitaValida() {
        teclado("31/02/2026\n1/1/2026\nabc\n15/03/2026\n");
        assertEquals(LocalDate.of(2026, 3, 15), Utils.lerData("Entrada"));
    }

    @Test
    void lerDataZeroCancela() {
        teclado("0\n");
        assertThrows(OperacaoCanceladaException.class, () -> Utils.lerData("Entrada"));
    }

    @Test
    void converterInteiroEDataSemInteracao() {
        assertEquals(7, Utils.converterInteiro(" 7 ").orElseThrow());
        assertTrue(Utils.converterInteiro("7,5").isEmpty());
        assertTrue(Utils.converterInteiro(null).isEmpty());
        assertTrue(Utils.converterData("29/02/2027").isEmpty());
        assertFalse(Utils.converterData("29/02/2028").isEmpty());
    }
}
