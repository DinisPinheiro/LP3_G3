package com.hotel4you.hotel4you.Utils;

import java.util.List;

/**
 * Resultado de validar um ficheiro XML contra o XSD.
 *
 * @param erros erros encontrados, por ordem de aparição no ficheiro (vazio se o XML for válido)
 */
public record ResultadoValidacao(List<ErroXml> erros) {

    /** Guarda uma cópia da lista, para que o resultado não possa ser alterado. */
    public ResultadoValidacao {
        erros = List.copyOf(erros);
    }

    /** @return {@code true} se o ficheiro cumpre o XSD */
    public boolean valido() {
        return erros.isEmpty();
    }

    /** @return todos os erros, um por linha de texto (vazio se o XML for válido) */
    public String mensagem() {
        StringBuilder texto = new StringBuilder();
        for (ErroXml erro : erros) {
            if (texto.length() > 0) {
                texto.append(System.lineSeparator());
            }
            texto.append(erro);
        }
        return texto.toString();
    }
}
