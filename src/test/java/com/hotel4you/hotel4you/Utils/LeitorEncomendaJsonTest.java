package com.hotel4you.hotel4you.Utils;

import com.hotel4you.hotel4you.Model.encomenda.EncomendaFornecedor;
import com.hotel4you.hotel4you.Model.encomenda.LinhaEncomenda;
import com.hotel4you.hotel4you.Model.encomenda.TotaisEncomenda;
import com.hotel4you.hotel4you.Model.enums.FormatoFicheiro;
import com.hotel4you.hotel4you.Model.enums.UnidadeMedida;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes da S7-03: leitura do JSON do fornecedor (o exemplo do professor, {@code LIndustrie.json}) e recusa de
 * JSON mal formado, incompleto ou com valores de tipo errado. Só usa ficheiros de
 * {@code src/test/resources/json}; não toca na base de dados.
 */
class LeitorEncomendaJsonTest {

    private final LeitorEncomendaJson leitor = new LeitorEncomendaJson();

    private static Path recurso(String nome) {
        return Paths.get("src", "test", "resources", "json", nome);
    }

    private EncomendaFornecedor lerFicheiro(String nome) throws IOException {
        return leitor.ler(Files.readAllBytes(recurso(nome)));
    }

    private EncomendaFornecedor ler(String json) throws IOException {
        return leitor.ler(json.getBytes(StandardCharsets.UTF_8));
    }

    /** Lê o JSON e devolve os motivos dos erros (que têm de existir). */
    private List<String> erros(String json) {
        XmlInvalidoException erro = assertThrows(XmlInvalidoException.class, () -> ler(json));
        return erro.getErros().stream().map(ErroXml::motivo).collect(Collectors.toList());
    }

    /** O JSON do professor com uma troca de texto, para testar um erro de cada vez. */
    private static String exemploCom(String velho, String novo) throws IOException {
        String texto = Files.readString(recurso("LIndustrie.json"), StandardCharsets.UTF_8);
        assertTrue(texto.contains(velho), "o exemplo tem de conter: " + velho);
        return texto.replace(velho, novo);
    }

    private static boolean algumContem(List<String> motivos, String... partes) {
        return motivos.stream().anyMatch(m -> {
            for (String parte : partes) {
                if (!m.contains(parte)) {
                    return false;
                }
            }
            return true;
        });
    }

    // ---- JSON válido ----------------------------------------------------------------------------------

    @Test
    void leitorDeJsonDizQueEJson() {
        assertEquals(FormatoFicheiro.JSON, leitor.formato());
    }

    @Test
    void jsonValidoDaOCabecalhoEOFornecedor() throws Exception {
        EncomendaFornecedor encomenda = lerFicheiro("LIndustrie.json");

        assertEquals("X012X", encomenda.numero());
        assertEquals(LocalDate.of(2026, 11, 6), encomenda.data());
        assertEquals("964070328", encomenda.fornecedor().identificador());
        assertEquals("L'industrie Alimentaire", encomenda.fornecedor().nome());
        assertEquals("Rue internationale de Paris 96", encomenda.fornecedor().morada1());
        assertEquals("", encomenda.fornecedor().morada2(), "Address2 é opcional");
        assertEquals("Paris", encomenda.fornecedor().cidade());
        assertEquals("75001", encomenda.fornecedor().codigoPostal());
        assertEquals("FR", encomenda.fornecedor().codigoPais());
        assertEquals("France", encomenda.fornecedor().pais());
        assertNull(encomenda.resumoXml(), "o exemplo não traz OrderSummary (é opcional)");
    }

    @Test
    void jsonValidoDaAsLinhasComQuantidadePrecoEImposto() throws Exception {
        EncomendaFornecedor encomenda = lerFicheiro("LIndustrie.json");

        assertEquals(3, encomenda.linhas().size());
        LinhaEncomenda primeira = encomenda.linhas().get(0);
        assertEquals(1, primeira.numero());
        assertEquals("63279196", primeira.codigoProduto());
        assertEquals("Supplier", primeira.agencia());
        assertEquals("Croissant Brioche délifrance", primeira.descricao());
        assertEquals(10000, primeira.quantidade().valor());
        assertEquals(UnidadeMedida.UNIDADE, primeira.quantidade().unidade());
        assertEquals(1, primeira.quantidadesInformativas().size());
        assertEquals(600, primeira.quantidadesInformativas().get(0).valor());
        assertEquals(UnidadeMedida.QUILOGRAMA, primeira.quantidadesInformativas().get(0).unidade());
        assertEquals(0, new BigDecimal("0.25").compareTo(primeira.precoUnitario().preco().valor()));
        assertEquals("EUR", primeira.precoUnitario().preco().moeda());
        assertEquals(1, primeira.precoUnitario().porQuantidade());
        assertEquals(UnidadeMedida.UNIDADE, primeira.precoUnitario().unidade());
        assertEquals("Tax", primeira.imposto().tipoAjuste());
        assertEquals("VAT", primeira.imposto().tipoImposto());
        assertEquals(20, primeira.imposto().percentagem());
        assertEquals(0, new BigDecimal("500").compareTo(primeira.imposto().valorImposto().valor()));
        assertEquals("FR", primeira.imposto().local());
        assertEquals(0, new BigDecimal("3000").compareTo(primeira.valorBase().valor()));
    }

    @Test
    void duasLinhasComAMesmaDescricaoMasCodigosDiferentesSaoLinhasDistintas() throws Exception {
        EncomendaFornecedor encomenda = lerFicheiro("LIndustrie.json");

        assertEquals("13576129", encomenda.linhas().get(1).codigoProduto());
        assertEquals("659985100", encomenda.linhas().get(2).codigoProduto());
        assertEquals(encomenda.linhas().get(1).descricao(), encomenda.linhas().get(2).descricao());
    }

    @Test
    void valoresDecimaisNaoPerdemCentimos() throws Exception {
        EncomendaFornecedor encomenda = lerFicheiro("LIndustrie.json");

        // 42.40 e 254.40 são lidos como BigDecimal, nunca como double (que daria 42.4000000000000021...).
        assertEquals(0, new BigDecimal("42.40").compareTo(encomenda.linhas().get(1).imposto().valorImposto().valor()));
        assertEquals(0, new BigDecimal("254.40").compareTo(encomenda.linhas().get(1).valorBase().valor()));
        assertEquals(0, new BigDecimal("0.1").compareTo(
                ler(exemploCom("\"Value\": 0.25", "\"Value\": 0.1")).linhas().get(0).precoUnitario().preco().valor()));
    }

    @Test
    void jsonDoExemploTemOsValoresCertosENaoDaAvisos() throws Exception {
        EncomendaFornecedor encomenda = lerFicheiro("LIndustrie.json");

        assertTrue(encomenda.avisos().isEmpty());
        TotaisEncomenda totais = encomenda.totais();
        assertEquals(0, new BigDecimal("2924.00").compareTo(totais.totalLinhas()));
        assertEquals(0, new BigDecimal("584.80").compareTo(totais.totalTaxas()));
        assertEquals(0, new BigDecimal("3508.80").compareTo(totais.totalComTaxas()));
        encomenda.validarValores();
    }

    @Test
    void jsonComOsMesmosDadosQueOXmlDaAMesmaEncomenda() throws Exception {
        EncomendaFornecedor doXml = RecursosDeTeste.ler(Paths.get("src", "test", "resources", "xml", "ProductHero.xml"));
        EncomendaFornecedor doJson = lerFicheiro("ProductHero.json");

        assertEquals(doXml.numero(), doJson.numero());
        assertEquals(doXml.data(), doJson.data());
        assertEquals(doXml.fornecedor(), doJson.fornecedor());
        assertEquals(doXml.linhas().size(), doJson.linhas().size());
        for (int i = 0; i < doXml.linhas().size(); i++) {
            assertMesmaLinha(doXml.linhas().get(i), doJson.linhas().get(i));
        }
        assertEquals(doXml.avisos(), doJson.avisos(), "os mesmos avisos de valores");
        assertMesmoValor(doXml.resumoXml().totalComTaxas(), doJson.resumoXml().totalComTaxas());
        assertMesmoValor(doXml.totais().totalComTaxas(), doJson.totais().totalComTaxas());
    }

    /** Tudo igual; os valores em dinheiro comparam-se pelo valor (42.40 e 42.4 são o mesmo valor). */
    private static void assertMesmaLinha(LinhaEncomenda xml, LinhaEncomenda json) {
        assertEquals(xml.numero(), json.numero());
        assertEquals(xml.codigoProduto(), json.codigoProduto());
        assertEquals(xml.agencia(), json.agencia());
        assertEquals(xml.descricao(), json.descricao());
        assertEquals(xml.quantidade(), json.quantidade());
        assertEquals(xml.quantidadesInformativas(), json.quantidadesInformativas());
        assertEquals(xml.precoUnitario().porQuantidade(), json.precoUnitario().porQuantidade());
        assertEquals(xml.precoUnitario().unidade(), json.precoUnitario().unidade());
        assertMesmoValor(xml.precoUnitario().preco().valor(), json.precoUnitario().preco().valor());
        assertEquals(xml.imposto().percentagem(), json.imposto().percentagem());
        assertEquals(xml.imposto().tipoImposto(), json.imposto().tipoImposto());
        assertEquals(xml.imposto().local(), json.imposto().local());
        assertMesmoValor(xml.imposto().baseCalculo().valor(), json.imposto().baseCalculo().valor());
        assertMesmoValor(xml.imposto().valorImposto().valor(), json.imposto().valorImposto().valor());
        assertMesmoValor(xml.valorBase().valor(), json.valorBase().valor());
    }

    private static void assertMesmoValor(BigDecimal esperado, BigDecimal obtido) {
        assertEquals(0, esperado.compareTo(obtido), "esperado " + esperado + " mas foi " + obtido);
    }

    @Test
    void camposDesconhecidosSaoIgnorados() throws Exception {
        EncomendaFornecedor encomenda = ler(exemploCom("\"Order\": {", "\"Order\": {\n    \"Notas\": \"qualquer coisa\","));

        assertEquals("X012X", encomenda.numero());
    }

    // ---- JSON mal formado --------------------------------------------------------------------------------

    @Test
    void jsonMalFormadoDizALinhaEAColuna() {
        XmlInvalidoException erro = assertThrows(XmlInvalidoException.class,
                () -> lerFicheiro("LIndustrie_mal_formado.json"));

        assertEquals(1, erro.getErros().size());
        ErroXml unico = erro.getErros().get(0);
        assertTrue(unico.linha() > 0, "indica a linha");
        assertTrue(unico.coluna() > 0, "indica a coluna");
        assertTrue(unico.motivo().startsWith("JSON mal formado"));
        assertTrue(unico.toString().startsWith("Linha " + unico.linha() + ", coluna " + unico.coluna()));
    }

    @Test
    void ficheiroVazioOuSoComEspacosERecusado() {
        assertTrue(erros("").get(0).contains("vazio"));
        assertTrue(erros("   \n ").get(0).contains("vazio"));
    }

    @Test
    void textoDepoisDoJsonERecusado() throws Exception {
        List<String> motivos = erros(exemploCom("\"Country\"", "\"Country\"") + " sobra");

        assertTrue(motivos.get(0).startsWith("JSON mal formado"));
    }

    @Test
    void chaveRepetidaERecusada() throws Exception {
        List<String> motivos = erros(exemploCom("\"OrderNumber\": \"X012X\",",
                "\"OrderNumber\": \"X012X\", \"OrderNumber\": \"OUTRO\","));

        assertTrue(motivos.get(0).startsWith("JSON mal formado"));
        assertTrue(motivos.get(0).contains("OrderNumber"));
    }

    @Test
    void ficheiroXmlEscolhidoComoJsonERecusadoComoMalFormado() throws Exception {
        byte[] xml = Files.readAllBytes(Paths.get("src", "test", "resources", "xml", "ProductHero.xml"));

        XmlInvalidoException erro = assertThrows(XmlInvalidoException.class, () -> leitor.ler(xml));

        assertTrue(erro.getErros().get(0).motivo().startsWith("JSON mal formado"));
    }

    // ---- estrutura e tipos ---------------------------------------------------------------------------------

    @Test
    void jsonDeExemploInvalidoDizOCaminhoDeCadaCampoErrado() {
        XmlInvalidoException erro = assertThrows(XmlInvalidoException.class,
                () -> lerFicheiro("LIndustrie_invalido.json"));
        List<String> motivos = erro.getErros().stream().map(ErroXml::motivo).collect(Collectors.toList());

        assertEquals(4, motivos.size(), "todos os erros de uma vez: " + motivos);
        assertTrue(algumContem(motivos, "Order.Header.OrderDate", "2026-13-45", "aaaa-mm-dd"));
        assertTrue(algumContem(motivos, "Order.Line[1].Quantity.Value.Value", "número inteiro", "dez mil"));
        assertTrue(algumContem(motivos, "Order.Line[2].Quantity.Value.UOM", "Pallet", "Box, Unit, Kilogram"));
        assertTrue(algumContem(motivos, "Order.Line[3].Product.ProductDescription", "não pode estar vazio"));
    }

    @Test
    void raizSemOrderOuQueNaoEObjetoERecusada() {
        assertEquals(List.of("Order: falta o campo obrigatório."), erros("{}"));
        assertEquals(List.of("Order: falta o campo obrigatório."), erros("[]"));
        assertEquals(List.of("Order: falta o campo obrigatório."), erros("42"));
        assertEquals(List.of("Order: tem de ser um objeto { ... }."), erros("{\"Order\": []}"));
    }

    @Test
    void campoObrigatorioEmFaltaDizOCaminho() throws Exception {
        List<String> motivos = erros(exemploCom("\"OrderNumber\": \"X012X\",", ""));

        assertEquals(List.of("Order.Header.OrderNumber: falta o campo obrigatório."), motivos);
    }

    @Test
    void campoComValorNuloContaComoEmFalta() throws Exception {
        List<String> motivos = erros(exemploCom("\"OrderNumber\": \"X012X\"", "\"OrderNumber\": null"));

        assertEquals(List.of("Order.Header.OrderNumber: falta o campo obrigatório."), motivos);
    }

    @Test
    void listaDeLinhasVaziaOuQueNaoELista() throws Exception {
        String semLinhas = "{\"Order\": {\"Header\": " + cabecalhoDoExemplo() + ", \"Line\": []}}";

        assertTrue(erros(semLinhas).contains("Order.Line: tem de ter pelo menos um elemento."));
        assertTrue(erros(semLinhas.replace("[]", "{}")).contains("Order.Line: tem de ser uma lista [ ... ]."));
        assertTrue(erros(semLinhas.replace("[]", "[1]")).contains("Order.Line[1]: tem de ser um objeto { ... }."));
    }

    private static String cabecalhoDoExemplo() throws Exception {
        String texto = Files.readString(recurso("LIndustrie.json"), StandardCharsets.UTF_8);
        int inicio = texto.indexOf("\"Header\":") + "\"Header\":".length();
        int fim = texto.indexOf("\"Line\":");
        String cabecalho = texto.substring(inicio, fim).trim();
        return cabecalho.substring(0, cabecalho.lastIndexOf(',')); // tira a vírgula antes de "Line"
    }

    @Test
    void quantidadeComCasasDecimaisOuForaDoIntervaloERecusada() throws Exception {
        assertTrue(algumContem(erros(exemploCom("\"Value\": 10000", "\"Value\": 10.5")),
                "Order.Line[1].Quantity.Value.Value", "número inteiro"));
        assertTrue(algumContem(erros(exemploCom("\"Value\": 10000", "\"Value\": 70000")),
                "Order.Line[1].Quantity.Value.Value", "entre 0 e 65535", "70000"));
        assertTrue(algumContem(erros(exemploCom("\"Value\": 10000", "\"Value\": -5")),
                "Order.Line[1].Quantity.Value.Value", "entre 0 e 65535"));
    }

    @Test
    void taxaForaDoIntervaloERecusada() throws Exception {
        assertTrue(algumContem(erros(exemploCom("\"TaxPercent\": 20", "\"TaxPercent\": 300")),
                "Order.Line[1].MonetaryAdjustment.TaxAdjustment.TaxPercent", "entre 0 e 255"));
    }

    @Test
    void dinheiroEscritoComoTextoERecusado() throws Exception {
        assertTrue(algumContem(erros(exemploCom("\"Value\": 0.25", "\"Value\": \"0.25\"")),
                "Order.Line[1].PricePerUnit.CurrencyValue.Value", "tem de ser um número"));
    }

    @Test
    void numeroDaEncomendaQueNaoETextoERecusado() throws Exception {
        assertTrue(algumContem(erros(exemploCom("\"OrderNumber\": \"X012X\"", "\"OrderNumber\": 12")),
                "Order.Header.OrderNumber", "tem de ser um texto"));
    }

    @Test
    void dataEmOutroFormatoERecusada() throws Exception {
        assertTrue(algumContem(erros(exemploCom("\"2026-11-06\"", "\"06/11/2026\"")),
                "Order.Header.OrderDate", "06/11/2026"));
        assertTrue(algumContem(erros(exemploCom("\"2026-11-06\"", "{\"Year\": 2026}")),
                "Order.Header.OrderDate", "tem de ser um texto"));
    }

    @Test
    void produtoSemIdentificadorOuComListaVaziaERecusado() throws Exception {
        String comLista = exemploCom("\"ProductIdentifier\": [", "\"ProductIdentifier\": [], \"Outro\": [");

        assertTrue(algumContem(erros(comLista), "Order.Line[1].Product.ProductIdentifier", "pelo menos um elemento"));
    }

    @Test
    void impostoEmFaltaDizOCaminho() throws Exception {
        List<String> motivos = erros(exemploCom("\"TaxLocation\": \"FR\"", "\"Outro\": 1"));

        assertTrue(algumContem(motivos, "Order.Line[1].MonetaryAdjustment.TaxAdjustment.TaxLocation", "falta"));
    }

    @Test
    void muitosErrosSaoCortadosENumaNotaFinal() {
        StringBuilder linhas = new StringBuilder();
        for (int i = 0; i < 60; i++) {
            linhas.append(i == 0 ? "" : ",").append("{}");
        }
        List<String> motivos = erros("{\"Order\": {\"Line\": [" + linhas + "]}}");

        assertEquals(51, motivos.size(), "50 erros e uma nota a dizer que há mais");
        assertTrue(motivos.get(50).contains("mais erros"));
    }

    // ---- campos opcionais ------------------------------------------------------------------------------------

    @Test
    void camposOpcionaisPodemFaltar() throws Exception {
        // QuantityType, InformationalQuantity, Address2 e OrderSummary não existem no exemplo do professor.
        EncomendaFornecedor encomenda = lerFicheiro("LIndustrie.json");

        assertEquals("", encomenda.linhas().get(0).quantidade().tipo());
        assertFalse(encomenda.linhas().get(0).quantidadesInformativas().isEmpty());
    }

    @Test
    void linhaSemQuantidadesInformativasELida() throws Exception {
        String json = exemploCom("\"InformationalQuantity\": [", "\"OutroNome\": [");

        assertTrue(ler(json).linhas().get(0).quantidadesInformativas().isEmpty());
    }

    @Test
    void resumoDaEncomendaQuandoVemELido() throws Exception {
        String resumo = ",\n \"OrderSummary\": {"
                + "\"TotalLinesAmount\": {\"CurrencyValue\": {\"CurrencyType\": \"EUR\", \"Value\": 2924.00}},"
                + "\"TotalTaxAmount\": {\"CurrencyValue\": {\"CurrencyType\": \"EUR\", \"Value\": 584.80}},"
                + "\"OrderTotalAmount\": {\"CurrencyValue\": {\"CurrencyType\": \"EUR\", \"Value\": 3508.80}}}";
        String texto = Files.readString(recurso("LIndustrie.json"), StandardCharsets.UTF_8);
        int fim = texto.lastIndexOf('}', texto.lastIndexOf('}') - 1); // fecha o "Order"
        String comResumo = texto.substring(0, fim) + resumo + "\n  }\n}";

        EncomendaFornecedor encomenda = ler(comResumo);

        assertEquals(0, new BigDecimal("3508.80").compareTo(encomenda.resumoXml().totalComTaxas()));
        assertTrue(encomenda.avisos().isEmpty());
    }

    @Test
    void resumoIncompletoERecusado() throws Exception {
        String texto = Files.readString(recurso("LIndustrie.json"), StandardCharsets.UTF_8);
        int fim = texto.lastIndexOf('}', texto.lastIndexOf('}') - 1);
        String comResumo = texto.substring(0, fim) + ", \"OrderSummary\": {}\n  }\n}";

        List<String> motivos = erros(comResumo);

        assertTrue(algumContem(motivos, "Order.OrderSummary.TotalLinesAmount", "falta"));
        assertTrue(algumContem(motivos, "Order.OrderSummary.OrderTotalAmount", "falta"));
    }

    // ---- valores (iguais aos do XML: o JSON dá os mesmos avisos) --------------------------------------------

    @Test
    void ivaOuTotaisDiferentesDosCalculadosDaoAviso() throws Exception {
        EncomendaFornecedor encomenda = lerFicheiro("LIndustrie_valores_errados.json");

        List<String> avisos = encomenda.avisos();
        assertFalse(avisos.isEmpty());
        assertTrue(avisos.stream().anyMatch(a -> a.contains("Linha 1") && a.contains("imposto")));
        assertTrue(avisos.stream().anyMatch(a -> a.contains("Linha 3") && a.contains("total da linha")));
        assertFalse(avisos.stream().anyMatch(a -> a.contains("Linha 2")), "a linha 2 está certa");
    }

    @Test
    void precoNegativoNoJsonERecusadoNaVerificacaoDosValores() throws Exception {
        EncomendaFornecedor encomenda = ler(exemploCom("\"Value\": 0.25", "\"Value\": -0.25"));

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, encomenda::validarValores);
        assertTrue(erro.getMessage().contains("negativo"));
    }

    @Test
    void jsonComBomUtf8EAcentosELido() throws Exception {
        byte[] json = Files.readAllBytes(recurso("LIndustrie.json"));
        byte[] comBom = new byte[json.length + 3];
        comBom[0] = (byte) 0xEF;
        comBom[1] = (byte) 0xBB;
        comBom[2] = (byte) 0xBF;
        System.arraycopy(json, 0, comBom, 3, json.length);

        EncomendaFornecedor encomenda = leitor.ler(comBom);

        assertEquals("Pão Campagne Francês", encomenda.linhas().get(1).descricao());
    }
}
