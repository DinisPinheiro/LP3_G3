package com.hotel4you.hotel4you.Utils;

import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Valida ficheiros XML de encomendas de fornecedores contra o XSD que o próprio fornecedor importou
 * (S5-05, S6-02), recebido por parâmetro. A aplicação não traz nenhum XSD seu.
 * <p>
 * Devolve todos os erros encontrados, cada um com a linha e o motivo. Não lê os dados nem
 * mostra nada no ecrã: quem a usa decide o que fazer com o resultado. Usa apenas o que já vem
 * no JDK ({@code javax.xml.validation}), sem bibliotecas externas.
 * <p>
 * Por segurança, ficheiros com {@code DOCTYPE} são recusados (evita ataques XXE) e não são
 * abertos recursos externos.
 */
public final class ValidadorXml {

    private static final String RECUSAR_DOCTYPE = "http://apache.org/xml/features/disallow-doctype-decl";

    private ValidadorXml() {
    }

    /**
     * Valida um XML contra o XSD indicado.
     *
     * @param xml    conteúdo do XML (não é fechado por este método)
     * @param schema esquema contra o qual validar, ver {@link #carregarSchema(InputStream)}
     * @return o resultado, com a lista de erros (vazia se o XML for válido)
     * @throws IOException se o conteúdo não puder ser lido
     */
    public static ResultadoValidacao validar(InputStream xml, Schema schema) throws IOException {
        RecolhaDeErros recolha = new RecolhaDeErros();
        try {
            SAXParserFactory fabrica = SAXParserFactory.newInstance();
            fabrica.setNamespaceAware(true);
            fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            fabrica.setFeature(RECUSAR_DOCTYPE, true);
            XMLReader leitor = fabrica.newSAXParser().getXMLReader();

            Validator validador = schema.newValidator();
            validador.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            validador.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            validador.setErrorHandler(recolha);
            validador.validate(new SAXSource(leitor, new InputSource(xml)));
        } catch (SAXParseException e) {
            // XML mal formado: o validador pára aqui. Regista-se o erro se ainda não foi registado.
            recolha.registar(e);
        } catch (SAXException e) {
            recolha.erros.add(new ErroXml(0, 0, e.getMessage()));
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("O JDK não suporta a configuração segura do XML.", e);
        }
        return new ResultadoValidacao(recolha.erros);
    }

    /**
     * Lê e compila um ficheiro XSD, para o poder usar na validação (S5-05, S6-02). Falha se o ficheiro não
     * for um esquema XML válido. Não são abertos recursos externos (por exemplo, outros XSD por URL).
     *
     * @param xsd conteúdo do XSD (não é fechado por este método)
     * @return o esquema pronto a usar em {@link #validar(InputStream, Schema)}
     * @throws XsdInvalidoException se o ficheiro não for um XSD válido (a mensagem indica o motivo)
     */
    public static Schema carregarSchema(InputStream xsd) {
        try {
            SchemaFactory fabrica = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            fabrica.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            fabrica.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return fabrica.newSchema(new StreamSource(xsd));
        } catch (SAXParseException e) {
            ErroXml erro = new ErroXml(e.getLineNumber(), e.getColumnNumber(), e.getMessage());
            throw new XsdInvalidoException("O ficheiro XSD é inválido. " + erro, e);
        } catch (SAXException e) {
            throw new XsdInvalidoException("O ficheiro XSD é inválido. " + e.getMessage(), e);
        }
    }

    /** Guarda todos os erros em vez de parar no primeiro. */
    private static final class RecolhaDeErros implements ErrorHandler {

        private final List<ErroXml> erros = new ArrayList<>();

        @Override
        public void warning(SAXParseException e) {
            // Avisos não tornam o XML inválido.
        }

        @Override
        public void error(SAXParseException e) {
            registar(e);
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            registar(e);
            throw e;
        }

        /**
         * Um único problema costuma gerar vários avisos na mesma posição (por exemplo, valor
         * inválido e atributo inválido). Fica só o primeiro, que é o motivo real.
         */
        private void registar(SAXParseException e) {
            ErroXml novo = new ErroXml(e.getLineNumber(), e.getColumnNumber(), e.getMessage());
            boolean mesmaPosicao = !erros.isEmpty()
                    && erros.get(erros.size() - 1).linha() == novo.linha()
                    && erros.get(erros.size() - 1).coluna() == novo.coluna();
            if (!mesmaPosicao) {
                erros.add(novo);
            }
        }
    }
}
