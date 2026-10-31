package com.hotel4you.hotel4you.DAO.db;

import com.hotel4you.hotel4you.DAO.DAOException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.Security;

public final class DBConnection {

    public static final String FICHEIRO_PREDEFINIDO = "db.properties";

    public static final String PROPRIEDADE_FICHEIRO = "hotel4you.config";

    private static final int VALIDACAO_SEGUNDOS = 3;
    private static final String PREFIXO_AMBIENTE = "HOTEL4YOU_DB_";
    private static final Logger LOG = Logger.getLogger(DBConnection.class.getName());

    private static volatile Properties configuracao;

    static {
        // Servidor do ISEP antigo: só negoceia TLS 1.0/1.1, que o Java 17 desativa por omissão.
        Security.setProperty("jdk.tls.disabledAlgorithms",
                "SSLv3, DTLSv1.0, RC4, DES, MD5withRSA, DH keySize < 1024, EC keySize < 224, anon, NULL");
        System.setProperty("jdk.tls.client.protocols", "TLSv1,TLSv1.1,TLSv1.2");
        // Descomenta só para diagnóstico, se continuar a falhar:
        // System.setProperty("javax.net.debug", "ssl:handshake");
    }

    private DBConnection() {
    }

    /**
     * Abre uma ligação nova. Quem a pede é responsável por a fechar (try-with-resources).
     *
     * @return ligação aberta à base de dados
     * @throws DAOException se a configuração estiver em falta ou não for possível ligar
     */
    public static Connection getConnection() {
        Properties cfg = configuracao();
        String url = construirUrl(cfg);
        String utilizador = valor(cfg, "user", null);
        String password = valor(cfg, "password", null);
        try {
            if (utilizador == null) {
                return DriverManager.getConnection(url);
            }
            return DriverManager.getConnection(url, utilizador, password == null ? "" : password);
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Falha ao ligar à base de dados", e);
            throw new DAOException("Não foi possível ligar à base de dados Hotel4You. Confirme que o SQL Server "
                    + "está a correr, que o script db/Hotel4You.sql foi executado e que o utilizador e a "
                    + "palavra-chave em '" + FICHEIRO_PREDEFINIDO + "' estão corretos.", e);
        }
    }

    /**
     * Testa a ligação (abre e fecha uma ligação). Usado no arranque da aplicação.
     *
     * @throws DAOException se não for possível ligar
     */
    public static void verificarLigacao() {
        try (Connection conn = getConnection()) {
            if (!conn.isValid(VALIDACAO_SEGUNDOS)) {
                throw new DAOException("A ligação à base de dados Hotel4You foi aberta mas não responde.");
            }
        } catch (SQLException e) {
            throw new DAOException("Não foi possível testar a ligação à base de dados Hotel4You.", e);
        }
    }

    /** Esquece a configuração já lida, para ser lida de novo na próxima ligação (usado nos testes). */
    public static void recarregarConfiguracao() {
        configuracao = null;
    }

    private static Properties configuracao() {
        Properties atual = configuracao;
        if (atual == null) {
            synchronized (DBConnection.class) {
                if (configuracao == null) {
                    configuracao = lerFicheiro();
                }
                atual = configuracao;
            }
        }
        return atual;
    }

    private static Properties lerFicheiro() {
        Properties props = new Properties();
        Path caminho = Path.of(System.getProperty(PROPRIEDADE_FICHEIRO, FICHEIRO_PREDEFINIDO));
        if (Files.isRegularFile(caminho)) {
            try (InputStream in = Files.newInputStream(caminho)) {
                props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new DAOException("Não foi possível ler o ficheiro de configuração '" + caminho + "'.", e);
            }
        }
        return props;
    }

    private static String construirUrl(Properties cfg) {
        String urlCompleta = valor(cfg, "url", null);
        if (urlCompleta != null) {
            return urlCompleta;
        }
        if (valor(cfg, "user", null) == null) {
            throw new DAOException("Falta a configuração da base de dados. Verifique 'db.properties'" +
                    "(na pasta raiz) e preencha db.user e db.password.");
        }
        return "jdbc:sqlserver://" + valor(cfg, "host", "localhost") + ":" + valor(cfg, "port", "1433")
                + ";databaseName=" + valor(cfg, "name", "Hotel4You")
                + ";encrypt=" + valor(cfg, "encrypt", "true")
                + ";trustServerCertificate=" + valor(cfg, "trustServerCertificate", "true")
                + ";loginTimeout=5;";
    }

    /** Variável de ambiente, senão ficheiro, senão valor por omissão. Valores em branco contam como em falta. */
    private static String valor(Properties cfg, String chave, String porOmissao) {
        String doAmbiente = System.getenv(PREFIXO_AMBIENTE + chave.toUpperCase());
        if (doAmbiente != null && !doAmbiente.isBlank()) {
            return doAmbiente.trim();
        }
        String doFicheiro = cfg.getProperty("db." + chave);
        if (doFicheiro != null && !doFicheiro.isBlank()) {
            return doFicheiro.trim();
        }
        return porOmissao;
    }
}
