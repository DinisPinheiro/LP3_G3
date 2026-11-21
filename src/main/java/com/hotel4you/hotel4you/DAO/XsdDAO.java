package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.Model.FicheiroXsd;

import java.util.Optional;

/**
 * Acesso aos XSD importados pelos fornecedores (S5-05, S6-02). Cada fornecedor tem o seu.
 */
public interface XsdDAO {

    /**
     * Guarda um XSD novo, que passa a ser o que está em vigor para o fornecedor a que pertence.
     *
     * @param xsd          ficheiro XSD já validado (traz o fornecedor)
     * @param idUtilizador id da conta que o importou
     * @return o mesmo ficheiro, já com id
     * @throws DAOException se não for possível guardar
     */
    FicheiroXsd guardar(FicheiroXsd xsd, int idUtilizador);

    /**
     * @param idFornecedor id do fornecedor
     * @return o XSD importado mais recentemente por esse fornecedor, ou vazio se ainda não importou nenhum
     *         (não há XSD geral nem XSD que venha com a aplicação)
     * @throws DAOException se não for possível ler
     */
    Optional<FicheiroXsd> obterAtual(int idFornecedor);
}
