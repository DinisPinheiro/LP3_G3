package com.hotel4you.hotel4you.Model;

import com.hotel4you.hotel4you.Model.enums.Perfil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testes do utilizador: password só em hash, perfis e permissões por piso. */
class UtilizadorTest {

    private static Utilizador com(Perfil perfil) {
        return Utilizador.novo("user", "password1", "Utilizador de Teste", perfil);
    }

    @Test
    void novoGuardaApenasHashEVerificaPassword() {
        Utilizador gestor = Utilizador.novo("gestor", "gestor123", "Gestor Inicial", Perfil.GESTOR);

        assertEquals(0, gestor.getId(), "ainda não foi guardado");
        assertTrue(gestor.isAtivo());
        assertNotEquals("gestor123", gestor.getPasswordHash());
        assertFalse(gestor.getPasswordHash().contains("gestor123"));
        assertTrue(gestor.verificarPassword("gestor123"));
        assertFalse(gestor.verificarPassword("Gestor123"));
        assertFalse(gestor.verificarPassword(null));
    }

    @Test
    void aceitaOsQuatroPerfis() {
        for (Perfil perfil : Perfil.values()) {
            assertEquals(perfil, com(perfil).getPerfil());
        }
        assertEquals(4, Perfil.values().length);
    }

    @Test
    void rejeitaDadosObrigatoriosInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> Utilizador.novo(null, "p", "Nome", Perfil.GESTOR));
        assertThrows(IllegalArgumentException.class, () -> Utilizador.novo("  ", "p", "Nome", Perfil.GESTOR));
        assertThrows(IllegalArgumentException.class, () -> Utilizador.novo("u", "", "Nome", Perfil.GESTOR));
        assertThrows(IllegalArgumentException.class, () -> Utilizador.novo("u", null, "Nome", Perfil.GESTOR));
        assertThrows(IllegalArgumentException.class, () -> Utilizador.novo("u", "p", " ", Perfil.GESTOR));
        assertThrows(NullPointerException.class, () -> Utilizador.novo("u", "p", "Nome", null));
        assertThrows(IllegalArgumentException.class, () -> new Utilizador(1, "u", " ", "Nome", Perfil.GESTOR, true));
    }

    @Test
    void usernameENomeGuardamSemEspacosNasPontas() {
        Utilizador utilizador = Utilizador.novo("  ana  ", "p", "  Ana Silva ", Perfil.RECECIONISTA);

        assertEquals("ana", utilizador.getUsername());
        assertEquals("Ana Silva", utilizador.getNome());
    }

    @Test
    void alterarPasswordSubstituiHashSemGuardarPasswordEmClaro() {
        Utilizador utilizador = com(Perfil.RECECIONISTA);
        String hashAntigo = utilizador.getPasswordHash();

        utilizador.alterarPassword("novaPassword");

        assertNotEquals(hashAntigo, utilizador.getPasswordHash());
        assertFalse(utilizador.getPasswordHash().contains("novaPassword"));
        assertTrue(utilizador.verificarPassword("novaPassword"));
        assertFalse(utilizador.verificarPassword("password1"), "a antiga deixa de funcionar");
    }

    @Test
    void alterarPasswordParaVaziaFalhaEMantemAAntiga() {
        Utilizador utilizador = com(Perfil.GESTOR);

        assertThrows(IllegalArgumentException.class, () -> utilizador.alterarPassword(""));

        assertTrue(utilizador.verificarPassword("password1"));
    }

    @Test
    void setNomeValidaEPerfilNaoPodeSerNulo() {
        Utilizador utilizador = com(Perfil.GESTOR);

        utilizador.setNome("Novo Nome");
        assertEquals("Novo Nome", utilizador.getNome());
        assertThrows(IllegalArgumentException.class, () -> utilizador.setNome(" "));
        assertEquals("Novo Nome", utilizador.getNome());

        utilizador.setPerfil(Perfil.FORNECEDOR);
        assertEquals(Perfil.FORNECEDOR, utilizador.getPerfil());
        assertThrows(NullPointerException.class, () -> utilizador.setPerfil(null));
        assertEquals(Perfil.FORNECEDOR, utilizador.getPerfil());
    }

    @Test
    void utilizadorPodeSerDesativadoEReativado() {
        Utilizador utilizador = com(Perfil.GESTOR);

        utilizador.setAtivo(false);
        assertFalse(utilizador.isAtivo());
        utilizador.setAtivo(true);
        assertTrue(utilizador.isAtivo());
    }

    @Test
    void gestorAcedeATodosOsPisos() {
        Utilizador gestor = com(Perfil.GESTOR);
        for (int piso = -1; piso <= 3; piso++) {
            assertTrue(gestor.podeAcederAoPiso(piso), "piso " + piso);
        }
    }

    @Test
    void rececionistaSoAcedeAosPisos0e1() {
        Utilizador rececionista = com(Perfil.RECECIONISTA);

        assertTrue(rececionista.podeAcederAoPiso(0));
        assertTrue(rececionista.podeAcederAoPiso(1));
        assertFalse(rececionista.podeAcederAoPiso(-1));
        assertFalse(rececionista.podeAcederAoPiso(2));
        assertFalse(rececionista.podeAcederAoPiso(3));
    }

    @Test
    void empresaDeRestauracaoSoAcedeAoPiso1() {
        Utilizador restauracao = com(Perfil.EMPRESA_RESTAURACAO);

        assertTrue(restauracao.podeAcederAoPiso(1));
        for (int piso : new int[]{-1, 0, 2, 3}) {
            assertFalse(restauracao.podeAcederAoPiso(piso), "piso " + piso);
        }
    }

    @Test
    void fornecedorSoAcedeAoArmazemDoPisoMenos1() {
        Utilizador fornecedor = com(Perfil.FORNECEDOR);

        assertTrue(fornecedor.podeAcederAoPiso(-1));
        for (int piso : new int[]{0, 1, 2, 3}) {
            assertFalse(fornecedor.podeAcederAoPiso(piso), "piso " + piso);
        }
    }

    @Test
    void ninguemAcedeAPisosQueNaoExistem() {
        for (Perfil perfil : Perfil.values()) {
            Utilizador utilizador = com(perfil);
            assertFalse(utilizador.podeAcederAoPiso(-2), perfil + " piso -2");
            assertFalse(utilizador.podeAcederAoPiso(4), perfil + " piso 4");
        }
    }

    @Test
    void soOGestorPodeApagarReservas() {
        assertTrue(com(Perfil.GESTOR).podeApagarReservas());
        assertFalse(com(Perfil.RECECIONISTA).podeApagarReservas());
        assertFalse(com(Perfil.EMPRESA_RESTAURACAO).podeApagarReservas());
        assertFalse(com(Perfil.FORNECEDOR).podeApagarReservas());
    }

    @Test
    void toStringNaoMostraAPasswordNemOHash() {
        Utilizador utilizador = com(Perfil.GESTOR);

        assertFalse(utilizador.toString().contains(utilizador.getPasswordHash()));
        assertFalse(utilizador.toString().contains("password1"));
    }

    @Test
    void perfisTemNomeParaMostrarAoUtilizador() {
        assertEquals("Rececionista", Perfil.RECECIONISTA.getNome());
        assertEquals("Gestor", Perfil.GESTOR.getNome());
        assertEquals("Empresa de restauração", Perfil.EMPRESA_RESTAURACAO.getNome());
        assertEquals("Fornecedor", Perfil.FORNECEDOR.getNome());
    }
}