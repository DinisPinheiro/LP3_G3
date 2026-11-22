package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.Model.LocalHotel;
import com.hotel4you.hotel4you.Model.Passagem;

import java.util.List;
import java.util.Optional;

/**
 * Acesso aos locais do hotel (tabela {@code dbo.LocalHotel}) e às passagens de cartões
 * (tabela {@code dbo.Passagem}).
 */
public interface PassagemDAO {

    /**
     * Guarda uma passagem.
     *
     * @param passagem passagem a guardar (o local tem de existir em {@code dbo.LocalHotel})
     * @throws DAOException se não for possível guardar
     */
    void inserir(Passagem passagem);

    /**
     * @return todos os locais do hotel, pela ordem em que foram criados
     * @throws DAOException se não for possível ler
     */
    List<LocalHotel> listarLocais();

    /**
     * @param codigo código do local (ex.: {@code RESTAURANTE})
     * @return o local, ou vazio se não existir
     * @throws DAOException se não for possível ler
     */
    Optional<LocalHotel> procurarLocal(String codigo);

    /**
     * @param numeroCartao número do cartão
     * @return passagens do cartão, da mais recente para a mais antiga
     * @throws DAOException se não for possível ler
     */
    List<Passagem> listarPorCartao(int numeroCartao);

    /**
     * @param numeroQuarto número do quarto (ex.: 202)
     * @return passagens de cartões nesse quarto, da mais recente para a mais antiga
     * @throws DAOException se não for possível ler
     */
    List<Passagem> listarPorQuarto(int numeroQuarto);

    /**
     * @param codigoLocal código do local (ex.: {@code PISCINA})
     * @return passagens de cartões nesse local, da mais recente para a mais antiga
     * @throws DAOException se não for possível ler
     */
    List<Passagem> listarPorLocal(String codigoLocal);
}