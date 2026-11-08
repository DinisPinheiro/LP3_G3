package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.DAO.db.DBConnection;
import com.hotel4you.hotel4you.Model.CartaoMagnetico;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@link CartaoDAO} sobre a tabela {@code dbo.CartaoMagnetico}. A coluna {@code Numero} é UNIQUE
 * e a coluna {@code Ativo} diz se o cartão ainda pode ser usado.
 */
public class CartaoDAOSQL implements CartaoDAO {

    private static final String COLUNAS = "IdCartao, Numero, Mestre, Ativo";
    /** Código de erro do SQL Server para "Invalid column name". */
    private static final int ERRO_COLUNA_INEXISTENTE = 207;

    @Override
    public CartaoMagnetico inserir(CartaoMagnetico cartao) {
        String sql = "INSERT INTO dbo.CartaoMagnetico (Numero, Mestre, Ativo) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection()) {
            int id = SqlUtil.inserirComId(conn, sql, ps -> {
                ps.setInt(1, cartao.getNumero());
                ps.setBoolean(2, cartao.isMestre());
                ps.setBoolean(3, cartao.isAtivo());
            });
            cartao.setId(id);
            return cartao;
        } catch (SQLException e) {
            if (SqlUtil.violaUnicidade(e)) {
                throw new DAOException("Já existe um cartão com o número " + cartao.getNumero() + ".", e);
            }
            throw erro("Não foi possível guardar o cartão.", e);
        }
    }

    @Override
    public void atualizar(CartaoMagnetico cartao) {
        String sql = "UPDATE dbo.CartaoMagnetico SET Numero = ?, Mestre = ?, Ativo = ? WHERE IdCartao = ?";
        try (Connection conn = DBConnection.getConnection()) {
            int linhas = SqlUtil.executar(conn, sql, ps -> {
                ps.setInt(1, cartao.getNumero());
                ps.setBoolean(2, cartao.isMestre());
                ps.setBoolean(3, cartao.isAtivo());
                ps.setInt(4, cartao.getId());
            });
            if (linhas == 0) {
                throw new DAOException("O cartão com id " + cartao.getId() + " não existe.");
            }
        } catch (SQLException e) {
            if (SqlUtil.violaUnicidade(e)) {
                throw new DAOException("Já existe outro cartão com o número " + cartao.getNumero() + ".", e);
            }
            throw erro("Não foi possível atualizar o cartão.", e);
        }
    }

    @Override
    public void remover(int id) {
        try (Connection conn = DBConnection.getConnection()) {
            int linhas = SqlUtil.executar(conn, "DELETE FROM dbo.CartaoMagnetico WHERE IdCartao = ?",
                    ps -> ps.setInt(1, id));
            if (linhas == 0) {
                throw new DAOException("O cartão com id " + id + " não existe.");
            }
        } catch (SQLException e) {
            if (SqlUtil.violaChaveEstrangeira(e)) {
                throw new DAOException("O cartão tem reservas ou registos de acesso associados e não pode ser removido.", e);
            }
            throw erro("Não foi possível remover o cartão.", e);
        }
    }

    @Override
    public Optional<CartaoMagnetico> procurarPorId(int id) {
        return procurarUm("SELECT " + COLUNAS + " FROM dbo.CartaoMagnetico WHERE IdCartao = ?",
                ps -> ps.setInt(1, id));
    }

    @Override
    public Optional<CartaoMagnetico> procurarPorNumero(int numero) {
        return procurarUm("SELECT " + COLUNAS + " FROM dbo.CartaoMagnetico WHERE Numero = ?",
                ps -> ps.setInt(1, numero));
    }

    @Override
    public List<CartaoMagnetico> listarTodos() {
        String sql = "SELECT " + COLUNAS + " FROM dbo.CartaoMagnetico ORDER BY Numero";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<CartaoMagnetico> lista = new ArrayList<>();
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw erro("Não foi possível consultar os cartões.", e);
        }
    }

    @Override
    public int proximoNumero() {
        String sql = "SELECT COALESCE(MAX(Numero), 0) FROM dbo.CartaoMagnetico";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return Math.max(rs.getInt(1) + 1, PRIMEIRO_NUMERO_CLIENTE);
        } catch (SQLException e) {
            throw erro("Não foi possível obter o próximo número de cartão.", e);
        }
    }

    private Optional<CartaoMagnetico> procurarUm(String sql, SqlUtil.Preparador p) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            p.preparar(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw erro("Não foi possível consultar o cartão.", e);
        }
    }

    /**
     * Cria o erro a mostrar ao utilizador. Se a base de dados ainda não tem a coluna {@code Ativo}
     * (erro 207 do SQL Server, "Invalid column name"), diz qual é o script que falta correr.
     */
    private static DAOException erro(String mensagem, SQLException e) {
        if (e.getErrorCode() == ERRO_COLUNA_INEXISTENTE) {
            return new DAOException("A tabela dbo.CartaoMagnetico ainda não tem a coluna Ativo. "
                    + "Execute o script DATABASE/EsquemaDB_2_2_checkin.sql na base de dados.", e);
        }
        return new DAOException(mensagem, e);
    }

    private static CartaoMagnetico mapear(ResultSet rs) throws SQLException {
        return new CartaoMagnetico(rs.getInt("IdCartao"), rs.getInt("Numero"),
                rs.getBoolean("Mestre"), rs.getBoolean("Ativo"));
    }
}