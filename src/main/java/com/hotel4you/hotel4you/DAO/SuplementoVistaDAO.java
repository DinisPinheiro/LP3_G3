package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.Model.enums.Vista;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Acesso aos valores das vistas dos quartos (tabela {@code dbo.SuplementoVista}), que o gestor pode
 * editar sem mexer na base de dados.
 * <p>
 */
public interface SuplementoVistaDAO {

    /**
     * @return o valor em vigor de cada vista, pela ordem de {@link Vista}; uma vista que ainda não esteja
     *         na base de dados aparece com o seu valor inicial ({@link Vista#getSuplemento()})
     * @throws DAOException se não for possível ler
     */
    Map<Vista, BigDecimal> listar();

    /**
     * Guarda o novo valor de uma vista. A partir daqui o preço por noite dos quartos com essa vista,
     * a disponibilidade e o total das reservas novas usam este valor.
     *
     * @param vista vista a alterar
     * @param valor novo valor a somar ao preço base por noite, não negativo
     * @throws DAOException se não for possível guardar
     */
    void atualizar(Vista vista, BigDecimal valor);
}
