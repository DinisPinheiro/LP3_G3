package com.hotel4you.hotel4you.DAO;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuartoDAO {

    public boolean inserir(int numero, int piso, int idTipoQuarto, BigDecimal precoBase, String vista) {
        String sql = "INSERT INTO dbo.Quarto (Numero, Piso, IdTipoQuarto, PrecoBase, Vista, Ativo) VALUES (?, ?, ?, ?, ?, 1)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, numero);
            stmt.setInt(2, piso);
            stmt.setInt(3, idTipoQuarto);
            stmt.setBigDecimal(4, precoBase);
            stmt.setString(5, vista);

            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Integer> listarQuartosAtivos() {
        List<Integer> quartos = new ArrayList<>();
        String sql = "SELECT Numero FROM dbo.Quarto WHERE Ativo = 1";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                quartos.add(rs.getInt("Numero"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return quartos;
    }
}