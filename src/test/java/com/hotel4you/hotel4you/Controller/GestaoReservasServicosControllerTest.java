package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.ClienteDAOMemoria;
import com.hotel4you.hotel4you.DAO.QuartoDAOMemoria;
import com.hotel4you.hotel4you.DAO.ReservaDAOMemoria;
import com.hotel4you.hotel4you.DAO.ServicoDAOMemoria;
import com.hotel4you.hotel4you.DAO.StockDAOMemoria;
import com.hotel4you.hotel4you.Model.Artigo;
import com.hotel4you.hotel4you.Model.Cliente;
import com.hotel4you.hotel4you.Model.ConsumoServico;
import com.hotel4you.hotel4you.Model.Fornecedor;
import com.hotel4you.hotel4you.Model.MovimentoStock;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes do {@link GestaoReservasController} para a S5-06 (serviços e produtos na reserva) e a
 * S5-09 (os preços em vigor ficam guardados na reserva), com os DAO em memória.
 */
class GestaoReservasServicosControllerTest {

    private final LocalDate entrada = LocalDate.now().plusDays(10);
    private final LocalDate saida = LocalDate.now().plusDays(13);   // 3 noites

    private ReservaDAOMemoria reservaDAO;
    private ServicoDAOMemoria servicoDAO;
    private StockDAOMemoria stockDAO;
    private Sessao sessao;
    private GestaoReservasController controller;
    private Cliente maria;
    private Quarto quarto202;
    private Quarto quarto203;
    private Servico limpeza;
    private Servico pequenoAlmoco;

    @BeforeEach
    void preparar() {
        ClienteDAOMemoria clienteDAO = new ClienteDAOMemoria();
        QuartoDAOMemoria quartoDAO = new QuartoDAOMemoria();
        reservaDAO = new ReservaDAOMemoria();
        servicoDAO = new ServicoDAOMemoria();
        // O DAO de stock em memória não conhece as reservas; aqui desconta o que elas usam,
        // como o DAO real faz com a tabela ReservaProduto.
        stockDAO = new StockDAOMemoria() {
            @Override
            public List<Artigo> listarArtigos() {
                List<Artigo> artigos = super.listarArtigos();
                for (Artigo artigo : artigos) {
                    int usado = 0;
                    for (Reserva reserva : reservaDAO.listarTodos()) {
                        usado += reserva.quantidadeUsada(artigo);
                    }
                    artigo.setQuantidadeReservada(usado);
                }
                return artigos;
            }
        };
        sessao = new Sessao();
        Utilizador rececionista = Utilizador.novo("rececao", "rececao123", "Ana Rececionista", Perfil.RECECIONISTA);
        sessao.iniciar(rececionista);
        controller = new GestaoReservasController(reservaDAO, clienteDAO, quartoDAO, servicoDAO, stockDAO, sessao);

        maria = clienteDAO.inserir(new Cliente(0, "Maria Silva", "PT123456", "910000000"));
        // 80 (preço base) + 20 (vista mar) = 100 por noite; 3 noites = 300
        quarto202 = quartoDAO.inserir(new Quarto(0, 202, 2, "", TipoQuarto.DUPLO,
                new BigDecimal("80.00"), Vista.MAR, true));
        quarto203 = quartoDAO.inserir(new Quarto(0, 203, 2, "", TipoQuarto.DUPLO,
                new BigDecimal("80.00"), Vista.MAR, true));
        limpeza = servicoDAO.inserir(new Servico(0, TipoServico.EXTRA, "Limpeza extra", "", "",
                new BigDecimal("10.00"), TipoCobranca.POR_UNIDADE, true));
        pequenoAlmoco = servicoDAO.inserir(new Servico(0, TipoServico.SERVICO, "Pequeno-almoço", "", "",
                new BigDecimal("8.00"), TipoCobranca.POR_NOITE, true));
        // 10 toalhas em stock
        stockDAO.registarEntrada(new Fornecedor(0, "Fornecedor Lda", "500000000", ""),
                List.of(new MovimentoStock(TipoMovimento.ENTRADA, 10, LocalDateTime.now(), rececionista,
                        new Artigo(0, "Toalhas", "un", 0))));
    }

    private Artigo toalhas() {
        return stockDAO.listarArtigos().get(0);
    }

    private ConsumoServico linha(Servico servico, int quantidade) {
        return new ConsumoServico(servico, quantidade, LocalDate.now());
    }

    private static void assertValor(String esperado, BigDecimal obtido) {
        assertEquals(0, new BigDecimal(esperado).compareTo(obtido), "esperado " + esperado + ", obtido " + obtido);
    }

    // ---- S5-06: escolher serviços e produtos na criação da reserva ----

    @Test
    void listaOsServicosAtivosEOsProdutosComStockDisponivel() {
        limpeza.setAtivo(false);

        List<Servico> servicos = controller.consultarServicosDisponiveis();
        List<Artigo> produtos = controller.consultarProdutosDisponiveis();

        assertEquals(List.of(pequenoAlmoco), servicos);
        assertEquals(1, produtos.size());
        assertEquals(10, produtos.get(0).getQuantidadeDisponivel());
    }

    @Test
    void criarReservaGuardaOsServicosEOsProdutosComAQuantidade() {
        Reserva reserva = controller.criarReserva(maria, quarto202, entrada, saida,
                List.of(linha(limpeza, 2), linha(pequenoAlmoco, 1)), List.of(new ProdutoReserva(toalhas(), 4)));

        Reserva guardada = reservaDAO.procurarPorId(reserva.getId()).orElseThrow();
        assertEquals(2, guardada.getConsumos().size());
        assertEquals(2, guardada.getConsumos().get(0).getQuantidade());
        assertEquals(1, guardada.getProdutos().size());
        assertEquals(4, guardada.getProdutos().get(0).getQuantidade());
    }

    @Test
    void oTotalSomaOPrecoBaseAVistaEOsExtras() {
        Reserva reserva = controller.criarReserva(maria, quarto202, entrada, saida,
                List.of(linha(limpeza, 2), linha(pequenoAlmoco, 1)), List.of());

        assertValor("344.00", reserva.calcularTotal());   // 3 x (80 + 20) + 2 x 10 + 1 x 8 x 3 noites
    }

    @Test
    void aEstimativaIncluiOsServicosEscolhidos() {
        assertValor("300.00", controller.estimarValor(quarto202, entrada, saida));
        assertValor("320.00", controller.estimarValor(quarto202, entrada, saida, List.of(linha(limpeza, 2))));
    }

    @Test
    void servicoDesativadoNaoPodeSerJuntoAReserva() {
        ConsumoServico escolhido = linha(limpeza, 1);
        limpeza.setAtivo(false);   // o gestor desativa o serviço depois de o rececionista o escolher

        assertThrows(IllegalArgumentException.class,
                () -> controller.criarReserva(maria, quarto202, entrada, saida, List.of(escolhido), List.of()));
        assertTrue(reservaDAO.listarTodos().isEmpty());
    }

    // ---- S5-06: os produtos usados reduzem o stock disponível ----

    @Test
    void produtoUsadoNaReservaReduzOStockDisponivelENaoOStockDoHotel() {
        controller.criarReserva(maria, quarto202, entrada, saida, List.of(), List.of(new ProdutoReserva(toalhas(), 4)));

        assertEquals(10, toalhas().getQuantidadeAtual());
        assertEquals(6, toalhas().getQuantidadeDisponivel());
        assertEquals(6, controller.consultarProdutosDisponiveis().get(0).getQuantidadeDisponivel());
    }

    @Test
    void naoSeUsaMaisDoQueOStockDisponivel() {
        controller.criarReserva(maria, quarto202, entrada, saida, List.of(), List.of(new ProdutoReserva(toalhas(), 7)));

        // Restam 3 disponíveis: outra reserva não pode levar 4.
        assertThrows(IllegalArgumentException.class, () -> controller.criarReserva(maria, quarto203, entrada, saida,
                List.of(), List.of(new ProdutoReserva(toalhas(), 4))));
        assertEquals(1, reservaDAO.listarTodos().size());
        assertEquals(3, toalhas().getQuantidadeDisponivel());
    }

    @Test
    void asLinhasDoMesmoProdutoSaoSomadasParaVerificarOStock() {
        List<ProdutoReserva> produtos = List.of(new ProdutoReserva(toalhas(), 6), new ProdutoReserva(toalhas(), 5));

        assertThrows(IllegalArgumentException.class,
                () -> controller.criarReserva(maria, quarto202, entrada, saida, List.of(), produtos));
    }

    @Test
    void produtoEsgotadoDeixaDeAparecerNaListaDeDisponiveis() {
        controller.criarReserva(maria, quarto202, entrada, saida, List.of(), List.of(new ProdutoReserva(toalhas(), 10)));

        assertTrue(controller.consultarProdutosDisponiveis().isEmpty());
    }

    @Test
    void cancelarAReservaDevolveOStockDisponivel() {
        Reserva reserva = controller.criarReserva(maria, quarto202, entrada, saida,
                List.of(), List.of(new ProdutoReserva(toalhas(), 4)));

        controller.cancelarReserva(reserva.getId());

        assertEquals(10, toalhas().getQuantidadeDisponivel());
    }

    // ---- S5-09: os preços em vigor ficam guardados na reserva ----

    @Test
    void alterarOValorDaVistaSoSeAplicaAsReservasSeguintes() {
        Reserva antes = controller.criarReserva(maria, quarto202, entrada, saida);

        // O gestor altera o valor da vista mar de 20 para 35 (o DAO passa a dar o valor novo aos quartos).
        quarto202.setSuplementoVista(new BigDecimal("35.00"));
        quarto203.setSuplementoVista(new BigDecimal("35.00"));
        Reserva depois = controller.criarReserva(maria, quarto203, entrada, saida);

        assertValor("300.00", reservaDAO.procurarPorId(antes.getId()).orElseThrow().calcularTotal());
        assertValor("345.00", depois.calcularTotal());   // 3 x (80 + 35)
    }

    @Test
    void alterarOValorDeUmExtraSoSeAplicaAsReservasSeguintes() {
        Reserva antes = controller.criarReserva(maria, quarto202, entrada, saida,
                List.of(linha(limpeza, 2)), List.of());

        limpeza.setValor(new BigDecimal("15.00"));   // o gestor sobe a limpeza de 10 para 15
        Reserva depois = controller.criarReserva(maria, quarto203, entrada, saida,
                List.of(linha(limpeza, 2)), List.of());

        assertValor("320.00", reservaDAO.procurarPorId(antes.getId()).orElseThrow().calcularTotal());
        assertValor("330.00", depois.calcularTotal());   // 300 + 2 x 15
    }

    @Test
    void aReservaFicaComOValorDoExtraEmVigorNaCriacao() {
        // O rececionista escolheu a limpeza quando custava 10; entretanto o gestor passou-a para 15.
        ConsumoServico escolhido = linha(limpeza, 1);
        limpeza.setValor(new BigDecimal("15.00"));

        Reserva reserva = controller.criarReserva(maria, quarto202, entrada, saida, List.of(escolhido), List.of());

        assertValor("15.00", reserva.getConsumos().get(0).getValorUnitario());
        assertValor("315.00", reserva.calcularTotal());
    }

    // ---- Permissões ----

    @Test
    void soORececionistaEOGestorConsultamServicosEProdutos() {
        sessao.terminar();
        sessao.iniciar(Utilizador.novo("fornecedor", "fornecedor123", "Fornecedor Lda", Perfil.FORNECEDOR));

        assertThrows(SecurityException.class, () -> controller.consultarServicosDisponiveis());
        assertThrows(SecurityException.class, () -> controller.consultarProdutosDisponiveis());
    }
}
