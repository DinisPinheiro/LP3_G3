package com.hotel4you.hotel4you.DAO;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    // Ajusta a URL, o utilizador e a password conforme a tua base de dados (ex: SQL Server / MySQL)
    private static final String URL = "jdbc:sqlserver://localhost:1433;databaseName=Hotel4You;encrypt=true;trustServerCertificate=true;";
    private static final String USER = "sa";
    private static final String PASSWORD = "tua_password";

    public static Connection getConnection() throws SQLException {
        try {
            // Exemplo para SQL Server (ou altera o driver se usares MySQL/PostgreSQL)
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver JDBC não encontrado!", e);
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}