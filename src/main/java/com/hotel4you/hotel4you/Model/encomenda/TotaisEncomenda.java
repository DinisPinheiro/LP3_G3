package com.hotel4you.hotel4you.Model.encomenda;

import java.math.BigDecimal;

/**
 * Totais de uma encomenda: soma das linhas sem impostos, soma das taxas e total com taxas.
 * <p>
 * Os valores são calculados pelo programa a partir das linhas (ver {@link EncomendaFornecedor#totais()})
 * e também podem vir no XML (elemento {@code OrderSummary}).
 *
 * @param totalLinhas   soma do valor das linhas sem impostos
 * @param totalTaxas    soma dos impostos (taxas) das linhas
 * @param totalComTaxas total a pagar: linhas mais taxas
 * @param moeda         código da moeda, por exemplo {@code "EUR"}
 */
public record TotaisEncomenda(BigDecimal totalLinhas, BigDecimal totalTaxas, BigDecimal totalComTaxas,
                              String moeda) {
}
