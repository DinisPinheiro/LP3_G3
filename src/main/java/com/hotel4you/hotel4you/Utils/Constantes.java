package com.hotel4you.hotel4you.Utils;

/**
 * Constantes da estrutura física do Hotel4You (enunciado, secção 2).
 * <p>
 * Pisos: -1 garagem/armazém, 0 receção/bar/conferências, 1 restaurante/piscina,
 * 2 e 3 dormidas. O hotel tem 20 quartos (7 individuais, 10 duplos e 3 de família),
 * 10 em cada piso de dormidas.
 */
public final class Constantes {

    public static final int PISO_GARAGEM_ARMAZEM = -1;
    public static final int PISO_RECECAO = 0;
    public static final int PISO_RESTAURANTE_PISCINA = 1;
    public static final int PRIMEIRO_PISO_DORMIDAS = 2;
    public static final int ULTIMO_PISO_DORMIDAS = 3;

    public static final int PISO_MIN = PISO_GARAGEM_ARMAZEM;
    public static final int PISO_MAX = ULTIMO_PISO_DORMIDAS;

    public static final int QUARTOS_POR_PISO = 10;
    public static final int TOTAL_INDIVIDUAIS = 7;
    public static final int TOTAL_DUPLOS = 10;
    public static final int TOTAL_FAMILIA = 3;
    public static final int TOTAL_QUARTOS = TOTAL_INDIVIDUAIS + TOTAL_DUPLOS + TOTAL_FAMILIA;

    private Constantes() {
    }

    /**
     * @param piso número do piso
     * @return {@code true} se o piso existe no hotel
     */
    public static boolean pisoValido(int piso) {
        return piso >= PISO_MIN && piso <= PISO_MAX;
    }

    /**
     * @param piso número do piso
     * @return {@code true} se o piso tem quartos
     */
    public static boolean pisoDeDormidas(int piso) {
        return piso >= PRIMEIRO_PISO_DORMIDAS && piso <= ULTIMO_PISO_DORMIDAS;
    }
}
