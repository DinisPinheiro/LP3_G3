package com.hotel4you.hotel4you.Model.encomenda;

import java.time.LocalDate;
import java.util.List;

/**
 * Encomenda enviada por um fornecedor em ficheiro XML (elemento {@code Order}).
 * <p>
 * Esta classe só guarda os dados lidos do ficheiro. Nesta sprint não dá entrada em stock.
 *
 * @param numero  número da encomenda, por exemplo {@code "OC56974"}
 * @param data    data da encomenda
 * @param fornecedor fornecedor que enviou a encomenda
 * @param linhas  produtos encomendados (pelo menos um)
 */
public record EncomendaFornecedor(String numero, LocalDate data, FornecedorEncomenda fornecedor,
                                  List<LinhaEncomenda> linhas) {

    /** Garante que a lista não pode ser alterada por quem recebe a encomenda. */
    public EncomendaFornecedor {
        linhas = List.copyOf(linhas);
    }
}
