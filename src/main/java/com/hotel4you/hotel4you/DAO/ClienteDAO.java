package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.Model.Cliente;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Acesso aos clientes guardados. O documento de identificação do cliente ({@link Cliente#getDocumento()})
 * é o NIF e é único: não podem existir dois clientes com o mesmo.
 */
public interface ClienteDAO extends DAO<Cliente> {

    /**
     * {@inheritDoc}
     *
     * @throws DAOException se já existir um cliente com o mesmo NIF/documento, ou se não for possível guardar
     */
    @Override
    Cliente inserir(Cliente cliente);

    /**
     * @param documento NIF/documento de identificação
     * @return o cliente, ou vazio se não existir
     * @throws DAOException se não for possível ler
     */
    Optional<Cliente> procurarPorDocumento(String documento);

    /**
     * Uma página de clientes por ordem alfabética do nome, só os que correspondem à pesquisa (parte do
     * nome ou do NIF/documento, sem distinguir maiúsculas). A implementação por omissão filtra em
     * memória; os DAO sobre bases de dados devem fazer a paginação na própria consulta.
     *
     * @param pesquisa texto a procurar; vazio ou nulo devolve todos
     * @param pagina   número da página, a começar em 0
     * @param tamanho  máximo de clientes por página (pelo menos 1)
     * @return os clientes da página (vazia se a página não existir)
     * @throws IllegalArgumentException se a página for negativa ou o tamanho for inferior a 1
     * @throws DAOException             se não for possível ler
     */
    default List<Cliente> listarPagina(String pesquisa, int pagina, int tamanho) {
        if (pagina < 0 || tamanho < 1) {
            throw new IllegalArgumentException("Página inválida.");
        }
        return listarTodos().stream()
                .filter(cliente -> corresponde(cliente, pesquisa))
                .sorted(Comparator.comparing(Cliente::getNome, String.CASE_INSENSITIVE_ORDER)
                        .thenComparingInt(Cliente::getId))
                .skip((long) pagina * tamanho)
                .limit(tamanho)
                .toList();
    }

    /**
     * @param pesquisa texto a procurar (como em {@link #listarPagina}); vazio ou nulo conta todos
     * @return quantos clientes correspondem à pesquisa
     * @throws DAOException se não for possível ler
     */
    default int contar(String pesquisa) {
        return (int) listarTodos().stream().filter(cliente -> corresponde(cliente, pesquisa)).count();
    }

    private static boolean corresponde(Cliente cliente, String pesquisa) {
        if (pesquisa == null || pesquisa.isBlank()) {
            return true;
        }
        String nome = pesquisa.trim().toLowerCase(Locale.ROOT);
        String documento = pesquisa.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        return cliente.getNome().toLowerCase(Locale.ROOT).contains(nome)
                || cliente.getDocumento().toLowerCase(Locale.ROOT).contains(documento);
    }
}
