package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.Model.Reserva;
import com.hotel4you.hotel4you.Model.enums.EstadoReserva;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@link ReservaDAO} em memória, <b>só para testes unitários</b>. Respeita o contrato do DAO real:
 * não deixa reservar um quarto que já tenha uma reserva ativa (pendente ou com check-in)
 * que se sobreponha ao período.
 */
public class ReservaDAOMemoria implements ReservaDAO {

    private final Map<Integer, Reserva> reservas = new LinkedHashMap<>();
    private int proximoId = 1;

    @Override
    public Reserva inserir(Reserva reserva) {
        if (existeSobreposicao(reserva.getQuarto().getId(), reserva.getDataEntrada(), reserva.getDataSaida())) {
            throw new DAOException("O " + reserva.getQuarto() + " já está reservado nesse período.");
        }
        reserva.setId(proximoId++);
        reservas.put(reserva.getId(), reserva);
        return reserva;
    }

    @Override
    public void atualizar(Reserva reserva) {
        if (!reservas.containsKey(reserva.getId())) {
            throw new DAOException("A reserva com id " + reserva.getId() + " não existe.");
        }
        reservas.put(reserva.getId(), reserva);
    }

    @Override
    public void remover(int id) {
        if (reservas.remove(id) == null) {
            throw new DAOException("A reserva com id " + id + " não existe.");
        }
    }

    @Override
    public Optional<Reserva> procurarPorId(int id) {
        return Optional.ofNullable(reservas.get(id));
    }

    @Override
    public List<Reserva> listarTodos() {
        return new ArrayList<>(reservas.values());
    }

    @Override
    public List<Reserva> listarPorCliente(int idCliente) {
        List<Reserva> lista = new ArrayList<>();
        for (Reserva r : reservas.values()) {
            if (r.getCliente().getId() == idCliente) {
                lista.add(r);
            }
        }
        return lista;
    }

    @Override
    public boolean existeSobreposicao(int idQuarto, LocalDate dataEntrada, LocalDate dataSaida) {
        for (Reserva r : reservas.values()) {
            boolean ativa = r.getEstado() == EstadoReserva.PENDENTE || r.getEstado() == EstadoReserva.CHECK_IN;
            if (ativa && r.getQuarto().getId() == idQuarto
                    && r.getDataEntrada().isBefore(dataSaida) && r.getDataSaida().isAfter(dataEntrada)) {
                return true;
            }
        }
        return false;
    }
}