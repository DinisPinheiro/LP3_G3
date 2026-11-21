package com.hotel4you.hotel4you.Model.encomenda;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Encomenda enviada por um fornecedor em ficheiro XML (elemento {@code Order}).
 * <p>
 * Guarda os dados lidos do ficheiro e sabe calcular os seus totais ({@link #totais()}) e comparar
 * os valores do XML com os calculados ({@link #avisos()}).
 *
 * @param numero     número da encomenda, por exemplo {@code "OC56974"}
 * @param data       data da encomenda
 * @param fornecedor fornecedor que enviou a encomenda
 * @param linhas     produtos encomendados (pelo menos um)
 * @param resumoXml  totais indicados no XML (elemento opcional {@code OrderSummary}); {@code null} se não vierem
 */
public record EncomendaFornecedor(String numero, LocalDate data, FornecedorEncomenda fornecedor,
                                  List<LinhaEncomenda> linhas, TotaisEncomenda resumoXml) {

    /** Limites das colunas de dbo.Encomenda (Numero, Moeda) e dbo.EncomendaLinha (CodigoProduto). */
    private static final int MAX_NUMERO_ENCOMENDA = 30;
    private static final int MAX_MOEDA = 3;
    private static final int MAX_CODIGO_PRODUTO = 40;

    /** Garante que a lista não pode ser alterada por quem recebe a encomenda. */
    public EncomendaFornecedor {
        linhas = List.copyOf(linhas);
    }

    /** Encomenda sem os totais do XML. */
    public EncomendaFornecedor(String numero, LocalDate data, FornecedorEncomenda fornecedor,
                               List<LinhaEncomenda> linhas) {
        this(numero, data, fornecedor, linhas, null);
    }

    /**
     * Verifica os valores que o XSD aceita mas que a base de dados não: preços negativos, taxas acima
     * de 100, textos longos demais, e se os totais se conseguem calcular. Não guarda nada.
     *
     * @throws IllegalArgumentException se algum valor for inválido
     */
    public void validarValores() {
        limitar(numero, MAX_NUMERO_ENCOMENDA, "O número da encomenda");
        for (LinhaEncomenda linha : linhas) {
            String prefixo = "Linha " + linha.numero() + ": ";
            limitar(linha.codigoProduto(), MAX_CODIGO_PRODUTO, prefixo + "o código do produto");
            limitar(linha.precoUnitario().preco().moeda(), MAX_MOEDA, prefixo + "a moeda");
            if (linha.precoUnitario().preco().valor().signum() < 0) {
                throw new IllegalArgumentException(prefixo + "o preço unitário não pode ser negativo.");
            }
            if (linha.precoUnitario().porQuantidade() < 1) {
                throw new IllegalArgumentException(prefixo + "o preço tem de se referir a pelo menos 1 unidade.");
            }
            if (linha.imposto().percentagem() > 100) {
                throw new IllegalArgumentException(prefixo + "a taxa de imposto não pode ser superior a 100%.");
            }
        }
        totais();
    }

    /**
     * Calcula os totais da encomenda a partir das linhas.
     *
     * @return total das linhas (sem impostos), total das taxas e total com taxas
     * @throws IllegalArgumentException se as linhas usarem moedas diferentes
     */
    public TotaisEncomenda totais() {
        String moeda = linhas.get(0).precoUnitario().preco().moeda();
        BigDecimal totalLinhas = BigDecimal.ZERO;
        BigDecimal totalTaxas = BigDecimal.ZERO;
        for (LinhaEncomenda linha : linhas) {
            String moedaLinha = linha.precoUnitario().preco().moeda();
            if (!moeda.equals(moedaLinha)) {
                throw new IllegalArgumentException("Linha " + linha.numero() + ": a moeda '" + moedaLinha
                        + "' é diferente da moeda da encomenda ('" + moeda + "').");
            }
            totalLinhas = totalLinhas.add(linha.subtotalCalculado());
            totalTaxas = totalTaxas.add(linha.taxaCalculada());
        }
        return new TotaisEncomenda(totalLinhas, totalTaxas, totalLinhas.add(totalTaxas), moeda);
    }

    /**
     * Compara os valores que vêm no XML com os calculados pelo programa. Uma diferença não impede
     * a entrada em stock (o programa guarda sempre os valores calculados), mas deve ser mostrada.
     *
     * @return uma frase por cada diferença encontrada (vazia se o XML estiver certo)
     */
    public List<String> avisos() {
        List<String> avisos = new ArrayList<>();
        for (LinhaEncomenda linha : linhas) {
            String prefixo = "Linha " + linha.numero() + ": ";
            if (!linha.subtotalCalculavel()) {
                avisos.add(prefixo + "o preço é por " + linha.precoUnitario().unidade().getAbreviatura()
                        + " mas a quantidade não vem nessa unidade; usa-se o valor sem imposto do XML.");
            } else {
                comparar(avisos, prefixo + "valor sem imposto", linha.imposto().baseCalculo().valor(),
                        linha.subtotalCalculado());
            }
            comparar(avisos, prefixo + "imposto", linha.imposto().valorImposto().valor(), linha.taxaCalculada());
            comparar(avisos, prefixo + "total da linha", linha.valorBase().valor(), linha.totalCalculado());
        }
        if (resumoXml != null) {
            TotaisEncomenda calculados = totais();
            comparar(avisos, "Encomenda: total das linhas", resumoXml.totalLinhas(), calculados.totalLinhas());
            comparar(avisos, "Encomenda: total das taxas", resumoXml.totalTaxas(), calculados.totalTaxas());
            comparar(avisos, "Encomenda: total com taxas", resumoXml.totalComTaxas(), calculados.totalComTaxas());
        }
        return avisos;
    }

    private static void limitar(String texto, int maximo, String descricao) {
        if (texto != null && texto.trim().length() > maximo) {
            throw new IllegalArgumentException(descricao + " não pode ter mais de " + maximo + " caracteres.");
        }
    }

    private static void comparar(List<String> avisos, String descricao, BigDecimal noXml, BigDecimal calculado) {
        if (noXml.compareTo(calculado) != 0) {
            avisos.add(descricao + " no XML (" + texto(noXml) + ") difere do calculado (" + texto(calculado) + ").");
        }
    }

    private static String texto(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
