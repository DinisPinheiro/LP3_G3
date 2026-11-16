package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.Model.ImportacaoFornecedor;
import com.hotel4you.hotel4you.Model.enums.EstadoImportacao;

import java.util.List;
import java.util.Optional;

/**
 * Acesso aos ficheiros XML enviados pelos fornecedores e ao seu estado (S5-03).
 */
public interface ImportacaoDAO {

    /**
     * Guarda uma importação nova e atribui-lhe o identificador.
     *
     * @param importacao importação ainda sem id
     * @return a mesma importação, já com id
     * @throws DAOException se não for possível guardar
     */
    ImportacaoFornecedor inserir(ImportacaoFornecedor importacao);

    /**
     * @param id identificador da importação
     * @return a importação (com o conteúdo do XML), ou vazia se não existir
     * @throws DAOException se não for possível ler
     */
    Optional<ImportacaoFornecedor> procurarPorId(int id);

    /**
     * @param estado estado a filtrar, ou {@code null} para todos
     * @return as importações desse estado, da mais recente para a mais antiga
     * @throws DAOException se não for possível ler
     */
    List<ImportacaoFornecedor> listar(EstadoImportacao estado);

    /**
     * @param idFornecedor id do fornecedor
     * @return as importações desse fornecedor, da mais recente para a mais antiga
     * @throws DAOException se não for possível ler
     */
    List<ImportacaoFornecedor> listarDoFornecedor(int idFornecedor);

    /**
     * Guarda a decisão (aprovar ou rejeitar) de uma importação, mas só se na base de dados ela ainda
     * estiver pendente. Assim, dois utilizadores a decidir o mesmo ficheiro ao mesmo tempo não o
     * decidem duas vezes.
     *
     * @param importacao importação já aprovada ou rejeitada no modelo
     * @throws DAOException se a importação não existir, já tiver sido decidida ou não for possível guardar
     */
    void guardarDecisao(ImportacaoFornecedor importacao);
}
