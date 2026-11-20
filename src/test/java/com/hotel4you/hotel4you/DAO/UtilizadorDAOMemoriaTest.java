package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.Model.Utilizador;
import com.hotel4you.hotel4you.Model.enums.Perfil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testes do DAO de utilizadores em memória (o mesmo contrato do DAO de SQL Server). */
class UtilizadorDAOMemoriaTest {

    private UtilizadorDAOMemoria dao;

    @BeforeEach
    void preparar() {
        dao = new UtilizadorDAOMemoria();
    }

    private Utilizador inserir(String username, Perfil perfil) {
        return dao.inserir(Utilizador.novo(username, "password1", "Nome de " + username, perfil));
    }

    @Test
    void insereAtribuiIdEPermiteConsultarPorIdUsernameEListagem() {
        Utilizador ana = inserir("ana", Perfil.RECECIONISTA);
        Utilizador rui = inserir("rui", Perfil.GESTOR);

        assertEquals(1, ana.getId());
        assertEquals(2, rui.getId());
        assertSame(ana, dao.procurarPorId(1).orElseThrow());
        assertSame(rui, dao.procurarPorUsername("rui").orElseThrow());
        assertEquals(List.of(ana, rui), dao.listarTodos());
    }

    @Test
    void procurarPorUsernameNaoDistingueMaiusculasEIgnoraEspacos() {
        Utilizador ana = inserir("Ana", Perfil.RECECIONISTA);

        assertSame(ana, dao.procurarPorUsername("ana").orElseThrow());
        assertSame(ana, dao.procurarPorUsername("ANA").orElseThrow());
        assertSame(ana, dao.procurarPorUsername("  ana ").orElseThrow());
    }

    @Test
    void procurarDevolveVazioSeNaoExisteOuSeOUsernameForNulo() {
        inserir("ana", Perfil.RECECIONISTA);

        assertTrue(dao.procurarPorUsername("outro").isEmpty());
        assertTrue(dao.procurarPorUsername(null).isEmpty());
        assertTrue(dao.procurarPorId(99).isEmpty());
    }

    @Test
    void rejeitaUsernameDuplicadoSemDistinguirMaiusculas() {
        inserir("ana", Perfil.RECECIONISTA);

        DAOException erro = assertThrows(DAOException.class, () -> inserir("ANA", Perfil.GESTOR));

        assertTrue(erro.getMessage().contains("ANA"));
        assertEquals(1, dao.listarTodos().size(), "o duplicado não fica guardado");
    }

    @Test
    void atualizaRemoveEConsultaUtilizador() {
        Utilizador ana = inserir("ana", Perfil.RECECIONISTA);

        ana.setNome("Ana Maria");
        ana.setPerfil(Perfil.GESTOR);
        ana.setAtivo(false);
        ana.alterarPassword("novaPassword");
        dao.atualizar(ana);

        Utilizador lido = dao.procurarPorId(ana.getId()).orElseThrow();
        assertEquals("Ana Maria", lido.getNome());
        assertEquals(Perfil.GESTOR, lido.getPerfil());
        assertFalse(lido.isAtivo());
        assertTrue(lido.verificarPassword("novaPassword"));

        dao.remover(ana.getId());
        assertTrue(dao.procurarPorId(ana.getId()).isEmpty());
        assertTrue(dao.listarTodos().isEmpty());
    }

    @Test
    void falhaAoAtualizarOuRemoverUtilizadorInexistente() {
        Utilizador semId = Utilizador.novo("ana", "password1", "Ana", Perfil.RECECIONISTA);
        semId.setId(42);

        assertThrows(DAOException.class, () -> dao.atualizar(semId));
        assertThrows(DAOException.class, () -> dao.remover(42));
    }

    @Test
    void usernameLibertadoDepoisDeRemoverPodeSerReutilizado() {
        Utilizador ana = inserir("ana", Perfil.RECECIONISTA);
        dao.remover(ana.getId());

        Utilizador nova = inserir("ana", Perfil.GESTOR);

        assertEquals(Perfil.GESTOR, dao.procurarPorUsername("ana").orElseThrow().getPerfil());
        assertNotEquals(ana.getId(), nova.getId(), "o id não é reutilizado");
    }

    @Test
    void listarTodosDevolveUmaCopia() {
        inserir("ana", Perfil.RECECIONISTA);

        dao.listarTodos().clear();

        assertEquals(1, dao.listarTodos().size());
    }
}