/* =====================================================================
   Hotel4You - S6-06 Picagem de cartão e registo de passagens

   O que faz:
     1. Cria a tabela dbo.Passagem: uma linha por cada vez que um cartão é picado num local
        (quarto, restaurante, piscina ou cozinha), com a data e a hora.
     2. Cria os índices para consultar as passagens por cartão e por quarto.

   Precisa de dbo.CartaoMagnetico e dbo.Quarto (EsquemaDB_2_1.sql e EsquemaDB_2_2_checkin.sql).
   Pode ser executado mais do que uma vez: só cria o que ainda não existe.
   ===================================================================== */
SET NOCOUNT ON;
GO

-- Sem esta linha o script corre na base de dados escolhida no editor (por omissão, a master),
-- onde não existem dbo.CartaoMagnetico nem dbo.Quarto e as chaves estrangeiras falham.
USE [2026_LP3_FEIRA_G3];
GO


/* ---------------------------------------------------------------------
   0. Confirma que as tabelas de que a dbo.Passagem depende já existem
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.CartaoMagnetico', N'U') IS NULL OR OBJECT_ID(N'dbo.Quarto', N'U') IS NULL
    RAISERROR(N'Faltam as tabelas dbo.CartaoMagnetico ou dbo.Quarto. Execute primeiro EsquemaDB_2_1.sql e EsquemaDB_2_2_checkin.sql.', 16, 1);
GO


/* ---------------------------------------------------------------------
   1. Tabela das passagens
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.Passagem', N'U') IS NULL
    AND OBJECT_ID(N'dbo.CartaoMagnetico', N'U') IS NOT NULL
    AND OBJECT_ID(N'dbo.Quarto', N'U') IS NOT NULL
BEGIN
    CREATE TABLE dbo.Passagem (
        IdPassagem INT IDENTITY(1,1) NOT NULL,
        IdCartao   INT NOT NULL,
        IdQuarto   INT NULL,
        Local      NVARCHAR(30) NOT NULL,
        DataHora   DATETIME2(0) NOT NULL CONSTRAINT DF_Passagem_DataHora DEFAULT SYSDATETIME(),

        CONSTRAINT PK_Passagem PRIMARY KEY (IdPassagem),
        CONSTRAINT FK_Passagem_Cartao FOREIGN KEY (IdCartao) REFERENCES dbo.CartaoMagnetico(IdCartao),
        CONSTRAINT FK_Passagem_Quarto FOREIGN KEY (IdQuarto) REFERENCES dbo.Quarto(IdQuarto),
        CONSTRAINT CK_Passagem_Local CHECK (Local IN ('QUARTO', 'RESTAURANTE', 'PISCINA', 'COZINHA'))
    );
END;
GO


/* ---------------------------------------------------------------------
   2. Índices para as consultas por cartão e por quarto
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.Passagem', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_Passagem_Cartao' AND object_id = OBJECT_ID(N'dbo.Passagem'))
    CREATE INDEX IX_Passagem_Cartao ON dbo.Passagem(IdCartao, DataHora);
GO

IF OBJECT_ID(N'dbo.Passagem', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_Passagem_Quarto' AND object_id = OBJECT_ID(N'dbo.Passagem'))
    CREATE INDEX IX_Passagem_Quarto ON dbo.Passagem(IdQuarto, DataHora);
GO


/* ---------------------------------------------------------------------
   3. Verificação: deve mostrar a tabela dbo.Passagem e os dois índices
   --------------------------------------------------------------------- */
SELECT t.name AS Tabela, i.name AS Indice
FROM sys.tables t
LEFT JOIN sys.indexes i ON i.object_id = t.object_id AND i.name LIKE N'IX_Passagem_%'
WHERE t.name = N'Passagem';
GO
