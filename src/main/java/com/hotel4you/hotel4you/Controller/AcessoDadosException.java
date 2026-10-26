package com.hotel4you.hotel4you.Controller;

/**
 * Erro ao aceder aos dados, visto pela View. Os Controllers convertem as falhas dos DAO
 * nesta exceção para que a View não dependa da camada de dados.
 */
public class AcessoDadosException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param mensagem descrição do erro, pronta a mostrar ao utilizador
     * @param causa    erro original do DAO
     */
    public AcessoDadosException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
