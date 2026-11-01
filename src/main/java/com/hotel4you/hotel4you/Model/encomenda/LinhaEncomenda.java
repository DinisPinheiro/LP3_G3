package com.hotel4you.hotel4you.Model.encomenda;

import java.util.List;

/**
 * Uma linha (produto) da encomenda do fornecedor (elemento {@code Line} do XML).
 *
 * @param numero                  número da linha
 * @param codigoProduto           código do produto dado pelo fornecedor
 * @param agencia                 quem atribuiu o código (atributo {@code Agency}, por exemplo {@code "Supplier"})
 * @param descricao               descrição do produto
 * @param quantidade              quantidade encomendada
 * @param quantidadesInformativas quantidades só informativas (peso, número de caixas, ...)
 * @param precoUnitario           preço por unidade
 * @param imposto                 imposto da linha
 * @param valorBase               valor total da linha
 */
public record LinhaEncomenda(int numero, String codigoProduto, String agencia, String descricao,
                             Quantidade quantidade, List<Quantidade> quantidadesInformativas,
                             PrecoUnitario precoUnitario, ImpostoLinha imposto, Montante valorBase) {

    /** Garante que a lista não pode ser alterada por quem recebe a linha. */
    public LinhaEncomenda {
        quantidadesInformativas = List.copyOf(quantidadesInformativas);
    }
}
