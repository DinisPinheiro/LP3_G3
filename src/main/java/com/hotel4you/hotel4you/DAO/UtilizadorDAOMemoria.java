package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.Model.Utilizador;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementação <b>temporária</b> de {@link UtilizadorDAO} que guarda os dados em memória.
 * <p>
 * Existe para a aplicação arrancar e o login funcionar antes de a ligação ao SQL Server (S2-04)
 * estar pronta, e para os testes unitários. Quando existir o DAO SQL, só é preciso trocar a
 * instância criada no {@code Main}.
 */
public class UtilizadorDAOMemoria implements UtilizadorDAO {

    private final Map<Integer, Utilizador> utilizadores = new LinkedHashMap<>();
    private int proximoId = 1;

    @Override
    public Utilizador inserir(Utilizador utilizador) {
        if (procurarPorUsername(utilizador.getUsername()).isPresent()) {
            throw new DAOException("Já existe um utilizador com o username '" + utilizador.getUsername() + "'.");
        }
        utilizador.setId(proximoId++);
        utilizadores.put(utilizador.getId(), utilizador);
        return utilizador;
    }

    @Override
    public void atualizar(Utilizador utilizador) {
        if (!utilizadores.containsKey(utilizador.getId())) {
            throw new DAOException("O utilizador com id " + utilizador.getId() + " não existe.");
        }
        utilizadores.put(utilizador.getId(), utilizador);
    }

    @Override
    public void remover(int id) {
        if (utilizadores.remove(id) == null) {
            throw new DAOException("O utilizador com id " + id + " não existe.");
        }
    }

    @Override
    public Optional<Utilizador> procurarPorId(int id) {
        return Optional.ofNullable(utilizadores.get(id));
    }

    @Override
    public List<Utilizador> listarTodos() {
        return new ArrayList<>(utilizadores.values());
    }

    @Override
    public Optional<Utilizador> procurarPorUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return utilizadores.values().stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(username.trim()))
                .findFirst();
    }
}