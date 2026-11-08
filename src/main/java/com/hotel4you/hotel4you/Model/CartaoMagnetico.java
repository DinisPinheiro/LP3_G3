package com.hotel4you.hotel4you.Model;

import com.hotel4you.hotel4you.Utils.Constantes;

/**
 * Cartão magnético entregue ao cliente no check-in, ou cartão mestre do gestor.
 * <p>
 * O cartão mestre abre todos os quartos e zonas. <b>Assunção:</b> um cartão normal
 * dá acesso aos pisos 1 (restaurante e piscina) e 2 e 3 (dormidas); a verificação
 * de que o quarto é mesmo o da reserva é feita pela {@link Reserva}.
 * <p>
 * Um cartão fica <b>ativo</b> quando é criado e é <b>desativado</b> no check-out
 * ({@link #desativar()}); um cartão desativado já não dá acesso a nenhum piso.
 */
public class CartaoMagnetico {

    private int id;
    private final int numero;
    private final boolean mestre;
    private boolean ativo;

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
        if (numero <= 0) {
            throw new IllegalArgumentException("O número do cartão deve ser positivo.");
        }
        this.id = id;
        this.numero = numero;
        this.mestre = mestre;
        this.ativo = ativo;
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

    @Override
    public String toString() {
        return "Cartão " + numero + (mestre ? " (mestre)" : "") + (ativo ? "" : " (desativado)");
    }
}