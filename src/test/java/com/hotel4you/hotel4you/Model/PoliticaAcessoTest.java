package com.hotel4you.hotel4you.Model;

import com.hotel4you.hotel4you.DAO.PassagemDAOMemoria;
import com.hotel4you.hotel4you.Model.enums.Perfil;
import com.hotel4you.hotel4you.Model.enums.TipoQuarto;
import com.hotel4you.hotel4you.Model.enums.Vista;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes unitários das regras de acesso por perfil e por local (S6-06): cartão mestre, cartões de
 * funcionário (gestor, rececionista, restauração, fornecedor) e cartões de cliente.
 */
class PoliticaAcessoTest {

    private static final Set<String> TODOS = Set.of(
            LocalHotel.QUARTO, LocalHotel.RESTAURANTE, LocalHotel.PISCINA, LocalHotel.COZINHA,
            LocalHotel.RECECAO, LocalHotel.BAR, LocalHotel.CONFERENCIAS, LocalHotel.ARMAZEM);

    private Quarto quarto202;
    private Quarto quarto301;
    private CartaoMagnetico cartaoFuncionario;

    @BeforeEach
    void preparar() {
        quarto202 = quarto(1, 202, 2);
        quarto301 = quarto(2, 301, 3);
        cartaoFuncionario = CartaoMagnetico.paraFuncionario(201, 1);
    }

    // ---------------------------------------------------------------- cartão mestre

    @Test
    void cartaoMestreAcedeATodosOsLocais() {
        CartaoMagnetico mestre = new CartaoMagnetico(1, 1, true, true);

        for (LocalHotel local : PassagemDAOMemoria.locaisPadrao()) {
            assertTrue(PoliticaAcesso.paraMestre(mestre, local).permitido(), local.getCodigo());
        }
    }

    @Test
    void cartaoMestreDesativadoNaoAbreNada() {
        CartaoMagnetico mestre = new CartaoMagnetico(1, 1, true, false);

        assertFalse(PoliticaAcesso.paraMestre(mestre, local(LocalHotel.RECECAO)).permitido());
    }

    @Test
    void cartaoQueNaoEMestreNaoPassaComoMestre() {
        CartaoMagnetico normal = new CartaoMagnetico(1, 1001, false, true);

        assertFalse(PoliticaAcesso.paraMestre(normal, local(LocalHotel.RECECAO)).permitido());
    }

    // ---------------------------------------------------------------- funcionários

    @Test
    void gestorAcedeATodosOsLocaisEAQualquerQuarto() {
        Utilizador gestor = funcionario(Perfil.GESTOR);

        assertEquals(TODOS, locaisPermitidos(gestor));
        assertTrue(PoliticaAcesso.paraFuncionario(cartaoFuncionario, gestor, local(LocalHotel.QUARTO), quarto301).permitido());
    }

    @Test
    void rececionistaSoAcedeAosPisosZeroEUm() {
        Set<String> esperados = Set.of(LocalHotel.RECECAO, LocalHotel.BAR, LocalHotel.CONFERENCIAS,
                LocalHotel.RESTAURANTE, LocalHotel.PISCINA, LocalHotel.COZINHA);

        assertEquals(esperados, locaisPermitidos(funcionario(Perfil.RECECIONISTA)));
    }

    @Test
    void rececionistaNaoEntraNosQuartosDeDormidas() {
        Utilizador rececionista = funcionario(Perfil.RECECIONISTA);

        assertFalse(PoliticaAcesso.paraFuncionario(cartaoFuncionario, rececionista, local(LocalHotel.QUARTO), quarto202).permitido());
        assertFalse(PoliticaAcesso.paraFuncionario(cartaoFuncionario, rececionista, local(LocalHotel.QUARTO), quarto301).permitido());
    }

    @Test
    void restauracaoSoAcedeACozinhaERestaurante() {
        Utilizador restauracao = funcionario(Perfil.EMPRESA_RESTAURACAO);

        assertEquals(Set.of(LocalHotel.COZINHA, LocalHotel.RESTAURANTE), locaisPermitidos(restauracao));
    }

    @Test
    void restauracaoNaoAcedeAPiscinaMesmoSendoNoMesmoPiso() {
        Utilizador restauracao = funcionario(Perfil.EMPRESA_RESTAURACAO);

        PoliticaAcesso.Decisao decisao = PoliticaAcesso.paraFuncionario(
                cartaoFuncionario, restauracao, local(LocalHotel.PISCINA), null);

        assertFalse(decisao.permitido());
        assertEquals("A restauração só acede à cozinha e ao restaurante.", decisao.motivo());
    }

    @Test
    void fornecedorSoAcedeAoArmazem() {
        assertEquals(Set.of(LocalHotel.ARMAZEM), locaisPermitidos(funcionario(Perfil.FORNECEDOR)));
    }

    @Test
    void cartaoDeFuncionarioDesativadoNaoAbreNada() {
        CartaoMagnetico desativado = new CartaoMagnetico(5, 201, false, false, 1);

        assertFalse(PoliticaAcesso.paraFuncionario(desativado, funcionario(Perfil.GESTOR), local(LocalHotel.RECECAO), null).permitido());
    }

    @Test
    void funcionarioDesativadoNaoAcedeANada() {
        Utilizador desativado = funcionario(Perfil.GESTOR);
        desativado.setAtivo(false);

        assertFalse(PoliticaAcesso.paraFuncionario(cartaoFuncionario, desativado, local(LocalHotel.RECECAO), null).permitido());
    }

    @Test
    void funcionarioPrecisaDoQuartoParaEntrarNumQuarto() {
        PoliticaAcesso.Decisao decisao = PoliticaAcesso.paraFuncionario(
                cartaoFuncionario, funcionario(Perfil.GESTOR), local(LocalHotel.QUARTO), null);

        assertFalse(decisao.permitido());
    }

    // ---------------------------------------------------------------- clientes

    @Test
    void clienteEntraNoQuartoDaReserva() {
        CartaoMagnetico cartao = new CartaoMagnetico(1, 1001, false, true);
        Reserva reserva = reservaEmCheckIn(cartao, quarto202);

        assertTrue(PoliticaAcesso.paraCliente(cartao, reserva, local(LocalHotel.QUARTO), quarto202).permitido());
    }

    @Test
    void clienteNaoEntraNoutroQuarto() {
        CartaoMagnetico cartao = new CartaoMagnetico(1, 1001, false, true);
        Reserva reserva = reservaEmCheckIn(cartao, quarto202);

        PoliticaAcesso.Decisao decisao = PoliticaAcesso.paraCliente(cartao, reserva, local(LocalHotel.QUARTO), quarto301);

        assertFalse(decisao.permitido());
        assertEquals("O cartão só permite entrar no quarto da reserva.", decisao.motivo());
    }

    @Test
    void clienteNuncaEntraNaCozinha() {
        CartaoMagnetico cartao = new CartaoMagnetico(1, 1001, false, true);
        Reserva reserva = reservaEmCheckIn(cartao, quarto202);

        assertFalse(PoliticaAcesso.paraCliente(cartao, reserva, local(LocalHotel.COZINHA), null).permitido());
    }

    @Test
    void clienteNaoAcedeAosPisosZeroEMenosUm() {
        CartaoMagnetico cartao = new CartaoMagnetico(1, 1001, false, true);
        Reserva reserva = reservaEmCheckIn(cartao, quarto202);

        for (String codigo : List.of(LocalHotel.RECECAO, LocalHotel.BAR, LocalHotel.CONFERENCIAS, LocalHotel.ARMAZEM)) {
            assertFalse(PoliticaAcesso.paraCliente(cartao, reserva, local(codigo), null).permitido(), codigo);
        }
    }

    @Test
    void restauranteESoPermitidoSeOServicoEstiverNaReserva() {
        CartaoMagnetico cartao = new CartaoMagnetico(1, 1001, false, true);
        Reserva reserva = reservaEmCheckIn(cartao, quarto202);

        assertFalse(PoliticaAcesso.paraCliente(cartao, reserva, local(LocalHotel.RESTAURANTE), null).permitido());

        reserva.adicionarConsumo(consumo("Jantar no restaurante"));

        assertTrue(PoliticaAcesso.paraCliente(cartao, reserva, local(LocalHotel.RESTAURANTE), null).permitido());
    }

    @Test
    void servicoDeUmLocalNaoAbreOOutro() {
        CartaoMagnetico cartao = new CartaoMagnetico(1, 1001, false, true);
        Reserva reserva = reservaEmCheckIn(cartao, quarto202);
        reserva.adicionarConsumo(consumo("Acesso à piscina"));

        assertTrue(PoliticaAcesso.paraCliente(cartao, reserva, local(LocalHotel.PISCINA), null).permitido());
        assertFalse(PoliticaAcesso.paraCliente(cartao, reserva, local(LocalHotel.RESTAURANTE), null).permitido());
    }

    @Test
    void clienteSemReservaEmCheckInNaoAcedeANada() {
        CartaoMagnetico cartao = new CartaoMagnetico(1, 1001, false, true);
        Reserva pendente = new Reserva(1, new Cliente(1, "Maria", "123456789", ""), quarto202,
                LocalDate.now(), LocalDate.now().plusDays(2));

        assertFalse(PoliticaAcesso.paraCliente(cartao, null, local(LocalHotel.QUARTO), quarto202).permitido());
        assertFalse(PoliticaAcesso.paraCliente(cartao, pendente, local(LocalHotel.QUARTO), quarto202).permitido());
    }

    @Test
    void cartaoDeClienteDesativadoNaoAbreNada() {
        CartaoMagnetico cartao = new CartaoMagnetico(1, 1001, false, true);
        Reserva reserva = reservaEmCheckIn(cartao, quarto202);
        cartao.desativar();

        assertFalse(PoliticaAcesso.paraCliente(cartao, reserva, local(LocalHotel.QUARTO), quarto202).permitido());
    }

    // ---------------------------------------------------------------- auxiliares

    /** Códigos dos locais a que o funcionário acede (o quarto 202, quando o local é o quarto). */
    private Set<String> locaisPermitidos(Utilizador funcionario) {
        Set<String> permitidos = new java.util.HashSet<>();
        for (LocalHotel local : PassagemDAOMemoria.locaisPadrao()) {
            if (PoliticaAcesso.paraFuncionario(cartaoFuncionario, funcionario, local, quarto202).permitido()) {
                permitidos.add(local.getCodigo());
            }
        }
        return permitidos;
    }

    private static LocalHotel local(String codigo) {
        return PassagemDAOMemoria.local(codigo);
    }

    private static Utilizador funcionario(Perfil perfil) {
        return new Utilizador(1, "func", "hash", "Funcionário", perfil, true);
    }

    private static Quarto quarto(int id, int numero, int piso) {
        return new Quarto(id, numero, piso, "", TipoQuarto.DUPLO, new BigDecimal("80.00"), Vista.MAR, true);
    }

    private static Reserva reservaEmCheckIn(CartaoMagnetico cartao, Quarto quarto) {
        Reserva reserva = new Reserva(1, new Cliente(1, "Maria", "123456789", ""), quarto,
                LocalDate.now(), LocalDate.now().plusDays(2));
        reserva.fazerCheckIn(cartao);
        return reserva;
    }

    private static ConsumoServico consumo(String nomeDoServico) {
        return new ConsumoServico(new Servico(1, nomeDoServico, "", new BigDecimal("5.00")), 1, LocalDate.now());
    }
}