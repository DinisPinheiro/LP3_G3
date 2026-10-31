package com.hotel4you.hotel4you.DAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;

/**
 * Pequenas funções partilhadas pelos DAO SQL (só existem para não repetir código de JDBC).
 * Visível apenas dentro do pacote DAO: nenhuma outra camada deve tocar em JDBC.
 */
final class SqlUtil {

    /** Preenche os parâmetros {@code ?} de um {@link PreparedStatement}. */
    @FunctionalInterface
    interface Preparador {
        void preparar(PreparedStatement ps) throws SQLException;
    }

    private SqlUtil() {
    }

    /**
     * Executa um INSERT e devolve o identificador gerado pela base de dados.
     *
     * @param conn ligação (pode estar dentro de uma transação)
     * @param sql  INSERT com parâmetros {@code ?}
     * @param p    preenche os parâmetros
     * @return o id gerado (IDENTITY)
     * @throws SQLException se o INSERT falhar ou não devolver id
     */
    static int inserirComId(Connection conn, String sql, Preparador p) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            p.preparar(ps);
            ps.executeUpdate();
            try (ResultSet chaves = ps.getGeneratedKeys()) {
                if (!chaves.next()) {
                    throw new SQLException("A base de dados não devolveu o identificador gerado.");
                }
                return chaves.getInt(1);
            }
        }
    }

    /**
     * Executa um UPDATE ou DELETE.
     *
     * @param conn ligação
     * @param sql  instrução com parâmetros {@code ?}
     * @param p    preenche os parâmetros
     * @return número de linhas afetadas
     * @throws SQLException se a instrução falhar
     */
    static int executar(Connection conn, String sql, Preparador p) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            p.preparar(ps);
            return ps.executeUpdate();
        }
    }

    /**
     * @param e erro SQL
     * @return {@code true} se foi violada uma restrição UNIQUE (SQL Server 2627/2601; SQLState 23505)
     */
    static boolean violaUnicidade(SQLException e) {
        for (SQLException x = e; x != null; x = x.getNextException()) {
            if (x.getErrorCode() == 2627 || x.getErrorCode() == 2601 || "23505".equals(x.getSQLState())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param e erro SQL
     * @return {@code true} se foi violada uma chave estrangeira (SQL Server 547; SQLState 23503)
     */
    static boolean violaChaveEstrangeira(SQLException e) {
        for (SQLException x = e; x != null; x = x.getNextException()) {
            if (x.getErrorCode() == 547 || "23503".equals(x.getSQLState())) {
                return true;
            }
        }
        return false;
    }

    /** Desfaz a transação em curso sem esconder o erro original. */
    static void reverter(Connection conn, SQLException original) {
        try {
            conn.rollback();
        } catch (SQLException e) {
            original.addSuppressed(e);
        }
    }

    /** Grava um {@link LocalDateTime} que pode ser {@code null}. */
    static void definirDataHora(PreparedStatement ps, int indice, LocalDateTime valor) throws SQLException {
        if (valor == null) {
            ps.setNull(indice, Types.TIMESTAMP);
        } else {
            ps.setTimestamp(indice, Timestamp.valueOf(valor));
        }
    }

    /** Lê um {@link LocalDateTime} que pode ser {@code null}. */
    static LocalDateTime lerDataHora(ResultSet rs, String coluna) throws SQLException {
        Timestamp t = rs.getTimestamp(coluna);
        return t == null ? null : t.toLocalDateTime();
    }
}
