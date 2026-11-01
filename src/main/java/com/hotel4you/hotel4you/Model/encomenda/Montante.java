package com.hotel4you.hotel4you.Model.encomenda;

import java.math.BigDecimal;

/**
 * Valor monetário de uma encomenda (elemento {@code CurrencyValue} do XML).
 *
 * @param valor valor em decimal (nunca {@code double}, para não perder cêntimos)
 * @param moeda código da moeda, por exemplo {@code "EUR"}
 */
public record Montante(BigDecimal valor, String moeda) {
}
