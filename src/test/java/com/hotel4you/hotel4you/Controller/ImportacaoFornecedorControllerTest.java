package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.FornecedorDAOMemoria;
import com.hotel4you.hotel4you.DAO.ImportacaoDAOMemoria;
import com.hotel4you.hotel4you.DAO.UtilizadorDAOMemoria;
import com.hotel4you.hotel4you.DAO.XsdDAOMemoria;
import com.hotel4you.hotel4you.Model.Fornecedor;
import com.hotel4you.hotel4you.Model.ImportacaoFornecedor;
import com.hotel4you.hotel4you.Model.Utilizador;
import com.hotel4you.hotel4you.Model.enums.EstadoImportacao;
import com.hotel4you.hotel4you.Model.enums.Perfil;
import com.hotel4you.hotel4you.Utils.XmlInvalidoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testes da S5-02: o fornecedor entra com a sua conta e só importa ficheiros seus. */
class ImportacaoFornecedorControllerTest {

    @TempDir
    Path pasta;

    private UtilizadorDAOMemoria utilizadores;
    private FornecedorDAOMemoria fornecedores;
    private ImportacaoDAOMemoria importacoes;
    private Sessao sessao;
    private ImportacaoFornecedorController controller;
    private Utilizador contaProductHero;

    @BeforeEach
    void preparar() {
        utilizadores = new UtilizadorDAOMemoria();
        fornecedores = new FornecedorDAOMemoria();
        importacoes = new ImportacaoDAOMemoria();
        sessao = new Sessao();
        controller = new ImportacaoFornecedorController(importacoes, fornecedores,
                new GestaoXsdController(new XsdDAOMemoria(), sessao), sessao);
        contaProductHero = criarFornecedor("producthero", "ProductHero", "11PT920");
        sessao.iniciar(contaProductHero);
    }

    private Utilizador criarFornecedor(String username, String nome, String codigo) {
        Utilizador conta = utilizadores.inserir(Utilizador.novo(username, "segredo", nome, Perfil.FORNECEDOR));
        fornecedores.inserir(new Fornecedor(0, nome, codigo, "", "", "", "", conta.getId()));
        return conta;
    }

    private static Path recurso(String nome) throws Exception {
        return Paths.get(ImportacaoFornecedorControllerTest.class.getResource("/xml/" + nome).toURI());
    }

    /** Cópia do XML de exemplo com outro código de fornecedor (como se fosse de outra empresa). */
    private Path xmlDoFornecedor(String codigo) throws Exception {
        String xml = Files.readString(recurso("ProductHero.xml"), StandardCharsets.UTF_8);
        Path destino = pasta.resolve("entrega-" + codigo + ".xml");
        Files.writeString(destino, xml.replace("11PT920", codigo), StandardCharsets.UTF_8);
        return destino;
    }

    @Test
    void ficheiroDoProprioFornecedorFicaPendente() throws Exception {
        ImportacaoFornecedor enviada = controller.enviar(recurso("ProductHero.xml"), false);

        assertEquals(EstadoImportacao.PENDENTE, enviada.getEstado());
        assertEquals("OC56974", enviada.getNumeroEncomenda());
        assertTrue(enviada.getId() > 0);
        assertEquals(1, controller.listarMinhasImportacoes().size());
    }

    @Test
    void envioSemAtualizarNaoMexeNaFicha() throws Exception {
        controller.enviar(recurso("ProductHero.xml"), false);
        assertEquals("", controller.obterFornecedorAtual().getMorada());
    }

    @Test
    void fichaPodeSerAtualizadaComOsDadosDoXml() throws Exception {
        controller.enviar(recurso("ProductHero.xml"), true);

        Fornecedor ficha = fornecedores.procurarPorIdentificador("11PT920").orElseThrow();
        assertEquals("Rua da Alegria, nº1", ficha.getMorada());
        assertEquals("4000-032", ficha.getCodigoPostal());
        assertEquals("Porto", ficha.getCidade());
        assertTrue(ficha.temConta(), "a conta continua ligada ao fornecedor");
    }

    @Test
    void ficheiroDeOutroFornecedorERecusadoENadaFicaGuardado() throws Exception {
        Path alheio = xmlDoFornecedor("22PT111");

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> controller.enviar(alheio, true));
        assertTrue(erro.getMessage().contains("22PT111"));
        assertThrows(IllegalArgumentException.class, () -> controller.lerEncomenda(alheio));
        assertTrue(controller.listarMinhasImportacoes().isEmpty());
        assertTrue(importacoes.listar(null).isEmpty());
        assertEquals("", fornecedores.procurarPorIdentificador("11PT920").orElseThrow().getMorada());
    }

    @Test
    void cadaFornecedorSoVeOsSeusFicheiros() throws Exception {
        controller.enviar(recurso("ProductHero.xml"), false);
        Utilizador outra = criarFornecedor("outra", "Outra Lda", "22PT111");
        sessao.iniciar(outra);
        controller.enviar(xmlDoFornecedor("22PT111"), false);

        assertEquals(1, controller.listarMinhasImportacoes().size());
        assertEquals("22PT111", controller.listarMinhasImportacoes().get(0).getFornecedor().getIdentificador());
        assertEquals(2, importacoes.listar(null).size());
    }

    @Test
    void ficheiroInvalidoDizLinhaEMotivoENaoFicaGuardado() throws Exception {
        XmlInvalidoException erro = assertThrows(XmlInvalidoException.class,
                () -> controller.enviar(recurso("ProductHero_invalido.xml"), false));

        assertFalse(erro.getErros().isEmpty());
        erro.getErros().forEach(e -> {
            assertTrue(e.linha() > 0, "cada erro indica a linha");
            assertFalse(e.motivo().isBlank(), "cada erro indica o motivo");
        });
        assertTrue(importacoes.listar(null).isEmpty());
    }

    @Test
    void soUmFornecedorPodeImportar() throws Exception {
        sessao.iniciar(Utilizador.novo("gestor", "gestor123", "Gestor", Perfil.GESTOR));
        Path ficheiro = recurso("ProductHero.xml");
        assertThrows(SecurityException.class, () -> controller.enviar(ficheiro, false));
        sessao.terminar();
        assertThrows(SecurityException.class, () -> controller.enviar(ficheiro, false));
    }

    @Test
    void contaSemFornecedorAssociadoNaoImporta() throws Exception {
        sessao.iniciar(utilizadores.inserir(Utilizador.novo("solta", "segredo", "Conta solta", Perfil.FORNECEDOR)));
        Path ficheiro = recurso("ProductHero.xml");
        SecurityException erro = assertThrows(SecurityException.class, () -> controller.enviar(ficheiro, false));
        assertTrue(erro.getMessage().contains("não está associada"));
    }

    @Test
    void ficheiroInexistenteDaMensagemEmVezDeFecharAAplicacao() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> controller.enviar(Paths.get("nao-existe.xml"), false));
        assertTrue(erro.getMessage().contains("nao-existe.xml"));
        assertThrows(IllegalArgumentException.class, () -> controller.enviar(null, false));
    }

    @Test
    void contaBaseCriadaPeloGestorSoComCodigoENomeCompletaFichaNoPrimeiroXml() throws Exception {
        GestaoFornecedoresController gestao = new GestaoFornecedoresController(fornecedores, utilizadores, sessao);
        sessao.iniciar(utilizadores.inserir(Utilizador.novo("gestor", "gestor123", "Gestor", Perfil.GESTOR)));
        Fornecedor base = gestao.criarFornecedor("33PT333", "Fornecedora SA", "", "", "", "fornecedora", "segredo");
        assertEquals("", base.getMorada());

        sessao.iniciar(utilizadores.procurarPorUsername("fornecedora").orElseThrow());
        controller.enviar(xmlDoFornecedor("33PT333"), true);

        Fornecedor ficha = fornecedores.procurarPorIdentificador("33PT333").orElseThrow();
        assertEquals("Rua da Alegria, nº1", ficha.getMorada());
        assertEquals("Porto", ficha.getCidade());
        assertTrue(ficha.temConta());
    }
}