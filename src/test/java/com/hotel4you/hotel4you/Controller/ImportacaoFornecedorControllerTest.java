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
import com.hotel4you.hotel4you.Utils.RecursosDeTeste;
import com.hotel4you.hotel4you.Utils.XmlInvalidoException;
import com.hotel4you.hotel4you.Utils.XsdEmFaltaException;
import com.hotel4you.hotel4you.Utils.XsdInvalidoException;
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

/**
 * Testes da S5-02 (o fornecedor entra com a sua conta e só importa ficheiros seus), da S6-02 (importa o XML e
 * a seguir o XSD que o valida) e da S6-03 (encomenda repetida bloqueada e avisos de IVA e totais).
 */
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
        controller.importarXsd(RecursosDeTeste.xsd());
    }

    private Utilizador criarFornecedor(String username, String nome, String codigo) {
        Utilizador conta = utilizadores.inserir(Utilizador.novo(username, "segredo", nome, Perfil.FORNECEDOR));
        fornecedores.inserir(new Fornecedor(0, nome, codigo, "", "", "", "", conta.getId()));
        return conta;
    }

    private static Path recurso(String nome) throws Exception {
        return Paths.get("src", "test", "resources", "xml", nome);
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
        controller.importarXsd(RecursosDeTeste.xsd());
        // Mesmo número de encomenda (OC56974) que o da ProductHero: é de outro fornecedor, por isso é aceite.
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
        controller.importarXsd(RecursosDeTeste.xsd());
        controller.enviar(xmlDoFornecedor("33PT333"), true);

        Fornecedor ficha = fornecedores.procurarPorIdentificador("33PT333").orElseThrow();
        assertEquals("Rua da Alegria, nº1", ficha.getMorada());
        assertEquals("Porto", ficha.getCidade());
        assertTrue(ficha.temConta());
    }

    // ---- S6-02: o fornecedor importa o XSD a seguir ao XML ------------------------------------------

    @Test
    void semXsdImportadoNaoHaValidacaoNemEnvio() throws Exception {
        sessao.iniciar(criarFornecedor("nova", "Nova Lda", "44PT444"));
        Path ficheiro = xmlDoFornecedor("44PT444");

        XsdEmFaltaException erro = assertThrows(XsdEmFaltaException.class, () -> controller.lerEncomenda(ficheiro));
        assertTrue(erro.getMessage().contains("XSD"));
        assertThrows(XsdEmFaltaException.class, () -> controller.enviar(ficheiro, false));
        assertTrue(importacoes.listar(null).isEmpty());
        assertTrue(controller.descreverXsd().contains("Ainda não importou"));
    }

    @Test
    void depoisDeImportarOXsdOMesmoXmlPassaAValidarEFicaGuardadoParaOsSeguintes() throws Exception {
        sessao.iniciar(criarFornecedor("nova", "Nova Lda", "44PT444"));
        Path ficheiro = xmlDoFornecedor("44PT444");
        assertThrows(XsdEmFaltaException.class, () -> controller.lerEncomenda(ficheiro));

        controller.importarXsd(RecursosDeTeste.xsd());

        assertEquals("OC56974", controller.lerEncomenda(ficheiro).numero());
        assertTrue(controller.descreverXsd().contains("Hotel4You.xsd"));
        // Não precisa de importar o XSD outra vez: o seguinte XML já é validado com o guardado.
        assertEquals(EstadoImportacao.PENDENTE, controller.enviar(ficheiro, false).getEstado());
        Path seguinte = pasta.resolve("seguinte.xml");
        Files.writeString(seguinte, Files.readString(ficheiro, StandardCharsets.UTF_8)
                .replace("OC56974", "OC56975"), StandardCharsets.UTF_8);
        assertEquals("OC56975", controller.lerEncomenda(seguinte).numero());
    }

    @Test
    void xsdImportadoSubstituiOAnteriorEPassaAValidarOsXml() throws Exception {
        Path restrito = pasta.resolve("restrito.xsd");
        Files.writeString(restrito, "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">"
                + "<xs:element name=\"Encomenda\" type=\"xs:string\"/></xs:schema>", StandardCharsets.UTF_8);

        controller.importarXsd(restrito);

        assertTrue(controller.descreverXsd().contains("restrito.xsd"));
        assertThrows(XmlInvalidoException.class, () -> controller.lerEncomenda(recurso("ProductHero.xml")));
    }

    @Test
    void xsdInvalidoERejeitadoComMensagemEOAnteriorContinuaEmUso() throws Exception {
        XsdInvalidoException erro = assertThrows(XsdInvalidoException.class,
                () -> controller.importarXsd(recurso("XsdInvalido.xsd")));

        assertTrue(erro.getMessage().startsWith("O ficheiro XSD é inválido."));
        assertTrue(controller.descreverXsd().contains("Hotel4You.xsd"));
        assertEquals("OC56974", controller.lerEncomenda(recurso("ProductHero.xml")).numero());
    }

    @Test
    void xmlInvalidoContinuaARejeitadoComLinhaEMotivo() throws Exception {
        XmlInvalidoException erro = assertThrows(XmlInvalidoException.class,
                () -> controller.lerEncomenda(recurso("ProductHero_invalido.xml")));

        assertFalse(erro.getErros().isEmpty());
        assertTrue(erro.getErros().get(0).linha() > 0);
        assertFalse(erro.getErros().get(0).motivo().isBlank());
    }

    @Test
    void soUmFornecedorImportaOXsd() {
        for (Perfil perfil : new Perfil[]{Perfil.GESTOR, Perfil.RECECIONISTA}) {
            sessao.iniciar(Utilizador.novo("outro", "x", "Outro", perfil));
            assertThrows(SecurityException.class, () -> controller.importarXsd(RecursosDeTeste.xsd()));
        }
    }

    // ---- S6-03: validação dos dados do XML ------------------------------------------------------------

    @Test
    void encomendaJaEnviadaPeloMesmoFornecedorEBloqueadaComMensagem() throws Exception {
        controller.enviar(recurso("ProductHero.xml"), false);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> controller.enviar(recurso("ProductHero.xml"), false));

        assertTrue(erro.getMessage().contains("OC56974"));
        assertTrue(erro.getMessage().contains("já foi enviada"));
        assertEquals(1, importacoes.listar(null).size());
    }

    @Test
    void ficheiroDiferenteComOMesmoNumeroDeEncomendaTambemEBloqueadoJaNaLeitura() throws Exception {
        controller.enviar(recurso("ProductHero.xml"), false);

        // O bloqueio vê-se logo ao escolher o ficheiro, antes de tentar enviar.
        assertThrows(IllegalArgumentException.class,
                () -> controller.lerEncomenda(recurso("ProductHero_encomenda_repetida.xml")));
        assertThrows(IllegalArgumentException.class,
                () -> controller.enviar(recurso("ProductHero_encomenda_repetida.xml"), false));
        assertEquals(1, importacoes.listar(null).size());
    }

    @Test
    void encomendaRepetidaContaMesmoDepoisDeRejeitadaOuAprovada() throws Exception {
        ImportacaoFornecedor enviada = controller.enviar(recurso("ProductHero.xml"), false);
        Utilizador gestor = utilizadores.inserir(Utilizador.novo("gestor", "gestor123", "Gestor", Perfil.GESTOR));
        enviada.rejeitar(gestor, java.time.LocalDateTime.now());
        importacoes.guardarDecisao(enviada);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> controller.enviar(recurso("ProductHero.xml"), false));
        assertTrue(erro.getMessage().contains("rejeitado"));
    }

    @Test
    void encomendaComOutroNumeroDoMesmoFornecedorEAceite() throws Exception {
        controller.enviar(recurso("ProductHero.xml"), false);
        controller.enviar(recurso("ProductHero_encomenda_nova.xml"), false);

        assertEquals(2, controller.listarMinhasImportacoes().size());
    }

    @Test
    void ivaOuTotaisDiferentesDosDoFicheiroDaoAvisoMasOEnvioContinua() throws Exception {
        // O XML tem 6% numa linha cujo imposto foi calculado a 23%.
        var encomenda = controller.lerEncomenda(recurso("ProductHero_iva_errado.xml"));

        assertFalse(encomenda.avisos().isEmpty());
        assertTrue(encomenda.avisos().stream().anyMatch(a -> a.contains("Linha 1")));
        assertEquals(EstadoImportacao.PENDENTE, controller.enviar(recurso("ProductHero_iva_errado.xml"), false).getEstado());
    }

    @Test
    void xmlComOsValoresCertosNaoDaAvisos() throws Exception {
        // O XML de exemplo tem um imposto com 3 cêntimos de diferença na linha 2 (160,51 em vez de 160,54).
        assertTrue(controller.lerEncomenda(recurso("ProductHero.xml")).avisos().stream()
                .anyMatch(a -> a.contains("Linha 2")));
        Path certo = pasta.resolve("certo.xml");
        Files.writeString(certo, Files.readString(recurso("ProductHero.xml"), StandardCharsets.UTF_8)
                .replace("160.51", "160.54"), StandardCharsets.UTF_8);

        assertTrue(controller.lerEncomenda(certo).avisos().isEmpty());
    }
}
