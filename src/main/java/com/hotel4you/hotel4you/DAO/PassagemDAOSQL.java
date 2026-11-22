package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.DAO.db.DBConnection;
import com.hotel4you.hotel4you.Model.LocalHotel;
import com.hotel4you.hotel4you.Model.Passagem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@link PassagemDAO} sobre as tabelas {@code dbo.LocalHotel} e {@code dbo.Passagem}
 * (script {@code EsquemaDB_S6_picagem_completa.sql}). Cada operação abre uma só ligação (S6-01).
 */
public class PassagemDAOSQL implements PassagemDAO {

    private static final String SELECT_LOCAL = "SELECT IdLocal, Codigo, Nome, Piso FROM dbo.LocalHotel ";

    /** Passagem com o número do cartão e (se for num quarto) o número do quarto. */
    private static final String SELECT_PASSAGEM =
            "SELECT p.IdPassagem, p.IdCartao, c.Numero, p.IdQuarto, q.Numero AS NumeroQuarto, "
                    + "p.Local, p.DataHora "
                    + "FROM dbo.Passagem p "
                    + "JOIN dbo.CartaoMagnetico c ON p.IdCartao = c.IdCartao "
                    + "LEFT JOIN dbo.Quarto q ON p.IdQuarto = q.IdQuarto ";

    private static final String ORDEM = "ORDER BY p.DataHora DESC, p.IdPassagem DESC";

    /** Códigos de erro do SQL Server para "Invalid column name" (207) e "Invalid object name" (208). */
    private static final int ERRO_COLUNA_INEXISTENTE = 207;
    private static final int ERRO_TABELA_INEXISTENTE = 208;

    @Override
    public void inserir(Passagem passagem) {
        String sql = "INSERT INTO dbo.Passagem (IdCartao, IdQuarto, Local, DataHora) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, passagem.getIdCartao());
            if (passagem.getIdQuarto() == null) {
                ps.setNull(2, Types.INTEGER);
            } else {
                ps.setInt(2, passagem.getIdQuarto());
            }
            ps.setString(3, passagem.getLocal());
            ps.setTimestamp(4, Timestamp.valueOf(passagem.getDataHora()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw erro("Não foi possível registar a passagem.", e);
        }
    }

    @Override
    public List<LocalHotel> listarLocais() {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_LOCAL + "ORDER BY IdLocal");
             ResultSet rs = ps.executeQuery()) {
            List<LocalHotel> locais = new ArrayList<>();
            while (rs.next()) {
                locais.add(mapearLocal(rs));
            }
            return locais;
        } catch (SQLException e) {
            throw erro("Não foi possível consultar os locais do hotel.", e);
        }
    }

    @Override
    public Optional<LocalHotel> procurarLocal(String codigo) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_LOCAL + "WHERE Codigo = ?")) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapearLocal(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw erro("Não foi possível consultar o local.", e);
        }
    }

    @Override
    public List<Passagem> listarPorCartao(int numeroCartao) {
        return consultar(SELECT_PASSAGEM + "WHERE c.Numero = ? " + ORDEM, ps -> ps.setInt(1, numeroCartao));
    }

    @Override
    public List<Passagem> listarPorQuarto(int numeroQuarto) {
        return consultar(SELECT_PASSAGEM + "WHERE q.Numero = ? " + ORDEM, ps -> ps.setInt(1, numeroQuarto));
    }

    @Override
    public List<Passagem> listarPorLocal(String codigoLocal) {
        return consultar(SELECT_PASSAGEM + "WHERE p.Local = ? " + ORDEM, ps -> ps.setString(1, codigoLocal));
    }

    private List<Passagem> consultar(String sql, SqlUtil.Preparador preparador) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            preparador.preparar(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<Passagem> passagens = new ArrayList<>();
                while (rs.next()) {
                    passagens.add(mapearPassagem(rs));
                }
                return passagens;
            }
        } catch (SQLException e) {
            throw erro("Não foi possível consultar as passagens.", e);
        }
    }

    /**
     * Cria o erro a mostrar ao utilizador. Se a base de dados ainda não tem a tabela dos locais
     * (erro 208) ou as colunas novas (erro 207), diz qual é o script que falta correr.
     */
    private static DAOException erro(String mensagem, SQLException e) {
        if (e.getErrorCode() == ERRO_TABELA_INEXISTENTE || e.getErrorCode() == ERRO_COLUNA_INEXISTENTE) {
            return new DAOException("A base de dados ainda não tem a picagem de cartão completa. "
                    + "Execute o script DATABASE/EsquemaDB_S6_picagem_completa.sql na base de dados.", e);
        }
        return new DAOException(mensagem, e);
    }

    private static LocalHotel mapearLocal(ResultSet rs) throws SQLException {
        int piso = rs.getInt("Piso");
        Integer pisoOuNulo = rs.wasNull() ? null : piso;
        return new LocalHotel(rs.getInt("IdLocal"), rs.getString("Codigo"), rs.getString("Nome"), pisoOuNulo);
    }

    private static Passagem mapearPassagem(ResultSet rs) throws SQLException {
        int idQuarto = rs.getInt("IdQuarto");
        Integer quarto = rs.wasNull() ? null : idQuarto;
        int numeroQuarto = rs.getInt("NumeroQuarto");
        Integer numero = rs.wasNull() ? null : numeroQuarto;
        return new Passagem(
                rs.getInt("IdPassagem"),
                rs.getInt("IdCartao"),
                rs.getInt("Numero"),
                quarto,
                numero,
                rs.getString("Local"),
                rs.getTimestamp("DataHora").toLocalDateTime());
    }
}