package com.hotel4you.hotel4you.DAO;

/**
 * Falha no acesso aos dados. Esconde os detalhes de SQL (ex.: {@code SQLException})
 * para que Controller e View não dependam da base de dados.
 */
public class DAOException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param mensagem descrição do erro, pronta a mostrar ao utilizador
     */
    public DAOException(String mensagem) {
        super(mensagem);
    }

    /**
     * @param mensagem descrição do erro
     * @param causa    erro original
     */
    public DAOException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
