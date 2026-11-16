/* =====================================================================
   Hotel4You - S5-02 Perfil Fornecedor e importação de XML
             - S5-03 Aprovação de ficheiros de fornecedor
             - S5-05 Importação do XSD

   Corre-se POR CIMA dos scripts anteriores (EsquemaDB_2_1.sql, ...). Não apaga nada e pode
   correr-se várias vezes: só faz o que ainda não estiver feito.
   Se o EsquemaDB_2_1.sql for executado outra vez (apaga tudo), este script tem de correr a seguir.

   O que faz:
     1. (S5-02) Fornecedor: novas colunas Morada, CodigoPostal e Cidade. Têm valor por omissão
        vazio para que as versões antigas da aplicação (base de dados partilhada) continuem a
        conseguir inserir fornecedores. O código do fornecedor é a coluna Nif (PartyIdentifier do XML)
        e a ligação à conta de login já existia (Fornecedor.IdUtilizador).
     2. (S5-03) ImportacaoFornecedor: um registo por ficheiro XML enviado pelo fornecedor, com o
        estado PENDENTE, APROVADO ou REJEITADO. Só os ficheiros APROVADOS dão entrada em stock.
     3. (S5-05) FicheiroXsd: XSD importado pelo gestor; o mais recente é o que valida os XML.
        Sem nenhum registo, a aplicação usa o XSD que vem com ela.
   ===================================================================== */
SET NOCOUNT ON;
GO

USE [2026_LP3_FEIRA_G3];
GO

/* ---------------------------------------------------------------------
   1. (S5-02) Dados do fornecedor
   --------------------------------------------------------------------- */
IF COL_LENGTH(N'dbo.Fornecedor', N'Morada') IS NULL
    ALTER TABLE dbo.Fornecedor ADD Morada NVARCHAR(200) NOT NULL CONSTRAINT DF_Fornecedor_Morada DEFAULT (N'');
GO

IF COL_LENGTH(N'dbo.Fornecedor', N'CodigoPostal') IS NULL
    ALTER TABLE dbo.Fornecedor ADD CodigoPostal NVARCHAR(20) NOT NULL CONSTRAINT DF_Fornecedor_CodigoPostal DEFAULT (N'');
GO

IF COL_LENGTH(N'dbo.Fornecedor', N'Cidade') IS NULL
    ALTER TABLE dbo.Fornecedor ADD Cidade NVARCHAR(100) NOT NULL CONSTRAINT DF_Fornecedor_Cidade DEFAULT (N'');
GO


/* ---------------------------------------------------------------------
   2. (S5-03) Ficheiros XML enviados pelos fornecedores
      Conteudo guarda os bytes do XML tal como foi enviado; a leitura e a entrada em stock
      só acontecem quando o gestor ou o rececionista aprova.
      IdDecisor/DataDecisao ficam vazios enquanto o ficheiro está PENDENTE.
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.ImportacaoFornecedor', N'U') IS NULL
CREATE TABLE dbo.ImportacaoFornecedor (
    IdImportacao     INT IDENTITY(1,1)  NOT NULL,
    IdFornecedor     INT                NOT NULL,
    NomeFicheiro     NVARCHAR(260)      NOT NULL,
    NumeroEncomenda  NVARCHAR(50)       NOT NULL,
    Conteudo         VARBINARY(MAX)     NOT NULL,
    Estado           VARCHAR(10)        NOT NULL CONSTRAINT DF_Importacao_Estado DEFAULT ('PENDENTE'),
    DataImportacao   DATETIME2(0)       NOT NULL CONSTRAINT DF_Importacao_Data DEFAULT (SYSDATETIME()),
    IdDecisor        INT                NULL,
    DataDecisao      DATETIME2(0)       NULL,

    CONSTRAINT PK_ImportacaoFornecedor            PRIMARY KEY (IdImportacao),
    CONSTRAINT FK_Importacao_Fornecedor           FOREIGN KEY (IdFornecedor) REFERENCES dbo.Fornecedor (IdFornecedor),
    CONSTRAINT FK_Importacao_Decisor              FOREIGN KEY (IdDecisor)    REFERENCES dbo.Utilizador (IdUtilizador),
    CONSTRAINT CK_Importacao_Estado               CHECK (Estado IN ('PENDENTE', 'APROVADO', 'REJEITADO')),
    CONSTRAINT CK_Importacao_Decisao              CHECK ((Estado = 'PENDENTE' AND IdDecisor IS NULL AND DataDecisao IS NULL)
                                                      OR (Estado <> 'PENDENTE' AND IdDecisor IS NOT NULL AND DataDecisao IS NOT NULL))
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_Importacao_Estado')
    CREATE INDEX IX_Importacao_Estado ON dbo.ImportacaoFornecedor (Estado, DataImportacao);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_Importacao_Fornecedor')
    CREATE INDEX IX_Importacao_Fornecedor ON dbo.ImportacaoFornecedor (IdFornecedor, DataImportacao);
GO


/* ---------------------------------------------------------------------
   3. (S5-05) XSD importado pelo gestor
      Cada importação cria uma linha nova; o XSD em vigor é o de maior IdXsd.
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.FicheiroXsd', N'U') IS NULL
CREATE TABLE dbo.FicheiroXsd (
    IdXsd            INT IDENTITY(1,1)  NOT NULL,
    NomeFicheiro     NVARCHAR(260)      NOT NULL,
    Conteudo         VARBINARY(MAX)     NOT NULL,
    DataImportacao   DATETIME2(0)       NOT NULL CONSTRAINT DF_FicheiroXsd_Data DEFAULT (SYSDATETIME()),
    IdUtilizador     INT                NOT NULL,

    CONSTRAINT PK_FicheiroXsd            PRIMARY KEY (IdXsd),
    CONSTRAINT FK_FicheiroXsd_Utilizador FOREIGN KEY (IdUtilizador) REFERENCES dbo.Utilizador (IdUtilizador)
);
GO


/* ---------------------------------------------------------------------
   Verificação: no fim devem aparecer três tabelas, sem mensagens de erro a vermelho.
   --------------------------------------------------------------------- */
SELECT IdFornecedor, Nome, Nif, Morada, CodigoPostal, Cidade, IdUtilizador FROM dbo.Fornecedor ORDER BY Nome;
SELECT IdImportacao, IdFornecedor, NomeFicheiro, NumeroEncomenda, Estado, DataImportacao FROM dbo.ImportacaoFornecedor ORDER BY IdImportacao;
SELECT IdXsd, NomeFicheiro, DataImportacao FROM dbo.FicheiroXsd ORDER BY IdXsd;
GO

PRINT N'S5-02, S5-03 e S5-05: fornecedor, importações e XSD configurados.';
GO
