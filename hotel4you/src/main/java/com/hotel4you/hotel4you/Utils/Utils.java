package com.hotel4you.hotel4you.Utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Optional;
import java.util.Scanner;

/**
 * Leitura e validação de entradas na consola, partilhada por todas as Views.
 * <p>
 * Regras comuns:
 * <ul>
 *   <li>entradas inválidas mostram uma mensagem de erro e voltam a pedir o valor;</li>
 *   <li>nos formulários, o input "0" cancela tudo (lança {@link OperacaoCanceladaException});</li>
 *   <li>nos menus, o "0" é uma opção normal (ex.: terminar sessão), por isso
 *       {@link #lerInteiro(String, int, int)} não o trata como cancelamento.</li>
 * </ul>
 */
public final class Utils {

    /** Texto que cancela um formulário. */
    public static final String CANCELAR = "0";

    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private static Scanner scanner = new Scanner(System.in);

    private Utils() {
    }

    /**
     * Substitui a fonte de leitura (usado nos testes unitários para simular o teclado).
     *
     * @param novoScanner scanner a usar nas próximas leituras
     */
    public static void setScanner(Scanner novoScanner) {
        scanner = novoScanner;
    }

    /**
     * Lê um texto não vazio. O texto devolvido não tem espaços no início nem no fim.
     *
     * @param mensagem texto mostrado ao utilizador (ex.: "Nome: ")
     * @return o texto introduzido
     * @throws OperacaoCanceladaException se o utilizador escrever "0"
     */
    public static String lerCampo(String mensagem) {
        while (true) {
            String texto = lerLinha(mensagem);
            if (texto.equals(CANCELAR)) {
                throw new OperacaoCanceladaException();
            }
            if (!texto.isEmpty()) {
                return texto;
            }
            System.out.println("Erro: o campo não pode estar vazio.");
        }
    }

    /**
     * Lê um número inteiro dentro de um intervalo. O "0" NÃO cancela: é devolvido
     * se estiver dentro do intervalo (útil para menus).
     *
     * @param mensagem texto mostrado ao utilizador
     * @param min      valor mínimo aceite (inclusivo)
     * @param max      valor máximo aceite (inclusivo)
     * @return o inteiro introduzido
     */
    public static int lerInteiro(String mensagem, int min, int max) {
        return lerInteiro(mensagem, min, max, false);
    }

    /**
     * Lê um número inteiro dentro de um intervalo, para usar em formulários.
     * O "0" cancela o formulário, por isso {@code min} deve ser pelo menos 1.
     *
     * @param mensagem texto mostrado ao utilizador
     * @param min      valor mínimo aceite (inclusivo)
     * @param max      valor máximo aceite (inclusivo)
     * @return o inteiro introduzido
     * @throws OperacaoCanceladaException se o utilizador escrever "0"
     */
    public static int lerInteiroOuCancelar(String mensagem, int min, int max) {
        return lerInteiro(mensagem, min, max, true);
    }

    /**
     * Lê uma data no formato dd/mm/aaaa. Datas impossíveis (ex.: 31/02/2026) são rejeitadas.
     *
     * @param mensagem texto mostrado ao utilizador (o formato é acrescentado automaticamente)
     * @return a data introduzida
     * @throws OperacaoCanceladaException se o utilizador escrever "0"
     */
    public static LocalDate lerData(String mensagem) {
        while (true) {
            String texto = lerLinha(mensagem + " (dd/mm/aaaa): ");
            if (texto.equals(CANCELAR)) {
                throw new OperacaoCanceladaException();
            }
            Optional<LocalDate> data = converterData(texto);
            if (data.isPresent()) {
                return data.get();
            }
            System.out.println("Erro: data inválida. Use o formato dd/mm/aaaa.");
        }
    }

    /**
     * Converte um texto para inteiro, sem interagir com o utilizador.
     *
     * @param texto texto a converter
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
     * Converte um texto dd/mm/aaaa para data, sem interagir com o utilizador.
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

    private static int lerInteiro(String mensagem, int min, int max, boolean zeroCancela) {
        while (true) {
            String texto = lerLinha(mensagem);
            if (zeroCancela && texto.equals(CANCELAR)) {
                throw new OperacaoCanceladaException();
            }
            Optional<Integer> valor = converterInteiro(texto);
            if (valor.isPresent() && valor.get() >= min && valor.get() <= max) {
                return valor.get();
            }
            System.out.println("Erro: introduza um número inteiro entre " + min + " e " + max + ".");
        }
    }

    private static String lerLinha(String mensagem) {
        System.out.print(mensagem);
        return scanner.nextLine().trim();
    }
}
