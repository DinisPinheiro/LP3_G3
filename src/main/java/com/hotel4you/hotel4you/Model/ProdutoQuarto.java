package com.hotel4you.hotel4you.Model;

import java.util.Objects;

/**
 * Equipamento de um quarto escolhido a partir dos produtos do stock ({@link Artigo}), com a quantidade
 * (S6-05). Por exemplo, um quarto duplo tem 2 toalhas, 2 champôs e 1 televisão.
 * <p>
 * No check-in estes produtos passam para a reserva e saem do stock disponível
 * ({@link Reserva#fazerCheckIn(CartaoMagnetico, java.time.LocalDateTime, java.util.List)}).
 */
public class ProdutoQuarto {

    private final Artigo artigo;
    private final int quantidade;

    /**
     * @param artigo     produto do stock
     * @param quantidade quantidade que o quarto tem, pelo menos 1
     * @throws IllegalArgumentException se a quantidade for inferior a 1
     */
    public ProdutoQuarto(Artigo artigo, int quantidade) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser pelo menos 1.");
        }
        this.artigo = Objects.requireNonNull(artigo, "O produto é obrigatório.");
        this.quantidade = quantidade;
    }

    /** @return produto do stock */
    public Artigo getArtigo() {
        return artigo;
    }

    /** @return quantidade que o quarto tem */
    public int getQuantidade() {
        return quantidade;
    }

    @Override
    public String toString() {
        return artigo.getNome() + " x" + quantidade;
    }
}
