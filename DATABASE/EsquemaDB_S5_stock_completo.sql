/* =====================================================================
   Hotel4You - S5-04 Gestão de stock completa
             (unidades, preços, taxas e totais das encomendas)

   O que faz:
     1. dbo.Artigo: guarda o último preço unitário, a unidade do preço e a taxa (IVA) do artigo.
     2. dbo.Encomenda: uma linha por encomenda de fornecedor que deu entrada em stock, com o
        total das linhas (sem taxas), o total das taxas e o total com taxas.
     3. dbo.EncomendaLinha: uma linha por produto da encomenda, com produto, quantidade, unidade,
        caixas e unidades, preço unitário, taxa e total da linha.

   O stock do hotel (Artigo.QuantidadeAtual) continua separado do stock disponível, que
   desconta o que está usado em reservas (ver EsquemaDB_S5_servicos_produtos_reserva.sql).
   ===================================================================== */
SET NOCOUNT ON;
GO

USE [2026_LP3_FEIRA_G3];
GO


/* ---------------------------------------------------------------------
   1. Último preço e taxa do artigo (NULL nos artigos que entraram antes desta versão)
   --------------------------------------------------------------------- */
IF COL_LENGTH(N'dbo.Artigo', N'PrecoUnitario') IS NULL
    ALTER TABLE dbo.Artigo ADD PrecoUnitario DECIMAL(10,2) NULL;
GO

IF COL_LENGTH(N'dbo.Artigo', N'UnidadePreco') IS NULL
    ALTER TABLE dbo.Artigo ADD UnidadePreco NVARCHAR(20) NULL;
GO

IF COL_LENGTH(N'dbo.Artigo', N'TaxaPercent') IS NULL
    ALTER TABLE dbo.Artigo ADD TaxaPercent TINYINT NULL;
GO

IF OBJECT_ID(N'dbo.CK_Artigo_Preco', N'C') IS NULL
    ALTER TABLE dbo.Artigo ADD CONSTRAINT CK_Artigo_Preco CHECK (PrecoUnitario >= 0 AND TaxaPercent <= 100);
GO


/* ---------------------------------------------------------------------
   2. Encomenda: cabeçalho e totais
      Os totais são calculados pela aplicação (BigDecimal) a partir das linhas:
          TotalComTaxas = TotalLinhas + TotalTaxas
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.Encomenda', N'U') IS NULL
CREATE TABLE dbo.Encomenda (
    IdEncomenda     INT IDENTITY(1,1)  NOT NULL,
    Numero          NVARCHAR(30)       NOT NULL,
    DataEncomenda   DATE               NOT NULL,
    IdFornecedor    INT                NOT NULL,
    IdUtilizador    INT                NOT NULL,
    DataEntrada     DATETIME2(0)       NOT NULL CONSTRAINT DF_Encomenda_DataEntrada DEFAULT (SYSDATETIME()),
    Moeda           NVARCHAR(3)        NOT NULL CONSTRAINT DF_Encomenda_Moeda DEFAULT (N'EUR'),
    TotalLinhas     DECIMAL(12,2)      NOT NULL,
    TotalTaxas      DECIMAL(12,2)      NOT NULL,
    TotalComTaxas   DECIMAL(12,2)      NOT NULL,

    CONSTRAINT PK_Encomenda PRIMARY KEY (IdEncomenda),
    CONSTRAINT FK_Encomenda_Fornecedor FOREIGN KEY (IdFornecedor) REFERENCES dbo.Fornecedor (IdFornecedor),
    CONSTRAINT FK_Encomenda_Utilizador FOREIGN KEY (IdUtilizador) REFERENCES dbo.Utilizador (IdUtilizador),
    CONSTRAINT CK_Encomenda_Totais CHECK (TotalLinhas >= 0 AND TotalTaxas >= 0
                                          AND TotalComTaxas = TotalLinhas + TotalTaxas)
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_Encomenda_Fornecedor')
    CREATE INDEX IX_Encomenda_Fornecedor ON dbo.Encomenda (IdFornecedor, Numero);
GO


/* ---------------------------------------------------------------------
   3. EncomendaLinha: uma linha por produto
      Quantidade/Unidade: o que foi encomendado ('cx', 'un' ou 'kg').
      QuantidadeCaixas e QuantidadeUnidades: quantas caixas e quantas unidades a linha traz
      (NULL se o XML não o indica).
      PrecoUnitario/UnidadePreco: preço de 1 unidade de medida, por exemplo 2,51 por 'cx'.
          ValorSemTaxa = PrecoUnitario x quantidade na unidade do preço
          ValorTaxa    = ValorSemTaxa x TaxaPercent / 100
          TotalLinha   = ValorSemTaxa + ValorTaxa
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.EncomendaLinha', N'U') IS NULL
CREATE TABLE dbo.EncomendaLinha (
    IdLinha            INT IDENTITY(1,1)  NOT NULL,
    IdEncomenda        INT                NOT NULL,
    Numero             INT                NOT NULL,
    CodigoProduto      NVARCHAR(40)       NOT NULL,
    IdArtigo           INT                NOT NULL,
    Quantidade         INT                NOT NULL,
    Unidade            NVARCHAR(20)       NOT NULL,
    QuantidadeCaixas   INT                NULL,
    QuantidadeUnidades INT                NULL,
    PrecoUnitario      DECIMAL(10,2)      NOT NULL,
    UnidadePreco       NVARCHAR(20)       NOT NULL,
    TaxaPercent        TINYINT            NOT NULL,
    ValorSemTaxa       DECIMAL(12,2)      NOT NULL,
    ValorTaxa          DECIMAL(12,2)      NOT NULL,
    TotalLinha         DECIMAL(12,2)      NOT NULL,

    CONSTRAINT PK_EncomendaLinha PRIMARY KEY (IdLinha),
    CONSTRAINT UQ_EncomendaLinha_Numero UNIQUE (IdEncomenda, Numero),
    CONSTRAINT FK_EncomendaLinha_Encomenda FOREIGN KEY (IdEncomenda) REFERENCES dbo.Encomenda (IdEncomenda),
    CONSTRAINT FK_EncomendaLinha_Artigo    FOREIGN KEY (IdArtigo)    REFERENCES dbo.Artigo (IdArtigo),
    CONSTRAINT CK_EncomendaLinha_Quantidade CHECK (Quantidade > 0
                                                   AND (QuantidadeCaixas IS NULL OR QuantidadeCaixas >= 0)
                                                   AND (QuantidadeUnidades IS NULL OR QuantidadeUnidades >= 0)),
    CONSTRAINT CK_EncomendaLinha_Valores CHECK (PrecoUnitario >= 0 AND TaxaPercent <= 100
                                                AND ValorSemTaxa >= 0 AND ValorTaxa >= 0
                                                AND TotalLinha = ValorSemTaxa + ValorTaxa)
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_EncomendaLinha_Artigo')
    CREATE INDEX IX_EncomendaLinha_Artigo ON dbo.EncomendaLinha (IdArtigo);
GO


/* ---------------------------------------------------------------------
   Verificação: stock do hotel, stock disponível e último preço de cada artigo,
   e os totais das encomendas já registadas.
   --------------------------------------------------------------------- */
SELECT a.Nome, a.Unidade, a.QuantidadeAtual AS StockDoHotel,
       a.QuantidadeAtual - ISNULL((SELECT SUM(rp.Quantidade)
                                   FROM dbo.ReservaProduto rp
                                   JOIN dbo.Reserva r ON r.IdReserva = rp.IdReserva
                                   WHERE rp.IdArtigo = a.IdArtigo AND r.Estado <> 'CANCELADA'), 0) AS StockDisponivel,
       a.PrecoUnitario, a.UnidadePreco, a.TaxaPercent
FROM dbo.Artigo a
ORDER BY a.Nome;

SELECT e.IdEncomenda, e.Numero, f.Nome AS Fornecedor, e.DataEncomenda,
       e.TotalLinhas, e.TotalTaxas, e.TotalComTaxas, e.Moeda
FROM dbo.Encomenda e
JOIN dbo.Fornecedor f ON f.IdFornecedor = e.IdFornecedor
ORDER BY e.IdEncomenda;
GO

PRINT N'S5-04: stock completo (preços, taxas, linhas e totais das encomendas) configurado.';
GO
