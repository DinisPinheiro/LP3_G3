package com.hotel4you.hotel4you.Utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Optional;

/**
 * Validação de entradas partilhada por todos os formulários JavaFX.
 * <p>
 * Os ViewControllers leem o texto dos campos e usam estes métodos para o validar.
 * Se o valor for inválido, mostram uma mensagem de erro no ecrã e não avançam.
 * Os métodos não mostram nada nem leem do teclado, por isso são fáceis de testar.
 */
public final class Utils {

    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private Utils() {
    }

    /**
     * Verifica se um campo de texto foi preenchido.
     *
     * @param texto texto do campo
     * @return {@code true} se o texto não for nulo, vazio nem só espaços
     */
    public static boolean estaPreenchido(String texto) {
        return texto != null && !texto.isBlank();
    }

    /**
     * Converte um texto para inteiro.
     *
     * @param texto texto a converter (espaços no início e no fim são ignorados)
     * @return o inteiro, ou vazio se o texto for nulo ou não for um inteiro
     */
    public static Optional<Integer> converterInteiro(String texto) {
        if (texto == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(texto.trim()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Converte um texto para inteiro, aceitando só valores dentro de um intervalo.
     *
     * @param texto texto a converter
     * @param min   valor mínimo aceite (inclusivo)
     * @param max   valor máximo aceite (inclusivo)
     * @return o inteiro, ou vazio se o texto for inválido ou estiver fora do intervalo
     */
    public static Optional<Integer> converterInteiro(String texto, int min, int max) {
        return converterInteiro(texto).filter(valor -> valor >= min && valor <= max);
    }

    /**
     * Converte um texto no formato dd/mm/aaaa para data. Datas impossíveis
     * (ex.: 31/02/2026) são rejeitadas.
     *
     * @param texto texto a converter
     * @return a data, ou vazio se o texto for nulo ou não for uma data válida
     */
    public static Optional<LocalDate> converterData(String texto) {
        if (texto == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(texto.trim(), FORMATO_DATA));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }
}
