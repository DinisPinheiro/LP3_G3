package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.DAO.db.DBConnection;
import com.hotel4you.hotel4you.Model.CartaoMagnetico;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@link CartaoDAO} sobre a tabela {@code dbo.CartaoMagnetico}. A coluna {@code Numero} é UNIQUE,
 * a coluna {@code Ativo} diz se o cartão ainda pode ser usado e a coluna {@code IdUtilizador}
 * (só nos cartões de funcionário) diz a quem pertence.
 * <p>
 * Se a base de dados ainda não tem a coluna {@code IdUtilizador} (script
 * {@code EsquemaDB_S6_picagem_completa.sql} por correr), os cartões continuam a ler-se e a guardar-se
 * como cartões sem dono, para as reservas e o check-in não deixarem de funcionar. Só a emissão de
 * cartões de funcionário exige o script.
 */
public class CartaoDAOSQL implements CartaoDAO {

    private static final String COLUNAS = "IdCartao, Numero, Mestre, Ativo, IdUtilizador";
    /** Igual a {@link #COLUNAS}, para uma base de dados que ainda não tem a coluna IdUtilizador. */
    private static final String COLUNAS_SEM_DONO = "IdCartao, Numero, Mestre, Ativo, CAST(NULL AS INT) AS IdUtilizador";

    private static final String INSERIR_COM_DONO =
            "INSERT INTO dbo.CartaoMagnetico (Numero, Mestre, Ativo, IdUtilizador) VALUES (?, ?, ?, ?)";
    private static final String INSERIR_SEM_DONO =
            "INSERT INTO dbo.CartaoMagnetico (Numero, Mestre, Ativo) VALUES (?, ?, ?)";

    /** Código de erro do SQL Server para "Invalid column name". */
    private static final int ERRO_COLUNA_INEXISTENTE = 207;

    /** Uma consulta que recebe a lista de colunas a pedir. */
    private interface Consulta<T> {
        T executar(String colunas) throws SQLException;
    }

    @Override
    public CartaoMagnetico inserir(CartaoMagnetico cartao) {
        try (Connection conn = DBConnection.getConnection()) {
            int id;
            try {
                id = SqlUtil.inserirComId(conn, INSERIR_COM_DONO, ps -> {
                    ps.setInt(1, cartao.getNumero());
                    ps.setBoolean(2, cartao.isMestre());
                    ps.setBoolean(3, cartao.isAtivo());
                    if (cartao.getIdUtilizador() == null) {
                        ps.setNull(4, Types.INTEGER);
                    } else {
                        ps.setInt(4, cartao.getIdUtilizador());
                    }
                });
            } catch (SQLException e) {
                if (e.getErrorCode() != ERRO_COLUNA_INEXISTENTE || cartao.isDeFuncionario()) {
                    throw e;
                }
                // Sem a coluna IdUtilizador: guarda o cartão sem dono, como antes do S6-06.
                id = SqlUtil.inserirComId(conn, INSERIR_SEM_DONO, ps -> {
                    ps.setInt(1, cartao.getNumero());
                    ps.setBoolean(2, cartao.isMestre());
                    ps.setBoolean(3, cartao.isAtivo());
                });
            }
            cartao.setId(id);
            return cartao;
        } catch (SQLException e) {
            if (SqlUtil.violaUnicidade(e)) {
                throw new DAOException("Já existe um cartão com o número " + cartao.getNumero() + ".", e);
            }
            throw erro("Não foi possível guardar o cartão.", e);
        }
    }

    @Override
    public void atualizar(CartaoMagnetico cartao) {
        String sql = "UPDATE dbo.CartaoMagnetico SET Numero = ?, Mestre = ?, Ativo = ? WHERE IdCartao = ?";
        try (Connection conn = DBConnection.getConnection()) {
            int linhas = SqlUtil.executar(conn, sql, ps -> {
                ps.setInt(1, cartao.getNumero());
                ps.setBoolean(2, cartao.isMestre());
                ps.setBoolean(3, cartao.isAtivo());
                ps.setInt(4, cartao.getId());
            });
            if (linhas == 0) {
                throw new DAOException("O cartão com id " + cartao.getId() + " não existe.");
            }
        } catch (SQLException e) {
            if (SqlUtil.violaUnicidade(e)) {
                throw new DAOException("Já existe outro cartão com o número " + cartao.getNumero() + ".", e);
            }
            throw erro("Não foi possível atualizar o cartão.", e);
        }
    }

    @Override
    public void remover(int id) {
        try (Connection conn = DBConnection.getConnection()) {
            int linhas = SqlUtil.executar(conn, "DELETE FROM dbo.CartaoMagnetico WHERE IdCartao = ?",
                    ps -> ps.setInt(1, id));
            if (linhas == 0) {
                throw new DAOException("O cartão com id " + id + " não existe.");
            }
        } catch (SQLException e) {
            if (SqlUtil.violaChaveEstrangeira(e)) {
                throw new DAOException("O cartão tem reservas ou registos de acesso associados e não pode ser removido.", e);
            }
            throw erro("Não foi possível remover o cartão.", e);
        }
    }

    @Override
    public Optional<CartaoMagnetico> procurarPorId(int id) {
        return procurarUm("WHERE IdCartao = ?", ps -> ps.setInt(1, id));
    }

    @Override
    public Optional<CartaoMagnetico> procurarPorNumero(int numero) {
        return procurarUm("WHERE Numero = ?", ps -> ps.setInt(1, numero));
    }

    @Override
    public List<CartaoMagnetico> listarTodos() {
        try (Connection conn = DBConnection.getConnection()) {
            return comColunas(colunas -> {
                String sql = "SELECT " + colunas + " FROM dbo.CartaoMagnetico ORDER BY Numero";
                try (PreparedStatement ps = conn.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {
                    List<CartaoMagnetico> lista = new ArrayList<>();
                    while (rs.next()) {
                        lista.add(mapear(rs));
                    }
                    return lista;
                }
            });
        } catch (SQLException e) {
            throw erro("Não foi possível consultar os cartões.", e);
        }
    }

    @Override
    public int proximoNumero() {
        String sql = "SELECT COALESCE(MAX(Numero), 0) FROM dbo.CartaoMagnetico";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return Math.max(rs.getInt(1) + 1, PRIMEIRO_NUMERO_CLIENTE);
        } catch (SQLException e) {
            throw erro("Não foi possível obter o próximo número de cartão.", e);
        }
    }

    /**
     * Lê um cartão com uma ligação que já está aberta (não a fecha). Usado pelo {@link ReservaDAOSQL}
     * para ler o cartão de cada reserva na mesma ligação, em vez de abrir uma ligação por cartão (S6-01).
     *
     * @param conn ligação aberta
     * @param id   identificador do cartão
     * @return o cartão, ou vazio se não existir
     * @throws SQLException se a consulta falhar
     */
    static Optional<CartaoMagnetico> lerPorId(Connection conn, int id) throws SQLException {
        return procurarUm(conn, "WHERE IdCartao = ?", ps -> ps.setInt(1, id));
    }

    private Optional<CartaoMagnetico> procurarUm(String condicao, SqlUtil.Preparador p) {
        try (Connection conn = DBConnection.getConnection()) {
            return procurarUm(conn, condicao, p);
        } catch (SQLException e) {
            throw erro("Não foi possível consultar o cartão.", e);
        }
    }

    private static Optional<CartaoMagnetico> procurarUm(Connection conn, String condicao, SqlUtil.Preparador p)
            throws SQLException {
        return comColunas(colunas -> {
            String sql = "SELECT " + colunas + " FROM dbo.CartaoMagnetico " + condicao;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                p.preparar(ps);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
                }
            }
        });
    }

    /**
     * Corre a consulta com a coluna IdUtilizador; se a base de dados ainda não a tem (script do S6-06
     * por correr), repete-a sem ela, com todos os cartões sem dono.
     */
    private static <T> T comColunas(Consulta<T> consulta) throws SQLException {
        try {
            return consulta.executar(COLUNAS);
        } catch (SQLException e) {
            if (e.getErrorCode() != ERRO_COLUNA_INEXISTENTE) {
                throw e;
            }
            return consulta.executar(COLUNAS_SEM_DONO);
        }
    }

    /**
     * Cria o erro a mostrar ao utilizador. Se a base de dados ainda não tem a coluna {@code Ativo} ou
     * {@code IdUtilizador} (erro 207 do SQL Server, "Invalid column name"), diz quais são os scripts
     * que faltam correr.
     */
    private static DAOException erro(String mensagem, SQLException e) {
        if (e.getErrorCode() == ERRO_COLUNA_INEXISTENTE) {
            return new DAOException("A tabela dbo.CartaoMagnetico ainda não tem as colunas Ativo e IdUtilizador. "
                    + "Execute os scripts DATABASE/EsquemaDB_2_2_checkin.sql e "
                    + "DATABASE/EsquemaDB_S6_picagem_completa.sql na base de dados.", e);
        }
        return new DAOException(mensagem, e);
    }

    private static CartaoMagnetico mapear(ResultSet rs) throws SQLException {
        int idUtilizador = rs.getInt("IdUtilizador");
        Integer dono = rs.wasNull() ? null : idUtilizador;
        return new CartaoMagnetico(rs.getInt("IdCartao"), rs.getInt("Numero"),
                rs.getBoolean("Mestre"), rs.getBoolean("Ativo"), dono);
    }
}