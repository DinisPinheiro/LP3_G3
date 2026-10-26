package com.hotel4you.hotel4you.Model.enums;

import java.math.BigDecimal;

public enum Vista {
    NENHUMA("Sem vista especial", new BigDecimal("0.00")),
    MAR("Vista mar", new BigDecimal("20.00")),
    MONTANHA("Vista montanha", new BigDecimal("15.00"));

    private final String nome;
    private final BigDecimal suplemento;

    Vista(String nome, BigDecimal suplemento) {
        this.nome = nome;
        this.suplemento = suplemento;
    }

    /** @return nome para mostrar ao utilizador */
    public String getNome() {
        return nome;
    }

    /** @return valor a somar ao preço base por noite */
    public BigDecimal getSuplemento() {
        return suplemento;
    }
}
