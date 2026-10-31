package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.DAO.db.DBConnection;
import com.hotel4you.hotel4you.Model.Equipamento;
import com.hotel4you.hotel4you.Model.Quarto;
import com.hotel4you.hotel4you.Model.enums.TipoQuarto;
import com.hotel4you.hotel4you.Model.enums.Vista;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * {@link QuartoDAO} sobre as tabelas {@code dbo.Quarto} e {@code dbo.Quarto_Equipamento}.
 * Gravar um quarto e a sua lista de equipamentos é feito numa só transação.
 */
public class QuartoDAOSQL implements QuartoDAO {

    private static final String COLUNAS = "IdQuarto, Numero, Piso, Local, Tipo, PrecoBase, Vista, Ativo";

    @Override
    public Quarto inserir(Quarto quarto) {
        String sql = "INSERT INTO dbo.Quarto (Numero, Piso, Local, Tipo, PrecoBase, Vista, Ativo) VALUES (?, ?, ?, ?, ?, ?, ?)";
        Set<Integer> equipamentos = idsDosEquipamentos(quarto);
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int id = SqlUtil.inserirComId(conn, sql, ps -> {
                    ps.setInt(1, quarto.getNumero());
                    ps.setInt(2, quarto.getPiso());
                    ps.setString(3, quarto.getLocal());
                    ps.setString(4, quarto.getTipo().name());
                    ps.setBigDecimal(5, quarto.getPrecoBase());
                    ps.setString(6, quarto.getVista().name());
                    ps.setBoolean(7, quarto.isAtivo());
                });
                gravarEquipamentos(conn, id, equipamentos);
                conn.commit();
                quarto.setId(id);
                return quarto;
            } catch (SQLException e) {
                SqlUtil.reverter(conn, e);
                if (SqlUtil.violaUnicidade(e)) {
                    throw new DAOException("Já existe o quarto número " + quarto.getNumero() + ".", e);
                }
                throw new DAOException("Não foi possível guardar o quarto.", e);
            }
        } catch (SQLException e) {
            throw new DAOException("Não foi possível guardar o quarto.", e);
        }
    }

    @Override
    public void atualizar(Quarto quarto) {
        String sql = "UPDATE dbo.Quarto SET Local = ?, Tipo = ?, PrecoBase = ?, Vista = ?, Ativo = ? WHERE IdQuarto = ?";
        Set<Integer> equipamentos = idsDosEquipamentos(quarto);
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int linhas = SqlUtil.executar(conn, sql, ps -> {
                    ps.setString(1, quarto.getLocal());
                    ps.setString(2, quarto.getTipo().name());
                    ps.setBigDecimal(3, quarto.getPrecoBase());
                    ps.setString(4, quarto.getVista().name());
                    ps.setBoolean(5, quarto.isAtivo());
                    ps.setInt(6, quarto.getId());
                });
                if (linhas == 0) {
                    conn.rollback();
                    throw new DAOException("O quarto com id " + quarto.getId() + " não existe.");
                }
                SqlUtil.executar(conn, "DELETE FROM dbo.Quarto_Equipamento WHERE IdQuarto = ?",
                        ps -> ps.setInt(1, quarto.getId()));
                gravarEquipamentos(conn, quarto.getId(), equipamentos);
                conn.commit();
            } catch (SQLException e) {
                SqlUtil.reverter(conn, e);
                throw new DAOException("Não foi possível atualizar o quarto.", e);
            }
        } catch (SQLException e) {
            throw new DAOException("Não foi possível atualizar o quarto.", e);
        }
    }

    @Override
    public void remover(int id) {
        try (Connection conn = DBConnection.getConnection()) {
            int linhas = SqlUtil.executar(conn, "DELETE FROM dbo.Quarto WHERE IdQuarto = ?", ps -> ps.setInt(1, id));
            if (linhas == 0) {
                throw new DAOException("O quarto com id " + id + " não existe.");
            }
        } catch (SQLException e) {
            if (SqlUtil.violaChaveEstrangeira(e)) {
                throw new DAOException("O quarto tem reservas associadas e não pode ser removido; desative-o.", e);
            }
            throw new DAOException("Não foi possível remover o quarto.", e);
        }
    }

    @Override
    public Optional<Quarto> procurarPorId(int id) {
        return procurarUm("SELECT " + COLUNAS + " FROM dbo.Quarto WHERE IdQuarto = ?", ps -> ps.setInt(1, id));
    }

    @Override
    public Optional<Quarto> procurarPorNumero(int numero) {
        return procurarUm("SELECT " + COLUNAS + " FROM dbo.Quarto WHERE Numero = ?", ps -> ps.setInt(1, numero));
    }

    @Override
    public List<Quarto> listarTodos() {
        return listar("SELECT " + COLUNAS + " FROM dbo.Quarto ORDER BY Numero", ps -> { });
    }

    @Override
    public List<Quarto> listarAtivos() {
        return listar("SELECT " + COLUNAS + " FROM dbo.Quarto WHERE Ativo = 1 ORDER BY Numero", ps -> { });
    }

    @Override
    public List<Quarto> listarDisponiveis(TipoQuarto tipo, LocalDate dataEntrada, LocalDate dataSaida) {
        if (dataEntrada == null || dataSaida == null || !dataSaida.isAfter(dataEntrada)) {
            throw new IllegalArgumentException("A data de saída deve ser posterior à de entrada.");
        }
        String sql = "SELECT " + COLUNAS + " FROM dbo.Quarto q WHERE q.Ativo = 1"
                + (tipo == null ? "" : " AND q.Tipo = ?")
                + " AND NOT EXISTS (SELECT 1 FROM dbo.Reserva r WHERE r.IdQuarto = q.IdQuarto"
                + " AND r.Estado IN ('PENDENTE', 'CHECK_IN') AND r.DataEntrada < ? AND r.DataSaida > ?)"
                + " ORDER BY q.Numero";
        return listar(sql, ps -> {
            int i = 1;
            if (tipo != null) {
                ps.setString(i++, tipo.name());
            }
            ps.setDate(i++, Date.valueOf(dataSaida));
            ps.setDate(i, Date.valueOf(dataEntrada));
        });
    }

    private Optional<Quarto> procurarUm(String sql, SqlUtil.Preparador p) {
        List<Quarto> encontrados = listar(sql, p);
        return encontrados.isEmpty() ? Optional.empty() : Optional.of(encontrados.get(0));
    }

    private List<Quarto> listar(String sql, SqlUtil.Preparador p) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            p.preparar(ps);
            List<Quarto> lista = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
            for (Quarto quarto : lista) {
                carregarEquipamentos(conn, quarto);
            }
            return lista;
        } catch (SQLException e) {
            throw new DAOException("Não foi possível consultar os quartos.", e);
        }
    }

    private static Quarto mapear(ResultSet rs) throws SQLException {
        return new Quarto(rs.getInt("IdQuarto"), rs.getInt("Numero"), rs.getInt("Piso"), rs.getString("Local"),
                TipoQuarto.valueOf(rs.getString("Tipo")), rs.getBigDecimal("PrecoBase"),
                Vista.valueOf(rs.getString("Vista")), rs.getBoolean("Ativo"));
    }

    private static void carregarEquipamentos(Connection conn, Quarto quarto) throws SQLException {
        String sql = "SELECT e.IdEquipamento, e.Nome FROM dbo.Quarto_Equipamento qe "
                + "JOIN dbo.Equipamento e ON e.IdEquipamento = qe.IdEquipamento "
                + "WHERE qe.IdQuarto = ? ORDER BY e.Nome";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, quarto.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    quarto.adicionarEquipamento(new Equipamento(rs.getInt("IdEquipamento"), rs.getString("Nome")));
                }
            }
        }
    }

    /** Ids dos equipamentos do quarto (sem repetidos); todos têm de existir já no catálogo. */
    private static Set<Integer> idsDosEquipamentos(Quarto quarto) {
        Set<Integer> ids = new LinkedHashSet<>();
        for (Equipamento equipamento : quarto.getEquipamentos()) {
            if (equipamento.getId() <= 0) {
                throw new DAOException("O equipamento '" + equipamento.getNome()
                        + "' ainda não está guardado; escolha um equipamento do catálogo.");
            }
            ids.add(equipamento.getId());
        }
        return ids;
    }

    private static void gravarEquipamentos(Connection conn, int idQuarto, Set<Integer> idsEquipamentos)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO dbo.Quarto_Equipamento (IdQuarto, IdEquipamento) VALUES (?, ?)")) {
            for (int idEquipamento : idsEquipamentos) {
                ps.setInt(1, idQuarto);
                ps.setInt(2, idEquipamento);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
