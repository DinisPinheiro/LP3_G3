package com.hotel4you.hotel4you.Utils;

import com.hotel4you.hotel4you.Model.encomenda.EncomendaFornecedor;

import javax.xml.validation.Schema;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Ficheiros de {@code src/test/resources/xml} e atalhos para os usar nos testes. A aplicação não traz
 * nenhum XSD seu (o XSD é importado pelo fornecedor, S6-02), por isso os testes usam este.
 */
public final class RecursosDeTeste {

    private RecursosDeTeste() {
    }

    /** @return caminho de um ficheiro de {@code src/test/resources/xml} */
    public static Path recurso(String nome) {
        return Paths.get("src", "test", "resources", "xml", nome);
    }

    /** @return o XSD de teste que o fornecedor importa (o do enunciado, {@code Hotel4You.xsd}) */
    public static Path xsd() {
        return recurso("Hotel4You.xsd");
    }

    /** @return o esquema compilado do XSD de teste */
    public static Schema schema() {
        try {
            return ValidadorXml.carregarSchema(new ByteArrayInputStream(Files.readAllBytes(xsd())));
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível ler o XSD de teste.", e);
        }
    }

    /**
     * Valida um XML com o XSD de teste e lê-o, como faz a importação do fornecedor, incluindo a
     * verificação dos valores ({@link EncomendaFornecedor#validarValores()}).
     *
     * @param ficheiro caminho do XML
     * @return a encomenda lida
     * @throws IOException          se o ficheiro não puder ser lido
     * @throws XmlInvalidoException se o XML não cumprir o XSD
     */
    public static EncomendaFornecedor ler(Path ficheiro) throws IOException {
        EncomendaFornecedor encomenda = LeitorEncomendaXml.ler(Files.readAllBytes(ficheiro), schema());
        encomenda.validarValores();
        return encomenda;
    }
}
