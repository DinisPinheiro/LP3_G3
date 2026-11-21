package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.EquipamentoDAOMemoria;
import com.hotel4you.hotel4you.DAO.QuartoDAOMemoria;
import com.hotel4you.hotel4you.DAO.ReservaDAOMemoria;
import com.hotel4you.hotel4you.DAO.ServicoDAOMemoria;
import com.hotel4you.hotel4you.DAO.StockDAOMemoria;
import com.hotel4you.hotel4you.Model.Artigo;
import com.hotel4you.hotel4you.Model.Cliente;
import com.hotel4you.hotel4you.Model.Fornecedor;
import com.hotel4you.hotel4you.Model.MovimentoStock;
import com.hotel4you.hotel4you.Model.ProdutoQuarto;
import com.hotel4you.hotel4you.Model.Quarto;
import com.hotel4you.hotel4you.Model.Reserva;
import com.hotel4you.hotel4you.Model.Utilizador;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * S6-04 (saída de stock e stock real/disponível) e S6-05 (equipamentos do quarto ligados ao stock),
 * nos controllers, com DAO em memória. O stock em uso é calculado a partir das reservas, como o DAO real
 * faz com a tabela ReservaProduto (só reservas pendentes e com check-in).
 */
class StockSaidasEquipamentosControllerTest {

    private Sessao sessao;
    private Utilizador gestor;
    private ReservaDAOMemoria reservaDAO;
    private StockDAOMemoria stockDAO;
    private QuartoDAOMemoria quartoDAO;
    private CheckInCheckOutControllerTest.CartaoDAOEmMemoria cartaoDAO;
    private GestaoStockController stock;
    private CheckInCheckOutController checkInOut;
    private EdicaoReservaController edicao;
    private GestaoQuartosController quartos;
    private Cliente maria;

    @BeforeEach
    void preparar() {
        sessao = new Sessao();
        gestor = Utilizador.novo("gestor", "gestor123", "Gestor Inicial", Perfil.GESTOR);
        gestor.setId(1);
        sessao.iniciar(gestor);
        reservaDAO = new ReservaDAOMemoria();
        stockDAO = new StockDAOMemoria() {
            @Override
            public List<Artigo> listarArtigos() {
                List<Artigo> artigos = super.listarArtigos();
                for (Artigo artigo : artigos) {
                    int emUso = 0;
                    for (Reserva reserva : reservaDAO.listarTodos()) {
                        emUso += reserva.quantidadeUsada(artigo);
                    }
                    artigo.setQuantidadeReservada(emUso);
                }
                return artigos;
            }
        };
        quartoDAO = new QuartoDAOMemoria();
        cartaoDAO = new CheckInCheckOutControllerTest.CartaoDAOEmMemoria();
        stock = new GestaoStockController(stockDAO, sessao);
        checkInOut = new CheckInCheckOutController(reservaDAO, cartaoDAO, stockDAO, sessao);
        edicao = new EdicaoReservaController(reservaDAO, new ServicoDAOMemoria(), stockDAO, sessao);
        quartos = new GestaoQuartosController(quartoDAO, new EquipamentoDAOMemoria(), null, stockDAO, sessao);
        maria = new Cliente(1, "Maria Silva", "PT123456", "910000000");

        // Stock inicial: 10 toalhas, 10 champôs (consumível) e 2 televisões.
        List<MovimentoStock> entradas = new ArrayList<>();
        entradas.add(entrada("Toalha de banho", 10));
        entradas.add(entrada("Champô", 10));
        entradas.add(entrada("Televisão", 2));
        stockDAO.registarEntrada(new Fornecedor(0, "ProductHero", "11PT920", ""), entradas);
        stock.definirConsumivel(artigo("Champô").getId(), true);
    }

    private MovimentoStock entrada(String nome, int quantidade) {
        return new MovimentoStock(TipoMovimento.ENTRADA, quantidade, LocalDateTime.now(), gestor,
                new Artigo(0, nome, "un", 0));
    }

    private Artigo artigo(String nome) {
        return stockDAO.listarArtigos().stream().filter(a -> a.getNome().equals(nome)).findFirst().orElseThrow();
    }

    /** Quarto duplo com 2 toalhas, 2 champôs e 1 televisão, configurado pelo gestor. */
    private Quarto quartoDuplo() {
        return quartos.criarQuarto(205, "Ala poente", TipoQuarto.DUPLO, new BigDecimal("80.00"), Vista.NENHUMA,
                List.of(), List.of(new ProdutoQuarto(artigo("Toalha de banho"), 2),
                        new ProdutoQuarto(artigo("Champô"), 2), new ProdutoQuarto(artigo("Televisão"), 1)));
    }

    private Reserva reservaParaHoje(Quarto quarto) {
        return reservaDAO.inserir(new Reserva(0, maria, quarto, LocalDate.now(), LocalDate.now().plusDays(2)));
    }

    // ================= S6-04: saída de stock =================

    @Test
    void saidaDescontaAoStockRealEFicaNoHistoricoComAJustificacao() {
        int idTv = artigo("Televisão").getId();

        stock.darSaida(idTv, 1, "Televisão avariada no quarto 205");

        assertEquals(1, artigo("Televisão").getQuantidadeAtual());
        assertEquals(1, artigo("Televisão").getQuantidadeDisponivel());
        List<MovimentoStock> historico = stock.consultarHistoricoSaidas();
        assertEquals(1, historico.size());
        assertEquals(TipoMovimento.SAIDA, historico.get(0).getTipo());
        assertEquals("Televisão", historico.get(0).getArtigo().getNome());
        assertEquals(1, historico.get(0).getQuantidade());
        assertEquals("Televisão avariada no quarto 205", historico.get(0).getJustificacao());
        assertEquals(gestor, historico.get(0).getUtilizador());
    }

    @Test
    void saidaSemJustificacaoOuSemStockNaoFicaGuardada() {
        int idTv = artigo("Televisão").getId();

        assertThrows(IllegalArgumentException.class, () -> stock.darSaida(idTv, 1, " "));
        assertThrows(IllegalArgumentException.class, () -> stock.darSaida(idTv, 3, "Avaria"));
        assertThrows(IllegalArgumentException.class, () -> stock.darSaida(idTv, 0, "Avaria"));
        assertThrows(IllegalArgumentException.class, () -> stock.darSaida(999, 1, "Avaria"));

        assertEquals(2, artigo("Televisão").getQuantidadeAtual());
        assertTrue(stock.consultarHistoricoSaidas().isEmpty());
    }

    @Test
    void funcionarioDaRececaoTambemDaSaidaDeStock() {
        sessao.iniciar(Utilizador.novo("rececao", "rececao123", "Ana", Perfil.RECECIONISTA));

        stock.darSaida(artigo("Toalha de banho").getId(), 2, "Toalhas rasgadas");

        assertEquals(8, artigo("Toalha de banho").getQuantidadeAtual());
    }

    @Test
    void outrosPerfisNaoDaoSaidaDeStock() {
        sessao.iniciar(Utilizador.novo("fornecedor", "fornecedor1", "Fornecedor", Perfil.FORNECEDOR));

        assertThrows(SecurityException.class, () -> stock.darSaida(1, 1, "Avaria"));
        assertThrows(SecurityException.class, () -> stock.consultarHistoricoSaidas());
    }

    @Test
    void gestaoDeStockMostraORealEODisponivel() {
        Quarto quarto = quartoDuplo();
        Reserva reserva = reservaParaHoje(quarto);
        checkInOut.fazerCheckIn(reserva.getId());

        Artigo toalhas = artigo("Toalha de banho");
        assertEquals(10, toalhas.getQuantidadeAtual(), "stock real: o total do hotel");
        assertEquals(2, toalhas.getQuantidadeReservada(), "em uso na reserva com check-in");
        assertEquals(8, toalhas.getQuantidadeDisponivel(), "disponível: real menos o que está em uso");
    }

    @Test
    void consumivelPodeSerMarcadoEDesmarcado() {
        int id = artigo("Toalha de banho").getId();

        stock.definirConsumivel(id, true);
        assertTrue(artigo("Toalha de banho").isConsumivel());

        stock.definirConsumivel(id, false);
        assertFalse(artigo("Toalha de banho").isConsumivel());
        assertThrows(IllegalArgumentException.class, () -> stock.definirConsumivel(999, true));
    }

    // ================= S6-05: equipamentos do quarto ligados ao stock =================

    @Test
    void equipamentosDoQuartoSaoEscolhidosDosProdutosEmStock() {
        Quarto quarto = quartoDuplo();

        Quarto guardado = quartoDAO.procurarPorId(quarto.getId()).orElseThrow();
        assertEquals(3, guardado.getProdutos().size());
        assertEquals(3, quartos.consultarProdutosStock().size());

        Artigo inexistente = new Artigo(999, "Frigobar", "un", 0);
        assertThrows(IllegalArgumentException.class, () -> quartos.criarQuarto(206, "", TipoQuarto.DUPLO,
                new BigDecimal("80.00"), Vista.NENHUMA, List.of(), List.of(new ProdutoQuarto(inexistente, 1))));
    }

    @Test
    void editarOQuartoMudaOsProdutos() {
        Quarto quarto = quartoDuplo();

        quartos.editarQuarto(quarto.getId(), "Ala poente", TipoQuarto.DUPLO, new BigDecimal("80.00"), Vista.NENHUMA,
                true, List.of(), List.of(new ProdutoQuarto(artigo("Toalha de banho"), 4)));

        Quarto guardado = quartoDAO.procurarPorId(quarto.getId()).orElseThrow();
        assertEquals(1, guardado.getProdutos().size());
        assertEquals(4, guardado.getProdutos().get(0).getQuantidade());
    }

    @Test
    void noCheckInOsProdutosDoQuartoDaoBaixaNoStockDisponivel() {
        Reserva reserva = reservaParaHoje(quartoDuplo());

        checkInOut.fazerCheckIn(reserva.getId());

        assertEquals(EstadoReserva.CHECK_IN, reservaDAO.procurarPorId(reserva.getId()).orElseThrow().getEstado());
        assertEquals(8, artigo("Toalha de banho").getQuantidadeDisponivel());
        assertEquals(8, artigo("Champô").getQuantidadeDisponivel());
        assertEquals(1, artigo("Televisão").getQuantidadeDisponivel());
        assertEquals(10, artigo("Toalha de banho").getQuantidadeAtual(), "o stock real não muda");
    }

    @Test
    void semStockDisponivelOCheckInFalhaSemDeixarCartao() {
        Quarto quarto = quartoDuplo();
        stock.darSaida(artigo("Televisão").getId(), 2, "Televisões avariadas");
        Reserva reserva = reservaParaHoje(quarto);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> checkInOut.fazerCheckIn(reserva.getId()));

        assertTrue(erro.getMessage().contains("Televisão"));
        assertEquals(EstadoReserva.PENDENTE, reservaDAO.procurarPorId(reserva.getId()).orElseThrow().getEstado());
        assertTrue(cartaoDAO.listarTodos().isEmpty(), "o cartão criado foi removido");
        assertEquals(10, artigo("Toalha de banho").getQuantidadeDisponivel());
    }

    @Test
    void noCheckOutOsProdutosRegressamExcetoConsumiveis() {
        Reserva reserva = reservaParaHoje(quartoDuplo());
        checkInOut.fazerCheckIn(reserva.getId());

        checkInOut.fazerCheckOut(reserva.getId());

        assertEquals(10, artigo("Toalha de banho").getQuantidadeDisponivel(), "as toalhas voltam");
        assertEquals(2, artigo("Televisão").getQuantidadeDisponivel(), "a televisão volta");
        assertEquals(8, artigo("Champô").getQuantidadeAtual(), "o champô gasto sai do stock real");
        assertEquals(8, artigo("Champô").getQuantidadeDisponivel());
        List<MovimentoStock> historico = stock.consultarHistoricoSaidas();
        assertEquals(1, historico.size());
        assertEquals(TipoMovimento.UTILIZACAO, historico.get(0).getTipo());
        assertEquals("Champô", historico.get(0).getArtigo().getNome());
    }

    @Test
    void naEdicaoDaReservaAlterarOsProdutosAjustaOStock() {
        Reserva reserva = reservaParaHoje(quartoDuplo());
        checkInOut.fazerCheckIn(reserva.getId());
        int idToalhas = artigo("Toalha de banho").getId();

        // Só uma pessoa no quarto duplo: 1 toalha em vez de 2.
        edicao.alterarProduto(reserva.getId(), idToalhas, 1);

        assertEquals(1, reservaDAO.procurarPorId(reserva.getId()).orElseThrow().quantidadeUsada(artigo("Toalha de banho")));
        assertEquals(9, artigo("Toalha de banho").getQuantidadeDisponivel());

        edicao.alterarProduto(reserva.getId(), idToalhas, 0);
        assertEquals(10, artigo("Toalha de banho").getQuantidadeDisponivel());
    }

    @Test
    void naEdicaoNaoSePedeMaisDoQueODisponivel() {
        Reserva reserva = reservaParaHoje(quartoDuplo());
        checkInOut.fazerCheckIn(reserva.getId());

        assertThrows(IllegalArgumentException.class,
                () -> edicao.alterarProduto(reserva.getId(), artigo("Televisão").getId(), 3));
        assertEquals(1, artigo("Televisão").getQuantidadeDisponivel());
    }

    @Test
    void reservaComCheckOutNaoAlteraProdutos() {
        Reserva reserva = reservaParaHoje(quartoDuplo());
        checkInOut.fazerCheckIn(reserva.getId());
        checkInOut.fazerCheckOut(reserva.getId());

        assertThrows(IllegalArgumentException.class,
                () -> edicao.alterarProduto(reserva.getId(), artigo("Toalha de banho").getId(), 1));
    }
}
