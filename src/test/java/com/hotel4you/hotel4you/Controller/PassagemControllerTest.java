package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.CartaoDAOMemoria;
import com.hotel4you.hotel4you.DAO.PassagemDAOMemoria;
import com.hotel4you.hotel4you.DAO.QuartoDAOMemoria;
import com.hotel4you.hotel4you.DAO.ReservaDAOMemoria;
import com.hotel4you.hotel4you.DAO.UtilizadorDAOMemoria;
import com.hotel4you.hotel4you.Model.CartaoMagnetico;
import com.hotel4you.hotel4you.Model.Cliente;
import com.hotel4you.hotel4you.Model.ConsumoServico;
import com.hotel4you.hotel4you.Model.LocalHotel;
import com.hotel4you.hotel4you.Model.Passagem;
import com.hotel4you.hotel4you.Model.Quarto;
import com.hotel4you.hotel4you.Model.Reserva;
import com.hotel4you.hotel4you.Model.Servico;
import com.hotel4you.hotel4you.Model.Utilizador;
import com.hotel4you.hotel4you.Model.enums.Perfil;
import com.hotel4you.hotel4you.Model.enums.TipoQuarto;
import com.hotel4you.hotel4you.Model.enums.Vista;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes do controller de picagem (S6-06): picar o cartão sem login (cliente com NIF, funcionários
 * por perfil e cartão mestre) e consultar as passagens por cartão, quarto e local (só o gestor).
 */
class PassagemControllerTest {

    private static final int CARTAO_CLIENTE = 1001;
    private static final int CARTAO_GESTOR = 201;
    private static final int CARTAO_RECECIONISTA = 202;
    private static final int CARTAO_RESTAURACAO = 203;
    private static final int CARTAO_MESTRE = 1;
    private static final String NIF = "123456789";

    private Sessao sessao;
    private PassagemDAOMemoria passagens;
    private ReservaDAOMemoria reservas;
    private PassagemController controller;
    private Reserva reserva;

    @BeforeEach
    void preparar() {
        sessao = new Sessao();
        passagens = new PassagemDAOMemoria();
        reservas = new ReservaDAOMemoria();
        CartaoDAOMemoria cartoes = new CartaoDAOMemoria();
        QuartoDAOMemoria quartos = new QuartoDAOMemoria();
        UtilizadorDAOMemoria utilizadores = new UtilizadorDAOMemoria();

        Quarto quarto202 = quartos.inserir(new Quarto(0, 202, 2, "", TipoQuarto.DUPLO,
                new BigDecimal("80.00"), Vista.MAR, true));
        quartos.inserir(new Quarto(0, 203, 2, "", TipoQuarto.DUPLO, new BigDecimal("80.00"), Vista.NENHUMA, true));

        CartaoMagnetico cartaoCliente = cartoes.inserir(new CartaoMagnetico(CARTAO_CLIENTE, false));
        cartoes.inserir(new CartaoMagnetico(CARTAO_MESTRE, true));
        cartoes.inserir(cartaoDe(utilizadores, "gestor", Perfil.GESTOR, CARTAO_GESTOR));
        cartoes.inserir(cartaoDe(utilizadores, "rececao", Perfil.RECECIONISTA, CARTAO_RECECIONISTA));
        cartoes.inserir(cartaoDe(utilizadores, "cozinha", Perfil.EMPRESA_RESTAURACAO, CARTAO_RESTAURACAO));

        reserva = reservas.inserir(new Reserva(0, new Cliente(1, "Maria", NIF, "910000000"), quarto202,
                LocalDate.now(), LocalDate.now().plusDays(2)));
        reserva.fazerCheckIn(cartaoCliente);
        reservas.atualizar(reserva);

        controller = new PassagemController(passagens, cartoes, reservas, quartos, utilizadores, sessao);
    }

    // ---------------------------------------------------------------- locais e picagem sem login

    @Test
    void listaOsLocaisDoHotelSemSessao() {
        List<LocalHotel> locais = controller.listarLocais();

        assertEquals(8, locais.size());
        assertEquals(LocalHotel.QUARTO, locais.get(0).getCodigo());
    }

    @Test
    void picarNaoExigeSessaoIniciada() {
        Passagem passagem = controller.picar(CARTAO_CLIENTE, NIF, "QUARTO", 202);

        assertNotNull(passagem.getDataHora());
        assertEquals(1, passagens.guardadas().size());
    }

    // ---------------------------------------------------------------- cliente

    @Test
    void clienteEntraNoSeuQuartoComONif() {
        Passagem passagem = controller.picar(CARTAO_CLIENTE, NIF, "QUARTO", 202);

        assertEquals(CARTAO_CLIENTE, passagem.getNumeroCartao());
        assertEquals("QUARTO", passagem.getLocal());
        assertEquals(202, passagem.getNumeroQuarto());
        assertNotNull(passagem.getIdQuarto());
    }

    @Test
    void nifComEspacosEAceite() {
        controller.picar(CARTAO_CLIENTE, " 123 456 789 ", "QUARTO", 202);

        assertEquals(1, passagens.guardadas().size());
    }

    @Test
    void clienteSemNifNaoPica() {
        assertThrows(IllegalArgumentException.class, () -> controller.picar(CARTAO_CLIENTE, "", "QUARTO", 202));
        assertThrows(IllegalArgumentException.class, () -> controller.picar(CARTAO_CLIENTE, null, "QUARTO", 202));

        assertTrue(passagens.guardadas().isEmpty());
    }

    @Test
    void nifQueNaoECorrespondeAoTitularERecusado() {
        AcessoRecusadoException erro = assertThrows(AcessoRecusadoException.class,
                () -> controller.picar(CARTAO_CLIENTE, "999999999", "QUARTO", 202));

        assertEquals("O NIF não corresponde ao titular do cartão.", erro.getMessage());
        assertTrue(passagens.guardadas().isEmpty());
    }

    @Test
    void clienteNaoEntraNumQuartoQueNaoEDaReserva() {
        assertThrows(AcessoRecusadoException.class, () -> controller.picar(CARTAO_CLIENTE, NIF, "QUARTO", 203));

        assertTrue(passagens.guardadas().isEmpty());
    }

    @Test
    void quartoInexistenteEErroDeDados() {
        assertThrows(IllegalArgumentException.class, () -> controller.picar(CARTAO_CLIENTE, NIF, "QUARTO", 999));
        assertThrows(IllegalArgumentException.class, () -> controller.picar(CARTAO_CLIENTE, NIF, "QUARTO", null));
    }

    @Test
    void clienteNaoEntraNaCozinha() {
        assertThrows(AcessoRecusadoException.class, () -> controller.picar(CARTAO_CLIENTE, NIF, "COZINHA", null));
    }

    @Test
    void restauranteSoComOServicoNaReserva() {
        assertThrows(AcessoRecusadoException.class, () -> controller.picar(CARTAO_CLIENTE, NIF, "RESTAURANTE", null));

        reserva.adicionarConsumo(new ConsumoServico(
                new Servico(1, "Jantar no restaurante", "", new BigDecimal("20.00")), 1, LocalDate.now()));
        reservas.atualizar(reserva);

        Passagem passagem = controller.picar(CARTAO_CLIENTE, NIF, "RESTAURANTE", null);

        assertEquals("RESTAURANTE", passagem.getLocal());
        assertNull(passagem.getIdQuarto());
    }

    @Test
    void cartaoDeClienteDepoisDoCheckOutJaNaoAbreNada() {
        reserva.fazerCheckOut();
        reservas.atualizar(reserva);

        assertThrows(AcessoRecusadoException.class, () -> controller.picar(CARTAO_CLIENTE, NIF, "QUARTO", 202));
    }

    // ---------------------------------------------------------------- funcionários

    @Test
    void gestorPicaOndeQuiser() {
        controller.picar(CARTAO_GESTOR, null, "RECECAO", null);
        controller.picar(CARTAO_GESTOR, null, "ARMAZEM", null);
        controller.picar(CARTAO_GESTOR, null, "QUARTO", 203);

        assertEquals(3, passagens.guardadas().size());
    }

    @Test
    void rececionistaPicaNaRececaoMasNaoNosQuartos() {
        controller.picar(CARTAO_RECECIONISTA, null, "RECECAO", null);

        assertThrows(AcessoRecusadoException.class, () -> controller.picar(CARTAO_RECECIONISTA, null, "QUARTO", 202));
        assertEquals(1, passagens.guardadas().size());
    }

    @Test
    void restauracaoSoPicaNaCozinhaENoRestaurante() {
        controller.picar(CARTAO_RESTAURACAO, null, "COZINHA", null);
        controller.picar(CARTAO_RESTAURACAO, null, "RESTAURANTE", null);

        assertThrows(AcessoRecusadoException.class, () -> controller.picar(CARTAO_RESTAURACAO, null, "PISCINA", null));
        assertThrows(AcessoRecusadoException.class, () -> controller.picar(CARTAO_RESTAURACAO, null, "RECECAO", null));
        assertEquals(2, passagens.guardadas().size());
    }

    // ---------------------------------------------------------------- cartão mestre

    @Test
    void cartaoMestrePicaEmTodosOsLocaisSemNif() {
        for (LocalHotel local : controller.listarLocais()) {
            controller.picar(CARTAO_MESTRE, null, local.getCodigo(), local.isQuarto() ? 203 : null);
        }

        assertEquals(8, passagens.guardadas().size());
    }

    // ---------------------------------------------------------------- entradas inválidas

    @Test
    void cartaoInexistenteELocalInvalidoSaoRecusados() {
        assertThrows(IllegalArgumentException.class, () -> controller.picar(555, NIF, "BAR", null));
        assertThrows(IllegalArgumentException.class, () -> controller.picar(CARTAO_MESTRE, null, "GARAGEM", null));
        assertThrows(IllegalArgumentException.class, () -> controller.picar(CARTAO_MESTRE, null, "", null));
        assertThrows(IllegalArgumentException.class, () -> controller.picar(CARTAO_MESTRE, null, null, null));
    }

    @Test
    void codigoDoLocalAceitaMinusculas() {
        Passagem passagem = controller.picar(CARTAO_MESTRE, null, " bar ", null);

        assertEquals("BAR", passagem.getLocal());
    }

    // ---------------------------------------------------------------- consulta (só gestor)

    @Test
    void soOGestorConsultaPorCartaoQuartoELocal() {
        controller.picar(CARTAO_CLIENTE, NIF, "QUARTO", 202);

        assertThrows(SecurityException.class, () -> controller.consultarCartao(CARTAO_CLIENTE));

        entrar(Perfil.RECECIONISTA);
        assertThrows(SecurityException.class, () -> controller.consultarCartao(CARTAO_CLIENTE));
        assertThrows(SecurityException.class, () -> controller.consultarQuarto(202));
        assertThrows(SecurityException.class, () -> controller.consultarLocal("QUARTO"));

        entrar(Perfil.EMPRESA_RESTAURACAO);
        assertThrows(SecurityException.class, () -> controller.consultarLocal("QUARTO"));

        entrar(Perfil.GESTOR);
        assertEquals(1, controller.consultarCartao(CARTAO_CLIENTE).size());
        assertEquals(1, controller.consultarQuarto(202).size());
        assertEquals(1, controller.consultarLocal("QUARTO").size());
    }

    @Test
    void gestorFiltraAsPassagensPorLocalCartaoEQuarto() {
        controller.picar(CARTAO_CLIENTE, NIF, "QUARTO", 202);
        controller.picar(CARTAO_RESTAURACAO, null, "COZINHA", null);
        controller.picar(CARTAO_MESTRE, null, "QUARTO", 203);
        controller.picar(CARTAO_MESTRE, null, "COZINHA", null);

        entrar(Perfil.GESTOR);

        assertEquals(2, controller.consultarLocal("COZINHA").size());
        assertEquals(2, controller.consultarLocal("QUARTO").size());
        assertEquals(2, controller.consultarCartao(CARTAO_MESTRE).size());
        assertEquals(1, controller.consultarQuarto(203).size());
        assertEquals(CARTAO_MESTRE, controller.consultarQuarto(203).get(0).getNumeroCartao());
        assertTrue(controller.consultarQuarto(999).isEmpty());
    }

    @Test
    void consultarLocalInexistenteEErro() {
        entrar(Perfil.GESTOR);

        assertThrows(IllegalArgumentException.class, () -> controller.consultarLocal("GARAGEM"));
    }

    @Test
    void gestorDesativadoJaNaoConsulta() {
        Utilizador gestor = Utilizador.novo("g", "abc123456", "Gestor", Perfil.GESTOR);
        gestor.setAtivo(false);
        sessao.iniciar(gestor);

        assertThrows(SecurityException.class, () -> controller.consultarCartao(CARTAO_CLIENTE));
    }

    // ---------------------------------------------------------------- auxiliares

    private void entrar(Perfil perfil) {
        sessao.iniciar(new Utilizador(99, "teste", "hash", "Teste", perfil, true));
    }

    /** Cria o funcionário, guarda-o e devolve o cartão que lhe pertence. */
    private static CartaoMagnetico cartaoDe(UtilizadorDAOMemoria utilizadores, String username, Perfil perfil, int numero) {
        Utilizador funcionario = utilizadores.inserir(new Utilizador(0, username, "hash", username, perfil, true));
        return CartaoMagnetico.paraFuncionario(numero, funcionario.getId());
    }
}