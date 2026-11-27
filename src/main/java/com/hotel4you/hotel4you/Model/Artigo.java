package com.hotel4you.hotel4you.Model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Artigo em stock (toalhas, champô, lençóis, camas, ...). O stock nunca fica negativo.
 * <p>
 * O stock do hotel ({@link #getQuantidadeAtual()}) é separado do stock disponível
 * ({@link #getQuantidadeDisponivel()}), que desconta o que já está usado em reservas (S5-06).
 * <p>
 * S6-04 e S6-05: o stock real é o total que o hotel tem; o disponível desconta o que está em uso nas
 * reservas pendentes e com check-in. As regras de baixa e reposição estão aqui:
 * <ul>
 *     <li>{@link #darBaixa(int)}: a reserva passa a usar o produto (sai do disponível, o real não muda);</li>
 *     <li>{@link #devolver(int)}: a reserva deixa de usar o produto sem o gastar (volta ao disponível);</li>
 *     <li>{@link #reporNoCheckOut(int)}: no check-out os produtos consumíveis (ex.: champô) saem
 *     definitivamente do stock real; os outros (ex.: toalhas) voltam ao disponível.</li>
 * </ul>
 * A saída de stock com justificação (avarias, perdas) é um movimento {@code SAIDA} aplicado com
 * {@link #aplicar(MovimentoStock)}.
 */
public class Artigo {

    private int id;
    private String nome;
    private String unidade;
    private int quantidadeAtual;
    /** Quantidade em uso nas reservas com check-in (calculada pelo DAO). */
    private int quantidadeReservada;
    /** Produto que se gasta com o uso (ex.: champô): no check-out não volta ao stock (S6-04). */
    private boolean consumivel;
    private Fornecedor fornecedor;
    /** Preço unitário da última entrada (sem imposto), ou {@code null} se ainda não se sabe. */
    private BigDecimal precoUnitario;
    /** Unidade a que o preço se refere (ex.: "cx"). */
    private String unidadePreco;
    /** Taxa de imposto da última entrada, em percentagem (ex.: 23), ou {@code null}. */
    private Integer taxaPercentagem;
    /** Quantas unidades principais tem uma caixa (ex.: 10), ou {@code null} se ainda não se sabe (S7-02). */
    private Integer unidadesPorCaixa;

    /** Unidade principal dos produtos contados à unidade (S7-02). */
    public static final String UNIDADE = "un";
    /** Unidade dos produtos antigos que ainda estão guardados em caixas (S7-02). */
    public static final String CAIXA = "cx";
    /** Limite da conversão, para evitar enganos de digitação. */
    public static final int MAX_UNIDADES_POR_CAIXA = 10000;

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
     * @return quantidade em uso nas reservas com check-in
     */
    public int getQuantidadeReservada() {
        return quantidadeReservada;
    }

    /**
     * Indica quanto deste artigo está em uso em reservas. Não altera o stock do hotel.
     *
     * @param quantidadeReservada quantidade em uso nas reservas com check-in, não negativa
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

    /** @return {@code true} se o produto se gasta com o uso (no check-out não volta ao stock) */
    public boolean isConsumivel() {
        return consumivel;
    }

    /** @param consumivel {@code true} se o produto se gasta com o uso (ex.: champô, sabonete) */
    public void setConsumivel(boolean consumivel) {
        this.consumivel = consumivel;
    }

    /**
     * Uma reserva passa a usar este produto: sai do stock disponível, mas o stock real não muda
     * (o produto continua a ser do hotel). Usado no check-in e quando se aumenta a quantidade na reserva.
     *
     * @param quantidade quantidade a usar, pelo menos 1
     * @throws IllegalArgumentException se a quantidade for inferior a 1
     * @throws IllegalStateException    se o stock disponível não chegar
     */
    public void darBaixa(int quantidade) {
        exigirPositiva(quantidade);
        if (quantidade > getQuantidadeDisponivel()) {
            throw new IllegalStateException("Stock disponível insuficiente de '" + nome + "': há "
                    + getQuantidadeDisponivel() + " " + unidade + " disponível(eis) e são precisos " + quantidade + ".");
        }
        quantidadeReservada += quantidade;
    }

    /**
     * Uma reserva deixa de usar este produto sem o gastar (por exemplo, a quantidade baixou na edição
     * da reserva): volta ao stock disponível. O stock real não muda.
     *
     * @param quantidade quantidade devolvida, pelo menos 1
     * @throws IllegalArgumentException se for inferior a 1 ou maior do que a quantidade em uso
     */
    public void devolver(int quantidade) {
        exigirPositiva(quantidade);
        if (quantidade > quantidadeReservada) {
            throw new IllegalArgumentException("Só há " + quantidadeReservada + " de '" + nome
                    + "' em uso; não se podem devolver " + quantidade + ".");
        }
        quantidadeReservada -= quantidade;
    }

    /**
     * Reposição no check-out: a reserva deixa de usar o produto. Um produto consumível foi gasto e sai
     * definitivamente do stock real; um não consumível volta ao stock disponível.
     *
     * @param quantidade quantidade que a reserva usava, pelo menos 1
     * @return {@code true} se o produto foi gasto (consumível), {@code false} se voltou ao stock
     * @throws IllegalArgumentException se a quantidade for inferior a 1
     */
    public boolean reporNoCheckOut(int quantidade) {
        exigirPositiva(quantidade);
        quantidadeReservada = Math.max(0, quantidadeReservada - quantidade);
        if (!consumivel) {
            return false;
        }
        quantidadeAtual = Math.max(0, quantidadeAtual - quantidade);
        return true;
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

    // ---- S7-02: unidade principal e conversão de caixas ----

    /**
     * @return quantas unidades principais tem uma caixa, ou {@code null} se a conversão não estiver definida
     */
    public Integer getUnidadesPorCaixa() {
        return unidadesPorCaixa;
    }

    /** @return {@code true} se se sabe quantas unidades tem uma caixa deste produto */
    public boolean temConversaoCaixa() {
        return unidadesPorCaixa != null;
    }

    /**
     * Define quantas unidades principais tem uma caixa (ex.: 1 caixa = 10 unidades).
     *
     * @param unidadesPorCaixa unidades numa caixa, entre 1 e {@link #MAX_UNIDADES_POR_CAIXA}
     * @throws IllegalArgumentException se estiver fora desses limites
     */
    public void definirConversaoCaixa(int unidadesPorCaixa) {
        if (unidadesPorCaixa < 1 || unidadesPorCaixa > MAX_UNIDADES_POR_CAIXA) {
            throw new IllegalArgumentException("Uma caixa de '" + nome + "' tem de ter entre 1 e "
                    + MAX_UNIDADES_POR_CAIXA + " unidades.");
        }
        this.unidadesPorCaixa = unidadesPorCaixa;
    }

    /**
     * @return {@code true} se o produto ainda está guardado em caixas (produtos importados antes da S7-02)
     */
    public boolean estaEmCaixas() {
        return CAIXA.equalsIgnoreCase(unidade);
    }

    /**
     * Converte um produto guardado em caixas para a unidade principal: o stock real e o que está em uso
     * passam a contar unidades (ex.: 50 cx com 1 cx = 10 un passam a 500 un).
     *
     * @param unidadesPorCaixa unidades numa caixa
     * @throws IllegalStateException    se o produto não estiver em caixas
     * @throws IllegalArgumentException se a conversão for inválida
     */
    public void converterCaixasParaUnidades(int unidadesPorCaixa) {
        if (!estaEmCaixas()) {
            throw new IllegalStateException("'" + nome + "' já está em " + unidade + ".");
        }
        definirConversaoCaixa(unidadesPorCaixa);
        this.quantidadeAtual = Math.multiplyExact(quantidadeAtual, unidadesPorCaixa);
        this.quantidadeReservada = Math.multiplyExact(quantidadeReservada, unidadesPorCaixa);
        this.unidade = UNIDADE;
    }

    /**
     * Converte caixas para a unidade principal com a conversão do produto.
     *
     * @param caixas número de caixas, não negativo
     * @return quantidade em unidades principais
     * @throws IllegalStateException    se a conversão não estiver definida
     * @throws IllegalArgumentException se o número de caixas for negativo
     */
    public int caixasParaUnidades(int caixas) {
        if (caixas < 0) {
            throw new IllegalArgumentException("O número de caixas não pode ser negativo.");
        }
        if (!temConversaoCaixa()) {
            throw new IllegalStateException("Defina quantas unidades tem uma caixa de '" + nome + "'.");
        }
        return Math.multiplyExact(caixas, unidadesPorCaixa);
    }

    /**
     * Os quartos, as reservas e os serviços usam a unidade principal: um produto ainda em caixas não
     * pode ser usado até a conversão ser definida.
     *
     * @throws IllegalArgumentException se o produto ainda estiver em caixas
     */
    public void exigirUnidadePrincipal() {
        if (estaEmCaixas()) {
            throw new IllegalArgumentException("'" + nome + "' ainda está em caixas. Defina quantas unidades tem "
                    + "uma caixa em \"Unidades dos produtos\" antes de o usar.");
        }
    }

    /** @return a conversão para mostrar (ex.: "1 cx = 10 un"), ou "" se não estiver definida */
    public String descreverConversao() {
        return temConversaoCaixa() ? "1 " + CAIXA + " = " + unidadesPorCaixa + " " + unidade : "";
    }

    /**
     * Descreve uma quantidade na unidade principal e, se houver conversão, em caixas
     * (ex.: "25 un (2 cx + 5 un)").
     *
     * @param quantidade quantidade na unidade principal
     * @return o texto para mostrar no stock
     */
    public String descreverQuantidade(int quantidade) {
        String texto = quantidade + " " + unidade;
        if (!temConversaoCaixa() || unidadesPorCaixa == 1 || estaEmCaixas()) {
            return texto;
        }
        int caixas = quantidade / unidadesPorCaixa;
        int resto = quantidade % unidadesPorCaixa;
        return texto + " (" + caixas + " " + CAIXA + (resto == 0 ? "" : " + " + resto + " " + unidade) + ")";
    }

    private static void exigirPositiva(int quantidade) {
        if (quantidade < 1) {
            throw new IllegalArgumentException("A quantidade deve ser pelo menos 1.");
        }
    }

    @Override
    public String toString() {
        return nome + " (" + quantidadeAtual + " " + unidade + ")";
    }
}
