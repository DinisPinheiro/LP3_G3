package com.hotel4you.hotel4you.Model;

import com.hotel4you.hotel4you.Utils.Constantes;

/**
 * Cartão magnético entregue ao cliente no check-in, ou cartão mestre do gestor.
 * <p>
 * O cartão mestre abre todos os quartos e zonas. <b>Assunção:</b> um cartão normal
 * dá acesso aos pisos 1 (restaurante e piscina) e 2 e 3 (dormidas); a verificação
 * de que o quarto é mesmo o da reserva é feita pela {@link Reserva}.
 */
public class CartaoMagnetico {

    private final int numero;
    private final boolean mestre;

    /**
     * @param numero número associado ao cartão (ex.: 12345)
     * @param mestre {@code true} para o cartão mestre do gestor
     */
    public CartaoMagnetico(int numero, boolean mestre) {
        if (numero <= 0) {
            throw new IllegalArgumentException("O número do cartão deve ser positivo.");
        }
        this.numero = numero;
        this.mestre = mestre;
    }

    /**
     * @param piso número do piso
     * @return {@code true} se o cartão abre zonas desse piso
     */
    public boolean temAcessoAoPiso(int piso) {
        if (!Constantes.pisoValido(piso)) {
            return false;
        }
        return mestre || piso >= Constantes.PISO_RESTAURANTE_PISCINA;
    }

    public int getNumero() {
        return numero;
    }

    public boolean isMestre() {
        return mestre;
    }

    @Override
    public String toString() {
        return "Cartão " + numero + (mestre ? " (mestre)" : "");
    }
}
