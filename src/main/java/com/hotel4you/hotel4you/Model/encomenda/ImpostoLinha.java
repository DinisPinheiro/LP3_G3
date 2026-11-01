package com.hotel4you.hotel4you.Model.encomenda;

/**
 * Imposto aplicado a uma linha (elemento {@code MonetaryAdjustment} com {@code TaxAdjustment}).
 *
 * @param tipoAjuste    tipo de ajuste monetário, por exemplo {@code "Tax"}
 * @param numeroAjuste  número do ajuste dentro da linha
 * @param baseCalculo   valor sobre o qual o imposto é calculado
 * @param tipoImposto   tipo de imposto, por exemplo {@code "VAT"}
 * @param percentagem   taxa em percentagem, por exemplo 23
 * @param valorImposto  valor do imposto
 * @param local         país onde o imposto se aplica, por exemplo {@code "PT"}
 */
public record ImpostoLinha(String tipoAjuste, int numeroAjuste, Montante baseCalculo, String tipoImposto,
                           int percentagem, Montante valorImposto, String local) {
}
