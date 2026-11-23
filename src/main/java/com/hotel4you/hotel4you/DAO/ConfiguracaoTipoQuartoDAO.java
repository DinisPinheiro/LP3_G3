package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.Model.ConfiguracaoTipoQuarto;
import com.hotel4you.hotel4you.Model.enums.TipoQuarto;

import java.util.List;
import java.util.Optional;

/**
 * Acesso à configuração de cada tipo de quarto (S6-09): preço base e equipamentos que os quartos do
 * tipo herdam. Há no máximo uma configuração por tipo.
 */
public interface ConfiguracaoTipoQuartoDAO {

    /**
     * @param tipo tipo de quarto
     * @return a configuração do tipo, ou vazio se o gestor ainda não a definiu
     * @throws DAOException se não for possível ler
     */
    Optional<ConfiguracaoTipoQuarto> procurar(TipoQuarto tipo);

    /**
     * @return as configurações que já foram definidas (uma por tipo, pela ordem dos tipos)
     * @throws DAOException se não for possível ler
     */
    List<ConfiguracaoTipoQuarto> listar();

    /**
     * Guarda a configuração de um tipo, substituindo a anterior (preço, equipamentos e produtos numa só
     * transação). Os equipamentos e os produtos têm de estar já guardados.
     *
     * @param configuracao configuração a guardar
     * @throws DAOException se algum equipamento ou produto não estiver guardado ou se não for possível guardar
     */
    void guardar(ConfiguracaoTipoQuarto configuracao);
}
