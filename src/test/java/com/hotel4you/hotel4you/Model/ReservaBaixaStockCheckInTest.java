package com.hotel4you.hotel4you.Model;

import com.hotel4you.hotel4you.Model.enums.EstadoReserva;
import com.hotel4you.hotel4you.Model.enums.Perfil;
import com.hotel4you.hotel4you.Model.enums.TipoCobranca;
import com.hotel4you.hotel4you.Model.enums.TipoMovimento;
import com.hotel4you.hotel4you.Model.enums.TipoQuarto;
import com.hotel4you.hotel4you.Model.enums.TipoServico;
import com.hotel4you.hotel4you.Model.enums.Vista;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * S7-01: o stock só baixa no check-in (não na criação da reserva). A baixa inclui os equipamentos
 * configurados do quarto, os produtos escolhidos na reserva e os produtos dos serviços ligados ao stock
 * (bebidas). Na edição, alterar quantidades, produtos ou serviços ajusta o stock; no check-out os
 * produtos regressam ao stock, exceto os consumíveis. As regras estão no model (Reserva, Servico, Artigo).
 */
class ReservaBaixaStockCheckInTest {

    private final LocalDateTime agora = LocalDateTime.of(2026, 11, 27, 15, 0);
    private final Utilizador rececionista = Utilizador.novo("rececao", "rececao123", "Ana", Perfil.RECECIONISTA);

    private Artigo toalhas;
    private Artigo agua;
    private List<Artigo> stock;
    private Quarto quarto;
    private Servico aguaExtra;
    private Servico lavandaria;

    @BeforeEach
    void preparar() {
        toalhas = new Artigo(1, "Toalha de banho", "un", 10);
        agua = new Artigo(2, "Água 50cl", "un", 6);
        agua.setConsumivel(true);
        stock = List.of(toalhas, agua);

        quarto = new Quarto(1, 205, 2, "", TipoQuarto.DUPLO, new BigDecimal("80.00"), Vista.NENHUMA, true);
        quarto.definirProduto(new ProdutoQuarto(toalhas, 2));

        aguaExtra = new Servico(1, TipoServico.EXTRA, "Água do minibar", "Bebidas", "", new BigDecimal("2.00"),
                TipoCobranca.POR_UNIDADE, true);
        aguaExtra.ligarProduto(agua, 1);
        lavandaria = new Servico(2, TipoServico.EXTRA, "Lavandaria", "Lavandaria", "", new BigDecimal("10.00"),
                TipoCobranca.POR_UNIDADE, true);
    }

    /** Reserva pendente com 1 toalha extra escolhida e 2 águas do minibar. */
    private Reserva reservaComExtras() {
        Cliente cliente = new Cliente(1, "Maria Silva", "PT123456", "910000000");
        return new Reserva(7, cliente, quarto, LocalDate.of(2026, 11, 27), LocalDate.of(2026, 11, 29),
                List.of(new ConsumoServico(aguaExtra, 2, LocalDate.of(2026, 11, 27))),
                List.of(new ProdutoReserva(toalhas, 1)));
    }

    private static CartaoMagnetico cartao() {
        return new CartaoMagnetico(1, 1001, false, true);
    }

    // ---- O stock só baixa no check-in ----

    @Test
    void naCriacaoDaReservaOStockNaoBaixa() {
        Reserva reserva = reservaComExtras();

        assertEquals(EstadoReserva.PENDENTE, reserva.getEstado());
        assertFalse(reserva.usaStock());
        assertEquals(0, reserva.quantidadeUsada(toalhas));
        assertEquals(1, reserva.quantidadeNaReserva(toalhas), "a toalha extra fica escolhida");
        assertEquals(10, toalhas.getQuantidadeDisponivel());
        assertEquals(6, agua.getQuantidadeDisponivel());
    }

    @Test
    void noCheckInBaixamOsEquipamentosDoQuartoEOsExtrasDaReserva() {
        Reserva reserva = reservaComExtras();

        reserva.fazerCheckIn(cartao(), agora, stock);

        assertTrue(reserva.usaStock());
        assertEquals(3, reserva.quantidadeUsada(toalhas), "2 do quarto + 1 extra");
        assertEquals(2, reserva.quantidadeUsada(agua), "2 águas do minibar (serviço ligado ao stock)");
        assertEquals(2, reserva.getProdutos().size(), "uma linha por produto");
        assertEquals(7, toalhas.getQuantidadeDisponivel());
        assertEquals(4, agua.getQuantidadeDisponivel());
        assertEquals(10, toalhas.getQuantidadeAtual(), "o stock real não muda no check-in");
        assertEquals(6, agua.getQuantidadeAtual());
    }

    @Test
    void servicoSemProdutoNaoMexeNoStock() {
        Cliente cliente = new Cliente(1, "Maria Silva", "PT123456", "910000000");
        Reserva reserva = new Reserva(7, cliente, quarto, LocalDate.of(2026, 11, 27), LocalDate.of(2026, 11, 29),
                List.of(new ConsumoServico(lavandaria, 1, LocalDate.of(2026, 11, 27))), List.of());

        reserva.fazerCheckIn(cartao(), agora, stock);

        assertEquals(1, reserva.getProdutos().size(), "só as toalhas do quarto");
        assertEquals(6, agua.getQuantidadeDisponivel());
    }

    @Test
    void semStockDeUmExtraOCheckInNaoAconteceENadaBaixa() {
        agua.setQuantidadeReservada(5);   // só resta 1 água disponível e a reserva tem 2
        Reserva reserva = reservaComExtras();

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> reserva.fazerCheckIn(cartao(), agora, stock));

        assertTrue(erro.getMessage().contains("Água"));
        assertEquals(EstadoReserva.PENDENTE, reserva.getEstado());
        assertEquals(10, toalhas.getQuantidadeDisponivel(), "as toalhas também não tiveram baixa");
        assertEquals(1, reserva.getProdutos().size(), "a reserva continua só com a toalha escolhida");
    }

    // ---- Edição da reserva ajusta o stock ----

    @Test
    void naEdicaoComCheckInAlterarQuantidadesAjustaOStock() {
        Reserva reserva = reservaComExtras();
        reserva.fazerCheckIn(cartao(), agora, stock);

        reserva.alterarProduto(toalhas, 1);

        assertEquals(1, reserva.quantidadeUsada(toalhas));
        assertEquals(9, toalhas.getQuantidadeDisponivel());
    }

    @Test
    void naEdicaoComCheckInAcrescentarUmaBebidaDaBaixa() {
        Reserva reserva = reservaComExtras();
        reserva.fazerCheckIn(cartao(), agora, stock);

        reserva.adicionarServicoEdicao(new ConsumoServico(aguaExtra, 3, LocalDate.of(2026, 11, 28)), stock);

        assertEquals(5, reserva.quantidadeUsada(agua));
        assertEquals(1, agua.getQuantidadeDisponivel());
        assertEquals(2, reserva.getConsumos().size());
    }

    @Test
    void acrescentarUmaBebidaSemStockNaoMudaAReserva() {
        Reserva reserva = reservaComExtras();
        reserva.fazerCheckIn(cartao(), agora, stock);

        assertThrows(IllegalStateException.class, () -> reserva.adicionarServicoEdicao(
                new ConsumoServico(aguaExtra, 5, LocalDate.of(2026, 11, 28)), stock));

        assertEquals(1, reserva.getConsumos().size());
        assertEquals(2, reserva.quantidadeUsada(agua));
        assertEquals(4, agua.getQuantidadeDisponivel());
    }

    @Test
    void naEdicaoRemoverUmaBebidaDevolveAoStock() {
        Reserva reserva = reservaComExtras();
        reserva.fazerCheckIn(cartao(), agora, stock);

        reserva.removerServicoEdicao(0, stock);

        assertTrue(reserva.getConsumos().isEmpty());
        assertEquals(0, reserva.quantidadeUsada(agua));
        assertEquals(6, agua.getQuantidadeDisponivel());
        assertEquals(1, reserva.getProdutos().size(), "a linha da água sai da reserva");
    }

    // ---- Check-out: regressam ao stock, exceto os consumíveis ----

    @Test
    void noCheckOutOsProdutosRegressamExcetoOsConsumiveis() {
        Reserva reserva = reservaComExtras();
        reserva.fazerCheckIn(cartao(), agora, stock);

        List<MovimentoStock> consumos = reserva.fazerCheckOut(agora.plusDays(2), stock, rececionista);

        assertEquals(10, toalhas.getQuantidadeDisponivel(), "as toalhas voltam ao stock");
        assertEquals(10, toalhas.getQuantidadeAtual());
        assertEquals(4, agua.getQuantidadeAtual(), "as águas bebidas saem do stock real");
        assertEquals(4, agua.getQuantidadeDisponivel());
        assertEquals(1, consumos.size());
        assertEquals(TipoMovimento.UTILIZACAO, consumos.get(0).getTipo());
        assertEquals(2, consumos.get(0).getQuantidade());
        assertEquals(0, reserva.quantidadeUsada(agua));
    }

    // ---- Serviços ligados ao stock ----

    @Test
    void servicoLigadoAoStockDaOProdutoVezesAQuantidade() {
        aguaExtra.ligarProduto(agua, 2);   // ex.: pack de 2 garrafas

        ProdutoReserva gasto = new ConsumoServico(aguaExtra, 3, LocalDate.of(2026, 11, 27)).produtoDoStock();

        assertEquals(agua, gasto.getArtigo());
        assertEquals(6, gasto.getQuantidade());
        assertNull(new ConsumoServico(lavandaria, 1, LocalDate.of(2026, 11, 27)).produtoDoStock());
    }

    @Test
    void ligarProdutoValidaOsDados() {
        assertThrows(IllegalArgumentException.class, () -> lavandaria.ligarProduto(null, 1));
        assertThrows(IllegalArgumentException.class, () -> lavandaria.ligarProduto(agua, 0));
        assertFalse(lavandaria.temProduto());

        aguaExtra.desligarProduto();
        assertFalse(aguaExtra.temProduto());
        assertEquals(0, aguaExtra.getQuantidadeProduto());
    }
}
