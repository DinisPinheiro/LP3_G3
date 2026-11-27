package com.hotel4you.hotel4you.Model;

import com.hotel4you.hotel4you.Model.encomenda.LinhaEncomenda;
import com.hotel4you.hotel4you.Model.enums.UnidadeMedida;

import java.util.OptionalInt;

/**
 * Regras de conversão de caixas para a unidade principal, usadas na entrada em stock das encomendas
 * dos fornecedores (S7-02).
 * <p>
 * O stock guarda sempre a unidade principal: unidades ("un") para o que vem em caixas ou unidades, ou
 * a unidade da linha (ex.: "kg"). Uma linha em caixas é convertida com a conversão do próprio ficheiro
 * (as caixas e as unidades da mesma linha, ex.: 50 caixas e 500 unidades = 10 unidades por caixa);
 * quando o ficheiro não a traz, usa-se a que o gestor ou o funcionário definiu.
 * <p>
 * Exemplo: 1 caixa = 10 unidades; uma linha de 3 caixas dá entrada de 30 unidades.
 */
public final class ConversaoUnidades {

    private ConversaoUnidades() {
    }

    /**
     * Conversão indicada no ficheiro: a mesma linha traz a quantidade em caixas e em unidades
     * (uma delas pode ser informativa).
     *
     * @param linha linha da encomenda
     * @return unidades por caixa, ou vazio se o ficheiro não traz as duas quantidades
     * @throws IllegalArgumentException se as unidades não derem um número certo por caixa
     */
    public static OptionalInt conversaoDoFicheiro(LinhaEncomenda linha) {
        OptionalInt caixas = linha.caixas();
        OptionalInt unidades = linha.unidades();
        if (caixas.isEmpty() || unidades.isEmpty() || caixas.getAsInt() < 1 || unidades.getAsInt() < 1) {
            return OptionalInt.empty();
        }
        if (unidades.getAsInt() % caixas.getAsInt() != 0) {
            throw new IllegalArgumentException("Linha " + linha.numero() + ": " + unidades.getAsInt()
                    + " unidades não dão um número certo de unidades por caixa (" + caixas.getAsInt() + " caixas).");
        }
        return OptionalInt.of(unidades.getAsInt() / caixas.getAsInt());
    }

    /**
     * Escolhe a conversão a usar: a do ficheiro; se não vier, a que o utilizador acabou de definir; se
     * não houver, a que o produto já tem guardada.
     *
     * @param linha    linha da encomenda
     * @param definida conversão definida agora pelo gestor ou funcionário, ou {@code null}
     * @param guardada conversão já guardada no produto, ou {@code null}
     * @return unidades por caixa, ou {@code null} se nenhuma for conhecida
     */
    public static Integer escolherConversao(LinhaEncomenda linha, Integer definida, Integer guardada) {
        OptionalInt doFicheiro = conversaoDoFicheiro(linha);
        if (doFicheiro.isPresent()) {
            return doFicheiro.getAsInt();
        }
        return definida != null ? definida : guardada;
    }

    /**
     * @param linha linha da encomenda
     * @return a unidade principal em que a linha fica em stock: "un" para caixas e unidades, a abreviatura
     *         da unidade nos outros casos (ex.: "kg")
     */
    public static String unidadePrincipal(LinhaEncomenda linha) {
        UnidadeMedida unidade = linha.quantidade().unidade();
        return unidade == UnidadeMedida.CAIXA || unidade == UnidadeMedida.UNIDADE
                ? Artigo.UNIDADE : unidade.getAbreviatura();
    }

    /**
     * Quantidade da linha na unidade principal. Em caixas multiplica pela conversão.
     *
     * @param linha            linha da encomenda
     * @param unidadesPorCaixa conversão a usar (ver {@link #escolherConversao}), ou {@code null}
     * @return quantidade em unidades principais
     * @throws ConversaoEmFaltaException se a linha vier em caixas e não houver conversão
     * @throws IllegalArgumentException  se a conversão for inferior a 1
     */
    public static int quantidadeNaUnidadePrincipal(LinhaEncomenda linha, Integer unidadesPorCaixa) {
        int quantidade = linha.quantidade().valor();
        if (linha.quantidade().unidade() != UnidadeMedida.CAIXA) {
            return quantidade;
        }
        if (unidadesPorCaixa == null) {
            throw new ConversaoEmFaltaException(linha.numero(), linha.descricao().trim());
        }
        if (unidadesPorCaixa < 1) {
            throw new IllegalArgumentException("Linha " + linha.numero() + ": uma caixa tem de ter pelo menos 1 unidade.");
        }
        return Math.multiplyExact(quantidade, unidadesPorCaixa);
    }
}
