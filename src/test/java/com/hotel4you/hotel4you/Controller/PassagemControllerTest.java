
package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.*;
import com.hotel4you.hotel4you.Model.*;
import com.hotel4you.hotel4you.Model.enums.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class PassagemControllerTest {

    private Sessao sessao;
    private PassagemController controller;
    private List<Passagem> guardadas;
    private CartaoMagnetico cartao;

    @BeforeEach
    void preparar() {

        sessao = new Sessao();
        guardadas = new ArrayList<>();

        ReservaDAOMemoria reservas =
                new ReservaDAOMemoria();

        cartao = new CartaoMagnetico(
                1, 1001, false, true
        );

        CartaoMagnetico mestre =
                new CartaoMagnetico(
                        2, 99, true, true
                );

        Quarto quarto = new Quarto(
                1, 202, 2, "",
                TipoQuarto.DUPLO,
                new BigDecimal("80.00"),
                Vista.MAR,
                true
        );

        Cliente cliente = new Cliente(
                1, "Maria", "PT123456", "910000000"
        );

        Reserva reserva = reservas.inserir(
                new Reserva(
                        0,
                        cliente,
                        quarto,
                        LocalDate.now(),
                        LocalDate.now().plusDays(2)
                )
        );

        reserva.fazerCheckIn(cartao);
        reservas.atualizar(reserva);

        CartaoDAO cartoes = new CartaoDAO() {

            public CartaoMagnetico inserir(
                    CartaoMagnetico c) {
                return c;
            }

            public void atualizar(CartaoMagnetico c) {
            }

            public void remover(int id) {
            }

            public Optional<CartaoMagnetico> procurarPorId(
                    int id) {

                if (id == 1) {
                    return Optional.of(cartao);
                }

                if (id == 2) {
                    return Optional.of(mestre);
                }

                return Optional.empty();
            }

            public Optional<CartaoMagnetico> procurarPorNumero(
                    int numero) {

                if (numero == 1001) {
                    return Optional.of(cartao);
                }

                if (numero == 99) {
                    return Optional.of(mestre);
                }

                return Optional.empty();
            }

            public List<CartaoMagnetico> listarTodos() {
                return List.of(cartao, mestre);
            }

            public int proximoNumero() {
                return 1002;
            }
        };

        PassagemDAO passagens = new PassagemDAO() {

            public void inserir(Passagem p) {
                guardadas.add(p);
            }

            public List<Passagem> listarPorCartao(int n) {
                return guardadas;
            }

            public List<Passagem> listarPorQuarto(int n) {
                return guardadas;
            }
        };

        QuartoDAO quartos = new QuartoDAO() {

            public Quarto inserir(Quarto q) {
                return q;
            }

            public void atualizar(Quarto q) {
            }

            public void remover(int id) {
            }

            public Optional<Quarto> procurarPorId(int id) {
                return Optional.of(quarto);
            }

            public Optional<Quarto> procurarPorNumero(
                    int numero) {

                if (numero == 202) {
                    return Optional.of(quarto);
                }

                return Optional.empty();
            }

            public List<Quarto> listarTodos() {
                return List.of(quarto);
            }

            public List<Quarto> listarAtivos() {
                return List.of(quarto);
            }

            public List<Quarto> listarDisponiveis(
                    TipoQuarto tipo,
                    LocalDate entrada,
                    LocalDate saida) {

                return List.of(quarto);
            }
        };

        controller = new PassagemController(
                passagens,
                cartoes,
                reservas,
                quartos,
                sessao
        );
    }

    private void entrar(Perfil perfil) {

        sessao.iniciar(
                Utilizador.novo(
                        "teste",
                        "abc123456",
                        "Teste",
                        perfil
                )
        );
    }

    @Test
    void rececionistaRegistaQuartoDaReserva() {

        entrar(Perfil.RECECIONISTA);

        controller.registar(1001, "QUARTO", 202);

        assertEquals(1, guardadas.size());
        assertNotNull(guardadas.get(0).getDataHora());
    }

    @Test
    void quartoErradoNaoRegistaPassagem() {

        entrar(Perfil.RECECIONISTA);

        assertThrows(
                IllegalArgumentException.class,
                () -> controller.registar(
                        1001, "QUARTO", 203
                )
        );

        assertTrue(guardadas.isEmpty());
    }

    @Test
    void restauranteSemServicoNaoPermiteAcesso() {

        entrar(Perfil.RECECIONISTA);

        assertThrows(
                IllegalArgumentException.class,
                () -> controller.registar(
                        1001, "RESTAURANTE", null
                )
        );
    }

    @Test
    void restauracaoNaoRegistaPiscina() {

        entrar(Perfil.EMPRESA_RESTAURACAO);

        assertThrows(
                SecurityException.class,
                () -> controller.registar(
                        99, "PISCINA", null
                )
        );
    }

    @Test
    void restauracaoRegistaCartaoMestreNoRestaurante() {

        entrar(Perfil.EMPRESA_RESTAURACAO);

        controller.registar(
                99, "RESTAURANTE", null
        );

        assertEquals(1, guardadas.size());
    }

    @Test
    void soGestorConsultaRegistos() {

        entrar(Perfil.RECECIONISTA);

        assertThrows(
                SecurityException.class,
                () -> controller.consultarCartao(1001)
        );

        entrar(Perfil.GESTOR);

        assertNotNull(
                controller.consultarCartao(1001)
        );

        assertNotNull(
                controller.consultarQuarto(202)
        );
    }
}
