package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.Model.Utilizador;
import com.hotel4you.hotel4you.Model.enums.Perfil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testes da verificação de permissões por perfil (RF-03), partilhada pelos controllers. */
class PermissoesTest {

    private Sessao sessao;

    @BeforeEach
    void preparar() {
        sessao = new Sessao();
    }

    private void entrarComo(Perfil perfil, boolean ativo) {
        Utilizador utilizador = Utilizador.novo("user", "password1", "Nome", perfil);
        utilizador.setAtivo(ativo);
        sessao.iniciar(utilizador);
    }

    @Test
    void semSessaoNinguemTemPerfil() {
        assertFalse(Permissoes.temPerfil(sessao, Perfil.GESTOR));
        assertFalse(Permissoes.temPerfil(sessao, Perfil.values()));
    }

    @Test
    void temPerfilSoSeOPerfilDoUtilizadorEstiverNaLista() {
        entrarComo(Perfil.RECECIONISTA, true);

        assertTrue(Permissoes.temPerfil(sessao, Perfil.RECECIONISTA));
        assertTrue(Permissoes.temPerfil(sessao, Perfil.GESTOR, Perfil.RECECIONISTA));
        assertFalse(Permissoes.temPerfil(sessao, Perfil.GESTOR));
        assertFalse(Permissoes.temPerfil(sessao, Perfil.FORNECEDOR, Perfil.EMPRESA_RESTAURACAO));
    }

    @Test
    void semPerfisNaListaNinguemTemAcesso() {
        entrarComo(Perfil.GESTOR, true);

        assertFalse(Permissoes.temPerfil(sessao));
    }

    @Test
    void utilizadorDesativadoNaoTemPerfilMesmoComSessaoIniciada() {
        entrarComo(Perfil.GESTOR, false);

        assertFalse(Permissoes.temPerfil(sessao, Perfil.GESTOR));
    }

    @Test
    void depoisDoLogoutJaNaoTemPerfil() {
        entrarComo(Perfil.GESTOR, true);
        sessao.terminar();

        assertFalse(Permissoes.temPerfil(sessao, Perfil.GESTOR));
    }

    @Test
    void exigirPerfilDeixaPassarQuemTemPermissao() {
        entrarComo(Perfil.GESTOR, true);

        Permissoes.exigirPerfil(sessao, "Sem acesso.", Perfil.GESTOR);
        Permissoes.exigirPerfil(sessao, "Sem acesso.", Perfil.RECECIONISTA, Perfil.GESTOR);
    }

    @Test
    void exigirPerfilLancaSecurityExceptionComAMensagemPedida() {
        entrarComo(Perfil.FORNECEDOR, true);

        SecurityException erro = assertThrows(SecurityException.class,
                () -> Permissoes.exigirPerfil(sessao, "Só o gestor pode fazer isto.", Perfil.GESTOR));

        assertEquals("Só o gestor pode fazer isto.", erro.getMessage());
    }

    @Test
    void exigirPerfilRecusaSemSessaoEUtilizadorDesativado() {
        assertThrows(SecurityException.class, () -> Permissoes.exigirPerfil(sessao, "Sem acesso.", Perfil.GESTOR));

        entrarComo(Perfil.GESTOR, false);
        assertThrows(SecurityException.class, () -> Permissoes.exigirPerfil(sessao, "Sem acesso.", Perfil.GESTOR));
    }
}