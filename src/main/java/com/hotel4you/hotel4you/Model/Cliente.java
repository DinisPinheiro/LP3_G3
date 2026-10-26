package com.hotel4you.hotel4you.Model;

/**
 * Cliente do hotel: quem faz reservas e usufrui dos serviços.
 */
public class Cliente {

    private int id;
    private String nome;
    private String documento;
    private String contacto;

    /**
     * @param id        identificador (0 se ainda não foi guardado)
     * @param nome      nome completo
     * @param documento número do documento de identificação
     * @param contacto  telefone ou email
     */
    public Cliente(int id, String nome, String documento, String contacto) {
        this.id = id;
        this.nome = exigirTexto(nome, "nome");
        this.documento = exigirTexto(documento, "documento");
        this.contacto = contacto == null ? "" : contacto.trim();
    }

    public int getId() {
        return id;
    }

    /** Usado pelo DAO ao guardar o cliente pela primeira vez. */
    public void setId(int id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = exigirTexto(nome, "nome");
    }

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = exigirTexto(documento, "documento");
    }

    public String getContacto() {
        return contacto;
    }

    public void setContacto(String contacto) {
        this.contacto = contacto == null ? "" : contacto.trim();
    }

    @Override
    public String toString() {
        return nome + " (" + documento + ")";
    }

    private static String exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("O campo '" + campo + "' é obrigatório.");
        }
        return valor.trim();
    }
}
