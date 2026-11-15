package com.hotel4you.hotel4you.Model;

import java.util.Objects;

/**
 * Produto do stock ({@link Artigo}) usado por uma {@link Reserva}, com a quantidade (S5-06).
 * <p>
 * Os produtos não têm preço para o cliente: não entram no total da reserva, mas a quantidade
 * usada é descontada ao stock disponível ({@link Artigo#getQuantidadeDisponivel()}).
 */
public class ProdutoReserva {

    private final Artigo artigo;
    private final int quantidade;

    /**
     * @param artigo     produto do stock
     * @param quantidade quantidade usada, pelo menos 1
     * @throws IllegalArgumentException se a quantidade for inferior a 1
     */
    public ProdutoReserva(Artigo artigo, int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser pelo menos 1.");
        }
        this.artigo = Objects.requireNonNull(artigo, "O produto é obrigatório.");
        this.quantidade = quantidade;
    }

    /**
     * @return produto do stock
     */
    public Artigo getArtigo() {
        return artigo;
    }

    /**
     * @return quantidade usada pela reserva
     */
    public int getQuantidade() {
        return quantidade;
    }
}
