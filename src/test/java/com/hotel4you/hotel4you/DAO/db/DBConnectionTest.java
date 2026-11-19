package com.hotel4you.hotel4you.DAO.db;

import com.hotel4you.hotel4you.DAO.BaseDadosH2;
import com.hotel4you.hotel4you.DAO.DAOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes da abertura e do fecho de ligações no {@link DBConnection} (S6-01).
 * Usam uma base de dados H2 em memória, nunca a base de dados do ISEP.
 */
class DBConnectionTest {

    private BaseDadosH2 bd;

    @BeforeEach
    void criarBaseDeDados() {
        bd = BaseDadosH2.vazia("dbconnection");
    }

    @AfterEach
    void apagarBaseDeDados() throws SQLException, IOException {
        bd.fechar();
    }

    @Test
    void ligacaoEstaAbertaDuranteAOperacaoEFechadaNoFim() throws SQLException {
        Connection usada;
        try (Connection conn = DBConnection.getConnection()) {
            usada = conn;
            assertFalse(conn.isClosed());
            assertTrue(conn.isValid(2));
            assertEquals(1, bd.ligacoesAbertas());
        }
        assertTrue(usada.isClosed());
        assertEquals(0, bd.ligacoesAbertas());
    }

    @Test
    void cadaPedidoAbreUmaLigacaoNovaEContaParaOTotal() throws SQLException {
        int antes = DBConnection.totalLigacoes();
        try (Connection primeira = DBConnection.getConnection();
             Connection segunda = DBConnection.getConnection()) {
            assertFalse(primeira == segunda);
            assertEquals(2, bd.ligacoesAbertas());
        }
        assertEquals(2, DBConnection.totalLigacoes() - antes);
        assertEquals(0, bd.ligacoesAbertas());
    }

    @Test
    void verificarLigacaoNaoDeixaLigacoesAbertas() throws SQLException {
        DBConnection.verificarLigacao();
        assertEquals(0, bd.ligacoesAbertas());
    }

    @Test
    void servidorInacessivelDaMensagemClaraEmVezDeRebentar() throws IOException {
        comConfiguracao("db.url=jdbc:hotel4you-servidor-inexistente:teste");

        DAOException erro = assertThrows(DAOException.class, DBConnection::getConnection);
        assertTrue(erro.getMessage().startsWith("Não foi possível ligar à base de dados Hotel4You"));
        assertThrows(DAOException.class, DBConnection::verificarLigacao);
    }

    @Test
    void semUtilizadorNaConfiguracaoDaMensagemClara() throws IOException {
        comConfiguracao("db.host=localhost");

        DAOException erro = assertThrows(DAOException.class, DBConnection::getConnection);
        assertTrue(erro.getMessage().contains("db.properties"));
    }

    /** Troca a configuração do DBConnection só para este teste (o @AfterEach repõe a anterior). */
    private static void comConfiguracao(String conteudo) throws IOException {
        Path ficheiro = Files.createTempFile("hotel4you-teste", ".properties");
        ficheiro.toFile().deleteOnExit();
        Files.writeString(ficheiro, conteudo, StandardCharsets.UTF_8);
        System.setProperty(DBConnection.PROPRIEDADE_FICHEIRO, ficheiro.toString());
        DBConnection.recarregarConfiguracao();
    }
}
