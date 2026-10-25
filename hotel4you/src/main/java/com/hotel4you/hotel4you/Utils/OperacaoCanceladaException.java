package com.hotel4you.hotel4you.Utils;

/**
 * Lançada quando o utilizador escreve "0" para cancelar o formulário em curso.
 * <p>
 * É uma exceção não verificada para que qualquer método de leitura possa
 * interromper o formulário inteiro, sem cada View ter de testar o "0" campo a campo.
 * A View que abriu o formulário apanha-a e volta ao menu.
 */
public class OperacaoCanceladaException extends RuntimeException {

    /** Cria a exceção com a mensagem padrão. */
    public OperacaoCanceladaException() {
        super("Operação cancelada pelo utilizador.");
    }
}
