package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.EquipamentoDAOMemoria;
import com.hotel4you.hotel4you.DAO.QuartoDAOMemoria;
import com.hotel4you.hotel4you.DAO.ReservaDAOMemoria;
import com.hotel4you.hotel4you.DAO.ServicoDAOMemoria;
import com.hotel4you.hotel4you.DAO.StockDAOMemoria;
import com.hotel4you.hotel4you.Model.Artigo;
import com.hotel4you.hotel4you.Model.Cliente;
import com.hotel4you.hotel4you.Model.ConsumoServico;
import com.hotel4you.hotel4you.Model.Fornecedor;
import com.hotel4you.hotel4you.Model.MovimentoStock;
import com.hotel4you.hotel4you.Model.ProdutoQuarto;
import com.hotel4you.hotel4you.Model.ProdutoReserva;
import com.hotel4you.hotel4you.Model.Quarto;
import com.hotel4you.hotel4you.Model.Reserva;
import com.hotel4you.hotel4you.Model.Servico;
import com.hotel4you.hotel4you.Model.Utilizador;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * S7-01: baixa de stock no check-in, nos controllers, com DAO em memória. Tal como o DAO real, o stock
 * em uso é a soma dos produtos das reservas com check-in.
 */
class BaixaStockCheckInControllerTest {

    private Sessao sessao;
    private Utilizador gestor;
    private ReservaDAOMemoria reservaDAO;
    private StockDAOMemoria stockDAO;
    private ServicoDAOMemoria servicoDAO;
    private GestaoStockController stock;
    private GestaoServicosController servicos;
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
        servicoDAO = new ServicoDAOMemoria();
        stock = new GestaoStockController(stockDAO, sessao);
        servicos = new GestaoServicosController(servicoDAO, stockDAO, sessao);
        checkInOut = new CheckInCheckOutController(reservaDAO, new CheckInCheckOutControllerTest.CartaoDAOEmMemoria(),
                stockDAO, sessao);
        edicao = new EdicaoReservaController(reservaDAO, servicoDAO, stockDAO, sessao);
        quartos = new GestaoQuartosController(new QuartoDAOMemoria(), new EquipamentoDAOMemoria(), null, stockDAO, sessao);
        maria = new Cliente(1, "Maria Silva", "PT123456", "910000000");

        // Stock inicial: 10 toalhas e 6 águas (consumível).
        List<MovimentoStock> entradas = new ArrayList<>();
        entradas.add(new MovimentoStock(TipoMovimento.ENTRADA, 10, LocalDateTime.now(), gestor,
                new Artigo(0, "Toalha de banho", "un", 0)));
        entradas.add(new MovimentoStock(TipoMovimento.ENTRADA, 6, LocalDateTime.now(), gestor,
                new Artigo(0, "Água 50cl", "un", 0)));
        stockDAO.registarEntrada(new Fornecedor(0, "ProductHero", "11PT920", ""), entradas);
        stock.definirConsumivel(artigo("Água 50cl").getId(), true);
    }

    private Artigo artigo(String nome) {
        return stockDAO.listarArtigos().stream().filter(a -> a.getNome().equals(nome)).findFirst().orElseThrow();
    }

    /** Extra "Água do minibar", ligado pelo gestor a 1 garrafa de água por unidade. */
    private Servico aguaDoMinibar() {
        return servicos.criarServico(TipoServico.EXTRA, "Água do minibar", "Bebidas", "", new BigDecimal("2.00"),
                TipoCobranca.POR_UNIDADE, artigo("Água 50cl").getId(), 1);
    }

    /** Reserva pendente para hoje no quarto 205 (2 toalhas), com 1 toalha extra e 2 águas do minibar. */
    private Reserva reservaComExtras() {
        Quarto quarto = quartos.criarQuarto(205, "Ala poente", TipoQuarto.DUPLO, new BigDecimal("80.00"),
                Vista.NENHUMA, List.of(), List.of(new ProdutoQuarto(artigo("Toalha de banho"), 2)));
        return reservaDAO.inserir(new Reserva(0, maria, quarto, LocalDate.now(), LocalDate.now().plusDays(2),
                List.of(new ConsumoServico(aguaDoMinibar(), 2, LocalDate.now())),
                List.of(new ProdutoReserva(artigo("Toalha de banho"), 1))));
    }

    @Test
    void gestorLigaUmServicoAUmProdutoDoStock() {
        Servico agua = aguaDoMinibar();

        assertTrue(agua.temProduto());
        assertEquals("Água 50cl", agua.getProduto().getNome());

        servicos.editarServico(agua.getId(), TipoServico.EXTRA, "Água do minibar", "Bebidas", "",
                new BigDecimal("2.00"), TipoCobranca.POR_UNIDADE, true, null, 0);
        assertFalse(servicoDAO.procurarPorId(agua.getId()).orElseThrow().temProduto());
        assertThrows(IllegalArgumentException.class, () -> servicos.criarServico(TipoServico.EXTRA, "Sumo", "", "",
                BigDecimal.ONE, TipoCobranca.POR_UNIDADE, 999, 1));
    }

    @Test
    void criarAReservaNaoBaixaOStock() {
        reservaComExtras();

        assertEquals(10, artigo("Toalha de banho").getQuantidadeDisponivel());
        assertEquals(6, artigo("Água 50cl").getQuantidadeDisponivel());
    }

    @Test
    void checkInBaixaEquipamentosDoQuartoExtrasEBebidas() {
        Reserva reserva = reservaComExtras();

        checkInOut.fazerCheckIn(reserva.getId());

        assertEquals(7, artigo("Toalha de banho").getQuantidadeDisponivel(), "2 do quarto + 1 extra");
        assertEquals(4, artigo("Água 50cl").getQuantidadeDisponivel(), "2 águas do minibar");
        assertEquals(10, artigo("Toalha de banho").getQuantidadeAtual(), "o stock real não muda");
    }

    @Test
    void naEdicaoAcrescentarERemoverBebidasAjustaOStock() {
        Reserva reserva = reservaComExtras();
        checkInOut.fazerCheckIn(reserva.getId());

        edicao.adicionarServico(reserva.getId(), servicoDAO.listarTodos().get(0), 3);
        assertEquals(1, artigo("Água 50cl").getQuantidadeDisponivel());

        assertThrows(IllegalArgumentException.class,
                () -> edicao.adicionarServico(reserva.getId(), servicoDAO.listarTodos().get(0), 2),
                "só resta 1 água disponível");

        edicao.removerServico(reserva.getId(), 1);   // as 3 águas acrescentadas
        assertEquals(4, artigo("Água 50cl").getQuantidadeDisponivel());
    }

    @Test
    void naEdicaoAlterarAQuantidadeAjustaOStock() {
        Reserva reserva = reservaComExtras();
        checkInOut.fazerCheckIn(reserva.getId());

        edicao.alterarProduto(reserva.getId(), artigo("Toalha de banho").getId(), 1);

        assertEquals(9, artigo("Toalha de banho").getQuantidadeDisponivel());
    }

    @Test
    void naEdicaoDeUmaPendenteOStockNaoMexe() {
        Reserva reserva = reservaComExtras();

        edicao.alterarProduto(reserva.getId(), artigo("Toalha de banho").getId(), 4);

        assertEquals(4, reservaDAO.procurarPorId(reserva.getId()).orElseThrow()
                .quantidadeNaReserva(artigo("Toalha de banho")));
        assertEquals(10, artigo("Toalha de banho").getQuantidadeDisponivel());
    }

    @Test
    void checkOutDevolveAoStockExcetoConsumiveis() {
        Reserva reserva = reservaComExtras();
        checkInOut.fazerCheckIn(reserva.getId());

        checkInOut.fazerCheckOut(reserva.getId());

        assertEquals(10, artigo("Toalha de banho").getQuantidadeDisponivel(), "as toalhas voltam");
        assertEquals(4, artigo("Água 50cl").getQuantidadeAtual(), "as águas bebidas saem do stock real");
        assertEquals(4, artigo("Água 50cl").getQuantidadeDisponivel());
        List<MovimentoStock> historico = stock.consultarHistoricoSaidas();
        assertEquals(1, historico.size());
        assertEquals("Água 50cl", historico.get(0).getArtigo().getNome());
    }
}
