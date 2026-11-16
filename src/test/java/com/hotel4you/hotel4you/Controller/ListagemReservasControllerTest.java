package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.ClienteDAOMemoria;
import com.hotel4you.hotel4you.DAO.QuartoDAOMemoria;
import com.hotel4you.hotel4you.DAO.ReservaDAOMemoria;
import com.hotel4you.hotel4you.DAO.ServicoDAOMemoria;
import com.hotel4you.hotel4you.DAO.StockDAOMemoria;
import com.hotel4you.hotel4you.Model.CartaoMagnetico;
import com.hotel4you.hotel4you.Model.Cliente;
import com.hotel4you.hotel4you.Model.Quarto;
import com.hotel4you.hotel4you.Model.Reserva;
import com.hotel4you.hotel4you.Model.Utilizador;
import com.hotel4you.hotel4you.Model.enums.EstadoReserva;
import com.hotel4you.hotel4you.Model.enums.Perfil;
import com.hotel4you.hotel4you.Model.enums.TipoQuarto;
import com.hotel4you.hotel4you.Model.enums.Vista;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes dos ecrãs "Reservas ativas" e "Histórico de reservas": a listagem por estado e por página
 * (10 reservas por página). Reservas ativas são as pendentes e as com check-in.
 */
class ListagemReservasControllerTest {

    private ReservaDAOMemoria reservaDAO;
    private ClienteDAOMemoria clienteDAO;
    private QuartoDAOMemoria quartoDAO;
    private Sessao sessao;
    private GestaoReservasController controller;
    private Cliente maria;
    private int proximoQuarto;
    private final LocalDate hoje = LocalDate.now();

    @BeforeEach
    void preparar() {
        reservaDAO = new ReservaDAOMemoria();
        clienteDAO = new ClienteDAOMemoria();
        quartoDAO = new QuartoDAOMemoria();
        sessao = new Sessao();
        sessao.iniciar(Utilizador.novo("rececao", "rececao123", "Ana Rececionista", Perfil.RECECIONISTA));
        controller = new GestaoReservasController(reservaDAO, clienteDAO, quartoDAO,
                new ServicoDAOMemoria(), new StockDAOMemoria(), sessao);
        maria = clienteDAO.inserir(new Cliente(0, "Maria Silva", "PT123456", ""));
        proximoQuarto = 200;
    }

    /** Cria uma reserva num quarto novo (assim não há reservas sobrepostas). */
    private Reserva novaReserva() {
        Quarto quarto = quartoDAO.inserir(new Quarto(0, proximoQuarto++, 2, "", TipoQuarto.DUPLO,
                new BigDecimal("80.00"), Vista.NENHUMA, true));
        return reservaDAO.inserir(new Reserva(0, maria, quarto, hoje, hoje.plusDays(2)));
    }

    /** Cria uma reserva em cada estado: pendente, com check-in, com check-out e cancelada. */
    private void criarUmaReservaEmCadaEstado() {
        novaReserva();

        Reserva comCheckIn = novaReserva();
        comCheckIn.fazerCheckIn(new CartaoMagnetico(1, 1001, false, true));
        reservaDAO.atualizar(comCheckIn);

        Reserva comCheckOut = novaReserva();
        comCheckOut.fazerCheckIn(new CartaoMagnetico(2, 1002, false, true));
        comCheckOut.fazerCheckOut();
        reservaDAO.atualizar(comCheckOut);

        Reserva cancelada = novaReserva();
        cancelada.cancelar();
        reservaDAO.atualizar(cancelada);
    }

    // ---- Reservas ativas ----

    @Test
    void reservasAtivasMostramSoPendentesEComCheckIn() {
        criarUmaReservaEmCadaEstado();

        List<Reserva> ativas = controller.listarReservasFiltradas(false, null, 1);

        assertEquals(2, ativas.size());
        assertEquals(2, controller.contarReservasFiltradas(false, null));
        for (Reserva reserva : ativas) {
            assertTrue(reserva.getEstado() == EstadoReserva.PENDENTE || reserva.getEstado() == EstadoReserva.CHECK_IN);
        }
    }

    @Test
    void reservasAtivasPodemFiltrarPorEstado() {
        criarUmaReservaEmCadaEstado();

        List<Reserva> pendentes = controller.listarReservasFiltradas(false, EstadoReserva.PENDENTE, 1);
        List<Reserva> comCheckIn = controller.listarReservasFiltradas(false, EstadoReserva.CHECK_IN, 1);

        assertEquals(1, pendentes.size());
        assertEquals(EstadoReserva.PENDENTE, pendentes.get(0).getEstado());
        assertEquals(1, comCheckIn.size());
        assertEquals(EstadoReserva.CHECK_IN, comCheckIn.get(0).getEstado());
    }

    @Test
    void reservasAtivasNaoAceitamFiltrosDeReservasTerminadasOuCanceladas() {
        criarUmaReservaEmCadaEstado();

        assertThrows(IllegalArgumentException.class,
                () -> controller.listarReservasFiltradas(false, EstadoReserva.CHECK_OUT, 1));
        assertThrows(IllegalArgumentException.class,
                () -> controller.contarReservasFiltradas(false, EstadoReserva.CANCELADA));
    }

    @Test
    void semReservasAListaFicaVaziaEOTotalEZero() {
        assertTrue(controller.listarReservasFiltradas(false, null, 1).isEmpty());
        assertEquals(0, controller.contarReservasFiltradas(false, null));
    }

    // ---- Histórico ----

    @Test
    void historicoSemFiltroMostraSoReservasTerminadasECanceladas() {
        criarUmaReservaEmCadaEstado();

        List<Reserva> historico = controller.listarHistoricoComFiltro(null, false, 1);

        assertEquals(2, historico.size());
        assertEquals(2, controller.contarHistoricoComFiltro(null, false));
        for (Reserva reserva : historico) {
            assertTrue(reserva.getEstado() == EstadoReserva.CHECK_OUT || reserva.getEstado() == EstadoReserva.CANCELADA);
        }
    }

    @Test
    void historicoComFiltroTodosMostraOsQuatroEstados() {
        criarUmaReservaEmCadaEstado();

        assertEquals(4, controller.listarHistoricoComFiltro(null, true, 1).size());
        assertEquals(4, controller.contarHistoricoComFiltro(null, true));
    }

    @Test
    void historicoComFiltroPorEstadoMostraSoReservasDesseEstado() {
        criarUmaReservaEmCadaEstado();

        for (EstadoReserva estado : EstadoReserva.values()) {
            List<Reserva> reservas = controller.listarHistoricoComFiltro(estado, true, 1);

            assertEquals(1, reservas.size(), estado.name());
            assertEquals(estado, reservas.get(0).getEstado());
            assertEquals(1, controller.contarHistoricoComFiltro(estado, true), estado.name());
        }
    }

    // ---- Páginas (10 por página) ----

    @Test
    void asReservasSaoMostradasDezDeCadaVez() {
        for (int i = 0; i < 12; i++) {
            novaReserva();
        }

        assertEquals(12, controller.contarReservasFiltradas(false, null));
        assertEquals(10, controller.listarReservasFiltradas(false, null, 1).size());
        assertEquals(2, controller.listarReservasFiltradas(false, null, 2).size());
        assertTrue(controller.listarReservasFiltradas(false, null, 3).isEmpty());
    }

    @Test
    void umaPaginaInvalidaELancaErro() {
        assertThrows(IllegalArgumentException.class, () -> controller.listarReservasFiltradas(false, null, 0));
        assertThrows(IllegalArgumentException.class, () -> controller.listarHistoricoComFiltro(null, true, -1));
    }

    // ---- Permissões ----

    @Test
    void soORececionistaEOGestorConsultamAsListagens() {
        sessao.iniciar(Utilizador.novo("forn", "forn12345", "Fornecedor", Perfil.FORNECEDOR));

        assertThrows(SecurityException.class, () -> controller.listarReservasFiltradas(false, null, 1));
        assertThrows(SecurityException.class, () -> controller.listarHistoricoComFiltro(null, true, 1));

        sessao.iniciar(Utilizador.novo("gestor", "gestor123", "Gestor", Perfil.GESTOR));
        assertEquals(0, controller.contarReservasFiltradas(false, null));
    }
}