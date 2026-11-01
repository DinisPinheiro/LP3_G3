package com.hotel4you.hotel4you.Utils;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Validação e formatação de entradas partilhada por todos os formulários JavaFX.
 * <p>
 * Os ViewControllers leem o texto dos campos e usam estes métodos para o validar.
 * Se o valor for inválido, mostram uma mensagem de erro no ecrã e não avançam.
 * Os métodos não mostram nada nem leem do teclado, por isso são fáceis de testar.
 * <p>
 * Apoio de IA: Claude (Anthropic), revisto pela equipa.
 */
public final class Utils {

    /** Texto que, escrito num campo, cancela o formulário. */
    public static final String CANCELAR = "0";

    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final Pattern DECIMAL = Pattern.compile("-?\\d+(\\.\\d+)?");
    private static final Locale PORTUGAL = Locale.forLanguageTag("pt-PT");

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
     * Verifica se o utilizador pediu para cancelar o formulário (escreveu "0").
     *
     * @param texto texto do campo
     * @return {@code true} se o texto, sem espaços, for "0"
     */
    public static boolean eCancelamento(String texto) {
        return texto != null && CANCELAR.equals(texto.trim());
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
     * Converte um texto para número decimal, aceitando vírgula ou ponto (ex.: "60,50" ou "60.5").
     * Notação científica e separadores de milhares são rejeitados.
     *
     * @param texto texto a converter
     * @return o número, ou vazio se o texto for nulo ou não for um número
     */
    public static Optional<BigDecimal> converterDecimal(String texto) {
        if (texto == null) {
            return Optional.empty();
        }
        String normalizado = texto.trim().replace(',', '.');
        if (!DECIMAL.matcher(normalizado).matches()) {
            return Optional.empty();
        }
        return Optional.of(new BigDecimal(normalizado));
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

    /**
     * Formata um valor em euros no formato português (ex.: "80,00 €").
     *
     * @param valor valor a formatar
     * @return o valor formatado
     */
    public static String formatarPreco(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(PORTUGAL).format(valor);
    }
}
