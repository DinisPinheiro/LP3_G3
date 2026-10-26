package com.hotel4you.hotel4you.Model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Consumo de um {@link Servico} durante uma {@link Reserva}.
 */
public class ConsumoServico {

    private final Servico servico;
    private final int quantidade;
    private final LocalDate data;

    /**
     * @param servico    serviço consumido
     * @param quantidade quantidade, pelo menos 1
     * @param data       dia do consumo
     */
    public ConsumoServico(Servico servico, int quantidade, LocalDate data) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser pelo menos 1.");
        }
        this.servico = Objects.requireNonNull(servico, "O serviço é obrigatório.");
        this.quantidade = quantidade;
        this.data = Objects.requireNonNull(data, "A data é obrigatória.");
    }

    /** @return valor do serviço multiplicado pela quantidade */
    public BigDecimal total() {
        return servico.getValor().multiply(BigDecimal.valueOf(quantidade));
    }

    public Servico getServico() {
        return servico;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public LocalDate getData() {
        return data;
    }
}
