package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.Model.FicheiroXsd;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@link XsdDAO} em memória, <b>só para testes unitários</b>. O XSD em vigor é o último guardado.
 */
public class XsdDAOMemoria implements XsdDAO {

    private final List<FicheiroXsd> ficheiros = new ArrayList<>();

    @Override
    public FicheiroXsd guardar(FicheiroXsd xsd, int idDoGestor) {
        xsd.setId(ficheiros.size() + 1);
        ficheiros.add(xsd);
        return xsd;
    }

    @Override
    public Optional<FicheiroXsd> obterAtual() {
        return ficheiros.isEmpty() ? Optional.empty() : Optional.of(ficheiros.get(ficheiros.size() - 1));
    }

    /** @return quantos XSD foram guardados (para os testes confirmarem) */
    public int total() {
        return ficheiros.size();
    }
}
