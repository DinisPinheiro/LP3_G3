package com.hotel4you.hotel4you.Model.enums;

/**
 * Estado de um ficheiro XML enviado por um fornecedor (S5-03).
 * <p>
 * Todo o ficheiro começa {@link #PENDENTE}; o gestor ou o rececionista passa-o a {@link #APROVADO}
 * (e só então dá entrada em stock) ou a {@link #REJEITADO}. Um ficheiro já decidido não muda de estado.
 */
public enum EstadoImportacao {
    PENDENTE("Pendente"),
    APROVADO("Aprovado"),
    REJEITADO("Rejeitado");

    private final String nome;

    EstadoImportacao(String nome) {
        this.nome = nome;
    }

    /** @return o nome do estado, pronto para mostrar ao utilizador */
    public String getNome() {
        return nome;
    }

    @Override
    public String toString() {
        return nome;
    }
}
