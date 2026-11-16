package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.Model.Artigo;
import com.hotel4you.hotel4you.Model.Fornecedor;
import com.hotel4you.hotel4you.Model.MovimentoStock;
import com.hotel4you.hotel4you.Model.encomenda.EncomendaFornecedor;

import java.util.List;

/**
 * Acesso ao stock do hotel: artigos, fornecedores, encomendas e movimentos de stock.
 */
public interface StockDAO {

    /**
     * Regista as entradas de stock de um fornecedor sem guardar a encomenda (sem preços nem totais).
     *
     * @param fornecedor fornecedor que entregou o material
     * @param entradas   movimentos do tipo {@code ENTRADA}
     * @throws DAOException se não for possível guardar
     * @see #registarEntrada(Fornecedor, EncomendaFornecedor, List)
     */
    default void registarEntrada(Fornecedor fornecedor, List<MovimentoStock> entradas) {
        registarEntrada(fornecedor, null, entradas);
    }

    /**
     * Regista as entradas de stock de uma encomenda de um fornecedor, tudo ou nada: se alguma falhar,
     * nenhuma fica guardada.
     * <p>
     * O fornecedor é guardado se ainda não existir (o identificador é único). Cada artigo é procurado
     * pelo nome e criado se não existir; a sua quantidade soma a da entrada, passa a ter este fornecedor
     * e fica com o preço unitário e a taxa desta linha. A encomenda é guardada com os totais
     * ({@link EncomendaFornecedor#totais()}) e cada linha com produto, quantidade, unidade (caixas e
     * unidades), preço unitário, taxa e total da linha. Os identificadores gerados são atribuídos ao
     * fornecedor e aos artigos recebidos.
     *
     * @param fornecedor fornecedor que entregou o material
     * @param encomenda  encomenda de onde vêm as entradas; {@code null} para entradas sem encomenda
     * @param entradas   movimentos do tipo {@code ENTRADA}, um por linha da encomenda e pela mesma ordem
     * @throws DAOException             se não for possível guardar
     * @throws IllegalArgumentException se houver encomenda e o número de entradas não for o das linhas
     */
    void registarEntrada(Fornecedor fornecedor, EncomendaFornecedor encomenda, List<MovimentoStock> entradas);

    /**
     * @return todos os artigos com o stock atual, a quantidade usada em reservas não canceladas
     *         (para o stock disponível), o fornecedor e o último preço e taxa, ordenados por nome
     * @throws DAOException se não for possível ler
     */
    List<Artigo> listarArtigos();
}
