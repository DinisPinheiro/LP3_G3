package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.StockDAOMemoria;
import com.hotel4you.hotel4you.Model.Artigo;
import com.hotel4you.hotel4you.Model.Utilizador;
import com.hotel4you.hotel4you.Model.encomenda.EncomendaFornecedor;
import com.hotel4you.hotel4you.Model.encomenda.LinhaEncomenda;
import com.hotel4you.hotel4you.Model.encomenda.TotaisEncomenda;
import com.hotel4you.hotel4you.Model.enums.Perfil;
import com.hotel4you.hotel4you.Utils.XmlInvalidoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * S5-04 (stock completo): cada linha tem produto, quantidade, unidade (caixas e unidades), preço unitário, taxa e
 * total; cada encomenda tem o total das linhas, das taxas e o total com taxas; o stock do hotel fica separado do
 * stock disponível. O stock é lido do {@code ProductHero.xml}.
 */
class GestaoStockCompletoControllerTest {

    private StockDAOMemoria dao;
    private GestaoStockController controller;

    @BeforeEach
    void preparar() {
        dao = new StockDAOMemoria();
        Sessao sessao = new Sessao();
        sessao.iniciar(Utilizador.novo("gestor", "gestor123", "Gestor Inicial", Perfil.GESTOR));
        controller = new GestaoStockController(dao, sessao);
    }

    private static Path recurso(String nome) throws URISyntaxException {
        return Paths.get("src", "test", "resources", "xml", nome);
    }

    /** O ProductHero.xml sem o {@code OrderSummary} (se tiver) e com o resumo indicado (se houver). */
    private static String xmlComResumo(String linhas, String taxas, String total) throws Exception {
        String xml = Files.readString(recurso("ProductHero.xml"), StandardCharsets.UTF_8)
                .replaceAll("(?s)\\s*<OrderSummary>.*?</OrderSummary>", "");
        if (linhas == null) {
            return xml;
        }
        String resumo = "<OrderSummary>"
                + "<TotalLinesAmount><CurrencyValue CurrencyType=\"EUR\">" + linhas + "</CurrencyValue></TotalLinesAmount>"
                + "<TotalTaxAmount><CurrencyValue CurrencyType=\"EUR\">" + taxas + "</CurrencyValue></TotalTaxAmount>"
                + "<OrderTotalAmount><CurrencyValue CurrencyType=\"EUR\">" + total + "</CurrencyValue></OrderTotalAmount>"
                + "</OrderSummary>";
        return xml.replace("</Order>", resumo + "</Order>");
    }

    private EncomendaFornecedor ler(Path pasta, String xml) throws Exception {
        return controller.lerEncomendaXml(Files.writeString(pasta.resolve("encomenda.xml"), xml, StandardCharsets.UTF_8));
    }

    private Artigo artigo(String inicio) {
        return controller.consultarStock().stream().filter(a -> a.getNome().startsWith(inicio)).findFirst().orElseThrow();
    }

    // ---- cada linha: unidade (caixas e unidades), preço, taxa e total ---------------------------

    @Test
    void linhasDoXMLTemCaixasUnidadesPrecoTaxaETotal() throws Exception {
        EncomendaFornecedor encomenda = controller.lerEncomendaXml(recurso("ProductHero.xml"));
        LinhaEncomenda toalhas = encomenda.linhas().get(0);
        assertEquals(50, toalhas.caixas().getAsInt());
        assertEquals(500, toalhas.unidades().getAsInt());
        assertEquals(new BigDecimal("2.51"), toalhas.precoUnitario().preco().valor());
        assertEquals(23, toalhas.imposto().percentagem());
        assertEquals(new BigDecimal("154.37"), toalhas.totalCalculado());

        LinhaEncomenda champo = encomenda.linhas().get(1);
        assertEquals(10, champo.caixas().getAsInt());
        assertEquals(200, champo.unidades().getAsInt());
        assertEquals(new BigDecimal("858.54"), champo.totalCalculado());
    }

    @Test
    void entradaGuardaOUltimoPrecoETaxaNoArtigo() throws Exception {
        controller.darEntrada(controller.lerEncomendaXml(recurso("ProductHero.xml")));

        Artigo toalhas = artigo("Toalha");
        assertEquals(new BigDecimal("2.51"), toalhas.getPrecoUnitario());
        assertEquals("cx", toalhas.getUnidadePreco());
        assertEquals(23, toalhas.getTaxaPercentagem());

        Artigo champo = artigo("Rituals");
        assertEquals(new BigDecimal("3.49"), champo.getPrecoUnitario());
        assertEquals("un", champo.getUnidadePreco());
    }

    @Test
    void segundaEntradaAtualizaOPrecoESomaOStock(@TempDir Path pasta) throws Exception {
        controller.darEntrada(controller.lerEncomendaXml(recurso("ProductHero.xml")));
        String maisCaro = xmlComResumo(null, null, null).replace("<CurrencyValue CurrencyType=\"EUR\">29.99</CurrencyValue>",
                "<CurrencyValue CurrencyType=\"EUR\">31.50</CurrencyValue>");
        controller.darEntrada(ler(pasta, maisCaro));

        Artigo secador = artigo("Secador");
        assertEquals(100, secador.getQuantidadeAtual());
        assertEquals(new BigDecimal("31.50"), secador.getPrecoUnitario(), "fica o preço da última entrada");
        assertEquals(2, dao.getEncomendas().size());
    }

    // ---- cada encomenda: total das linhas, das taxas e total com taxas --------------------------

    @Test
    void encomendaTemTotalDasLinhasDasTaxasETotalComTaxas() throws Exception {
        EncomendaFornecedor encomenda = controller.lerEncomendaXml(recurso("ProductHero.xml"));
        TotaisEncomenda totais = encomenda.totais();
        assertEquals(new BigDecimal("14138.00"), totais.totalLinhas());
        assertEquals(new BigDecimal("3251.75"), totais.totalTaxas());
        assertEquals(new BigDecimal("17389.75"), totais.totalComTaxas());
        assertEquals("EUR", totais.moeda());
    }

    @Test
    void entradaGuardaAEncomendaComOsSeusTotais() throws Exception {
        controller.darEntrada(controller.lerEncomendaXml(recurso("ProductHero.xml")));
        assertEquals(1, dao.getEncomendas().size());
        EncomendaFornecedor guardada = dao.getEncomendas().get(0);
        assertEquals("OC56974", guardada.numero());
        assertEquals(5, guardada.linhas().size());
        assertEquals(new BigDecimal("17389.75"), guardada.totais().totalComTaxas());
    }

    @Test
    void valorDoXMLDiferenteDoCalculadoGeraAvisoMasNaoImpedeAEntrada() throws Exception {
        // no ProductHero.xml o imposto da linha 2 está como 160,51 (o certo é 160,54)
        EncomendaFornecedor encomenda = controller.lerEncomendaXml(recurso("ProductHero.xml"));
        assertTrue(encomenda.avisos().stream().anyMatch(a -> a.startsWith("Linha 2: imposto")));
        assertEquals(5, controller.darEntrada(encomenda));
    }

    // ---- OrderSummary (campo novo do XML/XSD) ---------------------------------------------------

    @Test
    void xmlSemResumoContinuaValido(@TempDir Path pasta) throws Exception {
        EncomendaFornecedor encomenda = ler(pasta, xmlComResumo(null, null, null));
        assertEquals(null, encomenda.resumoXml());
    }

    @Test
    void resumoDoXMLCertoEELidoSemAvisosDeEncomenda(@TempDir Path pasta) throws Exception {
        EncomendaFornecedor encomenda = ler(pasta, xmlComResumo("14138.00", "3251.75", "17389.75"));
        assertEquals(new BigDecimal("17389.75"), encomenda.resumoXml().totalComTaxas());
        assertTrue(encomenda.avisos().stream().noneMatch(a -> a.startsWith("Encomenda:")));
    }

    @Test
    void resumoDoXMLErradoGeraAvisoDeEncomenda(@TempDir Path pasta) throws Exception {
        EncomendaFornecedor encomenda = ler(pasta, xmlComResumo("14138.00", "3251.75", "17000.00"));
        List<String> avisos = encomenda.avisos().stream().filter(a -> a.startsWith("Encomenda:")).toList();
        assertEquals(1, avisos.size());
        assertTrue(avisos.get(0).contains("total com taxas"));
    }

    @Test
    void resumoIncompletoEInvalidoPeloXSD(@TempDir Path pasta) throws Exception {
        String semTotal = xmlComResumo("14138.00", "3251.75", "17389.75")
                .replaceAll("(?s)<OrderTotalAmount>.*?</OrderTotalAmount>", "");
        assertThrows(XmlInvalidoException.class, () -> ler(pasta, semTotal));
    }

    // ---- valores que o XSD aceita mas não fazem sentido -----------------------------------------

    @Test
    void precoNegativoERejeitadoENadaFicaGuardado(@TempDir Path pasta) throws Exception {
        String xml = xmlComResumo(null, null, null).replace("<CurrencyValue CurrencyType=\"EUR\">2.51</CurrencyValue>",
                "<CurrencyValue CurrencyType=\"EUR\">-2.51</CurrencyValue>");
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> ler(pasta, xml));
        assertTrue(erro.getMessage().startsWith("Linha 1"));
        assertTrue(controller.consultarStock().isEmpty());
        assertTrue(dao.getEncomendas().isEmpty());
    }

    @Test
    void taxaAcimaDeCemERejeitada(@TempDir Path pasta) throws Exception {
        String xml = xmlComResumo(null, null, null).replaceFirst("<TaxPercent>23</TaxPercent>",
                "<TaxPercent>150</TaxPercent>");
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> ler(pasta, xml));
        assertTrue(erro.getMessage().contains("taxa"));
    }

    @Test
    void numeroDeEncomendaMuitoLongoERejeitado(@TempDir Path pasta) throws Exception {
        String xml = xmlComResumo(null, null, null).replace("OC56974", "OC" + "9".repeat(40));
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> ler(pasta, xml));
        assertTrue(erro.getMessage().contains("número da encomenda"));
    }

    // ---- o stock do hotel fica separado do stock disponível -------------------------------------

    @Test
    void gestaoDeStockMostraOStockDoHotelEODisponivel() throws Exception {
        controller.darEntrada(controller.lerEncomendaXml(recurso("ProductHero.xml")));
        String nome = artigo("Secador").getNome();
        dao.definirReservado(nome, 12);

        Artigo secador = artigo("Secador");
        assertEquals(50, secador.getQuantidadeAtual(), "stock do hotel");
        assertEquals(38, secador.getQuantidadeDisponivel(), "stock disponível: o do hotel menos o usado em reservas");
        assertEquals(50, artigo("Cofre").getQuantidadeDisponivel(), "sem reservas o disponível é o do hotel");
    }

    @Test
    void entradaNovaNaoPerdeOUsadoEmReservas() throws Exception {
        controller.darEntrada(controller.lerEncomendaXml(recurso("ProductHero.xml")));
        dao.definirReservado(artigo("Secador").getNome(), 12);
        controller.darEntrada(controller.lerEncomendaXml(recurso("ProductHero.xml")));

        Artigo secador = artigo("Secador");
        assertEquals(100, secador.getQuantidadeAtual());
        assertEquals(88, secador.getQuantidadeDisponivel());
    }
}
