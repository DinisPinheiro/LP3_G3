package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.FornecedorDAOMemoria;
import com.hotel4you.hotel4you.DAO.ImportacaoDAOMemoria;
import com.hotel4you.hotel4you.DAO.StockDAOMemoria;
import com.hotel4you.hotel4you.DAO.UtilizadorDAOMemoria;
import com.hotel4you.hotel4you.DAO.XsdDAOMemoria;
import com.hotel4you.hotel4you.Model.Fornecedor;
import com.hotel4you.hotel4you.Model.ImportacaoFornecedor;
import com.hotel4you.hotel4you.Model.Utilizador;
import com.hotel4you.hotel4you.Model.encomenda.EncomendaFornecedor;
import com.hotel4you.hotel4you.Model.enums.EstadoImportacao;
import com.hotel4you.hotel4you.Model.enums.Perfil;
import com.hotel4you.hotel4you.Utils.RecursosDeTeste;
import com.hotel4you.hotel4you.Utils.XmlInvalidoException;
import com.hotel4you.hotel4you.Utils.XsdEmFaltaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testes da S5-03: transições Pendente, Aprovado e Rejeitado, e só os aprovados dão entrada em stock. */
class AprovacaoImportacoesControllerTest {

    @TempDir
    Path pasta;

    private FornecedorDAOMemoria fornecedores;
    private int numeroDaEntrega;
    private StockDAOMemoria stock;
    private ImportacaoDAOMemoria importacoes;
    private Sessao sessao;
    private Utilizador conta;
    private Utilizador rececionista;
    private Utilizador gestor;
    private ImportacaoFornecedorController envio;
    private GestaoXsdController xsd;
    private AprovacaoImportacoesController aprovacao;

    @BeforeEach
    void preparar() {
        UtilizadorDAOMemoria utilizadores = new UtilizadorDAOMemoria();
        fornecedores = new FornecedorDAOMemoria();
        importacoes = new ImportacaoDAOMemoria();
        stock = new StockDAOMemoria();
        sessao = new Sessao();
        xsd = new GestaoXsdController(new XsdDAOMemoria(), sessao);
        envio = new ImportacaoFornecedorController(importacoes, fornecedores, xsd, sessao);
        aprovacao = new AprovacaoImportacoesController(importacoes, new GestaoStockController(stock, sessao), xsd, sessao);

        conta = utilizadores.inserir(Utilizador.novo("producthero", "segredo", "ProductHero", Perfil.FORNECEDOR));
        fornecedores.inserir(new Fornecedor(0, "ProductHero", "11PT920", "", "", "", "", conta.getId()));
        rececionista = utilizadores.inserir(Utilizador.novo("rececao", "rececao123", "Rececionista", Perfil.RECECIONISTA));
        gestor = utilizadores.inserir(Utilizador.novo("gestor", "gestor123", "Gestor", Perfil.GESTOR));
    }

    private static Path recurso(String nome) throws Exception {
        return Paths.get("src", "test", "resources", "xml", nome);
    }

    /**
     * O fornecedor importa o XSD e envia o XML de exemplo, com um número de encomenda novo de cada vez
     * (o mesmo número não pode ser enviado duas vezes); devolve o id da importação pendente.
     */
    private int fornecedorEnviaFicheiro() throws Exception {
        sessao.iniciar(conta);
        envio.importarXsd(RecursosDeTeste.xsd());
        String xml = Files.readString(recurso("ProductHero.xml"), StandardCharsets.UTF_8)
                .replace("<OrderNumber>OC56974</OrderNumber>", "<OrderNumber>OC" + (60000 + ++numeroDaEntrega) + "</OrderNumber>");
        Path ficheiro = Files.writeString(pasta.resolve("entrega-" + numeroDaEntrega + ".xml"), xml, StandardCharsets.UTF_8);
        return envio.enviar(ficheiro, false).getId();
    }

    @Test
    void ficheiroImportadoFicaPendenteENaoDaEntradaEmStock() throws Exception {
        int id = fornecedorEnviaFicheiro();
        sessao.iniciar(rececionista);

        assertEquals(EstadoImportacao.PENDENTE, importacoes.procurarPorId(id).orElseThrow().getEstado());
        assertTrue(stock.listarArtigos().isEmpty(), "sem aprovação não há entrada em stock");
        assertEquals(1, aprovacao.listar(EstadoImportacao.PENDENTE).size());
    }

    @Test
    void aprovarDaEntradaEmStockEPassaAAprovado() throws Exception {
        int id = fornecedorEnviaFicheiro();
        sessao.iniciar(rececionista);

        aprovacao.aprovar(id);

        ImportacaoFornecedor guardada = importacoes.procurarPorId(id).orElseThrow();
        assertEquals(EstadoImportacao.APROVADO, guardada.getEstado());
        assertEquals(rececionista.getId(), guardada.getIdDecisor());
        assertTrue(guardada.getDataDecisao() != null);
        EncomendaFornecedor encomenda = aprovacao.consultarEncomenda(id);
        assertEquals(encomenda.linhas().size(), stock.listarArtigos().size());
    }

    @Test
    void rejeitarNaoDaEntradaEmStock() throws Exception {
        int id = fornecedorEnviaFicheiro();
        sessao.iniciar(gestor);

        aprovacao.rejeitar(id);

        assertEquals(EstadoImportacao.REJEITADO, importacoes.procurarPorId(id).orElseThrow().getEstado());
        assertEquals(gestor.getId(), importacoes.procurarPorId(id).orElseThrow().getIdDecisor());
        assertTrue(stock.listarArtigos().isEmpty());
    }

    @Test
    void ficheiroAprovadoNaoVoltaADarEntrada() throws Exception {
        int id = fornecedorEnviaFicheiro();
        sessao.iniciar(rececionista);
        aprovacao.aprovar(id);
        int quantidade = stock.listarArtigos().get(0).getQuantidadeAtual();

        assertThrows(IllegalStateException.class, () -> aprovacao.aprovar(id));
        assertThrows(IllegalStateException.class, () -> aprovacao.rejeitar(id));
        assertEquals(quantidade, stock.listarArtigos().get(0).getQuantidadeAtual());
        assertEquals(EstadoImportacao.APROVADO, importacoes.procurarPorId(id).orElseThrow().getEstado());
    }

    @Test
    void ficheiroRejeitadoNaoPodeSerAprovado() throws Exception {
        int id = fornecedorEnviaFicheiro();
        sessao.iniciar(rececionista);
        aprovacao.rejeitar(id);

        assertThrows(IllegalStateException.class, () -> aprovacao.aprovar(id));
        assertThrows(IllegalStateException.class, () -> aprovacao.rejeitar(id));
        assertTrue(stock.listarArtigos().isEmpty());
    }

    @Test
    void listagemSeparaPendentesAprovadosERejeitados() throws Exception {
        int aprovado = fornecedorEnviaFicheiro();
        int rejeitado = fornecedorEnviaFicheiro();
        fornecedorEnviaFicheiro();
        sessao.iniciar(rececionista);
        aprovacao.aprovar(aprovado);
        aprovacao.rejeitar(rejeitado);

        assertEquals(1, aprovacao.listar(EstadoImportacao.PENDENTE).size());
        assertEquals(aprovado, aprovacao.listar(EstadoImportacao.APROVADO).get(0).getId());
        assertEquals(rejeitado, aprovacao.listar(EstadoImportacao.REJEITADO).get(0).getId());
        assertEquals(3, aprovacao.listar(null).size());
    }

    @Test
    void fornecedorNaoPodeAprovarNemRejeitarNemListar() throws Exception {
        int id = fornecedorEnviaFicheiro();

        assertThrows(SecurityException.class, () -> aprovacao.aprovar(id));
        assertThrows(SecurityException.class, () -> aprovacao.rejeitar(id));
        assertThrows(SecurityException.class, () -> aprovacao.listar(null));
        assertTrue(stock.listarArtigos().isEmpty());
    }

    @Test
    void ficheiroQueJaNaoCumpreOXsdNaoDaEntradaEFicaPendente() throws Exception {
        int id = fornecedorEnviaFicheiro();
        Path xsdRestrito = pasta.resolve("restrito.xsd");
        Files.writeString(xsdRestrito, "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">"
                + "<xs:element name=\"Encomenda\" type=\"xs:string\"/></xs:schema>", StandardCharsets.UTF_8);
        // Entretanto o fornecedor importa um XSD diferente, que o XML já enviado não cumpre.
        sessao.iniciar(conta);
        envio.importarXsd(xsdRestrito);
        sessao.iniciar(gestor);

        assertThrows(XmlInvalidoException.class, () -> aprovacao.aprovar(id));
        assertEquals(EstadoImportacao.PENDENTE, importacoes.procurarPorId(id).orElseThrow().getEstado());
        assertTrue(stock.listarArtigos().isEmpty());
        aprovacao.rejeitar(id);
        assertEquals(EstadoImportacao.REJEITADO, importacoes.procurarPorId(id).orElseThrow().getEstado());
    }

    @Test
    void ficheiroDeFornecedorSemXsdNaoDaEntradaEFicaPendente() throws Exception {
        // Ficheiro enviado antes de o XSD passar a ser do fornecedor: o fornecedor ainda não tem XSD.
        Fornecedor fornecedor = fornecedores.procurarPorIdentificador("11PT920").orElseThrow();
        ImportacaoFornecedor antigo = importacoes.inserir(new ImportacaoFornecedor(fornecedor, "antigo.xml", "OC1",
                Files.readAllBytes(recurso("ProductHero.xml")), java.time.LocalDateTime.now()));
        sessao.iniciar(gestor);

        assertThrows(XsdEmFaltaException.class, () -> aprovacao.aprovar(antigo.getId()));
        assertEquals(EstadoImportacao.PENDENTE, importacoes.procurarPorId(antigo.getId()).orElseThrow().getEstado());
        assertTrue(stock.listarArtigos().isEmpty());
    }

    @Test
    void importacaoInexistenteDaMensagem() {
        sessao.iniciar(rececionista);
        assertThrows(IllegalArgumentException.class, () -> aprovacao.aprovar(999));
        assertThrows(IllegalArgumentException.class, () -> aprovacao.rejeitar(999));
    }
}
