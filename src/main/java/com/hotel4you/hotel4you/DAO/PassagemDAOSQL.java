package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.DAO.db.DBConnection;
import com.hotel4you.hotel4you.Model.Passagem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PassagemDAOSQL implements PassagemDAO {

    @Override
    public void inserir(Passagem passagem) {

        String sql = "INSERT INTO dbo.Passagem " +
                "(IdCartao, IdQuarto, Local, DataHora) " +
                "VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, passagem.getIdCartao());

            if (passagem.getIdQuarto() == null) {
                ps.setNull(2, Types.INTEGER);
            } else {
                ps.setInt(2, passagem.getIdQuarto());
            }

            ps.setString(3, passagem.getLocal());

            ps.setTimestamp(4,
                    Timestamp.valueOf(passagem.getDataHora()));

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DAOException(
                    "Não foi possível registar a passagem.", e);
        }
    }

    @Override
    public List<Passagem> listarPorCartao(int numeroCartao) {

        String sql =
                "SELECT p.IdPassagem, p.IdCartao, c.Numero, " +
                        "p.IdQuarto, p.Local, p.DataHora " +
                        "FROM dbo.Passagem p " +
                        "JOIN dbo.CartaoMagnetico c " +
                        "ON p.IdCartao = c.IdCartao " +
                        "WHERE c.Numero = ? " +
                        "ORDER BY p.DataHora DESC, p.IdPassagem DESC";

        return consultar(sql, numeroCartao);
    }

    @Override
    public List<Passagem> listarPorQuarto(int numeroQuarto) {

        String sql =
                "SELECT p.IdPassagem, p.IdCartao, c.Numero, " +
                        "p.IdQuarto, p.Local, p.DataHora " +
                        "FROM dbo.Passagem p " +
                        "JOIN dbo.CartaoMagnetico c " +
                        "ON p.IdCartao = c.IdCartao " +
                        "JOIN dbo.Quarto q " +
                        "ON p.IdQuarto = q.IdQuarto " +
                        "WHERE q.Numero = ? " +
                        "ORDER BY p.DataHora DESC, p.IdPassagem DESC";

        return consultar(sql, numeroQuarto);
    }

    private List<Passagem> consultar(String sql, int numero) {

        List<Passagem> passagens = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, numero);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    int idQuarto = rs.getInt("IdQuarto");

                    Integer quarto = null;

                    if (!rs.wasNull()) {
                        quarto = idQuarto;
                    }

                    Passagem passagem = new Passagem(
                            rs.getInt("IdPassagem"),
                            rs.getInt("IdCartao"),
                            rs.getInt("Numero"),
                            quarto,
                            rs.getString("Local"),
                            rs.getTimestamp("DataHora")
                                    .toLocalDateTime()
                    );

                    passagens.add(passagem);
                }
            }

        } catch (SQLException e) {
            throw new DAOException(
                    "Não foi possível consultar as passagens.", e);
        }

        return passagens;
    }
}
