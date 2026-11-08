package com.hotel4you.hotel4you.Model.enums;

import java.math.BigDecimal;

/**
 * Vistas de um quarto. {@link #getSuplemento()} é o valor <b>por omissão</b> (o que a base de dados traz de
 * início). O valor em vigor das vistas especiais é editável pelo gestor e fica na tabela
 * {@code dbo.SuplementoVista} (ver {@code GestaoVistasController}).
 */
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

    /** @return valor por omissão a somar ao preço base por noite (o valor em vigor está na base de dados) */
    public BigDecimal getSuplemento() {
        return suplemento;
    }

    /** @return {@code true} para as vistas especiais (mar, montanha), cujo valor o gestor pode editar */
    public boolean isEspecial() {
        return this != NENHUMA;
    }
}
