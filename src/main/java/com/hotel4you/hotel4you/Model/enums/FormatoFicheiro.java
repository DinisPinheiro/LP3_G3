package com.hotel4you.hotel4you.Model.enums;

import java.util.Locale;

/**
 * Formato de um ficheiro de encomenda enviado por um fornecedor (S7-03).
 * <p>
 * Os fornecedores enviam o XML (enunciado v1.0) ou, se não o conseguirem exportar, o JSON
 * (enunciado v1.1), que segue as mesmas regras. O formato fica guardado com cada importação para o
 * histórico e para a aprovação saber como ler o ficheiro.
 */
public enum FormatoFicheiro {
    XML("XML", ".xml"),
    JSON("JSON", ".json");

    private final String nome;
    private final String extensao;

    FormatoFicheiro(String nome, String extensao) {
        this.nome = nome;
        this.extensao = extensao;
    }

    /** @return o nome do formato, pronto para mostrar ao utilizador (por exemplo {@code "JSON"}) */
    public String getNome() {
        return nome;
    }

    /** @return a extensão dos ficheiros deste formato, com o ponto (por exemplo {@code ".json"}) */
    public String getExtensao() {
        return extensao;
    }

    /**
     * Descobre o formato pela extensão do nome do ficheiro, sem distinguir maiúsculas de minúsculas.
     *
     * @param nomeFicheiro nome do ficheiro (por exemplo {@code "entrega.JSON"})
     * @return o formato correspondente à extensão
     * @throws IllegalArgumentException se o nome não terminar em {@code .xml} nem em {@code .json}
     */
    public static FormatoFicheiro deNomeFicheiro(String nomeFicheiro) {
        String minusculas = nomeFicheiro == null ? "" : nomeFicheiro.trim().toLowerCase(Locale.ROOT);
        for (FormatoFicheiro formato : values()) {
            if (minusculas.endsWith(formato.extensao)) {
                return formato;
            }
        }
        throw new IllegalArgumentException("O ficheiro '" + nomeFicheiro + "' não é XML nem JSON. "
                + "Escolha um ficheiro .xml ou .json.");
    }

    @Override
    public String toString() {
        return nome;
    }
}
