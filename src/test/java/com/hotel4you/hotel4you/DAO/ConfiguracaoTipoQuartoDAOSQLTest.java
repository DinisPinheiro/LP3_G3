package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.Model.Artigo;
import com.hotel4you.hotel4you.Model.ConfiguracaoTipoQuarto;
import com.hotel4you.hotel4you.Model.Equipamento;
import com.hotel4you.hotel4you.Model.ProdutoQuarto;
import com.hotel4you.hotel4you.Model.Quarto;
import com.hotel4you.hotel4you.Model.enums.TipoQuarto;
import com.hotel4you.hotel4you.Model.enums.Vista;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * S6-09 nos DAO SQL, numa base de dados H2 em memória (nunca na base de dados do ISEP): a configuração por
 * tipo de quarto ({@code dbo.ConfigTipoQuarto*}) e as exceções guardadas no quarto.
 */
class ConfiguracaoTipoQuartoDAOSQLTest {

    private BaseDadosH2 bd;
    private final ConfiguracaoTipoQuartoDAO configuracaoDAO = new ConfiguracaoTipoQuartoDAOSQL();
    private final QuartoDAO quartoDAO = new QuartoDAOSQL();
    private Equipamento frigobar;
    private Equipamento cofre;
    private Artigo toalhas;

    @BeforeEach
    void preparar() throws SQLException {
        bd = BaseDadosH2.comTabelas("configtipos6");
        bd.executar(
                "INSERT INTO dbo.SuplementoVista VALUES ('NENHUMA', 0), ('MAR', 20), ('MONTANHA', 15)",
                "INSERT INTO dbo.Equipamento (Nome) VALUES ('Frigobar'), ('Cofre')",
                "INSERT INTO dbo.Artigo (Nome, Unidade, QuantidadeAtual) VALUES ('Toalha de banho', 'un', 10)");
        frigobar = new Equipamento(1, "Frigobar");
        cofre = new Equipamento(2, "Cofre");
        toalhas = new Artigo(1, "Toalha de banho", "un", 10);
    }

    @AfterEach
    void apagar() throws SQLException, IOException {
        bd.fechar();
    }

    private static void assertValor(String esperado, BigDecimal obtido) {
        assertEquals(0, new BigDecimal(esperado).compareTo(obtido), "esperado " + esperado + ", obtido " + obtido);
    }

    @Test
    void tipoSemConfiguracaoNaoDevolveNada() {
        assertTrue(configuracaoDAO.procurar(TipoQuarto.DUPLO).isEmpty());
        assertTrue(configuracaoDAO.listar().isEmpty());
    }

    @Test
    void guardaELeAConfiguracaoDeUmTipo() {
        configuracaoDAO.guardar(new ConfiguracaoTipoQuarto(TipoQuarto.DUPLO, new BigDecimal("80.00"),
                List.of(frigobar, cofre), List.of(new ProdutoQuarto(toalhas, 2))));

        ConfiguracaoTipoQuarto lida = configuracaoDAO.procurar(TipoQuarto.DUPLO).orElseThrow();
        assertValor("80.00", lida.getPrecoBase());
        assertEquals(2, lida.getEquipamentos().size());
        assertEquals(1, lida.getProdutos().size());
        assertEquals("Toalha de banho", lida.getProdutos().get(0).getArtigo().getNome());
        assertEquals(2, lida.getProdutos().get(0).getQuantidade());
        assertTrue(configuracaoDAO.procurar(TipoQuarto.INDIVIDUAL).isEmpty());
    }

    @Test
    void guardarDeNovoSubstituiAConfiguracao() {
        configuracaoDAO.guardar(new ConfiguracaoTipoQuarto(TipoQuarto.DUPLO, new BigDecimal("80.00"),
                List.of(frigobar, cofre), List.of(new ProdutoQuarto(toalhas, 2))));

        configuracaoDAO.guardar(new ConfiguracaoTipoQuarto(TipoQuarto.DUPLO, new BigDecimal("90.00"),
                List.of(cofre), List.of()));

        ConfiguracaoTipoQuarto lida = configuracaoDAO.procurar(TipoQuarto.DUPLO).orElseThrow();
        assertValor("90.00", lida.getPrecoBase());
        assertEquals(List.of("Cofre"), lida.getEquipamentos().stream().map(Equipamento::getNome).toList());
        assertTrue(lida.getProdutos().isEmpty());
        assertEquals(1, configuracaoDAO.listar().size());
    }

    @Test
    void listaUmaConfiguracaoPorTipo() {
        configuracaoDAO.guardar(new ConfiguracaoTipoQuarto(TipoQuarto.FAMILIA, BigDecimal.TEN, List.of(), List.of()));
        configuracaoDAO.guardar(new ConfiguracaoTipoQuarto(TipoQuarto.INDIVIDUAL, BigDecimal.ONE, List.of(), List.of()));

        assertEquals(List.of(TipoQuarto.INDIVIDUAL, TipoQuarto.FAMILIA),
                configuracaoDAO.listar().stream().map(ConfiguracaoTipoQuarto::getTipo).toList());
    }

    @Test
    void recusaEquipamentosEProdutosQueNaoEstaoGuardados() {
        assertThrows(DAOException.class, () -> configuracaoDAO.guardar(new ConfiguracaoTipoQuarto(TipoQuarto.DUPLO,
                BigDecimal.TEN, List.of(new Equipamento(0, "Novo")), List.of())));
        assertThrows(DAOException.class, () -> configuracaoDAO.guardar(new ConfiguracaoTipoQuarto(TipoQuarto.DUPLO,
                BigDecimal.TEN, List.of(), List.of(new ProdutoQuarto(new Artigo(0, "Novo", "un", 1), 1)))));
        assertTrue(configuracaoDAO.procurar(TipoQuarto.DUPLO).isEmpty());
    }

    // ---- Exceções guardadas no quarto ----

    @Test
    void quartoGuardaSeOPrecoEOsEquipamentosSaoExcecoes() {
        ConfiguracaoTipoQuarto duplo = new ConfiguracaoTipoQuarto(TipoQuarto.DUPLO, new BigDecimal("80.00"),
                List.of(frigobar), List.of());
        Quarto herda = new Quarto(0, 205, 2, "", TipoQuarto.DUPLO, BigDecimal.ZERO, Vista.NENHUMA, true);
        herda.herdarPreco(duplo);
        herda.herdarEquipamentos(duplo);
        Quarto excecao = new Quarto(0, 206, 2, "", TipoQuarto.DUPLO, new BigDecimal("95.00"), Vista.NENHUMA, true);
        quartoDAO.inserir(herda);
        quartoDAO.inserir(excecao);

        Quarto lidoHerda = quartoDAO.procurarPorNumero(205).orElseThrow();
        assertFalse(lidoHerda.isPrecoPersonalizado());
        assertFalse(lidoHerda.isEquipamentosPersonalizados());
        assertValor("80.00", lidoHerda.getPrecoBase());
        assertEquals(1, lidoHerda.getEquipamentos().size());

        Quarto lidoExcecao = quartoDAO.procurarPorNumero(206).orElseThrow();
        assertTrue(lidoExcecao.isPrecoPersonalizado());
        assertTrue(lidoExcecao.isEquipamentosPersonalizados());
    }

    @Test
    void atualizarQuartoGuardaAsExcecoes() {
        ConfiguracaoTipoQuarto duplo = new ConfiguracaoTipoQuarto(TipoQuarto.DUPLO, new BigDecimal("80.00"),
                List.of(frigobar), List.of());
        Quarto quarto = new Quarto(0, 205, 2, "", TipoQuarto.DUPLO, BigDecimal.ZERO, Vista.NENHUMA, true);
        quarto.herdarPreco(duplo);
        quarto.herdarEquipamentos(duplo);
        quartoDAO.inserir(quarto);

        quarto.personalizarPreco(new BigDecimal("99.00"));
        quartoDAO.atualizar(quarto);

        Quarto lido = quartoDAO.procurarPorNumero(205).orElseThrow();
        assertTrue(lido.isPrecoPersonalizado());
        assertFalse(lido.isEquipamentosPersonalizados());
        assertValor("99.00", lido.getPrecoBase());
    }

    @Test
    void quartosJaExistentesFicamComExcecoes() throws SQLException {
        // Linha criada sem indicar as colunas novas, como as que já existiam antes da S6-09.
        bd.executar("INSERT INTO dbo.Quarto (Numero, Piso, Local, Tipo, PrecoBase, Vista, Ativo) VALUES "
                + "(207, 2, '', 'DUPLO', 70, 'NENHUMA', 1)");

        Quarto lido = quartoDAO.procurarPorNumero(207).orElseThrow();

        assertTrue(lido.isPrecoPersonalizado());
        assertTrue(lido.isEquipamentosPersonalizados());
        assertValor("70.00", lido.getPrecoBase());
    }
}
