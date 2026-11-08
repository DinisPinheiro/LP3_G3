package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.CartaoDAO;
import com.hotel4you.hotel4you.DAO.DAOException;
import com.hotel4you.hotel4you.DAO.ReservaDAOMemoria;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CheckInCheckOutControllerTest {

    private ReservaDAOMemoria reservaDAO;
    private CartaoDAOEmMemoria cartaoDAO;
    private Sessao sessao;
    private CheckInCheckOutController controller;
    private Cliente maria;
    private Quarto quarto202;
    private Quarto quarto203;
    private final LocalDate hoje = LocalDate.now();

    @BeforeEach
    void preparar() {
        reservaDAO = new ReservaDAOMemoria();
        cartaoDAO = new CartaoDAOEmMemoria();
        sessao = new Sessao();
        sessao.iniciar(Utilizador.novo("rececao", "rececao123", "Ana Rececionista", Perfil.RECECIONISTA));
        controller = new CheckInCheckOutController(reservaDAO, cartaoDAO, sessao);
        maria = new Cliente(1, "Maria Silva", "PT123456", "910000000");
        quarto202 = new Quarto(1, 202, 2, "", TipoQuarto.DUPLO, new BigDecimal("80.00"), Vista.MAR, true);
        quarto203 = new Quarto(2, 203, 2, "", TipoQuarto.INDIVIDUAL, new BigDecimal("50.00"), Vista.NENHUMA, true);
    }

    private Reserva guardar(Quarto quarto, LocalDate entrada, LocalDate saida) {
        return reservaDAO.inserir(new Reserva(0, maria, quarto, entrada, saida));
    }

    // ---- Check-in ----

    @Test
    void checkInAtribuiUmCartaoComNumeroUnicoEMudaOEstado() {
        Reserva reserva = guardar(quarto202, hoje, hoje.plusDays(2));

        CartaoMagnetico cartao = controller.fazerCheckIn(reserva.getId());

        assertNotNull(cartao);
        assertTrue(cartao.getId() > 0);
        assertTrue(cartao.getNumero() >= CartaoDAO.PRIMEIRO_NUMERO_CLIENTE);
        assertTrue(cartao.isAtivo());
        assertFalse(cartao.isMestre());
        Reserva guardada = reservaDAO.procurarPorId(reserva.getId()).orElseThrow();
        assertEquals(EstadoReserva.CHECK_IN, guardada.getEstado());
        assertEquals(cartao, guardada.getCartao());
        assertNotNull(guardada.getDataCheckIn());
        assertEquals(1, cartaoDAO.listarTodos().size());
    }

    @Test
    void doisCheckInsRecebemNumerosDeCartaoDiferentes() {
        Reserva a = guardar(quarto202, hoje, hoje.plusDays(2));
        Reserva b = guardar(quarto203, hoje, hoje.plusDays(2));

        CartaoMagnetico cartaoA = controller.fazerCheckIn(a.getId());
        CartaoMagnetico cartaoB = controller.fazerCheckIn(b.getId());

        assertNotEquals(cartaoA.getNumero(), cartaoB.getNumero());
    }

    @Test
    void oNumeroDeUmCartaoDesativadoNaoVoltaASerUsado() {
        Reserva a = guardar(quarto202, hoje, hoje.plusDays(2));
        int numeroA = controller.fazerCheckIn(a.getId()).getNumero();
        controller.fazerCheckOut(a.getId());
        Reserva b = guardar(quarto203, hoje, hoje.plusDays(2));

        int numeroB = controller.fazerCheckIn(b.getId()).getNumero();

        assertNotEquals(numeroA, numeroB);
    }

    @Test
    void checkInAindaPodeSerFeitoSeOClienteChegaDepoisDoDiaDeEntrada() {
        Reserva reserva = guardar(quarto202, hoje.minusDays(2), hoje.plusDays(1));

        controller.fazerCheckIn(reserva.getId());

        assertEquals(EstadoReserva.CHECK_IN, reservaDAO.procurarPorId(reserva.getId()).orElseThrow().getEstado());
    }

    @Test
    void checkInAntesDoDiaDeEntradaERejeitado() {
        Reserva reserva = guardar(quarto202, hoje.plusDays(3), hoje.plusDays(5));

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> controller.fazerCheckIn(reserva.getId()));

        assertTrue(erro.getMessage().contains("a partir de"));
        assertEquals(EstadoReserva.PENDENTE, reserva.getEstado());
        assertTrue(cartaoDAO.listarTodos().isEmpty());
    }

    @Test
    void checkInDepoisDeTerminadoOPeriodoERejeitado() {
        Reserva reserva = guardar(quarto202, hoje.minusDays(5), hoje.minusDays(2));

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> controller.fazerCheckIn(reserva.getId()));

        assertTrue(erro.getMessage().contains("terminou"));
        assertEquals(EstadoReserva.PENDENTE, reserva.getEstado());
        assertTrue(cartaoDAO.listarTodos().isEmpty());
    }

    @Test
    void checkInNoDiaDeSaidaERejeitado() {
        Reserva reserva = guardar(quarto202, hoje.minusDays(2), hoje);

        assertThrows(IllegalArgumentException.class, () -> controller.fazerCheckIn(reserva.getId()));
        assertTrue(cartaoDAO.listarTodos().isEmpty());
    }

    @Test
    void checkInSoEPossivelEmReservasPendentes() {
        Reserva jaComCheckIn = guardar(quarto202, hoje, hoje.plusDays(2));
        controller.fazerCheckIn(jaComCheckIn.getId());
        Reserva comCheckOut = guardar(quarto203, hoje, hoje.plusDays(2));
        controller.fazerCheckIn(comCheckOut.getId());
        controller.fazerCheckOut(comCheckOut.getId());
        Reserva cancelada = guardar(new Quarto(3, 204, 2, "", TipoQuarto.DUPLO, new BigDecimal("80.00"),
                Vista.NENHUMA, true), hoje, hoje.plusDays(2));
        cancelada.cancelar();
        int cartoesAntes = cartaoDAO.listarTodos().size();

        IllegalArgumentException erro1 = assertThrows(IllegalArgumentException.class,
                () -> controller.fazerCheckIn(jaComCheckIn.getId()));
        IllegalArgumentException erro2 = assertThrows(IllegalArgumentException.class,
                () -> controller.fazerCheckIn(comCheckOut.getId()));
        IllegalArgumentException erro3 = assertThrows(IllegalArgumentException.class,
                () -> controller.fazerCheckIn(cancelada.getId()));

        assertTrue(erro1.getMessage().contains("já tem check-in"));
        assertTrue(erro2.getMessage().contains("já terminou"));
        assertTrue(erro3.getMessage().contains("cancelada"));
        assertEquals(cartoesAntes, cartaoDAO.listarTodos().size());
    }

    @Test
    void checkInDeReservaInexistenteLancaErro() {
        assertThrows(IllegalArgumentException.class, () -> controller.fazerCheckIn(99));
    }

    @Test
    void seAReservaNaoForGuardadaOCartaoNaoFicaPerdido() {
        ReservaDAOMemoria falha = new ReservaDAOMemoria() {
            @Override
            public void atualizar(Reserva reserva) {
                throw new DAOException("falha ao guardar");
            }
        };
        CheckInCheckOutController comFalha = new CheckInCheckOutController(falha, cartaoDAO, sessao);
        Reserva reserva = falha.inserir(new Reserva(0, maria, quarto202, hoje, hoje.plusDays(2)));

        assertThrows(DAOException.class, () -> comFalha.fazerCheckIn(reserva.getId()));

        assertTrue(cartaoDAO.listarTodos().isEmpty());
    }

    // ---- Check-out ----

    @Test
    void checkOutRegistaASaidaDesativaOCartaoELibertaOQuarto() {
        Reserva reserva = guardar(quarto202, hoje, hoje.plusDays(3));
        CartaoMagnetico cartao = controller.fazerCheckIn(reserva.getId());
        assertTrue(reservaDAO.existeSobreposicao(quarto202.getId(), hoje, hoje.plusDays(3)));

        Reserva depois = controller.fazerCheckOut(reserva.getId());

        assertEquals(EstadoReserva.CHECK_OUT, depois.getEstado());
        assertNotNull(depois.getDataCheckOut());
        assertFalse(cartaoDAO.procurarPorId(cartao.getId()).orElseThrow().isAtivo());
        assertFalse(cartao.temAcessoAoPiso(2));
        assertFalse(reservaDAO.existeSobreposicao(quarto202.getId(), hoje, hoje.plusDays(3)));
    }

    @Test
    void checkOutAntecipadoLibertaOQuartoNoRestanteDoPeriodo() {
        Reserva reserva = guardar(quarto202, hoje, hoje.plusDays(5));
        controller.fazerCheckIn(reserva.getId());

        controller.fazerCheckOut(reserva.getId());

        Reserva novaReserva = guardar(quarto202, hoje.plusDays(1), hoje.plusDays(4));
        assertEquals(EstadoReserva.PENDENTE, novaReserva.getEstado());
    }

    @Test
    void checkOutSemCheckInERejeitado() {
        Reserva reserva = guardar(quarto202, hoje, hoje.plusDays(2));

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> controller.fazerCheckOut(reserva.getId()));

        assertTrue(erro.getMessage().contains("ainda não tem check-in"));
        assertEquals(EstadoReserva.PENDENTE, reserva.getEstado());
    }

    @Test
    void checkOutDuasVezesOuEmReservaCanceladaERejeitado() {
        Reserva feita = guardar(quarto202, hoje, hoje.plusDays(2));
        controller.fazerCheckIn(feita.getId());
        controller.fazerCheckOut(feita.getId());
        Reserva cancelada = guardar(quarto203, hoje, hoje.plusDays(2));
        cancelada.cancelar();

        IllegalArgumentException erro1 = assertThrows(IllegalArgumentException.class,
                () -> controller.fazerCheckOut(feita.getId()));
        IllegalArgumentException erro2 = assertThrows(IllegalArgumentException.class,
                () -> controller.fazerCheckOut(cancelada.getId()));

        assertTrue(erro1.getMessage().contains("já tem check-out"));
        assertTrue(erro2.getMessage().contains("cancelada"));
    }

    @Test
    void checkOutDeReservaInexistenteLancaErro() {
        assertThrows(IllegalArgumentException.class, () -> controller.fazerCheckOut(99));
    }

    // ---- Permissões ----

    @Test
    void gestorTambemPodeFazerCheckInECheckOut() {
        Reserva reserva = guardar(quarto202, hoje, hoje.plusDays(2));
        sessao.iniciar(Utilizador.novo("gestor", "gestor123", "Gestor", Perfil.GESTOR));

        controller.fazerCheckIn(reserva.getId());
        controller.fazerCheckOut(reserva.getId());

        assertEquals(EstadoReserva.CHECK_OUT, reservaDAO.procurarPorId(reserva.getId()).orElseThrow().getEstado());
    }

    @Test
    void outrosPerfisESemSessaoNaoPodemFazerCheckInNemCheckOut() {
        Reserva reserva = guardar(quarto202, hoje, hoje.plusDays(2));

        sessao.iniciar(Utilizador.novo("forn", "forn12345", "Fornecedor", Perfil.FORNECEDOR));
        assertThrows(SecurityException.class, () -> controller.fazerCheckIn(reserva.getId()));
        assertThrows(SecurityException.class, () -> controller.fazerCheckOut(reserva.getId()));

        sessao.terminar();
        assertThrows(SecurityException.class, () -> controller.fazerCheckIn(reserva.getId()));
        assertThrows(SecurityException.class, () -> controller.fazerCheckOut(reserva.getId()));
        assertEquals(EstadoReserva.PENDENTE, reserva.getEstado());
        assertTrue(cartaoDAO.listarTodos().isEmpty());
    }

    // ---- DAO de cartões em memória, só para este teste ----

    static class CartaoDAOEmMemoria implements CartaoDAO {

        private final Map<Integer, CartaoMagnetico> cartoes = new LinkedHashMap<>();
        private int proximoId = 1;

        @Override
        public CartaoMagnetico inserir(CartaoMagnetico cartao) {
            if (procurarPorNumero(cartao.getNumero()).isPresent()) {
                throw new DAOException("Já existe um cartão com o número " + cartao.getNumero() + ".");
            }
            cartao.setId(proximoId++);
            cartoes.put(cartao.getId(), cartao);
            return cartao;
        }

        @Override
        public void atualizar(CartaoMagnetico cartao) {
            if (!cartoes.containsKey(cartao.getId())) {
                throw new DAOException("O cartão com id " + cartao.getId() + " não existe.");
            }
            cartoes.put(cartao.getId(), cartao);
        }

        @Override
        public void remover(int id) {
            if (cartoes.remove(id) == null) {
                throw new DAOException("O cartão com id " + id + " não existe.");
            }
        }

        @Override
        public Optional<CartaoMagnetico> procurarPorId(int id) {
            return Optional.ofNullable(cartoes.get(id));
        }

        @Override
        public Optional<CartaoMagnetico> procurarPorNumero(int numero) {
            return cartoes.values().stream().filter(c -> c.getNumero() == numero).findFirst();
        }

        @Override
        public List<CartaoMagnetico> listarTodos() {
            return new ArrayList<>(cartoes.values());
        }

        @Override
        public int proximoNumero() {
            int maior = cartoes.values().stream().mapToInt(CartaoMagnetico::getNumero).max().orElse(0);
            return Math.max(maior + 1, PRIMEIRO_NUMERO_CLIENTE);
        }
    }
}