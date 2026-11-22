package com.hotel4you.hotel4you.Model;

import java.time.LocalDateTime;

/**
 * Uma picagem de cartão: o cartão, o local (código de {@link LocalHotel}) e quando aconteceu.
 * Se o local for um quarto, guarda também esse quarto.
 */
public class Passagem {

    private int id;
    private final int idCartao;
    private final int numeroCartao;
    private final Integer idQuarto;
    private final Integer numeroQuarto;
    private final String local;
    private final LocalDateTime dataHora;

    /**
     * Passagem sem o número do quarto (usada ao criar; o número é lido da base de dados nas consultas).
     *
     * @param id           identificador (0 se ainda não foi guardada)
     * @param idCartao     identificador do cartão
     * @param numeroCartao número do cartão
     * @param idQuarto     identificador do quarto, ou {@code null} se o local não for um quarto
     * @param local        código do local (ex.: {@code RESTAURANTE})
     * @param dataHora     data e hora da picagem
     */
    public Passagem(int id, int idCartao,
                    int numeroCartao, Integer idQuarto,
                    String local, LocalDateTime dataHora) {

        this(id, idCartao, numeroCartao, idQuarto, null, local, dataHora);
    }

    /**
     * @param id           identificador (0 se ainda não foi guardada)
     * @param idCartao     identificador do cartão
     * @param numeroCartao número do cartão
     * @param idQuarto     identificador do quarto, ou {@code null} se o local não for um quarto
     * @param numeroQuarto número do quarto (ex.: 202), ou {@code null} se o local não for um quarto
     * @param local        código do local (ex.: {@code RESTAURANTE})
     * @param dataHora     data e hora da picagem
     */
    public Passagem(int id, int idCartao,
                    int numeroCartao, Integer idQuarto, Integer numeroQuarto,
                    String local, LocalDateTime dataHora) {

        this.id = id;
        this.idCartao = idCartao;
        this.numeroCartao = numeroCartao;
        this.idQuarto = idQuarto;
        this.numeroQuarto = numeroQuarto;
        this.local = local;
        this.dataHora = dataHora;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIdCartao() {
        return idCartao;
    }

    public int getNumeroCartao() {
        return numeroCartao;
    }

    public Integer getIdQuarto() {
        return idQuarto;
    }

    /** @return número do quarto (ex.: 202), ou {@code null} se o local não foi um quarto */
    public Integer getNumeroQuarto() {
        return numeroQuarto;
    }

    public String getLocal() {
        return local;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }
}