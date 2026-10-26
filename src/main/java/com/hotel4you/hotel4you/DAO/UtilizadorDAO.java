package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.Model.Utilizador;

import java.util.Optional;

/**
 * Acesso aos utilizadores guardados (RF: todos os utilizadores são guardados em sistema
 * e podem ser consultados a qualquer momento).
 */
public interface UtilizadorDAO extends DAO<Utilizador> {

    /**
     * @param username nome de utilizador (sem distinguir maiúsculas de minúsculas)
     * @return o utilizador, ou vazio se não existir
     * @throws DAOException se não for possível ler
     */
    Optional<Utilizador> procurarPorUsername(String username);
}
