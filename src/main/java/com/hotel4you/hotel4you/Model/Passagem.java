package com.hotel4you.hotel4you.Model;

import java.time.LocalDateTime;

public class Passagem {

    private int id;
    private final int idCartao;
    private final int numeroCartao;
    private final Integer idQuarto;
    private final String local;
    private final LocalDateTime dataHora;

    public Passagem(int id, int idCartao,
                    int numeroCartao, Integer idQuarto,
                    String local, LocalDateTime dataHora) {

        this.id = id;
        this.idCartao = idCartao;
        this.numeroCartao = numeroCartao;
        this.idQuarto = idQuarto;
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

    public String getLocal() {
        return local;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }
}
