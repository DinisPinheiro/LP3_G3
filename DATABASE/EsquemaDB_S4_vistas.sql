/* =====================================================================
   Hotel4You - Valores das vistas especiais editáveis pelo gestor

   Corre-se POR CIMA do EsquemaDB_2_1.sql (não apaga nada): cria a tabela dbo.SuplementoVista com o
   valor a somar ao preço base por noite de cada vista. Os valores iniciais são os de antes
   (mar +20, montanha +15); a partir daqui o gestor altera-os no ecrã "Valores das vistas especiais".
   Pode correr-se várias vezes. Se o EsquemaDB_2_1.sql for executado outra vez (apaga tudo),
   este script tem de correr a seguir.
   ===================================================================== */
SET NOCOUNT ON;
GO

USE [2026_LP3_FEIRA_G3];
GO

IF OBJECT_ID(N'dbo.SuplementoVista', N'U') IS NULL
CREATE TABLE dbo.SuplementoVista (
    Vista  VARCHAR(20)   NOT NULL,
    Valor  DECIMAL(10,2) NOT NULL CONSTRAINT DF_SuplVista_Valor DEFAULT (0),

    CONSTRAINT PK_SuplementoVista        PRIMARY KEY (Vista),
    CONSTRAINT CK_SuplementoVista_Vista  CHECK (Vista IN ('NENHUMA', 'MAR', 'MONTANHA')),
    CONSTRAINT CK_SuplementoVista_Valor  CHECK (Valor >= 0),
    CONSTRAINT CK_SuplementoVista_Nenhum CHECK (Vista <> 'NENHUMA' OR Valor = 0)
);
GO

IF NOT EXISTS (SELECT 1 FROM dbo.SuplementoVista WHERE Vista = 'NENHUMA')
    INSERT INTO dbo.SuplementoVista (Vista, Valor) VALUES ('NENHUMA', 0.00);
IF NOT EXISTS (SELECT 1 FROM dbo.SuplementoVista WHERE Vista = 'MAR')
    INSERT INTO dbo.SuplementoVista (Vista, Valor) VALUES ('MAR', 20.00);
IF NOT EXISTS (SELECT 1 FROM dbo.SuplementoVista WHERE Vista = 'MONTANHA')
    INSERT INTO dbo.SuplementoVista (Vista, Valor) VALUES ('MONTANHA', 15.00);
GO

SELECT Vista, Valor FROM dbo.SuplementoVista ORDER BY Vista;
GO
