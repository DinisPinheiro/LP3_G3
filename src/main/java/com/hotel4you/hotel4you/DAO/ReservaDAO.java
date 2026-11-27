package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.Model.Reserva;

import java.time.LocalDate;
import java.util.List;

/**
 * Acesso às reservas. O histórico não se perde: o DAO só apaga reservas que nunca tiveram
 * check-in ({@code Reserva.podeSerApagada()}). Quem tem permissão para apagar
 * ({@code Utilizador.podeApagarReservas()}) é verificado pelo Controller.
 * <p>
 * Persistem-se cliente, quarto, datas, estado, valor total, momentos de check-in/check-out,
 * o cartão magnético entregue no check-in, os preços em vigor na criação (preço base e vista)
 * e os serviços e produtos escolhidos, com a quantidade.
 */
public interface ReservaDAO extends DAO<Reserva> {

    /**
     * {@inheritDoc}
     * <p>
     * A verificação de disponibilidade e a gravação são feitas na mesma transação, para duas
     * reservas em simultâneo não ocuparem o mesmo quarto. Os serviços e os produtos da reserva
     * são guardados na mesma transação, que também confirma se o stock dos produtos chega.
     *
     * @throws DAOException se o cliente ou o quarto não estiverem guardados, se o quarto já tiver
     *                      uma reserva ativa nesse período, se não houver stock suficiente de um
     *                      produto ou se não for possível guardar
     */
    @Override
    Reserva inserir(Reserva reserva);

    /**
     * {@inheritDoc}
     * <p>
     * Atualiza estado, valor total e momentos de check-in/check-out (cliente, quarto e datas não mudam).
     * Numa reserva pendente ou com check-in também guarda os produtos do stock (S6-05), na mesma
     * transação; com check-in confirma ainda que o stock chega (S7-01: só aí o stock está em uso).
     *
     * @throws DAOException se a reserva não existir, se o stock de um produto não chegar ou se não for
     *                      possível guardar
     */
    @Override
    void atualizar(Reserva reserva);

    /**
     * Atualiza os serviços e o valor total de uma reserva com check-in. Também guarda os produtos do
     * stock da reserva, porque os serviços ligados ao stock (ex.: bebidas) mudam esses produtos (S7-01),
     * e confirma que o stock chega.
     *
     * @param reserva reserva com os serviços atualizados
     */
    void atualizarServicos(Reserva reserva);

    /**
     * {@inheritDoc}
     * <p>
     * Regra de negócio validada aqui (e não no Controller): só se apaga uma reserva que nunca
     * teve check-in, ou seja, enquanto {@code Reserva.podeSerApagada()} for verdadeiro.
     * Uma reserva com check-in (ou já com check-out) fica guardada no histórico.
     * Os serviços e os produtos da reserva são apagados com ela.
     *
     * @throws DAOException se a reserva não existir, se já teve check-in ou se não for possível apagar
     */
    @Override
    void remover(int id);

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

    /**
     * Igual a {@link #existeSobreposicao(int, LocalDate, LocalDate)}, mas sem contar com uma reserva
     * (a que está a ser editada).
     *
     * @param idQuarto          identificador do quarto
     * @param dataEntrada       primeira noite
     * @param dataSaida         dia de saída
     * @param idReservaIgnorada reserva que não conta como ocupação
     * @return {@code true} se outra reserva ativa se sobrepõe ao período
     * @throws DAOException se não for possível ler
     */
    boolean existeSobreposicao(int idQuarto, LocalDate dataEntrada, LocalDate dataSaida, int idReservaIgnorada);

    /**
     * Guarda as novas datas de uma reserva ativa e o novo valor total. A verificação de disponibilidade e a
     * gravação são feitas na mesma transação.
     *
     * @param reserva reserva com as datas já alteradas
     * @throws DAOException se a reserva não existir, não estiver ativa, se o quarto já tiver outra reserva
     *                      ativa nesse período ou se não for possível guardar
     */
    void atualizarDatas(Reserva reserva);

    // Consulta paginada das reservas nos estados escolhidos.
    List<Reserva> listarPorEstados(List<com.hotel4you.hotel4you.Model.enums.EstadoReserva> estados, int pagina, int tamanhoPagina);
    // Número de reservas nos estados escolhidos.
    int contarPorEstados(List<com.hotel4you.hotel4you.Model.enums.EstadoReserva> estados);

}
