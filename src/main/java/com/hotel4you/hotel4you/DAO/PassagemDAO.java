package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.Model.Passagem;
import java.util.List;

public interface PassagemDAO {

    void inserir(Passagem passagem);

    List<Passagem> listarPorCartao(int numeroCartao);

    List<Passagem> listarPorQuarto(int numeroQuarto);
}
