package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.Model.Utilizador;
import com.hotel4you.hotel4you.Model.enums.Perfil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testes da sessão da aplicação (quem fez login). */
class SessaoTest {

    private static Utilizador utilizador(String username, Perfil perfil) {
        return Utilizador.novo(username, "password1", "Nome", perfil);
    }

    @Test
    void sessaoNovaNaoTemUtilizador() {
        Sessao sessao = new Sessao();

        assertFalse(sessao.temSessaoAtiva());
        assertTrue(sessao.getUtilizadorAtual().isEmpty());
    }

    @Test
    void iniciarGuardaOUtilizadorAutenticado() {
        Sessao sessao = new Sessao();
        Utilizador ana = utilizador("ana", Perfil.RECECIONISTA);

        sessao.iniciar(ana);

        assertTrue(sessao.temSessaoAtiva());
        assertSame(ana, sessao.getUtilizadorAtual().orElseThrow());
    }

    @Test
    void terminarLimpaASessao() {
        Sessao sessao = new Sessao();
        sessao.iniciar(utilizador("ana", Perfil.RECECIONISTA));

        sessao.terminar();

        assertFalse(sessao.temSessaoAtiva());
        assertTrue(sessao.getUtilizadorAtual().isEmpty());
    }

    @Test
    void novoLoginSubstituiOUtilizadorAnterior() {
        Sessao sessao = new Sessao();
        Utilizador ana = utilizador("ana", Perfil.RECECIONISTA);
        Utilizador rui = utilizador("rui", Perfil.GESTOR);

        sessao.iniciar(ana);
        sessao.iniciar(rui);

        assertSame(rui, sessao.getUtilizadorAtual().orElseThrow());
    }

    @Test
    void terminarSemSessaoNaoFazNada() {
        Sessao sessao = new Sessao();

        sessao.terminar();

        assertFalse(sessao.temSessaoAtiva());
    }
}