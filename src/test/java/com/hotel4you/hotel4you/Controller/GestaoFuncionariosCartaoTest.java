package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.CartaoDAOMemoria;
import com.hotel4you.hotel4you.DAO.UtilizadorDAOMemoria;
import com.hotel4you.hotel4you.Model.CartaoMagnetico;
import com.hotel4you.hotel4you.Model.Utilizador;
import com.hotel4you.hotel4you.Model.enums.Perfil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes da emissão do cartão próprio ao criar um funcionário (S6-06).
 */
class GestaoFuncionariosCartaoTest {

    private UtilizadorDAOMemoria utilizadores;
    private CartaoDAOMemoria cartoes;
    private GestaoFuncionariosController controller;

    @BeforeEach
    void preparar() {
        utilizadores = new UtilizadorDAOMemoria();
        cartoes = new CartaoDAOMemoria();
        Sessao sessao = new Sessao();
        sessao.iniciar(new Utilizador(1, "gestor", "hash", "Gestor", Perfil.GESTOR, true));
        controller = new GestaoFuncionariosController(utilizadores, cartoes, sessao);
    }

    @Test
    void rececionistaRecebeUmCartaoProprio() {
        int numero = controller.criarFuncionario("Ana", "ana", "abc123456", Perfil.RECECIONISTA);

        CartaoMagnetico cartao = cartoes.procurarPorNumero(numero).orElseThrow();
        Utilizador ana = utilizadores.procurarPorUsername("ana").orElseThrow();

        assertTrue(numero >= 1001);
        assertTrue(cartao.isDeFuncionario());
        assertEquals(ana.getId(), cartao.getIdUtilizador());
        assertTrue(cartao.isAtivo());
    }

    @Test
    void gestorERestauracaoTambemRecebemCartao() {
        int gestor = controller.criarFuncionario("Rui", "rui", "abc123456", Perfil.GESTOR);
        int restauracao = controller.criarFuncionario("Cozinha", "coz", "abc123456", Perfil.EMPRESA_RESTAURACAO);

        assertTrue(gestor > 0);
        assertTrue(restauracao > 0);
        assertTrue(gestor != restauracao);
    }

    @Test
    void fornecedorNaoRecebeCartao() {
        int numero = controller.criarFuncionario("Forn", "forn", "abc123456", Perfil.FORNECEDOR);

        assertEquals(0, numero);
        assertTrue(cartoes.listarTodos().isEmpty());
    }

    @Test
    void semAcessoAosCartoesNaoEmiteCartao() {
        Sessao sessao = new Sessao();
        sessao.iniciar(new Utilizador(1, "gestor", "hash", "Gestor", Perfil.GESTOR, true));
        GestaoFuncionariosController semCartoes = new GestaoFuncionariosController(new UtilizadorDAOMemoria(), sessao);

        assertEquals(0, semCartoes.criarFuncionario("Ana", "ana", "abc123456", Perfil.RECECIONISTA));
    }
}