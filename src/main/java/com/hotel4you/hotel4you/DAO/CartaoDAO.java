package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.Model.CartaoMagnetico;

import java.util.Optional;

/**
 * Acesso aos cartões magnéticos guardados (tabela {@code dbo.CartaoMagnetico}).
 */
public interface CartaoDAO extends DAO<CartaoMagnetico> {

    /** Primeiro número usado nos cartões entregues aos clientes (os mais baixos são do arranque/mestre). */
    int PRIMEIRO_NUMERO_CLIENTE = 1001;

    @Override
    CartaoMagnetico inserir(CartaoMagnetico cartao);

    @Override
    void atualizar(CartaoMagnetico cartao);

    /**
     * @param numero número do cartão
     * @return o cartão, ou vazio se não existir
     * @throws DAOException se não for possível ler
     */
    Optional<CartaoMagnetico> procurarPorNumero(int numero);

    /**
     * Devolve o próximo número de cartão livre: um a mais do que o maior já usado
     * (incluindo cartões desativados, para um número nunca ser reutilizado), e nunca
     * menor que {@link #PRIMEIRO_NUMERO_CLIENTE}.
     *
     * @return número que ainda não existe
     * @throws DAOException se não for possível ler
     */
    int proximoNumero();
}