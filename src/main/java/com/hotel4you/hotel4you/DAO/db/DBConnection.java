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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.Security;

/**
 * Única classe que abre ligações à base de dados Hotel4You (SQL Server do ISEP).
 * <p>
 * Cada operação de um DAO pede uma ligação com {@link #getConnection()} e fecha-a no fim com
 * try-with-resources. Como abrir uma ligação ao servidor do ISEP demora (rede + TLS + login), cada
 * operação deve abrir uma só ligação e fazer nela todas as consultas de que precisa (S6-01).
 * A configuração vem de {@code db.properties} (na raiz do projeto) ou de variáveis de ambiente
 * {@code HOTEL4YOU_DB_*}; não há credenciais no código.
 */
public final class DBConnection {

    public static final String FICHEIRO_PREDEFINIDO = "db.properties";

    public static final String PROPRIEDADE_FICHEIRO = "hotel4you.config";

    private static final int VALIDACAO_SEGUNDOS = 3;
    private static final String PREFIXO_AMBIENTE = "HOTEL4YOU_DB_";
    private static final Logger LOG = Logger.getLogger(DBConnection.class.getName());

    private static volatile Properties configuracao;

    /** Ligações abertas desde o arranque (usado nas medições e nos testes da S6-01). */
    private static final AtomicInteger TOTAL_LIGACOES = new AtomicInteger();

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
            Connection ligacao = utilizador == null
                    ? DriverManager.getConnection(url)
                    : DriverManager.getConnection(url, utilizador, password == null ? "" : password);
            TOTAL_LIGACOES.incrementAndGet();
            return ligacao;
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Falha ao ligar à base de dados", e);
            throw new DAOException("Não foi possível ligar à base de dados Hotel4You. Confirme que tem ligação "
                    + "à rede, que os scripts da pasta DATABASE foram executados e que o "
                    + "utilizador e a palavra-chave em '" + FICHEIRO_PREDEFINIDO + "' estão corretos.", e);
        }
    }

    /**
     * Número de ligações abertas desde o arranque da aplicação. Serve para medir quantas ligações
     * uma operação abre (por exemplo, listar uma página de reservas deve abrir só uma).
     *
     * @return total de ligações abertas com sucesso por {@link #getConnection()}
     */
    public static int totalLigacoes() {
        return TOTAL_LIGACOES.get();
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
            throw new DAOException("Falta a configuração da base de dados. Verifique 'db.properties' "
                    + "(na pasta raiz) e preencha db.user e db.password.");
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
