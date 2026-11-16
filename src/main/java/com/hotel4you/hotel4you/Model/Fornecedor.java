package com.hotel4you.hotel4you.Model;

import com.hotel4you.hotel4you.Model.encomenda.FornecedorEncomenda;

/**
 * Empresa que entrega material ao hotel. É criada pelo gestor (com conta de login própria) ou fica
 * guardada quando dá entrada o primeiro XML desse fornecedor. O código ({@link #getIdentificador()})
 * é o {@code PartyIdentifier} do XML e é único.
 * <p>
 * A ficha (nome, morada, código postal e cidade) pode ser atualizada a partir dos dados de um XML
 * importado, ver {@link #atualizarDadosDe(FornecedorEncomenda)}.
 */
public class Fornecedor {

    /** Limites das colunas de {@code dbo.Fornecedor}. */
    public static final int MAX_IDENTIFICADOR = 20;
    public static final int MAX_NOME = 100;
    public static final int MAX_MORADA = 200;
    public static final int MAX_CODIGO_POSTAL = 20;
    public static final int MAX_CIDADE = 100;

    private int id;
    private String nome;
    private final String identificador;
    private final String contacto;
    private String morada;
    private String codigoPostal;
    private String cidade;
    private int idUtilizador;

    /**
     * Fornecedor só com os dados mínimos (os que vêm de uma entrada de stock): sem morada, código
     * postal, cidade nem conta de login.
     *
     * @param id            identificador na base de dados (0 se ainda não foi guardado)
     * @param nome          nome do fornecedor
     * @param identificador código único do fornecedor (coluna {@code Nif} da tabela Fornecedor)
     * @param contacto      contacto; pode ser vazio
     * @throws IllegalArgumentException se o nome ou o identificador estiverem vazios
     */
    public Fornecedor(int id, String nome, String identificador, String contacto) {
        this(id, nome, identificador, contacto, "", "", "", 0);
    }

    /**
     * @param id             identificador na base de dados (0 se ainda não foi guardado)
     * @param nome           nome do fornecedor
     * @param identificador  código único do fornecedor (coluna {@code Nif} da tabela Fornecedor)
     * @param contacto       contacto; pode ser vazio
     * @param morada         morada; pode ser vazia
     * @param codigoPostal   código postal; pode ser vazio
     * @param cidade         cidade; pode ser vazia
     * @param idUtilizador   id da conta de login do fornecedor (0 se ainda não tem conta)
     * @throws IllegalArgumentException se o nome ou o identificador estiverem vazios
     */
    public Fornecedor(int id, String nome, String identificador, String contacto, String morada,
                      String codigoPostal, String cidade, int idUtilizador) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome do fornecedor é obrigatório.");
        }
        if (identificador == null || identificador.isBlank()) {
            throw new IllegalArgumentException("O identificador do fornecedor é obrigatório.");
        }
        this.id = id;
        this.nome = nome.trim();
        this.identificador = identificador.trim();
        this.contacto = limpar(contacto);
        this.morada = limpar(morada);
        this.codigoPostal = limpar(codigoPostal);
        this.cidade = limpar(cidade);
        this.idUtilizador = idUtilizador;
    }

    /**
     * Atualiza a ficha com os dados do fornecedor que vêm num XML (nome, morada, código postal e
     * cidade). O código do fornecedor não muda. A morada junta as duas linhas do XML.
     *
     * @param dados dados do fornecedor lidos do XML
     * @throws IllegalArgumentException se o código do XML não for o deste fornecedor ou o nome vier vazio
     */
    public void atualizarDadosDe(FornecedorEncomenda dados) {
        if (!identificador.equalsIgnoreCase(dados.identificador().trim())) {
            throw new IllegalArgumentException("Os dados do XML são do fornecedor '" + dados.identificador()
                    + "' e não deste fornecedor ('" + identificador + "').");
        }
        if (dados.nome() == null || dados.nome().isBlank()) {
            throw new IllegalArgumentException("O nome do fornecedor no XML está vazio.");
        }
        atualizarFicha(dados.nome(), juntarMorada(dados.morada1(), dados.morada2()),
                dados.codigoPostal(), dados.cidade());
    }

    /**
     * Substitui o nome, a morada, o código postal e a cidade da ficha. O código do fornecedor não muda.
     *
     * @param nome         novo nome
     * @param morada       nova morada (pode ser vazia)
     * @param codigoPostal novo código postal (pode ser vazio)
     * @param cidade       nova cidade (pode ser vazia)
     * @throws IllegalArgumentException se o nome estiver vazio
     */
    public void atualizarFicha(String nome, String morada, String codigoPostal, String cidade) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome do fornecedor é obrigatório.");
        }
        this.nome = nome.trim();
        this.morada = limpar(morada);
        this.codigoPostal = limpar(codigoPostal);
        this.cidade = limpar(cidade);
    }

    /**
     * @param morada1 primeira linha da morada
     * @param morada2 segunda linha da morada (pode ser vazia)
     * @return as duas linhas separadas por vírgula, ou só a primeira se a segunda estiver vazia
     */
    public static String juntarMorada(String morada1, String morada2) {
        String primeira = limpar(morada1);
        String segunda = limpar(morada2);
        if (segunda.isEmpty()) {
            return primeira;
        }
        return primeira.isEmpty() ? segunda : primeira + ", " + segunda;
    }

    /** @return {@code true} se o fornecedor já tem conta de login */
    public boolean temConta() {
        return idUtilizador > 0;
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

    public String getMorada() {
        return morada;
    }

    public String getCodigoPostal() {
        return codigoPostal;
    }

    public String getCidade() {
        return cidade;
    }

    /** @return id da conta de login do fornecedor, ou 0 se não tem conta */
    public int getIdUtilizador() {
        return idUtilizador;
    }

    /**
     * @param idUtilizador id da conta de login do fornecedor
     */
    public void setIdUtilizador(int idUtilizador) {
        this.idUtilizador = idUtilizador;
    }

    @Override
    public String toString() {
        return nome + " (" + identificador + ")";
    }

    private static String limpar(String texto) {
        return texto == null ? "" : texto.trim();
    }
}
