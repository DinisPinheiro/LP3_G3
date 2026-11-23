package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.DAO.db.DBConnection;
import com.hotel4you.hotel4you.Model.Artigo;
import com.hotel4you.hotel4you.Model.ConfiguracaoTipoQuarto;
import com.hotel4you.hotel4you.Model.Equipamento;
import com.hotel4you.hotel4you.Model.ProdutoQuarto;
import com.hotel4you.hotel4you.Model.enums.TipoQuarto;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@link ConfiguracaoTipoQuartoDAO} sobre as tabelas {@code dbo.ConfigTipoQuarto} (preço base por tipo),
 * {@code dbo.ConfigTipoQuarto_Equipamento} e {@code dbo.ConfigTipoQuarto_Produto} (S6-09). Criadas por
 * {@code DATABASE/EsquemaDB_S6_config_tipo_quarto.sql}.
 */
public class ConfiguracaoTipoQuartoDAOSQL implements ConfiguracaoTipoQuartoDAO {

    @Override
    public Optional<ConfiguracaoTipoQuarto> procurar(TipoQuarto tipo) {
        try (Connection conn = DBConnection.getConnection()) {
            return ler(conn, tipo);
        } catch (SQLException e) {
            throw new DAOException("Não foi possível consultar a configuração do tipo de quarto.", e);
        }
    }

    @Override
    public List<ConfiguracaoTipoQuarto> listar() {
        List<ConfiguracaoTipoQuarto> lista = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection()) {
            for (TipoQuarto tipo : TipoQuarto.values()) {
                ler(conn, tipo).ifPresent(lista::add);
            }
            return lista;
        } catch (SQLException e) {
            throw new DAOException("Não foi possível consultar a configuração dos tipos de quarto.", e);
        }
    }

    @Override
    public void guardar(ConfiguracaoTipoQuarto configuracao) {
        for (Equipamento equipamento : configuracao.getEquipamentos()) {
            if (equipamento.getId() <= 0) {
                throw new DAOException("O equipamento '" + equipamento.getNome()
                        + "' ainda não está guardado; escolha um equipamento do catálogo.");
            }
        }
        for (ProdutoQuarto produto : configuracao.getProdutos()) {
            if (produto.getArtigo().getId() <= 0) {
                throw new DAOException("O produto '" + produto.getArtigo().getNome()
                        + "' não está guardado no stock.");
            }
        }
        String tipo = configuracao.getTipo().name();
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int linhas = SqlUtil.executar(conn, "UPDATE dbo.ConfigTipoQuarto SET PrecoBase = ? WHERE Tipo = ?", ps -> {
                    ps.setBigDecimal(1, configuracao.getPrecoBase());
                    ps.setString(2, tipo);
                });
                if (linhas == 0) {
                    SqlUtil.executar(conn, "INSERT INTO dbo.ConfigTipoQuarto (Tipo, PrecoBase) VALUES (?, ?)", ps -> {
                        ps.setString(1, tipo);
                        ps.setBigDecimal(2, configuracao.getPrecoBase());
                    });
                }
                SqlUtil.executar(conn, "DELETE FROM dbo.ConfigTipoQuarto_Equipamento WHERE Tipo = ?",
                        ps -> ps.setString(1, tipo));
                SqlUtil.executar(conn, "DELETE FROM dbo.ConfigTipoQuarto_Produto WHERE Tipo = ?",
                        ps -> ps.setString(1, tipo));
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO dbo.ConfigTipoQuarto_Equipamento (Tipo, IdEquipamento) VALUES (?, ?)")) {
                    for (Equipamento equipamento : configuracao.getEquipamentos()) {
                        ps.setString(1, tipo);
                        ps.setInt(2, equipamento.getId());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO dbo.ConfigTipoQuarto_Produto (Tipo, IdArtigo, Quantidade) VALUES (?, ?, ?)")) {
                    for (ProdutoQuarto produto : configuracao.getProdutos()) {
                        ps.setString(1, tipo);
                        ps.setInt(2, produto.getArtigo().getId());
                        ps.setInt(3, produto.getQuantidade());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                conn.commit();
            } catch (SQLException e) {
                SqlUtil.reverter(conn, e);
                throw new DAOException("Não foi possível guardar a configuração do tipo de quarto.", e);
            }
        } catch (SQLException e) {
            throw new DAOException("Não foi possível guardar a configuração do tipo de quarto.", e);
        }
    }

    private static Optional<ConfiguracaoTipoQuarto> ler(Connection conn, TipoQuarto tipo) throws SQLException {
        BigDecimal preco;
        try (PreparedStatement ps = conn.prepareStatement("SELECT PrecoBase FROM dbo.ConfigTipoQuarto WHERE Tipo = ?")) {
            ps.setString(1, tipo.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                preco = rs.getBigDecimal("PrecoBase");
            }
        }
        List<Equipamento> equipamentos = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT e.IdEquipamento, e.Nome "
                + "FROM dbo.ConfigTipoQuarto_Equipamento ce JOIN dbo.Equipamento e ON e.IdEquipamento = ce.IdEquipamento "
                + "WHERE ce.Tipo = ? ORDER BY e.Nome")) {
            ps.setString(1, tipo.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    equipamentos.add(new Equipamento(rs.getInt("IdEquipamento"), rs.getString("Nome")));
                }
            }
        }
        List<ProdutoQuarto> produtos = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT cp.Quantidade, a.IdArtigo, a.Nome, a.Unidade, "
                + "a.QuantidadeAtual, a.Consumivel FROM dbo.ConfigTipoQuarto_Produto cp "
                + "JOIN dbo.Artigo a ON a.IdArtigo = cp.IdArtigo WHERE cp.Tipo = ? ORDER BY a.Nome")) {
            ps.setString(1, tipo.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Artigo artigo = new Artigo(rs.getInt("IdArtigo"), rs.getString("Nome"),
                            rs.getString("Unidade"), rs.getInt("QuantidadeAtual"));
                    artigo.setConsumivel(rs.getBoolean("Consumivel"));
                    produtos.add(new ProdutoQuarto(artigo, rs.getInt("Quantidade")));
                }
            }
        }
        return Optional.of(new ConfiguracaoTipoQuarto(tipo, preco, equipamentos, produtos));
    }
}
