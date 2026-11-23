package com.hotel4you.hotel4you.Model;

import com.hotel4you.hotel4you.Model.enums.TipoQuarto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Configuração de um tipo de quarto (individual, duplo ou família): o preço base por noite e os
 * equipamentos que todos os quartos desse tipo têm (S6-09).
 * <p>
 * Os quartos herdam esta configuração: o gestor define-a uma só vez por tipo, em vez de configurar cada
 * quarto novo. Um quarto pode ter exceções (ver {@link Quarto#personalizarPreco(BigDecimal)} e
 * {@link Quarto#personalizarEquipamentos(List, List)}).
 * <p>
 * Os equipamentos são de dois tipos, tal como nos quartos: os do catálogo ({@link Equipamento}) e os
 * produtos do stock com a quantidade ({@link ProdutoQuarto}, S6-05).
 */
public class ConfiguracaoTipoQuarto {

    private final TipoQuarto tipo;
    private final BigDecimal precoBase;
    private final List<Equipamento> equipamentos;
    private final List<ProdutoQuarto> produtos;

    /**
     * @param tipo         tipo de quarto a que a configuração se aplica
     * @param precoBase    preço base por noite, não negativo
     * @param equipamentos equipamentos do catálogo (sem repetidos)
     * @param produtos     produtos do stock com a quantidade (o mesmo produto fica com a última quantidade)
     * @throws NullPointerException     se o tipo ou o preço forem nulos
     * @throws IllegalArgumentException se o preço for negativo
     */
    public ConfiguracaoTipoQuarto(TipoQuarto tipo, BigDecimal precoBase, List<Equipamento> equipamentos,
                                  List<ProdutoQuarto> produtos) {
        this.tipo = Objects.requireNonNull(tipo, "O tipo de quarto é obrigatório.");
        Objects.requireNonNull(precoBase, "O preço base é obrigatório.");
        if (precoBase.signum() < 0) {
            throw new IllegalArgumentException("O preço base não pode ser negativo.");
        }
        this.precoBase = precoBase;
        Map<Integer, Equipamento> unicos = new LinkedHashMap<>();
        for (Equipamento equipamento : equipamentos == null ? List.<Equipamento>of() : equipamentos) {
            Objects.requireNonNull(equipamento, "O equipamento é obrigatório.");
            unicos.put(equipamento.getId(), equipamento);
        }
        this.equipamentos = new ArrayList<>(unicos.values());
        Map<Integer, ProdutoQuarto> produtosUnicos = new LinkedHashMap<>();
        for (ProdutoQuarto produto : produtos == null ? List.<ProdutoQuarto>of() : produtos) {
            produtosUnicos.put(Objects.requireNonNull(produto, "O produto é obrigatório.").getArtigo().getId(), produto);
        }
        this.produtos = new ArrayList<>(produtosUnicos.values());
    }

    /** @return tipo de quarto a que a configuração se aplica */
    public TipoQuarto getTipo() {
        return tipo;
    }

    /** @return preço base por noite dos quartos deste tipo */
    public BigDecimal getPrecoBase() {
        return precoBase;
    }

    /** @return lista só de leitura dos equipamentos do catálogo */
    public List<Equipamento> getEquipamentos() {
        return Collections.unmodifiableList(equipamentos);
    }

    /** @return lista só de leitura dos produtos do stock com a quantidade */
    public List<ProdutoQuarto> getProdutos() {
        return Collections.unmodifiableList(produtos);
    }

    @Override
    public String toString() {
        return tipo.getNome() + ": " + precoBase + " €/noite, " + equipamentos.size() + " equip., "
                + produtos.size() + " prod. stock";
    }
}
