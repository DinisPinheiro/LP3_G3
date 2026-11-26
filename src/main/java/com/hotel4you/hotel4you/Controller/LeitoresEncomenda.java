package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.Model.Fornecedor;
import com.hotel4you.hotel4you.Model.enums.FormatoFicheiro;
import com.hotel4you.hotel4you.Utils.LeitorEncomenda;
import com.hotel4you.hotel4you.Utils.LeitorEncomendaJson;
import com.hotel4you.hotel4you.Utils.LeitorEncomendaXml;
import com.hotel4you.hotel4you.Utils.XsdEmFaltaException;

/**
 * Escolhe o {@link LeitorEncomenda} certo para cada formato de ficheiro (S7-03).
 * <p>
 * É o único sítio que conhece os formatos suportados: a importação do fornecedor e a aprovação do hotel
 * pedem o leitor aqui e usam-no sem saber se o ficheiro é XML ou JSON, por isso o código não se repete e um
 * formato novo só obriga a mexer nesta classe.
 */
final class LeitoresEncomenda {

    private LeitoresEncomenda() {
    }

    /**
     * @param formato    formato do ficheiro a ler
     * @param fornecedor fornecedor dono do ficheiro (o seu XSD valida os XML)
     * @param gestaoXsd  fornece o XSD de cada fornecedor
     * @return o leitor do formato
     * @throws XsdEmFaltaException se o formato for XML e o fornecedor ainda não importou o XSD (o JSON não usa XSD)
     */
    static LeitorEncomenda para(FormatoFicheiro formato, Fornecedor fornecedor, GestaoXsdController gestaoXsd) {
        return switch (formato) {
            case XML -> new LeitorEncomendaXml(gestaoXsd.obterSchema(fornecedor));
            case JSON -> new LeitorEncomendaJson();
        };
    }
}
