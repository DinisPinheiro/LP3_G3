package com.hotel4you.hotel4you.Model;

import com.hotel4you.hotel4you.Model.enums.EstadoImportacao;
import com.hotel4you.hotel4you.Model.enums.FormatoFicheiro;
import com.hotel4you.hotel4you.Model.enums.Perfil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testes da S5-03 (transições de estado Pendente, Aprovado e Rejeitado) e da S6-03 (encomenda repetida). */
class ImportacaoFornecedorTest {

    private static final LocalDateTime ENVIO = LocalDateTime.of(2026, 11, 10, 9, 0);
    private static final LocalDateTime DECISAO = LocalDateTime.of(2026, 11, 11, 15, 30);

    private Utilizador decisor;
    private ImportacaoFornecedor importacao;

    @BeforeEach
    void preparar() {
        decisor = new Utilizador(7, "rececao", "hash", "Rececionista", Perfil.RECECIONISTA, true);
        importacao = nova();
    }

    private static ImportacaoFornecedor nova() {
        return new ImportacaoFornecedor(new Fornecedor(1, "ProductHero", "11PT920", ""), "entrega.xml", "OC1",
                new byte[]{1, 2, 3}, ENVIO);
    }

    @Test
    void importacaoNovaFicaPendenteSemDecisao() {
        assertEquals(EstadoImportacao.PENDENTE, importacao.getEstado());
        assertEquals(0, importacao.getIdDecisor());
        assertNull(importacao.getDataDecisao());
    }

    @Test
    void pendentePodeSerAprovadoERegistaQuemEQuando() {
        importacao.aprovar(decisor, DECISAO);
        assertEquals(EstadoImportacao.APROVADO, importacao.getEstado());
        assertEquals(7, importacao.getIdDecisor());
        assertEquals(DECISAO, importacao.getDataDecisao());
    }

    @Test
    void pendentePodeSerRejeitadoERegistaQuemEQuando() {
        importacao.rejeitar(decisor, DECISAO);
        assertEquals(EstadoImportacao.REJEITADO, importacao.getEstado());
        assertEquals(7, importacao.getIdDecisor());
        assertEquals(DECISAO, importacao.getDataDecisao());
    }

    @Test
    void aprovadoNaoMudaDeEstado() {
        importacao.aprovar(decisor, DECISAO);
        assertThrows(IllegalStateException.class, () -> importacao.aprovar(decisor, DECISAO));
        assertThrows(IllegalStateException.class, () -> importacao.rejeitar(decisor, DECISAO));
        assertEquals(EstadoImportacao.APROVADO, importacao.getEstado());
    }

    @Test
    void rejeitadoNaoMudaDeEstado() {
        importacao.rejeitar(decisor, DECISAO);
        assertThrows(IllegalStateException.class, () -> importacao.aprovar(decisor, DECISAO));
        assertThrows(IllegalStateException.class, () -> importacao.rejeitar(decisor, DECISAO));
        assertEquals(EstadoImportacao.REJEITADO, importacao.getEstado());
    }

    @Test
    void exigirPendenteFalhaDepoisDeDecidido() {
        importacao.exigirPendente();
        importacao.rejeitar(decisor, DECISAO);
        IllegalStateException erro = assertThrows(IllegalStateException.class, importacao::exigirPendente);
        assertEquals("O ficheiro 'entrega.xml' já está rejeitado e não pode mudar de estado.", erro.getMessage());
    }

    @Test
    void ficheiroVazioOuSemDadosObrigatoriosERecusado() {
        Fornecedor fornecedor = new Fornecedor(1, "ProductHero", "11PT920", "");
        assertThrows(IllegalArgumentException.class,
                () -> new ImportacaoFornecedor(fornecedor, "a.xml", "OC1", new byte[0], ENVIO));
        assertThrows(IllegalArgumentException.class,
                () -> new ImportacaoFornecedor(fornecedor, " ", "OC1", new byte[]{1}, ENVIO));
        assertThrows(IllegalArgumentException.class,
                () -> new ImportacaoFornecedor(fornecedor, "a.xml", "", new byte[]{1}, ENVIO));
    }

    @Test
    void conteudoNaoPodeSerAlteradoPorFora() {
        byte[] copia = importacao.getConteudo();
        copia[0] = 99;
        assertArrayEquals(new byte[]{1, 2, 3}, importacao.getConteudo());
    }

    // ---- S6-03: o mesmo fornecedor não envia duas vezes a mesma encomenda ---------------------------

    @Test
    void numeroDeEncomendaJaEnviadoEBloqueadoComMensagem() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> ImportacaoFornecedor.exigirEncomendaNova("OC1", List.of(importacao)));

        assertTrue(erro.getMessage().contains("OC1"));
        assertTrue(erro.getMessage().contains("já foi enviada"));
        assertTrue(erro.getMessage().contains("entrega.xml"));
    }

    @Test
    void numeroDeEncomendaNovoEAceite() {
        assertDoesNotThrow(() -> ImportacaoFornecedor.exigirEncomendaNova("OC2", List.of(importacao)));
        assertDoesNotThrow(() -> ImportacaoFornecedor.exigirEncomendaNova("OC1", List.of()));
    }

    @Test
    void numeroDeEncomendaComparaSemMaiusculasNemEspacosNasPontas() {
        assertThrows(IllegalArgumentException.class,
                () -> ImportacaoFornecedor.exigirEncomendaNova("  oc1 ", List.of(importacao)));
    }

    @Test
    void encomendaJaDecididaContaComoEnviada() {
        importacao.rejeitar(decisor, DECISAO);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> ImportacaoFornecedor.exigirEncomendaNova("OC1", List.of(importacao)));
        assertTrue(erro.getMessage().contains("rejeitado"));
    }

    @Test
    void numeroDeEncomendaEmBrancoEInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> ImportacaoFornecedor.exigirEncomendaNova(" ", List.of()));
    }

    // ---- S7-03: formato do ficheiro (XML ou JSON) --------------------------------------------------------

    @Test
    void importacaoSemFormatoIndicadoEXml() {
        assertEquals(FormatoFicheiro.XML, importacao.getFormato());
    }

    @Test
    void importacaoGuardaOFormatoIndicadoEMantemO() {
        ImportacaoFornecedor json = new ImportacaoFornecedor(new Fornecedor(1, "ProductHero", "11PT920", ""),
                "entrega.json", FormatoFicheiro.JSON, "OC2", new byte[]{1}, ENVIO);

        assertEquals(FormatoFicheiro.JSON, json.getFormato());
        json.aprovar(decisor, DECISAO);
        assertEquals(FormatoFicheiro.JSON, json.getFormato(), "a decisão não muda o formato");
    }

    @Test
    void importacaoJaGuardadaGuardaOFormato() {
        ImportacaoFornecedor guardada = new ImportacaoFornecedor(5, new Fornecedor(1, "ProductHero", "11PT920", ""),
                "entrega.json", FormatoFicheiro.JSON, "OC3", new byte[]{1}, ENVIO, EstadoImportacao.PENDENTE, 0, null);

        assertEquals(FormatoFicheiro.JSON, guardada.getFormato());
    }

    @Test
    void importacaoSemFormatoERecusada() {
        Fornecedor fornecedor = new Fornecedor(1, "ProductHero", "11PT920", "");
        assertThrows(NullPointerException.class,
                () -> new ImportacaoFornecedor(fornecedor, "a.json", null, "OC1", new byte[]{1}, ENVIO));
    }

    @Test
    void encomendaRepetidaContaQualquerQueSejaOFormatoDoEnvioAnterior() {
        ImportacaoFornecedor jaEnviadaEmJson = new ImportacaoFornecedor(new Fornecedor(1, "ProductHero", "11PT920", ""),
                "entrega.json", FormatoFicheiro.JSON, "OC9", new byte[]{1}, ENVIO);

        assertThrows(IllegalArgumentException.class,
                () -> ImportacaoFornecedor.exigirEncomendaNova("oc9", List.of(jaEnviadaEmJson)));
    }
}
