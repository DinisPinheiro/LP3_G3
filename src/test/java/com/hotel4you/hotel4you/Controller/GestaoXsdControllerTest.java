package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.XsdDAOMemoria;
import com.hotel4you.hotel4you.Model.FicheiroXsd;
import com.hotel4you.hotel4you.Model.Utilizador;
import com.hotel4you.hotel4you.Model.enums.Perfil;
import com.hotel4you.hotel4you.Utils.LeitorEncomendaXml;
import com.hotel4you.hotel4you.Utils.ValidadorXml;
import com.hotel4you.hotel4you.Utils.XmlInvalidoException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testes da S5-05: o gestor importa ou atualiza o XSD e a validação passa a usá-lo. */
class GestaoXsdControllerTest {

    private static final String XSD_ENCOMENDA = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">"
            + "<xs:element name=\"Encomenda\" type=\"xs:string\"/></xs:schema>";

    @TempDir
    Path pasta;

    private XsdDAOMemoria dao;
    private Sessao sessao;
    private GestaoXsdController controller;

    @BeforeEach
    void preparar() {
        dao = new XsdDAOMemoria();
        sessao = new Sessao();
        sessao.iniciar(Utilizador.novo("gestor", "gestor123", "Gestor", Perfil.GESTOR));
        controller = new GestaoXsdController(dao, sessao);
    }

    private Path ficheiro(String nome, String conteudo) throws Exception {
        Path destino = pasta.resolve(nome);
        Files.writeString(destino, conteudo, StandardCharsets.UTF_8);
        return destino;
    }

    private static byte[] produtoHero() throws Exception {
        return Files.readAllBytes(Paths.get(GestaoXsdControllerTest.class.getResource("/xml/ProductHero.xml").toURI()));
    }

    @Test
    void semXsdImportadoUsaOXsdDaAplicacao() throws Exception {
        assertTrue(ValidadorXml.validar(new ByteArrayInputStream(produtoHero()), controller.obterSchemaEmVigor()).valido());
        assertTrue(controller.descreverXsdEmVigor().contains("vem com a aplicação"));
    }

    @Test
    void xsdValidoFicaGuardadoEPassaAValidar() throws Exception {
        FicheiroXsd guardado = controller.importarXsd(ficheiro("encomenda.xsd", XSD_ENCOMENDA));

        assertTrue(guardado.getId() > 0);
        assertTrue(controller.descreverXsdEmVigor().contains("encomenda.xsd"));
        // O XML de exemplo cumpria o XSD da aplicação, mas não cumpre o escolhido.
        assertTrue(!ValidadorXml.validar(new ByteArrayInputStream(produtoHero()), controller.obterSchemaEmVigor()).valido());
        assertTrue(ValidadorXml.validar(new ByteArrayInputStream("<Encomenda>x</Encomenda>".getBytes(StandardCharsets.UTF_8)),
                controller.obterSchemaEmVigor()).valido());
    }

    @Test
    void xsdNovoSubstituiOAnterior() throws Exception {
        controller.importarXsd(ficheiro("primeiro.xsd", XSD_ENCOMENDA));
        controller.importarXsd(ficheiro("segundo.xsd", XSD_ENCOMENDA));

        assertEquals(2, dao.total());
        assertTrue(controller.descreverXsdEmVigor().contains("segundo.xsd"));
    }

    @Test
    void xsdInvalidoERejeitadoENadaMuda() throws Exception {
        controller.importarXsd(ficheiro("bom.xsd", XSD_ENCOMENDA));

        XsdInvalidoException erro = assertThrows(XsdInvalidoException.class,
                () -> controller.importarXsd(ficheiro("mau.xsd", "<xs:schema>")));

        assertTrue(erro.getMessage().startsWith("O ficheiro XSD é inválido."));
        assertEquals(1, dao.total());
        assertTrue(controller.descreverXsdEmVigor().contains("bom.xsd"));
    }

    @Test
    void ficheiroVazioOuInexistenteDaMensagem() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> controller.importarXsd(null));
        assertThrows(IllegalArgumentException.class, () -> controller.importarXsd(ficheiro("vazio.xsd", "")));
        assertThrows(IllegalArgumentException.class, () -> controller.importarXsd(Paths.get("nao-existe.xsd")));
        assertEquals(0, dao.total());
    }

    @Test
    void soOGestorImportaOXsd() throws Exception {
        Path xsd = ficheiro("encomenda.xsd", XSD_ENCOMENDA);
        sessao.iniciar(Utilizador.novo("rececao", "x", "Rececionista", Perfil.RECECIONISTA));
        assertThrows(SecurityException.class, () -> controller.importarXsd(xsd));
        sessao.iniciar(Utilizador.novo("forn", "x", "Fornecedor", Perfil.FORNECEDOR));
        assertThrows(SecurityException.class, () -> controller.importarXsd(xsd));
        assertEquals(0, dao.total());
    }

    @Test
    void xmlQueCumpreOXsdMasNaoTemEstruturaDeEncomendaDaMensagemClara() throws Exception {
        controller.importarXsd(ficheiro("encomenda.xsd", XSD_ENCOMENDA));
        byte[] xml = "<Encomenda>x</Encomenda>".getBytes(StandardCharsets.UTF_8);

        XmlInvalidoException erro = assertThrows(XmlInvalidoException.class,
                () -> LeitorEncomendaXml.ler(xml, controller.obterSchemaEmVigor()));
        assertTrue(erro.getMessage().contains("estrutura"));
    }
}
