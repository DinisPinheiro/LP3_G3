/* =====================================================================
   Hotel4You - S7-03 Importação de ficheiros JSON

   Corre-se POR CIMA dos scripts anteriores (EsquemaDB_S5_fornecedor_importacoes_xsd.sql, ...).
   Não apaga nada e pode correr-se várias vezes: só faz o que ainda não estiver feito.

   O que faz:
     1. ImportacaoFornecedor: nova coluna Formato (XML ou JSON), com o formato do ficheiro enviado pelo
        fornecedor. Fica guardada no histórico e é com ela que a aprovação sabe como ler o ficheiro.
        As importações que já existiam eram todas XML, por isso ficam com 'XML'.
   ===================================================================== */
SET NOCOUNT ON;
GO

USE [2026_LP3_FEIRA_G3];
GO

/* ---------------------------------------------------------------------
   1. (S7-03) Formato do ficheiro importado
   --------------------------------------------------------------------- */
IF COL_LENGTH(N'dbo.ImportacaoFornecedor', N'Formato') IS NULL
    ALTER TABLE dbo.ImportacaoFornecedor
        ADD Formato VARCHAR(10) NOT NULL CONSTRAINT DF_Importacao_Formato DEFAULT ('XML');
GO

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_Importacao_Formato')
    ALTER TABLE dbo.ImportacaoFornecedor
        ADD CONSTRAINT CK_Importacao_Formato CHECK (Formato IN ('XML', 'JSON'));
GO


/* ---------------------------------------------------------------------
   Verificação: deve aparecer a coluna Formato, sem mensagens de erro a vermelho.
   --------------------------------------------------------------------- */
SELECT IdImportacao, IdFornecedor, NomeFicheiro, Formato, NumeroEncomenda, Estado, DataImportacao
FROM dbo.ImportacaoFornecedor ORDER BY IdImportacao;
GO

PRINT N'S7-03: formato (XML/JSON) das importações configurado.';
GO
