package com.hotel4you.hotel4you.Model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Ficheiro XSD importado pelo gestor (S5-05). O mais recente é o que valida os XML dos fornecedores.
 * Guarda os bytes do ficheiro tal como foi escolhido, para respeitar a codificação que ele declara.
 */
public class FicheiroXsd {

    private int id;
    private final String nomeFicheiro;
    private final byte[] conteudo;
    private final LocalDateTime dataImportacao;

    /**
     * @param id             identificador (0 se ainda não foi guardado)
     * @param nomeFicheiro   nome do ficheiro escolhido
     * @param conteudo       bytes do ficheiro XSD
     * @param dataImportacao data e hora da importação
     * @throws IllegalArgumentException se faltar o nome ou o conteúdo
     */
    public FicheiroXsd(int id, String nomeFicheiro, byte[] conteudo, LocalDateTime dataImportacao) {
        if (nomeFicheiro == null || nomeFicheiro.isBlank()) {
            throw new IllegalArgumentException("O nome do ficheiro XSD é obrigatório.");
        }
        if (conteudo == null || conteudo.length == 0) {
            throw new IllegalArgumentException("O ficheiro XSD está vazio.");
        }
        this.id = id;
        this.nomeFicheiro = nomeFicheiro.trim();
        this.conteudo = conteudo.clone();
        this.dataImportacao = Objects.requireNonNull(dataImportacao);
    }

    public int getId() {
        return id;
    }

    /** Usado pelo DAO ao guardar o ficheiro pela primeira vez. */
    public void setId(int id) {
        this.id = id;
    }

    public String getNomeFicheiro() {
        return nomeFicheiro;
    }

    /** @return uma cópia dos bytes do XSD */
    public byte[] getConteudo() {
        return conteudo.clone();
    }

    public LocalDateTime getDataImportacao() {
        return dataImportacao;
    }
}
