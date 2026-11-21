package com.hotel4you.hotel4you.Model;

import com.hotel4you.hotel4you.Model.enums.EstadoReserva;
import com.hotel4you.hotel4you.Model.enums.Perfil;
import com.hotel4you.hotel4you.Model.enums.TipoMovimento;
import com.hotel4you.hotel4you.Model.enums.TipoQuarto;
import com.hotel4you.hotel4you.Model.enums.Vista;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * S6-05: os equipamentos do quarto ligados ao stock dão baixa no stock disponível no check-in e
 * regressam no check-out (exceto os consumíveis). A regra está no model (Reserva e Artigo).
 */
class ReservaStockQuartoTest {

    private final LocalDateTime agora = LocalDateTime.of(2026, 11, 21, 15, 0);
    private final Utilizador rececionista = Utilizador.novo("rececao", "rececao123", "Ana", Perfil.RECECIONISTA);

    private Artigo toalhas;
    private Artigo champo;
    private Artigo tv;
    private List<Artigo> stock;
    private Quarto quartoDuplo;

    @BeforeEach
    void preparar() {
        toalhas = new Artigo(1, "Toalha de banho", "un", 10);
        champo = new Artigo(2, "Champô", "un", 10);
        champo.setConsumivel(true);
        tv = new Artigo(3, "Televisão", "un", 2);
        stock = List.of(toalhas, champo, tv);

        quartoDuplo = new Quarto(1, 205, 2, "", TipoQuarto.DUPLO, new BigDecimal("80.00"), Vista.NENHUMA, true);
        quartoDuplo.definirProduto(new ProdutoQuarto(toalhas, 2));
        quartoDuplo.definirProduto(new ProdutoQuarto(champo, 2));
        quartoDuplo.definirProduto(new ProdutoQuarto(tv, 1));
    }

    private Reserva reserva(int id) {
        Cliente cliente = new Cliente(1, "Maria Silva", "PT123456", "910000000");
        return new Reserva(id, cliente, quartoDuplo, LocalDate.of(2026, 11, 21), LocalDate.of(2026, 11, 23));
    }

    private static CartaoMagnetico cartao() {
        return new CartaoMagnetico(1, 1001, false, true);
    }

    // ---- Equipamentos do quarto ----

    @Test
    void quartoGuardaOsProdutosDoStockComAQuantidade() {
        assertEquals(3, quartoDuplo.getProdutos().size());

        quartoDuplo.definirProduto(new ProdutoQuarto(toalhas, 3));   // muda a quantidade, não repete
        assertEquals(3, quartoDuplo.getProdutos().size());

        assertTrue(quartoDuplo.removerProduto(tv.getId()));
        assertEquals(2, quartoDuplo.getProdutos().size());
        assertThrows(IllegalArgumentException.class, () -> new ProdutoQuarto(toalhas, 0));
    }

    // ---- Check-in: baixa no stock disponível ----

    @Test
    void noCheckInOsProdutosDoQuartoDaoBaixaNoStockDisponivel() {
        Reserva reserva = reserva(7);

        reserva.fazerCheckIn(cartao(), agora, stock);

        assertEquals(EstadoReserva.CHECK_IN, reserva.getEstado());
        assertEquals(3, reserva.getProdutos().size(), "os produtos do quarto passam para a reserva");
        assertEquals(2, reserva.quantidadeUsada(toalhas));
        assertEquals(8, toalhas.getQuantidadeDisponivel());
        assertEquals(8, champo.getQuantidadeDisponivel());
        assertEquals(1, tv.getQuantidadeDisponivel());
        assertEquals(10, toalhas.getQuantidadeAtual(), "o stock real não muda no check-in");
    }

    @Test
    void checkInJuntaOsProdutosDoQuartoAosQueAReservaJaTinha() {
        Cliente cliente = new Cliente(1, "Maria Silva", "PT123456", "910000000");
        Reserva reserva = new Reserva(7, cliente, quartoDuplo, LocalDate.of(2026, 11, 21), LocalDate.of(2026, 11, 23),
                List.of(), List.of(new ProdutoReserva(toalhas, 1)));
        toalhas.setQuantidadeReservada(1);   // a toalha extra já estava em uso (reserva pendente)

        reserva.fazerCheckIn(cartao(), agora, stock);

        assertEquals(3, reserva.quantidadeUsada(toalhas), "1 extra + 2 do quarto");
        assertEquals(3, reserva.getProdutos().size(), "uma linha por produto");
        assertEquals(7, toalhas.getQuantidadeDisponivel());
    }

    @Test
    void semStockDisponivelOCheckInNaoAconteceENadaMuda() {
        tv.setQuantidadeReservada(2);   // as 2 televisões já estão em uso noutros quartos
        Reserva reserva = reserva(7);

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> reserva.fazerCheckIn(cartao(), agora, stock));

        assertTrue(erro.getMessage().contains("Televisão"));
        assertEquals(EstadoReserva.PENDENTE, reserva.getEstado());
        assertTrue(reserva.getProdutos().isEmpty());
        assertEquals(10, toalhas.getQuantidadeDisponivel(), "nenhum produto teve baixa");
        assertEquals(10, champo.getQuantidadeDisponivel());
    }

    @Test
    void produtoDoQuartoQueJaNaoExisteEmStockImpedeOCheckIn() {
        Reserva reserva = reserva(7);

        assertThrows(IllegalArgumentException.class, () -> reserva.fazerCheckIn(cartao(), agora, List.of(toalhas)));
        assertEquals(EstadoReserva.PENDENTE, reserva.getEstado());
    }

    // ---- Check-out: reposição (exceto consumíveis) ----

    @Test
    void noCheckOutOsProdutosRegressamExcetoOsConsumiveis() {
        Reserva reserva = reserva(7);
        reserva.fazerCheckIn(cartao(), agora, stock);

        List<MovimentoStock> consumos = reserva.fazerCheckOut(agora.plusDays(2), stock, rececionista);

        assertEquals(EstadoReserva.CHECK_OUT, reserva.getEstado());
        assertEquals(10, toalhas.getQuantidadeAtual());
        assertEquals(10, toalhas.getQuantidadeDisponivel(), "as toalhas voltam ao stock");
        assertEquals(2, tv.getQuantidadeDisponivel(), "a televisão volta ao stock");
        assertEquals(8, champo.getQuantidadeAtual(), "o champô gasto sai do stock real");
        assertEquals(8, champo.getQuantidadeDisponivel());

        assertEquals(1, consumos.size(), "só o consumível fica como consumo");
        MovimentoStock consumo = consumos.get(0);
        assertEquals(TipoMovimento.UTILIZACAO, consumo.getTipo());
        assertEquals(champo, consumo.getArtigo());
        assertEquals(2, consumo.getQuantidade());
        assertEquals(rececionista, consumo.getUtilizador());
        assertTrue(consumo.getJustificacao().contains("reserva n.º 7"));
    }

    @Test
    void depoisDoCheckOutAReservaJaNaoUsaStock() {
        Reserva reserva = reserva(7);
        reserva.fazerCheckIn(cartao(), agora, stock);
        reserva.fazerCheckOut(agora.plusDays(2), stock, rececionista);

        assertEquals(0, reserva.quantidadeUsada(toalhas));
        assertTrue(reserva.getProdutos().size() == 3, "os produtos ficam no histórico da reserva");
    }

    @Test
    void reservaCanceladaNaoUsaStock() {
        Cliente cliente = new Cliente(1, "Maria Silva", "PT123456", "910000000");
        Reserva reserva = new Reserva(7, cliente, quartoDuplo, LocalDate.of(2026, 11, 21), LocalDate.of(2026, 11, 23),
                List.of(), List.of(new ProdutoReserva(toalhas, 2)));
        assertEquals(2, reserva.quantidadeUsada(toalhas), "uma reserva pendente já tem os produtos em uso");

        reserva.cancelar();

        assertEquals(0, reserva.quantidadeUsada(toalhas));
    }

    // ---- Edição da reserva: alterar a quantidade ajusta o stock ----

    @Test
    void umaPessoaNumQuartoDuploFicaComMenosToalhas() {
        Reserva reserva = reserva(7);
        reserva.fazerCheckIn(cartao(), agora, stock);
        assertEquals(8, toalhas.getQuantidadeDisponivel());

        reserva.alterarProduto(toalhas, 1);

        assertEquals(1, reserva.quantidadeUsada(toalhas));
        assertEquals(9, toalhas.getQuantidadeDisponivel(), "a toalha que sobra volta ao disponível");
        assertEquals(10, toalhas.getQuantidadeAtual(), "não foi gasta");
    }

    @Test
    void aumentarAQuantidadeDaBaixaDaDiferenca() {
        Reserva reserva = reserva(7);
        reserva.fazerCheckIn(cartao(), agora, stock);

        reserva.alterarProduto(champo, 5);

        assertEquals(5, reserva.quantidadeUsada(champo));
        assertEquals(5, champo.getQuantidadeDisponivel());
    }

    @Test
    void aumentarSemStockDisponivelNaoAlteraAReserva() {
        Reserva reserva = reserva(7);
        reserva.fazerCheckIn(cartao(), agora, stock);

        assertThrows(IllegalStateException.class, () -> reserva.alterarProduto(tv, 3));
        assertEquals(1, reserva.quantidadeUsada(tv));
        assertEquals(1, tv.getQuantidadeDisponivel());
    }

    @Test
    void quantidadeZeroRetiraOProdutoDaReserva() {
        Reserva reserva = reserva(7);
        reserva.fazerCheckIn(cartao(), agora, stock);

        reserva.alterarProduto(tv, 0);

        assertEquals(0, reserva.quantidadeUsada(tv));
        assertEquals(2, reserva.getProdutos().size());
        assertEquals(2, tv.getQuantidadeDisponivel());
    }

    @Test
    void naReservaPendenteTambemSeAlteramOsProdutos() {
        Reserva reserva = reserva(7);

        reserva.alterarProduto(toalhas, 1);

        assertEquals(1, reserva.quantidadeUsada(toalhas));
        assertEquals(9, toalhas.getQuantidadeDisponivel());
    }

    @Test
    void depoisDoCheckOutOsProdutosNaoSeAlteram() {
        Reserva reserva = reserva(7);
        reserva.fazerCheckIn(cartao(), agora, stock);
        reserva.fazerCheckOut(agora.plusDays(2), stock, rececionista);

        assertThrows(IllegalStateException.class, () -> reserva.alterarProduto(toalhas, 1));
        assertThrows(IllegalArgumentException.class, () -> reserva(8).alterarProduto(toalhas, -1));
    }
}
