package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.DAO.db.DBConnection;
import com.hotel4you.hotel4you.Model.FicheiroXsd;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * {@link XsdDAO} sobre a tabela {@code dbo.FicheiroXsd}. Cada importação é uma linha nova;
 * o XSD em vigor é o mais recente.
 */
public class XsdDAOSQL implements XsdDAO {

    @Override
    public FicheiroXsd guardar(FicheiroXsd xsd, int idDoGestor) {
        String sql = "INSERT INTO dbo.FicheiroXsd (NomeFicheiro, Conteudo, DataImportacao, IdUtilizador) "
                + "VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection()) {
            int id = SqlUtil.inserirComId(conn, sql, ps -> {
                ps.setString(1, xsd.getNomeFicheiro());
                ps.setBytes(2, xsd.getConteudo());
                SqlUtil.definirDataHora(ps, 3, xsd.getDataImportacao());
                ps.setInt(4, idDoGestor);
            });
            xsd.setId(id);
            return xsd;
        } catch (SQLException e) {
            throw new DAOException("Não foi possível guardar o ficheiro XSD.", e);
        }
    }

    @Override
    public Optional<FicheiroXsd> obterAtual() {
        String sql = "SELECT TOP 1 IdXsd, NomeFicheiro, Conteudo, DataImportacao FROM dbo.FicheiroXsd "
                + "ORDER BY IdXsd DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return Optional.empty();
            }
            return Optional.of(new FicheiroXsd(rs.getInt("IdXsd"), rs.getString("NomeFicheiro"),
                    rs.getBytes("Conteudo"), SqlUtil.lerDataHora(rs, "DataImportacao")));
        } catch (SQLException e) {
            throw new DAOException("Não foi possível consultar o ficheiro XSD.", e);
        }
    }
}
