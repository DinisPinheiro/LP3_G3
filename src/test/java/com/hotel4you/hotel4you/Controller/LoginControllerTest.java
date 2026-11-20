package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.DAOException;
import com.hotel4you.hotel4you.DAO.UtilizadorDAOMemoria;
import com.hotel4you.hotel4you.Model.Utilizador;
import com.hotel4you.hotel4you.Model.enums.Perfil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testes do login: credenciais certas e erradas, utilizador inativo e falhas de acesso aos dados. */
class LoginControllerTest {

    private UtilizadorDAOMemoria dao;
    private Sessao sessao;
    private LoginController login;
    private Utilizador gestor;

    @BeforeEach
    void preparar() {
        dao = new UtilizadorDAOMemoria();
        sessao = new Sessao();
        login = new LoginController(dao, sessao);
        gestor = dao.inserir(Utilizador.novo("gestor", "gestor123", "Gestor Inicial", Perfil.GESTOR));
    }

    @Test
    void credenciaisCertasIniciamASessaoComOUtilizador() {
        assertTrue(login.autenticar("gestor", "gestor123"));

        assertTrue(sessao.temSessaoAtiva());
        assertSame(gestor, sessao.getUtilizadorAtual().orElseThrow());
    }

    @Test
    void cadaPerfilFicaComASuaSessao() {
        for (Perfil perfil : Perfil.values()) {
            dao.inserir(Utilizador.novo("user_" + perfil.name(), "pass_" + perfil.name(), "Nome", perfil));
        }

        for (Perfil perfil : Perfil.values()) {
            assertTrue(login.autenticar("user_" + perfil.name(), "pass_" + perfil.name()));
            assertEquals(perfil, sessao.getUtilizadorAtual().orElseThrow().getPerfil());
        }
    }

    @Test
    void usernameNaoDistingueMaiusculasEIgnoraEspacosNasPontas() {
        assertTrue(login.autenticar("  GESTOR ", "gestor123"));
        assertTrue(sessao.temSessaoAtiva());
    }

    @Test
    void passwordErradaNaoIniciaSessao() {
        assertFalse(login.autenticar("gestor", "errada"));

        assertFalse(sessao.temSessaoAtiva());
    }

    @Test
    void passwordDistingueMaiusculasEEspacos() {
        assertFalse(login.autenticar("gestor", "Gestor123"));
        assertFalse(login.autenticar("gestor", " gestor123"));
        assertFalse(sessao.temSessaoAtiva());
    }

    @Test
    void usernameInexistenteNaoIniciaSessao() {
        assertFalse(login.autenticar("ninguem", "gestor123"));

        assertFalse(sessao.temSessaoAtiva());
    }

    @Test
    void utilizadorDesativadoNaoFazLoginMesmoComACredencialCerta() {
        gestor.setAtivo(false);
        dao.atualizar(gestor);

        assertFalse(login.autenticar("gestor", "gestor123"));

        assertFalse(sessao.temSessaoAtiva());
    }

    @Test
    void utilizadorReativadoVoltaAFazerLogin() {
        gestor.setAtivo(false);
        dao.atualizar(gestor);
        gestor.setAtivo(true);
        dao.atualizar(gestor);

        assertTrue(login.autenticar("gestor", "gestor123"));
    }

    @Test
    void passwordAlteradaPassaAValerEAAntigaDeixaDeValer() {
        gestor.alterarPassword("outraPassword");
        dao.atualizar(gestor);

        assertFalse(login.autenticar("gestor", "gestor123"));
        assertTrue(login.autenticar("gestor", "outraPassword"));
    }

    @Test
    void camposVaziosOuNulosSaoRecusadosSemConsultarOsDados() {
        UtilizadorDAOMemoria queNaoDeveSerUsado = new UtilizadorDAOMemoria() {
            @Override
            public Optional<Utilizador> procurarPorUsername(String username) {
                throw new AssertionError("não devia consultar o DAO");
            }
        };
        LoginController controller = new LoginController(queNaoDeveSerUsado, sessao);

        assertFalse(controller.autenticar(null, "gestor123"));
        assertFalse(controller.autenticar("", "gestor123"));
        assertFalse(controller.autenticar("   ", "gestor123"));
        assertFalse(controller.autenticar("gestor", null));
        assertFalse(controller.autenticar("gestor", ""));
        assertFalse(sessao.temSessaoAtiva());
    }

    @Test
    void falhaDoDaoViraAcessoDadosExceptionComAMensagemOriginal() {
        DAOException causa = new DAOException("Base de dados indisponível.");
        UtilizadorDAOMemoria avariado = new UtilizadorDAOMemoria() {
            @Override
            public Optional<Utilizador> procurarPorUsername(String username) {
                throw causa;
            }
        };
        LoginController controller = new LoginController(avariado, sessao);

        AcessoDadosException erro = assertThrows(AcessoDadosException.class,
                () -> controller.autenticar("gestor", "gestor123"));

        assertEquals("Base de dados indisponível.", erro.getMessage());
        assertSame(causa, erro.getCause());
        assertFalse(sessao.temSessaoAtiva());
    }

    @Test
    void terminarSessaoFazLogout() {
        login.autenticar("gestor", "gestor123");

        login.terminarSessao();

        assertFalse(sessao.temSessaoAtiva());
        assertTrue(sessao.getUtilizadorAtual().isEmpty());
    }

    @Test
    void depoisDoLogoutOutroUtilizadorPodeEntrar() {
        dao.inserir(Utilizador.novo("rececao", "rececao123", "Rececionista", Perfil.RECECIONISTA));
        login.autenticar("gestor", "gestor123");
        login.terminarSessao();

        assertTrue(login.autenticar("rececao", "rececao123"));

        assertEquals(Perfil.RECECIONISTA, sessao.getUtilizadorAtual().orElseThrow().getPerfil());
    }

    @Test
    void construtorExigeDaoESessao() {
        assertThrows(NullPointerException.class, () -> new LoginController(null, sessao));
        assertThrows(NullPointerException.class, () -> new LoginController(dao, null));
    }
}