package com.hotel4you.hotel4you.Utils;

import org.junit.jupiter.api.Test;

import javax.xml.validation.Schema;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testes da S5-05: a validação usa o XSD recebido por parâmetro; um XSD inválido é rejeitado. */
class ValidadorXmlTest {

    private static final String XSD_ENCOMENDA = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">"
            + "<xs:element name=\"Encomenda\" type=\"xs:string\"/></xs:schema>";

    private static InputStream texto(String conteudo) {
        return new ByteArrayInputStream(conteudo.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void xsdValidoECarregado() {
        Schema schema = ValidadorXml.carregarSchema(texto(XSD_ENCOMENDA));
        assertTrue(schema != null);
    }

    @Test
    void validacaoUsaOXsdEscolhido() throws Exception {
        Schema schema = ValidadorXml.carregarSchema(texto(XSD_ENCOMENDA));

        assertTrue(ValidadorXml.validar(texto("<Encomenda>ok</Encomenda>"), schema).valido());
        assertFalse(ValidadorXml.validar(texto("<Order>ok</Order>"), schema).valido());
        // O XSD da aplicação aceita o inverso: o resultado depende mesmo do XSD escolhido.
        assertFalse(ValidadorXml.validar(texto("<Encomenda>ok</Encomenda>")).valido());
    }

    @Test
    void xsdMalFormadoERejeitadoComMensagem() {
        XsdInvalidoException erro = assertThrows(XsdInvalidoException.class,
                () -> ValidadorXml.carregarSchema(texto("<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">")));
        assertTrue(erro.getMessage().startsWith("O ficheiro XSD é inválido."));
    }

    @Test
    void ficheiroQueNaoEXmlERejeitado() {
        assertThrows(XsdInvalidoException.class, () -> ValidadorXml.carregarSchema(texto("isto não é um XSD")));
    }

    @Test
    void xsdComTipoInexistenteERejeitado() {
        String xsd = "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">"
                + "<xs:element name=\"a\" type=\"xs:naoExiste\"/></xs:schema>";
        XsdInvalidoException erro = assertThrows(XsdInvalidoException.class,
                () -> ValidadorXml.carregarSchema(texto(xsd)));
        assertFalse(erro.getMessage().isBlank());
    }
}
