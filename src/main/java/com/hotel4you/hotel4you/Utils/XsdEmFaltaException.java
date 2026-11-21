package com.hotel4you.hotel4you.Utils;

/**
 * O fornecedor ainda não importou o seu XSD, por isso não há com que validar os seus XML (S6-02).
 * A aplicação não traz nenhum XSD seu: o fornecedor tem de importar o dele.
 */
public class XsdEmFaltaException extends IllegalStateException {

    /**
     * @param mensagem descrição do que falta, pronta a mostrar ao utilizador
     */
    public XsdEmFaltaException(String mensagem) {
        super(mensagem);
    }
}
