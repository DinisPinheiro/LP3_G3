package com.hotel4you.hotel4you.Model;

import com.hotel4you.hotel4you.Model.enums.EstadoImportacao;
import com.hotel4you.hotel4you.Model.enums.Perfil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Testes da S5-03: transições de estado Pendente, Aprovado e Rejeitado. */
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
}
