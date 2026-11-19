package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.DAO.db.DBConnection;
import com.hotel4you.hotel4you.Model.Artigo;
import com.hotel4you.hotel4you.Model.Cliente;
import com.hotel4you.hotel4you.Model.Reserva;
import com.hotel4you.hotel4you.Model.enums.EstadoReserva;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Testes do fecho das ligações nos DAO SQL usados pelos ecrãs que bloqueavam (S6-01): reservas ativas,
 * histórico, stock e fornecedores. Usam uma base de dados H2 em memória, nunca a base de dados do ISEP.
 */
class LigacoesDAOTest {

    private static final List<EstadoReserva> ATIVAS = List.of(EstadoReserva.PENDENTE, EstadoReserva.CHECK_IN);
    private static final List<EstadoReserva> HISTORICO = List.of(EstadoReserva.CHECK_OUT, EstadoReserva.CANCELADA);

    private static BaseDadosH2 bd;

    private final ReservaDAO reservaDAO = new ReservaDAOSQL();
    private final StockDAO stockDAO = new StockDAOSQL();
    private final FornecedorDAO fornecedorDAO = new FornecedorDAOSQL();
    private final ClienteDAO clienteDAO = new ClienteDAOSQL();

    /** 3 clientes, 3 quartos e 12 reservas (8 ativas e 4 no histórico), com serviços, produtos e um cartão. */
    @BeforeAll
    static void criarDados() throws SQLException {
        bd = BaseDadosH2.comTabelas("ligacoesdao");
        bd.executar(
                "INSERT INTO dbo.SuplementoVista VALUES ('NENHUMA', 0), ('MAR', 20), ('MONTANHA', 15)",
                "INSERT INTO dbo.Cliente (Nome, Nif, Contacto) VALUES ('Ana Silva', '123456789', '910000001'), "
                        + "('Bruno Costa', '234567890', '910000002'), ('Carla Dias', '345678901', '910000003')",
                "INSERT INTO dbo.Quarto (Numero, Piso, Local, Tipo, PrecoBase, Vista, Ativo) VALUES "
                        + "(201, 2, 'Ala norte', 'DUPLO', 80, 'MAR', 1), (202, 2, 'Ala norte', 'INDIVIDUAL', 50, 'NENHUMA', 1), "
                        + "(301, 3, 'Ala sul', 'FAMILIA', 120, 'MONTANHA', 1)",
                "INSERT INTO dbo.Equipamento (Nome) VALUES ('Televisão'), ('Minibar')",
                "INSERT INTO dbo.Quarto_Equipamento VALUES (1, 1), (1, 2), (3, 1)",
                "INSERT INTO dbo.CartaoMagnetico (Numero, Mestre, Ativo) VALUES (1001, 0, 1)",
                "INSERT INTO dbo.Servico (Nome, Categoria, Descricao, Tipo, Valor, Cobranca, Ativo) VALUES "
                        + "('Pequeno-almoço', 'Restauração', '', 'SERVICO', 8, 'POR_NOITE', 1)",
                "INSERT INTO dbo.Fornecedor (Nome, Nif, Contacto) VALUES ('Product Hero', '500000000', '')",
                "INSERT INTO dbo.Artigo (Nome, Unidade, QuantidadeAtual, IdFornecedor, PrecoUnitario, UnidadePreco, "
                        + "TaxaPercent) VALUES ('Champô', 'un', 600, 1, 1.20, 'un', 23)");
        for (int i = 0; i < 12; i++) {
            String estado = i < 6 ? "PENDENTE" : i < 8 ? "CHECK_IN" : i < 10 ? "CHECK_OUT" : "CANCELADA";
            String cartao = "CHECK_IN".equals(estado) ? "1" : "NULL";
            String checkIn = "PENDENTE".equals(estado) || "CANCELADA".equals(estado) ? "NULL" : "'2026-12-01 14:00:00'";
            String checkOut = "CHECK_OUT".equals(estado) ? "'2026-12-03 11:00:00'" : "NULL";
            bd.executar("INSERT INTO dbo.Reserva (IdCliente, IdQuarto, DataEntrada, DataSaida, Estado, ValorTotal, "
                    + "DataCheckIn, DataCheckOut, IdCartao, PrecoBaseNoite, ValorVistaNoite) VALUES ("
                    + (i % 3 + 1) + ", " + (i % 3 + 1) + ", DATEADD(DAY, " + (i * 3) + ", DATE '2026-12-01'), "
                    + "DATEADD(DAY, " + (i * 3 + 2) + ", DATE '2026-12-01'), '" + estado + "', 0, "
                    + checkIn + ", " + checkOut + ", " + cartao + ", 80, 20)");
        }
        bd.executar(
                "INSERT INTO dbo.ConsumoServico (IdReserva, IdServico, Quantidade, Data, ValorUnitario, Cobranca) "
                        + "VALUES (1, 1, 2, DATE '2026-12-01', 8, 'POR_NOITE')",
                "INSERT INTO dbo.ReservaProduto (IdReserva, IdArtigo, Quantidade) VALUES (1, 1, 2)");
    }

    @AfterAll
    static void apagarDados() throws SQLException, IOException {
        bd.fechar();
    }

    @Test
    void paginaDeReservasAbreUmaSoLigacaoEVemCompleta() throws SQLException {
        int antes = DBConnection.totalLigacoes();

        List<Reserva> pagina = reservaDAO.listarPorEstados(ATIVAS, 1, 10);

        // Antes da S6-01 esta página abria 1 + 3 (clientes) + 3 (quartos) + 1 (cartão) = 8 ligações.
        assertEquals(1, DBConnection.totalLigacoes() - antes);
        assertEquals(8, pagina.size());
        for (Reserva reserva : pagina) {
            assertNotNull(reserva.getCliente().getNome());
            assertNotNull(reserva.getQuarto());
            if (reserva.getEstado() == EstadoReserva.CHECK_IN) {
                assertEquals(1001, reserva.getCartao().getNumero());
            }
        }
        assertEquals(0, bd.ligacoesAbertas());
    }

    @Test
    void reservaLidaTrazQuartoComEquipamentosServicosEProdutos() {
        Reserva primeira = reservaDAO.procurarPorId(1).orElseThrow();

        assertEquals("Ana Silva", primeira.getCliente().getNome());
        assertEquals(201, primeira.getQuarto().getNumero());
        assertEquals(2, primeira.getQuarto().getEquipamentos().size());
        assertEquals(1, primeira.getConsumos().size());
        assertEquals(1, primeira.getProdutos().size());
    }

    @Test
    void consultasDosEcrasQueBloqueavamFechamTodasAsLigacoes() throws SQLException {
        assertEquals(8, reservaDAO.contarPorEstados(ATIVAS));
        assertEquals(4, reservaDAO.listarPorEstados(HISTORICO, 1, 10).size());
        assertEquals(4, reservaDAO.contarPorEstados(HISTORICO));
        List<Artigo> stock = stockDAO.listarArtigos();
        assertEquals(1, stock.size());
        assertEquals(598, stock.get(0).getQuantidadeDisponivel());
        assertEquals(1, fornecedorDAO.listarTodos().size());

        assertEquals(0, bd.ligacoesAbertas());
    }

    @Test
    void operacaoQueFalhaTambemFechaALigacao() throws SQLException {
        Cliente repetido = new Cliente(0, "Outra Ana", "123456789", "");

        assertThrows(DAOException.class, () -> clienteDAO.inserir(repetido));
        assertThrows(DAOException.class, () -> reservaDAO.remover(999));

        assertEquals(0, bd.ligacoesAbertas());
    }
}
