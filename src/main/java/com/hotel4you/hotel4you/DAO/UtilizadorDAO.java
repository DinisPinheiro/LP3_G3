package com.hotel4you.hotel4you.DAO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UtilizadorDAO {

    public boolean inserir(int idRole, String username, String passwordHash, String nome) {
        String sql = "INSERT INTO dbo.Utilizador (IdRole, Username, PasswordHash, Nome, Ativo) VALUES (?, ?, ?, ?, 1)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idRole);
            stmt.setString(2, username);
            stmt.setString(3, passwordHash);
            stmt.setString(4, nome);

            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<String> listarNomesUtilizadores() {
        List<String> lista = new ArrayList<>();
        String sql = "SELECT Nome FROM dbo.Utilizador WHERE Ativo = 1";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                lista.add(rs.getString("Nome"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lista;
    }
}