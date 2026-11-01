package com.hotel4you.hotel4you.Utils;

import com.hotel4you.hotel4you.Model.encomenda.EncomendaFornecedor;
import com.hotel4you.hotel4you.Model.encomenda.FornecedorEncomenda;
import com.hotel4you.hotel4you.Model.encomenda.ImpostoLinha;
import com.hotel4you.hotel4you.Model.encomenda.LinhaEncomenda;
import com.hotel4you.hotel4you.Model.encomenda.Montante;
import com.hotel4you.hotel4you.Model.encomenda.PrecoUnitario;
import com.hotel4you.hotel4you.Model.encomenda.Quantidade;
import com.hotel4you.hotel4you.Model.enums.UnidadeMedida;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Lê um ficheiro XML de encomenda de um fornecedor e converte-o em objetos Java
 * ({@link EncomendaFornecedor}).
 * <p>
 * O ficheiro é sempre validado primeiro com o {@link ValidadorXml}; só um XML válido é lido.
 * Esta classe não guarda nada na base de dados (esta sprint não há entrada em stock).
 */
public final class LeitorEncomendaXml {

    private static final String RECUSAR_DOCTYPE = "http://apache.org/xml/features/disallow-doctype-decl";

    private LeitorEncomendaXml() {
    }

    /**
     * Valida e lê um ficheiro XML de encomenda.
     *
     * @param ficheiro caminho do ficheiro XML
     * @return a encomenda com todos os dados do ficheiro
     * @throws IOException           se o ficheiro não puder ser lido
     * @throws XmlInvalidoException  se o XML não cumprir o XSD (traz a linha e o motivo de cada erro)
     */
    public static EncomendaFornecedor ler(Path ficheiro) throws IOException {
        byte[] conteudo = Files.readAllBytes(ficheiro);

        ResultadoValidacao resultado = ValidadorXml.validar(new ByteArrayInputStream(conteudo));
        if (!resultado.valido()) {
            throw new XmlInvalidoException(resultado.erros());
        }
        return converter(construirDocumento(conteudo).getDocumentElement());
    }

    private static Document construirDocumento(byte[] conteudo) throws IOException {
        try {
            DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
            fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            fabrica.setFeature(RECUSAR_DOCTYPE, true);
            fabrica.setExpandEntityReferences(false);
            return fabrica.newDocumentBuilder().parse(new ByteArrayInputStream(conteudo));
        } catch (ParserConfigurationException | SAXException e) {
            // Não deve acontecer: o XML já foi validado.
            throw new IllegalStateException("Não foi possível ler o XML validado.", e);
        }
    }

    private static EncomendaFornecedor converter(Element ordem) {
        Element cabecalho = filho(ordem, "Header");

        List<LinhaEncomenda> linhas = new ArrayList<>();
        for (Element linha : filhos(ordem, "Line")) {
            linhas.add(converterLinha(linha));
        }
        return new EncomendaFornecedor(
                texto(cabecalho, "OrderNumber"),
                converterData(filho(filho(cabecalho, "OrderDate"), "Date")),
                converterFornecedor(filho(cabecalho, "SupplierParty")),
                linhas);
    }

    private static LocalDate converterData(Element data) {
        int ano = inteiro(data, "Year");
        int mes = inteiro(data, "Month");
        int dia = inteiro(data, "Day");
        try {
            return LocalDate.of(ano, mes, dia);
        } catch (DateTimeException e) {
            // O XSD aceita qualquer número nestes campos, por isso o dia/mês impossível só se vê aqui.
            throw new XmlInvalidoException(List.of(new ErroXml(0, 0,
                    "Data da encomenda inválida: " + ano + "-" + mes + "-" + dia)));
        }
    }

    private static FornecedorEncomenda converterFornecedor(Element parte) {
        Element morada = filho(parte, "NameAddress");
        Element pais = filho(morada, "Country");
        return new FornecedorEncomenda(
                texto(parte, "PartyIdentifier"),
                texto(morada, "Name"),
                texto(morada, "Address1"),
                texto(morada, "Address2"),
                texto(morada, "City"),
                texto(morada, "PostalCode"),
                pais.getAttribute("ISOCountryCode"),
                pais.getTextContent().trim());
    }

    private static LinhaEncomenda converterLinha(Element linha) {
        Element produto = filho(linha, "Product");
        Element codigo = filho(produto, "ProductIdentifier");

        List<Quantidade> informativas = new ArrayList<>();
        for (Element informativa : filhos(linha, "InformationalQuantity")) {
            informativas.add(converterQuantidade(informativa));
        }
        return new LinhaEncomenda(
                inteiro(linha, "LineNumber"),
                codigo.getTextContent().trim(),
                codigo.getAttribute("Agency"),
                texto(produto, "ProductDescription"),
                converterQuantidade(filho(linha, "Quantity")),
                informativas,
                converterPreco(filho(linha, "PricePerUnit")),
                converterImposto(filho(linha, "MonetaryAdjustment")),
                converterMontante(filho(linha, "LineBaseAmount")));
    }

    private static Quantidade converterQuantidade(Element quantidade) {
        Element valor = filho(quantidade, "Value");
        return new Quantidade(
                quantidade.getAttribute("QuantityType"),
                Integer.parseInt(valor.getTextContent().trim()),
                UnidadeMedida.deXml(valor.getAttribute("UOM")));
    }

    private static PrecoUnitario converterPreco(Element preco) {
        Element valor = filho(preco, "Value");
        return new PrecoUnitario(
                converterMontante(preco),
                Integer.parseInt(valor.getTextContent().trim()),
                UnidadeMedida.deXml(valor.getAttribute("UOM")));
    }

    private static ImpostoLinha converterImposto(Element ajuste) {
        Element imposto = filho(ajuste, "TaxAdjustment");
        return new ImpostoLinha(
                ajuste.getAttribute("AdjustmentType"),
                inteiro(ajuste, "MonetaryAdjustmentLine"),
                converterMontante(filho(ajuste, "MonetaryAdjustmentStartAmount")),
                imposto.getAttribute("TaxType"),
                inteiro(imposto, "TaxPercent"),
                converterMontante(filho(imposto, "TaxAmount")),
                texto(imposto, "TaxLocation"));
    }

    /** Lê o {@code CurrencyValue} que está dentro do elemento indicado. */
    private static Montante converterMontante(Element pai) {
        Element valor = filho(pai, "CurrencyValue");
        return new Montante(new BigDecimal(valor.getTextContent().trim()), valor.getAttribute("CurrencyType"));
    }

    private static Element filho(Element pai, String nome) {
        List<Element> encontrados = filhos(pai, nome);
        if (encontrados.isEmpty()) {
            // Não deve acontecer: o XML já foi validado.
            throw new IllegalStateException("Falta o elemento <" + nome + "> dentro de <" + pai.getTagName() + ">.");
        }
        return encontrados.get(0);
    }

    private static List<Element> filhos(Element pai, String nome) {
        List<Element> encontrados = new ArrayList<>();
        for (Node no = pai.getFirstChild(); no != null; no = no.getNextSibling()) {
            if (no instanceof Element elemento && elemento.getTagName().equals(nome)) {
                encontrados.add(elemento);
            }
        }
        return encontrados;
    }

    private static String texto(Element pai, String nome) {
        return filho(pai, nome).getTextContent().trim();
    }

    private static int inteiro(Element pai, String nome) {
        return Integer.parseInt(texto(pai, nome));
    }
}
