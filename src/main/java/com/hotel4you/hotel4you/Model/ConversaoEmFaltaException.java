package com.hotel4you.hotel4you.Model;

/**
 * Uma linha da encomenda vem em caixas, o ficheiro não diz quantas unidades tem cada caixa e o produto
 * ainda não tem a conversão definida (S7-02). O gestor ou o funcionário tem de a definir para a
 * entrada em stock poder ser feita.
 */
public class ConversaoEmFaltaException extends IllegalArgumentException {

    private final int numeroLinha;
    private final String produto;

    /**
     * @param numeroLinha número da linha da encomenda
     * @param produto     descrição do produto
     */
    public ConversaoEmFaltaException(int numeroLinha, String produto) {
        super("Linha " + numeroLinha + ": '" + produto + "' vem em caixas e o ficheiro não indica quantas "
                + "unidades tem cada caixa. Defina a conversão (1 caixa = quantas unidades).");
        this.numeroLinha = numeroLinha;
        this.produto = produto;
    }

    /** @return número da linha da encomenda */
    public int getNumeroLinha() {
        return numeroLinha;
    }

    /** @return descrição do produto sem conversão */
    public String getProduto() {
        return produto;
    }
}
