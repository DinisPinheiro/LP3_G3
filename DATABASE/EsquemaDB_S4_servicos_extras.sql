/* =====================================================================
   Hotel4You - S4-05 Configuração de serviços e extras

   Corre-se POR CIMA do EsquemaDB_2_1.sql (não apaga nada): acrescenta à tabela dbo.Servico
   as colunas Descricao, Tipo (SERVICO ou EXTRA) e Ativo, e cria os serviços e extras do
   enunciado que ainda não existem. Pode correr-se várias vezes.
   Se o EsquemaDB_2_1.sql for executado outra vez (apaga tudo), este script tem de correr a seguir.

   Os preços dos itens novos são PROVISÓRIOS: o gestor altera-os no ecrã "Serviços e extras".
   ===================================================================== */
SET NOCOUNT ON;
GO

USE [2026_LP3_FEIRA_G3];
GO

IF COL_LENGTH(N'dbo.Servico', N'Descricao') IS NULL
    ALTER TABLE dbo.Servico ADD Descricao NVARCHAR(200) NOT NULL CONSTRAINT DF_Servico_Descricao DEFAULT (N'');
GO

IF COL_LENGTH(N'dbo.Servico', N'Tipo') IS NULL
    ALTER TABLE dbo.Servico ADD Tipo VARCHAR(10) NOT NULL CONSTRAINT DF_Servico_Tipo DEFAULT ('EXTRA');
GO

IF COL_LENGTH(N'dbo.Servico', N'Ativo') IS NULL
    ALTER TABLE dbo.Servico ADD Ativo BIT NOT NULL CONSTRAINT DF_Servico_Ativo DEFAULT (1);
GO

IF OBJECT_ID(N'dbo.CK_Servico_Tipo', N'C') IS NULL
    ALTER TABLE dbo.Servico ADD CONSTRAINT CK_Servico_Tipo CHECK (Tipo IN ('SERVICO', 'EXTRA'));
GO

/* O pequeno-almoço extra é um serviço de alimentação (as bebidas e a lavandaria já ficam como EXTRA). */
UPDATE dbo.Servico
SET Tipo = 'SERVICO', Categoria = N'Alimentação'
WHERE Nome = N'Pequeno-almoço extra';
GO

/* Serviços do hotel (alimentação, eventos culturais, piscina, spa, salas de conferência). */
INSERT INTO dbo.Servico (Nome, Categoria, Descricao, Tipo, Valor)
SELECT v.Nome, v.Categoria, v.Descricao, v.Tipo, v.Valor
FROM (VALUES
    (N'Jantar no restaurante',        N'Alimentação',        N'Jantar à carta no restaurante do hotel',     'SERVICO', 18.00),
    (N'Evento cultural',              N'Eventos culturais',  N'Bilhete para um evento cultural do hotel',   'SERVICO', 10.00),
    (N'Piscina privada (hora)',       N'Piscina',            N'Acesso à piscina privada por hora',          'SERVICO', 12.00),
    (N'Spa (sessão)',                 N'Spa',                N'Sessão de spa',                              'SERVICO', 35.00),
    (N'Sala de conferência (hora)',   N'Salas de conferência', N'Utilização de uma sala de conferência por hora', 'SERVICO', 40.00)
) AS v (Nome, Categoria, Descricao, Tipo, Valor)
WHERE NOT EXISTS (SELECT 1 FROM dbo.Servico s WHERE s.Nome = v.Nome);
GO

PRINT N'S4-05: serviços e extras configurados.';
GO
