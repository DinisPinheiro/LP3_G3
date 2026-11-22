package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.Model.LocalHotel;
import com.hotel4you.hotel4you.Model.Passagem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * {@link PassagemDAO} em memória, <b>só para testes unitários</b>. Já traz os 8 locais do hotel
 * (os mesmos do script {@code EsquemaDB_S6_picagem_completa.sql}). As consultas devolvem
 * a passagem mais recente primeiro.
 */
public class PassagemDAOMemoria implements PassagemDAO {

    private final List<LocalHotel> locais = locaisPadrao();
    private final List<Passagem> passagens = new ArrayList<>();

    /** @return os 8 locais do hotel, como no script SQL */
    public static List<LocalHotel> locaisPadrao() {
        return List.of(
                new LocalHotel(1, LocalHotel.QUARTO, "Quarto", null),
                new LocalHotel(2, LocalHotel.RESTAURANTE, "Restaurante", 1),
                new LocalHotel(3, LocalHotel.PISCINA, "Piscina", 1),
                new LocalHotel(4, LocalHotel.COZINHA, "Cozinha", 1),
                new LocalHotel(5, LocalHotel.RECECAO, "Receção", 0),
                new LocalHotel(6, LocalHotel.BAR, "Bar", 0),
                new LocalHotel(7, LocalHotel.CONFERENCIAS, "Sala de conferências", 0),
                new LocalHotel(8, LocalHotel.ARMAZEM, "Armazém", -1));
    }

    /**
     * @param codigo código do local
     * @return o local do catálogo padrão
     */
    public static LocalHotel local(String codigo) {
        return locaisPadrao().stream().filter(l -> l.getCodigo().equals(codigo)).findFirst().orElseThrow();
    }

    @Override
    public void inserir(Passagem passagem) {
        if (procurarLocal(passagem.getLocal()).isEmpty()) {
            throw new DAOException("O local " + passagem.getLocal() + " não existe.");
        }
        passagem.setId(passagens.size() + 1);
        passagens.add(passagem);
    }

    @Override
    public List<LocalHotel> listarLocais() {
        return new ArrayList<>(locais);
    }

    @Override
    public Optional<LocalHotel> procurarLocal(String codigo) {
        return locais.stream().filter(l -> l.getCodigo().equals(codigo)).findFirst();
    }

    @Override
    public List<Passagem> listarPorCartao(int numeroCartao) {
        return filtrar(p -> p.getNumeroCartao() == numeroCartao);
    }

    @Override
    public List<Passagem> listarPorQuarto(int numeroQuarto) {
        return filtrar(p -> p.getNumeroQuarto() != null && p.getNumeroQuarto() == numeroQuarto);
    }

    @Override
    public List<Passagem> listarPorLocal(String codigoLocal) {
        return filtrar(p -> p.getLocal().equals(codigoLocal));
    }

    /** @return todas as passagens guardadas, pela ordem em que foram registadas */
    public List<Passagem> guardadas() {
        return new ArrayList<>(passagens);
    }

    private List<Passagem> filtrar(java.util.function.Predicate<Passagem> condicao) {
        List<Passagem> resultado = new ArrayList<>(passagens.stream().filter(condicao).toList());
        Collections.reverse(resultado);
        return resultado;
    }
}