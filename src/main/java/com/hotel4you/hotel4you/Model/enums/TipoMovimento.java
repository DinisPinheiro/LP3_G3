package com.hotel4you.hotel4you.Model.enums;

/**
 * Tipo de movimento de stock: entrada, utilização e saída (enunciado, secção 2).
 */
public enum TipoMovimento {
    ENTRADA(true),
    UTILIZACAO(false),
    SAIDA(false);

    private final boolean aumentaStock;

    TipoMovimento(boolean aumentaStock) {
        this.aumentaStock = aumentaStock;
    }

    /** @return {@code true} se este movimento soma ao stock do artigo */
    public boolean aumentaStock() {
        return aumentaStock;
    }
}
