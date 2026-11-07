package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.StockDAOMemoria;
import com.hotel4you.hotel4you.Model.Artigo;
import com.hotel4you.hotel4you.Model.MovimentoStock;
import com.hotel4you.hotel4you.Model.Utilizador;
import com.hotel4you.hotel4you.Model.encomenda.EncomendaFornecedor;
import com.hotel4you.hotel4you.Model.enums.Perfil;
import com.hotel4you.hotel4you.Model.enums.TipoMovimento;
import com.hotel4you.hotel4you.Utils.ErroXml;
import com.hotel4you.hotel4you.Utils.XmlInvalidoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testes da S4-03 (validar o XML do fornecedor) e da S4-04 (entrada e consulta de stock). */
class GestaoStockControllerTest {

    private StockDAOMemoria dao;
    private Sessao sessao;
    private GestaoStockController controller;

    @BeforeEach
    void preparar() {
        dao = new StockDAOMemoria();
        sessao = new Sessao();
        sessao.iniciar(Utilizador.novo("gestor", "gestor123", "Gestor Inicial", Perfil.GESTOR));
        controller = new GestaoStockController(dao, sessao);
    }

    private static Path recurso(String nome) throws URISyntaxException {
        return Paths.get(GestaoStockControllerTest.class.getResource("/xml/" + nome).toURI());
    }

    // ---- S4-03: validação do XML ---------------------------------------------------------------

    @Test
    void xmlValidoDevolveOsDadosDaEncomenda() throws Exception {
        EncomendaFornecedor encomenda = controller.lerEncomendaXml(recurso("ProductHero.xml"));
        assertEquals("OC56974", encomenda.numero());
        assertEquals("ProductHero", encomenda.fornecedor().nome());
        assertEquals("11PT920", encomenda.fornecedor().identificador());
        assertEquals(5, encomenda.linhas().size());
        assertEquals("Toalha de banho 400g cinza", encomenda.linhas().get(0).descricao());
        assertEquals(50, encomenda.linhas().get(0).quantidade().valor());
    }

    @Test
    void xmlInvalidoDizLinhaEMotivoENaoGuardaNada() throws Exception {
        XmlInvalidoException erro = assertThrows(XmlInvalidoException.class,
                () -> controller.lerEncomendaXml(recurso("ProductHero_invalido.xml")));
        assertFalse(erro.getErros().isEmpty());
        for (ErroXml e : erro.getErros()) {
            assertTrue(e.linha() > 0, "cada erro indica a linha");
            assertFalse(e.motivo().isBlank(), "cada erro indica o motivo");
        }
        assertTrue(dao.listarArtigos().isEmpty());
    }

    @Test
    void ficheiroInexistenteDaMensagemEmVezDeFecharAAplicacao() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> controller.lerEncomendaXml(Paths.get("nao-existe.xml")));
        assertTrue(erro.getMessage().contains("nao-existe.xml"));
    }

    @Test
    void xmlMalFormadoEInvalido(@TempDir Path pasta) throws IOException {
        Path ficheiro = Files.writeString(pasta.resolve("estragado.xml"), "<Order><Header>", StandardCharsets.UTF_8);
        XmlInvalidoException erro = assertThrows(XmlInvalidoException.class, () -> controller.lerEncomendaXml(ficheiro));
        assertFalse(erro.getErros().isEmpty());
    }

    @Test
    void semFicheiroPedeParaEscolher() {
        assertThrows(IllegalArgumentException.class, () -> controller.lerEncomendaXml(null));
    }

    // ---- S4-04: entrada e consulta de stock ----------------------------------------------------

    @Test
    void entradaRegistaArtigoQuantidadeFornecedorEData() throws Exception {
        EncomendaFornecedor encomenda = controller.lerEncomendaXml(recurso("ProductHero.xml"));
        assertEquals(5, controller.darEntrada(encomenda));

        List<Artigo> stock = controller.consultarStock();
        assertEquals(5, stock.size());
        Artigo toalhas = stock.stream().filter(a -> a.getNome().startsWith("Toalha")).findFirst().orElseThrow();
        assertEquals(50, toalhas.getQuantidadeAtual());
        assertEquals("cx", toalhas.getUnidade());
        assertEquals("ProductHero", toalhas.getFornecedor().getNome());

        assertEquals(5, dao.getMovimentos().size());
        MovimentoStock movimento = dao.getMovimentos().get(0);
        assertEquals(TipoMovimento.ENTRADA, movimento.getTipo());
        assertNotNull(movimento.getDataHora());
        assertEquals("gestor", movimento.getUtilizador().getUsername());
        assertEquals(1, dao.getFornecedores().size());
    }

    @Test
    void segundaEntradaSomaAoStockESemRepetirOFornecedor() throws Exception {
        EncomendaFornecedor encomenda = controller.lerEncomendaXml(recurso("ProductHero.xml"));
        controller.darEntrada(encomenda);
        controller.darEntrada(encomenda);
        Artigo secador = controller.consultarStock().stream()
                .filter(a -> a.getNome().startsWith("Secador")).findFirst().orElseThrow();
        assertEquals(100, secador.getQuantidadeAtual());
        assertEquals(5, controller.consultarStock().size());
        assertEquals(1, dao.getFornecedores().size());
    }

    @Test
    void xmlInvalidoNaoGeraQualquerEntrada() throws Exception {
        assertThrows(XmlInvalidoException.class, () -> controller.lerEncomendaXml(recurso("ProductHero_invalido.xml")));
        assertTrue(controller.consultarStock().isEmpty());
        assertTrue(dao.getMovimentos().isEmpty());
        assertTrue(dao.getFornecedores().isEmpty());
    }

    @Test
    void quantidadeZeroERejeitadaENadaFicaGuardado(@TempDir Path pasta) throws Exception {
        String xml = Files.readString(recurso("ProductHero.xml"), StandardCharsets.UTF_8)
                // a linha 2 passa a ter quantidade 0 (o XSD aceita 0, a entrada não)
                .replace("<Value UOM=\"Unit\">200</Value>", "<Value UOM=\"Unit\">0</Value>");
        Path ficheiro = Files.writeString(pasta.resolve("zero.xml"), xml, StandardCharsets.UTF_8);
        EncomendaFornecedor encomenda = controller.lerEncomendaXml(ficheiro);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> controller.darEntrada(encomenda));
        assertTrue(erro.getMessage().startsWith("Linha 2"));
        assertTrue(controller.consultarStock().isEmpty());
        assertTrue(dao.getFornecedores().isEmpty());
    }

    @Test
    void unidadeDiferenteDaDoArtigoEmStockERejeitada(@TempDir Path pasta) throws Exception {
        controller.darEntrada(controller.lerEncomendaXml(recurso("ProductHero.xml")));
        String xml = Files.readString(recurso("ProductHero.xml"), StandardCharsets.UTF_8)
                // as toalhas passam a vir em unidades (em stock estão em caixas)
                .replace("<Value UOM=\"Box\">50</Value>", "<Value UOM=\"Unit\">50</Value>");
        EncomendaFornecedor encomenda = controller.lerEncomendaXml(
                Files.writeString(pasta.resolve("unidades.xml"), xml, StandardCharsets.UTF_8));

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> controller.darEntrada(encomenda));
        assertTrue(erro.getMessage().contains("Linha 1"));
        assertEquals(5, dao.getMovimentos().size(), "a segunda entrada não guardou nada");
    }

    @Test
    void rececionistaPodeDarEntradaEConsultar() throws Exception {
        sessao.iniciar(Utilizador.novo("rececao", "rececao123", "Ana Rececionista", Perfil.RECECIONISTA));
        controller.darEntrada(controller.lerEncomendaXml(recurso("ProductHero.xml")));
        assertEquals(5, controller.consultarStock().size());
    }

    @Test
    void outrosPerfisESemSessaoNaoTemAcesso() throws Exception {
        EncomendaFornecedor encomenda = controller.lerEncomendaXml(recurso("ProductHero.xml"));
        for (Perfil perfil : new Perfil[]{Perfil.FORNECEDOR, Perfil.EMPRESA_RESTAURACAO}) {
            sessao.iniciar(Utilizador.novo("outro", "outro1234", "Outro", perfil));
            assertThrows(SecurityException.class, () -> controller.consultarStock());
            assertThrows(SecurityException.class, () -> controller.lerEncomendaXml(null));
            assertThrows(SecurityException.class, () -> controller.darEntrada(encomenda));
        }
        sessao.terminar();
        assertThrows(SecurityException.class, () -> controller.consultarStock());
    }
}
