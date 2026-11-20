package com.hotel4you.hotel4you.Utils;

import com.hotel4you.hotel4you.Model.encomenda.EncomendaFornecedor;
import com.hotel4you.hotel4you.Model.encomenda.LinhaEncomenda;
import com.hotel4you.hotel4you.Model.enums.UnidadeMedida;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes da importação do XML do fornecedor: leitura de um XML válido e recusa de XML inválido,
 * mal formado ou perigoso. Só usa ficheiros de {@code src/test/resources/xml}; não toca na base de dados.
 */
class LeitorEncomendaXmlTest {

    private String xmlValido;

    @BeforeEach
    void preparar() throws Exception {
        xmlValido = new String(Files.readAllBytes(recurso("ProductHero.xml")), StandardCharsets.UTF_8);
    }

    private static Path recurso(String nome) throws URISyntaxException {
        return Paths.get("src", "test", "resources", "xml", nome);
    }

    private static EncomendaFornecedor ler(String xml) throws IOException {
        return LeitorEncomendaXml.ler(xml.getBytes(StandardCharsets.UTF_8), ValidadorXml.schemaPadrao());
    }

    @Test
    void xmlValidoDaOCabecalhoEOFornecedor() throws Exception {
        EncomendaFornecedor encomenda = LeitorEncomendaXml.ler(recurso("ProductHero.xml"));

        assertEquals("OC56974", encomenda.numero());
        assertEquals(LocalDate.of(2026, 10, 5), encomenda.data());
        assertEquals("11PT920", encomenda.fornecedor().identificador());
        assertEquals("ProductHero", encomenda.fornecedor().nome());
        assertEquals("Rua da Alegria", encomenda.fornecedor().morada1());
        assertEquals("Porto", encomenda.fornecedor().cidade());
        assertEquals("4000-032", encomenda.fornecedor().codigoPostal());
        assertEquals("PT", encomenda.fornecedor().codigoPais());
        assertEquals("Portugal", encomenda.fornecedor().pais());
    }

    @Test
    void xmlValidoDaAsLinhasComQuantidadePrecoEImposto() throws Exception {
        EncomendaFornecedor encomenda = LeitorEncomendaXml.ler(recurso("ProductHero.xml"));

        assertEquals(5, encomenda.linhas().size());
        LinhaEncomenda primeira = encomenda.linhas().get(0);
        assertEquals(1, primeira.numero());
        assertEquals("PZ-1989", primeira.codigoProduto());
        assertEquals("Supplier", primeira.agencia());
        assertEquals("Toalha de banho 400g cinza", primeira.descricao());
        assertEquals(50, primeira.quantidade().valor());
        assertEquals(UnidadeMedida.CAIXA, primeira.quantidade().unidade());
        assertEquals(2, primeira.quantidadesInformativas().size());
        assertEquals(0, new BigDecimal("2.51").compareTo(primeira.precoUnitario().preco().valor()));
        assertEquals("EUR", primeira.precoUnitario().preco().moeda());
        assertEquals(23, primeira.imposto().percentagem());
        assertEquals(0, new BigDecimal("28.87").compareTo(primeira.imposto().valorImposto().valor()));
        assertEquals(0, new BigDecimal("154.37").compareTo(primeira.valorBase().valor()));
    }

    @Test
    void xmlValidoDaOResumoDaEncomendaQuandoExiste() throws Exception {
        EncomendaFornecedor encomenda = ler(xmlValido);

        assertEquals(0, new BigDecimal("17389.75").compareTo(encomenda.resumoXml().totalComTaxas()));
        assertEquals(0, new BigDecimal("3251.75").compareTo(encomenda.resumoXml().totalTaxas()));
        assertEquals("EUR", encomenda.resumoXml().moeda());
    }

    @Test
    void xmlSemResumoELidoSemTotaisDoXml() throws Exception {
        String semResumo = xmlValido.replaceAll("(?s)<OrderSummary>.*?</OrderSummary>", "");
        assertFalse(semResumo.contains("OrderSummary"), "o teste só faz sentido sem o resumo");

        EncomendaFornecedor encomenda = ler(semResumo);

        assertNull(encomenda.resumoXml());
        assertEquals(5, encomenda.linhas().size());
    }

    @Test
    void xmlQueNaoCumpreOXsdLancaErrosComLinhaEMotivo() throws Exception {
        XmlInvalidoException erro = assertThrows(XmlInvalidoException.class,
                () -> LeitorEncomendaXml.ler(recurso("ProductHero_invalido.xml")));

        assertFalse(erro.getErros().isEmpty());
        for (ErroXml e : erro.getErros()) {
            assertTrue(e.linha() > 0, "o erro indica a linha");
            assertFalse(e.motivo().isBlank(), "o erro indica o motivo");
        }
        assertFalse(erro.getMessage().isBlank());
    }

    @Test
    void xmlSemElementoObrigatorioERejeitado() {
        String semNumero = xmlValido.replaceAll("<OrderNumber>.*?</OrderNumber>", "");
        assertNotEquals(xmlValido, semNumero);

        assertThrows(XmlInvalidoException.class, () -> ler(semNumero));
    }

    @Test
    void xmlMalFormadoOuTruncadoERejeitado() {
        String truncado = xmlValido.substring(0, xmlValido.length() / 2);

        assertThrows(XmlInvalidoException.class, () -> ler(truncado));
        assertThrows(XmlInvalidoException.class, () -> ler("isto não é XML"));
    }

    @Test
    void ficheiroVazioERejeitado() {
        assertThrows(XmlInvalidoException.class, () -> ler(""));
    }

    @Test
    void xmlComDoctypeERecusadoPorSegurancaEntidadesExternas() {
        String comDoctype = xmlValido.replaceFirst("(<\\?xml[^>]*\\?>)",
                "$1<!DOCTYPE Order [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>");
        assertTrue(comDoctype.contains("DOCTYPE"));

        assertThrows(XmlInvalidoException.class, () -> ler(comDoctype));
    }

    @Test
    void dataImpossivelNaoDaEntradaNumaEncomendaValida() {
        String dataImpossivel = xmlValido
                .replaceFirst("<Month>10</Month>", "<Month>02</Month>")
                .replaceFirst("<Day>05</Day>", "<Day>31</Day>");
        assertNotEquals(xmlValido, dataImpossivel);

        XmlInvalidoException erro = assertThrows(XmlInvalidoException.class, () -> ler(dataImpossivel));

        assertFalse(erro.getMessage().isBlank());
    }

    @Test
    void ficheiroQueNaoExisteLancaIOException() {
        assertThrows(IOException.class, () -> LeitorEncomendaXml.ler(Paths.get("nao-existe-mesmo.xml")));
    }

    @Test
    void aListaDeLinhasNaoPodeSerAlteradaPorQuemRecebeAEncomenda() throws Exception {
        EncomendaFornecedor encomenda = ler(xmlValido);

        assertThrows(UnsupportedOperationException.class, () -> encomenda.linhas().clear());
    }
}