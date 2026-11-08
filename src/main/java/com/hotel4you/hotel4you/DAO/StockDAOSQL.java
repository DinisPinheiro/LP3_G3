package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.DAO.db.DBConnection;
import com.hotel4you.hotel4you.Model.Artigo;
import com.hotel4you.hotel4you.Model.Fornecedor;
import com.hotel4you.hotel4you.Model.MovimentoStock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@link StockDAO} sobre as tabelas {@code dbo.Fornecedor}, {@code dbo.Artigo} e {@code dbo.MovimentoStock}.
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
    private static final String SQL_INSERIR_MOVIMENTO =
            "INSERT INTO dbo.MovimentoStock (IdArtigo, IdUtilizador, Tipo, Quantidade, DataHora) VALUES (?, ?, ?, ?, ?)";
    private static final String SQL_LISTAR_ARTIGOS =
            "SELECT a.IdArtigo, a.Nome, a.Unidade, a.QuantidadeAtual, "
                    + "f.IdFornecedor, f.Nome AS NomeFornecedor, f.Nif, f.Contacto "
                    + "FROM dbo.Artigo a LEFT JOIN dbo.Fornecedor f ON f.IdFornecedor = a.IdFornecedor "
                    + "ORDER BY a.Nome";

    @Override
    public void registarEntrada(Fornecedor fornecedor, List<MovimentoStock> entradas) {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                guardarFornecedor(conn, fornecedor);
                for (MovimentoStock entrada : entradas) {
                    registarMovimento(conn, fornecedor, entrada);
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

    /** Soma a entrada ao artigo (criado se não existir) e regista o movimento. */
    private static void registarMovimento(Connection conn, Fornecedor fornecedor, MovimentoStock entrada)
            throws SQLException {
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
        SqlUtil.executar(conn, SQL_ATUALIZAR_ARTIGO, ps -> {
            ps.setInt(1, atualizado.getQuantidadeAtual());
            ps.setInt(2, fornecedor.getId());
            ps.setInt(3, atualizado.getId());
        });
        SqlUtil.executar(conn, SQL_INSERIR_MOVIMENTO, ps -> {
            ps.setInt(1, atualizado.getId());
            ps.setInt(2, entrada.getUtilizador().getId());
            ps.setString(3, entrada.getTipo().name());
            ps.setInt(4, entrada.getQuantidade());
            SqlUtil.definirDataHora(ps, 5, entrada.getDataHora());
        });
        pedido.setId(atualizado.getId());
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
        int idFornecedor = rs.getInt("IdFornecedor");
        if (!rs.wasNull()) {
            artigo.setFornecedor(new Fornecedor(idFornecedor, rs.getString("NomeFornecedor"),
                    rs.getString("Nif"), rs.getString("Contacto")));
        }
        return artigo;
    }
}
