/* ============================================================================================
   Check-in e check-out (Sprint 4)
   --------------------------------------------------------------------------------------------
   O que este script faz:
     1. Garante que existe a tabela dbo.CartaoMagnetico (cartões entregues no check-in).
     2. Acrescenta a coluna Ativo aos cartões (o check-out desativa o cartão).
     3. Garante que a reserva sabe qual é o seu cartão (coluna IdCartao).
     4. Garante que o estado da reserva só pode ser PENDENTE, CHECK_IN, CHECK_OUT ou CANCELADA.
     5. Regra nova: uma reserva com check-in tem de ter um cartão.

   Como usar:
     - Corre DEPOIS do EsquemaDB_2_1.sql.
     - Pode ser corrido mais do que uma vez: só faz o que ainda não estiver feito.
     - Não apaga nem altera dados.
   ============================================================================================ */
SET NOCOUNT ON;
GO

USE [2026_LP3_FEIRA_G3];
GO


/* --------------------------------------------------------------------------------------------
   1. Tabela dos cartões magnéticos (só é criada se ainda não existir)
   -------------------------------------------------------------------------------------------- */
IF OBJECT_ID(N'dbo.CartaoMagnetico', N'U') IS NULL
BEGIN
CREATE TABLE dbo.CartaoMagnetico (
                                     IdCartao  INT IDENTITY(1,1)  NOT NULL,
                                     Numero    INT                NOT NULL,
                                     Mestre    BIT                NOT NULL CONSTRAINT DF_Cartao_Mestre DEFAULT (0),
                                     Ativo     BIT                NOT NULL CONSTRAINT DF_Cartao_Ativo  DEFAULT (1),

                                     CONSTRAINT PK_CartaoMagnetico        PRIMARY KEY (IdCartao),
                                     CONSTRAINT UQ_CartaoMagnetico_Numero UNIQUE (Numero),
                                     CONSTRAINT CK_CartaoMagnetico_Numero CHECK (Numero > 0)
);
END
GO


/* --------------------------------------------------------------------------------------------
   2. Coluna Ativo (1 = o cartão ainda abre portas, 0 = foi desativado no check-out)
      Os cartões que já existem ficam ativos.
   -------------------------------------------------------------------------------------------- */
IF COL_LENGTH(N'dbo.CartaoMagnetico', N'Ativo') IS NULL
BEGIN
ALTER TABLE dbo.CartaoMagnetico
    ADD Ativo BIT NOT NULL CONSTRAINT DF_Cartao_Ativo DEFAULT (1);
END
GO


/* --------------------------------------------------------------------------------------------
   3. Cartão da reserva (a reserva fica sem cartão até ao check-in)
   -------------------------------------------------------------------------------------------- */
IF COL_LENGTH(N'dbo.Reserva', N'IdCartao') IS NULL
BEGIN
ALTER TABLE dbo.Reserva ADD IdCartao INT NULL;
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_Reserva_Cartao')
BEGIN
ALTER TABLE dbo.Reserva
    ADD CONSTRAINT FK_Reserva_Cartao
        FOREIGN KEY (IdCartao) REFERENCES dbo.CartaoMagnetico (IdCartao);
END
GO


/* --------------------------------------------------------------------------------------------
   4. Estado da reserva: só são aceites estes quatro valores
        PENDENTE  -> reservado, à espera de check-in
        CHECK_IN  -> cliente instalado, com cartão
        CHECK_OUT -> cliente saiu, cartão desativado, quarto livre
        CANCELADA -> reserva cancelada antes do check-in
   -------------------------------------------------------------------------------------------- */
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_Reserva_Estado')
BEGIN
ALTER TABLE dbo.Reserva
    ADD CONSTRAINT CK_Reserva_Estado
        CHECK (Estado IN ('PENDENTE', 'CHECK_IN', 'CHECK_OUT', 'CANCELADA'));
END
GO


/* --------------------------------------------------------------------------------------------
   5. Regra: uma reserva em CHECK_IN tem sempre um cartão associado
   -------------------------------------------------------------------------------------------- */
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = N'CK_Reserva_CheckIn')
BEGIN
ALTER TABLE dbo.Reserva
    ADD CONSTRAINT CK_Reserva_CheckIn
        CHECK (Estado <> 'CHECK_IN' OR IdCartao IS NOT NULL);
END
GO


/* --------------------------------------------------------------------------------------------
   Verificação: no fim devem aparecer duas tabelas, sem mensagens de erro a vermelho.
   -------------------------------------------------------------------------------------------- */
SELECT IdCartao, Numero, Mestre, Ativo
FROM dbo.CartaoMagnetico
ORDER BY Numero;

SELECT IdReserva, Estado, IdCartao, DataCheckIn, DataCheckOut
FROM dbo.Reserva
ORDER BY IdReserva;
GO