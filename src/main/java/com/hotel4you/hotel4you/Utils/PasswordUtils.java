package com.hotel4you.hotel4you.Utils;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Geração e verificação de hashes de palavras-chave (requisito não funcional de segurança).
 * <p>
 * Usa PBKDF2 com HMAC-SHA256 e salt aleatório, disponível no próprio JDK
 * (não é necessária nenhuma biblioteca externa). O texto guardado tem o formato
 * {@code iteracoes:salt:hash}, com salt e hash em Base64.
 */
public final class PasswordUtils {

    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final int ITERACOES = 120_000;
    private static final int BYTES_SALT = 16;
    private static final int BITS_HASH = 256;
    private static final String SEPARADOR = ":";

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtils() {
    }

    /**
     * Gera o hash de uma palavra-chave, com um salt novo.
     *
     * @param password palavra-chave em claro
     * @return texto a guardar na base de dados
     * @throws IllegalArgumentException se a palavra-chave for nula ou vazia
     */
    public static String gerarHash(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("A palavra-chave não pode ser vazia.");
        }
        byte[] salt = new byte[BYTES_SALT];
        RANDOM.nextBytes(salt);
        byte[] hash = derivar(password, salt, ITERACOES);
        Base64.Encoder b64 = Base64.getEncoder();
        return ITERACOES + SEPARADOR + b64.encodeToString(salt) + SEPARADOR + b64.encodeToString(hash);
    }

    /**
     * Verifica se uma palavra-chave corresponde a um hash guardado.
     * Nunca compara a palavra-chave em claro.
     *
     * @param password     palavra-chave introduzida
     * @param hashGuardado texto devolvido por {@link #gerarHash(String)}
     * @return {@code true} se corresponder; {@code false} também se o hash estiver mal formado
     */
    public static boolean verificar(String password, String hashGuardado) {
        if (password == null || hashGuardado == null) {
            return false;
        }
        String[] partes = hashGuardado.split(SEPARADOR);
        if (partes.length != 3) {
            return false;
        }
        try {
            int iteracoes = Integer.parseInt(partes[0]);
            byte[] salt = Base64.getDecoder().decode(partes[1]);
            byte[] esperado = Base64.getDecoder().decode(partes[2]);
            byte[] obtido = derivar(password, salt, iteracoes);
            return MessageDigest.isEqual(esperado, obtido);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] derivar(String password, byte[] salt, int iteracoes) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iteracoes, BITS_HASH);
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Algoritmo de hash indisponível: " + ALGORITMO, e);
        }
    }
}
