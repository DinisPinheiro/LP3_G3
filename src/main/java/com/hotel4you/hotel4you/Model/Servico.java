package com.hotel4you.hotel4you.Model;

import java.math.BigDecimal;

/**
 * Serviço oferecido pelo hotel (alimentação, spa, lavandaria, bebidas, sala de conferência, ...)
 * com o valor acrescentado ao cliente.
 */
public class Servico {

    private int id;
    private String nome;
    private String categoria;
    private BigDecimal valor;

    /**
     * @param id        identificador (0 se ainda não foi guardado)
     * @param nome      nome do serviço
     * @param categoria categoria (ex.: "Bebidas", "Lavandaria")
     * @param valor     valor unitário, não negativo
     */
    public Servico(int id, String nome, String categoria, BigDecimal valor) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome do serviço é obrigatório.");
        }
        if (valor == null || valor.signum() < 0) {
            throw new IllegalArgumentException("O valor do serviço não pode ser negativo.");
        }
        this.id = id;
        this.nome = nome.trim();
        this.categoria = categoria == null ? "" : categoria.trim();
        this.valor = valor;
    }

    public int getId() {
        return id;
    }

    /** Usado pelo DAO ao guardar o serviço pela primeira vez. */
    public void setId(int id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public String getCategoria() {
        return categoria;
    }

    public BigDecimal getValor() {
        return valor;
    }

    @Override
    public String toString() {
        return nome;
    }
}
