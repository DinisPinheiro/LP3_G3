package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.Model.Quarto;
import com.hotel4you.hotel4you.Model.enums.TipoQuarto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * {@link QuartoDAO} em memória, <b>só para testes unitários</b>. O número do quarto é único e as
 * listas vêm ordenadas por número. {@link #listarDisponiveis} ignora as reservas (não há reservas aqui).
 */
public class QuartoDAOMemoria implements QuartoDAO {

    private final Map<Integer, Quarto> quartos = new LinkedHashMap<>();
    private int proximoId = 1;

    @Override
    public Quarto inserir(Quarto quarto) {
        if (procurarPorNumero(quarto.getNumero()).isPresent()) {
            throw new DAOException("Já existe o quarto número " + quarto.getNumero() + ".");
        }
        quarto.setId(proximoId++);
        quartos.put(quarto.getId(), quarto);
        return quarto;
    }

    @Override
    public void atualizar(Quarto quarto) {
        if (!quartos.containsKey(quarto.getId())) {
            throw new DAOException("O quarto com id " + quarto.getId() + " não existe.");
        }
        quartos.put(quarto.getId(), quarto);
    }

    @Override
    public void remover(int id) {
        if (quartos.remove(id) == null) {
            throw new DAOException("O quarto com id " + id + " não existe.");
        }
    }

    @Override
    public Optional<Quarto> procurarPorId(int id) {
        return Optional.ofNullable(quartos.get(id));
    }

    @Override
    public Optional<Quarto> procurarPorNumero(int numero) {
        return quartos.values().stream().filter(q -> q.getNumero() == numero).findFirst();
    }

    @Override
    public List<Quarto> listarTodos() {
        List<Quarto> lista = new ArrayList<>(quartos.values());
        lista.sort(Comparator.comparingInt(Quarto::getNumero));
        return lista;
    }

    @Override
    public List<Quarto> listarAtivos() {
        return listarTodos().stream().filter(Quarto::isAtivo).collect(Collectors.toList());
    }

    @Override
    public List<Quarto> listarDisponiveis(TipoQuarto tipo, LocalDate dataEntrada, LocalDate dataSaida) {
        return listarAtivos().stream().filter(q -> tipo == null || q.getTipo() == tipo).collect(Collectors.toList());
    }
}
