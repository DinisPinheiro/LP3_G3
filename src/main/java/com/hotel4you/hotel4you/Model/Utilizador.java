package com.hotel4you.hotel4you.Model;

import com.hotel4you.hotel4you.Utils.Constantes;
import com.hotel4you.hotel4you.Utils.PasswordUtils;
import com.hotel4you.hotel4you.Model.enums.Perfil;

import java.util.Objects;

/**
 * Utilizador do sistema (gestor, rececionista, empresa de restauração ou fornecedor).
 * <p>
 * A palavra-chave só existe na forma de hash; nunca é guardada nem comparada em claro.
 */
public class Utilizador {

    private int id;
    private final String username;
    private String passwordHash;
    private String nome;
    private Perfil perfil;
    private boolean ativo;

    /**
     * Reconstrói um utilizador já existente (ex.: lido da base de dados).
     *
     * @param id           identificador (0 se ainda não foi guardado)
     * @param username     nome de utilizador, único
     * @param passwordHash hash da palavra-chave, ver {@link PasswordUtils}
     * @param nome         nome completo
     * @param perfil       perfil (role)
     * @param ativo        se pode iniciar sessão
     */
    public Utilizador(int id, String username, String passwordHash, String nome, Perfil perfil, boolean ativo) {
        this.id = id;
        this.username = exigirTexto(username, "username");
        this.passwordHash = exigirTexto(passwordHash, "passwordHash");
        this.nome = exigirTexto(nome, "nome");
        this.perfil = Objects.requireNonNull(perfil, "O perfil é obrigatório.");
        this.ativo = ativo;
    }

    /**
     * Cria um utilizador novo e ativo, ainda sem id, encriptando a palavra-chave.
     *
     * @param username        nome de utilizador
     * @param passwordEmClaro palavra-chave (só é usada para gerar o hash)
     * @param nome            nome completo
     * @param perfil          perfil
     * @return utilizador pronto a ser inserido num DAO
     */
    public static Utilizador novo(String username, String passwordEmClaro, String nome, Perfil perfil) {
        return new Utilizador(0, username, PasswordUtils.gerarHash(passwordEmClaro), nome, perfil, true);
    }

    /**
     * @param password palavra-chave introduzida
     * @return {@code true} se corresponder ao hash guardado
     */
    public boolean verificarPassword(String password) {
        return PasswordUtils.verificar(password, passwordHash);
    }

    /**
     * Indica se o perfil pode aceder aos serviços de um piso.
     * <ul>
     *   <li>Gestor: todos os pisos;</li>
     *   <li>Rececionista: apenas pisos 0 e 1 (enunciado);</li>
     *   <li>Empresa de restauração: apenas o piso 1 (cozinha/restaurante);</li>
     *   <li>Fornecedor: apenas o piso -1 (armazém). <i>Assunção: o enunciado não o especifica.</i></li>
     * </ul>
     *
     * @param piso número do piso
     * @return {@code true} se tem acesso
     */
    public boolean podeAcederAoPiso(int piso) {
        if (!Constantes.pisoValido(piso)) {
            return false;
        }
        return switch (perfil) {
            case GESTOR -> true;
            case RECECIONISTA -> piso == Constantes.PISO_RECECAO || piso == Constantes.PISO_RESTAURANTE_PISCINA;
            case EMPRESA_RESTAURACAO -> piso == Constantes.PISO_RESTAURANTE_PISCINA;
            case FORNECEDOR -> piso == Constantes.PISO_GARAGEM_ARMAZEM;
        };
    }

    /** @return {@code true} apenas para gestores (enunciado: só gestores apagam reservas) */
    public boolean podeApagarReservas() {
        return perfil == Perfil.GESTOR;
    }

    /**
     * Substitui a palavra-chave, guardando apenas o novo hash.
     *
     * @param novaPasswordEmClaro nova palavra-chave
     */
    public void alterarPassword(String novaPasswordEmClaro) {
        this.passwordHash = PasswordUtils.gerarHash(novaPasswordEmClaro);
    }

    public int getId() {
        return id;
    }

    /** Usado pelo DAO ao guardar o utilizador pela primeira vez. */
    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    /** @return hash da palavra-chave (para o DAO persistir) */
    public String getPasswordHash() {
        return passwordHash;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = exigirTexto(nome, "nome");
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public void setPerfil(Perfil perfil) {
        this.perfil = Objects.requireNonNull(perfil, "O perfil é obrigatório.");
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    @Override
    public String toString() {
        return "Utilizador{id=" + id + ", username='" + username + "', perfil=" + perfil + ", ativo=" + ativo + "}";
    }

    private static String exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("O campo '" + campo + "' é obrigatório.");
        }
        return valor.trim();
    }
}
