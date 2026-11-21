package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.XsdDAOMemoria;
import com.hotel4you.hotel4you.Model.FicheiroXsd;
import com.hotel4you.hotel4you.Model.Fornecedor;
import com.hotel4you.hotel4you.Model.Utilizador;
import com.hotel4you.hotel4you.Model.enums.Perfil;
import com.hotel4you.hotel4you.Utils.LeitorEncomendaXml;
import com.hotel4you.hotel4you.Utils.RecursosDeTeste;
import com.hotel4you.hotel4you.Utils.ValidadorXml;
import com.hotel4you.hotel4you.Utils.XmlInvalidoException;
import com.hotel4you.hotel4you.Utils.XsdEmFaltaException;
import com.hotel4you.hotel4you.Utils.XsdInvalidoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testes da S5-05 e da S6-02: cada fornecedor importa o seu XSD e a validação passa a usá-lo. */
class GestaoXsdControllerTest {

    private static final String XSD_ENCOMENDA = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">"
            + "<xs:element name=\"Encomenda\" type=\"xs:string\"/></xs:schema>";

    @TempDir
    Path pasta;

    private XsdDAOMemoria dao;
    private Sessao sessao;
    private GestaoXsdController controller;
    private Fornecedor productHero;
    private Fornecedor outra;

    @BeforeEach
    void preparar() {
        dao = new XsdDAOMemoria();
        sessao = new Sessao();
        sessao.iniciar(Utilizador.novo("producthero", "segredo", "ProductHero", Perfil.FORNECEDOR));
        controller = new GestaoXsdController(dao, sessao);
        productHero = new Fornecedor(1, "ProductHero", "11PT920", "", "", "", "", 10);
        outra = new Fornecedor(2, "Outra Lda", "22PT111", "", "", "", "", 11);
    }

    private Path ficheiro(String nome, String conteudo) throws Exception {
        Path destino = pasta.resolve(nome);
        Files.writeString(destino, conteudo, StandardCharsets.UTF_8);
        return destino;
    }

    private static byte[] produtoHero() throws Exception {
        return Files.readAllBytes(RecursosDeTeste.recurso("ProductHero.xml"));
    }

    private static byte[] bytes(String texto) {
        return texto.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void semXsdImportadoNaoHaValidacaoENaoHaXsdDaAplicacao() {
        XsdEmFaltaException erro = assertThrows(XsdEmFaltaException.class, () -> controller.obterSchema(productHero));

        assertTrue(erro.getMessage().contains("ainda não importou"));
        assertTrue(controller.descreverXsd(productHero).contains("Ainda não importou"));
    }

    @Test
    void xsdValidoFicaGuardadoEPassaAValidar() throws Exception {
        FicheiroXsd guardado = controller.importarXsd(productHero, RecursosDeTeste.xsd());

        assertTrue(guardado.getId() > 0);
        assertEquals(productHero.getId(), guardado.getIdFornecedor());
        assertTrue(controller.descreverXsd(productHero).contains("Hotel4You.xsd"));
        assertTrue(ValidadorXml.validar(new ByteArrayInputStream(produtoHero()), controller.obterSchema(productHero))
                .valido());
    }

    @Test
    void xsdNovoSubstituiOAnteriorEPassaAValidarOsXmlSeguintes() throws Exception {
        controller.importarXsd(productHero, RecursosDeTeste.xsd());
        controller.importarXsd(productHero, ficheiro("encomenda.xsd", XSD_ENCOMENDA));

        assertEquals(2, dao.total());
        assertTrue(controller.descreverXsd(productHero).contains("encomenda.xsd"));
        // O XML de exemplo cumpria o primeiro XSD, mas não cumpre o que o substituiu.
        assertFalse(ValidadorXml.validar(new ByteArrayInputStream(produtoHero()), controller.obterSchema(productHero))
                .valido());
        assertTrue(ValidadorXml.validar(new ByteArrayInputStream(bytes("<Encomenda>x</Encomenda>")),
                controller.obterSchema(productHero)).valido());
    }

    @Test
    void xsdInvalidoERejeitadoComMensagemENadaMuda() throws Exception {
        controller.importarXsd(productHero, ficheiro("bom.xsd", XSD_ENCOMENDA));

        XsdInvalidoException erro = assertThrows(XsdInvalidoException.class,
                () -> controller.importarXsd(productHero, ficheiro("mau.xsd", "<xs:schema>")));

        assertTrue(erro.getMessage().startsWith("O ficheiro XSD é inválido."));
        assertEquals(1, dao.total());
        assertTrue(controller.descreverXsd(productHero).contains("bom.xsd"));
    }

    @Test
    void xsdDeTesteInvalidoDoProjetoERejeitado() {
        assertThrows(XsdInvalidoException.class,
                () -> controller.importarXsd(productHero, RecursosDeTeste.recurso("XsdInvalido.xsd")));
        assertEquals(0, dao.total());
    }

    @Test
    void ficheiroVazioOuInexistenteDaMensagem() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> controller.importarXsd(productHero, null));
        assertThrows(IllegalArgumentException.class, () -> controller.importarXsd(productHero, ficheiro("vazio.xsd", "")));
        assertThrows(IllegalArgumentException.class,
                () -> controller.importarXsd(productHero, Paths.get("nao-existe.xsd")));
        assertEquals(0, dao.total());
    }

    @Test
    void soOFornecedorImportaOXsd() {
        Path xsd = RecursosDeTeste.xsd();
        for (Perfil perfil : new Perfil[]{Perfil.GESTOR, Perfil.RECECIONISTA}) {
            sessao.iniciar(Utilizador.novo("outro", "x", "Outro", perfil));
            assertThrows(SecurityException.class, () -> controller.importarXsd(productHero, xsd));
        }
        sessao.terminar();
        assertThrows(SecurityException.class, () -> controller.importarXsd(productHero, xsd));
        assertEquals(0, dao.total());
    }

    @Test
    void xsdDeUmFornecedorNaoValidaOsXmlDeOutro() throws Exception {
        controller.importarXsd(productHero, ficheiro("encomenda.xsd", XSD_ENCOMENDA));

        assertThrows(XsdEmFaltaException.class, () -> controller.obterSchema(outra));

        controller.importarXsd(outra, RecursosDeTeste.xsd());
        assertTrue(ValidadorXml.validar(new ByteArrayInputStream(produtoHero()), controller.obterSchema(outra)).valido());
        assertFalse(ValidadorXml.validar(new ByteArrayInputStream(produtoHero()), controller.obterSchema(productHero))
                .valido());
    }

    @Test
    void xmlQueCumpreOXsdMasNaoTemEstruturaDeEncomendaDaMensagemClara() throws Exception {
        controller.importarXsd(productHero, ficheiro("encomenda.xsd", XSD_ENCOMENDA));
        byte[] xml = bytes("<Encomenda>x</Encomenda>");

        XmlInvalidoException erro = assertThrows(XmlInvalidoException.class,
                () -> LeitorEncomendaXml.ler(xml, controller.obterSchema(productHero)));
        assertTrue(erro.getMessage().contains("estrutura"));
    }
}
