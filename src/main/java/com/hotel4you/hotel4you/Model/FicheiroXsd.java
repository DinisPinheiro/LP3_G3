package com.hotel4you.hotel4you.Model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Ficheiro XSD importado por um fornecedor (S5-05, S6-02). O mais recente de cada fornecedor é o que
 * valida os XML desse fornecedor; não existe XSD geral nem XSD que venha com a aplicação.
 * Guarda os bytes do ficheiro tal como foi escolhido, para respeitar a codificação que ele declara.
 */
public class FicheiroXsd {

    private int id;
    private final int idFornecedor;
    private final String nomeFicheiro;
    private final byte[] conteudo;
    private final LocalDateTime dataImportacao;

    /**
     * @param id             identificador (0 se ainda não foi guardado)
     * @param idFornecedor   fornecedor a que o XSD pertence
     * @param nomeFicheiro   nome do ficheiro escolhido
     * @param conteudo       bytes do ficheiro XSD
     * @param dataImportacao data e hora da importação
     * @throws IllegalArgumentException se faltar o fornecedor, o nome ou o conteúdo
     */
    public FicheiroXsd(int id, int idFornecedor, String nomeFicheiro, byte[] conteudo,
                       LocalDateTime dataImportacao) {
        if (idFornecedor <= 0) {
            throw new IllegalArgumentException("O XSD tem de pertencer a um fornecedor.");
        }
        if (nomeFicheiro == null || nomeFicheiro.isBlank()) {
            throw new IllegalArgumentException("O nome do ficheiro XSD é obrigatório.");
        }
        if (conteudo == null || conteudo.length == 0) {
            throw new IllegalArgumentException("O ficheiro XSD está vazio.");
        }
        this.id = id;
        this.idFornecedor = idFornecedor;
        this.nomeFicheiro = nomeFicheiro.trim();
        this.conteudo = conteudo.clone();
        this.dataImportacao = Objects.requireNonNull(dataImportacao);
    }

    public int getId() {
        return id;
    }

    /** @return id do fornecedor a que o XSD pertence */
    public int getIdFornecedor() {
        return idFornecedor;
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
