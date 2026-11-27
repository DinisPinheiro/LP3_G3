/* =====================================================================
   Hotel4You - S7-01 Baixa de stock no check-in

   Corre-se POR CIMA dos scripts anteriores (sobretudo EsquemaDB_S5_servicos_produtos_reserva.sql
   e EsquemaDB_S6_stock_saidas_equipamentos.sql). Não apaga nada e pode correr-se várias vezes:
   só faz o que ainda não estiver feito.

   O que muda no stock:
     - O stock só baixa no CHECK-IN, nunca na criação da reserva. Uma reserva pendente guarda os
       extras escolhidos (dbo.ReservaProduto), mas eles ainda não estão "em uso".
     - No check-in dão baixa: os produtos escolhidos na reserva, os equipamentos configurados do
       quarto (dbo.QuartoProduto) e os produtos dos serviços ligados ao stock (dbo.ServicoProduto),
       todos gravados em dbo.ReservaProduto.
     - Em uso = soma de dbo.ReservaProduto das reservas com Estado = 'CHECK_IN'.
       Disponível = Artigo.QuantidadeAtual - em uso.
     - No check-out os produtos voltam ao disponível; os consumíveis (Artigo.Consumivel = 1)
       saem do stock real (movimento UTILIZACAO em dbo.MovimentoStock).

   O que faz este script:
     1. dbo.ServicoProduto (nova): tabela de ligação entre um serviço/extra e o produto do stock
        que ele gasta, com a quantidade por unidade (ex.: "Água 50cl" -> 1 garrafa de água).
     2. Índices nas tabelas de ligação usadas na baixa (ReservaProduto por reserva e artigo).
     3. Verificação: stock real / em uso / disponível com a regra nova, e os serviços ligados ao stock.
   ===================================================================== */
SET NOCOUNT ON;
GO

USE [2026_LP3_FEIRA_G3];
GO


/* ---------------------------------------------------------------------
   1. Ligação entre o serviço e o produto do stock
      Um serviço gasta no máximo um produto (chave = IdServico).
      Se o serviço for apagado, a ligação também é (ON DELETE CASCADE).
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.ServicoProduto', N'U') IS NULL
CREATE TABLE dbo.ServicoProduto (
    IdServico   INT  NOT NULL,
    IdArtigo    INT  NOT NULL,
    Quantidade  INT  NOT NULL,

    CONSTRAINT PK_ServicoProduto            PRIMARY KEY (IdServico),
    CONSTRAINT FK_ServicoProduto_Servico    FOREIGN KEY (IdServico) REFERENCES dbo.Servico (IdServico) ON DELETE CASCADE,
    CONSTRAINT FK_ServicoProduto_Artigo     FOREIGN KEY (IdArtigo)  REFERENCES dbo.Artigo (IdArtigo),
    CONSTRAINT CK_ServicoProduto_Quantidade CHECK (Quantidade > 0)
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_ServicoProduto_Artigo')
    CREATE INDEX IX_ServicoProduto_Artigo ON dbo.ServicoProduto (IdArtigo);
GO


/* ---------------------------------------------------------------------
   2. Índices nas tabelas de ligação (se ainda não existirem)
   --------------------------------------------------------------------- */
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_ReservaProduto_Reserva')
    CREATE INDEX IX_ReservaProduto_Reserva ON dbo.ReservaProduto (IdReserva);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_ReservaProduto_Artigo')
    CREATE INDEX IX_ReservaProduto_Artigo ON dbo.ReservaProduto (IdArtigo);
GO


/* ---------------------------------------------------------------------
   3. Verificação
      - stock real, em uso (só reservas com check-in) e disponível;
      - serviços e extras ligados ao stock (vazio até o gestor os ligar no ecrã
        "Serviços e extras").
   --------------------------------------------------------------------- */
SELECT a.Nome, a.Unidade, a.Consumivel,
       a.QuantidadeAtual AS StockReal,
       u.EmUso,
       CASE WHEN a.QuantidadeAtual > u.EmUso THEN a.QuantidadeAtual - u.EmUso ELSE 0 END AS StockDisponivel
FROM dbo.Artigo a
CROSS APPLY (SELECT ISNULL(SUM(rp.Quantidade), 0) AS EmUso
             FROM dbo.ReservaProduto rp
             JOIN dbo.Reserva r ON r.IdReserva = rp.IdReserva
             WHERE rp.IdArtigo = a.IdArtigo AND r.Estado = 'CHECK_IN') u
ORDER BY a.Nome;

SELECT s.Nome AS Servico, s.Tipo, a.Nome AS Produto, sp.Quantidade AS QuantidadePorUnidade
FROM dbo.ServicoProduto sp
JOIN dbo.Servico s ON s.IdServico = sp.IdServico
JOIN dbo.Artigo a  ON a.IdArtigo  = sp.IdArtigo
ORDER BY s.Nome;
GO

PRINT N'S7-01: baixa de stock no check-in configurada (tabela dbo.ServicoProduto).';
GO
