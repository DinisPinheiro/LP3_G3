package com.hotel4you.hotel4you.Utils;

import com.hotel4you.hotel4you.Model.encomenda.EncomendaFornecedor;
import com.hotel4you.hotel4you.Model.encomenda.FornecedorEncomenda;
import com.hotel4you.hotel4you.Model.encomenda.ImpostoLinha;
import com.hotel4you.hotel4you.Model.encomenda.LinhaEncomenda;
import com.hotel4you.hotel4you.Model.encomenda.Montante;
import com.hotel4you.hotel4you.Model.encomenda.PrecoUnitario;
import com.hotel4you.hotel4you.Model.encomenda.Quantidade;
import com.hotel4you.hotel4you.Model.encomenda.TotaisEncomenda;
import com.hotel4you.hotel4you.Model.enums.FormatoFicheiro;
import com.hotel4you.hotel4you.Model.enums.UnidadeMedida;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Lê um ficheiro XML de encomenda de um fornecedor e converte-o em objetos Java
 * ({@link EncomendaFornecedor}).
 * <p>
 * O ficheiro é sempre validado primeiro com o {@link ValidadorXml}; só um XML válido é lido.
 * Esta classe não guarda nada na base de dados: só converte o XML em objetos. É o leitor do formato
 * {@link FormatoFicheiro#XML}; o do JSON é o {@link LeitorEncomendaJson}.
 */
public final class LeitorEncomendaXml implements LeitorEncomenda {

    private static final String RECUSAR_DOCTYPE = "http://apache.org/xml/features/disallow-doctype-decl";

    private final Schema schema;

    /**
     * @param schema XSD contra o qual validar os XML (o que o fornecedor importou, S6-02)
     */
    public LeitorEncomendaXml(Schema schema) {
        this.schema = Objects.requireNonNull(schema, "O XSD é obrigatório.");
    }

    @Override
    public FormatoFicheiro formato() {
        return FormatoFicheiro.XML;
    }

    @Override
    public EncomendaFornecedor ler(byte[] conteudo) throws IOException {
        return ler(conteudo, schema);
    }

    /**
     * Valida um XML de encomenda contra o XSD indicado e, se for válido, lê os seus dados.
     *
     * @param conteudo bytes do ficheiro XML
     * @param schema   XSD contra o qual validar (o que o fornecedor importou, S6-02)
     * @return a encomenda com todos os dados do ficheiro
     * @throws IOException           se o conteúdo não puder ser lido
     * @throws XmlInvalidoException  se o XML não cumprir o XSD ou, mesmo cumprindo-o, não tiver a
     *                               estrutura de encomenda que a aplicação sabe ler
     */
    public static EncomendaFornecedor ler(byte[] conteudo, Schema schema) throws IOException {
        ResultadoValidacao resultado = ValidadorXml.validar(new ByteArrayInputStream(conteudo), schema);
        if (!resultado.valido()) {
            throw new XmlInvalidoException(resultado.erros());
        }
        try {
            return converter(construirDocumento(conteudo).getDocumentElement());
        } catch (IllegalStateException | NumberFormatException e) {
            // Um XSD importado pelo fornecedor pode aceitar ficheiros com outra estrutura: aqui falta algo
            // que a aplicação precisa para dar entrada em stock.
            throw new XmlInvalidoException(List.of(new ErroXml(0, 0,
                    "O ficheiro cumpre o XSD, mas não tem a estrutura de encomenda que a aplicação lê: "
                            + e.getMessage())));
        }
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
                linhas,
                converterResumo(filhos(ordem, "OrderSummary")));
    }

    /** Lê os totais do elemento opcional {@code OrderSummary}; devolve {@code null} se o XML não os tiver. */
    private static TotaisEncomenda converterResumo(List<Element> resumos) {
        if (resumos.isEmpty()) {
            return null;
        }
        Element resumo = resumos.get(0);
        Montante linhas = converterMontante(filho(resumo, "TotalLinesAmount"));
        Montante taxas = converterMontante(filho(resumo, "TotalTaxAmount"));
        Montante total = converterMontante(filho(resumo, "OrderTotalAmount"));
        return new TotaisEncomenda(linhas.valor(), taxas.valor(), total.valor(), total.moeda());
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
