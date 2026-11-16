package com.hotel4you.hotel4you.Model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Artigo em stock (toalhas, champô, lençóis, camas, ...). O stock nunca fica negativo.
 * <p>
 * O stock do hotel ({@link #getQuantidadeAtual()}) é separado do stock disponível
 * ({@link #getQuantidadeDisponivel()}), que desconta o que já está usado em reservas (S5-06).
 */
public class Artigo {

    private int id;
    private String nome;
    private String unidade;
    private int quantidadeAtual;
    /** Quantidade usada em reservas não canceladas (calculada pelo DAO). */
    private int quantidadeReservada;
    private Fornecedor fornecedor;
    /** Preço unitário da última entrada (sem imposto), ou {@code null} se ainda não se sabe. */
    private BigDecimal precoUnitario;
    /** Unidade a que o preço se refere (ex.: "cx"). */
    private String unidadePreco;
    /** Taxa de imposto da última entrada, em percentagem (ex.: 23), ou {@code null}. */
    private Integer taxaPercentagem;

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

    /**
     * @return quantidade usada em reservas não canceladas
     */
    public int getQuantidadeReservada() {
        return quantidadeReservada;
    }

    /**
     * Indica quanto deste artigo está usado em reservas. Não altera o stock do hotel.
     *
     * @param quantidadeReservada quantidade usada em reservas não canceladas, não negativa
     * @throws IllegalArgumentException se for negativa
     */
    public void setQuantidadeReservada(int quantidadeReservada) {
        if (quantidadeReservada < 0) {
            throw new IllegalArgumentException("A quantidade usada em reservas não pode ser negativa.");
        }
        this.quantidadeReservada = quantidadeReservada;
    }

    /**
     * Stock disponível para novas reservas: o stock do hotel menos o que já está usado em reservas.
     *
     * @return quantidade disponível (nunca negativa)
     */
    public int getQuantidadeDisponivel() {
        return Math.max(0, quantidadeAtual - quantidadeReservada);
    }

    /** @return o fornecedor da última entrada deste artigo, ou {@code null} se não tiver */
    public Fornecedor getFornecedor() {
        return fornecedor;
    }

    /** @param fornecedor fornecedor da última entrada deste artigo; pode ser {@code null} */
    public void setFornecedor(Fornecedor fornecedor) {
        this.fornecedor = fornecedor;
    }

    /** @return o preço unitário (sem imposto) da última entrada, ou {@code null} se não se sabe */
    public BigDecimal getPrecoUnitario() {
        return precoUnitario;
    }

    /** @return a unidade a que o preço se refere (ex.: "cx"), ou vazio se não há preço */
    public String getUnidadePreco() {
        return unidadePreco == null ? "" : unidadePreco;
    }

    /** @return a taxa de imposto da última entrada em percentagem (ex.: 23), ou {@code null} se não se sabe */
    public Integer getTaxaPercentagem() {
        return taxaPercentagem;
    }

    /**
     * Guarda o preço e a taxa da última entrada deste artigo. Não altera o stock.
     *
     * @param preco         preço unitário sem imposto, não negativo ({@code null} limpa o preço)
     * @param unidadePreco  unidade a que o preço se refere
     * @param taxaPercentagem taxa de imposto entre 0 e 100; pode ser {@code null}
     * @throws IllegalArgumentException se o preço for negativo ou a taxa estiver fora de 0 a 100
     */
    public void setUltimoPreco(BigDecimal preco, String unidadePreco, Integer taxaPercentagem) {
        if (preco != null && preco.signum() < 0) {
            throw new IllegalArgumentException("O preço unitário não pode ser negativo.");
        }
        if (taxaPercentagem != null && (taxaPercentagem < 0 || taxaPercentagem > 100)) {
            throw new IllegalArgumentException("A taxa de imposto tem de estar entre 0 e 100.");
        }
        this.precoUnitario = preco;
        this.unidadePreco = unidadePreco == null ? null : unidadePreco.trim();
        this.taxaPercentagem = taxaPercentagem;
    }

    @Override
    public String toString() {
        return nome + " (" + quantidadeAtual + " " + unidade + ")";
    }
}
