package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.DAO.db.DBConnection;
import com.hotel4you.hotel4you.Model.Utilizador;
import com.hotel4you.hotel4you.Model.enums.Perfil;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@link UtilizadorDAO} sobre a tabela {@code dbo.Utilizador} do SQL Server.
 * <p>
 * Só guarda e lê o hash da palavra-chave (ver {@code PasswordUtils}); o username é único e
 * não distingue maiúsculas de minúsculas (collation da base de dados).
 */
public class UtilizadorDAOSQL implements UtilizadorDAO {

    private static final String COLUNAS = "IdUtilizador, Username, PasswordHash, Nome, Perfil, Ativo";

    @Override
    public Utilizador inserir(Utilizador utilizador) {
        String sql = "INSERT INTO dbo.Utilizador (Username, PasswordHash, Nome, Perfil, Ativo) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection()) {
            int id = SqlUtil.inserirComId(conn, sql, ps -> {
                ps.setString(1, utilizador.getUsername());
                ps.setString(2, utilizador.getPasswordHash());
                ps.setString(3, utilizador.getNome());
                ps.setString(4, utilizador.getPerfil().name());
                ps.setBoolean(5, utilizador.isAtivo());
            });
            utilizador.setId(id);
            return utilizador;
        } catch (SQLException e) {
            if (SqlUtil.violaUnicidade(e)) {
                throw new DAOException("Já existe um utilizador com o username '" + utilizador.getUsername() + "'.", e);
            }
            throw new DAOException("Não foi possível guardar o utilizador.", e);
        }
    }

    @Override
    public void atualizar(Utilizador utilizador) {
        String sql = "UPDATE dbo.Utilizador SET PasswordHash = ?, Nome = ?, Perfil = ?, Ativo = ? WHERE IdUtilizador = ?";
        try (Connection conn = DBConnection.getConnection()) {
            int linhas = SqlUtil.executar(conn, sql, ps -> {
                ps.setString(1, utilizador.getPasswordHash());
                ps.setString(2, utilizador.getNome());
                ps.setString(3, utilizador.getPerfil().name());
                ps.setBoolean(4, utilizador.isAtivo());
                ps.setInt(5, utilizador.getId());
            });
            if (linhas == 0) {
                throw new DAOException("O utilizador com id " + utilizador.getId() + " não existe.");
            }
        } catch (SQLException e) {
            throw new DAOException("Não foi possível atualizar o utilizador.", e);
        }
    }

    @Override
    public void remover(int id) {
        try (Connection conn = DBConnection.getConnection()) {
            int linhas = SqlUtil.executar(conn, "DELETE FROM dbo.Utilizador WHERE IdUtilizador = ?",
                    ps -> ps.setInt(1, id));
            if (linhas == 0) {
                throw new DAOException("O utilizador com id " + id + " não existe.");
            }
        } catch (SQLException e) {
            if (SqlUtil.violaChaveEstrangeira(e)) {
                throw new DAOException("O utilizador tem registos associados e não pode ser removido; desative-o.", e);
            }
            throw new DAOException("Não foi possível remover o utilizador.", e);
        }
    }

    @Override
    public Optional<Utilizador> procurarPorId(int id) {
        return procurarUm("SELECT " + COLUNAS + " FROM dbo.Utilizador WHERE IdUtilizador = ?", ps -> ps.setInt(1, id));
    }

    @Override
    public Optional<Utilizador> procurarPorUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return procurarUm("SELECT " + COLUNAS + " FROM dbo.Utilizador WHERE Username = ?",
                ps -> ps.setString(1, username.trim()));
    }

    @Override
    public List<Utilizador> listarTodos() {
        String sql = "SELECT " + COLUNAS + " FROM dbo.Utilizador ORDER BY IdUtilizador";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Utilizador> lista = new ArrayList<>();
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw new DAOException("Não foi possível consultar os utilizadores.", e);
        }
    }

    private Optional<Utilizador> procurarUm(String sql, SqlUtil.Preparador p) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            p.preparar(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DAOException("Não foi possível consultar o utilizador.", e);
        }
    }

    private static Utilizador mapear(ResultSet rs) throws SQLException {
        return new Utilizador(rs.getInt("IdUtilizador"), rs.getString("Username"), rs.getString("PasswordHash"),
                rs.getString("Nome"), Perfil.valueOf(rs.getString("Perfil")), rs.getBoolean("Ativo"));
    }
}
