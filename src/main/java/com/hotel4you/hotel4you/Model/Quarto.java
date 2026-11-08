package com.hotel4you.hotel4you.Model;

import com.hotel4you.hotel4you.Utils.Constantes;
import com.hotel4you.hotel4you.Model.enums.TipoQuarto;
import com.hotel4you.hotel4you.Model.enums.Vista;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Quarto do hotel. Existem 20 quartos nos pisos 2 e 3 (10 por piso); o número
 * segue o padrão do enunciado (ex.: 202 e 301).
 */
public class Quarto {

    private int id;
    private final int numero;
    private final int piso;
    private String local;
    private TipoQuarto tipo;
    private BigDecimal precoBase;
    private Vista vista;
    /** Valor da vista em vigor (o configurado pelo gestor); por omissão, o valor inicial da vista. */
    private BigDecimal suplementoVista;
    private boolean ativo;
    private final List<Equipamento> equipamentos = new ArrayList<>();

    /**
     * @param id        identificador (0 se ainda não foi guardado)
     * @param numero    número do quarto (ex.: 202)
     * @param piso      piso de dormidas (2 ou 3)
     * @param local     descrição da localização (ex.: "Ala nascente"); pode ser vazia
     * @param tipo      tipo de quarto
     * @param precoBase preço base por noite, não negativo
     * @param vista     vista do quarto
     * @param ativo     se está disponível para reservas
     * @throws IllegalArgumentException se o piso não tiver quartos, o número for inválido ou o preço for negativo
     */
    public Quarto(int id, int numero, int piso, String local, TipoQuarto tipo,
                  BigDecimal precoBase, Vista vista, boolean ativo) {
        if (!Constantes.pisoDeDormidas(piso)) {
            throw new IllegalArgumentException("O piso " + piso + " não tem quartos (só os pisos 2 e 3).");
        }
        if (numero <= 0) {
            throw new IllegalArgumentException("O número do quarto deve ser positivo.");
        }
        this.id = id;
        this.numero = numero;
        this.piso = piso;
        this.local = local == null ? "" : local.trim();
        this.tipo = Objects.requireNonNull(tipo, "O tipo de quarto é obrigatório.");
        this.precoBase = validarPreco(precoBase);
        this.vista = Objects.requireNonNull(vista, "A vista é obrigatória.");
        this.suplementoVista = vista.getSuplemento();
        this.ativo = ativo;
    }

    /**
     * Preço de uma noite: preço base mais o valor da vista em vigor.
     * Consumos (bebidas, lavandaria) são somados na {@link Reserva}.
     *
     * @return preço por noite
     */
    public BigDecimal precoNoite() {
        return calcularPrecoNoite(precoBase, suplementoVista);
    }

    /**
     * Calcula o preço de uma noite: preço base mais o suplemento da vista.
     * Existe como método estático para o ecrã de configuração mostrar o preço antes de guardar o quarto.
     *
     * @param precoBase preço base por noite
     * @param vista     vista do quarto
     * @return preço por noite
     */
    public static BigDecimal calcularPrecoNoite(BigDecimal precoBase, Vista vista) {
        return precoBase.add(vista.getSuplemento());
    }

    /**
     * Calcula o preço de uma noite com um valor de vista já conhecido (o configurado pelo gestor).
     *
     * @param precoBase  preço base por noite
     * @param suplemento valor da vista a somar
     * @return preço por noite
     */
    public static BigDecimal calcularPrecoNoite(BigDecimal precoBase, BigDecimal suplemento) {
        return precoBase.add(suplemento);
    }

    /**
     * @param equipamento equipamento a colocar no quarto
     */
    public void adicionarEquipamento(Equipamento equipamento) {
        equipamentos.add(Objects.requireNonNull(equipamento, "O equipamento é obrigatório."));
    }

    /**
     * @param equipamento equipamento a retirar do quarto
     * @return {@code true} se o quarto o tinha
     */
    public boolean removerEquipamento(Equipamento equipamento) {
        return equipamentos.remove(equipamento);
    }

    /** @return lista só de leitura dos equipamentos do quarto */
    public List<Equipamento> getEquipamentos() {
        return Collections.unmodifiableList(equipamentos);
    }

    public int getId() {
        return id;
    }

    /** Usado pelo DAO ao guardar o quarto pela primeira vez. */
    public void setId(int id) {
        this.id = id;
    }

    public int getNumero() {
        return numero;
    }

    public int getPiso() {
        return piso;
    }

    public String getLocal() {
        return local;
    }

    public void setLocal(String local) {
        this.local = local == null ? "" : local.trim();
    }

    public TipoQuarto getTipo() {
        return tipo;
    }

    public void setTipo(TipoQuarto tipo) {
        this.tipo = Objects.requireNonNull(tipo, "O tipo de quarto é obrigatório.");
    }

    public BigDecimal getPrecoBase() {
        return precoBase;
    }

    public void setPrecoBase(BigDecimal precoBase) {
        this.precoBase = validarPreco(precoBase);
    }

    public Vista getVista() {
        return vista;
    }

    /** Muda a vista; o valor da vista volta ao valor inicial até o DAO ou o controller indicar o em vigor. */
    public void setVista(Vista vista) {
        this.vista = Objects.requireNonNull(vista, "A vista é obrigatória.");
        this.suplementoVista = vista.getSuplemento();
    }

    /** @return valor da vista em vigor, somado ao preço base em {@link #precoNoite()} */
    public BigDecimal getSuplementoVista() {
        return suplementoVista;
    }

    /**
     * Indica o valor da vista em vigor (lido da base de dados pelo DAO).
     *
     * @param suplementoVista valor a somar ao preço base por noite, não negativo
     * @throws IllegalArgumentException se for nulo ou negativo
     */
    public void setSuplementoVista(BigDecimal suplementoVista) {
        if (suplementoVista == null || suplementoVista.signum() < 0) {
            throw new IllegalArgumentException("O valor da vista não pode ser negativo.");
        }
        this.suplementoVista = suplementoVista;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    @Override
    public String toString() {
        return "Quarto " + numero + " (" + tipo.getNome() + ")";
    }

    private static BigDecimal validarPreco(BigDecimal preco) {
        if (preco == null || preco.signum() < 0) {
            throw new IllegalArgumentException("O preço base não pode ser negativo.");
        }
        return preco;
    }
}
