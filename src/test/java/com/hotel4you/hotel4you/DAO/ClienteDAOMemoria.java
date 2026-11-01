package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.Model.Cliente;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@link ClienteDAO} em memória, <b>só para testes unitários</b> (os controllers testam-se sem base de dados).
 * Respeita o contrato do DAO real: o NIF/documento não pode repetir-se.
 */
public class ClienteDAOMemoria implements ClienteDAO {

    private final Map<Integer, Cliente> clientes = new LinkedHashMap<>();
    private int proximoId = 1;

    @Override
    public Cliente inserir(Cliente cliente) {
        exigirDocumentoLivre(cliente, 0);
        cliente.setId(proximoId++);
        clientes.put(cliente.getId(), cliente);
        return cliente;
    }

    @Override
    public void atualizar(Cliente cliente) {
        if (!clientes.containsKey(cliente.getId())) {
            throw new DAOException("O cliente com id " + cliente.getId() + " não existe.");
        }
        exigirDocumentoLivre(cliente, cliente.getId());
        clientes.put(cliente.getId(), cliente);
    }

    @Override
    public void remover(int id) {
        if (clientes.remove(id) == null) {
            throw new DAOException("O cliente com id " + id + " não existe.");
        }
    }

    @Override
    public Optional<Cliente> procurarPorId(int id) {
        return Optional.ofNullable(clientes.get(id));
    }

    @Override
    public List<Cliente> listarTodos() {
        return new ArrayList<>(clientes.values());
    }

    @Override
    public Optional<Cliente> procurarPorDocumento(String documento) {
        if (documento == null) {
            return Optional.empty();
        }
        return clientes.values().stream()
                .filter(c -> c.getDocumento().equalsIgnoreCase(documento.trim()))
                .findFirst();
    }

    private void exigirDocumentoLivre(Cliente cliente, int idIgnorado) {
        boolean repetido = clientes.values().stream()
                .anyMatch(c -> c.getId() != idIgnorado && c.getDocumento().equalsIgnoreCase(cliente.getDocumento()));
        if (repetido) {
            throw new DAOException("Já existe um cliente com o NIF/documento '" + cliente.getDocumento() + "'.");
        }
    }
}
