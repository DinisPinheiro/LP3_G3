package com.hotel4you.hotel4you.Model.encomenda;

import com.hotel4you.hotel4you.Model.enums.UnidadeMedida;

/**
 * Preço de uma unidade de medida (elemento {@code PricePerUnit}), por exemplo 2,51 EUR por 1 caixa.
 *
 * @param preco         montante cobrado
 * @param porQuantidade quantidade a que o preço se refere (normalmente 1)
 * @param unidade       unidade a que o preço se refere
 */
public record PrecoUnitario(Montante preco, int porQuantidade, UnidadeMedida unidade) {
}
