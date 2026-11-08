package com.hotel4you.hotel4you.Model.encomenda;

/**
 * Dados do fornecedor que vêm no cabeçalho da encomenda (elemento {@code SupplierParty}).
 *
 * @param identificador identificador do fornecedor, por exemplo {@code "11PT920"}
 * @param nome          nome do fornecedor
 * @param morada1       primeira linha da morada
 * @param morada2       segunda linha da morada
 * @param cidade        cidade
 * @param codigoPostal  código postal
 * @param codigoPais    código ISO do país, por exemplo {@code "PT"}
 * @param pais          nome do país
 */
public record FornecedorEncomenda(String identificador, String nome, String morada1, String morada2,
                                  String cidade, String codigoPostal, String codigoPais, String pais) {
}
