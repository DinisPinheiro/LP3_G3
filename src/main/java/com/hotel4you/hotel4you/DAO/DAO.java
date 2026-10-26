package com.hotel4you.hotel4you.DAO;

import java.util.List;
import java.util.Optional;

/**
 * Interface base dos DAO (Data Access Objects). Cada entidade persistente tem uma
 * interface própria que estende esta, e uma ou mais implementações (SQL Server, memória, ...).
 *
 * @param <T> tipo da entidade
 */
public interface DAO<T> {

    /**
     * Guarda uma entidade nova e atribui-lhe o identificador.
     *
     * @param entidade entidade ainda sem id
     * @return a mesma entidade, já com id
     * @throws DAOException se não for possível guardar
     */
    T inserir(T entidade);

    /**
     * Atualiza uma entidade já guardada.
     *
     * @param entidade entidade com id existente
     * @throws DAOException se não existir ou não for possível atualizar
     */
    void atualizar(T entidade);

    /**
     * @param id identificador da entidade a remover
     * @throws DAOException se não for possível remover
     */
    void remover(int id);

    /**
     * @param id identificador
     * @return a entidade, ou vazio se não existir
     * @throws DAOException se não for possível ler
     */
    Optional<T> procurarPorId(int id);

    /**
     * @return todas as entidades guardadas
     * @throws DAOException se não for possível ler
     */
    List<T> listarTodos();
}
