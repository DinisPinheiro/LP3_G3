package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.Model.Fornecedor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@link FornecedorDAO} em memória, <b>só para testes unitários</b>. Respeita o contrato do DAO real:
 * o código do fornecedor é único (sem distinguir maiúsculas) e uma conta só pode ser de um fornecedor.
 */
public class FornecedorDAOMemoria implements FornecedorDAO {

    private final Map<Integer, Fornecedor> fornecedores = new LinkedHashMap<>();
    private int proximoId = 1;

    @Override
    public Fornecedor inserir(Fornecedor fornecedor) {
        if (procurarPorIdentificador(fornecedor.getIdentificador()).isPresent()) {
            throw new DAOException("Já existe um fornecedor com o código '" + fornecedor.getIdentificador() + "'.");
        }
        exigirContaLivre(fornecedor);
        fornecedor.setId(proximoId++);
        fornecedores.put(fornecedor.getId(), fornecedor);
        return fornecedor;
    }

    @Override
    public void atualizar(Fornecedor fornecedor) {
        if (!fornecedores.containsKey(fornecedor.getId())) {
            throw new DAOException("O fornecedor com id " + fornecedor.getId() + " não existe.");
        }
        exigirContaLivre(fornecedor);
        fornecedores.put(fornecedor.getId(), fornecedor);
    }

    @Override
    public void remover(int id) {
        if (fornecedores.remove(id) == null) {
            throw new DAOException("O fornecedor com id " + id + " não existe.");
        }
    }

    @Override
    public Optional<Fornecedor> procurarPorId(int id) {
        return Optional.ofNullable(fornecedores.get(id));
    }

    @Override
    public Optional<Fornecedor> procurarPorIdentificador(String identificador) {
        if (identificador == null) {
            return Optional.empty();
        }
        return fornecedores.values().stream()
                .filter(f -> f.getIdentificador().equalsIgnoreCase(identificador.trim()))
                .findFirst();
    }

    @Override
    public Optional<Fornecedor> procurarPorUtilizador(int idUtilizador) {
        return fornecedores.values().stream()
                .filter(f -> f.temConta() && f.getIdUtilizador() == idUtilizador)
                .findFirst();
    }

    @Override
    public List<Fornecedor> listarTodos() {
        List<Fornecedor> lista = new ArrayList<>(fornecedores.values());
        lista.sort((a, b) -> a.getNome().compareToIgnoreCase(b.getNome()));
        return lista;
    }

    private void exigirContaLivre(Fornecedor fornecedor) {
        if (fornecedor.temConta() && fornecedores.values().stream()
                .anyMatch(f -> f.getId() != fornecedor.getId() && f.getIdUtilizador() == fornecedor.getIdUtilizador())) {
            throw new DAOException("A conta já está ligada a outro fornecedor.");
        }
    }
}
