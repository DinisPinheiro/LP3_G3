package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.DAO.db.DBConnection;
import com.hotel4you.hotel4you.Model.Artigo;
import com.hotel4you.hotel4you.Model.Fornecedor;
import com.hotel4you.hotel4you.Model.MovimentoStock;
import com.hotel4you.hotel4you.Model.Utilizador;
import com.hotel4you.hotel4you.Model.encomenda.EncomendaFornecedor;
import com.hotel4you.hotel4you.Model.encomenda.LinhaEncomenda;
import com.hotel4you.hotel4you.Model.encomenda.TotaisEncomenda;
import com.hotel4you.hotel4you.Model.enums.Perfil;
import com.hotel4you.hotel4you.Model.enums.TipoMovimento;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@link StockDAO} sobre as tabelas {@code dbo.Fornecedor}, {@code dbo.Artigo}, {@code dbo.MovimentoStock},
 * {@code dbo.Encomenda} e {@code dbo.EncomendaLinha} (S5-04, ver {@code EsquemaDB_S5_stock_completo.sql}).
 * A quantidade usada em reservas vem da tabela {@code dbo.ReservaProduto} (S5-06).
 * <p>
 * S6-04: as saídas de stock e os consumos no check-out ficam em {@code dbo.MovimentoStock}, com a
 * justificação; a coluna {@code dbo.Artigo.Consumivel} indica os produtos que se gastam com o uso
 * (ver {@code EsquemaDB_S6_stock_saidas_equipamentos.sql}). Só as reservas pendentes e com check-in
 * contam como stock em uso.
 */
public class StockDAOSQL implements StockDAO {

    private static final String SQL_FORNECEDOR_POR_IDENTIFICADOR =
            "SELECT IdFornecedor FROM dbo.Fornecedor WHERE Nif = ?";
    private static final String SQL_INSERIR_FORNECEDOR =
            "INSERT INTO dbo.Fornecedor (Nome, Nif, Contacto) VALUES (?, ?, ?)";
    private static final String SQL_ARTIGO_POR_NOME =
            "SELECT IdArtigo, Nome, Unidade, QuantidadeAtual FROM dbo.Artigo WHERE Nome = ?";
    private static final String SQL_INSERIR_ARTIGO =
            "INSERT INTO dbo.Artigo (Nome, Unidade, QuantidadeAtual, IdFornecedor) VALUES (?, ?, 0, ?)";
    private static final String SQL_ATUALIZAR_ARTIGO =
            "UPDATE dbo.Artigo SET QuantidadeAtual = ?, IdFornecedor = ? WHERE IdArtigo = ?";
    /** Como o anterior, mas guarda também o último preço unitário, a unidade do preço e a taxa do artigo. */
    private static final String SQL_ATUALIZAR_ARTIGO_COM_PRECO =
            "UPDATE dbo.Artigo SET QuantidadeAtual = ?, IdFornecedor = ?, PrecoUnitario = ?, UnidadePreco = ?, "
                    + "TaxaPercent = ? WHERE IdArtigo = ?";
    private static final String SQL_INSERIR_ENCOMENDA =
            "INSERT INTO dbo.Encomenda (Numero, DataEncomenda, IdFornecedor, IdUtilizador, DataEntrada, Moeda, "
                    + "TotalLinhas, TotalTaxas, TotalComTaxas) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_INSERIR_LINHA_ENCOMENDA =
            "INSERT INTO dbo.EncomendaLinha (IdEncomenda, Numero, CodigoProduto, IdArtigo, Quantidade, Unidade, "
                    + "QuantidadeCaixas, QuantidadeUnidades, PrecoUnitario, UnidadePreco, TaxaPercent, "
                    + "ValorSemTaxa, ValorTaxa, TotalLinha) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_INSERIR_MOVIMENTO =
            "INSERT INTO dbo.MovimentoStock (IdArtigo, IdUtilizador, Tipo, Quantidade, DataHora, Justificacao) "
                    + "VALUES (?, ?, ?, ?, ?, ?)";
    /** Saída: só desconta se o stock real chegar (nunca fica negativo). */
    private static final String SQL_DESCONTAR_SAIDA =
            "UPDATE dbo.Artigo SET QuantidadeAtual = QuantidadeAtual - ? WHERE IdArtigo = ? AND QuantidadeAtual >= ?";
    /** Consumo no check-out: desconta no máximo o que existe. */
    private static final String SQL_DESCONTAR_CONSUMO =
            "UPDATE dbo.Artigo SET QuantidadeAtual = CASE WHEN QuantidadeAtual >= ? THEN QuantidadeAtual - ? ELSE 0 END "
                    + "WHERE IdArtigo = ?";
    private static final String SQL_STOCK_DO_ARTIGO =
            "SELECT Nome, QuantidadeAtual FROM dbo.Artigo WHERE IdArtigo = ?";
    private static final String SQL_LISTAR_SAIDAS =
            "SELECT m.Tipo, m.Quantidade, m.DataHora, m.Justificacao, "
                    + "a.IdArtigo, a.Nome AS NomeArtigo, a.Unidade, a.QuantidadeAtual, a.Consumivel, "
                    + "u.IdUtilizador, u.Username, u.PasswordHash, u.Nome AS NomeUtilizador, u.Perfil, u.Ativo "
                    + "FROM dbo.MovimentoStock m "
                    + "JOIN dbo.Artigo a ON a.IdArtigo = m.IdArtigo "
                    + "JOIN dbo.Utilizador u ON u.IdUtilizador = m.IdUtilizador "
                    + "WHERE m.Tipo IN ('SAIDA', 'UTILIZACAO') "
                    + "ORDER BY m.DataHora DESC, m.IdMovimento DESC";
    /**
     * Quantidade do artigo {@code a} em uso nas reservas pendentes e com check-in (S6-05): as canceladas
     * nunca usaram stock e as que já fizeram check-out devolveram-no.
     * Também é usada pelo {@link ReservaDAOSQL} para confirmar o stock ao guardar uma reserva.
     */
    static final String SQL_USADO_EM_RESERVAS =
            "(SELECT ISNULL(SUM(rp.Quantidade), 0) FROM dbo.ReservaProduto rp "
                    + "JOIN dbo.Reserva r ON r.IdReserva = rp.IdReserva "
                    + "WHERE rp.IdArtigo = a.IdArtigo AND r.Estado IN ('PENDENTE', 'CHECK_IN'))";
    private static final String SQL_LISTAR_ARTIGOS =
            "SELECT a.IdArtigo, a.Nome, a.Unidade, a.QuantidadeAtual, a.Consumivel, "
                    + SQL_USADO_EM_RESERVAS + " AS UsadoEmReservas, "
                    + "a.PrecoUnitario, a.UnidadePreco, a.TaxaPercent, "
                    + "f.IdFornecedor, f.Nome AS NomeFornecedor, f.Nif, f.Contacto "
                    + "FROM dbo.Artigo a LEFT JOIN dbo.Fornecedor f ON f.IdFornecedor = a.IdFornecedor "
                    + "ORDER BY a.Nome";

    @Override
    public void registarEntrada(Fornecedor fornecedor, EncomendaFornecedor encomenda,
                                List<MovimentoStock> entradas) {
        if (encomenda != null && encomenda.linhas().size() != entradas.size()) {
            throw new IllegalArgumentException("Tem de haver uma entrada por cada linha da encomenda.");
        }
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                guardarFornecedor(conn, fornecedor);
                int idEncomenda = encomenda == null || entradas.isEmpty() ? 0
                        : guardarEncomenda(conn, fornecedor, encomenda, entradas.get(0));
                for (int i = 0; i < entradas.size(); i++) {
                    LinhaEncomenda linha = encomenda == null ? null : encomenda.linhas().get(i);
                    registarMovimento(conn, fornecedor, entradas.get(i), idEncomenda, linha);
                }
                conn.commit();
            } catch (SQLException e) {
                SqlUtil.reverter(conn, e);
                throw new DAOException("Não foi possível registar a entrada de stock.", e);
            }
        } catch (SQLException e) {
            throw new DAOException("Não foi possível registar a entrada de stock.", e);
        }
    }

    @Override
    public List<Artigo> listarArtigos() {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_LISTAR_ARTIGOS);
             ResultSet rs = ps.executeQuery()) {
            List<Artigo> artigos = new ArrayList<>();
            while (rs.next()) {
                artigos.add(mapearArtigo(rs));
            }
            return artigos;
        } catch (SQLException e) {
            throw new DAOException("Não foi possível consultar o stock.", e);
        }
    }

    @Override
    public void registarSaidas(List<MovimentoStock> saidas) {
        for (MovimentoStock saida : saidas) {
            if (saida.getTipo() == TipoMovimento.ENTRADA) {
                throw new IllegalArgumentException("As entradas registam-se com registarEntrada.");
            }
        }
        if (saidas.isEmpty()) {
            return;
        }
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                for (MovimentoStock saida : saidas) {
                    descontar(conn, saida);
                    inserirMovimento(conn, saida.getArtigo().getId(), saida);
                }
                conn.commit();
            } catch (SQLException e) {
                SqlUtil.reverter(conn, e);
                throw new DAOException("Não foi possível registar a saída de stock.", e);
            } catch (DAOException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DAOException("Não foi possível registar a saída de stock.", e);
        }
    }

    @Override
    public List<MovimentoStock> listarSaidas() {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_LISTAR_SAIDAS);
             ResultSet rs = ps.executeQuery()) {
            List<MovimentoStock> saidas = new ArrayList<>();
            while (rs.next()) {
                Artigo artigo = new Artigo(rs.getInt("IdArtigo"), rs.getString("NomeArtigo"),
                        rs.getString("Unidade"), rs.getInt("QuantidadeAtual"));
                artigo.setConsumivel(rs.getBoolean("Consumivel"));
                Utilizador utilizador = new Utilizador(rs.getInt("IdUtilizador"), rs.getString("Username"),
                        rs.getString("PasswordHash"), rs.getString("NomeUtilizador"),
                        Perfil.valueOf(rs.getString("Perfil")), rs.getBoolean("Ativo"));
                saidas.add(new MovimentoStock(TipoMovimento.valueOf(rs.getString("Tipo").trim()),
                        rs.getInt("Quantidade"), SqlUtil.lerDataHora(rs, "DataHora"), utilizador, artigo,
                        rs.getString("Justificacao")));
            }
            return saidas;
        } catch (SQLException e) {
            throw new DAOException("Não foi possível consultar o histórico de saídas.", e);
        }
    }

    @Override
    public void definirConsumivel(int idArtigo, boolean consumivel) {
        try (Connection conn = DBConnection.getConnection()) {
            int linhas = SqlUtil.executar(conn, "UPDATE dbo.Artigo SET Consumivel = ? WHERE IdArtigo = ?", ps -> {
                ps.setBoolean(1, consumivel);
                ps.setInt(2, idArtigo);
            });
            if (linhas == 0) {
                throw new DAOException("O artigo com id " + idArtigo + " não existe.");
            }
        } catch (SQLException e) {
            throw new DAOException("Não foi possível guardar o artigo.", e);
        }
    }

    /** Desconta a saída ou o consumo ao stock real; uma saída sem stock suficiente lança DAOException. */
    private static void descontar(Connection conn, MovimentoStock saida) throws SQLException {
        int id = saida.getArtigo().getId();
        int quantidade = saida.getQuantidade();
        int linhas;
        if (saida.getTipo() == TipoMovimento.SAIDA) {
            linhas = SqlUtil.executar(conn, SQL_DESCONTAR_SAIDA, ps -> {
                ps.setInt(1, quantidade);
                ps.setInt(2, id);
                ps.setInt(3, quantidade);
            });
        } else {
            linhas = SqlUtil.executar(conn, SQL_DESCONTAR_CONSUMO, ps -> {
                ps.setInt(1, quantidade);
                ps.setInt(2, quantidade);
                ps.setInt(3, id);
            });
        }
        if (linhas == 1) {
            return;
        }
        try (PreparedStatement ps = conn.prepareStatement(SQL_STOCK_DO_ARTIGO)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new DAOException("O artigo '" + saida.getArtigo().getNome() + "' já não existe.");
                }
                throw new DAOException("Stock insuficiente de '" + rs.getString("Nome") + "': existem "
                        + rs.getInt("QuantidadeAtual") + " e foram pedidos " + quantidade + ".");
            }
        }
    }

    private static void inserirMovimento(Connection conn, int idArtigo, MovimentoStock movimento) throws SQLException {
        SqlUtil.executar(conn, SQL_INSERIR_MOVIMENTO, ps -> {
            ps.setInt(1, idArtigo);
            ps.setInt(2, movimento.getUtilizador().getId());
            ps.setString(3, movimento.getTipo().name());
            ps.setInt(4, movimento.getQuantidade());
            SqlUtil.definirDataHora(ps, 5, movimento.getDataHora());
            ps.setString(6, movimento.getJustificacao());
        });
    }

    /** Usa o fornecedor já guardado com o mesmo identificador ou guarda-o como novo. */
    private static void guardarFornecedor(Connection conn, Fornecedor fornecedor) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_FORNECEDOR_POR_IDENTIFICADOR)) {
            ps.setString(1, fornecedor.getIdentificador());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    fornecedor.setId(rs.getInt("IdFornecedor"));
                    return;
                }
            }
        }
        int id = SqlUtil.inserirComId(conn, SQL_INSERIR_FORNECEDOR, ps -> {
            ps.setString(1, fornecedor.getNome());
            ps.setString(2, fornecedor.getIdentificador());
            ps.setString(3, fornecedor.getContacto());
        });
        fornecedor.setId(id);
    }

    /** Guarda o cabeçalho da encomenda com os totais calculados; a data/hora de entrada é a da primeira entrada. */
    private static int guardarEncomenda(Connection conn, Fornecedor fornecedor, EncomendaFornecedor encomenda,
                                        MovimentoStock primeiraEntrada) throws SQLException {
        TotaisEncomenda totais = encomenda.totais();
        return SqlUtil.inserirComId(conn, SQL_INSERIR_ENCOMENDA, ps -> {
            ps.setString(1, encomenda.numero());
            ps.setDate(2, java.sql.Date.valueOf(encomenda.data()));
            ps.setInt(3, fornecedor.getId());
            ps.setInt(4, primeiraEntrada.getUtilizador().getId());
            SqlUtil.definirDataHora(ps, 5, primeiraEntrada.getDataHora());
            ps.setString(6, totais.moeda());
            ps.setBigDecimal(7, totais.totalLinhas());
            ps.setBigDecimal(8, totais.totalTaxas());
            ps.setBigDecimal(9, totais.totalComTaxas());
        });
    }

    /**
     * Soma a entrada ao artigo (criado se não existir) e regista o movimento. Se vier a linha da encomenda,
     * guarda o preço e a taxa no artigo e a linha em {@code dbo.EncomendaLinha}.
     */
    private static void registarMovimento(Connection conn, Fornecedor fornecedor, MovimentoStock entrada,
                                          int idEncomenda, LinhaEncomenda linha) throws SQLException {
        Artigo pedido = entrada.getArtigo();
        Artigo guardado = procurarArtigo(conn, pedido.getNome()).orElse(null);
        if (guardado == null) {
            int id = SqlUtil.inserirComId(conn, SQL_INSERIR_ARTIGO, ps -> {
                ps.setString(1, pedido.getNome());
                ps.setString(2, pedido.getUnidade());
                ps.setInt(3, fornecedor.getId());
            });
            guardado = new Artigo(id, pedido.getNome(), pedido.getUnidade(), 0);
        }
        guardado.aplicar(entrada);
        final Artigo atualizado = guardado;
        if (linha == null) {
            SqlUtil.executar(conn, SQL_ATUALIZAR_ARTIGO, ps -> {
                ps.setInt(1, atualizado.getQuantidadeAtual());
                ps.setInt(2, fornecedor.getId());
                ps.setInt(3, atualizado.getId());
            });
        } else {
            SqlUtil.executar(conn, SQL_ATUALIZAR_ARTIGO_COM_PRECO, ps -> {
                ps.setInt(1, atualizado.getQuantidadeAtual());
                ps.setInt(2, fornecedor.getId());
                ps.setBigDecimal(3, linha.precoUnitario().preco().valor());
                ps.setString(4, linha.precoUnitario().unidade().getAbreviatura());
                ps.setInt(5, linha.imposto().percentagem());
                ps.setInt(6, atualizado.getId());
            });
        }
        inserirMovimento(conn, atualizado.getId(), entrada);
        if (linha != null) {
            guardarLinha(conn, idEncomenda, atualizado.getId(), pedido.getUnidade(), entrada.getQuantidade(), linha);
        }
        pedido.setId(atualizado.getId());
    }

    /** Guarda a linha da encomenda com os valores calculados (valor sem taxa, taxa e total da linha). */
    private static void guardarLinha(Connection conn, int idEncomenda, int idArtigo, String unidade, int quantidade,
                                     LinhaEncomenda linha) throws SQLException {
        SqlUtil.executar(conn, SQL_INSERIR_LINHA_ENCOMENDA, ps -> {
            ps.setInt(1, idEncomenda);
            ps.setInt(2, linha.numero());
            ps.setString(3, linha.codigoProduto());
            ps.setInt(4, idArtigo);
            ps.setInt(5, quantidade);
            ps.setString(6, unidade);
            definirInteiroOuNulo(ps, 7, linha.caixas().isPresent() ? linha.caixas().getAsInt() : null);
            definirInteiroOuNulo(ps, 8, linha.unidades().isPresent() ? linha.unidades().getAsInt() : null);
            ps.setBigDecimal(9, linha.precoUnitario().preco().valor());
            ps.setString(10, linha.precoUnitario().unidade().getAbreviatura());
            ps.setInt(11, linha.imposto().percentagem());
            ps.setBigDecimal(12, linha.subtotalCalculado());
            ps.setBigDecimal(13, linha.taxaCalculada());
            ps.setBigDecimal(14, linha.totalCalculado());
        });
    }

    private static void definirInteiroOuNulo(PreparedStatement ps, int indice, Integer valor)
            throws SQLException {
        if (valor == null) {
            ps.setNull(indice, Types.INTEGER);
        } else {
            ps.setInt(indice, valor);
        }
    }

    private static Optional<Artigo> procurarArtigo(Connection conn, String nome) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_ARTIGO_POR_NOME)) {
            ps.setString(1, nome);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new Artigo(rs.getInt("IdArtigo"), rs.getString("Nome"),
                        rs.getString("Unidade"), rs.getInt("QuantidadeAtual")));
            }
        }
    }

    private static Artigo mapearArtigo(ResultSet rs) throws SQLException {
        Artigo artigo = new Artigo(rs.getInt("IdArtigo"), rs.getString("Nome"),
                rs.getString("Unidade"), rs.getInt("QuantidadeAtual"));
        artigo.setQuantidadeReservada(rs.getInt("UsadoEmReservas"));
        artigo.setConsumivel(rs.getBoolean("Consumivel"));
        BigDecimal preco = rs.getBigDecimal("PrecoUnitario");
        int taxa = rs.getInt("TaxaPercent");
        boolean semTaxa = rs.wasNull();
        if (preco != null) {
            artigo.setUltimoPreco(preco, rs.getString("UnidadePreco"), semTaxa ? null : taxa);
        }
        int idFornecedor = rs.getInt("IdFornecedor");
        if (!rs.wasNull()) {
            artigo.setFornecedor(new Fornecedor(idFornecedor, rs.getString("NomeFornecedor"),
                    rs.getString("Nif"), rs.getString("Contacto")));
        }
        return artigo;
    }
}
