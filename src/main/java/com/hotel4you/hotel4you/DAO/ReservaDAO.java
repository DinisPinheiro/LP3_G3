package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.Model.Reserva;

import java.time.LocalDate;
import java.util.List;

/**
 * Acesso às reservas. O histórico não se perde: a regra de quem pode apagar uma reserva
 * ({@code Utilizador.podeApagarReservas()} e {@code Reserva.podeSerApagada()}) é verificada
 * pelo Controller antes de chamar {@link #remover(int)}.
 * <p>
 * Nesta versão persistem-se cliente, quarto, datas, estado, valor total e momentos de
 * check-in/check-out. O cartão magnético e os consumos ficam para a Sprint 4.
 */
public interface ReservaDAO extends DAO<Reserva> {

    /**
     * {@inheritDoc}
     * <p>
     * A verificação de disponibilidade e a gravação são feitas na mesma transação, para duas
     * reservas em simultâneo não ocuparem o mesmo quarto.
     *
     * @throws DAOException se o cliente ou o quarto não estiverem guardados, se o quarto já tiver
     *                      uma reserva ativa nesse período ou se não for possível guardar
     */
    @Override
    Reserva inserir(Reserva reserva);

    /**
     * {@inheritDoc}
     * <p>
     * Atualiza estado, valor total e momentos de check-in/check-out (cliente, quarto e datas não mudam).
     */
    @Override
    void atualizar(Reserva reserva);

    /**
     * @param idCliente identificador do cliente
     * @return reservas do cliente, da mais recente para a mais antiga
     * @throws DAOException se não for possível ler
     */
    List<Reserva> listarPorCliente(int idCliente);

    /**
     * @param idQuarto    identificador do quarto
     * @param dataEntrada primeira noite
     * @param dataSaida   dia de saída
     * @return {@code true} se o quarto tem uma reserva ativa (pendente ou com check-in) que se sobreponha ao período
     * @throws DAOException se não for possível ler
     */
    boolean existeSobreposicao(int idQuarto, LocalDate dataEntrada, LocalDate dataSaida);
}
