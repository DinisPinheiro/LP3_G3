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
 * Guarda reservas em memória.
 * Utilizado principalmente nos testes unitários.
 */
public class ReservaDAOMemoria implements ReservaDAO {

    private final Map<Integer, Reserva> reservas =
            new LinkedHashMap<>();

    private int proximoId = 1;

    /**
     * Cria uma reserva.
     */
    @Override
    public Reserva inserir(Reserva reserva) {

        if (existeSobreposicao(
                reserva.getQuarto().getId(),
                reserva.getDataEntrada(),
                reserva.getDataSaida())) {

            throw new DAOException(
                    "O quarto já está reservado nesse período."
            );
        }

        reserva.setId(proximoId);

        reservas.put(proximoId, reserva);

        proximoId++;

        return reserva;
    }

    /**
     * Atualiza uma reserva existente.
     */
    @Override
    public void atualizar(Reserva reserva) {

        if (!reservas.containsKey(reserva.getId())) {
            throw new DAOException(
                    "A reserva não existe."
            );
        }

        reservas.put(reserva.getId(), reserva);
    }

    /**
     * Atualiza os serviços de uma reserva ativa.
     */
    @Override
    public void atualizarServicos(Reserva reserva) {

        if (reserva == null) {
            throw new IllegalArgumentException(
                    "A reserva é obrigatória."
            );
        }

        if (!reservas.containsKey(reserva.getId())) {
            throw new DAOException(
                    "A reserva não existe."
            );
        }

        if (!reserva.podeEditarServicos()) {
            throw new IllegalStateException(
                    "Esta reserva não pode ser editada."
            );
        }

        reservas.put(reserva.getId(), reserva);
    }

    /**
     * Remove uma reserva que nunca teve check-in.
     */
    @Override
    public void remover(int id) {

        Reserva reserva = reservas.get(id);

        if (reserva == null) {
            throw new DAOException(
                    "A reserva não existe."
            );
        }

        if (!reserva.podeSerApagada()) {
            throw new DAOException(
                    "A reserva já teve check-in e não pode ser apagada."
            );
        }

        reservas.remove(id);
    }

    /**
     * Procura uma reserva pelo identificador.
     */
    @Override
    public Optional<Reserva> procurarPorId(int id) {

        return Optional.ofNullable(reservas.get(id));
    }

    /**
     * Lista todas as reservas.
     */
    @Override
    public List<Reserva> listarTodos() {

        return new ArrayList<>(reservas.values());
    }

    /**
     * Lista as reservas de um cliente.
     */
    @Override
    public List<Reserva> listarPorCliente(int idCliente) {

        List<Reserva> lista = new ArrayList<>();

        for (Reserva reserva : reservas.values()) {

            if (reserva.getCliente().getId() == idCliente) {
                lista.add(reserva);
            }
        }

        return lista;
    }

    /**
     * Verifica se um quarto já está reservado.
     */
    @Override
    public boolean existeSobreposicao(
            int idQuarto,
            LocalDate dataEntrada,
            LocalDate dataSaida) {

        for (Reserva reserva : reservas.values()) {

            boolean ativa =
                    reserva.getEstado() == EstadoReserva.PENDENTE
                            || reserva.getEstado() == EstadoReserva.CHECK_IN;

            boolean mesmoQuarto =
                    reserva.getQuarto().getId() == idQuarto;

            boolean datasSobrepostas =
                    reserva.getDataEntrada().isBefore(dataSaida)
                            && reserva.getDataSaida().isAfter(dataEntrada);

            if (ativa && mesmoQuarto && datasSobrepostas) {
                return true;
            }
        }

        return false;
    }

    @Override
    public List<Reserva> listarPorEstados(List<EstadoReserva> estados, int pagina, int tamanhoPagina) {
        validarPaginacao(estados, pagina, tamanhoPagina);
        List<Reserva> resultado = new ArrayList<>();
        for (Reserva reserva : reservas.values()) {
            if (estados.contains(reserva.getEstado())) resultado.add(reserva);
        }
        resultado.sort((a, b) -> {
            int datas = b.getDataEntrada().compareTo(a.getDataEntrada());
            if (datas != 0) return datas;
            return Integer.compare(b.getId(), a.getId());
        });
        long inicio = ((long) pagina - 1) * tamanhoPagina;
        if (inicio >= resultado.size()) return new ArrayList<>();
        int fim = (int) Math.min(inicio + tamanhoPagina, resultado.size());
        return new ArrayList<>(resultado.subList((int) inicio, fim));
    }

    @Override
    public int contarPorEstados(List<EstadoReserva> estados) {
        validarPaginacao(estados, 1, 1);
        int total = 0;
        for (Reserva reserva : reservas.values()) {
            if (estados.contains(reserva.getEstado())) total++;
        }
        return total;
    }

    // Não se usa estados.contains(null): numa lista criada com List.of(...) isso lança NullPointerException.
    private static boolean temEstadoNulo(List<EstadoReserva> estados) {
        for (EstadoReserva estado : estados) {
            if (estado == null) {
                return true;
            }
        }
        return false;
    }

    private void validarPaginacao(List<EstadoReserva> estados, int pagina, int tamanhoPagina) {
        if (estados == null || estados.isEmpty() || temEstadoNulo(estados))
            throw new IllegalArgumentException("Escolha um estado válido.");
        if (pagina < 1 || tamanhoPagina < 1 || tamanhoPagina > 100
                || ((long) pagina - 1) * tamanhoPagina > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Paginação inválida.");
    }

}