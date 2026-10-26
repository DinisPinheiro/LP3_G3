package com.hotel4you.hotel4you.Model;

import com.hotel4you.hotel4you.Model.enums.EstadoReserva;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Reserva de um quarto por um cliente, de uma ou mais noites.
 * <p>
 * Ciclo de vida: {@link EstadoReserva#PENDENTE} → {@link EstadoReserva#CHECK_IN}
 * → {@link EstadoReserva#CHECK_OUT}, ou {@link EstadoReserva#CANCELADA} antes do check-in.
 * Operações fora deste ciclo lançam {@link IllegalStateException}.
 */
public class Reserva {

    private int id;
    private final Cliente cliente;
    private final Quarto quarto;
    private final LocalDate dataEntrada;
    private final LocalDate dataSaida;
    private EstadoReserva estado;
    private CartaoMagnetico cartao;
    private LocalDateTime dataCheckIn;
    private LocalDateTime dataCheckOut;
    private final List<ConsumoServico> consumos = new ArrayList<>();

    /**
     * Cria uma reserva nova, em estado pendente.
     *
     * @param id          identificador (0 se ainda não foi guardada)
     * @param cliente     cliente que reserva
     * @param quarto      quarto reservado
     * @param dataEntrada primeira noite
     * @param dataSaida   dia de saída (tem de ser depois da entrada: mínimo uma noite)
     * @throws IllegalArgumentException se a saída não for posterior à entrada
     */
    public Reserva(int id, Cliente cliente, Quarto quarto, LocalDate dataEntrada, LocalDate dataSaida) {
        this(id, cliente, quarto, dataEntrada, dataSaida, EstadoReserva.PENDENTE, null, null, null, List.of());
    }

    /**
     * Reconstrói uma reserva com todo o seu estado (usado pelos DAO).
     *
     * @param id           identificador
     * @param cliente      cliente
     * @param quarto       quarto
     * @param dataEntrada  primeira noite
     * @param dataSaida    dia de saída
     * @param estado       estado atual
     * @param cartao       cartão entregue no check-in, ou {@code null}
     * @param dataCheckIn  momento do check-in, ou {@code null}
     * @param dataCheckOut momento do check-out, ou {@code null}
     * @param consumos     consumos já registados
     */
    public Reserva(int id, Cliente cliente, Quarto quarto, LocalDate dataEntrada, LocalDate dataSaida,
                   EstadoReserva estado, CartaoMagnetico cartao,
                   LocalDateTime dataCheckIn, LocalDateTime dataCheckOut, List<ConsumoServico> consumos) {
        this.cliente = Objects.requireNonNull(cliente, "O cliente é obrigatório.");
        this.quarto = Objects.requireNonNull(quarto, "O quarto é obrigatório.");
        this.dataEntrada = Objects.requireNonNull(dataEntrada, "A data de entrada é obrigatória.");
        this.dataSaida = Objects.requireNonNull(dataSaida, "A data de saída é obrigatória.");
        if (!dataSaida.isAfter(dataEntrada)) {
            throw new IllegalArgumentException("A data de saída deve ser posterior à de entrada.");
        }
        this.id = id;
        this.estado = Objects.requireNonNull(estado, "O estado é obrigatório.");
        this.cartao = cartao;
        this.dataCheckIn = dataCheckIn;
        this.dataCheckOut = dataCheckOut;
        this.consumos.addAll(consumos);
    }

    /**
     * Efetiva o check-in, entregando o cartão magnético ao cliente.
     *
     * @param cartao cartão entregue
     * @throws IllegalStateException se a reserva não estiver pendente
     */
    public void fazerCheckIn(CartaoMagnetico cartao) {
        fazerCheckIn(cartao, LocalDateTime.now());
    }

    /**
     * Igual a {@link #fazerCheckIn(CartaoMagnetico)} mas com o momento indicado (útil em testes).
     *
     * @param cartao cartão entregue
     * @param quando momento do check-in
     */
    public void fazerCheckIn(CartaoMagnetico cartao, LocalDateTime quando) {
        if (estado != EstadoReserva.PENDENTE) {
            throw new IllegalStateException("Só é possível fazer check-in de reservas pendentes (estado: " + estado + ").");
        }
        this.cartao = Objects.requireNonNull(cartao, "O cartão é obrigatório.");
        this.dataCheckIn = Objects.requireNonNull(quando);
        this.estado = EstadoReserva.CHECK_IN;
    }

    /**
     * Efetiva o check-out.
     *
     * @throws IllegalStateException se o check-in ainda não foi feito
     */
    public void fazerCheckOut() {
        fazerCheckOut(LocalDateTime.now());
    }

    /**
     * Igual a {@link #fazerCheckOut()} mas com o momento indicado (útil em testes).
     *
     * @param quando momento do check-out
     */
    public void fazerCheckOut(LocalDateTime quando) {
        if (estado != EstadoReserva.CHECK_IN) {
            throw new IllegalStateException("Só é possível fazer check-out após o check-in (estado: " + estado + ").");
        }
        this.dataCheckOut = Objects.requireNonNull(quando);
        this.estado = EstadoReserva.CHECK_OUT;
    }

    /**
     * Cancela a reserva.
     *
     * @throws IllegalStateException se o check-in já foi efetuado ou a reserva já está cancelada
     */
    public void cancelar() {
        if (estado != EstadoReserva.PENDENTE) {
            throw new IllegalStateException("Só é possível cancelar reservas pendentes (estado: " + estado + ").");
        }
        this.estado = EstadoReserva.CANCELADA;
    }

    /**
     * Regra do enunciado: só se pode apagar uma reserva se o check-in não foi efetuado.
     * Quem tem permissão para apagar é decidido por {@link Utilizador#podeApagarReservas()}.
     *
     * @return {@code true} se nunca houve check-in
     */
    public boolean podeSerApagada() {
        return dataCheckIn == null;
    }

    /**
     * Regista um consumo (bebidas, lavandaria, ...) durante a estadia.
     *
     * @param consumo consumo a somar ao total
     * @throws IllegalStateException se o cliente não estiver instalado (sem check-in ativo)
     */
    public void adicionarConsumo(ConsumoServico consumo) {
        if (estado != EstadoReserva.CHECK_IN) {
            throw new IllegalStateException("Só se registam consumos durante a estadia (estado: " + estado + ").");
        }
        consumos.add(Objects.requireNonNull(consumo, "O consumo é obrigatório."));
    }

    /** @return número de noites da estadia */
    public long getNoites() {
        return ChronoUnit.DAYS.between(dataEntrada, dataSaida);
    }

    /**
     * Total a pagar: noites × preço por noite do quarto + consumos.
     * Uma reserva cancelada não tem custo.
     *
     * @return total da reserva
     */
    public BigDecimal calcularTotal() {
        if (estado == EstadoReserva.CANCELADA) {
            return BigDecimal.ZERO;
        }
        BigDecimal alojamento = quarto.precoNoite().multiply(BigDecimal.valueOf(getNoites()));
        BigDecimal extras = consumos.stream()
                .map(ConsumoServico::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return alojamento.add(extras);
    }

    public int getId() {
        return id;
    }

    /** Usado pelo DAO ao guardar a reserva pela primeira vez. */
    public void setId(int id) {
        this.id = id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Quarto getQuarto() {
        return quarto;
    }

    public LocalDate getDataEntrada() {
        return dataEntrada;
    }

    public LocalDate getDataSaida() {
        return dataSaida;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    /** @return cartão entregue no check-in, ou {@code null} se ainda não houve */
    public CartaoMagnetico getCartao() {
        return cartao;
    }

    public LocalDateTime getDataCheckIn() {
        return dataCheckIn;
    }

    public LocalDateTime getDataCheckOut() {
        return dataCheckOut;
    }

    /** @return lista só de leitura dos consumos */
    public List<ConsumoServico> getConsumos() {
        return Collections.unmodifiableList(consumos);
    }
}
