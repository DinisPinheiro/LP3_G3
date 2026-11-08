package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.DAO.db.DBConnection;
import com.hotel4you.hotel4you.Model.Servico;
import com.hotel4you.hotel4you.Model.enums.TipoServico;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@link ServicoDAO} sobre a tabela {@code dbo.Servico}
 * (IdServico, Nome UNIQUE, Categoria, Descricao, Tipo, Valor, Ativo).
 * <p>
 * As colunas Descricao, Tipo e Ativo são criadas por {@code DATABASE/EsquemaDB_S4_servicos_extras.sql}.
 * <p>
 */
public class ServicoDAOSQL implements ServicoDAO {

    private static final String COLUNAS = "IdServico, Nome, Categoria, Descricao, Tipo, Valor, Ativo";
    private static final String ORDEM = " ORDER BY Tipo, Categoria, Nome, IdServico";

    @Override
    public Servico inserir(Servico servico) {
        String sql = "INSERT INTO dbo.Servico (Nome, Categoria, Descricao, Tipo, Valor, Ativo) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection()) {
            int id = SqlUtil.inserirComId(conn, sql, ps -> preencher(ps, servico));
            servico.setId(id);
            return servico;
        } catch (SQLException e) {
            if (SqlUtil.violaUnicidade(e)) {
                throw new DAOException("Já existe um serviço ou extra com o nome '" + servico.getNome() + "'.", e);
            }
            throw new DAOException("Não foi possível guardar o serviço.", e);
        }
    }

    @Override
    public void atualizar(Servico servico) {
        String sql = "UPDATE dbo.Servico SET Nome = ?, Categoria = ?, Descricao = ?, Tipo = ?, Valor = ?, Ativo = ? "
                + "WHERE IdServico = ?";
        try (Connection conn = DBConnection.getConnection()) {
            int linhas = SqlUtil.executar(conn, sql, ps -> {
                preencher(ps, servico);
                ps.setInt(7, servico.getId());
            });
            if (linhas == 0) {
                throw new DAOException("O serviço com id " + servico.getId() + " não existe.");
            }
        } catch (SQLException e) {
            if (SqlUtil.violaUnicidade(e)) {
                throw new DAOException("Já existe outro serviço ou extra com o nome '" + servico.getNome() + "'.", e);
            }
            throw new DAOException("Não foi possível atualizar o serviço.", e);
        }
    }

    @Override
    public void remover(int id) {
        try (Connection conn = DBConnection.getConnection()) {
            int linhas = SqlUtil.executar(conn, "DELETE FROM dbo.Servico WHERE IdServico = ?", ps -> ps.setInt(1, id));
            if (linhas == 0) {
                throw new DAOException("O serviço com id " + id + " não existe.");
            }
        } catch (SQLException e) {
            if (SqlUtil.violaChaveEstrangeira(e)) {
                throw new DAOException("O serviço já foi usado em reservas e não pode ser removido. Desative-o.", e);
            }
            throw new DAOException("Não foi possível remover o serviço.", e);
        }
    }

    @Override
    public Optional<Servico> procurarPorId(int id) {
        String sql = "SELECT " + COLUNAS + " FROM dbo.Servico WHERE IdServico = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DAOException("Não foi possível consultar o serviço.", e);
        }
    }

    @Override
    public List<Servico> listarTodos() {
        return listar("SELECT " + COLUNAS + " FROM dbo.Servico" + ORDEM);
    }

    @Override
    public List<Servico> listarAtivos() {
        return listar("SELECT " + COLUNAS + " FROM dbo.Servico WHERE Ativo = 1" + ORDEM);
    }

    private List<Servico> listar(String sql) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Servico> lista = new ArrayList<>();
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw new DAOException("Não foi possível consultar os serviços.", e);
        }
    }

    private static void preencher(PreparedStatement ps, Servico servico) throws SQLException {
        ps.setString(1, servico.getNome());
        ps.setString(2, servico.getCategoria());
        ps.setString(3, servico.getDescricao());
        ps.setString(4, servico.getTipo().name());
        ps.setBigDecimal(5, servico.getValor());
        ps.setBoolean(6, servico.isAtivo());
    }

    private static Servico mapear(ResultSet rs) throws SQLException {
        return new Servico(rs.getInt("IdServico"), TipoServico.valueOf(rs.getString("Tipo")), rs.getString("Nome"),
                rs.getString("Categoria"), rs.getString("Descricao"), rs.getBigDecimal("Valor"), rs.getBoolean("Ativo"));
    }
}
