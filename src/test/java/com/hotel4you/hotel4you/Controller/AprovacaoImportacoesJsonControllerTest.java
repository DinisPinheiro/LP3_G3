package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.FornecedorDAOMemoria;
import com.hotel4you.hotel4you.DAO.ImportacaoDAOMemoria;
import com.hotel4you.hotel4you.DAO.StockDAOMemoria;
import com.hotel4you.hotel4you.DAO.UtilizadorDAOMemoria;
import com.hotel4you.hotel4you.DAO.XsdDAOMemoria;
import com.hotel4you.hotel4you.Model.Artigo;
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
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes da S7-03: um ficheiro JSON segue o mesmo fluxo de aprovação do XML. Só dá entrada em stock quando o
 * gestor ou o rececionista o aprova, e a aprovação lê o ficheiro com o leitor do formato guardado.
 */
class AprovacaoImportacoesJsonControllerTest {

    @TempDir
    Path pasta;

    private FornecedorDAOMemoria fornecedores;
    private StockDAOMemoria stock;
    private ImportacaoDAOMemoria importacoes;
    private Sessao sessao;
    private Utilizador contaLindustrie;
    private Utilizador contaProductHero;
    private Utilizador rececionista;
    private Utilizador gestor;
    private ImportacaoFornecedorController envio;
    private AprovacaoImportacoesController aprovacao;
    private int numeroDaEntrega;

    @BeforeEach
    void preparar() {
        UtilizadorDAOMemoria utilizadores = new UtilizadorDAOMemoria();
        fornecedores = new FornecedorDAOMemoria();
        importacoes = new ImportacaoDAOMemoria();
        stock = new StockDAOMemoria();
        sessao = new Sessao();
        GestaoXsdController xsd = new GestaoXsdController(new XsdDAOMemoria(), sessao);
        envio = new ImportacaoFornecedorController(importacoes, fornecedores, xsd, sessao);
        aprovacao = new AprovacaoImportacoesController(importacoes, new GestaoStockController(stock, sessao), xsd, sessao);

        contaLindustrie = utilizadores.inserir(Utilizador.novo("lindustrie", "segredo", "L'industrie Alimentaire",
                Perfil.FORNECEDOR));
        fornecedores.inserir(new Fornecedor(0, "L'industrie Alimentaire", "964070328", "", "", "", "",
                contaLindustrie.getId()));
        contaProductHero = utilizadores.inserir(Utilizador.novo("producthero", "segredo", "ProductHero",
                Perfil.FORNECEDOR));
        fornecedores.inserir(new Fornecedor(0, "ProductHero", "11PT920", "", "", "", "", contaProductHero.getId()));
        rececionista = utilizadores.inserir(Utilizador.novo("rececao", "rececao123", "Rececionista",
                Perfil.RECECIONISTA));
        gestor = utilizadores.inserir(Utilizador.novo("gestor", "gestor123", "Gestor", Perfil.GESTOR));
    }

    private static Path json(String nome) {
        return Paths.get("src", "test", "resources", "json", nome);
    }

    /** O fornecedor L'industrie envia o JSON do professor (sem XSD) e devolve o id da importação pendente. */
    private int lindustrieEnviaJson() throws Exception {
        sessao.iniciar(contaLindustrie);
        return envio.enviar(json("LIndustrie.json"), false).getId();
    }

    /** O fornecedor ProductHero envia o XML de exemplo, com número de encomenda novo de cada vez. */
    private int productHeroEnviaXml() throws Exception {
        sessao.iniciar(contaProductHero);
        envio.importarXsd(RecursosDeTeste.xsd());
        String xml = Files.readString(Paths.get("src", "test", "resources", "xml", "ProductHero.xml"),
                        StandardCharsets.UTF_8)
                .replace("<OrderNumber>OC56974</OrderNumber>", "<OrderNumber>OC" + (70000 + ++numeroDaEntrega) + "</OrderNumber>");
        Path ficheiro = Files.writeString(pasta.resolve("entrega-" + numeroDaEntrega + ".xml"), xml, StandardCharsets.UTF_8);
        return envio.enviar(ficheiro, false).getId();
    }

    @Test
    void jsonImportadoFicaPendenteENaoDaEntradaEmStock() throws Exception {
        int id = lindustrieEnviaJson();
        sessao.iniciar(rececionista);

        assertEquals(EstadoImportacao.PENDENTE, importacoes.procurarPorId(id).orElseThrow().getEstado());
        assertTrue(stock.listarArtigos().isEmpty(), "sem aprovação não há entrada em stock");
        assertEquals(1, aprovacao.listar(EstadoImportacao.PENDENTE).size());
        assertEquals(FormatoFicheiro.JSON, aprovacao.listar(EstadoImportacao.PENDENTE).get(0).getFormato());
    }

    @Test
    void gestorEVeOConteudoDoJsonAntesDeDecidir() throws Exception {
        int id = lindustrieEnviaJson();
        sessao.iniciar(gestor);

        EncomendaFornecedor encomenda = aprovacao.consultarEncomenda(id);

        assertEquals("X012X", encomenda.numero());
        assertEquals(3, encomenda.linhas().size());
        assertEquals("L'industrie Alimentaire", encomenda.fornecedor().nome());
    }

    @Test
    void aprovarJsonDaEntradaEmStockEPassaAAprovado() throws Exception {
        int id = lindustrieEnviaJson();
        sessao.iniciar(rececionista);

        aprovacao.aprovar(id);

        ImportacaoFornecedor guardada = importacoes.procurarPorId(id).orElseThrow();
        assertEquals(EstadoImportacao.APROVADO, guardada.getEstado());
        assertEquals(FormatoFicheiro.JSON, guardada.getFormato(), "o formato fica no histórico depois da decisão");
        assertEquals(rececionista.getId(), guardada.getIdDecisor());
        assertTrue(guardada.getDataDecisao() != null);

        // O stock identifica o artigo pela descrição: as linhas 2 e 3 (mesma descrição, códigos diferentes) somam.
        assertEquals(2, stock.listarArtigos().size());
        assertEquals(10000, quantidadeEmStock("Croissant Brioche délifrance"));
        assertEquals(200, quantidadeEmStock("Pão Campagne Francês"));
    }

    private int quantidadeEmStock(String nome) {
        return stock.listarArtigos().stream().filter(a -> a.getNome().equals(nome)).map(Artigo::getQuantidadeAtual)
                .findFirst().orElseThrow();
    }

    @Test
    void rejeitarJsonNaoDaEntradaEmStock() throws Exception {
        int id = lindustrieEnviaJson();
        sessao.iniciar(gestor);

        aprovacao.rejeitar(id);

        assertEquals(EstadoImportacao.REJEITADO, importacoes.procurarPorId(id).orElseThrow().getEstado());
        assertEquals(gestor.getId(), importacoes.procurarPorId(id).orElseThrow().getIdDecisor());
        assertTrue(stock.listarArtigos().isEmpty());
    }

    @Test
    void jsonAprovadoNaoVoltaADarEntrada() throws Exception {
        int id = lindustrieEnviaJson();
        sessao.iniciar(rececionista);
        aprovacao.aprovar(id);

        assertThrows(IllegalStateException.class, () -> aprovacao.aprovar(id));
        assertThrows(IllegalStateException.class, () -> aprovacao.rejeitar(id));
        assertEquals(10000, quantidadeEmStock("Croissant Brioche délifrance"));
    }

    @Test
    void jsonRejeitadoNaoPodeSerAprovado() throws Exception {
        int id = lindustrieEnviaJson();
        sessao.iniciar(rececionista);
        aprovacao.rejeitar(id);

        assertThrows(IllegalStateException.class, () -> aprovacao.aprovar(id));
        assertTrue(stock.listarArtigos().isEmpty());
    }

    @Test
    void fornecedorNaoPodeAprovarJson() throws Exception {
        int id = lindustrieEnviaJson();

        assertThrows(SecurityException.class, () -> aprovacao.aprovar(id));
        assertThrows(SecurityException.class, () -> aprovacao.rejeitar(id));
        assertTrue(stock.listarArtigos().isEmpty());
    }

    @Test
    void xmlEJsonPendentesAprovamSeCadaUmComOSeuLeitor() throws Exception {
        int idJson = lindustrieEnviaJson();
        int idXml = productHeroEnviaXml();
        sessao.iniciar(rececionista);

        assertEquals(FormatoFicheiro.XML, importacoes.procurarPorId(idXml).orElseThrow().getFormato());
        assertEquals(FormatoFicheiro.JSON, importacoes.procurarPorId(idJson).orElseThrow().getFormato());
        aprovacao.aprovar(idXml);
        aprovacao.aprovar(idJson);

        assertEquals(EstadoImportacao.APROVADO, importacoes.procurarPorId(idXml).orElseThrow().getEstado());
        assertEquals(EstadoImportacao.APROVADO, importacoes.procurarPorId(idJson).orElseThrow().getEstado());
        assertEquals(5 + 2, stock.listarArtigos().size(), "5 artigos do XML e 2 do JSON");
        assertEquals(2, aprovacao.listar(EstadoImportacao.APROVADO).size());
    }

    @Test
    void jsonGuardadoQueJaNaoEValidoNaoDaEntradaEFicaPendente() throws Exception {
        // Um JSON inválido que chegou à base de dados (por outra via) não pode dar entrada em stock.
        Fornecedor fornecedor = fornecedores.procurarPorIdentificador("964070328").orElseThrow();
        ImportacaoFornecedor invalido = importacoes.inserir(new ImportacaoFornecedor(fornecedor, "invalido.json",
                FormatoFicheiro.JSON, "X999X", Files.readAllBytes(json("LIndustrie_invalido.json")),
                LocalDateTime.now()));
        sessao.iniciar(gestor);

        assertThrows(XmlInvalidoException.class, () -> aprovacao.aprovar(invalido.getId()));
        assertEquals(EstadoImportacao.PENDENTE, importacoes.procurarPorId(invalido.getId()).orElseThrow().getEstado());
        assertTrue(stock.listarArtigos().isEmpty());
        aprovacao.rejeitar(invalido.getId());
        assertEquals(EstadoImportacao.REJEITADO, importacoes.procurarPorId(invalido.getId()).orElseThrow().getEstado());
    }

    @Test
    void ficheiroGuardadoComoXmlNaoSeLeComoJson() throws Exception {
        // A aprovação usa o formato guardado, não adivinha: um XML marcado como JSON é recusado.
        Fornecedor fornecedor = fornecedores.procurarPorIdentificador("11PT920").orElseThrow();
        ImportacaoFornecedor trocado = importacoes.inserir(new ImportacaoFornecedor(fornecedor, "entrega.json",
                FormatoFicheiro.JSON, "OC1",
                Files.readAllBytes(Paths.get("src", "test", "resources", "xml", "ProductHero.xml")),
                LocalDateTime.now()));
        sessao.iniciar(gestor);

        XmlInvalidoException erro = assertThrows(XmlInvalidoException.class, () -> aprovacao.aprovar(trocado.getId()));
        assertTrue(erro.getErros().get(0).motivo().startsWith("JSON mal formado"));
        assertTrue(stock.listarArtigos().isEmpty());
    }
}
