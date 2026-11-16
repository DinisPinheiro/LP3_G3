package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.Model.Fornecedor;
import com.hotel4you.hotel4you.Utils.Utils;

/**
 * Validação dos dados de um fornecedor, partilhada pelos controllers que os recebem
 * (do formulário do gestor ou de um XML importado).
 */
final class ValidacaoFornecedor {

    private ValidacaoFornecedor() {
    }

    /**
     * Confirma que os dados cabem nas colunas de {@code dbo.Fornecedor} e que o código e o nome existem.
     *
     * @param identificador código do fornecedor
     * @param nome          nome
     * @param morada        morada (pode ser vazia)
     * @param codigoPostal  código postal (pode ser vazio)
     * @param cidade        cidade (pode ser vazia)
     * @throws IllegalArgumentException se faltar o código ou o nome, ou se algum texto for longo demais
     */
    static void validar(String identificador, String nome, String morada, String codigoPostal, String cidade) {
        if (!Utils.estaPreenchido(identificador)) {
            throw new IllegalArgumentException("Introduza o código do fornecedor.");
        }
        if (!Utils.estaPreenchido(nome)) {
            throw new IllegalArgumentException("Introduza o nome do fornecedor.");
        }
        limitar(identificador, Fornecedor.MAX_IDENTIFICADOR, "O código do fornecedor");
        limitar(nome, Fornecedor.MAX_NOME, "O nome do fornecedor");
        limitar(morada, Fornecedor.MAX_MORADA, "A morada");
        limitar(codigoPostal, Fornecedor.MAX_CODIGO_POSTAL, "O código postal");
        limitar(cidade, Fornecedor.MAX_CIDADE, "A cidade");
    }

    static void limitar(String texto, int maximo, String descricao) {
        if (texto != null && texto.trim().length() > maximo) {
            throw new IllegalArgumentException(descricao + " não pode ter mais de " + maximo + " caracteres.");
        }
    }
}
