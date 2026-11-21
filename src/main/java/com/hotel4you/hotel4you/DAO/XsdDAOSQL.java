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
 * o XSD em vigor de um fornecedor é o mais recente desse fornecedor. As linhas sem fornecedor
 * (da época em que o gestor importava o XSD, S5-05) são ignoradas.
 */
public class XsdDAOSQL implements XsdDAO {

    @Override
    public FicheiroXsd guardar(FicheiroXsd xsd, int idUtilizador) {
        String sql = "INSERT INTO dbo.FicheiroXsd (IdFornecedor, NomeFicheiro, Conteudo, DataImportacao, IdUtilizador) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection()) {
            int id = SqlUtil.inserirComId(conn, sql, ps -> {
                ps.setInt(1, xsd.getIdFornecedor());
                ps.setString(2, xsd.getNomeFicheiro());
                ps.setBytes(3, xsd.getConteudo());
                SqlUtil.definirDataHora(ps, 4, xsd.getDataImportacao());
                ps.setInt(5, idUtilizador);
            });
            xsd.setId(id);
            return xsd;
        } catch (SQLException e) {
            throw new DAOException("Não foi possível guardar o ficheiro XSD.", e);
        }
    }

    @Override
    public Optional<FicheiroXsd> obterAtual(int idFornecedor) {
        String sql = "SELECT TOP 1 IdXsd, IdFornecedor, NomeFicheiro, Conteudo, DataImportacao FROM dbo.FicheiroXsd "
                + "WHERE IdFornecedor = ? ORDER BY IdXsd DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idFornecedor);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new FicheiroXsd(rs.getInt("IdXsd"), rs.getInt("IdFornecedor"),
                        rs.getString("NomeFicheiro"), rs.getBytes("Conteudo"),
                        SqlUtil.lerDataHora(rs, "DataImportacao")));
            }
        } catch (SQLException e) {
            throw new DAOException("Não foi possível consultar o ficheiro XSD.", e);
        }
    }
}
