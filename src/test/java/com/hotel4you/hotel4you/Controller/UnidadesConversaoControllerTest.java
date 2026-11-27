package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.EquipamentoDAOMemoria;
import com.hotel4you.hotel4you.DAO.QuartoDAOMemoria;
import com.hotel4you.hotel4you.DAO.StockDAOMemoria;
import com.hotel4you.hotel4you.Model.Artigo;
import com.hotel4you.hotel4you.Model.ConversaoEmFaltaException;
import com.hotel4you.hotel4you.Model.ProdutoQuarto;
import com.hotel4you.hotel4you.Model.Utilizador;
import com.hotel4you.hotel4you.Model.encomenda.EncomendaFornecedor;
import com.hotel4you.hotel4you.Model.encomenda.FornecedorEncomenda;
import com.hotel4you.hotel4you.Model.encomenda.ImpostoLinha;
import com.hotel4you.hotel4you.Model.encomenda.LinhaEncomenda;
import com.hotel4you.hotel4you.Model.encomenda.Montante;
import com.hotel4you.hotel4you.Model.encomenda.PrecoUnitario;
import com.hotel4you.hotel4you.Model.encomenda.Quantidade;
import com.hotel4you.hotel4you.Model.enums.Perfil;
import com.hotel4you.hotel4you.Model.enums.TipoQuarto;
import com.hotel4you.hotel4you.Model.enums.UnidadeMedida;
import com.hotel4you.hotel4you.Model.enums.Vista;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * S7-02: unidade principal e conversão de caixas na entrada de stock e na janela "Unidades dos produtos",
 * pelo {@link GestaoStockController}, com DAO em memória.
 */
class UnidadesConversaoControllerTest {

    private StockDAOMemoria dao;
    private Sessao sessao;
    private GestaoStockController controller;

    @BeforeEach
    void preparar() {
        dao = new StockDAOMemoria();
        sessao = new Sessao();
        sessao.iniciar(Utilizador.novo("rececao", "rececao123", "Ana Rececionista", Perfil.RECECIONISTA));
        controller = new GestaoStockController(dao, sessao);
    }

    private static LinhaEncomenda linha(String produto, int valor, UnidadeMedida unidade, Quantidade... informativas) {
        Montante zero = new Montante(BigDecimal.ZERO, "EUR");
        return new LinhaEncomenda(1, "P1", "", produto, new Quantidade("Count", valor, unidade), List.of(informativas),
                new PrecoUnitario(new Montante(BigDecimal.ONE, "EUR"), 1, unidade),
                new ImpostoLinha("", 0, zero, "VAT", 23, zero, ""), zero);
    }

    private static EncomendaFornecedor encomenda(LinhaEncomenda linha) {
        FornecedorEncomenda fornecedor = new FornecedorEncomenda("11PT920", "ProductHero", "", "", "", "", "PT", "");
        return new EncomendaFornecedor("E1", LocalDate.of(2026, 11, 27), fornecedor, List.of(linha));
    }

    private Artigo artigo(String nome) {
        return controller.consultarStock().stream().filter(a -> a.getNome().equals(nome)).findFirst().orElseThrow();
    }

    @Test
    void importacaoUsaAConversaoDoFicheiroEGuardaA() {
        controller.darEntrada(encomenda(linha("Toalha de banho", 5, UnidadeMedida.CAIXA,
                new Quantidade("Count", 50, UnidadeMedida.UNIDADE))));

        Artigo toalhas = artigo("Toalha de banho");
        assertEquals(50, toalhas.getQuantidadeAtual());
        assertEquals("un", toalhas.getUnidade());
        assertEquals(10, toalhas.getUnidadesPorCaixa());
    }

    @Test
    void semConversaoNoFicheiroOFuncionarioDefineA() {
        EncomendaFornecedor semConversao = encomenda(linha("Toalha de banho", 3, UnidadeMedida.CAIXA));

        assertThrows(ConversaoEmFaltaException.class, () -> controller.darEntrada(semConversao));
        assertTrue(controller.consultarStock().isEmpty(), "nada deu entrada");

        controller.darEntrada(semConversao, Map.of("toalha de BANHO", 12));

        assertEquals(36, artigo("Toalha de banho").getQuantidadeAtual());
        assertEquals(12, artigo("Toalha de banho").getUnidadesPorCaixa());
    }

    @Test
    void aConversaoGuardadaServeParaAsImportacoesSeguintes() {
        controller.darEntrada(encomenda(linha("Toalha de banho", 1, UnidadeMedida.CAIXA,
                new Quantidade("Count", 10, UnidadeMedida.UNIDADE))));

        controller.darEntrada(encomenda(linha("Toalha de banho", 2, UnidadeMedida.CAIXA)));

        assertEquals(30, artigo("Toalha de banho").getQuantidadeAtual());
    }

    @Test
    void funcionarioDefineAConversaoNaJanelaDeUnidades() {
        controller.darEntrada(encomenda(linha("Secador", 4, UnidadeMedida.UNIDADE)));

        Artigo secador = controller.definirConversaoCaixa(artigo("Secador").getId(), 2);

        assertEquals("1 cx = 2 un", secador.descreverConversao());
        assertEquals(2, artigo("Secador").getUnidadesPorCaixa());
        assertEquals(4, artigo("Secador").getQuantidadeAtual(), "o stock já estava em unidades");
        assertThrows(IllegalArgumentException.class, () -> controller.definirConversaoCaixa(secador.getId(), 0));
    }

    @Test
    void produtoAntigoEmCaixasPassaAUnidadesEDepoisPodeIrParaOsQuartos() {
        dao.guardarArtigo(new Artigo(0, "Toalha de banho", "cx", 5));
        sessao.iniciar(Utilizador.novo("gestor", "gestor123", "Gestor Inicial", Perfil.GESTOR));
        GestaoQuartosController quartos = new GestaoQuartosController(new QuartoDAOMemoria(),
                new EquipamentoDAOMemoria(), null, dao, sessao);
        List<ProdutoQuarto> duasToalhas = List.of(new ProdutoQuarto(artigo("Toalha de banho"), 2));

        assertThrows(IllegalArgumentException.class, () -> quartos.criarQuarto(205, "", TipoQuarto.DUPLO,
                new BigDecimal("80.00"), Vista.NENHUMA, List.of(), duasToalhas), "em caixas não vai para os quartos");
        assertThrows(IllegalArgumentException.class, () -> controller.darEntrada(encomenda(
                linha("Toalha de banho", 10, UnidadeMedida.UNIDADE))), "não se somam unidades a caixas");

        controller.definirConversaoCaixa(artigo("Toalha de banho").getId(), 10);

        assertEquals("un", artigo("Toalha de banho").getUnidade());
        assertEquals(50, artigo("Toalha de banho").getQuantidadeAtual());
        quartos.criarQuarto(205, "", TipoQuarto.DUPLO, new BigDecimal("80.00"), Vista.NENHUMA, List.of(),
                List.of(new ProdutoQuarto(artigo("Toalha de banho"), 2)));
    }

    @Test
    void unidadesDiferentesNoMesmoProdutoSaoRecusadas() {
        controller.darEntrada(encomenda(linha("Detergente", 5, UnidadeMedida.QUILOGRAMA)));

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> controller.darEntrada(encomenda(linha("Detergente", 5, UnidadeMedida.UNIDADE))));
        assertTrue(erro.getMessage().contains("kg"));
        assertEquals("kg", artigo("Detergente").getUnidade());
    }
}
