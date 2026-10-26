package com.hotel4you.hotel4you.DAO;

import java.sql.*;

public class ClienteDAO {

    public boolean inserir(String nome, String documento, String contacto) {
        String sql = "INSERT INTO dbo.Cliente (Nome, Documento, Contacto) VALUES (?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nome);
            stmt.setString(2, documento);
            stmt.setString(3, contacto);

            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String obterNomePorId(int idCliente) {
        String sql = "SELECT Nome FROM dbo.Cliente WHERE IdCliente = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idCliente);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("Nome");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}