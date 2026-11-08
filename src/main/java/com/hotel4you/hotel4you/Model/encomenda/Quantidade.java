package com.hotel4you.hotel4you.Model.encomenda;

import com.hotel4you.hotel4you.Model.enums.UnidadeMedida;

/**
 * Quantidade de uma linha da encomenda (elementos {@code Quantity} e {@code InformationalQuantity}).
 *
 * @param tipo    tipo de quantidade no XML, por exemplo {@code "Count"} ou {@code "NetWeight"}
 * @param valor   valor da quantidade
 * @param unidade unidade de medida
 */
public record Quantidade(String tipo, int valor, UnidadeMedida unidade) {
}
