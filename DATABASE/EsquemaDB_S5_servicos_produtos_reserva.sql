/* =====================================================================
   Hotel4You - S5-06 Serviços e produtos na reserva
             - S5-09 Preços guardados na reserva (vista e extras)

   Corre-se POR CIMA dos scripts anteriores (EsquemaDB_2_1.sql, EsquemaDB_2_2_checkin.sql,
   EsquemaDB_S4_servicos_extras.sql e EsquemaDB_S4_vistas.sql). Não apaga nada e pode
   correr-se várias vezes: só faz o que ainda não estiver feito.
   Se o EsquemaDB_2_1.sql for executado outra vez (apaga tudo), este script tem de correr a seguir.

   O que faz:
     1. (S5-09) Servico.Cobranca: cada serviço ou extra é cobrado por unidade ou por noite.
     2. (S5-09) Reserva.PrecoBaseNoite e Reserva.ValorVistaNoite: preços em vigor quando a
        reserva foi criada. Alterar o valor de uma vista deixa de mexer nas reservas já feitas.
     3. (S5-06 e S5-09) ConsumoServico liga a reserva aos serviços e extras, com a quantidade;
        passa a guardar também o valor e a forma de cobrança do momento da criação.
     4. (S5-06) ReservaProduto: tabela de ligação entre a reserva e os produtos do stock,
        com a quantidade. O stock disponível é o stock do hotel menos o usado em reservas.
     5. Dois extras de exemplo (limpeza por unidade, cama extra por noite).
   ===================================================================== */
SET NOCOUNT ON;
GO

USE [2026_LP3_FEIRA_G3];
GO


/* ---------------------------------------------------------------------
   1. (S5-09) Forma de cobrança dos serviços e extras
      POR_UNIDADE -> valor x quantidade            (ex.: duas limpezas)
      POR_NOITE   -> valor x quantidade x noites   (ex.: cama extra)
      Os serviços que já existem ficam "por unidade", como eram até aqui.
   --------------------------------------------------------------------- */
IF COL_LENGTH(N'dbo.Servico', N'Cobranca') IS NULL
    ALTER TABLE dbo.Servico ADD Cobranca VARCHAR(12) NOT NULL CONSTRAINT DF_Servico_Cobranca DEFAULT ('POR_UNIDADE');
GO

IF OBJECT_ID(N'dbo.CK_Servico_Cobranca', N'C') IS NULL
    ALTER TABLE dbo.Servico ADD CONSTRAINT CK_Servico_Cobranca CHECK (Cobranca IN ('POR_UNIDADE', 'POR_NOITE'));
GO


/* ---------------------------------------------------------------------
   2. (S5-09) Campos de preço na tabela Reserva
      Ficam preenchidos quando a reserva é criada e nunca mais mudam.
      Aceitam NULL só para as versões antigas da aplicação continuarem a conseguir
      criar reservas nesta base de dados (que é partilhada): nessas reservas a
      aplicação usa o preço atual do quarto.
   --------------------------------------------------------------------- */
IF COL_LENGTH(N'dbo.Reserva', N'PrecoBaseNoite') IS NULL
    ALTER TABLE dbo.Reserva ADD PrecoBaseNoite DECIMAL(10,2) NULL;
GO

IF COL_LENGTH(N'dbo.Reserva', N'ValorVistaNoite') IS NULL
    ALTER TABLE dbo.Reserva ADD ValorVistaNoite DECIMAL(10,2) NULL;
GO

IF OBJECT_ID(N'dbo.CK_Reserva_Precos', N'C') IS NULL
    ALTER TABLE dbo.Reserva ADD CONSTRAINT CK_Reserva_Precos CHECK (PrecoBaseNoite >= 0 AND ValorVistaNoite >= 0);
GO

/* Reservas que já existiam: ficam com os preços em vigor no dia em que este script corre. */
UPDATE r
SET r.PrecoBaseNoite  = ISNULL(r.PrecoBaseNoite, q.PrecoBase),
    r.ValorVistaNoite = ISNULL(r.ValorVistaNoite, ISNULL(sv.Valor, 0))
FROM dbo.Reserva r
JOIN dbo.Quarto q ON q.IdQuarto = r.IdQuarto
LEFT JOIN dbo.SuplementoVista sv ON sv.Vista = q.Vista
WHERE r.PrecoBaseNoite IS NULL OR r.ValorVistaNoite IS NULL;
GO


/* ---------------------------------------------------------------------
   3. (S5-06 e S5-09) Ligação entre a reserva e os serviços e extras
      A tabela dbo.ConsumoServico já existia (IdReserva, IdServico, Quantidade, Data).
      Passa a guardar o valor e a forma de cobrança que o serviço tinha na criação.
   --------------------------------------------------------------------- */
IF COL_LENGTH(N'dbo.ConsumoServico', N'ValorUnitario') IS NULL
    ALTER TABLE dbo.ConsumoServico ADD ValorUnitario DECIMAL(10,2) NULL;
GO

IF COL_LENGTH(N'dbo.ConsumoServico', N'Cobranca') IS NULL
    ALTER TABLE dbo.ConsumoServico ADD Cobranca VARCHAR(12) NULL;
GO

/* Linhas que já existissem: ficam com o valor e a forma de cobrança atuais do serviço. */
UPDATE cs
SET cs.ValorUnitario = ISNULL(cs.ValorUnitario, s.Valor),
    cs.Cobranca      = ISNULL(cs.Cobranca, s.Cobranca)
FROM dbo.ConsumoServico cs
JOIN dbo.Servico s ON s.IdServico = cs.IdServico
WHERE cs.ValorUnitario IS NULL OR cs.Cobranca IS NULL;
GO

/* A partir daqui as duas colunas são obrigatórias (só altera se ainda aceitarem NULL). */
IF COLUMNPROPERTY(OBJECT_ID(N'dbo.ConsumoServico'), N'ValorUnitario', 'AllowsNull') = 1
    ALTER TABLE dbo.ConsumoServico ALTER COLUMN ValorUnitario DECIMAL(10,2) NOT NULL;
GO

IF COLUMNPROPERTY(OBJECT_ID(N'dbo.ConsumoServico'), N'Cobranca', 'AllowsNull') = 1
    ALTER TABLE dbo.ConsumoServico ALTER COLUMN Cobranca VARCHAR(12) NOT NULL;
GO

IF OBJECT_ID(N'dbo.CK_Consumo_Valor', N'C') IS NULL
    ALTER TABLE dbo.ConsumoServico ADD CONSTRAINT CK_Consumo_Valor CHECK (ValorUnitario >= 0);
GO

IF OBJECT_ID(N'dbo.CK_Consumo_Cobranca', N'C') IS NULL
    ALTER TABLE dbo.ConsumoServico ADD CONSTRAINT CK_Consumo_Cobranca CHECK (Cobranca IN ('POR_UNIDADE', 'POR_NOITE'));
GO


/* ---------------------------------------------------------------------
   4. (S5-06) Ligação entre a reserva e os produtos do stock
      Os produtos não têm preço para o cliente. A quantidade usada em reservas
      não canceladas é descontada ao stock disponível:
          disponível = Artigo.QuantidadeAtual - soma de ReservaProduto.Quantidade
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.ReservaProduto', N'U') IS NULL
CREATE TABLE dbo.ReservaProduto (
    IdReservaProduto  INT IDENTITY(1,1)  NOT NULL,
    IdReserva         INT                NOT NULL,
    IdArtigo          INT                NOT NULL,
    Quantidade        INT                NOT NULL,

    CONSTRAINT PK_ReservaProduto            PRIMARY KEY (IdReservaProduto),
    CONSTRAINT FK_ReservaProduto_Reserva    FOREIGN KEY (IdReserva) REFERENCES dbo.Reserva (IdReserva),
    CONSTRAINT FK_ReservaProduto_Artigo     FOREIGN KEY (IdArtigo)  REFERENCES dbo.Artigo (IdArtigo),
    CONSTRAINT CK_ReservaProduto_Quantidade CHECK (Quantidade > 0)
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_ReservaProduto_Reserva')
    CREATE INDEX IX_ReservaProduto_Reserva ON dbo.ReservaProduto (IdReserva);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'IX_ReservaProduto_Artigo')
    CREATE INDEX IX_ReservaProduto_Artigo ON dbo.ReservaProduto (IdArtigo);
GO


/* ---------------------------------------------------------------------
   5. Extras de exemplo, um por unidade e outro por noite.
      Os preços são PROVISÓRIOS: o gestor altera-os no ecrã "Serviços e extras".
   --------------------------------------------------------------------- */
INSERT INTO dbo.Servico (Nome, Categoria, Descricao, Tipo, Valor, Cobranca)
SELECT v.Nome, v.Categoria, v.Descricao, v.Tipo, v.Valor, v.Cobranca
FROM (VALUES
    (N'Limpeza extra do quarto', N'Limpeza', N'Limpeza do quarto a pedido do cliente', 'EXTRA', 10.00, 'POR_UNIDADE'),
    (N'Cama extra',              N'Quarto',  N'Cama extra no quarto, por noite',       'EXTRA', 15.00, 'POR_NOITE')
) AS v (Nome, Categoria, Descricao, Tipo, Valor, Cobranca)
WHERE NOT EXISTS (SELECT 1 FROM dbo.Servico s WHERE s.Nome = v.Nome);
GO


/* ---------------------------------------------------------------------
   Verificação: no fim devem aparecer três tabelas, sem mensagens de erro a vermelho.
   --------------------------------------------------------------------- */
SELECT IdServico, Nome, Tipo, Valor, Cobranca, Ativo
FROM dbo.Servico
ORDER BY Tipo, Categoria, Nome;

SELECT IdReserva, Estado, ValorTotal, PrecoBaseNoite, ValorVistaNoite
FROM dbo.Reserva
ORDER BY IdReserva;

SELECT a.Nome, a.QuantidadeAtual AS StockDoHotel,
       a.QuantidadeAtual - ISNULL((SELECT SUM(rp.Quantidade)
                                   FROM dbo.ReservaProduto rp
                                   JOIN dbo.Reserva r ON r.IdReserva = rp.IdReserva
                                   WHERE rp.IdArtigo = a.IdArtigo AND r.Estado <> 'CANCELADA'), 0) AS StockDisponivel
FROM dbo.Artigo a
ORDER BY a.Nome;
GO

PRINT N'S5-06 e S5-09: serviços e produtos na reserva e preços guardados configurados.';
GO
