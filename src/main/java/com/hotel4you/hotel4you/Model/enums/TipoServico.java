package com.hotel4you.hotel4you.Model.enums;

/**
 * Grupo a que pertence um {@link com.hotel4you.hotel4you.Model.Servico}: serviço do hotel
 * (alimentação, eventos culturais, piscina, spa, salas de conferência) ou extra de quarto
 * (bebidas, lavandaria).
 * <p>
 */
public enum TipoServico {
    SERVICO("Serviço"),
    EXTRA("Extra de quarto");

    private final String nome;

    TipoServico(String nome) {
        this.nome = nome;
    }

    /**
     * @return o nome do tipo, pronto para mostrar ao utilizador
     */
    public String getNome() {
        return nome;
    }
}
