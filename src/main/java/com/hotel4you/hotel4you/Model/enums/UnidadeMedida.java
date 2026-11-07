package com.hotel4you.hotel4you.Model.enums;

/**
 * Unidade de medida de uma quantidade na encomenda do fornecedor (atributo {@code UOM} do XML).
 */
public enum UnidadeMedida {
    CAIXA("Box", "cx"),
    UNIDADE("Unit", "un"),
    QUILOGRAMA("Kilogram", "kg");

    private final String valorXml;
    private final String abreviatura;

    UnidadeMedida(String valorXml, String abreviatura) {
        this.valorXml = valorXml;
        this.abreviatura = abreviatura;
    }

    /** @return a abreviatura mostrada ao utilizador e guardada no stock ({@code "cx"}, {@code "un"} ou {@code "kg"}) */
    public String getAbreviatura() {
        return abreviatura;
    }

    /** @return o texto usado no ficheiro XML (por exemplo {@code "Box"}) */
    public String getValorXml() {
        return valorXml;
    }

    /**
     * Converte o texto do atributo {@code UOM} do XML na unidade correspondente.
     *
     * @param valor texto do XML ({@code "Box"}, {@code "Unit"} ou {@code "Kilogram"})
     * @return a unidade de medida
     * @throws IllegalArgumentException se o texto não for uma unidade conhecida
     */
    public static UnidadeMedida deXml(String valor) {
        for (UnidadeMedida unidade : values()) {
            if (unidade.valorXml.equals(valor)) {
                return unidade;
            }
        }
        throw new IllegalArgumentException("Unidade de medida desconhecida: " + valor);
    }
}
