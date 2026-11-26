package com.hotel4you.hotel4you.Model;

import com.hotel4you.hotel4you.Model.enums.FormatoFicheiro;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testes da S7-03: o formato (XML ou JSON) de um ficheiro de fornecedor descobre-se pela extensão. */
class FormatoFicheiroTest {

    @Test
    void extensaoXmlDaFormatoXml() {
        assertEquals(FormatoFicheiro.XML, FormatoFicheiro.deNomeFicheiro("ProductHero.xml"));
    }

    @Test
    void extensaoJsonDaFormatoJson() {
        assertEquals(FormatoFicheiro.JSON, FormatoFicheiro.deNomeFicheiro("LIndustrie.json"));
    }

    @Test
    void maiusculasEEspacosNasPontasNaoImportam() {
        assertEquals(FormatoFicheiro.JSON, FormatoFicheiro.deNomeFicheiro("ENTREGA.JSON"));
        assertEquals(FormatoFicheiro.XML, FormatoFicheiro.deNomeFicheiro("entrega.Xml "));
    }

    @Test
    void nomeComVariosPontosUsaAUltimaExtensao() {
        assertEquals(FormatoFicheiro.JSON, FormatoFicheiro.deNomeFicheiro("entrega.xml.json"));
    }

    @Test
    void outrasExtensoesOuSemExtensaoSaoRecusadas() {
        for (String nome : new String[]{"entrega.txt", "entrega.csv", "entrega", "json", "entrega.json.bak", "", null}) {
            IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                    () -> FormatoFicheiro.deNomeFicheiro(nome), "devia recusar: " + nome);
            assertTrue(erro.getMessage().contains("não é XML nem JSON"));
        }
    }

    @Test
    void nomeEExtensaoParaMostrarEProcurar() {
        assertEquals("JSON", FormatoFicheiro.JSON.getNome());
        assertEquals(".json", FormatoFicheiro.JSON.getExtensao());
        assertEquals("XML", FormatoFicheiro.XML.toString());
        assertEquals(".xml", FormatoFicheiro.XML.getExtensao());
    }
}
