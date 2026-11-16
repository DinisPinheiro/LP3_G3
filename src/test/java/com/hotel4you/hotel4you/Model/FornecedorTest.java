package com.hotel4you.hotel4you.Model;

import com.hotel4you.hotel4you.Model.encomenda.FornecedorEncomenda;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testes da S5-02: ficha do fornecedor e atualização a partir dos dados do XML. */
class FornecedorTest {

    private static FornecedorEncomenda doXml(String codigo) {
        return new FornecedorEncomenda(codigo, "ProductHero Lda", "Rua da Alegria", "nº1", "Porto",
                "4000-032", "PT", "Portugal");
    }

    @Test
    void fornecedorSemContaNemMoradaPorOmissao() {
        Fornecedor fornecedor = new Fornecedor(0, "ProductHero", "11PT920", "");
        assertFalse(fornecedor.temConta());
        assertEquals("", fornecedor.getMorada());
        assertEquals("", fornecedor.getCodigoPostal());
        assertEquals("", fornecedor.getCidade());
    }

    @Test
    void atualizarComDadosDoXmlPreencheNomeMoradaPostalECidade() {
        Fornecedor fornecedor = new Fornecedor(1, "Nome antigo", "11PT920", "", "", "", "", 5);
        fornecedor.atualizarDadosDe(doXml("11PT920"));
        assertEquals("ProductHero Lda", fornecedor.getNome());
        assertEquals("Rua da Alegria, nº1", fornecedor.getMorada());
        assertEquals("4000-032", fornecedor.getCodigoPostal());
        assertEquals("Porto", fornecedor.getCidade());
        assertEquals("11PT920", fornecedor.getIdentificador());
        assertTrue(fornecedor.temConta());
    }

    @Test
    void moradaSemSegundaLinhaNaoLevaVirgulaSobrante() {
        assertEquals("Rua da Alegria", Fornecedor.juntarMorada("Rua da Alegria", ""));
        assertEquals("nº1", Fornecedor.juntarMorada("", "nº1"));
        assertEquals("", Fornecedor.juntarMorada(null, null));
    }

    @Test
    void dadosDeOutroFornecedorSaoRecusados() {
        Fornecedor fornecedor = new Fornecedor(1, "ProductHero", "11PT920", "");
        assertThrows(IllegalArgumentException.class, () -> fornecedor.atualizarDadosDe(doXml("22PT111")));
        assertEquals("ProductHero", fornecedor.getNome());
    }

    @Test
    void codigoENomeSaoObrigatorios() {
        assertThrows(IllegalArgumentException.class, () -> new Fornecedor(0, "ProductHero", " ", ""));
        assertThrows(IllegalArgumentException.class, () -> new Fornecedor(0, "", "11PT920", ""));
    }
}
