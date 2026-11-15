package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.Model.Artigo;
import com.hotel4you.hotel4you.Model.Fornecedor;
import com.hotel4you.hotel4you.Model.MovimentoStock;

import java.util.List;

/**
 * Acesso ao stock do hotel: artigos, fornecedores e movimentos de stock.
 */
public interface StockDAO {

    /**
     * Regista as entradas de stock de um fornecedor, tudo ou nada: se alguma falhar, nenhuma fica guardada.
     * <p>
     * O fornecedor é guardado se ainda não existir (o identificador é único). Cada artigo é procurado
     * pelo nome e criado se não existir; a sua quantidade soma a da entrada e passa a ter este fornecedor.
     * Os identificadores gerados são atribuídos ao fornecedor e aos artigos recebidos.
     *
     * @param fornecedor fornecedor que entregou o material
     * @param entradas   movimentos do tipo {@code ENTRADA}, um por linha da encomenda
     * @throws DAOException se não for possível guardar
     */
    void registarEntrada(Fornecedor fornecedor, List<MovimentoStock> entradas);

    /**
     * @return todos os artigos com o stock atual, a quantidade usada em reservas não canceladas
     *         (para o stock disponível) e o fornecedor, ordenados por nome
     * @throws DAOException se não for possível ler
     */
    List<Artigo> listarArtigos();
}
