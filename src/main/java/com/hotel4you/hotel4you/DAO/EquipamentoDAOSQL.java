package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.DAO.db.DBConnection;
import com.hotel4you.hotel4you.Model.Equipamento;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** {@link EquipamentoDAO} sobre a tabela {@code dbo.Equipamento}. */
public class EquipamentoDAOSQL implements EquipamentoDAO {

    @Override
    public Equipamento inserir(Equipamento equipamento) {
        try (Connection conn = DBConnection.getConnection()) {
            int id = SqlUtil.inserirComId(conn, "INSERT INTO dbo.Equipamento (Nome) VALUES (?)",
                    ps -> ps.setString(1, equipamento.getNome()));
            equipamento.setId(id);
            return equipamento;
        } catch (SQLException e) {
            if (SqlUtil.violaUnicidade(e)) {
                throw new DAOException("Já existe o equipamento '" + equipamento.getNome() + "'.", e);
            }
            throw new DAOException("Não foi possível guardar o equipamento.", e);
        }
    }

    @Override
    public void atualizar(Equipamento equipamento) {
        try (Connection conn = DBConnection.getConnection()) {
            int linhas = SqlUtil.executar(conn, "UPDATE dbo.Equipamento SET Nome = ? WHERE IdEquipamento = ?", ps -> {
                ps.setString(1, equipamento.getNome());
                ps.setInt(2, equipamento.getId());
            });
            if (linhas == 0) {
                throw new DAOException("O equipamento com id " + equipamento.getId() + " não existe.");
            }
        } catch (SQLException e) {
            throw new DAOException("Não foi possível atualizar o equipamento.", e);
        }
    }

    @Override
    public void remover(int id) {
        try (Connection conn = DBConnection.getConnection()) {
            int linhas = SqlUtil.executar(conn, "DELETE FROM dbo.Equipamento WHERE IdEquipamento = ?",
                    ps -> ps.setInt(1, id));
            if (linhas == 0) {
                throw new DAOException("O equipamento com id " + id + " não existe.");
            }
        } catch (SQLException e) {
            if (SqlUtil.violaChaveEstrangeira(e)) {
                throw new DAOException("O equipamento está associado a quartos e não pode ser removido.", e);
            }
            throw new DAOException("Não foi possível remover o equipamento.", e);
        }
    }

    @Override
    public Optional<Equipamento> procurarPorId(int id) {
        String sql = "SELECT IdEquipamento, Nome FROM dbo.Equipamento WHERE IdEquipamento = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DAOException("Não foi possível consultar o equipamento.", e);
        }
    }

    @Override
    public List<Equipamento> listarTodos() {
        String sql = "SELECT IdEquipamento, Nome FROM dbo.Equipamento ORDER BY Nome";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Equipamento> lista = new ArrayList<>();
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw new DAOException("Não foi possível consultar os equipamentos.", e);
        }
    }

    private static Equipamento mapear(ResultSet rs) throws SQLException {
        return new Equipamento(rs.getInt("IdEquipamento"), rs.getString("Nome"));
    }
}
