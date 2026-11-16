package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.Model.FicheiroXsd;

import java.util.Optional;

/**
 * Acesso ao XSD importado pelo gestor (S5-05).
 */
public interface XsdDAO {

    /**
     * Guarda um XSD novo, que passa a ser o que está em vigor.
     *
     * @param xsd        ficheiro XSD já validado
     * @param idDoGestor id do utilizador que o importou
     * @return o mesmo ficheiro, já com id
     * @throws DAOException se não for possível guardar
     */
    FicheiroXsd guardar(FicheiroXsd xsd, int idDoGestor);

    /**
     * @return o XSD importado mais recentemente, ou vazio se o gestor ainda não importou nenhum
     *         (nesse caso usa-se o XSD que vem com a aplicação)
     * @throws DAOException se não for possível ler
     */
    Optional<FicheiroXsd> obterAtual();
}
