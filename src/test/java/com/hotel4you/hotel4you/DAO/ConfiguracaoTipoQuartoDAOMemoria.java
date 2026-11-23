package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.Model.ConfiguracaoTipoQuarto;
import com.hotel4you.hotel4you.Model.enums.TipoQuarto;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@link ConfiguracaoTipoQuartoDAO} em memória, <b>só para testes unitários</b>.
 */
public class ConfiguracaoTipoQuartoDAOMemoria implements ConfiguracaoTipoQuartoDAO {

    private final Map<TipoQuarto, ConfiguracaoTipoQuarto> configuracoes = new EnumMap<>(TipoQuarto.class);

    @Override
    public Optional<ConfiguracaoTipoQuarto> procurar(TipoQuarto tipo) {
        return Optional.ofNullable(configuracoes.get(tipo));
    }

    @Override
    public List<ConfiguracaoTipoQuarto> listar() {
        return new ArrayList<>(configuracoes.values());
    }

    @Override
    public void guardar(ConfiguracaoTipoQuarto configuracao) {
        configuracoes.put(configuracao.getTipo(), configuracao);
    }
}
