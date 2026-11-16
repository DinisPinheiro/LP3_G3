IF OBJECT_ID(N'dbo.Passagem', N'U') IS NULL
BEGIN
CREATE TABLE dbo.Passagem (
                              IdPassagem INT IDENTITY(1,1) PRIMARY KEY,
                              IdCartao INT NOT NULL,
                              IdQuarto INT NULL,
                              Local NVARCHAR(30) NOT NULL,
                              DataHora DATETIME2(0) NOT NULL
            DEFAULT SYSDATETIME(),

                              CONSTRAINT FK_Passagem_Cartao
                                  FOREIGN KEY (IdCartao)
                                      REFERENCES dbo.CartaoMagnetico(IdCartao),

                              CONSTRAINT FK_Passagem_Quarto
                                  FOREIGN KEY (IdQuarto)
                                      REFERENCES dbo.Quarto(IdQuarto),

                              CONSTRAINT CK_Passagem_Local
                                  CHECK (Local IN (
                                      'QUARTO',
                                      'RESTAURANTE',
                                      'PISCINA',
                                      'COZINHA'
                                      ))
    );

CREATE INDEX IX_Passagem_Cartao
    ON dbo.Passagem(IdCartao, DataHora);

CREATE INDEX IX_Passagem_Quarto
    ON dbo.Passagem(IdQuarto, DataHora);
END;
