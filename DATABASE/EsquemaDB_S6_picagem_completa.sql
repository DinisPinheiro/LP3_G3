/* =====================================================================
   Hotel4You - S6-06 Picagem de cartão completa

   O que faz:
     1. Cria a tabela dbo.LocalHotel (locais onde se pica o cartão) e os 8 locais do hotel.
     2. Liga dbo.Passagem a dbo.LocalHotel (o local passa a ser uma chave estrangeira em vez de
        uma lista fixa de 4 valores) e cria o índice para consultar passagens por local.
     3. Acrescenta a dbo.CartaoMagnetico a coluna IdUtilizador (dono do cartão de funcionário).
     4. Garante os dois cartões mestre (códigos 1 e 2) e emite um cartão a cada gestor,
        rececionista e empresa de restauração que ainda não tenha.

   Precisa de EsquemaDB_2_1.sql, EsquemaDB_2_2_checkin.sql e EsquemaDB_S6_passagens.sql.
   Pode ser executado mais do que uma vez: só cria o que ainda não existe. Para emitir cartões
   a funcionários criados entretanto sem cartão, volte a executar o passo 4.
   ===================================================================== */
SET NOCOUNT ON;
SET QUOTED_IDENTIFIER ON;   -- necessário para o índice filtrado UX_Cartao_Utilizador
SET ANSI_NULLS ON;
GO

USE [2026_LP3_FEIRA_G3];
GO

SELECT DB_NAME() AS BaseDeDadosAtual;   -- tem de ser 2026_LP3_FEIRA_G3 (a que a aplicação usa)
GO


/* ---------------------------------------------------------------------
   0. Confirma que as tabelas de que depende já existem
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.Passagem', N'U') IS NULL
    OR OBJECT_ID(N'dbo.CartaoMagnetico', N'U') IS NULL
    OR OBJECT_ID(N'dbo.Utilizador', N'U') IS NULL
    RAISERROR(N'Faltam tabelas. Execute primeiro EsquemaDB_2_1.sql, EsquemaDB_2_2_checkin.sql e EsquemaDB_S6_passagens.sql.', 16, 1);
GO


/* ---------------------------------------------------------------------
   1. Locais do hotel
      Piso NULL só no QUARTO (o piso depende do quarto: 2 ou 3).
      Codigo é NVARCHAR(30), igual a dbo.Passagem.Local, para poder ser chave estrangeira.
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.LocalHotel', N'U') IS NULL
BEGIN
CREATE TABLE dbo.LocalHotel (
                                IdLocal INT IDENTITY(1,1) NOT NULL,
                                Codigo  NVARCHAR(30)      NOT NULL,
                                Nome    NVARCHAR(60)      NOT NULL,
                                Piso    SMALLINT          NULL,

                                CONSTRAINT PK_LocalHotel        PRIMARY KEY (IdLocal),
                                CONSTRAINT UQ_LocalHotel_Codigo UNIQUE (Codigo),
                                CONSTRAINT CK_LocalHotel_Piso   CHECK (Piso BETWEEN -1 AND 3),
                                CONSTRAINT CK_LocalHotel_Quarto CHECK ((Codigo = N'QUARTO' AND Piso IS NULL)
                                    OR (Codigo <> N'QUARTO' AND Piso IS NOT NULL))
);
END;
GO

IF OBJECT_ID(N'dbo.LocalHotel', N'U') IS NOT NULL
BEGIN
INSERT INTO dbo.LocalHotel (Codigo, Nome, Piso)
SELECT v.Codigo, v.Nome, v.Piso
FROM (VALUES
          (N'QUARTO',       N'Quarto',               CAST(NULL AS SMALLINT)),
          (N'RESTAURANTE',  N'Restaurante',          CAST(1 AS SMALLINT)),
          (N'PISCINA',      N'Piscina',              CAST(1 AS SMALLINT)),
          (N'COZINHA',      N'Cozinha',              CAST(1 AS SMALLINT)),
          (N'RECECAO',      N'Receção',              CAST(0 AS SMALLINT)),
          (N'BAR',          N'Bar',                  CAST(0 AS SMALLINT)),
          (N'CONFERENCIAS', N'Sala de conferências', CAST(0 AS SMALLINT)),
          (N'ARMAZEM',      N'Armazém',              CAST(-1 AS SMALLINT))
     ) AS v (Codigo, Nome, Piso)
WHERE NOT EXISTS (SELECT 1 FROM dbo.LocalHotel l WHERE l.Codigo = v.Codigo);
END;
GO


/* ---------------------------------------------------------------------
   2. dbo.Passagem passa a referenciar dbo.LocalHotel
   --------------------------------------------------------------------- */
IF EXISTS (SELECT 1 FROM sys.check_constraints
           WHERE name = N'CK_Passagem_Local' AND parent_object_id = OBJECT_ID(N'dbo.Passagem'))
ALTER TABLE dbo.Passagem DROP CONSTRAINT CK_Passagem_Local;
GO

IF OBJECT_ID(N'dbo.Passagem', N'U') IS NOT NULL
    AND OBJECT_ID(N'dbo.LocalHotel', N'U') IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_Passagem_LocalHotel')