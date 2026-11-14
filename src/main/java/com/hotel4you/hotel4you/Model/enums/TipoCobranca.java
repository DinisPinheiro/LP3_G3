package com.hotel4you.hotel4you.Model.enums;

/**
 * Forma de cobrar um {@link com.hotel4you.hotel4you.Model.Servico} numa reserva (S5-09):
 * por cada unidade pedida (ex.: duas limpezas) ou por cada noite da estadia
 * (ex.: pequeno-almoço em todas as noites).
 */
public enum TipoCobranca {
    POR_UNIDADE("Por unidade"),
    POR_NOITE("Por noite");

    private final String nome;

    TipoCobranca(String nome) {
        this.nome = nome;
    }

    /**
     * @return o nome da forma de cobrança, pronto para mostrar ao utilizador
     */
    public String getNome() {
        return nome;
    }
}
