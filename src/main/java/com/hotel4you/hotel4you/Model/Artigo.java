package com.hotel4you.hotel4you.Model;

import java.util.Objects;

/**
 * Artigo em stock (toalhas, champô, lençóis, camas, ...). O stock nunca fica negativo.
 */
public class Artigo {

    private int id;
    private String nome;
    private String unidade;
    private int quantidadeAtual;

    /**
     * @param id              identificador (0 se ainda não foi guardado)
     * @param nome            nome do artigo
     * @param unidade         unidade de medida (ex.: "un", "cx")
     * @param quantidadeAtual stock atual, não negativo
     */
    public Artigo(int id, String nome, String unidade, int quantidadeAtual) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome do artigo é obrigatório.");
        }
        if (quantidadeAtual < 0) {
            throw new IllegalArgumentException("O stock não pode ser negativo.");
        }
        this.id = id;
        this.nome = nome.trim();
        this.unidade = unidade == null ? "" : unidade.trim();
        this.quantidadeAtual = quantidadeAtual;
    }

    /**
     * Atualiza o stock com um movimento: entradas somam; utilizações e saídas subtraem.
     *
     * @param movimento movimento a aplicar
     * @throws IllegalStateException se o stock não chegar para a utilização/saída
     */
    public void aplicar(MovimentoStock movimento) {
        Objects.requireNonNull(movimento, "O movimento é obrigatório.");
        int quantidade = movimento.getQuantidade();
        if (movimento.getTipo().aumentaStock()) {
            quantidadeAtual += quantidade;
        } else {
            if (quantidade > quantidadeAtual) {
                throw new IllegalStateException("Stock insuficiente de '" + nome + "': existem "
                        + quantidadeAtual + " e foram pedidos " + quantidade + ".");
            }
            quantidadeAtual -= quantidade;
        }
    }

    public int getId() {
        return id;
    }

    /** Usado pelo DAO ao guardar o artigo pela primeira vez. */
    public void setId(int id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public String getUnidade() {
        return unidade;
    }

    public int getQuantidadeAtual() {
        return quantidadeAtual;
    }

    @Override
    public String toString() {
        return nome + " (" + quantidadeAtual + " " + unidade + ")";
    }
}
