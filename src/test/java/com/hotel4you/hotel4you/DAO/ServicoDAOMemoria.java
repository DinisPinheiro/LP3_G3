package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.Model.Servico;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * {@link ServicoDAO} em memória, <b>só para testes unitários</b>. O nome do serviço é único,
 * como na base de dados.
 */
public class ServicoDAOMemoria implements ServicoDAO {

    private final Map<Integer, Servico> servicos = new LinkedHashMap<>();
    private int proximoId = 1;

    @Override
    public Servico inserir(Servico servico) {
        boolean repetido = servicos.values().stream()
                .anyMatch(s -> s.getNome().equalsIgnoreCase(servico.getNome()));
        if (repetido) {
            throw new DAOException("Já existe um serviço ou extra com o nome '" + servico.getNome() + "'.");
        }
        servico.setId(proximoId++);
        servicos.put(servico.getId(), servico);
        return servico;
    }

    @Override
    public void atualizar(Servico servico) {
        if (!servicos.containsKey(servico.getId())) {
            throw new DAOException("O serviço com id " + servico.getId() + " não existe.");
        }
        servicos.put(servico.getId(), servico);
    }

    @Override
    public void remover(int id) {
        if (servicos.remove(id) == null) {
            throw new DAOException("O serviço com id " + id + " não existe.");
        }
    }

    @Override
    public Optional<Servico> procurarPorId(int id) {
        return Optional.ofNullable(servicos.get(id));
    }

    @Override
    public List<Servico> listarTodos() {
        return new ArrayList<>(servicos.values());
    }

    @Override
    public List<Servico> listarAtivos() {
        return servicos.values().stream().filter(Servico::isAtivo).collect(Collectors.toList());
    }
}
