package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.DAO.db.DBConnection;
import com.hotel4you.hotel4you.Model.Fornecedor;
import com.hotel4you.hotel4you.Model.ImportacaoFornecedor;
import com.hotel4you.hotel4you.Model.enums.EstadoImportacao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@link ImportacaoDAO} sobre a tabela {@code dbo.ImportacaoFornecedor}.
 */
public class ImportacaoDAOSQL implements ImportacaoDAO {

    private static final String SELECIONAR =
            "SELECT i.IdImportacao, i.NomeFicheiro, i.NumeroEncomenda, i.Conteudo, i.Estado, i.DataImportacao, "
                    + "i.IdDecisor, i.DataDecisao, f.IdFornecedor, f.Nome, f.Nif, f.Contacto, f.Morada, "
                    + "f.CodigoPostal, f.Cidade, f.IdUtilizador "
                    + "FROM dbo.ImportacaoFornecedor i JOIN dbo.Fornecedor f ON f.IdFornecedor = i.IdFornecedor ";
    private static final String ORDEM = " ORDER BY i.DataImportacao DESC, i.IdImportacao DESC";

    @Override
    public ImportacaoFornecedor inserir(ImportacaoFornecedor importacao) {
        String sql = "INSERT INTO dbo.ImportacaoFornecedor (IdFornecedor, NomeFicheiro, NumeroEncomenda, Conteudo, "
                + "Estado, DataImportacao) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection()) {
            int id = SqlUtil.inserirComId(conn, sql, ps -> {
                ps.setInt(1, importacao.getFornecedor().getId());
                ps.setString(2, importacao.getNomeFicheiro());
                ps.setString(3, importacao.getNumeroEncomenda());
                ps.setBytes(4, importacao.getConteudo());
                ps.setString(5, importacao.getEstado().name());
                SqlUtil.definirDataHora(ps, 6, importacao.getDataImportacao());
            });
            importacao.setId(id);
            return importacao;
        } catch (SQLException e) {
            if (SqlUtil.violaUnicidade(e)) {
                throw new DAOException("A encomenda " + importacao.getNumeroEncomenda()
                        + " já foi enviada por este fornecedor. Não é possível enviá-la outra vez.", e);
            }
            throw new DAOException("Não foi possível guardar o ficheiro importado.", e);
        }
    }

    @Override
    public Optional<ImportacaoFornecedor> procurarPorId(int id) {
        List<ImportacaoFornecedor> lista = consultar(SELECIONAR + "WHERE i.IdImportacao = ?", ps -> ps.setInt(1, id));
        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    @Override
    public List<ImportacaoFornecedor> listar(EstadoImportacao estado) {
        if (estado == null) {
            return consultar(SELECIONAR + ORDEM, ps -> { });
        }
        return consultar(SELECIONAR + "WHERE i.Estado = ?" + ORDEM, ps -> ps.setString(1, estado.name()));
    }

    @Override
    public List<ImportacaoFornecedor> listarDoFornecedor(int idFornecedor) {
        return consultar(SELECIONAR + "WHERE i.IdFornecedor = ?" + ORDEM, ps -> ps.setInt(1, idFornecedor));
    }

    @Override
    public void guardarDecisao(ImportacaoFornecedor importacao) {
        String sql = "UPDATE dbo.ImportacaoFornecedor SET Estado = ?, IdDecisor = ?, DataDecisao = ? "
                + "WHERE IdImportacao = ? AND Estado = 'PENDENTE'";
        try (Connection conn = DBConnection.getConnection()) {
            int linhas = SqlUtil.executar(conn, sql, ps -> {
                ps.setString(1, importacao.getEstado().name());
                ps.setInt(2, importacao.getIdDecisor());
                SqlUtil.definirDataHora(ps, 3, importacao.getDataDecisao());
                ps.setInt(4, importacao.getId());
            });
            if (linhas == 0) {
                throw new DAOException("O ficheiro não existe ou já foi aprovado ou rejeitado por outro utilizador.");
            }
        } catch (SQLException e) {
            throw new DAOException("Não foi possível guardar a decisão sobre o ficheiro.", e);
        }
    }

    private List<ImportacaoFornecedor> consultar(String sql, SqlUtil.Preparador p) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            p.preparar(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<ImportacaoFornecedor> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
                return lista;
            }
        } catch (SQLException e) {
            throw new DAOException("Não foi possível consultar os ficheiros importados.", e);
        }
    }

    private static ImportacaoFornecedor mapear(ResultSet rs) throws SQLException {
        Fornecedor fornecedor = new Fornecedor(rs.getInt("IdFornecedor"), rs.getString("Nome"), rs.getString("Nif"),
                rs.getString("Contacto"), rs.getString("Morada"), rs.getString("CodigoPostal"),
                rs.getString("Cidade"), rs.getInt("IdUtilizador"));
        return new ImportacaoFornecedor(rs.getInt("IdImportacao"), fornecedor, rs.getString("NomeFicheiro"),
                rs.getString("NumeroEncomenda"), rs.getBytes("Conteudo"),
                SqlUtil.lerDataHora(rs, "DataImportacao"), EstadoImportacao.valueOf(rs.getString("Estado")),
                rs.getInt("IdDecisor"), SqlUtil.lerDataHora(rs, "DataDecisao"));
    }
}
