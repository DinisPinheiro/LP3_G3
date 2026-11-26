package com.hotel4you.hotel4you.Utils;

import com.hotel4you.hotel4you.Model.encomenda.EncomendaFornecedor;
import com.hotel4you.hotel4you.Model.enums.FormatoFicheiro;

import java.io.IOException;

/**
 * Lê o ficheiro de encomenda de um fornecedor e converte-o em objetos Java ({@link EncomendaFornecedor}).
 * <p>
 * Cada formato (XML, JSON) tem a sua implementação, mas todas devolvem o mesmo objeto, por isso o resto
 * da aplicação (avisos de valores, encomenda repetida, aprovação, entrada em stock) não sabe nem precisa
 * de saber de que formato veio a encomenda (S7-03). Um formato novo só precisa de uma nova implementação.
 */
public interface LeitorEncomenda {

    /** @return o formato de ficheiro que este leitor sabe ler */
    FormatoFicheiro formato();

    /**
     * Valida o ficheiro e, se for válido, lê os seus dados. Não guarda nada na base de dados.
     *
     * @param conteudo bytes do ficheiro
     * @return a encomenda com todos os dados do ficheiro
     * @throws IOException          se o conteúdo não puder ser lido
     * @throws XmlInvalidoException se o ficheiro não for válido (traz cada erro e o motivo)
     */
    EncomendaFornecedor ler(byte[] conteudo) throws IOException;
}
