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
 * <p>
 * No check-in a reserva recebe um cartão magnético ativo (nunca o cartão mestre); no check-out
 * o cartão é desativado e a reserva deixa de ocupar o quarto.
 * <p>
 * A reserva pode levar serviços e extras ({@link ConsumoServico}) e produtos do stock
 * ({@link ProdutoReserva}), cada um com a sua quantidade (S5-06).
 * <p>
 * Os preços ficam guardados na reserva quando ela é criada (S5-09): o preço base do quarto, o valor
 * da vista e o valor de cada serviço. Se o gestor os alterar depois, só as reservas novas mudam.
 */
public class Reserva {

    private int id;
    private final Cliente cliente;
    private final Quarto quarto;
    private final LocalDate dataEntrada;
    private final LocalDate dataSaida;
    /** Preço base do quarto por noite, guardado quando a reserva foi criada. */
    private final BigDecimal precoBaseNoite;
    /** Valor da vista por noite, guardado quando a reserva foi criada. */
    private final BigDecimal valorVistaNoite;
    private EstadoReserva estado;
    private CartaoMagnetico cartao;
    private LocalDateTime dataCheckIn;
    private LocalDateTime dataCheckOut;
    private final List<ConsumoServico> consumos = new ArrayList<>();
    private final List<ProdutoReserva> produtos = new ArrayList<>();

    /**
     * Cria uma reserva nova, em estado pendente, sem serviços nem produtos.
     *
     * @param id          identificador (0 se ainda não foi guardada)
     * @param cliente     cliente que reserva
     * @param quarto      quarto reservado
     * @param dataEntrada primeira noite
     * @param dataSaida   dia de saída (tem de ser depois da entrada: mínimo uma noite)
     * @throws IllegalArgumentException se a saída não for posterior à entrada
     */
    public Reserva(int id, Cliente cliente, Quarto quarto, LocalDate dataEntrada, LocalDate dataSaida) {
        this(id, cliente, quarto, dataEntrada, dataSaida, List.of(), List.of());
    }

    /**
     * Cria uma reserva nova, em estado pendente, com os serviços e os produtos escolhidos.
     * Os preços do quarto (preço base e valor da vista) em vigor neste momento ficam guardados
     * na reserva.
     *
     * @param id          identificador (0 se ainda não foi guardada)
     * @param cliente     cliente que reserva
     * @param quarto      quarto reservado
     * @param dataEntrada primeira noite
     * @param dataSaida   dia de saída (tem de ser depois da entrada: mínimo uma noite)
     * @param servicos    serviços e extras escolhidos, com a quantidade
     * @param produtos    produtos do stock escolhidos, com a quantidade
     * @throws IllegalArgumentException se a saída não for posterior à entrada
     */
    public Reserva(int id, Cliente cliente, Quarto quarto, LocalDate dataEntrada, LocalDate dataSaida,
                   List<ConsumoServico> servicos, List<ProdutoReserva> produtos) {
        this(id, cliente, quarto, dataEntrada, dataSaida,
                Objects.requireNonNull(quarto, "O quarto é obrigatório.").getPrecoBase(), quarto.getSuplementoVista(),
                EstadoReserva.PENDENTE, null, null, null, servicos, produtos);
    }

    /**
     * Reconstrói uma reserva com todo o seu estado (usado pelos DAO).
     *
     * @param id              identificador
     * @param cliente         cliente
     * @param quarto          quarto
     * @param dataEntrada     primeira noite
     * @param dataSaida       dia de saída
     * @param precoBaseNoite  preço base do quarto por noite guardado na reserva
     * @param valorVistaNoite valor da vista por noite guardado na reserva
     * @param estado          estado atual
     * @param cartao          cartão entregue no check-in, ou {@code null}
     * @param dataCheckIn     momento do check-in, ou {@code null}
     * @param dataCheckOut    momento do check-out, ou {@code null}
     * @param consumos        serviços e extras já registados
     * @param produtos        produtos do stock já registados
     * @throws IllegalArgumentException se a saída não for posterior à entrada ou algum preço for negativo
     */
    public Reserva(int id, Cliente cliente, Quarto quarto, LocalDate dataEntrada, LocalDate dataSaida,
                   BigDecimal precoBaseNoite, BigDecimal valorVistaNoite,
                   EstadoReserva estado, CartaoMagnetico cartao,
                   LocalDateTime dataCheckIn, LocalDateTime dataCheckOut,
                   List<ConsumoServico> consumos, List<ProdutoReserva> produtos) {
        this.cliente = Objects.requireNonNull(cliente, "O cliente é obrigatório.");
        this.quarto = Objects.requireNonNull(quarto, "O quarto é obrigatório.");
        this.dataEntrada = Objects.requireNonNull(dataEntrada, "A data de entrada é obrigatória.");
        this.dataSaida = Objects.requireNonNull(dataSaida, "A data de saída é obrigatória.");
        if (!dataSaida.isAfter(dataEntrada)) {
            throw new IllegalArgumentException("A data de saída deve ser posterior à de entrada.");
        }
        this.id = id;
        this.precoBaseNoite = validarPreco(precoBaseNoite, "O preço base");
        this.valorVistaNoite = validarPreco(valorVistaNoite, "O valor da vista");
        this.estado = Objects.requireNonNull(estado, "O estado é obrigatório.");
        this.cartao = cartao;
        this.dataCheckIn = dataCheckIn;
        this.dataCheckOut = dataCheckOut;
        this.consumos.addAll(consumos);
        this.produtos.addAll(produtos);
    }

    /**
     * Efetiva o check-in, entregando o cartão magnético ao cliente.
     *
     * @param cartao cartão entregue
     * @throws IllegalStateException    se a reserva não estiver pendente
     * @throws IllegalArgumentException se o cartão for o mestre ou estiver desativado
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
        Objects.requireNonNull(cartao, "O cartão é obrigatório.");
        if (cartao.isMestre()) {
            throw new IllegalArgumentException("O cartão mestre não pode ser atribuído a um cliente.");
        }
        if (!cartao.isAtivo()) {
            throw new IllegalArgumentException("O cartão " + cartao.getNumero() + " está desativado.");
        }
        this.cartao = cartao;
        this.dataCheckIn = Objects.requireNonNull(quando);
        this.estado = EstadoReserva.CHECK_IN;
    }

    /**
     * Efetiva o check-out: regista a saída, desativa o cartão do cliente e deixa o quarto livre
     * (só as reservas pendentes ou com check-in ocupam o quarto).
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
        if (cartao != null) {
            cartao.desativar();
        }
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
     * Preço de uma noite desta reserva: preço base do quarto mais o valor da vista, os dois
     * guardados quando a reserva foi criada.
     *
     * @return preço por noite
     */
    public BigDecimal getPrecoNoite() {
        return precoBaseNoite.add(valorVistaNoite);
    }

    /**
     * Total a pagar: noites × (preço base + vista) + serviços e extras, com os preços guardados
     * na reserva. Uma reserva cancelada não tem custo.
     *
     * @return total da reserva
     */
    public BigDecimal calcularTotal() {
        if (estado == EstadoReserva.CANCELADA) {
            return BigDecimal.ZERO;
        }
        return calcularTotal(getPrecoNoite(), getNoites(), consumos);
    }

    /**
     * Conta do total de uma estadia: noites × preço por noite + serviços e extras (cada um com a sua
     * quantidade, por unidade ou por noite). É a única conta do total: usam-na a reserva e a
     * estimativa mostrada antes de a reserva ser criada.
     *
     * @param precoNoite preço de uma noite (preço base + vista)
     * @param noites     número de noites
     * @param servicos   serviços e extras escolhidos
     * @return total da estadia
     */
    public static BigDecimal calcularTotal(BigDecimal precoNoite, long noites, List<ConsumoServico> servicos) {
        BigDecimal total = precoNoite.multiply(BigDecimal.valueOf(noites));
        for (ConsumoServico servico : servicos) {
            total = total.add(servico.total(noites));
        }
        return total;
    }

    /**
     * Quantidade de um artigo do stock usada por esta reserva. Uma reserva cancelada não usa stock.
     *
     * @param artigo artigo do stock
     * @return quantidade usada (0 se a reserva não tiver esse artigo ou estiver cancelada)
     */
    public int quantidadeUsada(Artigo artigo) {
        if (estado == EstadoReserva.CANCELADA) {
            return 0;
        }
        int quantidade = 0;
        for (ProdutoReserva produto : produtos) {
            if (produto.getArtigo().getId() == artigo.getId()) {
                quantidade += produto.getQuantidade();
            }
        }
        return quantidade;
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

    /** @return preço base do quarto por noite, guardado quando a reserva foi criada */
    public BigDecimal getPrecoBaseNoite() {
        return precoBaseNoite;
    }

    /** @return valor da vista por noite, guardado quando a reserva foi criada */
    public BigDecimal getValorVistaNoite() {
        return valorVistaNoite;
    }

    /** @return lista só de leitura dos serviços e extras */
    public List<ConsumoServico> getConsumos() {
        return Collections.unmodifiableList(consumos);
    }

    /** @return lista só de leitura dos produtos do stock */
    public List<ProdutoReserva> getProdutos() {
        return Collections.unmodifiableList(produtos);
    }

    private static BigDecimal validarPreco(BigDecimal preco, String descricao) {
        if (preco == null || preco.signum() < 0) {
            throw new IllegalArgumentException(descricao + " não pode ser negativo.");
        }
        return preco;
    }
}