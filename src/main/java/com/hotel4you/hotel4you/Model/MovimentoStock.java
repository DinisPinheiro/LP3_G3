package com.hotel4you.hotel4you.Model;

import com.hotel4you.hotel4you.Model.enums.TipoMovimento;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Registo de uma entrada, utilização ou saída de stock, feito por um funcionário ou gestor.
 * <p>
 * S6-04: a saída de stock ({@link TipoMovimento#SAIDA}) leva sempre uma justificação (por exemplo,
 * "televisão avariada"); a utilização ({@link TipoMovimento#UTILIZACAO}) regista o consumo de produtos
 * consumíveis no check-out de uma reserva.
 */
public class MovimentoStock {

    private final TipoMovimento tipo;
    private final int quantidade;
    private final LocalDateTime dataHora;
    private final Utilizador utilizador;
    private final Artigo artigo;
    private final String justificacao;

    /** Tamanho máximo da justificação (coluna dbo.MovimentoStock.Justificacao). */
    public static final int MAX_JUSTIFICACAO = 200;

    /**
     * @param tipo       tipo de movimento
     * @param quantidade quantidade movimentada, pelo menos 1
     * @param dataHora   momento do registo
     * @param utilizador quem registou
     * @param artigo     artigo movimentado
     */
    public MovimentoStock(TipoMovimento tipo, int quantidade, LocalDateTime dataHora,
                          Utilizador utilizador, Artigo artigo) {
        this(tipo, quantidade, dataHora, utilizador, artigo, "");
    }

    /**
     * @param tipo         tipo de movimento
     * @param quantidade   quantidade movimentada, pelo menos 1
     * @param dataHora     momento do registo
     * @param utilizador   quem registou
     * @param artigo       artigo movimentado
     * @param justificacao motivo do movimento; obrigatório nas saídas, até {@value #MAX_JUSTIFICACAO} caracteres
     * @throws IllegalArgumentException se a quantidade for inferior a 1, se for uma saída sem justificação
     *                                  ou se a justificação for longa demais
     */
    public MovimentoStock(TipoMovimento tipo, int quantidade, LocalDateTime dataHora,
                          Utilizador utilizador, Artigo artigo, String justificacao) {
        String motivo = justificacao == null ? "" : justificacao.trim();
        if (tipo == TipoMovimento.SAIDA && motivo.isEmpty()) {
            throw new IllegalArgumentException("Indique a justificação da saída de stock (ex.: televisão avariada).");
        }
        if (motivo.length() > MAX_JUSTIFICACAO) {
            throw new IllegalArgumentException("A justificação não pode ter mais de " + MAX_JUSTIFICACAO + " caracteres.");
        }
        this.justificacao = motivo;
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

    /** @return o motivo do movimento (vazio nas entradas sem motivo) */
    public String getJustificacao() {
        return justificacao;
    }
}
