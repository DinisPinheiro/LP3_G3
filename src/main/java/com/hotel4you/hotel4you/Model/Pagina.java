package com.hotel4you.hotel4you.Model;

import java.util.List;

/**
 * Uma página de resultados de uma consulta paginada (por exemplo, a lista de clientes).
 *
 * @param itens   elementos desta página
 * @param numero  número da página, a começar em 0
 * @param tamanho máximo de elementos por página (pelo menos 1)
 * @param total   número total de elementos em todas as páginas
 * @param <T>     tipo dos elementos
 */
public record Pagina<T>(List<T> itens, int numero, int tamanho, int total) {

    /**
     * @throws IllegalArgumentException se o tamanho for inferior a 1, o número for negativo ou o total for negativo
     */
    public Pagina {
        if (tamanho < 1 || numero < 0 || total < 0) {
            throw new IllegalArgumentException("Página inválida.");
        }
        itens = List.copyOf(itens);
    }

    /** @return número de páginas (pelo menos 1, mesmo sem resultados) */
    public int totalPaginas() {
        return Math.max(1, (total + tamanho - 1) / tamanho);
    }

    /** @return {@code true} se existe uma página antes desta */
    public boolean temAnterior() {
        return numero > 0;
    }

    /** @return {@code true} se existe uma página depois desta */
    public boolean temSeguinte() {
        return numero < totalPaginas() - 1;
    }
}
