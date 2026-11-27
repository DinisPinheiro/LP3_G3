/* =====================================================================
   Hotel4You - S7-02 Unidades de produto e conversão

   Corre-se POR CIMA dos scripts anteriores (sobretudo EsquemaDB_S5_stock_completo.sql,
   EsquemaDB_S6_stock_saidas_equipamentos.sql e EsquemaDB_S7_baixa_stock_checkin.sql).
   Não apaga nada e pode correr-se várias vezes: só faz o que ainda não estiver feito.

   Regra:
     - Cada produto tem uma UNIDADE PRINCIPAL (dbo.Artigo.Unidade): "un" para o que vem em caixas ou
       unidades, ou a unidade da linha (ex.: "kg").
     - dbo.Artigo.UnidadesPorCaixa guarda a conversão de caixa para unidades (ex.: 1 cx = 10 un).
     - O stock, os quartos, as reservas, os serviços e os movimentos contam na unidade principal.
     - Na importação, uma linha em caixas usa a conversão do ficheiro (caixas e unidades na mesma
       linha); quando não vem, o gestor ou o funcionário define-a ao aprovar ou na janela
       "Unidades dos produtos".

   O que faz este script:
     1. Nova coluna dbo.Artigo.UnidadesPorCaixa (NULL = conversão ainda não definida).
     2. Preenche a conversão a partir das linhas de encomenda já guardadas (dbo.EncomendaLinha tem
        QuantidadeCaixas e QuantidadeUnidades), quando a divisão é exata.
     3. Converte para unidades os produtos que ainda estavam em caixas ('cx') e já têm conversão:
        o stock, os produtos dos quartos, das configurações de tipo de quarto, das reservas e dos
        serviços, e os movimentos de stock. Os que ficam em 'cx' (sem conversão) aparecem na
        janela "Unidades dos produtos" para o gestor ou o funcionário definir a conversão.
     4. Verificação.
   ===================================================================== */
SET NOCOUNT ON;
GO

USE [2026_LP3_FEIRA_G3];
GO


/* ---------------------------------------------------------------------
   1. Conversão de caixa para unidades
   --------------------------------------------------------------------- */
IF COL_LENGTH(N'dbo.Artigo', N'UnidadesPorCaixa') IS NULL
    ALTER TABLE dbo.Artigo ADD UnidadesPorCaixa INT NULL;
GO

IF OBJECT_ID(N'dbo.CK_Artigo_UnidadesPorCaixa', N'C') IS NULL
    ALTER TABLE dbo.Artigo ADD CONSTRAINT CK_Artigo_UnidadesPorCaixa
        CHECK (UnidadesPorCaixa IS NULL OR (UnidadesPorCaixa >= 1 AND UnidadesPorCaixa <= 10000));
GO


/* ---------------------------------------------------------------------
   2. Conversão a partir das encomendas já importadas (a linha mais recente com divisão exata)
   --------------------------------------------------------------------- */
UPDATE a
SET a.UnidadesPorCaixa = c.UnidadesPorCaixa
FROM dbo.Artigo a
CROSS APPLY (SELECT TOP 1 el.QuantidadeUnidades / el.QuantidadeCaixas AS UnidadesPorCaixa
             FROM dbo.EncomendaLinha el
             WHERE el.IdArtigo = a.IdArtigo
               AND el.QuantidadeCaixas > 0 AND el.QuantidadeUnidades > 0
               AND el.QuantidadeUnidades % el.QuantidadeCaixas = 0
               AND el.QuantidadeUnidades / el.QuantidadeCaixas BETWEEN 1 AND 10000
             ORDER BY el.IdEncomenda DESC) c
WHERE a.UnidadesPorCaixa IS NULL;
GO


/* ---------------------------------------------------------------------
   3. Produtos ainda em caixas e com conversão: passam para unidades (tudo ou nada)
   --------------------------------------------------------------------- */
BEGIN TRANSACTION;

DECLARE @Converter TABLE (IdArtigo INT PRIMARY KEY, Fator INT NOT NULL);
INSERT INTO @Converter (IdArtigo, Fator)
SELECT IdArtigo, UnidadesPorCaixa FROM dbo.Artigo
WHERE Unidade = 'cx' AND UnidadesPorCaixa IS NOT NULL;

UPDATE t SET t.Quantidade = t.Quantidade * c.Fator
FROM dbo.QuartoProduto t JOIN @Converter c ON c.IdArtigo = t.IdArtigo;

UPDATE t SET t.Quantidade = t.Quantidade * c.Fator
FROM dbo.ConfigTipoQuarto_Produto t JOIN @Converter c ON c.IdArtigo = t.IdArtigo;

UPDATE t SET t.Quantidade = t.Quantidade * c.Fator
FROM dbo.ReservaProduto t JOIN @Converter c ON c.IdArtigo = t.IdArtigo;

UPDATE t SET t.Quantidade = t.Quantidade * c.Fator
FROM dbo.ServicoProduto t JOIN @Converter c ON c.IdArtigo = t.IdArtigo;

UPDATE t SET t.Quantidade = t.Quantidade * c.Fator
FROM dbo.MovimentoStock t JOIN @Converter c ON c.IdArtigo = t.IdArtigo;

UPDATE a SET a.QuantidadeAtual = a.QuantidadeAtual * c.Fator, a.Unidade = 'un'
FROM dbo.Artigo a JOIN @Converter c ON c.IdArtigo = a.IdArtigo;

COMMIT TRANSACTION;
GO


/* ---------------------------------------------------------------------
   4. Verificação: unidade principal, conversão e stock de cada produto.
      Os que aparecem com Unidade = 'cx' ainda precisam da conversão (janela "Unidades dos produtos").
   --------------------------------------------------------------------- */
SELECT a.Nome, a.Unidade AS UnidadePrincipal, a.UnidadesPorCaixa,
       a.QuantidadeAtual AS StockReal,
       CASE WHEN a.UnidadesPorCaixa > 0 THEN a.QuantidadeAtual / a.UnidadesPorCaixa END AS CaixasCompletas
FROM dbo.Artigo a
ORDER BY a.Nome;
GO

PRINT N'S7-02: unidades de produto e conversão de caixas configuradas.';
GO
