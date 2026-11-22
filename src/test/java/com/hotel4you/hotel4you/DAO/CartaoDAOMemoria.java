package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.Model.CartaoMagnetico;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@link CartaoDAO} em memória, <b>só para testes unitários</b>. O número do cartão é único.
 */
public class CartaoDAOMemoria implements CartaoDAO {

    private final Map<Integer, CartaoMagnetico> cartoes = new LinkedHashMap<>();
    private int proximoId = 1;

    @Override
    public CartaoMagnetico inserir(CartaoMagnetico cartao) {
        if (procurarPorNumero(cartao.getNumero()).isPresent()) {
            throw new DAOException("Já existe um cartão com o número " + cartao.getNumero() + ".");
        }
        cartao.setId(proximoId++);
        cartoes.put(cartao.getId(), cartao);
        return cartao;
    }

    @Override
    public void atualizar(CartaoMagnetico cartao) {
        if (!cartoes.containsKey(cartao.getId())) {
            throw new DAOException("O cartão com id " + cartao.getId() + " não existe.");
        }
        cartoes.put(cartao.getId(), cartao);
    }

    @Override
    public void remover(int id) {
        if (cartoes.remove(id) == null) {
            throw new DAOException("O cartão com id " + id + " não existe.");
        }
    }

    @Override
    public Optional<CartaoMagnetico> procurarPorId(int id) {
        return Optional.ofNullable(cartoes.get(id));
    }

    @Override
    public Optional<CartaoMagnetico> procurarPorNumero(int numero) {
        return cartoes.values().stream().filter(c -> c.getNumero() == numero).findFirst();
    }

    @Override
    public List<CartaoMagnetico> listarTodos() {
        return new ArrayList<>(cartoes.values());
    }

    @Override
    public int proximoNumero() {
        int maior = cartoes.values().stream().mapToInt(CartaoMagnetico::getNumero).max().orElse(0);
        return Math.max(maior + 1, PRIMEIRO_NUMERO_CLIENTE);
    }
}