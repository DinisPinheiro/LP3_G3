package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.ConfiguracaoTipoQuartoDAOMemoria;
import com.hotel4you.hotel4you.DAO.EquipamentoDAOMemoria;
import com.hotel4you.hotel4you.DAO.QuartoDAOMemoria;
import com.hotel4you.hotel4you.DAO.StockDAOMemoria;
import com.hotel4you.hotel4you.Model.Artigo;
import com.hotel4you.hotel4you.Model.ConfiguracaoTipoQuarto;
import com.hotel4you.hotel4you.Model.Equipamento;
import com.hotel4you.hotel4you.Model.Fornecedor;
import com.hotel4you.hotel4you.Model.MovimentoStock;
import com.hotel4you.hotel4you.Model.ProdutoQuarto;
import com.hotel4you.hotel4you.Model.Quarto;
import com.hotel4you.hotel4you.Model.Utilizador;
import com.hotel4you.hotel4you.Model.enums.Perfil;
import com.hotel4you.hotel4you.Model.enums.TipoMovimento;
import com.hotel4you.hotel4you.Model.enums.TipoQuarto;
import com.hotel4you.hotel4you.Model.enums.Vista;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * S6-09: o gestor configura o preço base e os equipamentos por tipo de quarto; os quartos herdam a
 * configuração e podem ter exceções. Só o gestor acede.
 */
class GestaoQuartosConfiguracaoTipoControllerTest {

    private QuartoDAOMemoria quartos;
    private EquipamentoDAOMemoria equipamentos;
    private ConfiguracaoTipoQuartoDAOMemoria configuracoes;
    private StockDAOMemoria stock;
    private Sessao sessao;
    private GestaoQuartosController controller;
    private Equipamento frigobar;
    private Equipamento cofre;
    private Artigo toalhas;
    private Artigo televisao;

    @BeforeEach
    void preparar() {
        quartos = new QuartoDAOMemoria();
        equipamentos = new EquipamentoDAOMemoria();
        configuracoes = new ConfiguracaoTipoQuartoDAOMemoria();
        stock = new StockDAOMemoria();
        sessao = new Sessao();
        Utilizador gestor = Utilizador.novo("gestor", "gestor123", "Gestor", Perfil.GESTOR);
        gestor.setId(1);
        sessao.iniciar(gestor);
        controller = new GestaoQuartosController(quartos, equipamentos, null, stock, configuracoes, sessao);
        frigobar = equipamentos.inserir(new Equipamento(0, "Frigobar"));
        cofre = equipamentos.inserir(new Equipamento(0, "Cofre"));
        stock.registarEntrada(new Fornecedor(0, "ProductHero", "11PT920", ""), List.of(
                entrada(gestor, "Toalha de banho", 10), entrada(gestor, "Televisão", 3)));
        toalhas = artigo("Toalha de banho");
        televisao = artigo("Televisão");
    }

    private static MovimentoStock entrada(Utilizador gestor, String nome, int quantidade) {
        return new MovimentoStock(TipoMovimento.ENTRADA, quantidade, LocalDateTime.now(), gestor,
                new Artigo(0, nome, "un", 0));
    }

    private Artigo artigo(String nome) {
        return stock.listarArtigos().stream().filter(a -> a.getNome().equals(nome)).findFirst().orElseThrow();
    }

    private void configurarDuplo(String preco) {
        controller.guardarConfiguracaoTipo(TipoQuarto.DUPLO, new BigDecimal(preco), List.of(frigobar),
                List.of(new ProdutoQuarto(toalhas, 2), new ProdutoQuarto(televisao, 1)));
    }

    private Quarto quartoHerdado(int numero) {
        return controller.criarQuartoDoTipo(numero, "", TipoQuarto.DUPLO, Vista.NENHUMA, null, null, null);
    }

    private static void assertValor(String esperado, BigDecimal obtido) {
        assertEquals(0, new BigDecimal(esperado).compareTo(obtido), "esperado " + esperado + ", obtido " + obtido);
    }

    // ---- Definir a configuração do tipo ----

    @Test
    void gestorDefinePrecoEEquipamentosPorTipo() {
        configurarDuplo("80");
        controller.guardarConfiguracaoTipo(TipoQuarto.INDIVIDUAL, new BigDecimal("50"), List.of(), List.of());
        controller.guardarConfiguracaoTipo(TipoQuarto.FAMILIA, new BigDecimal("140"), List.of(frigobar, cofre), List.of());

        ConfiguracaoTipoQuarto duplo = controller.consultarConfiguracaoTipo(TipoQuarto.DUPLO).orElseThrow();
        assertValor("80.00", duplo.getPrecoBase());
        assertEquals(List.of(frigobar), duplo.getEquipamentos());
        assertEquals(2, duplo.getProdutos().size());
        assertValor("50.00", controller.consultarConfiguracaoTipo(TipoQuarto.INDIVIDUAL).orElseThrow().getPrecoBase());
        assertEquals(2, controller.consultarConfiguracaoTipo(TipoQuarto.FAMILIA).orElseThrow().getEquipamentos().size());
    }

    @Test
    void tipoSemConfiguracaoNaoTemConfiguracao() {
        assertTrue(controller.consultarConfiguracaoTipo(TipoQuarto.DUPLO).isEmpty());
    }

    @Test
    void guardarDeNovoSubstituiAConfiguracaoDoTipo() {
        configurarDuplo("80");

        controller.guardarConfiguracaoTipo(TipoQuarto.DUPLO, new BigDecimal("85.50"), List.of(cofre), List.of());

        ConfiguracaoTipoQuarto duplo = controller.consultarConfiguracaoTipo(TipoQuarto.DUPLO).orElseThrow();
        assertValor("85.50", duplo.getPrecoBase());
        assertEquals(List.of(cofre), duplo.getEquipamentos());
        assertTrue(duplo.getProdutos().isEmpty());
    }

    @Test
    void validaAConfiguracaoDoTipo() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.guardarConfiguracaoTipo(null, BigDecimal.TEN, List.of(), List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> controller.guardarConfiguracaoTipo(TipoQuarto.DUPLO, null, List.of(), List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> controller.guardarConfiguracaoTipo(TipoQuarto.DUPLO, new BigDecimal("-5"), List.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> controller.guardarConfiguracaoTipo(TipoQuarto.DUPLO,
                new BigDecimal("100000000"), List.of(), List.of()));
        assertThrows(IllegalArgumentException.class, () -> controller.guardarConfiguracaoTipo(TipoQuarto.DUPLO,
                BigDecimal.TEN, List.of(new Equipamento(99, "Inexistente")), List.of()));
        assertThrows(IllegalArgumentException.class, () -> controller.guardarConfiguracaoTipo(TipoQuarto.DUPLO,
                BigDecimal.TEN, List.of(), List.of(new ProdutoQuarto(new Artigo(99, "Inexistente", "un", 1), 1))));
        assertTrue(controller.consultarConfiguracaoTipo(TipoQuarto.DUPLO).isEmpty(), "nada ficou guardado");
    }

    // ---- Os quartos herdam a configuração do tipo ----

    @Test
    void quartoNovoHerdaOPrecoEOsEquipamentosDoTipo() {
        configurarDuplo("80");

        Quarto quarto = quartoHerdado(205);

        assertFalse(quarto.isPrecoPersonalizado());
        assertFalse(quarto.isEquipamentosPersonalizados());
        assertValor("80.00", quarto.getPrecoBase());
        assertEquals(List.of(frigobar), quarto.getEquipamentos());
        assertEquals(2, quarto.getProdutos().size());
        assertEquals(quarto.getPrecoBase(), quartos.procurarPorNumero(205).orElseThrow().getPrecoBase());
    }

    @Test
    void naoHaQueReconfigurarCadaQuartoNovo() {
        configurarDuplo("80");

        Quarto a = quartoHerdado(205);
        Quarto b = quartoHerdado(206);
        Quarto c = quartoHerdado(207);

        for (Quarto quarto : List.of(a, b, c)) {
            assertValor("80.00", quarto.getPrecoBase());
            assertEquals(2, quarto.getProdutos().size());
        }
    }

    @Test
    void quartoDeUmTipoSoHerdaDaConfiguracaoDesseTipo() {
        configurarDuplo("80");
        controller.guardarConfiguracaoTipo(TipoQuarto.INDIVIDUAL, new BigDecimal("50"), List.of(cofre), List.of());

        Quarto individual = controller.criarQuartoDoTipo(201, "", TipoQuarto.INDIVIDUAL, Vista.NENHUMA, null, null, null);

        assertValor("50.00", individual.getPrecoBase());
        assertEquals(List.of(cofre), individual.getEquipamentos());
        assertTrue(individual.getProdutos().isEmpty());
    }

    @Test
    void tipoSemConfiguracaoNaoPodeSerHerdado() {
        assertThrows(IllegalArgumentException.class, () -> quartoHerdado(205));
        assertTrue(quartos.listarTodos().isEmpty(), "o quarto não ficou criado");
    }

    // ---- Exceções ----

    @Test
    void quartoPodeTerPrecoProprio() {
        configurarDuplo("80");

        Quarto quarto = controller.criarQuartoDoTipo(205, "", TipoQuarto.DUPLO, Vista.NENHUMA,
                new BigDecimal("95"), null, null);

        assertTrue(quarto.isPrecoPersonalizado());
        assertFalse(quarto.isEquipamentosPersonalizados());
        assertValor("95.00", quarto.getPrecoBase());
        assertEquals(List.of(frigobar), quarto.getEquipamentos(), "os equipamentos continuam a ser os do tipo");
    }

    @Test
    void quartoPodeTerEquipamentosProprios() {
        configurarDuplo("80");

        Quarto quarto = controller.criarQuartoDoTipo(205, "", TipoQuarto.DUPLO, Vista.NENHUMA, null,
                List.of(cofre), List.of(new ProdutoQuarto(toalhas, 4)));

        assertTrue(quarto.isEquipamentosPersonalizados());
        assertFalse(quarto.isPrecoPersonalizado());
        assertValor("80.00", quarto.getPrecoBase());
        assertEquals(List.of(cofre), quarto.getEquipamentos());
        assertEquals(1, quarto.getProdutos().size());
        assertEquals(4, quarto.getProdutos().get(0).getQuantidade());
    }

    @Test
    void quartoComTudoProprioNaoPrecisaDeConfiguracaoDoTipo() {
        Quarto quarto = controller.criarQuartoDoTipo(205, "", TipoQuarto.DUPLO, Vista.NENHUMA,
                new BigDecimal("70"), List.of(), List.of());

        assertTrue(quarto.isPrecoPersonalizado());
        assertTrue(quarto.isEquipamentosPersonalizados());
        assertValor("70.00", quarto.getPrecoBase());
    }

    @Test
    void quartoPodeVoltarAHerdarDoTipo() {
        configurarDuplo("80");
        Quarto quarto = controller.criarQuartoDoTipo(205, "", TipoQuarto.DUPLO, Vista.NENHUMA,
                new BigDecimal("95"), List.of(cofre), List.of());

        controller.editarQuartoDoTipo(quarto.getId(), "", TipoQuarto.DUPLO, Vista.NENHUMA, true, null, null, null);

        Quarto guardado = quartos.procurarPorId(quarto.getId()).orElseThrow();
        assertFalse(guardado.isPrecoPersonalizado());
        assertFalse(guardado.isEquipamentosPersonalizados());
        assertValor("80.00", guardado.getPrecoBase());
        assertEquals(List.of(frigobar), guardado.getEquipamentos());
    }

    @Test
    void quartoPodeDeixarDeHerdarEFicarComExcecao() {
        configurarDuplo("80");
        Quarto quarto = quartoHerdado(205);

        controller.editarQuartoDoTipo(quarto.getId(), "", TipoQuarto.DUPLO, Vista.NENHUMA, true,
                new BigDecimal("99"), null, null);

        Quarto guardado = quartos.procurarPorId(quarto.getId()).orElseThrow();
        assertTrue(guardado.isPrecoPersonalizado());
        assertValor("99.00", guardado.getPrecoBase());
    }

    @Test
    void mudarOTipoDoQuartoFazOQueHerdaPassarAVirDoTipoNovo() {
        configurarDuplo("80");
        controller.guardarConfiguracaoTipo(TipoQuarto.FAMILIA, new BigDecimal("140"), List.of(cofre), List.of());
        Quarto quarto = quartoHerdado(205);

        controller.editarQuartoDoTipo(quarto.getId(), "", TipoQuarto.FAMILIA, Vista.NENHUMA, true, null, null, null);

        Quarto guardado = quartos.procurarPorId(quarto.getId()).orElseThrow();
        assertEquals(TipoQuarto.FAMILIA, guardado.getTipo());
        assertValor("140.00", guardado.getPrecoBase());
        assertEquals(List.of(cofre), guardado.getEquipamentos());
        assertTrue(guardado.getProdutos().isEmpty());
    }

    @Test
    void editarComErroNaoMudaOQuarto() {
        configurarDuplo("80");
        Quarto quarto = quartoHerdado(205);

        assertThrows(IllegalArgumentException.class, () -> controller.editarQuartoDoTipo(quarto.getId(), "Ala nova",
                TipoQuarto.FAMILIA, Vista.MAR, false, null, null, null), "a família ainda não tem configuração");

        Quarto guardado = quartos.procurarPorId(quarto.getId()).orElseThrow();
        assertEquals(TipoQuarto.DUPLO, guardado.getTipo());
        assertEquals("", guardado.getLocal());
        assertEquals(Vista.NENHUMA, guardado.getVista());
        assertTrue(guardado.isAtivo());
    }

    // ---- Mudar a configuração do tipo ----

    @Test
    void mudarOTipoAtualizaOsQuartosQueHerdamEDeixaAsExcecoes() {
        configurarDuplo("80");
        Quarto herda = quartoHerdado(205);
        Quarto soPreco = controller.criarQuartoDoTipo(206, "", TipoQuarto.DUPLO, Vista.NENHUMA,
                new BigDecimal("95"), null, null);
        Quarto soEquipamentos = controller.criarQuartoDoTipo(207, "", TipoQuarto.DUPLO, Vista.NENHUMA, null,
                List.of(cofre), List.of());
        Quarto excecoes = controller.criarQuartoDoTipo(208, "", TipoQuarto.DUPLO, Vista.NENHUMA,
                new BigDecimal("70"), List.of(), List.of());

        int alterados = controller.guardarConfiguracaoTipo(TipoQuarto.DUPLO, new BigDecimal("90"),
                List.of(frigobar, cofre), List.of());

        assertEquals(3, alterados, "o quarto 208 só tem exceções");
        Quarto q205 = quartos.procurarPorId(herda.getId()).orElseThrow();
        assertValor("90.00", q205.getPrecoBase());
        assertEquals(2, q205.getEquipamentos().size());
        assertTrue(q205.getProdutos().isEmpty());

        Quarto q206 = quartos.procurarPorId(soPreco.getId()).orElseThrow();
        assertValor("95.00", q206.getPrecoBase());
        assertEquals(2, q206.getEquipamentos().size());

        Quarto q207 = quartos.procurarPorId(soEquipamentos.getId()).orElseThrow();
        assertValor("90.00", q207.getPrecoBase());
        assertEquals(List.of(cofre), q207.getEquipamentos());

        Quarto q208 = quartos.procurarPorId(excecoes.getId()).orElseThrow();
        assertValor("70.00", q208.getPrecoBase());
        assertTrue(q208.getEquipamentos().isEmpty());
    }

    @Test
    void mudarOTipoNaoMexeNosQuartosDeOutrosTipos() {
        configurarDuplo("80");
        controller.guardarConfiguracaoTipo(TipoQuarto.INDIVIDUAL, new BigDecimal("50"), List.of(), List.of());
        Quarto individual = controller.criarQuartoDoTipo(201, "", TipoQuarto.INDIVIDUAL, Vista.NENHUMA, null, null, null);

        int alterados = controller.guardarConfiguracaoTipo(TipoQuarto.DUPLO, new BigDecimal("90"), List.of(), List.of());

        assertEquals(0, alterados);
        assertValor("50.00", quartos.procurarPorId(individual.getId()).orElseThrow().getPrecoBase());
    }

    @Test
    void guardarAMesmaConfiguracaoNaoAlteraQuartos() {
        configurarDuplo("80");
        quartoHerdado(205);

        assertEquals(0, controller.guardarConfiguracaoTipo(TipoQuarto.DUPLO, new BigDecimal("80"), List.of(frigobar),
                List.of(new ProdutoQuarto(toalhas, 2), new ProdutoQuarto(televisao, 1))));
    }

    // ---- Quartos criados sem herança (comportamento anterior) ----

    @Test
    void quartoCriadoComValoresExplicitosTemExcecoesENaoMudaComOTipo() {
        configurarDuplo("80");
        Quarto quarto = controller.criarQuarto(205, "", TipoQuarto.DUPLO, new BigDecimal("60"), Vista.NENHUMA, List.of());
        assertTrue(quarto.isPrecoPersonalizado());
        assertTrue(quarto.isEquipamentosPersonalizados());

        controller.guardarConfiguracaoTipo(TipoQuarto.DUPLO, new BigDecimal("90"), List.of(), List.of());

        assertValor("60.00", quartos.procurarPorId(quarto.getId()).orElseThrow().getPrecoBase());
    }

    @Test
    void editarQuartoComValoresExplicitosPassaAExcecao() {
        configurarDuplo("80");
        Quarto quarto = quartoHerdado(205);

        controller.editarQuarto(quarto.getId(), "", TipoQuarto.DUPLO, new BigDecimal("75"), Vista.NENHUMA, true,
                List.of(frigobar));

        Quarto guardado = quartos.procurarPorId(quarto.getId()).orElseThrow();
        assertTrue(guardado.isPrecoPersonalizado());
        assertTrue(guardado.isEquipamentosPersonalizados());
        assertValor("75.00", guardado.getPrecoBase());
    }

    // ---- Permissões ----

    @Test
    void soOGestorAcedeAConfiguracaoPorTipo() {
        configurarDuplo("80");
        Quarto quarto = quartoHerdado(205);

        for (Perfil perfil : new Perfil[]{Perfil.RECECIONISTA, Perfil.FORNECEDOR, Perfil.EMPRESA_RESTAURACAO}) {
            sessao.iniciar(Utilizador.novo("outro", "outro12345", "Outro", perfil));
            assertThrows(SecurityException.class, () -> controller.consultarConfiguracaoTipo(TipoQuarto.DUPLO), "" + perfil);
            assertThrows(SecurityException.class, () -> controller.guardarConfiguracaoTipo(TipoQuarto.DUPLO,
                    BigDecimal.TEN, List.of(), List.of()), "" + perfil);
            assertThrows(SecurityException.class, () -> controller.criarQuartoDoTipo(206, "", TipoQuarto.DUPLO,
                    Vista.NENHUMA, null, null, null), "" + perfil);
            assertThrows(SecurityException.class, () -> controller.editarQuartoDoTipo(quarto.getId(), "",
                    TipoQuarto.DUPLO, Vista.NENHUMA, true, null, null, null), "" + perfil);
        }
        sessao.terminar();
        assertThrows(SecurityException.class, () -> controller.consultarConfiguracaoTipo(TipoQuarto.DUPLO));
        assertValor("80.00", quartos.procurarPorId(quarto.getId()).orElseThrow().getPrecoBase());
    }
}
