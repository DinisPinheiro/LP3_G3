package com.hotel4you.hotel4you.Model;

import com.hotel4you.hotel4you.Model.enums.TipoCobranca;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Serviço ou extra ({@link Servico}) associado a uma {@link Reserva}, com a quantidade.
 * <p>
 * O valor e a forma de cobrança ficam guardados aqui no momento em que a linha é criada (S5-09):
 * se o gestor alterar depois o preço do serviço, as reservas já feitas não mudam.
 */
public class ConsumoServico {

    private final Servico servico;
    private final int quantidade;
    private final LocalDate data;
    private final BigDecimal valorUnitario;
    private final TipoCobranca cobranca;

    /**
     * Cria uma linha nova, com o valor e a forma de cobrança que o serviço tem neste momento.
     *
     * @param servico    serviço consumido
     * @param quantidade quantidade, pelo menos 1
     * @param data       dia do consumo
     */
    public ConsumoServico(Servico servico, int quantidade, LocalDate data) {
        this(servico, quantidade, data,
                Objects.requireNonNull(servico, "O serviço é obrigatório.").getValor(), servico.getCobranca());
    }

    /**
     * Reconstrói uma linha já guardada, com o valor e a forma de cobrança que ficaram na reserva
     * (usado pelos DAO).
     *
     * @param servico       serviço consumido
     * @param quantidade    quantidade, pelo menos 1
     * @param data          dia do consumo
     * @param valorUnitario valor do serviço guardado na reserva, não negativo
     * @param cobranca      forma de cobrança guardada na reserva
     * @throws IllegalArgumentException se a quantidade for inferior a 1 ou o valor for negativo
     */
    public ConsumoServico(Servico servico, int quantidade, LocalDate data,
                          BigDecimal valorUnitario, TipoCobranca cobranca) {
        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser pelo menos 1.");
        }
        if (valorUnitario == null || valorUnitario.signum() < 0) {
            throw new IllegalArgumentException("O valor do serviço não pode ser negativo.");
        }
        this.servico = Objects.requireNonNull(servico, "O serviço é obrigatório.");
        this.quantidade = quantidade;
        this.data = Objects.requireNonNull(data, "A data é obrigatória.");
        this.valorUnitario = valorUnitario;
        this.cobranca = Objects.requireNonNull(cobranca, "A forma de cobrança é obrigatória.");
    }

    /**
     * Total da linha: valor guardado × quantidade, e ainda × noites se o serviço for cobrado por noite.
     *
     * @param noites número de noites da estadia
     * @return total da linha
     */
    public BigDecimal total(long noites) {
        BigDecimal total = valorUnitario.multiply(BigDecimal.valueOf(quantidade));
        if (cobranca == TipoCobranca.POR_NOITE) {
            total = total.multiply(BigDecimal.valueOf(noites));
        }
        return total;
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

    /**
     * @return valor do serviço guardado na reserva (não muda quando o gestor altera o preço)
     */
    public BigDecimal getValorUnitario() {
        return valorUnitario;
    }

    /**
     * @return forma de cobrança guardada na reserva (por unidade ou por noite)
     */
    public TipoCobranca getCobranca() {
        return cobranca;
    }
}
