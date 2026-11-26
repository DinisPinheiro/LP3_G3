package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.FornecedorDAOMemoria;
import com.hotel4you.hotel4you.DAO.ImportacaoDAOMemoria;
import com.hotel4you.hotel4you.DAO.UtilizadorDAOMemoria;
import com.hotel4you.hotel4you.DAO.XsdDAOMemoria;
import com.hotel4you.hotel4you.Model.Fornecedor;
import com.hotel4you.hotel4you.Model.ImportacaoFornecedor;
import com.hotel4you.hotel4you.Model.Utilizador;
import com.hotel4you.hotel4you.Model.encomenda.EncomendaFornecedor;
import com.hotel4you.hotel4you.Model.enums.EstadoImportacao;
import com.hotel4you.hotel4you.Model.enums.FormatoFicheiro;
import com.hotel4you.hotel4you.Model.enums.Perfil;
import com.hotel4you.hotel4you.Utils.RecursosDeTeste;
import com.hotel4you.hotel4you.Utils.XmlInvalidoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes da S7-03: o fornecedor escolhe um ficheiro JSON na mesma janela do XML, o JSON é validado, a encomenda
 * repetida é bloqueada, os valores diferentes do cálculo dão aviso e o ficheiro fica pendente como o XML.
 */
class ImportacaoFornecedorJsonControllerTest {

    private static final String CODIGO_LINDUSTRIE = "964070328";

    @TempDir
    Path pasta;

    private UtilizadorDAOMemoria utilizadores;
    private FornecedorDAOMemoria fornecedores;
    private ImportacaoDAOMemoria importacoes;
    private Sessao sessao;
    private ImportacaoFornecedorController controller;
    private Utilizador contaLindustrie;

    @BeforeEach
    void preparar() {
        utilizadores = new UtilizadorDAOMemoria();
        fornecedores = new FornecedorDAOMemoria();
        importacoes = new ImportacaoDAOMemoria();
        sessao = new Sessao();
        controller = new ImportacaoFornecedorController(importacoes, fornecedores,
                new GestaoXsdController(new XsdDAOMemoria(), sessao), sessao);
        contaLindustrie = criarFornecedor("lindustrie", "L'industrie Alimentaire", CODIGO_LINDUSTRIE);
        // Sem XSD importado: o JSON não precisa dele.
        sessao.iniciar(contaLindustrie);
    }

    private Utilizador criarFornecedor(String username, String nome, String codigo) {
        Utilizador conta = utilizadores.inserir(Utilizador.novo(username, "segredo", nome, Perfil.FORNECEDOR));
        fornecedores.inserir(new Fornecedor(0, nome, codigo, "", "", "", "", conta.getId()));
        return conta;
    }

    private static Path json(String nome) {
        return Paths.get("src", "test", "resources", "json", nome);
    }

    private static Path xml(String nome) {
        return Paths.get("src", "test", "resources", "xml", nome);
    }

    /** Cópia do JSON de exemplo com outro código de fornecedor (como se fosse de outra empresa). */
    private Path jsonDoFornecedor(String codigo) throws Exception {
        String texto = Files.readString(json("LIndustrie.json"), StandardCharsets.UTF_8);
        Path destino = pasta.resolve("entrega-" + codigo + ".json");
        Files.writeString(destino, texto.replace(CODIGO_LINDUSTRIE, codigo), StandardCharsets.UTF_8);
        return destino;
    }

    // ---- escolher e enviar o JSON -----------------------------------------------------------------------

    @Test
    void jsonDoProprioFornecedorFicaPendenteComFormatoJson() throws Exception {
        ImportacaoFornecedor enviada = controller.enviar(json("LIndustrie.json"), false);

        assertEquals(EstadoImportacao.PENDENTE, enviada.getEstado());
        assertEquals(FormatoFicheiro.JSON, enviada.getFormato());
        assertEquals("X012X", enviada.getNumeroEncomenda());
        assertEquals("LIndustrie.json", enviada.getNomeFicheiro());
        assertTrue(enviada.getId() > 0);
        assertEquals(1, controller.listarMinhasImportacoes().size());
        assertEquals(FormatoFicheiro.JSON, controller.listarMinhasImportacoes().get(0).getFormato());
    }

    @Test
    void ficheiroGuardadoTemOsMesmosBytesQueOEnviado() throws Exception {
        ImportacaoFornecedor enviada = controller.enviar(json("LIndustrie.json"), false);

        assertTrue(java.util.Arrays.equals(Files.readAllBytes(json("LIndustrie.json")),
                importacoes.procurarPorId(enviada.getId()).orElseThrow().getConteudo()));
    }

    @Test
    void lerSemEnviarDaOsDadosENaoGuardaNada() throws Exception {
        EncomendaFornecedor encomenda = controller.lerEncomenda(json("LIndustrie.json"));

        assertEquals("X012X", encomenda.numero());
        assertEquals(3, encomenda.linhas().size());
        assertTrue(importacoes.listar(null).isEmpty());
    }

    @Test
    void jsonNaoPrecisaDeXsdImportado() throws Exception {
        assertTrue(controller.descreverXsd().contains("Ainda não importou"));

        assertEquals(EstadoImportacao.PENDENTE, controller.enviar(json("LIndustrie.json"), false).getEstado());
    }

    @Test
    void extensaoEmMaiusculasEAceite() throws Exception {
        Path maiusculas = pasta.resolve("ENTREGA.JSON");
        Files.copy(json("LIndustrie.json"), maiusculas);

        assertEquals(FormatoFicheiro.JSON, controller.enviar(maiusculas, false).getFormato());
    }

    @Test
    void ficheiroQueNaoEXmlNemJsonERecusadoComMensagem() throws Exception {
        Path texto = pasta.resolve("entrega.txt");
        Files.copy(json("LIndustrie.json"), texto);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> controller.enviar(texto, false));
        assertTrue(erro.getMessage().contains("não é XML nem JSON"));
        assertThrows(IllegalArgumentException.class, () -> controller.lerEncomenda(texto));
        assertTrue(importacoes.listar(null).isEmpty());
    }

    @Test
    void semFicheiroEscolhidoPedeXmlOuJson() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> controller.enviar(null, false));
        assertTrue(erro.getMessage().contains("XML ou JSON"));
    }

    @Test
    void fichaPodeSerAtualizadaComOsDadosDoJson() throws Exception {
        controller.enviar(json("LIndustrie.json"), true);

        Fornecedor ficha = fornecedores.procurarPorIdentificador(CODIGO_LINDUSTRIE).orElseThrow();
        assertEquals("Rue internationale de Paris 96", ficha.getMorada());
        assertEquals("75001", ficha.getCodigoPostal());
        assertEquals("Paris", ficha.getCidade());
        assertTrue(ficha.temConta(), "a conta continua ligada ao fornecedor");
    }

    @Test
    void envioDeJsonSemAtualizarNaoMexeNaFicha() throws Exception {
        controller.enviar(json("LIndustrie.json"), false);

        assertEquals("", controller.obterFornecedorAtual().getMorada());
    }

    // ---- validação do JSON ------------------------------------------------------------------------------

    @Test
    void jsonInvalidoDizOMotivoDeCadaErroENaoFicaGuardado() {
        XmlInvalidoException erro = assertThrows(XmlInvalidoException.class,
                () -> controller.enviar(json("LIndustrie_invalido.json"), false));

        assertEquals(4, erro.getErros().size());
        erro.getErros().forEach(e -> assertFalse(e.motivo().isBlank(), "cada erro indica o motivo"));
        assertTrue(importacoes.listar(null).isEmpty());
        assertThrows(XmlInvalidoException.class, () -> controller.lerEncomenda(json("LIndustrie_invalido.json")));
    }

    @Test
    void jsonMalFormadoDizALinhaENaoFicaGuardado() {
        XmlInvalidoException erro = assertThrows(XmlInvalidoException.class,
                () -> controller.enviar(json("LIndustrie_mal_formado.json"), false));

        assertEquals(1, erro.getErros().size());
        assertTrue(erro.getErros().get(0).linha() > 0);
        assertTrue(importacoes.listar(null).isEmpty());
    }

    @Test
    void jsonVazioERecusado() throws Exception {
        Path vazio = Files.writeString(pasta.resolve("vazio.json"), "", StandardCharsets.UTF_8);

        XmlInvalidoException erro = assertThrows(XmlInvalidoException.class, () -> controller.enviar(vazio, false));
        assertTrue(erro.getErros().get(0).motivo().contains("vazio"));
    }

    @Test
    void jsonComPrecoNegativoERecusadoComMensagem() throws Exception {
        Path negativo = pasta.resolve("negativo.json");
        Files.writeString(negativo, Files.readString(json("LIndustrie.json"), StandardCharsets.UTF_8)
                .replace("\"Value\": 0.25", "\"Value\": -0.25"), StandardCharsets.UTF_8);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> controller.enviar(negativo, false));
        assertTrue(erro.getMessage().contains("negativo"));
        assertTrue(importacoes.listar(null).isEmpty());
    }

    // ---- fornecedor certo -------------------------------------------------------------------------------

    @Test
    void jsonDeOutroFornecedorERecusadoENadaFicaGuardado() throws Exception {
        Path alheio = jsonDoFornecedor("22PT111");

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> controller.enviar(alheio, true));
        assertTrue(erro.getMessage().contains("22PT111"));
        assertThrows(IllegalArgumentException.class, () -> controller.lerEncomenda(alheio));
        assertTrue(importacoes.listar(null).isEmpty());
        assertEquals("", fornecedores.procurarPorIdentificador(CODIGO_LINDUSTRIE).orElseThrow().getMorada());
    }

    @Test
    void soUmFornecedorPodeImportarJson() throws Exception {
        sessao.iniciar(Utilizador.novo("gestor", "gestor123", "Gestor", Perfil.GESTOR));
        Path ficheiro = json("LIndustrie.json");
        assertThrows(SecurityException.class, () -> controller.enviar(ficheiro, false));
        sessao.terminar();
        assertThrows(SecurityException.class, () -> controller.enviar(ficheiro, false));
    }

    // ---- encomenda repetida -----------------------------------------------------------------------------

    @Test
    void encomendaJsonJaEnviadaEBloqueadaComMensagem() throws Exception {
        controller.enviar(json("LIndustrie.json"), false);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> controller.enviar(json("LIndustrie.json"), false));

        assertTrue(erro.getMessage().contains("X012X"));
        assertTrue(erro.getMessage().contains("já foi enviada"));
        assertEquals(1, importacoes.listar(null).size());
    }

    @Test
    void ficheiroDiferenteComOMesmoNumeroDeEncomendaTambemEBloqueadoJaNaLeitura() throws Exception {
        controller.enviar(json("LIndustrie.json"), false);

        assertThrows(IllegalArgumentException.class,
                () -> controller.lerEncomenda(json("LIndustrie_encomenda_repetida.json")));
        assertThrows(IllegalArgumentException.class,
                () -> controller.enviar(json("LIndustrie_encomenda_repetida.json"), false));
        assertEquals(1, importacoes.listar(null).size());
    }

    @Test
    void encomendaRepetidaContaMesmoDepoisDeRejeitada() throws Exception {
        ImportacaoFornecedor enviada = controller.enviar(json("LIndustrie.json"), false);
        Utilizador gestor = utilizadores.inserir(Utilizador.novo("gestor", "gestor123", "Gestor", Perfil.GESTOR));
        enviada.rejeitar(gestor, java.time.LocalDateTime.now());
        importacoes.guardarDecisao(enviada);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> controller.enviar(json("LIndustrie.json"), false));
        assertTrue(erro.getMessage().contains("rejeitado"));
    }

    @Test
    void encomendaComOutroNumeroDoMesmoFornecedorEAceite() throws Exception {
        controller.enviar(json("LIndustrie.json"), false);
        controller.enviar(json("LIndustrie_encomenda_nova.json"), false);

        assertEquals(2, controller.listarMinhasImportacoes().size());
    }

    @Test
    void mesmoNumeroDeEncomendaEmOutroFornecedorEAceite() throws Exception {
        controller.enviar(json("LIndustrie.json"), false);
        sessao.iniciar(criarFornecedor("outra", "Outra Lda", "22PT111"));

        controller.enviar(jsonDoFornecedor("22PT111"), false);

        assertEquals(1, controller.listarMinhasImportacoes().size());
        assertEquals(2, importacoes.listar(null).size());
    }

    @Test
    void numeroDeEncomendaEnviadoEmXmlBloqueiaOMesmoNumeroEmJson() throws Exception {
        // O número da encomenda é do fornecedor, não do formato: o mesmo número em JSON depois do XML é repetido.
        Utilizador productHero = criarFornecedor("producthero", "ProductHero", "11PT920");
        sessao.iniciar(productHero);
        controller.importarXsd(RecursosDeTeste.xsd());
        controller.enviar(xml("ProductHero.xml"), false);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> controller.enviar(json("ProductHero.json"), false));

        assertTrue(erro.getMessage().contains("OC56974"));
        assertEquals(1, importacoes.listar(null).size());
    }

    @Test
    void fornecedorPodeEnviarXmlEJsonEOHistoricoGuardaOFormatoDeCada() throws Exception {
        Utilizador productHero = criarFornecedor("producthero", "ProductHero", "11PT920");
        sessao.iniciar(productHero);
        controller.importarXsd(RecursosDeTeste.xsd());
        controller.enviar(xml("ProductHero.xml"), false);
        Path jsonNovo = pasta.resolve("ProductHero_nova.json");
        Files.writeString(jsonNovo, Files.readString(json("ProductHero.json"), StandardCharsets.UTF_8)
                .replace("OC56974", "OC56975"), StandardCharsets.UTF_8);
        controller.enviar(jsonNovo, false);

        List<ImportacaoFornecedor> historico = controller.listarMinhasImportacoes();
        assertEquals(2, historico.size());
        // Do mais recente para o mais antigo: primeiro o JSON, depois o XML.
        assertEquals(FormatoFicheiro.JSON, historico.get(0).getFormato());
        assertEquals(FormatoFicheiro.XML, historico.get(1).getFormato());
    }

    // ---- valores diferentes do cálculo ------------------------------------------------------------------

    @Test
    void ivaOuTotaisDiferentesDosDoJsonDaoAvisoMasOEnvioContinua() throws Exception {
        EncomendaFornecedor encomenda = controller.lerEncomenda(json("LIndustrie_valores_errados.json"));

        assertFalse(encomenda.avisos().isEmpty());
        assertTrue(encomenda.avisos().stream().anyMatch(a -> a.contains("Linha 1")));
        assertTrue(encomenda.avisos().stream().anyMatch(a -> a.contains("Linha 3")));
        assertEquals(EstadoImportacao.PENDENTE,
                controller.enviar(json("LIndustrie_valores_errados.json"), false).getEstado());
    }

    @Test
    void jsonComOsValoresCertosNaoDaAvisos() throws Exception {
        assertTrue(controller.lerEncomenda(json("LIndustrie.json")).avisos().isEmpty());
    }
}
