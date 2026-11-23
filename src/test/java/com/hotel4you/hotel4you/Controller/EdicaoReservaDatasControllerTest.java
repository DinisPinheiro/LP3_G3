package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.ClienteDAOMemoria;
import com.hotel4you.hotel4you.DAO.DAOException;
import com.hotel4you.hotel4you.DAO.QuartoDAOMemoria;
import com.hotel4you.hotel4you.DAO.ReservaDAOMemoria;
import com.hotel4you.hotel4you.DAO.ServicoDAOMemoria;
import com.hotel4you.hotel4you.Model.CartaoMagnetico;
import com.hotel4you.hotel4you.Model.Cliente;
import com.hotel4you.hotel4you.Model.Quarto;
import com.hotel4you.hotel4you.Model.Reserva;
import com.hotel4you.hotel4you.Model.Utilizador;
import com.hotel4you.hotel4you.Model.enums.Perfil;
import com.hotel4you.hotel4you.Model.enums.TipoQuarto;
import com.hotel4you.hotel4you.Model.enums.Vista;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Testes da S6-08 no {@link EdicaoReservaController}: o rececionista altera as datas de uma reserva ativa,
 * a alteração é recusada se o quarto estiver ocupado e o valor total é recalculado.
 */
class EdicaoReservaDatasControllerTest {

    private final LocalDate entrada = LocalDate.now().plusDays(10);
    private final LocalDate saida = LocalDate.now().plusDays(13);   // 3 noites

    private ReservaDAOMemoria reservaDAO;
    private Sessao sessao;
    private EdicaoReservaController controller;
    private Cliente maria;
    private Cliente rui;
    private Quarto quarto202;

    @BeforeEach
    void preparar() {
        ClienteDAOMemoria clienteDAO = new ClienteDAOMemoria();
        QuartoDAOMemoria quartoDAO = new QuartoDAOMemoria();
        reservaDAO = new ReservaDAOMemoria();
        sessao = new Sessao();
        sessao.iniciar(Utilizador.novo("rececao", "rececao123", "Ana Rececionista", Perfil.RECECIONISTA));
        controller = new EdicaoReservaController(reservaDAO, new ServicoDAOMemoria(), sessao);

        maria = clienteDAO.inserir(new Cliente(0, "Maria Silva", "PT123456", "910000000"));
        rui = clienteDAO.inserir(new Cliente(0, "Rui Costa", "PT654321", "920000000"));
        // 80 (preço base) + vista mar = 100 por noite
        quarto202 = quartoDAO.inserir(new Quarto(0, 202, 2, "", TipoQuarto.DUPLO,
                new BigDecimal("80.00"), Vista.MAR, true));
    }

    private Reserva reservar(Cliente cliente, LocalDate de, LocalDate ate) {
        return reservaDAO.inserir(new Reserva(0, cliente, quarto202, de, ate));
    }

    private static void assertValor(String esperado, BigDecimal obtido) {
        assertEquals(0, new BigDecimal(esperado).compareTo(obtido), "esperado " + esperado + ", obtido " + obtido);
    }

    @Test
    void alteraAsDatasDeUmaReservaAtivaComDatasLivres() {
        Reserva reserva = reservar(maria, entrada, saida);

        controller.alterarDatas(reserva.getId(), entrada.plusDays(5), saida.plusDays(7));

        Reserva guardada = controller.consultarReserva(reserva.getId());
        assertEquals(entrada.plusDays(5), guardada.getDataEntrada());
        assertEquals(saida.plusDays(7), guardada.getDataSaida());
    }

    @Test
    void recusaSeOQuartoEstiverOcupadoNasNovasDatas() {
        Reserva reserva = reservar(maria, entrada, saida);
        reservar(rui, entrada.plusDays(10), entrada.plusDays(14));

        assertThrows(IllegalArgumentException.class,
                () -> controller.alterarDatas(reserva.getId(), entrada, entrada.plusDays(12)));

        Reserva guardada = controller.consultarReserva(reserva.getId());
        assertEquals(entrada, guardada.getDataEntrada());
        assertEquals(saida, guardada.getDataSaida());
        assertValor("300.00", guardada.calcularTotal());
    }

    @Test
    void aReservaNaoContaComElaPropriaNaDisponibilidade() {
        Reserva reserva = reservar(maria, entrada, saida);

        // Prolonga a própria estadia: as noites que já eram dela não bloqueiam a alteração.
        controller.alterarDatas(reserva.getId(), entrada, saida.plusDays(2));

        assertEquals(saida.plusDays(2), controller.consultarReserva(reserva.getId()).getDataSaida());
    }

    @Test
    void permiteColarAOutraReservaMasNaoSobrepor() {
        Reserva reserva = reservar(maria, entrada, saida);
        reservar(rui, saida, saida.plusDays(3));

        controller.alterarDatas(reserva.getId(), entrada, saida);   // sem mudanças
        assertThrows(IllegalArgumentException.class,
                () -> controller.alterarDatas(reserva.getId(), entrada, saida.plusDays(1)));
    }

    @Test
    void recalculaOValorTotal() {
        Reserva reserva = reservar(maria, entrada, saida);
        assertValor("300.00", reserva.calcularTotal());

        Reserva alterada = controller.alterarDatas(reserva.getId(), entrada, entrada.plusDays(5));

        assertValor("500.00", alterada.calcularTotal());
        assertValor("500.00", controller.consultarReserva(reserva.getId()).calcularTotal());
    }

    @Test
    void validaAsDatas() {
        Reserva reserva = reservar(maria, entrada, saida);

        assertThrows(IllegalArgumentException.class, () -> controller.alterarDatas(reserva.getId(), null, saida));
        assertThrows(IllegalArgumentException.class, () -> controller.alterarDatas(reserva.getId(), entrada, null));
        assertThrows(IllegalArgumentException.class, () -> controller.alterarDatas(reserva.getId(), saida, entrada));
        assertThrows(IllegalArgumentException.class, () -> controller.alterarDatas(reserva.getId(), entrada, entrada));
        assertThrows(IllegalArgumentException.class,
                () -> controller.alterarDatas(reserva.getId(), LocalDate.now().minusDays(2), saida),
                "a entrada não pode ser no passado");
        assertThrows(IllegalArgumentException.class, () -> controller.alterarDatas(999, entrada, saida));
        assertEquals(entrada, controller.consultarReserva(reserva.getId()).getDataEntrada());
    }

    @Test
    void reservaComCheckInSoPodeAlterarASaida() {
        Reserva reserva = reservar(maria, entrada, saida);
        reserva.fazerCheckIn(new CartaoMagnetico(12345, false));
        reservaDAO.atualizar(reserva);

        controller.alterarDatas(reserva.getId(), entrada, saida.plusDays(1));
        assertEquals(saida.plusDays(1), controller.consultarReserva(reserva.getId()).getDataSaida());

        assertThrows(IllegalArgumentException.class,
                () -> controller.alterarDatas(reserva.getId(), entrada.plusDays(1), saida));
    }

    @Test
    void reservaCanceladaOuComCheckOutNaoPodeSerEditada() {
        Reserva cancelada = reservar(maria, entrada, saida);
        cancelada.cancelar();
        reservaDAO.atualizar(cancelada);
        assertThrows(IllegalStateException.class,
                () -> controller.alterarDatas(cancelada.getId(), entrada, saida.plusDays(1)));

        Reserva terminada = reservar(rui, entrada, saida);
        terminada.fazerCheckIn(new CartaoMagnetico(12346, false));
        terminada.fazerCheckOut();
        reservaDAO.atualizar(terminada);
        assertThrows(IllegalStateException.class,
                () -> controller.alterarDatas(terminada.getId(), entrada, saida.plusDays(1)));
    }

    @Test
    void seAGravacaoFalharAReservaVoltaAsDatasAnteriores() {
        Reserva reserva = reservar(maria, entrada, saida);
        EdicaoReservaController comFalha = new EdicaoReservaController(new ReservaDAOMemoria() {
            @Override
            public java.util.Optional<Reserva> procurarPorId(int id) {
                return java.util.Optional.of(reserva);
            }

            @Override
            public void atualizarDatas(Reserva r) {
                throw new DAOException("falha ao gravar");
            }
        }, new ServicoDAOMemoria(), sessao);

        assertThrows(DAOException.class,
                () -> comFalha.alterarDatas(reserva.getId(), entrada, saida.plusDays(3)));

        assertEquals(entrada, reserva.getDataEntrada());
        assertEquals(saida, reserva.getDataSaida());
    }

    @Test
    void soRececionistaEGestorPodemAlterarDatas() {
        Reserva reserva = reservar(maria, entrada, saida);

        sessao.iniciar(Utilizador.novo("gestor", "gestor123", "Gestor", Perfil.GESTOR));
        controller.alterarDatas(reserva.getId(), entrada, saida.plusDays(1));

        sessao.iniciar(Utilizador.novo("forn", "forn12345", "Fornecedor", Perfil.FORNECEDOR));
        assertThrows(SecurityException.class,
                () -> controller.alterarDatas(reserva.getId(), entrada, saida.plusDays(2)));
        sessao.terminar();
        assertThrows(SecurityException.class,
                () -> controller.alterarDatas(reserva.getId(), entrada, saida.plusDays(2)));
    }
}
