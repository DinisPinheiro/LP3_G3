package com.hotel4you.hotel4you.Controller;

/**
 * Lançada quando as regras de acesso recusam uma picagem de cartão (por exemplo, a restauração a
 * tentar entrar na piscina). A mensagem diz o motivo e pode ser mostrada ao utilizador.
 */
public class AcessoRecusadoException extends RuntimeException {

    /**
     * @param mensagem motivo da recusa, pronto para mostrar ao utilizador
     */
    public AcessoRecusadoException(String mensagem) {
        super(mensagem);
    }
}