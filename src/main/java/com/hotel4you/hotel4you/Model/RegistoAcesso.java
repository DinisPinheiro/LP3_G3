package com.hotel4you.hotel4you.Model;

import com.hotel4you.hotel4you.Utils.Constantes;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Passagem de um cartão num local do hotel, com a hora de entrada
 * (ex.: "o cartão 12345 deu entrada na piscina às 9h").
 */
public class RegistoAcesso {

    private final CartaoMagnetico cartao;
    private final LocalDateTime dataHora;
    private final int piso;
    private final String local;

    /**
     * @param cartao   cartão que passou
     * @param dataHora momento da entrada
     * @param piso     piso do local
     * @param local    nome do local (ex.: "Piscina", "Quarto 202")
     */
    public RegistoAcesso(CartaoMagnetico cartao, LocalDateTime dataHora, int piso, String local) {
        if (!Constantes.pisoValido(piso)) {
            throw new IllegalArgumentException("Piso inexistente: " + piso);
        }
        if (local == null || local.isBlank()) {
            throw new IllegalArgumentException("O local é obrigatório.");
        }
        this.cartao = Objects.requireNonNull(cartao, "O cartão é obrigatório.");
        this.dataHora = Objects.requireNonNull(dataHora, "A data/hora é obrigatória.");
        this.piso = piso;
        this.local = local.trim();
    }

    public CartaoMagnetico getCartao() {
        return cartao;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public int getPiso() {
        return piso;
    }

    public String getLocal() {
        return local;
    }
}
