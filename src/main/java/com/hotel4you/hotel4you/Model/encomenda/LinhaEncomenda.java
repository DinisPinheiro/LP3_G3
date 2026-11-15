package com.hotel4you.hotel4you.Model.encomenda;

import com.hotel4you.hotel4you.Model.enums.UnidadeMedida;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.OptionalInt;

/**
 * Uma linha (produto) da encomenda do fornecedor (elemento {@code Line} do XML).
 * <p>
 * Além dos dados lidos do ficheiro, a linha sabe calcular o valor sem imposto, o imposto e o total
 * ({@link #subtotalCalculado()}, {@link #taxaCalculada()} e {@link #totalCalculado()}) e quantas
 * caixas e unidades traz ({@link #caixas()} e {@link #unidades()}). Os valores são sempre
 * {@link BigDecimal} com 2 casas decimais, arredondados meio para cima.
 *
 * @param numero                  número da linha
 * @param codigoProduto           código do produto dado pelo fornecedor
 * @param agencia                 quem atribuiu o código (atributo {@code Agency}, por exemplo {@code "Supplier"})
 * @param descricao               descrição do produto
 * @param quantidade              quantidade encomendada
 * @param quantidadesInformativas quantidades só informativas (peso, número de caixas, ...)
 * @param precoUnitario           preço por unidade
 * @param imposto                 imposto da linha, como vem no XML
 * @param valorBase               valor total da linha, como vem no XML (inclui o imposto)
 */
public record LinhaEncomenda(int numero, String codigoProduto, String agencia, String descricao,
                             Quantidade quantidade, List<Quantidade> quantidadesInformativas,
                             PrecoUnitario precoUnitario, ImpostoLinha imposto, Montante valorBase) {

    private static final BigDecimal CEM = BigDecimal.valueOf(100);

    /** Garante que a lista não pode ser alterada por quem recebe a linha. */
    public LinhaEncomenda {
        quantidadesInformativas = List.copyOf(quantidadesInformativas);
    }

    /**
     * Procura a quantidade da linha numa unidade: primeiro a quantidade encomendada e,
     * se for noutra unidade, as quantidades informativas.
     *
     * @param unidade unidade pretendida
     * @return a quantidade, ou vazio se a linha não a indica
     */
    public OptionalInt quantidadeEm(UnidadeMedida unidade) {
        if (quantidade.unidade() == unidade) {
            return OptionalInt.of(quantidade.valor());
        }
        for (Quantidade informativa : quantidadesInformativas) {
            if (informativa.unidade() == unidade) {
                return OptionalInt.of(informativa.valor());
            }
        }
        return OptionalInt.empty();
    }

    /** @return o número de caixas da linha, ou vazio se o XML não o indica */
    public OptionalInt caixas() {
        return quantidadeEm(UnidadeMedida.CAIXA);
    }

    /** @return o número de unidades da linha, ou vazio se o XML não o indica */
    public OptionalInt unidades() {
        return quantidadeEm(UnidadeMedida.UNIDADE);
    }

    /**
     * @return {@code true} se a linha indica a quantidade na unidade em que o preço é dado, o que
     *         permite calcular o valor sem imposto (preço x quantidade)
     */
    public boolean subtotalCalculavel() {
        return quantidadeEm(precoUnitario.unidade()).isPresent() && precoUnitario.porQuantidade() >= 1;
    }

    /**
     * Valor da linha sem imposto: preço unitário x quantidade na unidade do preço.
     * Se a linha não indicar a quantidade nessa unidade (ver {@link #subtotalCalculavel()}), usa o
     * valor sem imposto que vem no XML.
     *
     * @return valor sem imposto, com 2 casas decimais
     */
    public BigDecimal subtotalCalculado() {
        if (!subtotalCalculavel()) {
            return arredondar(imposto.baseCalculo().valor());
        }
        int quantidadeNaUnidade = quantidadeEm(precoUnitario.unidade()).getAsInt();
        return precoUnitario.preco().valor()
                .multiply(BigDecimal.valueOf(quantidadeNaUnidade))
                .divide(BigDecimal.valueOf(precoUnitario.porQuantidade()), 2, RoundingMode.HALF_UP);
    }

    /** @return imposto da linha: valor sem imposto x taxa / 100, com 2 casas decimais */
    public BigDecimal taxaCalculada() {
        return subtotalCalculado().multiply(BigDecimal.valueOf(imposto.percentagem()))
                .divide(CEM, 2, RoundingMode.HALF_UP);
    }

    /** @return total da linha: valor sem imposto mais imposto */
    public BigDecimal totalCalculado() {
        return subtotalCalculado().add(taxaCalculada());
    }

    private static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
