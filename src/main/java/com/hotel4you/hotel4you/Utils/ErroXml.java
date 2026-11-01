package com.hotel4you.hotel4you.Utils;

/**
 * Um erro encontrado ao validar um ficheiro XML.
 *
 * @param linha  linha do ficheiro onde está o erro (0 se não for conhecida)
 * @param coluna coluna do ficheiro onde está o erro (0 se não for conhecida)
 * @param motivo explicação do erro dada pelo validador
 */
public record ErroXml(int linha, int coluna, String motivo) {

    /** @return o erro em texto, pronto a mostrar ao utilizador, por exemplo "Linha 12, coluna 30: motivo" */
    @Override
    public String toString() {
        if (linha <= 0) {
            return motivo;
        }
        return "Linha " + linha + ", coluna " + coluna + ": " + motivo;
    }
}
