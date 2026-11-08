package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.Model.CartaoMagnetico;
import com.hotel4you.hotel4you.Model.Cliente;
import com.hotel4you.hotel4you.Model.Quarto;
import com.hotel4you.hotel4you.Model.Reserva;
import com.hotel4you.hotel4you.Model.enums.TipoQuarto;
import com.hotel4you.hotel4you.Model.enums.Vista;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes da regra do DAO: {@link ReservaDAO#remover(int)} só apaga reservas que nunca tiveram
 * check-in. Usa o DAO em memória, que respeita o mesmo contrato do DAO de SQL Server.
 */
class ReservaDAOMemoriaTest {

    private ReservaDAOMemoria dao;
    private Cliente maria;
    private Quarto quarto202;
    private Quarto quarto203;
    private final LocalDate entrada = LocalDate.now().plusDays(5);
    private final LocalDate saida = LocalDate.now().plusDays(8);

    @BeforeEach
    void preparar() {
        dao = new ReservaDAOMemoria();
        maria = new Cliente(1, "Maria Silva", "PT123456", "910000000");
        quarto202 = new Quarto(1, 202, 2, "", TipoQuarto.DUPLO, new BigDecimal("80.00"), Vista.MAR, true);
        quarto203 = new Quarto(2, 203, 2, "", TipoQuarto.INDIVIDUAL, new BigDecimal("50.00"), Vista.NENHUMA, true);
    }

    private Reserva guardar(Quarto quarto) {
        return dao.inserir(new Reserva(0, maria, quarto, entrada, saida));
    }

    // ---- Sem check-in: apaga ----

    @Test
    void removerUmaReservaSemCheckInApagaORegisto() {
        Reserva reserva = guardar(quarto202);

        dao.remover(reserva.getId());

        assertTrue(dao.procurarPorId(reserva.getId()).isEmpty());
        assertTrue(dao.listarTodos().isEmpty());
    }

    @Test
    void removerUmaReservaCanceladaSemCheckInApagaORegisto() {
        Reserva reserva = guardar(quarto202);
        reserva.cancelar();
        dao.atualizar(reserva);

        dao.remover(reserva.getId());

        assertTrue(dao.listarTodos().isEmpty());
    }

    // ---- Com check-in: recusa e mantém o histórico ----

    @Test
    void removerUmaReservaComCheckInLancaErroEMantemORegisto() {
        Reserva reserva = guardar(quarto202);
        reserva.fazerCheckIn(new CartaoMagnetico(1, 1001, false, true));
        dao.atualizar(reserva);

        DAOException erro = assertThrows(DAOException.class, () -> dao.remover(reserva.getId()));

        assertTrue(erro.getMessage().contains("check-in"));
        assertEquals(1, dao.listarTodos().size());
        assertTrue(dao.procurarPorId(reserva.getId()).isPresent());
    }

    @Test
    void removerUmaReservaComCheckOutLancaErroEMantemORegisto() {
        Reserva reserva = guardar(quarto202);
        reserva.fazerCheckIn(new CartaoMagnetico(1, 1001, false, true));
        reserva.fazerCheckOut();
        dao.atualizar(reserva);

        assertThrows(DAOException.class, () -> dao.remover(reserva.getId()));

        assertEquals(1, dao.listarTodos().size());
    }

    // ---- Outros casos ----

    @Test
    void removerUmaReservaInexistenteLancaErro() {
        DAOException erro = assertThrows(DAOException.class, () -> dao.remover(99));

        assertTrue(erro.getMessage().contains("não existe"));
    }

    @Test
    void soSeApagaAReservaPedidaEAsOutrasFicam() {
        Reserva semCheckIn = guardar(quarto202);
        Reserva comCheckIn = guardar(quarto203);
        comCheckIn.fazerCheckIn(new CartaoMagnetico(1, 1001, false, true));
        dao.atualizar(comCheckIn);

        dao.remover(semCheckIn.getId());

        assertEquals(1, dao.listarTodos().size());
        assertTrue(dao.procurarPorId(comCheckIn.getId()).isPresent());
    }
}
