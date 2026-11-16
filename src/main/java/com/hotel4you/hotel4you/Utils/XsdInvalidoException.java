package com.hotel4you.hotel4you.Utils;

/**
 * O ficheiro XSD escolhido não é um esquema XML válido e por isso não pode ser usado para validar
 * ficheiros (S5-05). A mensagem diz o motivo, com a linha quando o validador a indica.
 */
public class XsdInvalidoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param mensagem motivo pelo qual o XSD foi recusado, pronto a mostrar ao utilizador
     * @param causa    erro original
     */
    public XsdInvalidoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
