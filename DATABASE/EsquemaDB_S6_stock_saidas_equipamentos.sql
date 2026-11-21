/* =====================================================================
   Hotel4You - S6-04 Saída de stock e stock real/disponível
             - S6-05 Equipamentos do quarto ligados ao stock

   O que faz:
     1. (S6-04) dbo.Artigo: coluna Consumivel. Os produtos consumíveis (ex.: champô) gastam-se
        com o uso e, no check-out, saem definitivamente do stock; os outros voltam ao stock.
     2. (S6-04) dbo.MovimentoStock (tabela de movimentos de stock): coluna Justificacao. Cada saída
        de stock fica registada com o produto, a quantidade, a justificação (ex.: televisão avariada),
        o utilizador e a data/hora. Os consumos no check-out ficam como 'UTILIZACAO'.
     3. (S6-05) dbo.QuartoProduto: tabela de ligação entre o quarto e os produtos do stock, com a
        quantidade (ex.: quarto 205 tem 2 toalhas, 2 champôs e 1 televisão).

   Stock real       = Artigo.QuantidadeAtual (o total que o hotel tem)
   Stock disponível = stock real - produtos em uso nas reservas PENDENTE e CHECK_IN
   No check-in os produtos do quarto passam para a reserva (dbo.ReservaProduto) e saem do disponível;
   no check-out voltam ao disponível, exceto os consumíveis, que saem do stock real.

   Precisa de EsquemaDB_2_1.sql e EsquemaDB_S5_servicos_produtos_reserva.sql.
   Pode ser executado mais do que uma vez: só cria o que ainda não existe.
   ===================================================================== */
SET NOCOUNT ON;
GO

USE [2026_LP3_FEIRA_G3];
GO


/* ---------------------------------------------------------------------
   0. Confirma que as tabelas de que este script depende já existem
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.Artigo', N'U') IS NULL OR OBJECT_ID(N'dbo.MovimentoStock', N'U') IS NULL
   OR OBJECT_ID(N'dbo.Quarto', N'U') IS NULL OR OBJECT_ID(N'dbo.ReservaProduto', N'U') IS NULL
    RAISERROR(N'Faltam tabelas. Execute primeiro EsquemaDB_2_1.sql e EsquemaDB_S5_servicos_produtos_reserva.sql.', 16, 1);
GO


/* ---------------------------------------------------------------------
   1. (S6-04) Produtos consumíveis
   --------------------------------------------------------------------- */
IF COL_LENGTH(N'dbo.Artigo', N'Consumivel') IS NULL
    ALTER TABLE dbo.Artigo ADD Consumivel BIT NOT NULL CONSTRAINT DF_Artigo_Consumivel DEFAULT (0);
GO

-- Produtos que já existem e se gastam com o uso (o gestor pode mudar no ecrã do stock).
UPDATE dbo.Artigo
SET Consumivel = 1
WHERE Nome LIKE N'%Champô%' OR Nome LIKE N'%Champo%' OR Nome LIKE N'%Condicionador%'
   OR Nome LIKE N'%Sabonete%' OR Nome LIKE N'%Gel de banho%' OR Nome LIKE N'%Papel higiénico%';
GO


/* ---------------------------------------------------------------------
   2. (S6-04) Justificação dos movimentos de stock
      As entradas antigas ficam com justificação vazia.
   --------------------------------------------------------------------- */
IF COL_LENGTH(N'dbo.MovimentoStock', N'Justificacao') IS NULL
    ALTER TABLE dbo.MovimentoStock ADD Justificacao NVARCHAR(200) NOT NULL
        CONSTRAINT DF_Movimento_Justificacao DEFAULT (N'');
GO

-- Uma saída tem sempre justificação (a aplicação também o confirma).
IF OBJECT_ID(N'dbo.CK_Movimento_Justificacao', N'C') IS NULL
    ALTER TABLE dbo.MovimentoStock ADD CONSTRAINT CK_Movimento_Justificacao
        CHECK (Tipo <> 'SAIDA' OR LEN(Justificacao) > 0);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_MovimentoStock_Tipo_DataHora')
    CREATE INDEX IX_MovimentoStock_Tipo_DataHora ON dbo.MovimentoStock (Tipo, DataHora);
GO


/* ---------------------------------------------------------------------
   3. (S6-05) Ligação entre o quarto e os produtos do stock
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.QuartoProduto', N'U') IS NULL
CREATE TABLE dbo.QuartoProduto (
    IdQuarto    INT  NOT NULL,
    IdArtigo    INT  NOT NULL,
    Quantidade  INT  NOT NULL,

    CONSTRAINT PK_QuartoProduto            PRIMARY KEY (IdQuarto, IdArtigo),
    CONSTRAINT FK_QuartoProduto_Quarto     FOREIGN KEY (IdQuarto) REFERENCES dbo.Quarto (IdQuarto) ON DELETE CASCADE,
    CONSTRAINT FK_QuartoProduto_Artigo     FOREIGN KEY (IdArtigo) REFERENCES dbo.Artigo (IdArtigo),
    CONSTRAINT CK_QuartoProduto_Quantidade CHECK (Quantidade > 0)
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_QuartoProduto_Artigo')
    CREATE INDEX IX_QuartoProduto_Artigo ON dbo.QuartoProduto (IdArtigo);
GO


/* ---------------------------------------------------------------------
   Verificação: stock real, em uso, disponível e se é consumível; últimas saídas;
   e os equipamentos do stock de cada quarto (vazio até o gestor os escolher).
   --------------------------------------------------------------------- */
SELECT a.Nome, a.Unidade, a.Consumivel,
       a.QuantidadeAtual AS StockReal,
       u.EmUso,
       CASE WHEN a.QuantidadeAtual > u.EmUso THEN a.QuantidadeAtual - u.EmUso ELSE 0 END AS StockDisponivel
FROM dbo.Artigo a
CROSS APPLY (SELECT ISNULL(SUM(rp.Quantidade), 0) AS EmUso
             FROM dbo.ReservaProduto rp
             JOIN dbo.Reserva r ON r.IdReserva = rp.IdReserva
             WHERE rp.IdArtigo = a.IdArtigo AND r.Estado IN ('PENDENTE', 'CHECK_IN')) u
ORDER BY a.Nome;

SELECT TOP 20 m.DataHora, m.Tipo, a.Nome AS Produto, m.Quantidade, m.Justificacao
FROM dbo.MovimentoStock m
JOIN dbo.Artigo a ON a.IdArtigo = m.IdArtigo
WHERE m.Tipo IN ('SAIDA', 'UTILIZACAO')
ORDER BY m.DataHora DESC;

SELECT q.Numero AS Quarto, a.Nome AS Produto, qp.Quantidade
FROM dbo.QuartoProduto qp
JOIN dbo.Quarto q ON q.IdQuarto = qp.IdQuarto
JOIN dbo.Artigo a ON a.IdArtigo = qp.IdArtigo
ORDER BY q.Numero, a.Nome;
GO

PRINT N'S6-04 e S6-05: saídas de stock, consumíveis e equipamentos do quarto ligados ao stock configurados.';
GO
