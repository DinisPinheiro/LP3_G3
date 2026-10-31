package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.Model.Cliente;

import java.util.Optional;

/**
 * Acesso aos clientes guardados. O documento de identificação do cliente ({@link Cliente#getDocumento()})
 * é o NIF e é único: não podem existir dois clientes com o mesmo.
 */
public interface ClienteDAO extends DAO<Cliente> {

    /**
     * {@inheritDoc}
     *
     * @throws DAOException se já existir um cliente com o mesmo NIF/documento, ou se não for possível guardar
     */
    @Override
    Cliente inserir(Cliente cliente);

    /**
     * @param documento NIF/documento de identificação
     * @return o cliente, ou vazio se não existir
     * @throws DAOException se não for possível ler
     */
    Optional<Cliente> procurarPorDocumento(String documento);
}
