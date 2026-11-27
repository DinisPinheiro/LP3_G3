package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.DAO.db.DBConnection;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Base de dados H2 em memória (modo SQL Server) para os testes das ligações (S6-01).
 * <p>
 * Põe o {@link DBConnection} a apontar para esta base de dados através de um ficheiro de configuração
 * temporário, por isso os testes nunca tocam na base de dados do ISEP. Também sabe contar as sessões
 * abertas no H2, o que permite confirmar que uma operação fechou todas as ligações que abriu.
 */
public final class BaseDadosH2 {

    private final String url;
    private final String configAnterior;
    private final Path ficheiroConfig;

    private BaseDadosH2(String nome) {
        this.url = "jdbc:h2:mem:" + nome + ";MODE=MSSQLServer;DB_CLOSE_DELAY=-1";
        this.configAnterior = System.getProperty(DBConnection.PROPRIEDADE_FICHEIRO);
        try {
            this.ficheiroConfig = Files.createTempFile("hotel4you-teste", ".properties");
            Files.writeString(ficheiroConfig, "db.url=" + url + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        System.setProperty(DBConnection.PROPRIEDADE_FICHEIRO, ficheiroConfig.toString());
        DBConnection.recarregarConfiguracao();
    }

    /**
     * Cria uma base de dados vazia e põe o {@link DBConnection} a usá-la.
     *
     * @param nome nome da base de dados em memória (um por classe de teste)
     * @return a base de dados de teste
     */
    public static BaseDadosH2 vazia(String nome) {
        return new BaseDadosH2(nome);
    }

    /**
     * Cria uma base de dados com as tabelas usadas pelas reservas, pelo stock e pelos fornecedores
     * (só as colunas que os DAO leem) e põe o {@link DBConnection} a usá-la.
     *
     * @param nome nome da base de dados em memória (um por classe de teste)
     * @return a base de dados de teste
     * @throws SQLException se não for possível criar as tabelas
     */
    public static BaseDadosH2 comTabelas(String nome) throws SQLException {
        BaseDadosH2 bd = new BaseDadosH2(nome);
        bd.executar(
                "CREATE SCHEMA dbo",
                "CREATE TABLE dbo.Cliente (IdCliente INT IDENTITY(1,1) PRIMARY KEY, Nome NVARCHAR(100) NOT NULL, "
                        + "Nif VARCHAR(20) NOT NULL UNIQUE, Contacto VARCHAR(50))",
                "CREATE TABLE dbo.SuplementoVista (Vista VARCHAR(20) PRIMARY KEY, Valor DECIMAL(10,2) NOT NULL)",
                "CREATE TABLE dbo.Quarto (IdQuarto INT IDENTITY(1,1) PRIMARY KEY, Numero INT NOT NULL UNIQUE, "
                        + "Piso INT NOT NULL, Local NVARCHAR(50), Tipo VARCHAR(20) NOT NULL, "
                        + "PrecoBase DECIMAL(10,2) NOT NULL, Vista VARCHAR(20) NOT NULL, Ativo BIT NOT NULL, "
                        + "PrecoPersonalizado BIT NOT NULL DEFAULT 1, EquipamentosPersonalizados BIT NOT NULL DEFAULT 1)",
                "CREATE TABLE dbo.Equipamento (IdEquipamento INT IDENTITY(1,1) PRIMARY KEY, Nome NVARCHAR(100))",
                "CREATE TABLE dbo.Quarto_Equipamento (IdQuarto INT NOT NULL, IdEquipamento INT NOT NULL)",
                "CREATE TABLE dbo.CartaoMagnetico (IdCartao INT IDENTITY(1,1) PRIMARY KEY, Numero INT NOT NULL UNIQUE, "
                        + "Mestre BIT NOT NULL, Ativo BIT NOT NULL, IdUtilizador INT)",
                "CREATE TABLE dbo.Reserva (IdReserva INT IDENTITY(1,1) PRIMARY KEY, IdCliente INT NOT NULL, "
                        + "IdQuarto INT NOT NULL, DataEntrada DATE NOT NULL, DataSaida DATE NOT NULL, "
                        + "Estado VARCHAR(20) NOT NULL, ValorTotal DECIMAL(10,2), DataCheckIn TIMESTAMP, "
                        + "DataCheckOut TIMESTAMP, IdCartao INT, PrecoBaseNoite DECIMAL(10,2), "
                        + "ValorVistaNoite DECIMAL(10,2))",
                "CREATE TABLE dbo.Servico (IdServico INT IDENTITY(1,1) PRIMARY KEY, Nome NVARCHAR(100), "
                        + "Categoria NVARCHAR(50), Descricao NVARCHAR(200), Tipo VARCHAR(10), Valor DECIMAL(10,2), "
                        + "Cobranca VARCHAR(12), Ativo BIT)",
                "CREATE TABLE dbo.ConsumoServico (IdConsumo INT IDENTITY(1,1) PRIMARY KEY, IdReserva INT NOT NULL, "
                        + "IdServico INT NOT NULL, Quantidade INT NOT NULL, Data DATE NOT NULL, "
                        + "ValorUnitario DECIMAL(10,2), Cobranca VARCHAR(12))",
                "CREATE TABLE dbo.Fornecedor (IdFornecedor INT IDENTITY(1,1) PRIMARY KEY, Nome NVARCHAR(100), "
                        + "Nif VARCHAR(20), Contacto VARCHAR(50), Morada NVARCHAR(200) DEFAULT '', "
                        + "CodigoPostal NVARCHAR(20) DEFAULT '', Cidade NVARCHAR(100) DEFAULT '', IdUtilizador INT)",
                "CREATE TABLE dbo.Artigo (IdArtigo INT IDENTITY(1,1) PRIMARY KEY, Nome NVARCHAR(100), "
                        + "Unidade VARCHAR(20), QuantidadeAtual INT, IdFornecedor INT, PrecoUnitario DECIMAL(10,2), "
                        + "UnidadePreco VARCHAR(20), TaxaPercent INT, Consumivel BIT NOT NULL DEFAULT 0, "
                        + "UnidadesPorCaixa INT)",
                "CREATE TABLE dbo.Utilizador (IdUtilizador INT IDENTITY(1,1) PRIMARY KEY, Username VARCHAR(50), "
                        + "PasswordHash VARCHAR(200), Nome NVARCHAR(100), Perfil VARCHAR(30), Ativo BIT)",
                "CREATE TABLE dbo.MovimentoStock (IdMovimento INT IDENTITY(1,1) PRIMARY KEY, IdArtigo INT NOT NULL, "
                        + "IdUtilizador INT NOT NULL, Tipo VARCHAR(12) NOT NULL, Quantidade INT NOT NULL, "
                        + "DataHora TIMESTAMP NOT NULL, Justificacao NVARCHAR(200) NOT NULL DEFAULT '')",
                "CREATE TABLE dbo.QuartoProduto (IdQuarto INT NOT NULL, IdArtigo INT NOT NULL, Quantidade INT NOT NULL, "
                        + "PRIMARY KEY (IdQuarto, IdArtigo))",
                "CREATE TABLE dbo.ConfigTipoQuarto (Tipo VARCHAR(20) PRIMARY KEY, PrecoBase DECIMAL(10,2) NOT NULL)",
                "CREATE TABLE dbo.ConfigTipoQuarto_Equipamento (Tipo VARCHAR(20) NOT NULL, IdEquipamento INT NOT NULL, "
                        + "PRIMARY KEY (Tipo, IdEquipamento))",
                "CREATE TABLE dbo.ConfigTipoQuarto_Produto (Tipo VARCHAR(20) NOT NULL, IdArtigo INT NOT NULL, "
                        + "Quantidade INT NOT NULL, PRIMARY KEY (Tipo, IdArtigo))",
                "CREATE TABLE dbo.ReservaProduto (IdReservaProduto INT IDENTITY(1,1) PRIMARY KEY, "
                        + "IdReserva INT NOT NULL, IdArtigo INT NOT NULL, Quantidade INT NOT NULL)",
                "CREATE TABLE dbo.ServicoProduto (IdServico INT NOT NULL PRIMARY KEY, IdArtigo INT NOT NULL, "
                        + "Quantidade INT NOT NULL)");
        return bd;
    }

    /**
     * Executa instruções SQL diretamente no H2 (para preparar os dados de um teste).
     *
     * @param instrucoes instruções a executar, por ordem
     * @throws SQLException se alguma falhar
     */
    public void executar(String... instrucoes) throws SQLException {
        try (Connection conn = DriverManager.getConnection(url);
             Statement st = conn.createStatement()) {
            for (String sql : instrucoes) {
                st.execute(sql);
            }
        }
    }

    /**
     * Conta as ligações que estão abertas neste momento no H2 (sem contar a que é usada para contar).
     *
     * @return número de ligações abertas pelos DAO e ainda não fechadas
     * @throws SQLException se a contagem falhar
     */
    public int ligacoesAbertas() throws SQLException {
        try (Connection conn = DriverManager.getConnection(url);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM INFORMATION_SCHEMA.SESSIONS")) {
            rs.next();
            return rs.getInt(1) - 1;
        }
    }

    /**
     * Apaga a base de dados e repõe a configuração que o {@link DBConnection} tinha antes do teste.
     *
     * @throws SQLException se não for possível apagar a base de dados
     * @throws IOException  se não for possível apagar o ficheiro de configuração temporário
     */
    public void fechar() throws SQLException, IOException {
        executar("SHUTDOWN");
        if (configAnterior == null) {
            System.clearProperty(DBConnection.PROPRIEDADE_FICHEIRO);
        } else {
            System.setProperty(DBConnection.PROPRIEDADE_FICHEIRO, configAnterior);
        }
        DBConnection.recarregarConfiguracao();
        Files.deleteIfExists(ficheiroConfig);
    }
}
