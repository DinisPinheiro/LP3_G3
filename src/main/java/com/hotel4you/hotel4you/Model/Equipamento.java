package com.hotel4you.hotel4you.Model;

/**
 * Item de equipamento de um quarto (toalhas, secador, televisão, cofre, ...).
 * O gestor configura quais os equipamentos de cada quarto.
 */
public class Equipamento {

    private int id;
    private final String nome;

    /**
     * @param id   identificador (0 se ainda não foi guardado)
     * @param nome nome do equipamento
     */
    public Equipamento(int id, String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome do equipamento é obrigatório.");
        }
        this.id = id;
        this.nome = nome.trim();
    }

    public int getId() {
        return id;
    }

    /** Usado pelo DAO ao guardar o equipamento pela primeira vez. */
    public void setId(int id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    @Override
    public String toString() {
        return nome;
    }
}
