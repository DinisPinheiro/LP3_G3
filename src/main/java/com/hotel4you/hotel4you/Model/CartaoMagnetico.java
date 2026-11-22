package com.hotel4you.hotel4you.Model;

import com.hotel4you.hotel4you.Utils.Constantes;

/**
 * Cartão magnético entregue ao cliente no check-in, ou cartão mestre do gestor.
 * <p>
 * O cartão mestre abre todos os quartos e zonas. <b>Assunção:</b> um cartão normal
 * dá acesso aos pisos 1 (restaurante e piscina) e 2 e 3 (dormidas); a verificação
 * de que o quarto é mesmo o da reserva é feita pela {@link Reserva}.
 * <p>
 * Há três tipos de cartão: o <b>mestre</b> (código próprio, abre tudo), o de <b>funcionário</b>
 * (pertence a um utilizador, ver {@link #getIdUtilizador()}) e o de <b>cliente</b> (entregue no check-in).
 * <p>
 * Um cartão fica <b>ativo</b> quando é criado e é <b>desativado</b> no check-out
 * ({@link #desativar()}); um cartão desativado já não dá acesso a nenhum piso.
 */
public class CartaoMagnetico {

    private int id;
    private final int numero;
    private final boolean mestre;
    private boolean ativo;
    private final Integer idUtilizador;

    /**
     * Cria um cartão novo, ainda não guardado e ativo.
     *
     * @param numero número associado ao cartão (ex.: 12345)
     * @param mestre {@code true} para o cartão mestre do gestor
     */
    public CartaoMagnetico(int numero, boolean mestre) {
        this(0, numero, mestre, true);
    }

    /**
     * Reconstrói um cartão com todo o seu estado (usado pelos DAO).
     *
     * @param id     identificador (0 se ainda não foi guardado)
     * @param numero número associado ao cartão
     * @param mestre {@code true} para o cartão mestre do gestor
     * @param ativo  {@code true} se o cartão ainda pode ser usado
     */
    public CartaoMagnetico(int id, int numero, boolean mestre, boolean ativo) {
        this(id, numero, mestre, ativo, null);
    }

    /**
     * Reconstrói um cartão com todo o seu estado, incluindo o funcionário a quem pertence.
     *
     * @param id           identificador (0 se ainda não foi guardado)
     * @param numero       número associado ao cartão
     * @param mestre       {@code true} para o cartão mestre
     * @param ativo        {@code true} se o cartão ainda pode ser usado
     * @param idUtilizador identificador do funcionário dono do cartão, ou {@code null} se não for de funcionário
     * @throws IllegalArgumentException se o número não for positivo, se o id do utilizador não for positivo
     *                                  ou se o cartão mestre tiver dono (o mestre não pertence a ninguém)
     */
    public CartaoMagnetico(int id, int numero, boolean mestre, boolean ativo, Integer idUtilizador) {
        if (numero <= 0) {
            throw new IllegalArgumentException("O número do cartão deve ser positivo.");
        }
        if (idUtilizador != null && idUtilizador <= 0) {
            throw new IllegalArgumentException("O funcionário do cartão é inválido.");
        }
        if (mestre && idUtilizador != null) {
            throw new IllegalArgumentException("Um cartão mestre não pertence a um funcionário.");
        }
        this.id = id;
        this.numero = numero;
        this.mestre = mestre;
        this.ativo = ativo;
        this.idUtilizador = idUtilizador;
    }

    /**
     * Cria um cartão novo, ativo, para um funcionário.
     *
     * @param numero       número do cartão
     * @param idUtilizador identificador do funcionário (utilizador já guardado)
     * @return cartão de funcionário, ainda sem id
     */
    public static CartaoMagnetico paraFuncionario(int numero, int idUtilizador) {
        return new CartaoMagnetico(0, numero, false, true, idUtilizador);
    }

    /**
     * @param piso número do piso
     * @return {@code true} se o cartão está ativo e abre zonas desse piso
     */
    public boolean temAcessoAoPiso(int piso) {
        if (!ativo || !Constantes.pisoValido(piso)) {
            return false;
        }
        return mestre || piso >= Constantes.PISO_RESTAURANTE_PISCINA;
    }

    /** Desativa o cartão (feito no check-out): deixa de dar acesso a qualquer piso. */
    public void desativar() {
        this.ativo = false;
    }

    public int getId() {
        return id;
    }

    /** Usado pelo DAO ao guardar o cartão pela primeira vez. */
    public void setId(int id) {
        this.id = id;
    }

    public int getNumero() {
        return numero;
    }

    public boolean isMestre() {
        return mestre;
    }

    public boolean isAtivo() {
        return ativo;
    }

    /** @return {@code true} se o cartão pertence a um funcionário (e não a um cliente nem é o mestre) */
    public boolean isDeFuncionario() {
        return idUtilizador != null;
    }

    /** @return identificador do funcionário dono do cartão, ou {@code null} se não for de funcionário */
    public Integer getIdUtilizador() {
        return idUtilizador;
    }

    @Override
    public String toString() {
        return "Cartão " + numero + (mestre ? " (mestre)" : "") + (idUtilizador != null ? " (funcionário)" : "")
                + (ativo ? "" : " (desativado)");
    }
}