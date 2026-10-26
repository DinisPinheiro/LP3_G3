package com.hotel4you.hotel4you.Model;

import com.hotel4you.hotel4you.Model.enums.TipoMovimento;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Registo de uma entrada, utilização ou saída de stock, feito por um funcionário ou gestor.
 */
public class MovimentoStock {

    private final TipoMovimento tipo;
    private final int quantidade;
    private final LocalDateTime dataHora;
    private final Utilizador utilizador;
    private final Artigo artigo;

    /**
     * @param tipo       tipo de movimento
     * @param quantidade quantidade movimentada, pelo menos 1
     * @param dataHora   momento do registo
     * @param utilizador quem registou
     * @param artigo     artigo movimentado
     */
    public MovimentoStock(TipoMovimento tipo, int quantidade, LocalDateTime dataHora,
                          Utilizador utilizador, Artigo artigo) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser pelo menos 1.");
        }
        this.tipo = Objects.requireNonNull(tipo, "O tipo de movimento é obrigatório.");
        this.quantidade = quantidade;
        this.dataHora = Objects.requireNonNull(dataHora, "A data/hora é obrigatória.");
        this.utilizador = Objects.requireNonNull(utilizador, "O utilizador é obrigatório.");
        this.artigo = Objects.requireNonNull(artigo, "O artigo é obrigatório.");
    }

    public TipoMovimento getTipo() {
        return tipo;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public Utilizador getUtilizador() {
        return utilizador;
    }

    public Artigo getArtigo() {
        return artigo;
    }
}
