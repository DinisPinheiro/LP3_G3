package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.ClienteDAOMemoria;
import com.hotel4you.hotel4you.Model.Cliente;
import com.hotel4you.hotel4you.Model.Utilizador;
import com.hotel4you.hotel4you.Model.enums.Perfil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GestaoClientesControllerTest {

    private ClienteDAOMemoria dao;
    private Sessao sessao;
    private GestaoClientesController controller;

    @BeforeEach
    void preparar() {
        dao = new ClienteDAOMemoria();
        sessao = new Sessao();
        sessao.iniciar(Utilizador.novo("rececao", "rececao123", "Ana Rececionista", Perfil.RECECIONISTA));
        controller = new GestaoClientesController(dao, sessao);
    }

    @Test
    void registarGuardaOClienteComDocumentoNormalizado() {
        Cliente cliente = controller.registarCliente("  Maria Silva ", " pt 123 456 ", "910000000");
        assertTrue(cliente.getId() > 0);
        assertEquals("Maria Silva", cliente.getNome());
        assertEquals("PT123456", cliente.getDocumento());
        assertEquals(1, dao.listarTodos().size());
    }

    @Test
    void registarRejeitaTextosMaisLongosQueAsColunasDaBaseDeDados() {
        assertThrows(IllegalArgumentException.class, () -> controller.registarCliente("N".repeat(101), "123", ""));
        assertThrows(IllegalArgumentException.class, () -> controller.registarCliente("Maria", "9".repeat(21), ""));
        assertThrows(IllegalArgumentException.class, () -> controller.registarCliente("Maria", "123", "c".repeat(101)));
        assertTrue(dao.listarTodos().isEmpty());
        assertEquals(20, controller.registarCliente("Maria", "9".repeat(20), "").getDocumento().length());
    }

    @Test
    void contactoEOpcional() {
        Cliente cliente = controller.registarCliente("Maria Silva", "123456789", null);
        assertEquals("", cliente.getContacto());
    }

    @Test
    void registarRejeitaDocumentoRepetidoComMensagemDeAviso() {
        controller.registarCliente("Maria Silva", "pt123", "");
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> controller.registarCliente("Outra Pessoa", "PT 123", ""));
        assertTrue(erro.getMessage().contains("Já existe"));
        assertTrue(erro.getMessage().contains("Maria Silva"));
        assertEquals(1, dao.listarTodos().size());
    }

    @Test
    void registarExigeNomeEDocumento() {
        assertThrows(IllegalArgumentException.class, () -> controller.registarCliente("", "123", ""));
        assertThrows(IllegalArgumentException.class, () -> controller.registarCliente("   ", "123", ""));
        assertThrows(IllegalArgumentException.class, () -> controller.registarCliente("Maria", "", ""));
        assertThrows(IllegalArgumentException.class, () -> controller.registarCliente("Maria", null, ""));
        assertTrue(dao.listarTodos().isEmpty());
    }

    @Test
    void consultarFiltraPorNomeOuDocumentoESemPesquisaDevolveTodos() {
        controller.registarCliente("Maria Silva", "111111111", "");
        controller.registarCliente("Joao Santos", "222222222", "");
        controller.registarCliente("Ana Maria", "333333333", "");

        assertEquals(3, controller.consultarClientes("").size());
        assertEquals(3, controller.consultarClientes(null).size());
        List<Cliente> porNome = controller.consultarClientes("maria");
        assertEquals(2, porNome.size());
        assertEquals("Ana Maria", porNome.get(0).getNome());
        assertEquals(1, controller.consultarClientes("2222").size());
        assertTrue(controller.consultarClientes("zzz").isEmpty());
    }

    @Test
    void gestorTambemPodeGerirClientes() {
        sessao.iniciar(Utilizador.novo("gestor", "gestor123", "Gestor", Perfil.GESTOR));
        assertEquals("Maria", controller.registarCliente("Maria", "123", "").getNome());
    }

    @Test
    void outrosPerfisSemSessaoOuDesativadosSaoRecusados() {
        sessao.iniciar(Utilizador.novo("forn", "forn12345", "Fornecedor", Perfil.FORNECEDOR));
        assertThrows(SecurityException.class, () -> controller.registarCliente("Maria", "123", ""));
        assertThrows(SecurityException.class, () -> controller.consultarClientes(""));

        sessao.terminar();
        assertThrows(SecurityException.class, () -> controller.registarCliente("Maria", "123", ""));

        Utilizador desativado = Utilizador.novo("rec2", "rec212345", "Rec", Perfil.RECECIONISTA);
        desativado.setAtivo(false);
        sessao.iniciar(desativado);
        assertThrows(SecurityException.class, () -> controller.registarCliente("Maria", "123", ""));
    }

    @Test
    void normalizarDocumentoRemoveEspacosEPoeMaiusculas() {
        assertEquals("AB123", GestaoClientesController.normalizarDocumento(" ab 1 2 3 "));
        assertEquals("", GestaoClientesController.normalizarDocumento(null));
    }
}
