package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.EquipamentoDAOMemoria;
import com.hotel4you.hotel4you.DAO.QuartoDAOMemoria;
import com.hotel4you.hotel4you.Model.Equipamento;
import com.hotel4you.hotel4you.Model.Quarto;
import com.hotel4you.hotel4you.Model.Utilizador;
import com.hotel4you.hotel4you.Model.enums.Perfil;
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

class GestaoQuartosControllerTest {

    private QuartoDAOMemoria quartos;
    private EquipamentoDAOMemoria equipamentos;
    private Sessao sessao;
    private GestaoQuartosController controller;
    private Equipamento toalhas;
    private Equipamento secador;

    @BeforeEach
    void preparar() {
        quartos = new QuartoDAOMemoria();
        equipamentos = new EquipamentoDAOMemoria();
        sessao = new Sessao();
        sessao.iniciar(Utilizador.novo("gestor", "gestor123", "Gestor", Perfil.GESTOR));
        controller = new GestaoQuartosController(quartos, equipamentos, sessao);
        toalhas = equipamentos.inserir(new Equipamento(0, "Toalhas"));
        secador = equipamentos.inserir(new Equipamento(0, "Secador de cabelo"));
    }

    private Quarto criarDuplo(int numero) {
        return controller.criarQuarto(numero, "", TipoQuarto.DUPLO, new BigDecimal("80"), Vista.NENHUMA, List.of());
    }

    @Test
    void criarQuartoGuardaDadosPisoEEquipamentos() {
        Quarto quarto = controller.criarQuarto(205, "Ala norte", TipoQuarto.DUPLO, new BigDecimal("80"),
                Vista.MAR, List.of(toalhas));

        assertTrue(quarto.getId() > 0);
        assertEquals(2, quarto.getPiso());
        assertEquals(new BigDecimal("80.00"), quarto.getPrecoBase());
        assertEquals(Vista.MAR, quarto.getVista());
        assertTrue(quarto.isAtivo());
        assertEquals(1, quarto.getEquipamentos().size());
        assertEquals(new BigDecimal("80.00").add(Vista.MAR.getSuplemento()), quarto.precoNoite());
        assertEquals(1, controller.consultarQuartos().size());
    }

    @Test
    void numerosForaDosPisos2e3SaoRejeitados() {
        for (int numero : new int[]{0, 5, 100, 101, 200, 211, 311, 401, 1005}) {
            assertThrows(IllegalArgumentException.class, () -> criarDuplo(numero), "número " + numero);
        }
        assertTrue(controller.consultarQuartos().isEmpty());
    }

    @Test
    void numerosDosPisos2e3SaoAceites() {
        assertEquals(2, criarDuplo(201).getPiso());
        assertEquals(2, criarDuplo(210).getPiso());
        assertEquals(3, criarDuplo(301).getPiso());
        assertEquals(3, criarDuplo(310).getPiso());
    }

    @Test
    void nuncaHaMaisDe10QuartosPorPiso() {
        for (int numero = 201; numero <= 210; numero++) {
            criarDuplo(numero);
        }
        assertThrows(IllegalArgumentException.class, () -> criarDuplo(211));
        assertThrows(IllegalArgumentException.class, () -> criarDuplo(205));
    }

    @Test
    void numeroRepetidoEMensagemClara() {
        criarDuplo(205);
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> criarDuplo(205));
        assertTrue(erro.getMessage().contains("205"));
    }

    @Test
    void dadosObrigatoriosEPrecoNaoNegativo() {
        assertThrows(IllegalArgumentException.class, () -> controller.criarQuarto(
                205, "", null, new BigDecimal("80"), Vista.NENHUMA, List.of()));
        assertThrows(IllegalArgumentException.class, () -> controller.criarQuarto(
                205, "", TipoQuarto.DUPLO, new BigDecimal("80"), null, List.of()));
        assertThrows(IllegalArgumentException.class, () -> controller.criarQuarto(
                205, "", TipoQuarto.DUPLO, null, Vista.NENHUMA, List.of()));
        assertThrows(IllegalArgumentException.class, () -> controller.criarQuarto(
                205, "", TipoQuarto.DUPLO, new BigDecimal("-1"), Vista.NENHUMA, List.of()));
        assertTrue(controller.consultarQuartos().isEmpty());
    }

    @Test
    void textosEPrecosMaiorQueOsDaBaseDeDadosSaoRejeitados() {
        assertThrows(IllegalArgumentException.class, () -> controller.criarQuarto(
                205, "L".repeat(101), TipoQuarto.DUPLO, new BigDecimal("80"), Vista.NENHUMA, List.of()));
        assertThrows(IllegalArgumentException.class, () -> controller.criarQuarto(
                205, "", TipoQuarto.DUPLO, new BigDecimal("100000000"), Vista.NENHUMA, List.of()));
        assertThrows(IllegalArgumentException.class, () -> controller.criarEquipamento("E".repeat(61)));
        assertEquals(new BigDecimal("99999999.99"), controller.criarQuarto(
                205, "L".repeat(100), TipoQuarto.DUPLO, new BigDecimal("99999999.99"), Vista.NENHUMA, List.of()).getPrecoBase());
    }

    @Test
    void equipamentoForaDoCatalogoERejeitadoERepetidosSaoAgrupados() {
        Equipamento fantasma = new Equipamento(99, "Fantasma");
        assertThrows(IllegalArgumentException.class, () -> controller.criarQuarto(
                205, "", TipoQuarto.DUPLO, new BigDecimal("80"), Vista.NENHUMA, List.of(fantasma)));

        Quarto quarto = controller.criarQuarto(205, "", TipoQuarto.DUPLO, new BigDecimal("80"),
                Vista.NENHUMA, List.of(toalhas, toalhas, secador));
        assertEquals(2, quarto.getEquipamentos().size());
    }

    @Test
    void editarAlteraOsDadosEMantemNumeroEPiso() {
        Quarto criado = controller.criarQuarto(205, "", TipoQuarto.DUPLO, new BigDecimal("80"),
                Vista.NENHUMA, List.of(toalhas));

        controller.editarQuarto(criado.getId(), "Ala sul", TipoQuarto.FAMILIA, new BigDecimal("150.5"),
                Vista.MONTANHA, false, List.of(secador));

        Quarto guardado = quartos.procurarPorId(criado.getId()).orElseThrow();
        assertEquals(205, guardado.getNumero());
        assertEquals(2, guardado.getPiso());
        assertEquals("Ala sul", guardado.getLocal());
        assertEquals(TipoQuarto.FAMILIA, guardado.getTipo());
        assertEquals(new BigDecimal("150.50"), guardado.getPrecoBase());
        assertEquals(Vista.MONTANHA, guardado.getVista());
        assertFalse(guardado.isAtivo());
        assertEquals(1, guardado.getEquipamentos().size());
        assertEquals("Secador de cabelo", guardado.getEquipamentos().get(0).getNome());
    }

    @Test
    void editarComDadosInvalidosNaoAlteraOQuarto() {
        Quarto criado = controller.criarQuarto(205, "", TipoQuarto.DUPLO, new BigDecimal("80"),
                Vista.NENHUMA, List.of(toalhas));
        assertThrows(IllegalArgumentException.class, () -> controller.editarQuarto(
                criado.getId(), "", TipoQuarto.FAMILIA, new BigDecimal("-5"), Vista.MAR, true, List.of()));
        assertEquals(TipoQuarto.DUPLO, criado.getTipo());
        assertEquals(new BigDecimal("80.00"), criado.getPrecoBase());
        assertEquals(1, criado.getEquipamentos().size());
    }

    @Test
    void editarQuartoInexistenteFalha() {
        assertThrows(IllegalArgumentException.class, () -> controller.editarQuarto(
                99, "", TipoQuarto.DUPLO, new BigDecimal("80"), Vista.NENHUMA, true, List.of()));
    }

    @Test
    void criarEquipamentoNovoRejeitaVazioERepetido() {
        Equipamento novo = controller.criarEquipamento("  Frigobar ");
        assertEquals("Frigobar", novo.getNome());
        assertTrue(controller.consultarEquipamentos().size() >= 3);
        assertThrows(IllegalArgumentException.class, () -> controller.criarEquipamento("frigobar"));
        assertThrows(IllegalArgumentException.class, () -> controller.criarEquipamento("   "));
    }

    @Test
    void precoPorNoiteESomaDoPrecoBaseEDoSuplementoDaVista() {
        BigDecimal base = new BigDecimal("60.00");
        assertEquals(base.add(Vista.MAR.getSuplemento()), controller.calcularPrecoNoite(base, Vista.MAR));
        assertEquals(base.add(Vista.MONTANHA.getSuplemento()), controller.calcularPrecoNoite(base, Vista.MONTANHA));
        assertEquals(base, controller.calcularPrecoNoite(base, Vista.NENHUMA));
    }

    @Test
    void pisoDoNumeroDevolveOPisoCorreto() {
        assertEquals(2, GestaoQuartosController.pisoDoNumero(205));
        assertEquals(3, GestaoQuartosController.pisoDoNumero(310));
    }

    @Test
    void apenasOGestorAcedeAConfiguracaoDeQuartos() {
        Quarto criado = criarDuplo(205);
        sessao.iniciar(Utilizador.novo("rececao", "rececao123", "Ana", Perfil.RECECIONISTA));

        assertThrows(SecurityException.class, () -> controller.consultarQuartos());
        assertThrows(SecurityException.class, () -> controller.consultarEquipamentos());
        assertThrows(SecurityException.class, () -> criarDuplo(206));
        assertThrows(SecurityException.class, () -> controller.editarQuarto(
                criado.getId(), "", TipoQuarto.DUPLO, new BigDecimal("80"), Vista.NENHUMA, true, List.of()));
        assertThrows(SecurityException.class, () -> controller.criarEquipamento("Frigobar"));

        sessao.terminar();
        assertThrows(SecurityException.class, () -> controller.consultarQuartos());
    }

    @Test
    void gestorDesativadoNaoAcede() {
        Utilizador desativado = Utilizador.novo("gestor2", "gestor2123", "Gestor 2", Perfil.GESTOR);
        desativado.setAtivo(false);
        sessao.iniciar(desativado);
        assertThrows(SecurityException.class, () -> controller.consultarQuartos());
    }
}
