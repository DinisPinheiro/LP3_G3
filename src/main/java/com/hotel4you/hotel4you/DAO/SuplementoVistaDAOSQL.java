package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.DAO.db.DBConnection;
import com.hotel4you.hotel4you.Model.enums.Vista;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * {@link SuplementoVistaDAO} sobre a tabela {@code dbo.SuplementoVista}.
 * <p>
 */
public class SuplementoVistaDAOSQL implements SuplementoVistaDAO {

    @Override
    public Map<Vista, BigDecimal> listar() {
        Map<Vista, BigDecimal> valores = new EnumMap<>(Vista.class);
        for (Vista vista : Vista.values()) {
            valores.put(vista, vista.getSuplemento());
        }
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT Vista, Valor FROM dbo.SuplementoVista");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                try {
                    valores.put(Vista.valueOf(rs.getString("Vista")), rs.getBigDecimal("Valor"));
                } catch (IllegalArgumentException e) {
                    // vista desconhecida na base de dados: ignora-se
                }
            }
            return valores;
        } catch (SQLException e) {
            throw new DAOException("Não foi possível ler os valores das vistas.", e);
        }
    }

    @Override
    public void atualizar(Vista vista, BigDecimal valor) {
        Objects.requireNonNull(vista);
        Objects.requireNonNull(valor);
        try (Connection conn = DBConnection.getConnection()) {
            int linhas = SqlUtil.executar(conn, "UPDATE dbo.SuplementoVista SET Valor = ? WHERE Vista = ?", ps -> {
                ps.setBigDecimal(1, valor);
                ps.setString(2, vista.name());
            });
            if (linhas == 0) {
                SqlUtil.executar(conn, "INSERT INTO dbo.SuplementoVista (Vista, Valor) VALUES (?, ?)", ps -> {
                    ps.setString(1, vista.name());
                    ps.setBigDecimal(2, valor);
                });
            }
        } catch (SQLException e) {
            throw new DAOException("Não foi possível guardar o valor da vista.", e);
        }
    }
}
