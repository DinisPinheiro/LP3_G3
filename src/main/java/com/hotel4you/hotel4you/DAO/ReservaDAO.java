package com.hotel4you.hotel4you.DAO;

import java.sql.*;

public class ReservaDAO {

    public boolean inserir(int idCliente, int idQuarto, String dataEntrada, String dataSaida) {
        String sql = "INSERT INTO dbo.Reserva (IdCliente, IdQuarto, DataEntrada, DataSaida, Estado) VALUES (?, ?, ?, ?, 'PENDENTE')";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idCliente);
            stmt.setInt(2, idQuarto);
            stmt.setDate(3, Date.valueOf(dataEntrada)); // Formato "AAAA-MM-DD"
            stmt.setDate(4, Date.valueOf(dataSaida));

            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean atualizarEstado(int idReserva, String novoEstado) {
        String sql = "UPDATE dbo.Reserva SET Estado = ? WHERE IdReserva = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, novoEstado);
            stmt.setInt(2, idReserva);

            int linhasAfetadas = stmt.executeUpdate();
            return linhasAfetadas > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}