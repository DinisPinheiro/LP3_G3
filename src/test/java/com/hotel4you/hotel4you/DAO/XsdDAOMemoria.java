package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.Model.FicheiroXsd;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@link XsdDAO} em memória, <b>só para testes unitários</b>. O XSD em vigor de um fornecedor é o último
 * que esse fornecedor guardou.
 */
public class XsdDAOMemoria implements XsdDAO {

    private final List<FicheiroXsd> ficheiros = new ArrayList<>();

    @Override
    public FicheiroXsd guardar(FicheiroXsd xsd, int idUtilizador) {
        xsd.setId(ficheiros.size() + 1);
        ficheiros.add(xsd);
        return xsd;
    }

    @Override
    public Optional<FicheiroXsd> obterAtual(int idFornecedor) {
        FicheiroXsd atual = null;
        for (FicheiroXsd xsd : ficheiros) {
            if (xsd.getIdFornecedor() == idFornecedor) {
                atual = xsd;
            }
        }
        return Optional.ofNullable(atual);
    }

    /** @return quantos XSD foram guardados, de todos os fornecedores (para os testes confirmarem) */
    public int total() {
        return ficheiros.size();
    }
}
