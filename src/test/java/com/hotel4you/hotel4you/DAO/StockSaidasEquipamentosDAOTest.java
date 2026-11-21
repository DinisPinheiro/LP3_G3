package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.Model.Artigo;
import com.hotel4you.hotel4you.Model.MovimentoStock;
import com.hotel4you.hotel4you.Model.ProdutoQuarto;
import com.hotel4you.hotel4you.Model.Quarto;
import com.hotel4you.hotel4you.Model.Reserva;
import com.hotel4you.hotel4you.Model.Utilizador;
import com.hotel4you.hotel4you.Model.enums.Perfil;
import com.hotel4you.hotel4you.Model.enums.TipoMovimento;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * S6-04 e S6-05 nos DAO SQL, numa base de dados H2 em memória (nunca na base de dados do ISEP):
 * saídas e histórico, consumíveis, stock em uso só nas reservas ativas, produtos do quarto
 * (dbo.QuartoProduto) e produtos regravados ao atualizar a reserva.
 */
class StockSaidasEquipamentosDAOTest {

    private BaseDadosH2 bd;
    private final StockDAO stockDAO = new StockDAOSQL();
    private final QuartoDAO quartoDAO = new QuartoDAOSQL();
    private final ReservaDAO reservaDAO = new ReservaDAOSQL();
    private Utilizador gestor;

    @BeforeEach
    void preparar() throws SQLException {
        bd = BaseDadosH2.comTabelas("stocks6");
        bd.executar(
                "INSERT INTO dbo.SuplementoVista VALUES ('NENHUMA', 0), ('MAR', 20), ('MONTANHA', 15)",
                "INSERT INTO dbo.Utilizador (Username, PasswordHash, Nome, Perfil, Ativo) "
                        + "VALUES ('gestor', 'hash', 'Gestor Inicial', 'GESTOR', 1)",
                "INSERT INTO dbo.Cliente (Nome, Nif, Contacto) VALUES ('Ana Silva', '123456789', '910000001')",
                "INSERT INTO dbo.Quarto (Numero, Piso, Local, Tipo, PrecoBase, Vista, Ativo) VALUES "
                        + "(205, 2, 'Ala poente', 'DUPLO', 80, 'NENHUMA', 1)",
                "INSERT INTO dbo.Artigo (Nome, Unidade, QuantidadeAtual) VALUES "
                        + "('Champô', 'un', 10), ('Televisão', 'un', 2), ('Toalha de banho', 'un', 10)",
                "UPDATE dbo.Artigo SET Consumivel = 1 WHERE Nome = 'Champô'",
                "INSERT INTO dbo.CartaoMagnetico (Numero, Mestre, Ativo) VALUES (1001, 0, 1)");
        gestor = new Utilizador(1, "gestor", "hash", "Gestor Inicial", Perfil.GESTOR, true);
    }

    @AfterEach
    void apagar() throws SQLException, IOException {
        bd.fechar();
    }

    private Artigo artigo(String nome) {
        return stockDAO.listarArtigos().stream().filter(a -> a.getNome().equals(nome)).findFirst().orElseThrow();
    }

    private void reserva(String estado, int idArtigo, int quantidade) throws SQLException {
        bd.executar("INSERT INTO dbo.Reserva (IdCliente, IdQuarto, DataEntrada, DataSaida, Estado, ValorTotal, "
                        + "PrecoBaseNoite, ValorVistaNoite) VALUES (1, 1, DATE '2026-12-01', DATE '2026-12-03', '"
                        + estado + "', 160, 80, 0)",
                "INSERT INTO dbo.ReservaProduto (IdReserva, IdArtigo, Quantidade) "
                        + "VALUES ((SELECT MAX(IdReserva) FROM dbo.Reserva), " + idArtigo + ", " + quantidade + ")");
    }

    // ================= S6-04 =================

    @Test
    void listarArtigosTrazOConsumivelEOEmUsoSoDasReservasAtivas() throws SQLException {
        int idToalhas = artigo("Toalha de banho").getId();
        reserva("PENDENTE", idToalhas, 1);
        reserva("CHECK_IN", idToalhas, 2);
        reserva("CHECK_OUT", idToalhas, 4);   // já devolveu
        reserva("CANCELADA", idToalhas, 3);   // nunca usou

        Artigo toalhas = artigo("Toalha de banho");
        assertEquals(10, toalhas.getQuantidadeAtual());
        assertEquals(3, toalhas.getQuantidadeReservada());
        assertEquals(7, toalhas.getQuantidadeDisponivel());
        assertTrue(artigo("Champô").isConsumivel());
        assertFalse(toalhas.isConsumivel());
    }

    @Test
    void saidaDescontaOStockRealEFicaNoHistorico() {
        Artigo tv = artigo("Televisão");

        stockDAO.registarSaidas(List.of(new MovimentoStock(TipoMovimento.SAIDA, 1, LocalDateTime.of(2026, 11, 21, 10, 0),
                gestor, tv, "Televisão avariada")));

        assertEquals(1, artigo("Televisão").getQuantidadeAtual());
        List<MovimentoStock> historico = stockDAO.listarSaidas();
        assertEquals(1, historico.size());
        assertEquals("Televisão", historico.get(0).getArtigo().getNome());
        assertEquals("Televisão avariada", historico.get(0).getJustificacao());
        assertEquals("Gestor Inicial", historico.get(0).getUtilizador().getNome());
        assertEquals(LocalDateTime.of(2026, 11, 21, 10, 0), historico.get(0).getDataHora());
    }

    @Test
    void saidasGuardamSeTodasOuNenhuma() {
        Artigo tv = artigo("Televisão");
        Artigo toalhas = artigo("Toalha de banho");
        LocalDateTime agora = LocalDateTime.now();

        DAOException erro = assertThrows(DAOException.class, () -> stockDAO.registarSaidas(List.of(
                new MovimentoStock(TipoMovimento.SAIDA, 1, agora, gestor, toalhas, "Rasgada"),
                new MovimentoStock(TipoMovimento.SAIDA, 5, agora, gestor, tv, "Avaria"))));

        assertTrue(erro.getMessage().contains("Televisão"));
        assertEquals(10, artigo("Toalha de banho").getQuantidadeAtual(), "a primeira saída foi revertida");
        assertTrue(stockDAO.listarSaidas().isEmpty());
    }

    @Test
    void consumoDescontaNoMaximoOQueExisteEEntradaNaoEAceite() {
        Artigo champo = artigo("Champô");

        stockDAO.registarSaidas(List.of(new MovimentoStock(TipoMovimento.UTILIZACAO, 12, LocalDateTime.now(),
                gestor, champo, "Consumo na reserva n.º 1")));

        assertEquals(0, artigo("Champô").getQuantidadeAtual());
        assertEquals(TipoMovimento.UTILIZACAO, stockDAO.listarSaidas().get(0).getTipo());
        assertThrows(IllegalArgumentException.class, () -> stockDAO.registarSaidas(List.of(
                new MovimentoStock(TipoMovimento.ENTRADA, 1, LocalDateTime.now(), gestor, champo))));
    }

    @Test
    void definirConsumivelGuardaNaBaseDeDados() {
        int id = artigo("Televisão").getId();

        stockDAO.definirConsumivel(id, true);
        assertTrue(artigo("Televisão").isConsumivel());

        assertThrows(DAOException.class, () -> stockDAO.definirConsumivel(999, true));
    }

    // ================= S6-05 =================

    @Test
    void produtosDoQuartoSaoGuardadosELidosComAQuantidade() {
        Quarto quarto = quartoDAO.procurarPorId(1).orElseThrow();
        quarto.definirProduto(new ProdutoQuarto(artigo("Toalha de banho"), 2));
        quarto.definirProduto(new ProdutoQuarto(artigo("Champô"), 2));

        quartoDAO.atualizar(quarto);

        Quarto lido = quartoDAO.procurarPorId(1).orElseThrow();
        assertEquals(2, lido.getProdutos().size());
        assertEquals("Champô", lido.getProdutos().get(0).getArtigo().getNome());
        assertTrue(lido.getProdutos().get(0).getArtigo().isConsumivel());
        assertEquals(2, lido.getProdutos().get(1).getQuantidade());

        lido.limparProdutos();
        quartoDAO.atualizar(lido);
        assertTrue(quartoDAO.procurarPorId(1).orElseThrow().getProdutos().isEmpty());
    }

    @Test
    void checkInGuardaOsProdutosDoQuartoNaReserva() throws SQLException {
        Quarto quarto = quartoDAO.procurarPorId(1).orElseThrow();
        quarto.definirProduto(new ProdutoQuarto(artigo("Toalha de banho"), 2));
        quartoDAO.atualizar(quarto);
        bd.executar("INSERT INTO dbo.Reserva (IdCliente, IdQuarto, DataEntrada, DataSaida, Estado, ValorTotal, "
                + "PrecoBaseNoite, ValorVistaNoite) VALUES (1, 1, DATE '2026-12-01', DATE '2026-12-03', 'PENDENTE', 160, 80, 0)");
        Reserva reserva = reservaDAO.procurarPorId(1).orElseThrow();

        reserva.fazerCheckIn(new com.hotel4you.hotel4you.Model.CartaoMagnetico(1, 1001, false, true),
                LocalDateTime.now(), stockDAO.listarArtigos());
        reservaDAO.atualizar(reserva);

        assertEquals(2, reservaDAO.procurarPorId(1).orElseThrow().getProdutos().get(0).getQuantidade());
        assertEquals(8, artigo("Toalha de banho").getQuantidadeDisponivel());
    }

    @Test
    void atualizarAReservaNaoAceitaMaisDoQueOStock() throws SQLException {
        int idTv = artigo("Televisão").getId();
        reserva("CHECK_IN", idTv, 1);
        Reserva reserva = reservaDAO.procurarPorId(1).orElseThrow();
        Artigo tv = artigo("Televisão");
        tv.setQuantidadeReservada(0);   // simula uma leitura antiga: o model deixa passar, o DAO não

        reserva.alterarProduto(tv, 3);

        assertThrows(DAOException.class, () -> reservaDAO.atualizar(reserva));
        assertEquals(1, reservaDAO.procurarPorId(1).orElseThrow().getProdutos().get(0).getQuantidade(),
                "nada foi guardado");
    }
}
