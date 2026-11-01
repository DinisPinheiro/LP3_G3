package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.Model.Equipamento;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@link EquipamentoDAO} em memória, <b>só para testes unitários</b>. O nome do equipamento é único.
 */
public class EquipamentoDAOMemoria implements EquipamentoDAO {

    private final Map<Integer, Equipamento> equipamentos = new LinkedHashMap<>();
    private int proximoId = 1;

    @Override
    public Equipamento inserir(Equipamento equipamento) {
        boolean existe = equipamentos.values().stream()
                .anyMatch(e -> e.getNome().equalsIgnoreCase(equipamento.getNome()));
        if (existe) {
            throw new DAOException("Já existe o equipamento '" + equipamento.getNome() + "'.");
        }
        equipamento.setId(proximoId++);
        equipamentos.put(equipamento.getId(), equipamento);
        return equipamento;
    }

    @Override
    public void atualizar(Equipamento equipamento) {
        if (!equipamentos.containsKey(equipamento.getId())) {
            throw new DAOException("O equipamento com id " + equipamento.getId() + " não existe.");
        }
        equipamentos.put(equipamento.getId(), equipamento);
    }

    @Override
    public void remover(int id) {
        if (equipamentos.remove(id) == null) {
            throw new DAOException("O equipamento com id " + id + " não existe.");
        }
    }

    @Override
    public Optional<Equipamento> procurarPorId(int id) {
        return Optional.ofNullable(equipamentos.get(id));
    }

    @Override
    public List<Equipamento> listarTodos() {
        return new ArrayList<>(equipamentos.values());
    }
}
