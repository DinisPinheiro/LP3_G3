package com.hotel4you.hotel4you.Model;

import com.hotel4you.hotel4you.Utils.Constantes;

import java.util.Locale;
import java.util.Objects;

/**
 * Local do hotel onde um cartão pode ser picado (tabela {@code dbo.LocalHotel}).
 * <p>
 * Cada local tem um código estável (ex.: {@code RESTAURANTE}) e fica num piso do hotel
 * ({@link Constantes}). O local {@link #QUARTO} é especial: representa qualquer quarto, por isso não
 * tem piso próprio; o piso é o do quarto escolhido na picagem.
 */
public class LocalHotel {

    /** Qualquer quarto de dormidas (pisos 2 e 3); exige o número do quarto na picagem. */
    public static final String QUARTO = "QUARTO";
    public static final String RESTAURANTE = "RESTAURANTE";
    public static final String PISCINA = "PISCINA";
    public static final String COZINHA = "COZINHA";
    public static final String RECECAO = "RECECAO";
    public static final String BAR = "BAR";
    public static final String CONFERENCIAS = "CONFERENCIAS";
    public static final String ARMAZEM = "ARMAZEM";

    private final int id;
    private final String codigo;
    private final String nome;
    private final Integer piso;

    /**
     * @param id     identificador (0 se ainda não foi guardado)
     * @param codigo código do local, em maiúsculas (ex.: {@code PISCINA})
     * @param nome   nome para mostrar ao utilizador (ex.: "Piscina")
     * @param piso   piso do local; tem de ser {@code null} no {@link #QUARTO} e um piso válido nos outros
     * @throws IllegalArgumentException se faltar o código ou o nome, ou se o piso não for coerente com o local
     */
    public LocalHotel(int id, String codigo, String nome, Integer piso) {
        this.id = id;
        this.codigo = exigirTexto(codigo, "código").toUpperCase(Locale.ROOT);
        this.nome = exigirTexto(nome, "nome");
        if (QUARTO.equals(this.codigo)) {
            this.piso = null;
        } else {
            if (piso == null || !Constantes.pisoValido(piso)) {
                throw new IllegalArgumentException("O local '" + this.codigo + "' precisa de um piso válido.");
            }
            this.piso = piso;
        }
    }

    /** @return {@code true} se o local é um quarto (o piso depende do quarto escolhido) */
    public boolean isQuarto() {
        return QUARTO.equals(codigo);
    }

    public int getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }

    /** @return o piso do local, ou {@code null} se for o {@link #QUARTO} */
    public Integer getPiso() {
        return piso;
    }

    @Override
    public boolean equals(Object outro) {
        return outro instanceof LocalHotel local && codigo.equals(local.codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigo);
    }

    /** Mostra o nome, que é o que aparece nas listas de escolha. */
    @Override
    public String toString() {
        return nome;
    }

    private static String exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("O campo '" + campo + "' é obrigatório.");
        }
        return valor.trim();
    }
}