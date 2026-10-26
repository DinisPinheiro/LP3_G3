package com.hotel4you.hotel4you.Model.enums;

public enum TipoQuarto {
    INDIVIDUAL("Individual", 1, 1),
    DUPLO("Duplo", 2, 2),
    FAMILIA("Família", 3, 4);

    private final String nome;
    private final int capacidadeMinima;
    private final int capacidadeMaxima;

    TipoQuarto(String nome, int capacidadeMinima, int capacidadeMaxima) {
        this.nome = nome;
        this.capacidadeMinima = capacidadeMinima;
        this.capacidadeMaxima = capacidadeMaxima;
    }

    /** @return nome para mostrar ao utilizador */
    public String getNome() {
        return nome;
    }

    /** @return número mínimo de hóspedes */
    public int getCapacidadeMinima() {
        return capacidadeMinima;
    }

    /** @return número máximo de hóspedes (família: 3 ou 4) */
    public int getCapacidadeMaxima() {
        return capacidadeMaxima;
    }
}
