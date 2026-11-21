package com.hotel4you.hotel4you.Model;

import com.hotel4you.hotel4you.Model.enums.EstadoReserva;
import com.hotel4you.hotel4you.Model.enums.TipoMovimento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * <p>
 * Stock (S6-05): os produtos de uma reserva pendente ou com check-in estão em uso e saem do stock
 * disponível. No check-in os equipamentos do quarto ({@link Quarto#getProdutos()}) passam para a
 * reserva; no check-out os consumíveis saem do stock real e os restantes voltam ao disponível. As
 * contas de cada produto são feitas pelo {@link Artigo}.
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
        validarCheckIn(cartao);
        concluirCheckIn(cartao, quando);
    }

    /**
     * Check-in com baixa no stock (S6-05): além de entregar o cartão, os equipamentos do quarto
     * ({@link Quarto#getProdutos()}) passam para a reserva e saem do stock disponível. Se o stock
     * disponível de algum produto não chegar, nada muda (nem o estado, nem os produtos, nem o stock).
     *
     * @param cartao cartão entregue
     * @param quando momento do check-in
     * @param stock  produtos do stock com o stock real e o que está em uso (lidos agora do DAO);
     *               os que a reserva usa ficam com a baixa feita
     * @throws IllegalStateException    se a reserva não estiver pendente ou o stock disponível não chegar
     * @throws IllegalArgumentException se o cartão for o mestre ou estiver desativado, ou se um
     *                                  produto do quarto já não existir em stock
     */
    public void fazerCheckIn(CartaoMagnetico cartao, LocalDateTime quando, List<Artigo> stock) {
        validarCheckIn(cartao);
        Objects.requireNonNull(stock, "O stock é obrigatório.");
        List<ProdutoReserva> doQuarto = new ArrayList<>();
        for (ProdutoQuarto produto : quarto.getProdutos()) {
            Artigo artigo = procurarNoStock(stock, produto.getArtigo());
            doQuarto.add(new ProdutoReserva(artigo, produto.getQuantidade()));
        }
        darBaixa(doQuarto);
        for (ProdutoReserva produto : doQuarto) {
            juntarProduto(produto.getArtigo(), produto.getQuantidade());
        }
        concluirCheckIn(cartao, quando);
    }

    private void validarCheckIn(CartaoMagnetico cartao) {
        if (estado != EstadoReserva.PENDENTE) {
            throw new IllegalStateException("Só é possível fazer check-in de reservas pendentes " + "(estado: " + estado + ").");
        }
        Objects.requireNonNull(cartao, "O cartão é obrigatório.");
        if (cartao.isMestre()) {
            throw new IllegalArgumentException("O cartão mestre não pode ser atribuído a um cliente.");
        }
        if (!cartao.isAtivo()) {
            throw new IllegalArgumentException("O cartão " + cartao.getNumero() + " está desativado.");
        }
    }

    private void concluirCheckIn(CartaoMagnetico cartao, LocalDateTime quando) {
        this.cartao = cartao;
        this.dataCheckIn = Objects.requireNonNull(quando);
        this.estado = EstadoReserva.CHECK_IN;
    }

    /**
     * Dá baixa de todos os produtos ou de nenhum: primeiro confirma que o disponível chega para cada
     * artigo (somando linhas repetidas) e só depois altera o stock.
     */
    private static void darBaixa(List<ProdutoReserva> pedidos) {
        Map<Artigo, Integer> porArtigo = new LinkedHashMap<>();
        for (ProdutoReserva pedido : pedidos) {
            porArtigo.merge(pedido.getArtigo(), pedido.getQuantidade(), Integer::sum);
        }
        for (Map.Entry<Artigo, Integer> pedido : porArtigo.entrySet()) {
            Artigo artigo = pedido.getKey();
            if (pedido.getValue() > artigo.getQuantidadeDisponivel()) {
                artigo.darBaixa(pedido.getValue());   // lança a exceção com a mensagem do Artigo
            }
        }
        porArtigo.forEach(Artigo::darBaixa);
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
            throw new IllegalStateException("Só é possível fazer check-out após o check-in " + "(estado: " + estado + ").");
        }
        this.dataCheckOut = Objects.requireNonNull(quando);
        this.estado = EstadoReserva.CHECK_OUT;
        if (cartao != null) {
            cartao.desativar();
        }
    }

    /**
     * Check-out com reposição do stock (S6-05): além do check-out normal, cada produto da reserva deixa
     * de estar em uso. Os consumíveis (ex.: champô) saem definitivamente do stock real e ficam num
     * movimento de utilização; os outros (ex.: toalhas) voltam ao stock disponível.
     *
     * @param quando momento do check-out
     * @param stock  produtos do stock (lidos agora do DAO); ficam com a reposição feita
     * @param quem   utilizador que faz o check-out (fica nos movimentos)
     * @return os movimentos de utilização dos consumíveis, a guardar no stock (vazio se não houver)
     * @throws IllegalStateException se o check-in ainda não foi feito
     */
    public List<MovimentoStock> fazerCheckOut(LocalDateTime quando, List<Artigo> stock, Utilizador quem) {
        Objects.requireNonNull(stock, "O stock é obrigatório.");
        Objects.requireNonNull(quem, "O utilizador é obrigatório.");
        fazerCheckOut(quando);
        List<MovimentoStock> consumos = new ArrayList<>();
        for (ProdutoReserva produto : produtos) {
            Artigo artigo = procurarNoStockOuNulo(stock, produto.getArtigo().getId());
            if (artigo != null && artigo.reporNoCheckOut(produto.getQuantidade())) {
                consumos.add(new MovimentoStock(TipoMovimento.UTILIZACAO, produto.getQuantidade(), quando, quem,
                        artigo, "Consumo na reserva n.º " + id + " (quarto " + quarto.getNumero() + ")"));
            }
        }
        return consumos;
    }

    /**
     * Cancela a reserva.
     *
     * @throws IllegalStateException se o check-in já foi efetuado ou a reserva já está cancelada
     */
    public void cancelar() {
        if (estado != EstadoReserva.PENDENTE) {
            throw new IllegalStateException("Só é possível cancelar reservas pendentes " + "(estado: " + estado + ").");
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
            throw new IllegalStateException("Só se registam consumos durante a estadia " + "(estado: " + estado + ").");
        }
        consumos.add(Objects.requireNonNull(consumo, "O consumo é obrigatório."));
    }

    // EDIÇÃO DE SERVIÇOS DA RESERVA
    /**
     * Verifica se os serviços da reserva podem ser editados.
     *
     * Apenas reservas com check-in efetuado podem ser editadas.
     * Reservas pendentes, canceladas ou com check-out
     * não podem ser editadas nesta funcionalidade.
     *
     * @return true se a reserva estiver em CHECK_IN
     */
    public boolean podeEditarServicos() {

        if (estado == EstadoReserva.CHECK_IN) {
            return true;
        }

        return false;
    }

    /**
     * Acrescenta um serviço a uma reserva ativa.
     *
     * @param consumo serviço que será acrescentado
     */
    public void adicionarServicoEdicao(ConsumoServico consumo) {

        // Verifica se a reserva pode ser editada.
        if (!podeEditarServicos()) {
            throw new IllegalStateException("Não é possível editar os serviços desta reserva.");
        }

        // Verifica se foi escolhido um serviço.
        if (consumo == null) {
            throw new IllegalArgumentException("Tem de selecionar um serviço.");
        }

        // Adiciona o serviço à lista.
        consumos.add(consumo);
    }

    /**
     * Remove um serviço de uma reserva ativa.
     *
     * @param indice posição do serviço na lista
     */
    public void removerServicoEdicao(int indice) {
        // Verifica se a reserva pode ser editada.
        if (!podeEditarServicos()) {
            throw new IllegalStateException("Não é possível editar os serviços desta reserva.");
        }

        // Verifica se a posição é válida.
        if (indice < 0 || indice >= consumos.size()) {
            throw new IllegalArgumentException("O serviço selecionado não existe.");
        }
        consumos.remove(indice);
    }

    /**
     * Devolve o número de noites da reserva.
     */
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
     * Quantidade de um artigo do stock em uso por esta reserva. Só as reservas pendentes e com check-in
     * usam stock: uma reserva cancelada nunca usou e uma com check-out já devolveu (S6-05).
     *
     * @param artigo artigo do stock
     * @return quantidade em uso (0 se a reserva não tiver esse artigo ou não estiver ativa)
     */
    public int quantidadeUsada(Artigo artigo) {
        if (!usaStock()) {
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

    /**
     * @return {@code true} se os produtos desta reserva estão em uso (reserva pendente ou com check-in)
     */
    public boolean usaStock() {
        return estado == EstadoReserva.PENDENTE || estado == EstadoReserva.CHECK_IN;
    }

    /**
     * Na edição da reserva os produtos podem mudar enquanto a reserva estiver pendente ou com check-in.
     *
     * @return {@code true} se os produtos podem ser alterados
     */
    public boolean podeEditarProdutos() {
        return usaStock();
    }

    /**
     * Muda a quantidade de um produto do stock na reserva (por exemplo, só uma pessoa num quarto duplo
     * precisa de 1 toalha em vez de 2) e ajusta o stock (S6-05): se aumentar, a diferença sai do stock
     * disponível; se diminuir, volta ao disponível sem ser gasta. Com quantidade 0 o produto sai da reserva.
     *
     * @param artigoDoStock    o produto, lido agora do stock (com o real e o que está em uso)
     * @param novaQuantidade   nova quantidade na reserva (0 retira o produto)
     * @throws IllegalStateException    se a reserva não estiver pendente nem com check-in, ou se o
     *                                  stock disponível não chegar para o aumento
     * @throws IllegalArgumentException se a quantidade for negativa
     */
    public void alterarProduto(Artigo artigoDoStock, int novaQuantidade) {
        Objects.requireNonNull(artigoDoStock, "Escolha um produto.");
        if (!podeEditarProdutos()) {
            throw new IllegalStateException("Os produtos só se alteram em reservas pendentes ou com check-in "
                    + "(estado: " + estado + ").");
        }
        if (novaQuantidade < 0) {
            throw new IllegalArgumentException("A quantidade não pode ser negativa.");
        }
        int atual = quantidadeUsada(artigoDoStock);
        if (novaQuantidade > atual) {
            artigoDoStock.darBaixa(novaQuantidade - atual);
        } else if (novaQuantidade < atual) {
            artigoDoStock.devolver(atual - novaQuantidade);
        }
        produtos.removeIf(produto -> produto.getArtigo().getId() == artigoDoStock.getId());
        if (novaQuantidade > 0) {
            produtos.add(new ProdutoReserva(artigoDoStock, novaQuantidade));
        }
    }

    /** Junta a quantidade à linha do mesmo produto (ou cria uma linha nova). */
    private void juntarProduto(Artigo artigo, int quantidade) {
        int atual = 0;
        for (ProdutoReserva produto : produtos) {
            if (produto.getArtigo().getId() == artigo.getId()) {
                atual += produto.getQuantidade();
            }
        }
        produtos.removeIf(produto -> produto.getArtigo().getId() == artigo.getId());
        produtos.add(new ProdutoReserva(artigo, atual + quantidade));
    }

    private static Artigo procurarNoStock(List<Artigo> stock, Artigo procurado) {
        Artigo artigo = procurarNoStockOuNulo(stock, procurado.getId());
        if (artigo == null) {
            throw new IllegalArgumentException("O produto '" + procurado.getNome() + "' já não existe em stock.");
        }
        return artigo;
    }

    private static Artigo procurarNoStockOuNulo(List<Artigo> stock, int idArtigo) {
        for (Artigo artigo : stock) {
            if (artigo.getId() == idArtigo) {
                return artigo;
            }
        }
        return null;
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

    public CartaoMagnetico getCartao() {
        return cartao;
    }

    public LocalDateTime getDataCheckIn() {
        return dataCheckIn;
    }

    public LocalDateTime getDataCheckOut() {
        return dataCheckOut;
    }

    public BigDecimal getPrecoBaseNoite() {
        return precoBaseNoite;
    }

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