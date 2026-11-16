package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.DAOException;
import com.hotel4you.hotel4you.DAO.FornecedorDAOMemoria;
import com.hotel4you.hotel4you.DAO.UtilizadorDAOMemoria;
import com.hotel4you.hotel4you.Model.Fornecedor;
import com.hotel4you.hotel4you.Model.Utilizador;
import com.hotel4you.hotel4you.Model.enums.Perfil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testes da S5-02: o gestor cria o fornecedor (código, nome, morada, código postal, cidade) e a sua conta. */
class GestaoFornecedoresControllerTest {

    private UtilizadorDAOMemoria utilizadores;
    private FornecedorDAOMemoria fornecedores;
    private Sessao sessao;
    private GestaoFornecedoresController controller;

    @BeforeEach
    void preparar() {
        utilizadores = new UtilizadorDAOMemoria();
        fornecedores = new FornecedorDAOMemoria();
        sessao = new Sessao();
        sessao.iniciar(Utilizador.novo("gestor", "gestor123", "Gestor", Perfil.GESTOR));
        controller = new GestaoFornecedoresController(fornecedores, utilizadores, sessao);
    }

    private Fornecedor criar(String codigo, String username) {
        return controller.criarFornecedor(codigo, "ProductHero", "Rua da Alegria, nº1", "4000-032", "Porto",
                username, "segredo");
    }

    @Test
    void gestorCriaFornecedorComTodosOsDadosEContaComPerfilFornecedor() {
        Fornecedor criado = criar("11PT920", "producthero");

        assertTrue(criado.getId() > 0);
        assertEquals("11PT920", criado.getIdentificador());
        assertEquals("Rua da Alegria, nº1", criado.getMorada());
        assertEquals("4000-032", criado.getCodigoPostal());
        assertEquals("Porto", criado.getCidade());
        Utilizador conta = utilizadores.procurarPorUsername("producthero").orElseThrow();
        assertEquals(Perfil.FORNECEDOR, conta.getPerfil());
        assertTrue(conta.verificarPassword("segredo"));
        assertEquals(conta.getId(), criado.getIdUtilizador());
        assertEquals(criado.getId(), fornecedores.procurarPorUtilizador(conta.getId()).orElseThrow().getId());
    }

    @Test
    void codigoRepetidoComContaERecusado() {
        criar("11PT920", "producthero");
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> criar("11pt920", "outro"));
        assertTrue(erro.getMessage().contains("11pt920"));
        assertEquals(1, fornecedores.listarTodos().size());
        assertTrue(utilizadores.procurarPorUsername("outro").isEmpty());
    }

    @Test
    void fornecedorQueJaDeuEntradaSemContaFicaLigadoAContaNova() {
        fornecedores.inserir(new Fornecedor(0, "Nome vindo do XML", "11PT920", ""));

        Fornecedor completo = criar("11PT920", "producthero");

        assertEquals(1, fornecedores.listarTodos().size(), "não cria um fornecedor duplicado");
        assertTrue(completo.temConta());
        assertEquals("Porto", fornecedores.procurarPorIdentificador("11PT920").orElseThrow().getCidade());
    }

    @Test
    void usernameRepetidoNaoCriaFornecedor() {
        utilizadores.inserir(Utilizador.novo("producthero", "x", "Outro", Perfil.RECECIONISTA));
        assertThrows(IllegalArgumentException.class, () -> criar("11PT920", "producthero"));
        assertTrue(fornecedores.listarTodos().isEmpty());
    }

    @Test
    void camposObrigatoriosELimites() {
        assertThrows(IllegalArgumentException.class, () -> criar(" ", "u"));
        assertThrows(IllegalArgumentException.class, () -> controller.criarFornecedor("1", "", "", "", "", "u", "p"));
        assertThrows(IllegalArgumentException.class, () -> controller.criarFornecedor("1", "N", "", "", "", "", "p"));
        assertThrows(IllegalArgumentException.class, () -> controller.criarFornecedor("1", "N", "", "", "", "u", ""));
        assertThrows(IllegalArgumentException.class, () -> criar("C".repeat(Fornecedor.MAX_IDENTIFICADOR + 1), "u"));
        assertTrue(fornecedores.listarTodos().isEmpty());
        assertTrue(utilizadores.listarTodos().isEmpty());
    }

    @Test
    void contaCriadaNaoFicaSemDonoSeOFornecedorNaoForGuardado() {
        FornecedorDAOMemoria avariado = new FornecedorDAOMemoria() {
            @Override
            public Fornecedor inserir(Fornecedor fornecedor) {
                throw new DAOException("falha de teste");
            }
        };
        GestaoFornecedoresController falha = new GestaoFornecedoresController(avariado, utilizadores, sessao);

        assertThrows(AcessoDadosException.class,
                () -> falha.criarFornecedor("11PT920", "N", "", "", "", "producthero", "segredo"));
        assertTrue(utilizadores.procurarPorUsername("producthero").isEmpty());
    }

    @Test
    void soOGestorCriaELista() {
        sessao.iniciar(Utilizador.novo("rececao", "x", "Rececionista", Perfil.RECECIONISTA));
        assertThrows(SecurityException.class, () -> criar("11PT920", "producthero"));
        assertThrows(SecurityException.class, controller::listarFornecedores);
        assertFalse(utilizadores.procurarPorUsername("producthero").isPresent());
    }
}
