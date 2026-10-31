CREATE TABLE dbo.Utilizador (
    IdUtilizador  INT IDENTITY(1,1)  NOT NULL,
    Username      NVARCHAR(50)       NOT NULL,
    PasswordHash  NVARCHAR(255)      NOT NULL,
    Nome          NVARCHAR(100)      NOT NULL,
    Perfil        VARCHAR(30)        NOT NULL,
    Ativo         BIT                NOT NULL CONSTRAINT DF_Utilizador_Ativo DEFAULT (1),

    CONSTRAINT PK_Utilizador          PRIMARY KEY (IdUtilizador),
    CONSTRAINT UQ_Utilizador_Username UNIQUE (Username),
    CONSTRAINT CK_Utilizador_Perfil   CHECK (Perfil IN
        ('RECECIONISTA', 'GESTOR', 'EMPRESA_RESTAURACAO', 'FORNECEDOR')),
    CONSTRAINT CK_Utilizador_Username CHECK (LEN(LTRIM(RTRIM(Username))) > 0)
);
GO

/* ---- Equipamento -------------------------------- */
CREATE TABLE dbo.Equipamento (
    IdEquipamento INT IDENTITY(1,1)  NOT NULL,
    Nome          NVARCHAR(60)       NOT NULL,

    CONSTRAINT PK_Equipamento      PRIMARY KEY (IdEquipamento),
    CONSTRAINT UQ_Equipamento_Nome UNIQUE (Nome)
);
GO


CREATE TABLE dbo.Quarto (
    IdQuarto      INT IDENTITY(1,1)  NOT NULL,
    Numero        INT                NOT NULL,
    Piso          TINYINT            NOT NULL,
    Local         NVARCHAR(100)      NOT NULL CONSTRAINT DF_Quarto_Local DEFAULT (N''),
    Tipo          VARCHAR(20)        NOT NULL,
    PrecoBase     DECIMAL(10,2)      NOT NULL,
    Vista         VARCHAR(20)        NOT NULL CONSTRAINT DF_Quarto_Vista DEFAULT ('NENHUMA'),
    Ativo         BIT                NOT NULL CONSTRAINT DF_Quarto_Ativo DEFAULT (1),

    CONSTRAINT PK_Quarto        PRIMARY KEY (IdQuarto),
    CONSTRAINT UQ_Quarto_Numero UNIQUE (Numero),
    CONSTRAINT CK_Quarto_Piso   CHECK (Piso IN (2, 3)),
    CONSTRAINT CK_Quarto_Numero CHECK (Numero BETWEEN Piso * 100 + 1 AND Piso * 100 + 10),
    CONSTRAINT CK_Quarto_Tipo   CHECK (Tipo IN ('INDIVIDUAL', 'DUPLO', 'FAMILIA')),
    CONSTRAINT CK_Quarto_Preco  CHECK (PrecoBase >= 0),
    CONSTRAINT CK_Quarto_Vista  CHECK (Vista IN ('NENHUMA', 'MAR', 'MONTANHA'))
);
GO

CREATE TABLE dbo.Quarto_Equipamento (
    IdQuarto      INT NOT NULL,
    IdEquipamento INT NOT NULL,

    CONSTRAINT PK_Quarto_Equipamento PRIMARY KEY (IdQuarto, IdEquipamento),
    CONSTRAINT FK_QuartoEquip_Quarto
        FOREIGN KEY (IdQuarto) REFERENCES dbo.Quarto (IdQuarto) ON DELETE CASCADE,
    CONSTRAINT FK_QuartoEquip_Equipamento
        FOREIGN KEY (IdEquipamento) REFERENCES dbo.Equipamento (IdEquipamento)
);
GO


CREATE TABLE dbo.Cliente (
    IdCliente     INT IDENTITY(1,1)  NOT NULL,
    Nome          NVARCHAR(100)      NOT NULL,
    Nif           NVARCHAR(20)       NOT NULL,
    Contacto      NVARCHAR(100)      NOT NULL CONSTRAINT DF_Cliente_Contacto DEFAULT (N''),

    CONSTRAINT PK_Cliente     PRIMARY KEY (IdCliente),
    CONSTRAINT UQ_Cliente_Nif UNIQUE (Nif)
);
GO

CREATE TABLE dbo.Reserva (
    IdReserva     INT IDENTITY(1,1)  NOT NULL,
    IdCliente     INT                NOT NULL,
    IdQuarto      INT                NOT NULL,
    DataEntrada   DATE               NOT NULL,
    DataSaida     DATE               NOT NULL,
    Estado        VARCHAR(15)        NOT NULL CONSTRAINT DF_Reserva_Estado DEFAULT ('PENDENTE'),
    ValorTotal    DECIMAL(10,2)      NOT NULL CONSTRAINT DF_Reserva_Valor DEFAULT (0),
    DataCheckIn   DATETIME2(0)       NULL,
    DataCheckOut  DATETIME2(0)       NULL,
    DataCriacao   DATETIME2(0)       NOT NULL CONSTRAINT DF_Reserva_Criacao DEFAULT (SYSDATETIME()),

    CONSTRAINT PK_Reserva          PRIMARY KEY (IdReserva),
    CONSTRAINT FK_Reserva_Cliente  FOREIGN KEY (IdCliente) REFERENCES dbo.Cliente (IdCliente),
    CONSTRAINT FK_Reserva_Quarto   FOREIGN KEY (IdQuarto)  REFERENCES dbo.Quarto (IdQuarto),
    CONSTRAINT CK_Reserva_Estado   CHECK (Estado IN ('PENDENTE', 'CHECK_IN', 'CHECK_OUT', 'CANCELADA')),
    CONSTRAINT CK_Reserva_Datas    CHECK (DataSaida > DataEntrada),
    CONSTRAINT CK_Reserva_Valor    CHECK (ValorTotal >= 0),
    CONSTRAINT CK_Reserva_CheckOut CHECK (DataCheckOut IS NULL OR DataCheckIn IS NOT NULL)
);
GO

CREATE INDEX IX_Reserva_Quarto_Datas ON dbo.Reserva (IdQuarto, DataEntrada, DataSaida) INCLUDE (Estado);
CREATE INDEX IX_Reserva_Cliente      ON dbo.Reserva (IdCliente);
GO

CREATE TABLE dbo.CartaoMagnetico (
    IdCartao      INT IDENTITY(1,1)  NOT NULL,
    Numero        INT                NOT NULL,
    Mestre        BIT                NOT NULL CONSTRAINT DF_Cartao_Mestre DEFAULT (0),

    CONSTRAINT PK_CartaoMagnetico        PRIMARY KEY (IdCartao),
    CONSTRAINT UQ_CartaoMagnetico_Numero UNIQUE (Numero),
    CONSTRAINT CK_CartaoMagnetico_Numero CHECK (Numero > 0)
);
GO

/* Cartão entregue no check-in (0..1 por reserva). */
ALTER TABLE dbo.Reserva ADD IdCartao INT NULL;
GO
ALTER TABLE dbo.Reserva ADD CONSTRAINT FK_Reserva_Cartao
    FOREIGN KEY (IdCartao) REFERENCES dbo.CartaoMagnetico (IdCartao);
GO

/* Passagens de cartão: registo de cada acesso a um piso (RegistoAcesso). */
CREATE TABLE dbo.RegistoAcesso (
    IdRegisto     INT IDENTITY(1,1)  NOT NULL,
    IdCartao      INT                NOT NULL,
    DataHora      DATETIME2(0)       NOT NULL CONSTRAINT DF_RegistoAcesso_DataHora DEFAULT (SYSDATETIME()),
    Piso          SMALLINT           NOT NULL,
    Local         NVARCHAR(100)      NOT NULL,

    CONSTRAINT PK_RegistoAcesso        PRIMARY KEY (IdRegisto),
    CONSTRAINT FK_RegistoAcesso_Cartao FOREIGN KEY (IdCartao) REFERENCES dbo.CartaoMagnetico (IdCartao),
    CONSTRAINT CK_RegistoAcesso_Piso   CHECK (Piso BETWEEN -1 AND 3)
);
GO

CREATE INDEX IX_RegistoAcesso_Cartao_DataHora ON dbo.RegistoAcesso (IdCartao, DataHora);
GO

/* Catálogo de serviços/extras (bebidas, lavandaria, ...) e consumos por reserva. */
CREATE TABLE dbo.Servico (
    IdServico     INT IDENTITY(1,1)  NOT NULL,
    Nome          NVARCHAR(80)       NOT NULL,
    Categoria     NVARCHAR(40)       NOT NULL CONSTRAINT DF_Servico_Categoria DEFAULT (N''),
    Valor         DECIMAL(10,2)      NOT NULL,

    CONSTRAINT PK_Servico      PRIMARY KEY (IdServico),
    CONSTRAINT UQ_Servico_Nome UNIQUE (Nome),
    CONSTRAINT CK_Servico_Valor CHECK (Valor >= 0)
);
GO

CREATE TABLE dbo.ConsumoServico (
    IdConsumo     INT IDENTITY(1,1)  NOT NULL,
    IdReserva     INT                NOT NULL,
    IdServico     INT                NOT NULL,
    Quantidade    INT                NOT NULL,
    Data          DATE               NOT NULL CONSTRAINT DF_Consumo_Data DEFAULT (CAST(SYSDATETIME() AS DATE)),

    CONSTRAINT PK_ConsumoServico PRIMARY KEY (IdConsumo),
    CONSTRAINT FK_Consumo_Reserva FOREIGN KEY (IdReserva) REFERENCES dbo.Reserva (IdReserva),
    CONSTRAINT FK_Consumo_Servico FOREIGN KEY (IdServico) REFERENCES dbo.Servico (IdServico),
    CONSTRAINT CK_Consumo_Quantidade CHECK (Quantidade > 0)
);
GO

CREATE INDEX IX_ConsumoServico_Reserva ON dbo.ConsumoServico (IdReserva);
GO

CREATE TABLE dbo.Fornecedor (
    IdFornecedor  INT IDENTITY(1,1)  NOT NULL,
    Nome          NVARCHAR(100)      NOT NULL,
    Nif           NVARCHAR(20)       NOT NULL,
    Contacto      NVARCHAR(100)      NOT NULL CONSTRAINT DF_Fornecedor_Contacto DEFAULT (N''),
    IdUtilizador  INT                NULL,

    CONSTRAINT PK_Fornecedor      PRIMARY KEY (IdFornecedor),
    CONSTRAINT UQ_Fornecedor_Nif  UNIQUE (Nif),
    CONSTRAINT FK_Fornecedor_Utilizador FOREIGN KEY (IdUtilizador) REFERENCES dbo.Utilizador (IdUtilizador)
);
GO

/* Um utilizador só pode representar um fornecedor (UNIQUE ignorando NULL). */
CREATE UNIQUE INDEX UX_Fornecedor_Utilizador ON dbo.Fornecedor (IdUtilizador) WHERE IdUtilizador IS NOT NULL;
GO

/* Artigo: QuantidadeAtual é a soma dos movimentos (Artigo.aplicar no Java). */
CREATE TABLE dbo.Artigo (
    IdArtigo        INT IDENTITY(1,1)  NOT NULL,
    Nome            NVARCHAR(100)      NOT NULL,
    Unidade         NVARCHAR(20)       NOT NULL CONSTRAINT DF_Artigo_Unidade DEFAULT (N'un'),
    QuantidadeAtual INT                NOT NULL CONSTRAINT DF_Artigo_Qtd DEFAULT (0),
    IdFornecedor    INT                NULL,

    CONSTRAINT PK_Artigo      PRIMARY KEY (IdArtigo),
    CONSTRAINT UQ_Artigo_Nome UNIQUE (Nome),
    CONSTRAINT CK_Artigo_Qtd  CHECK (QuantidadeAtual >= 0),
    CONSTRAINT FK_Artigo_Fornecedor FOREIGN KEY (IdFornecedor) REFERENCES dbo.Fornecedor (IdFornecedor)
);
GO

CREATE TABLE dbo.MovimentoStock (
    IdMovimento   INT IDENTITY(1,1)  NOT NULL,
    IdArtigo      INT                NOT NULL,
    IdUtilizador  INT                NOT NULL,
    Tipo          VARCHAR(12)        NOT NULL,
    Quantidade    INT                NOT NULL,
    DataHora      DATETIME2(0)       NOT NULL CONSTRAINT DF_Movimento_DataHora DEFAULT (SYSDATETIME()),

    CONSTRAINT PK_MovimentoStock PRIMARY KEY (IdMovimento),
    CONSTRAINT FK_Movimento_Artigo     FOREIGN KEY (IdArtigo)     REFERENCES dbo.Artigo (IdArtigo),
    CONSTRAINT FK_Movimento_Utilizador FOREIGN KEY (IdUtilizador) REFERENCES dbo.Utilizador (IdUtilizador),
    CONSTRAINT CK_Movimento_Tipo       CHECK (Tipo IN ('ENTRADA', 'UTILIZACAO', 'SAIDA')),
    CONSTRAINT CK_Movimento_Quantidade CHECK (Quantidade > 0)
);
GO

CREATE INDEX IX_MovimentoStock_Artigo_DataHora ON dbo.MovimentoStock (IdArtigo, DataHora);
GO

CREATE TABLE dbo.SugestaoReclamacao (
    IdSugestao    INT IDENTITY(1,1)  NOT NULL,
    Tipo          VARCHAR(12)        NOT NULL,
    Descricao     NVARCHAR(1000)     NOT NULL,
    DataHora      DATETIME2(0)       NOT NULL CONSTRAINT DF_Sugestao_DataHora DEFAULT (SYSDATETIME()),
    Estado        VARCHAR(12)        NOT NULL CONSTRAINT DF_Sugestao_Estado DEFAULT ('ABERTA'),
    IdCliente     INT                NULL,
    IdReserva     INT                NULL,
    IdUtilizador  INT                NOT NULL,

    CONSTRAINT PK_SugestaoReclamacao PRIMARY KEY (IdSugestao),
    CONSTRAINT CK_Sugestao_Tipo   CHECK (Tipo IN ('SUGESTAO', 'RECLAMACAO')),
    CONSTRAINT CK_Sugestao_Estado CHECK (Estado IN ('ABERTA', 'EM_ANALISE', 'RESOLVIDA')),
    CONSTRAINT FK_Sugestao_Cliente    FOREIGN KEY (IdCliente)    REFERENCES dbo.Cliente (IdCliente),
    CONSTRAINT FK_Sugestao_Reserva    FOREIGN KEY (IdReserva)    REFERENCES dbo.Reserva (IdReserva),
    CONSTRAINT FK_Sugestao_Utilizador FOREIGN KEY (IdUtilizador) REFERENCES dbo.Utilizador (IdUtilizador)
);
GO

/* =============================================================================
   4. DADOS INICIAIS E DE TESTE
   ============================================================================= */

/* ---- Utilizadores --------------------------------------------------------------
   Gestor inicial (obrigatório) e um utilizador de teste por perfil.
   Palavras-chave de TESTE (só existem aqui em hash):
       gestor / gestor123           rececao / rececao123
       cozinha / cozinha123         fornecedor / fornecedor123
   Alterar a palavra-chave do gestor antes de qualquer utilização real. */
INSERT INTO dbo.Utilizador (Username, PasswordHash, Nome, Perfil) VALUES
 (N'gestor',     N'120000:fxKC+qhspx6jjnjxU3fPwg==:tJa6l+iN1vWwYBZkWFrcKKpks/ce6llk1yA4k+xhwnY=', N'Gestor Inicial',       'GESTOR'),
 (N'rececao',    N'120000:FGZHPj9jAc5Jqp93BknaYw==:RltOhfxcvLgG+FpLtzDD9qF7S8qPixDLRwnIyT2/dnY=', N'Ana Rececionista',    'RECECIONISTA'),
 (N'cozinha',    N'120000:36C8E8d/ETFF5JiCdl7T9g==:bAUJIG/IqV4ZpHKc5RHKHfGtBaAUNuO6uS0BSe5tbUE=', N'Empresa Restauração',  'EMPRESA_RESTAURACAO'),
 (N'fornecedor', N'120000:4+pGSWPo9xBVx9um3g+Tfg==:zBGy98wjIv69vpfA3EOjNBM9l+FnuD8GVUlDTnd2KBo=', N'Fornecedor Lda',       'FORNECEDOR');
GO

/* ---- Equipamentos ----------------------------------------------------------------- */
INSERT INTO dbo.Equipamento (Nome) VALUES
 (N'Toalhas'), (N'Champô'), (N'Secador'), (N'TV'),
 (N'Ar condicionado'), (N'Cofre'), (N'Minibar'), (N'Varanda');
GO

/* ---- Quartos: 20 no total (7 individuais, 10 duplos, 3 de família) -------------------
   Preço base por noite: individual 50, duplo 80, família 120.
   Suplemento da vista (aplicado na aplicação): mar +20, montanha +15. */
INSERT INTO dbo.Quarto (Numero, Piso, Local, Tipo, PrecoBase, Vista) VALUES
 -- Piso 2
 (201, 2, N'Ala nascente', 'INDIVIDUAL',  50.00, 'NENHUMA'),
 (202, 2, N'Ala nascente', 'INDIVIDUAL',  50.00, 'NENHUMA'),
 (203, 2, N'Ala nascente', 'INDIVIDUAL',  50.00, 'MONTANHA'),
 (204, 2, N'Ala nascente', 'INDIVIDUAL',  50.00, 'MONTANHA'),
 (205, 2, N'Ala poente',   'DUPLO',       80.00, 'NENHUMA'),
 (206, 2, N'Ala poente',   'DUPLO',       80.00, 'NENHUMA'),
 (207, 2, N'Ala poente',   'DUPLO',       80.00, 'MAR'),
 (208, 2, N'Ala poente',   'DUPLO',       80.00, 'MAR'),
 (209, 2, N'Ala poente',   'DUPLO',       80.00, 'MAR'),
 (210, 2, N'Ala poente',   'FAMILIA',    120.00, 'MAR'),
 -- Piso 3
 (301, 3, N'Ala nascente', 'INDIVIDUAL',  50.00, 'NENHUMA'),
 (302, 3, N'Ala nascente', 'INDIVIDUAL',  50.00, 'NENHUMA'),
 (303, 3, N'Ala nascente', 'INDIVIDUAL',  50.00, 'MONTANHA'),
 (304, 3, N'Ala nascente', 'DUPLO',       80.00, 'NENHUMA'),
 (305, 3, N'Ala nascente', 'DUPLO',       80.00, 'MONTANHA'),
 (306, 3, N'Ala poente',   'DUPLO',       80.00, 'NENHUMA'),
 (307, 3, N'Ala poente',   'DUPLO',       80.00, 'MAR'),
 (308, 3, N'Ala poente',   'DUPLO',       80.00, 'MAR'),
 (309, 3, N'Ala poente',   'FAMILIA',    120.00, 'MAR'),
 (310, 3, N'Ala poente',   'FAMILIA',    120.00, 'MONTANHA');
GO

/* Equipamentos: todos têm toalhas, champô, secador e TV; duplos e família têm
   ar condicionado; família tem ainda cofre e minibar; quartos com vista mar têm varanda. */
INSERT INTO dbo.Quarto_Equipamento (IdQuarto, IdEquipamento)
SELECT q.IdQuarto, e.IdEquipamento
FROM dbo.Quarto q
CROSS JOIN dbo.Equipamento e
WHERE e.Nome IN (N'Toalhas', N'Champô', N'Secador', N'TV')
   OR (e.Nome = N'Ar condicionado' AND q.Tipo IN ('DUPLO', 'FAMILIA'))
   OR (e.Nome IN (N'Cofre', N'Minibar') AND q.Tipo = 'FAMILIA')
   OR (e.Nome = N'Varanda' AND q.Vista = 'MAR');
GO

/* ---- Clientes (dados fictícios) -------------------------------------------------------- */
INSERT INTO dbo.Cliente (Nome, Nif, Contacto) VALUES
 (N'Maria Santos',    N'210000001', N'912000001'),
 (N'João Ferreira',   N'210000002', N'912000002'),
 (N'Cristiano Ronaldo',       N'210000777', N'cr7.siu@exemplo.pt'),
 (N'Pedro Almeida',   N'210000004', N'912000004'),
 (N'Sofia Rodrigues', N'210000005', N'sofia.rodrigues@exemplo.pt');
GO

/* ---- Reservas de teste (datas relativas a hoje) -----------------------------------------
   ValorTotal = noites x (PrecoBase + suplemento da vista); canceladas valem 0. */
INSERT INTO dbo.Reserva (IdCliente, IdQuarto, DataEntrada, DataSaida, Estado, ValorTotal, DataCheckIn, DataCheckOut)
SELECT c.IdCliente, q.IdQuarto, v.Entrada, v.Saida, v.Estado,
       CASE WHEN v.Estado = 'CANCELADA' THEN 0
            ELSE DATEDIFF(DAY, v.Entrada, v.Saida)
                 * (q.PrecoBase + CASE q.Vista WHEN 'MAR' THEN 20 WHEN 'MONTANHA' THEN 15 ELSE 0 END)
       END,
       v.CheckIn, v.CheckOut
FROM (VALUES
    (N'210000001', 205, CAST(DATEADD(DAY,  5, GETDATE()) AS DATE), CAST(DATEADD(DAY,  8, GETDATE()) AS DATE), 'PENDENTE',  CAST(NULL AS DATETIME2(0)), CAST(NULL AS DATETIME2(0))),
    (N'210000002', 210, CAST(DATEADD(DAY, 10, GETDATE()) AS DATE), CAST(DATEADD(DAY, 15, GETDATE()) AS DATE), 'PENDENTE',  CAST(NULL AS DATETIME2(0)), CAST(NULL AS DATETIME2(0))),
    (N'210000003', 301, CAST(DATEADD(DAY,  3, GETDATE()) AS DATE), CAST(DATEADD(DAY,  5, GETDATE()) AS DATE), 'CANCELADA', CAST(NULL AS DATETIME2(0)), CAST(NULL AS DATETIME2(0))),
    (N'210000004', 307, CAST(DATEADD(DAY, -6, GETDATE()) AS DATE), CAST(DATEADD(DAY, -3, GETDATE()) AS DATE), 'CHECK_OUT', CAST(DATEADD(DAY, -6, GETDATE()) AS DATETIME2(0)), CAST(DATEADD(DAY, -3, GETDATE()) AS DATETIME2(0)))
) AS v (Nif, Numero, Entrada, Saida, Estado, CheckIn, CheckOut)
JOIN dbo.Cliente c ON c.Nif = v.Nif
JOIN dbo.Quarto  q ON q.Numero = v.Numero;
GO

INSERT INTO dbo.CartaoMagnetico (Numero, Mestre) VALUES
 (1, 1), (2, 1),
 (101, 0), (102, 0), (103, 0), (104, 0), (105, 0);
GO

INSERT INTO dbo.Servico (Nome, Categoria, Valor) VALUES
 (N'Água',                 N'Bebidas',   1.50),
 (N'Refrigerante',         N'Bebidas',   2.50),
 (N'Cerveja',              N'Bebidas',   3.00),
 (N'Lavandaria (peça)',    N'Lavandaria', 4.00),
 (N'Pequeno-almoço extra', N'Restauração', 8.00);
GO

