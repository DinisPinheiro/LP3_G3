package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.Model.Fornecedor;

import java.util.Optional;

/**
 * Acesso aos fornecedores (S5-02): ficha com código, nome, morada, código postal e cidade, e a
 * ligação à conta de login do fornecedor.
 */
public interface FornecedorDAO extends DAO<Fornecedor> {

    /**
     * @param identificador código do fornecedor (sem distinguir maiúsculas de minúsculas)
     * @return o fornecedor, ou vazio se não existir
     * @throws DAOException se não for possível ler
     */
    Optional<Fornecedor> procurarPorIdentificador(String identificador);

    /**
     * @param idUtilizador id da conta de login
     * @return o fornecedor ligado a essa conta, ou vazio se a conta não for de nenhum fornecedor
     * @throws DAOException se não for possível ler
     */
    Optional<Fornecedor> procurarPorUtilizador(int idUtilizador);
}
