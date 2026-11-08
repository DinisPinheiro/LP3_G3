package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.Model.Servico;

import java.util.List;

/**
 * Acesso ao catálogo de serviços e extras. Implementação: {@link ServicoDAOSQL}.
 * <p>
 */
public interface ServicoDAO extends DAO<Servico> {

    /**
     * @return só os serviços e extras ativos (os que podem ser oferecidos ao cliente)
     * @throws DAOException se não for possível ler
     */
    List<Servico> listarAtivos();
}
