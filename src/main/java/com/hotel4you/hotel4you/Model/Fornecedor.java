package com.hotel4you.hotel4you.Model;

/**
 * Empresa que entrega material ao hotel. Fica guardada quando dá entrada o primeiro XML
 * desse fornecedor. O identificador é o {@code PartyIdentifier} do XML e é único.
 */
public class Fornecedor {

    private int id;
    private final String nome;
    private final String identificador;
    private final String contacto;

    /**
     * @param id            identificador na base de dados (0 se ainda não foi guardado)
     * @param nome          nome do fornecedor
     * @param identificador identificação única do fornecedor (coluna {@code Nif} da tabela Fornecedor)
     * @param contacto      contacto; pode ser vazio
     * @throws IllegalArgumentException se o nome ou o identificador estiverem vazios
     */
    public Fornecedor(int id, String nome, String identificador, String contacto) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome do fornecedor é obrigatório.");
        }
        if (identificador == null || identificador.isBlank()) {
            throw new IllegalArgumentException("O identificador do fornecedor é obrigatório.");
        }
        this.id = id;
        this.nome = nome.trim();
        this.identificador = identificador.trim();
        this.contacto = contacto == null ? "" : contacto.trim();
    }

    public int getId() {
        return id;
    }

    /** Usado pelo DAO ao guardar o fornecedor pela primeira vez. */
    public void setId(int id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public String getIdentificador() {
        return identificador;
    }

    public String getContacto() {
        return contacto;
    }

    @Override
    public String toString() {
        return nome + " (" + identificador + ")";
    }
}
