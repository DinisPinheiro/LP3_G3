package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.DAO.db.DBConnection;
import com.hotel4you.hotel4you.Model.Fornecedor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@link FornecedorDAO} sobre a tabela {@code dbo.Fornecedor}. O código do fornecedor é a coluna
 * {@code Nif} e é único.
 */
public class FornecedorDAOSQL implements FornecedorDAO {

    private static final String COLUNAS =
            "IdFornecedor, Nome, Nif, Contacto, Morada, CodigoPostal, Cidade, IdUtilizador";

    @Override
    public Fornecedor inserir(Fornecedor fornecedor) {
        String sql = "INSERT INTO dbo.Fornecedor (Nome, Nif, Contacto, Morada, CodigoPostal, Cidade, IdUtilizador) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection()) {
            int id = SqlUtil.inserirComId(conn, sql, ps -> {
                ps.setString(1, fornecedor.getNome());
                ps.setString(2, fornecedor.getIdentificador());
                ps.setString(3, fornecedor.getContacto());
                ps.setString(4, fornecedor.getMorada());
                ps.setString(5, fornecedor.getCodigoPostal());
                ps.setString(6, fornecedor.getCidade());
                definirUtilizador(ps, 7, fornecedor);
            });
            fornecedor.setId(id);
            return fornecedor;
        } catch (SQLException e) {
            if (SqlUtil.violaUnicidade(e)) {
                throw new DAOException("Já existe um fornecedor com o código '" + fornecedor.getIdentificador()
                        + "' ou a conta já está ligada a outro fornecedor.", e);
            }
            throw new DAOException("Não foi possível guardar o fornecedor.", e);
        }
    }

    @Override
    public void atualizar(Fornecedor fornecedor) {
        String sql = "UPDATE dbo.Fornecedor SET Nome = ?, Contacto = ?, Morada = ?, CodigoPostal = ?, "
                + "Cidade = ?, IdUtilizador = ? WHERE IdFornecedor = ?";
        try (Connection conn = DBConnection.getConnection()) {
            int linhas = SqlUtil.executar(conn, sql, ps -> {
                ps.setString(1, fornecedor.getNome());
                ps.setString(2, fornecedor.getContacto());
                ps.setString(3, fornecedor.getMorada());
                ps.setString(4, fornecedor.getCodigoPostal());
                ps.setString(5, fornecedor.getCidade());
                definirUtilizador(ps, 6, fornecedor);
                ps.setInt(7, fornecedor.getId());
            });
            if (linhas == 0) {
                throw new DAOException("O fornecedor com id " + fornecedor.getId() + " não existe.");
            }
        } catch (SQLException e) {
            if (SqlUtil.violaUnicidade(e)) {
                throw new DAOException("A conta já está ligada a outro fornecedor.", e);
            }
            throw new DAOException("Não foi possível atualizar o fornecedor.", e);
        }
    }

    @Override
    public void remover(int id) {
        try (Connection conn = DBConnection.getConnection()) {
            int linhas = SqlUtil.executar(conn, "DELETE FROM dbo.Fornecedor WHERE IdFornecedor = ?",
                    ps -> ps.setInt(1, id));
            if (linhas == 0) {
                throw new DAOException("O fornecedor com id " + id + " não existe.");
            }
        } catch (SQLException e) {
            if (SqlUtil.violaChaveEstrangeira(e)) {
                throw new DAOException("O fornecedor tem artigos ou importações associados e não pode ser removido.", e);
            }
            throw new DAOException("Não foi possível remover o fornecedor.", e);
        }
    }

    @Override
    public Optional<Fornecedor> procurarPorId(int id) {
        return procurarUm("SELECT " + COLUNAS + " FROM dbo.Fornecedor WHERE IdFornecedor = ?",
                ps -> ps.setInt(1, id));
    }

    @Override
    public Optional<Fornecedor> procurarPorIdentificador(String identificador) {
        if (identificador == null) {
            return Optional.empty();
        }
        return procurarUm("SELECT " + COLUNAS + " FROM dbo.Fornecedor WHERE Nif = ?",
                ps -> ps.setString(1, identificador.trim()));
    }

    @Override
    public Optional<Fornecedor> procurarPorUtilizador(int idUtilizador) {
        return procurarUm("SELECT " + COLUNAS + " FROM dbo.Fornecedor WHERE IdUtilizador = ?",
                ps -> ps.setInt(1, idUtilizador));
    }

    @Override
    public List<Fornecedor> listarTodos() {
        String sql = "SELECT " + COLUNAS + " FROM dbo.Fornecedor ORDER BY Nome";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Fornecedor> lista = new ArrayList<>();
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw new DAOException("Não foi possível consultar os fornecedores.", e);
        }
    }

    private Optional<Fornecedor> procurarUm(String sql, SqlUtil.Preparador p) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            p.preparar(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DAOException("Não foi possível consultar o fornecedor.", e);
        }
    }

    /** Sem conta de login, a coluna {@code IdUtilizador} fica a NULL. */
    private static void definirUtilizador(PreparedStatement ps, int indice, Fornecedor fornecedor) throws SQLException {
        if (fornecedor.temConta()) {
            ps.setInt(indice, fornecedor.getIdUtilizador());
        } else {
            ps.setNull(indice, Types.INTEGER);
        }
    }

    private static Fornecedor mapear(ResultSet rs) throws SQLException {
        return new Fornecedor(rs.getInt("IdFornecedor"), rs.getString("Nome"), rs.getString("Nif"),
                rs.getString("Contacto"), rs.getString("Morada"), rs.getString("CodigoPostal"),
                rs.getString("Cidade"), rs.getInt("IdUtilizador"));
    }
}
