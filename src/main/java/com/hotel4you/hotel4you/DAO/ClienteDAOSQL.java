package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.DAO.db.DBConnection;
import com.hotel4you.hotel4you.Model.Cliente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * {@link ClienteDAO} sobre a tabela {@code dbo.Cliente}. A coluna {@code Nif} (UNIQUE) guarda
 * o documento de identificação do cliente.
 */
public class ClienteDAOSQL implements ClienteDAO {

    private static final String COLUNAS = "IdCliente, Nome, Nif, Contacto";

    @Override
    public Cliente inserir(Cliente cliente) {
        String sql = "INSERT INTO dbo.Cliente (Nome, Nif, Contacto) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection()) {
            int id = SqlUtil.inserirComId(conn, sql, ps -> {
                ps.setString(1, cliente.getNome());
                ps.setString(2, cliente.getDocumento());
                ps.setString(3, cliente.getContacto());
            });
            cliente.setId(id);
            return cliente;
        } catch (SQLException e) {
            if (SqlUtil.violaUnicidade(e)) {
                throw new DAOException("Já existe um cliente com o NIF/documento '" + cliente.getDocumento() + "'.", e);
            }
            throw new DAOException("Não foi possível guardar o cliente.", e);
        }
    }

    @Override
    public void atualizar(Cliente cliente) {
        String sql = "UPDATE dbo.Cliente SET Nome = ?, Nif = ?, Contacto = ? WHERE IdCliente = ?";
        try (Connection conn = DBConnection.getConnection()) {
            int linhas = SqlUtil.executar(conn, sql, ps -> {
                ps.setString(1, cliente.getNome());
                ps.setString(2, cliente.getDocumento());
                ps.setString(3, cliente.getContacto());
                ps.setInt(4, cliente.getId());
            });
            if (linhas == 0) {
                throw new DAOException("O cliente com id " + cliente.getId() + " não existe.");
            }
        } catch (SQLException e) {
            if (SqlUtil.violaUnicidade(e)) {
                throw new DAOException("Já existe outro cliente com o NIF/documento '" + cliente.getDocumento() + "'.", e);
            }
            throw new DAOException("Não foi possível atualizar o cliente.", e);
        }
    }

    @Override
    public void remover(int id) {
        try (Connection conn = DBConnection.getConnection()) {
            int linhas = SqlUtil.executar(conn, "DELETE FROM dbo.Cliente WHERE IdCliente = ?", ps -> ps.setInt(1, id));
            if (linhas == 0) {
                throw new DAOException("O cliente com id " + id + " não existe.");
            }
        } catch (SQLException e) {
            if (SqlUtil.violaChaveEstrangeira(e)) {
                throw new DAOException("O cliente tem reservas associadas e não pode ser removido.", e);
            }
            throw new DAOException("Não foi possível remover o cliente.", e);
        }
    }

    @Override
    public Optional<Cliente> procurarPorId(int id) {
        return procurarUm("SELECT " + COLUNAS + " FROM dbo.Cliente WHERE IdCliente = ?", ps -> ps.setInt(1, id));
    }

    @Override
    public Optional<Cliente> procurarPorDocumento(String documento) {
        if (documento == null) {
            return Optional.empty();
        }
        return procurarUm("SELECT " + COLUNAS + " FROM dbo.Cliente WHERE Nif = ?",
                ps -> ps.setString(1, documento.trim()));
    }

    @Override
    public List<Cliente> listarTodos() {
        String sql = "SELECT " + COLUNAS + " FROM dbo.Cliente ORDER BY Nome, IdCliente";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Cliente> lista = new ArrayList<>();
            while (rs.next()) {
                lista.add(mapear(rs));
            }
            return lista;
        } catch (SQLException e) {
            throw new DAOException("Não foi possível consultar os clientes.", e);
        }
    }

    @Override
    public List<Cliente> listarPagina(String pesquisa, int pagina, int tamanho) {
        if (pagina < 0 || tamanho < 1) {
            throw new IllegalArgumentException("Página inválida.");
        }
        String sql = "SELECT " + COLUNAS + " FROM dbo.Cliente" + filtro(pesquisa)
                + " ORDER BY Nome, IdCliente OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = preencherFiltro(ps, pesquisa);
            ps.setInt(i++, pagina * tamanho);
            ps.setInt(i, tamanho);
            List<Cliente> lista = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new DAOException("Não foi possível consultar os clientes.", e);
        }
    }

    @Override
    public int contar(String pesquisa) {
        String sql = "SELECT COUNT(*) FROM dbo.Cliente" + filtro(pesquisa);
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            preencherFiltro(ps, pesquisa);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DAOException("Não foi possível contar os clientes.", e);
        }
    }

    /** Cláusula WHERE da pesquisa por nome ou NIF/documento (vazia se não há pesquisa). */
    private static String filtro(String pesquisa) {
        return semPesquisa(pesquisa) ? ""
                : " WHERE LOWER(Nome) LIKE ? ESCAPE '\\' OR LOWER(Nif) LIKE ? ESCAPE '\\'";
    }

    /** Preenche os {@code ?} do filtro e devolve o índice do próximo parâmetro. */
    private static int preencherFiltro(PreparedStatement ps, String pesquisa) throws SQLException {
        if (semPesquisa(pesquisa)) {
            return 1;
        }
        ps.setString(1, "%" + escaparLike(pesquisa.trim().toLowerCase(Locale.ROOT)) + "%");
        ps.setString(2, "%" + escaparLike(pesquisa.replaceAll("\\s+", "").toLowerCase(Locale.ROOT)) + "%");
        return 3;
    }

    private static boolean semPesquisa(String pesquisa) {
        return pesquisa == null || pesquisa.isBlank();
    }

    /** Impede que %, _ e [ escritos pelo utilizador funcionem como caracteres especiais do LIKE. */
    private static String escaparLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_").replace("[", "\\[");
    }

    private Optional<Cliente> procurarUm(String sql, SqlUtil.Preparador p) {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            p.preparar(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DAOException("Não foi possível consultar o cliente.", e);
        }
    }

    private static Cliente mapear(ResultSet rs) throws SQLException {
        return new Cliente(rs.getInt("IdCliente"), rs.getString("Nome"), rs.getString("Nif"), rs.getString("Contacto"));
    }
}
