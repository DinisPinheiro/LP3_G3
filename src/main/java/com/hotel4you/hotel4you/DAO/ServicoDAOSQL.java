package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.DAO.db.DBConnection;
import com.hotel4you.hotel4you.Model.Artigo;
import com.hotel4you.hotel4you.Model.Servico;
import com.hotel4you.hotel4you.Model.enums.TipoCobranca;
import com.hotel4you.hotel4you.Model.enums.TipoServico;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@link ServicoDAO} sobre a tabela {@code dbo.Servico}
 * (IdServico, Nome UNIQUE, Categoria, Descricao, Tipo, Valor, Cobranca, Ativo).
 * <p>
 * As colunas Descricao, Tipo e Ativo são criadas por {@code DATABASE/EsquemaDB_S4_servicos_extras.sql};
 * a coluna Cobranca (por unidade ou por noite) por {@code DATABASE/EsquemaDB_S5_servicos_produtos_reserva.sql}.
 * <p>
 * S7-01: o produto do stock ligado ao serviço (ex.: bebidas) fica na tabela de ligação
 * {@code dbo.ServicoProduto} (IdServico, IdArtigo, Quantidade), criada por
 * {@code DATABASE/EsquemaDB_S7_baixa_stock_checkin.sql}. É gravada na mesma transação do serviço.
 */
public class ServicoDAOSQL implements ServicoDAO {

    /** Colunas do serviço ({@code s}) e do produto do stock ligado (também usadas pelo ReservaDAOSQL). */
    static final String COLUNAS = "s.IdServico, s.Nome, s.Categoria, s.Descricao, s.Tipo, s.Valor, s.Cobranca, s.Ativo, "
            + "sp.IdArtigo AS IdArtigoProduto, sp.Quantidade AS QuantidadeProduto, pa.Nome AS NomeProduto, "
            + "pa.Unidade AS UnidadeProduto, pa.QuantidadeAtual AS StockProduto";
    /** Junta a {@code dbo.Servico s} o produto do stock ligado, se houver (S7-01). */
    static final String JUNCAO_PRODUTO = " LEFT JOIN dbo.ServicoProduto sp ON sp.IdServico = s.IdServico "
            + "LEFT JOIN dbo.Artigo pa ON pa.IdArtigo = sp.IdArtigo";
    private static final String SELECT = "SELECT " + COLUNAS + " FROM dbo.Servico s" + JUNCAO_PRODUTO;
    private static final String ORDEM = " ORDER BY s.Tipo, s.Categoria, s.Nome, s.IdServico";

    @Override
    public Servico inserir(Servico servico) {
        String sql = "INSERT INTO dbo.Servico (Nome, Categoria, Descricao, Tipo, Valor, Cobranca, Ativo) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int id = SqlUtil.inserirComId(conn, sql, ps -> preencher(ps, servico));
                gravarProduto(conn, id, servico);
                conn.commit();
                servico.setId(id);
                return servico;
            } catch (SQLException e) {
                SqlUtil.reverter(conn, e);
                throw e;
            }
        } catch (SQLException e) {
            if (SqlUtil.violaUnicidade(e)) {
                throw new DAOException("Já existe um serviço ou extra com o nome '" + servico.getNome() + "'.", e);
            }
            throw new DAOException("Não foi possível guardar o serviço.", e);
        }
    }

    @Override
    public void atualizar(Servico servico) {
        String sql = "UPDATE dbo.Servico SET Nome = ?, Categoria = ?, Descricao = ?, Tipo = ?, Valor = ?, "
                + "Cobranca = ?, Ativo = ? WHERE IdServico = ?";
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int linhas = SqlUtil.executar(conn, sql, ps -> {
                    preencher(ps, servico);
                    ps.setInt(8, servico.getId());
                });
                if (linhas == 0) {
                    conn.rollback();
                    throw new DAOException("O serviço com id " + servico.getId() + " não existe.");
                }
                gravarProduto(conn, servico.getId(), servico);
                conn.commit();
            } catch (SQLException e) {
                SqlUtil.reverter(conn, e);
                throw e;
            }
        } catch (SQLException e) {
            if (SqlUtil.violaUnicidade(e)) {
                throw new DAOException("Já existe outro serviço ou extra com o nome '" + servico.getNome() + "'.", e);
            }
            throw new DAOException("Não foi possível atualizar o serviço.", e);
        }
    }

    @Override
    public void remover(int id) {
        try (Connection conn = DBConnection.getConnection()) {
            int linhas = SqlUtil.executar(conn, "DELETE FROM dbo.Servico WHERE IdServico = ?", ps -> ps.setInt(1, id));
            if (linhas == 0) {
                throw new DAOException("O serviço com id " + id + " não existe.");
            }
        } catch (SQLException e) {
            if (SqlUtil.violaChaveEstrangeira(e)) {
                throw new DAOException("O serviço já foi usado em reservas e não pode ser removido. Desative-o.", e);
            }
            throw new DAOException("Não foi possível remover o serviço.", e);
        }
    }

    @Override
    public Optional<Servico> procurarPorId(int id) {
        String sql = SELECT + " WHERE s.IdServico = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DAOException("Não foi possível consultar o serviço.", e);
        }
    }

    @Override
    public List<Servico> listarTodos() {
        return listar(SELECT + ORDEM);
    }

    @Override
    public List<Servico> listarAtivos() {
        return listar(SELECT + " WHERE s.Ativo = 1" + ORDEM);
    }

    private List<Servico> listar(String sql) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Servico> lista = new ArrayList<>();
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw new DAOException("Não foi possível consultar os serviços.", e);
        }
    }

    private static void preencher(PreparedStatement ps, Servico servico) throws SQLException {
        ps.setString(1, servico.getNome());
        ps.setString(2, servico.getCategoria());
        ps.setString(3, servico.getDescricao());
        ps.setString(4, servico.getTipo().name());
        ps.setBigDecimal(5, servico.getValor());
        ps.setString(6, servico.getCobranca().name());
        ps.setBoolean(7, servico.isAtivo());
    }

    /** Grava o produto do stock ligado ao serviço (apaga o anterior; sem produto fica sem linha). */
    private static void gravarProduto(Connection conn, int idServico, Servico servico) throws SQLException {
        SqlUtil.executar(conn, "DELETE FROM dbo.ServicoProduto WHERE IdServico = ?", ps -> ps.setInt(1, idServico));
        if (servico.temProduto()) {
            SqlUtil.executar(conn, "INSERT INTO dbo.ServicoProduto (IdServico, IdArtigo, Quantidade) VALUES (?, ?, ?)",
                    ps -> {
                        ps.setInt(1, idServico);
                        ps.setInt(2, servico.getProduto().getId());
                        ps.setInt(3, servico.getQuantidadeProduto());
                    });
        }
    }

    /**
     * Cria o serviço a partir de uma linha com as {@link #COLUNAS} (também usado pelo ReservaDAOSQL),
     * já com o produto do stock ligado, se houver.
     */
    static Servico mapear(ResultSet rs) throws SQLException {
        Servico servico = new Servico(rs.getInt("IdServico"), TipoServico.valueOf(rs.getString("Tipo")),
                rs.getString("Nome"), rs.getString("Categoria"), rs.getString("Descricao"), rs.getBigDecimal("Valor"),
                TipoCobranca.valueOf(rs.getString("Cobranca")), rs.getBoolean("Ativo"));
        int idArtigo = rs.getInt("IdArtigoProduto");
        if (!rs.wasNull()) {
            servico.ligarProduto(new Artigo(idArtigo, rs.getString("NomeProduto"), rs.getString("UnidadeProduto"),
                    rs.getInt("StockProduto")), rs.getInt("QuantidadeProduto"));
        }
        return servico;
    }
}
