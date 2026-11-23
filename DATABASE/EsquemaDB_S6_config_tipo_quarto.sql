/* =====================================================================
   Hotel4You - S6-09 Configuração de quartos por tipo

   O que faz:
     1. dbo.ConfigTipoQuarto: o preço base de cada tipo de quarto (INDIVIDUAL, DUPLO, FAMILIA).
     2. dbo.ConfigTipoQuarto_Equipamento: os equipamentos do catálogo de cada tipo.
     3. dbo.ConfigTipoQuarto_Produto: os produtos do stock de cada tipo, com a quantidade.
     4. dbo.Quarto: duas colunas que dizem se o preço e os equipamentos do quarto são uma exceção
        à configuração do tipo (1) ou se o quarto os herda do tipo (0).

   Herança: os quartos herdam o preço base e os equipamentos do seu tipo. O gestor muda a configuração
   do tipo e os quartos que a herdam mudam com ela (feito pela aplicação). Os quartos que já existem
   ficam com os valores que têm, como exceções (1), para nada mudar até o gestor decidir.
   ===================================================================== */
SET NOCOUNT ON;
GO

USE [2026_LP3_FEIRA_G3];
GO


/* ---------------------------------------------------------------------
   0. Confirma que as tabelas de que o script depende já existem
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.Quarto', N'U') IS NULL OR OBJECT_ID(N'dbo.Equipamento', N'U') IS NULL
   OR OBJECT_ID(N'dbo.Artigo', N'U') IS NULL OR OBJECT_ID(N'dbo.QuartoProduto', N'U') IS NULL
    RAISERROR(N'Faltam tabelas. Execute primeiro EsquemaDB_2_1.sql e EsquemaDB_S6_stock_saidas_equipamentos.sql.', 16, 1);
GO


/* ---------------------------------------------------------------------
   1. Preço base por tipo de quarto
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.ConfigTipoQuarto', N'U') IS NULL
CREATE TABLE dbo.ConfigTipoQuarto (
    Tipo       VARCHAR(20)    NOT NULL,
    PrecoBase  DECIMAL(10,2)  NOT NULL,

    CONSTRAINT PK_ConfigTipoQuarto       PRIMARY KEY (Tipo),
    CONSTRAINT CK_ConfigTipoQuarto_Tipo  CHECK (Tipo IN ('INDIVIDUAL', 'DUPLO', 'FAMILIA')),
    CONSTRAINT CK_ConfigTipoQuarto_Preco CHECK (PrecoBase >= 0)
);
GO


/* ---------------------------------------------------------------------
   2. Equipamentos do catálogo de cada tipo
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.ConfigTipoQuarto_Equipamento', N'U') IS NULL
CREATE TABLE dbo.ConfigTipoQuarto_Equipamento (
    Tipo           VARCHAR(20) NOT NULL,
    IdEquipamento  INT         NOT NULL,

    CONSTRAINT PK_ConfigTipoQuarto_Equipamento PRIMARY KEY (Tipo, IdEquipamento),
    CONSTRAINT FK_ConfigTipoEquip_Tipo
        FOREIGN KEY (Tipo) REFERENCES dbo.ConfigTipoQuarto (Tipo) ON DELETE CASCADE,
    CONSTRAINT FK_ConfigTipoEquip_Equipamento
        FOREIGN KEY (IdEquipamento) REFERENCES dbo.Equipamento (IdEquipamento)
);
GO


/* ---------------------------------------------------------------------
   3. Produtos do stock de cada tipo, com a quantidade
   --------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.ConfigTipoQuarto_Produto', N'U') IS NULL
CREATE TABLE dbo.ConfigTipoQuarto_Produto (
    Tipo        VARCHAR(20) NOT NULL,
    IdArtigo    INT         NOT NULL,
    Quantidade  INT         NOT NULL,

    CONSTRAINT PK_ConfigTipoQuarto_Produto PRIMARY KEY (Tipo, IdArtigo),
    CONSTRAINT FK_ConfigTipoProd_Tipo
        FOREIGN KEY (Tipo) REFERENCES dbo.ConfigTipoQuarto (Tipo) ON DELETE CASCADE,
    CONSTRAINT FK_ConfigTipoProd_Artigo
        FOREIGN KEY (IdArtigo) REFERENCES dbo.Artigo (IdArtigo),
    CONSTRAINT CK_ConfigTipoProd_Quantidade CHECK (Quantidade > 0)
);
GO


/* ---------------------------------------------------------------------
   4. Exceções no quarto (1 = valores próprios do quarto, 0 = herda do tipo)
      O DEFAULT 1 faz com que os quartos que já existem fiquem como estão.
   --------------------------------------------------------------------- */
IF COL_LENGTH(N'dbo.Quarto', N'PrecoPersonalizado') IS NULL
    ALTER TABLE dbo.Quarto ADD PrecoPersonalizado BIT NOT NULL
        CONSTRAINT DF_Quarto_PrecoPersonalizado DEFAULT (1);
GO

IF COL_LENGTH(N'dbo.Quarto', N'EquipamentosPersonalizados') IS NULL
    ALTER TABLE dbo.Quarto ADD EquipamentosPersonalizados BIT NOT NULL
        CONSTRAINT DF_Quarto_EquipPersonalizados DEFAULT (1);
GO


/* ---------------------------------------------------------------------
   Verificação: configuração de cada tipo e, para cada quarto, o que herda e o que é exceção.
   --------------------------------------------------------------------- */
SELECT c.Tipo, c.PrecoBase,
       (SELECT COUNT(*) FROM dbo.ConfigTipoQuarto_Equipamento ce WHERE ce.Tipo = c.Tipo) AS Equipamentos,
       (SELECT COUNT(*) FROM dbo.ConfigTipoQuarto_Produto cp WHERE cp.Tipo = c.Tipo)     AS ProdutosStock
FROM dbo.ConfigTipoQuarto c
ORDER BY c.Tipo;

SELECT q.Numero, q.Tipo, q.PrecoBase,
       CASE WHEN q.PrecoPersonalizado = 1 THEN N'exceção' ELSE N'herda do tipo' END        AS Preco,
       CASE WHEN q.EquipamentosPersonalizados = 1 THEN N'exceção' ELSE N'herda do tipo' END AS Equipamentos
FROM dbo.Quarto q
ORDER BY q.Numero;
GO

PRINT N'S6-09: configuração de quartos por tipo criada (os quartos existentes ficam como exceções).';
GO
