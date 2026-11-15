package com.hotel4you.hotel4you.Model;

import com.hotel4you.hotel4you.Model.enums.TipoCobranca;
import com.hotel4you.hotel4you.Model.enums.TipoServico;

import java.math.BigDecimal;

/**
 * Serviço do hotel (alimentação, eventos culturais, piscina, spa, salas de conferência) ou extra de
 * quarto (bebidas, lavandaria), com o valor acrescentado ao cliente. O valor é cobrado por unidade
 * ou por noite ({@link TipoCobranca}).
 * <p>
 * O gestor pode alterar os dados e o valor, e desativar o item (um item desativado deixa de ser
 * oferecido, mas continua guardado porque pode já ter sido usado em reservas).
 * <p>
 */
public class Servico {

    private int id;
    private TipoServico tipo;
    private String nome;
    private String categoria;
    private String descricao;
    private BigDecimal valor;
    private TipoCobranca cobranca;
    private boolean ativo;

    /**
     * Cria um extra ativo, sem descrição e cobrado por unidade (construtor mantido por compatibilidade).
     *
     * @param id        identificador (0 se ainda não foi guardado)
     * @param nome      nome do serviço
     * @param categoria categoria (ex.: "Bebidas", "Lavandaria")
     * @param valor     valor unitário, não negativo
     */
    public Servico(int id, String nome, String categoria, BigDecimal valor) {
        this(id, TipoServico.EXTRA, nome, categoria, "", valor, TipoCobranca.POR_UNIDADE, true);
    }

    /**
     * @param id        identificador (0 se ainda não foi guardado)
     * @param tipo      serviço do hotel ou extra de quarto
     * @param nome      nome (obrigatório)
     * @param categoria categoria (ex.: "Spa", "Bebidas"); pode ser vazia
     * @param descricao descrição; pode ser vazia
     * @param valor     valor unitário, não negativo
     * @param cobranca  forma de cobrança: por unidade ou por noite
     * @param ativo     se o item está disponível
     * @throws IllegalArgumentException se faltar o tipo, o nome ou a forma de cobrança, ou se o valor
     *                                  for nulo ou negativo
     */
    public Servico(int id, TipoServico tipo, String nome, String categoria, String descricao,
                   BigDecimal valor, TipoCobranca cobranca, boolean ativo) {
        this.id = id;
        setTipo(tipo);
        setNome(nome);
        setCategoria(categoria);
        setDescricao(descricao);
        setValor(valor);
        setCobranca(cobranca);
        this.ativo = ativo;
    }

    /**
     * @return identificador (0 se ainda não foi guardado)
     */
    public int getId() {
        return id;
    }

    /**
     * Usado pelo DAO ao guardar o serviço pela primeira vez.
     *
     * @param id identificador gerado pela base de dados
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @return serviço do hotel ou extra de quarto
     */
    public TipoServico getTipo() {
        return tipo;
    }

    /**
     * @param tipo serviço do hotel ou extra de quarto
     * @throws IllegalArgumentException se o tipo for nulo
     */
    public void setTipo(TipoServico tipo) {
        if (tipo == null) {
            throw new IllegalArgumentException("O tipo (serviço ou extra) é obrigatório.");
        }
        this.tipo = tipo;
    }

    /**
     * @return nome do serviço
     */
    public String getNome() {
        return nome;
    }

    /**
     * @param nome nome (obrigatório)
     * @throws IllegalArgumentException se estiver vazio
     */
    public void setNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome do serviço é obrigatório.");
        }
        this.nome = nome.trim();
    }

    /**
     * @return categoria (nunca nula)
     */
    public String getCategoria() {
        return categoria;
    }

    /**
     * @param categoria categoria; {@code null} fica vazia
     */
    public void setCategoria(String categoria) {
        this.categoria = categoria == null ? "" : categoria.trim();
    }

    /**
     * @return descrição (nunca nula)
     */
    public String getDescricao() {
        return descricao;
    }

    /**
     * @param descricao descrição; {@code null} fica vazia
     */
    public void setDescricao(String descricao) {
        this.descricao = descricao == null ? "" : descricao.trim();
    }

    /**
     * @return valor unitário acrescentado ao cliente
     */
    public BigDecimal getValor() {
        return valor;
    }

    /**
     * Altera o valor. Só as reservas criadas a partir daqui usam o valor novo; as que já existem
     * guardam o valor que o serviço tinha quando foram criadas ({@link ConsumoServico}).
     *
     * @param valor valor unitário, não negativo
     * @throws IllegalArgumentException se for nulo ou negativo
     */
    public void setValor(BigDecimal valor) {
        if (valor == null || valor.signum() < 0) {
            throw new IllegalArgumentException("O valor do serviço não pode ser negativo.");
        }
        this.valor = valor;
    }

    /**
     * @return forma de cobrança: por unidade ou por noite
     */
    public TipoCobranca getCobranca() {
        return cobranca;
    }

    /**
     * @param cobranca forma de cobrança: por unidade ou por noite
     * @throws IllegalArgumentException se for nula
     */
    public void setCobranca(TipoCobranca cobranca) {
        if (cobranca == null) {
            throw new IllegalArgumentException("A forma de cobrança (por unidade ou por noite) é obrigatória.");
        }
        this.cobranca = cobranca;
    }

    /**
     * @return {@code true} se o item está disponível
     */
    public boolean isAtivo() {
        return ativo;
    }

    /**
     * @param ativo {@code false} para desativar o item
     */
    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    @Override
    public String toString() {
        return nome;
    }
}
