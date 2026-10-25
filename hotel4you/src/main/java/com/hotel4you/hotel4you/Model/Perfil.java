package com.hotel4you.hotel4you.Model;

/**
 * Perfil (role) de um utilizador do sistema.
 * <p>
 * Define o que cada utilizador pode ver e fazer (RF-03).
 */
public enum Perfil {
    RECECIONISTA("Rececionista"),
    GESTOR("Gestor"),
    EMPRESA_RESTAURACAO("Empresa de restauração"),
    FORNECEDOR("Fornecedor");

    private final String nome;

    Perfil(String nome) {
        this.nome = nome;
    }

    /**
     * @return o nome do perfil, pronto para mostrar ao utilizador
     */
    public String getNome() {
        return nome;
    }
}
