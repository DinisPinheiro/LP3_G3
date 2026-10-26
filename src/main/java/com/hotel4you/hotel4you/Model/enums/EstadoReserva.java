package com.hotel4you.hotel4you.Model.enums;

/**
 * Estado do ciclo de vida de uma {@link Reserva}.
 */
public enum EstadoReserva {
    /** Reserva feita, check-in ainda não efetuado. */
    PENDENTE,
    /** Cliente instalado no quarto. */
    CHECK_IN,
    /** Estadia terminada. */
    CHECK_OUT,
    /** Reserva cancelada antes do check-in. */
    CANCELADA
}
