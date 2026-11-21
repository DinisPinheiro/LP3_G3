/* =====================================================================
   Hotel4You - S6-02 Importação do XSD (pelo fornecedor)
             - S6-03 Validação de dados do XML (encomenda única por fornecedor)

   Corre-se POR CIMA dos scripts anteriores (EsquemaDB_S5_fornecedor_importacoes_xsd.sql, ...).
   Não apaga nada e pode correr-se várias vezes: só faz o que ainda não estiver feito.

   O que faz:
     1. (S6-02) FicheiroXsd: nova coluna IdFornecedor. O XSD passa a ser do fornecedor que o importa e
        valida apenas os XML desse fornecedor. Não existe XSD geral nem XSD que venha com a aplicação.
        Os XSD que o gestor importou na S5-05 ficam com IdFornecedor vazio e a aplicação ignora-os
        (podem apagar-se com: DELETE FROM dbo.FicheiroXsd WHERE IdFornecedor IS NULL).
     2. (S6-03) ImportacaoFornecedor: o número da encomenda é único por fornecedor, para que o mesmo
        fornecedor não consiga enviar duas vezes a mesma encomenda (a aplicação também o verifica antes
        de enviar; esta restrição é a rede de segurança).
   ===================================================================== */
SET NOCOUNT ON;
GO

USE [2026_LP3_FEIRA_G3];
GO

/* ---------------------------------------------------------------------
   1. (S6-02) XSD por fornecedor
   --------------------------------------------------------------------- */
IF COL_LENGTH(N'dbo.FicheiroXsd', N'IdFornecedor') IS NULL
    ALTER TABLE dbo.FicheiroXsd ADD IdFornecedor INT NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_FicheiroXsd_Fornecedor')
    ALTER TABLE dbo.FicheiroXsd ADD CONSTRAINT FK_FicheiroXsd_Fornecedor
        FOREIGN KEY (IdFornecedor) REFERENCES dbo.Fornecedor (IdFornecedor);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_FicheiroXsd_Fornecedor')
    CREATE INDEX IX_FicheiroXsd_Fornecedor ON dbo.FicheiroXsd (IdFornecedor, IdXsd);
GO


/* ---------------------------------------------------------------------
   2. (S6-03) Número de encomenda único por fornecedor
      Se já existirem encomendas repetidas (por testes anteriores), a restrição não é criada e
      são listadas abaixo: apague as repetidas (DELETE FROM dbo.ImportacaoFornecedor WHERE
      IdImportacao = ...) e volte a correr este script.
   --------------------------------------------------------------------- */
IF EXISTS (SELECT 1 FROM sys.key_constraints WHERE name = N'UQ_Importacao_Fornecedor_Encomenda')
    PRINT N'A restrição UQ_Importacao_Fornecedor_Encomenda já existe.';
ELSE IF EXISTS (SELECT 1 FROM dbo.ImportacaoFornecedor GROUP BY IdFornecedor, NumeroEncomenda HAVING COUNT(*) > 1)
BEGIN
    PRINT N'ATENÇÃO: há encomendas repetidas. A restrição NÃO foi criada. Apague as repetidas e corra de novo.';
    SELECT IdFornecedor, NumeroEncomenda, COUNT(*) AS Vezes, MIN(IdImportacao) AS PrimeiraImportacao
    FROM dbo.ImportacaoFornecedor
    GROUP BY IdFornecedor, NumeroEncomenda
    HAVING COUNT(*) > 1;
END
ELSE
    ALTER TABLE dbo.ImportacaoFornecedor ADD CONSTRAINT UQ_Importacao_Fornecedor_Encomenda
        UNIQUE (IdFornecedor, NumeroEncomenda);
GO


/* ---------------------------------------------------------------------
   Verificação
   --------------------------------------------------------------------- */
SELECT IdXsd, IdFornecedor, NomeFicheiro, DataImportacao FROM dbo.FicheiroXsd ORDER BY IdXsd;
SELECT name AS Restricao FROM sys.key_constraints WHERE name = N'UQ_Importacao_Fornecedor_Encomenda';
GO

PRINT N'S6-02 e S6-03: XSD por fornecedor e encomenda única por fornecedor configurados.';
GO
