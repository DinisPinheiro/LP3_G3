/*Roles*/
CREATE TABLE dbo.Role (
    IdRole  INT IDENTITY(1,1) CONSTRAINT PK_Role PRIMARY KEY,
    Nome    NVARCHAR(30) NOT NULL CONSTRAINT UQ_Role_Nome UNIQUE
);
GO

/*Utilizadores (gestor, receção, restauração, fornecedor)*/
CREATE TABLE dbo.Utilizador (
    IdUtilizador  INT IDENTITY(1,1) CONSTRAINT PK_Utilizador PRIMARY KEY,
    IdRole        INT NOT NULL CONSTRAINT FK_Utilizador_Role REFERENCES dbo.Role(IdRole),
    Username      NVARCHAR(50)  NOT NULL CONSTRAINT UQ_Utilizador_Username UNIQUE,
    PasswordHash  NVARCHAR(255) NOT NULL,
    Nome          NVARCHAR(100) NOT NULL,
    Ativo         BIT NOT NULL CONSTRAINT DF_Utilizador_Ativo DEFAULT 1,
    CriadoEm      DATETIME2(0) NOT NULL CONSTRAINT DF_Utilizador_CriadoEm DEFAULT SYSDATETIME()
);
GO

/*Clientes*/
CREATE TABLE dbo.Cliente (
    IdCliente  INT IDENTITY(1,1) CONSTRAINT PK_Cliente PRIMARY KEY,
    Nome       NVARCHAR(100) NOT NULL,
    Documento  NVARCHAR(30)  NOT NULL CONSTRAINT UQ_Cliente_Documento UNIQUE,
    Contacto   NVARCHAR(50)  NULL,
    CriadoEm   DATETIME2(0) NOT NULL CONSTRAINT DF_Cliente_CriadoEm DEFAULT SYSDATETIME()
);
GO

/*Tipos de quarto*/
CREATE TABLE dbo.TipoQuarto (
    IdTipoQuarto  INT IDENTITY(1,1) CONSTRAINT PK_TipoQuarto PRIMARY KEY,
    Nome          NVARCHAR(30) NOT NULL CONSTRAINT UQ_TipoQuarto_Nome UNIQUE,
    Lotacao       TINYINT NOT NULL CONSTRAINT CK_TipoQuarto_Lotacao CHECK (Lotacao > 0)
);
GO

/*Quartos*/
CREATE TABLE dbo.Quarto (
    IdQuarto      INT IDENTITY(1,1) CONSTRAINT PK_Quarto PRIMARY KEY,
    Numero        INT NOT NULL CONSTRAINT UQ_Quarto_Numero UNIQUE,
    Piso          TINYINT NOT NULL,
    IdTipoQuarto  INT NOT NULL CONSTRAINT FK_Quarto_TipoQuarto REFERENCES dbo.TipoQuarto(IdTipoQuarto),
    PrecoBase     DECIMAL(10,2) NOT NULL CONSTRAINT CK_Quarto_Preco CHECK (PrecoBase >= 0),
    Vista         NVARCHAR(30) NULL,
    Ativo         BIT NOT NULL CONSTRAINT DF_Quarto_Ativo DEFAULT 1
);
GO

/*Reservas*/
CREATE TABLE dbo.Reserva (
    IdReserva    INT IDENTITY(1,1) CONSTRAINT PK_Reserva PRIMARY KEY,
    IdCliente    INT NOT NULL CONSTRAINT FK_Reserva_Cliente REFERENCES dbo.Cliente(IdCliente),
    IdQuarto     INT NOT NULL CONSTRAINT FK_Reserva_Quarto  REFERENCES dbo.Quarto(IdQuarto),
    DataEntrada  DATE NOT NULL,
    DataSaida    DATE NOT NULL,
    Estado       NVARCHAR(15) NOT NULL CONSTRAINT DF_Reserva_Estado DEFAULT N'PENDENTE',
    DataCheckIn  DATETIME2(0) NULL,
    DataCheckOut DATETIME2(0) NULL,
    CriadaEm     DATETIME2(0) NOT NULL CONSTRAINT DF_Reserva_CriadaEm DEFAULT SYSDATETIME(),
    CONSTRAINT CK_Reserva_Datas  CHECK (DataSaida > DataEntrada),
    CONSTRAINT CK_Reserva_Estado CHECK (Estado IN (N'PENDENTE', N'CHECK_IN', N'CHECK_OUT', N'CANCELADA'))
);
GO

CREATE INDEX IX_Reserva_Quarto_Datas ON dbo.Reserva (IdQuarto, DataEntrada, DataSaida);
CREATE INDEX IX_Reserva_Cliente      ON dbo.Reserva (IdCliente);
GO

/*Dados base (não dependem de decisões em aberto)*/
INSERT INTO dbo.Role (Nome) VALUES
    (N'GESTOR'), (N'RECECAO'), (N'RESTAURACAO'), (N'FORNECEDOR');

INSERT INTO dbo.TipoQuarto (Nome, Lotacao) VALUES
    (N'INDIVIDUAL', 1), (N'DUPLO', 2), (N'FAMILIA', 4);
GO

/*Verificação rápida*/
SELECT t.name AS Tabela FROM sys.tables t ORDER BY t.name;
SELECT * FROM dbo.Role;
SELECT * FROM dbo.TipoQuarto;
GO
