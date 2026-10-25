package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.Model.OpcaoMenu;
import com.hotel4you.hotel4you.Model.Perfil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuControllerTest {

    private final MenuController controller = new MenuController();

    @Test
    void rececionistaNaoVeOpcoesDeGestor() {
        List<OpcaoMenu> opcoes = controller.obterOpcoes(Perfil.RECECIONISTA);
        assertTrue(opcoes.contains(OpcaoMenu.REGISTAR_CLIENTE));
        assertFalse(opcoes.contains(OpcaoMenu.GERIR_FUNCIONARIOS));
        assertFalse(opcoes.contains(OpcaoMenu.LISTAGEM_POR_CARTAO));
    }

    @Test
    void gestorVeOpcoesDoRececionistaEAsSuas() {
        List<OpcaoMenu> gestor = controller.obterOpcoes(Perfil.GESTOR);
        assertTrue(gestor.containsAll(controller.obterOpcoes(Perfil.RECECIONISTA)));
        assertTrue(gestor.contains(OpcaoMenu.GERIR_FUNCIONARIOS));
    }

    @Test
    void empresaRestauracaoSoVeCozinhaERestaurante() {
        assertEquals(List.of(OpcaoMenu.COZINHA_RESTAURANTE),
                controller.obterOpcoes(Perfil.EMPRESA_RESTAURACAO));
    }

    @Test
    void fornecedorSoVeAsSuasOpcoes() {
        assertEquals(List.of(OpcaoMenu.ENVIAR_ENTREGA_XML),
                controller.obterOpcoes(Perfil.FORNECEDOR));
    }

    @Test
    void temAcessoRecusaOpcaoDeOutroPerfil() {
        assertFalse(controller.temAcesso(Perfil.RECECIONISTA, OpcaoMenu.APAGAR_RESERVAS));
        assertTrue(controller.temAcesso(Perfil.GESTOR, OpcaoMenu.APAGAR_RESERVAS));
    }

    @Test
    void perfilNuloLancaExcecao() {
        assertThrows(NullPointerException.class, () -> controller.obterOpcoes(null));
    }
}
