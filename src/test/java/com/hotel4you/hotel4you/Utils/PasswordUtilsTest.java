package com.hotel4you.hotel4you.Utils;

import org.junit.jupiter.api.Test;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testes do hash das palavras-chave (PBKDF2 com salt): geração, verificação e hashes mal formados. */
class PasswordUtilsTest {

    @Test
    void hashTemOFormatoIteracoesSaltHashESemAPasswordEmClaro() {
        String hash = PasswordUtils.gerarHash("segredo123");

        String[] partes = hash.split(":");
        assertEquals(3, partes.length);
        assertTrue(Integer.parseInt(partes[0]) > 0, "o número de iterações vem no hash");
        assertEquals(16, Base64.getDecoder().decode(partes[1]).length, "salt de 16 bytes");
        assertEquals(32, Base64.getDecoder().decode(partes[2]).length, "hash de 256 bits");
        assertFalse(hash.contains("segredo123"));
    }

    @Test
    void mesmaPasswordGeraHashesDiferentesPorCausaDoSalt() {
        String primeiro = PasswordUtils.gerarHash("segredo123");
        String segundo = PasswordUtils.gerarHash("segredo123");

        assertNotEquals(primeiro, segundo);
        assertTrue(PasswordUtils.verificar("segredo123", primeiro));
        assertTrue(PasswordUtils.verificar("segredo123", segundo));
    }

    @Test
    void verificarAceitaAPasswordCertaERejeitaAErrada() {
        String hash = PasswordUtils.gerarHash("Segredo123");

        assertTrue(PasswordUtils.verificar("Segredo123", hash));
        assertFalse(PasswordUtils.verificar("segredo123", hash), "distingue maiúsculas de minúsculas");
        assertFalse(PasswordUtils.verificar("Segredo123 ", hash), "os espaços contam");
        assertFalse(PasswordUtils.verificar("", hash));
        assertFalse(PasswordUtils.verificar("outra", hash));
    }

    @Test
    void aceitaPasswordsComAcentosEEspacos() {
        String hash = PasswordUtils.gerarHash("pàssword com ç e espaços");

        assertTrue(PasswordUtils.verificar("pàssword com ç e espaços", hash));
        assertFalse(PasswordUtils.verificar("password com c e espacos", hash));
    }

    @Test
    void gerarHashRejeitaPasswordNulaOuVazia() {
        assertThrows(IllegalArgumentException.class, () -> PasswordUtils.gerarHash(null));
        assertThrows(IllegalArgumentException.class, () -> PasswordUtils.gerarHash(""));
    }

    @Test
    void verificarComArgumentosNulosDevolveFalso() {
        String hash = PasswordUtils.gerarHash("segredo123");

        assertFalse(PasswordUtils.verificar(null, hash));
        assertFalse(PasswordUtils.verificar("segredo123", null));
        assertFalse(PasswordUtils.verificar(null, null));
    }

    @Test
    void hashMalFormadoDevolveFalsoEmVezDeLancarErro() {
        assertFalse(PasswordUtils.verificar("segredo123", ""));
        assertFalse(PasswordUtils.verificar("segredo123", "segredo123"), "password em claro não é um hash");
        assertFalse(PasswordUtils.verificar("segredo123", "120000:abc"), "faltam partes");
        assertFalse(PasswordUtils.verificar("segredo123", "120000:abc:def:ghi"), "partes a mais");
        assertFalse(PasswordUtils.verificar("segredo123", "muitas:AAAA:AAAA"), "iterações não numéricas");
        assertFalse(PasswordUtils.verificar("segredo123", "120000:!!!:!!!"), "Base64 inválido");
        assertFalse(PasswordUtils.verificar("segredo123", "0:AAAAAAAAAAAAAAAAAAAAAA==:AAAAAAAAAAAAAAAAAAAAAA=="),
                "zero iterações");
    }

    @Test
    void hashAdulteradoDeixaDeCorresponder() {
        String[] partes = PasswordUtils.gerarHash("segredo123").split(":");
        byte[] adulterado = Base64.getDecoder().decode(partes[2]);
        adulterado[0] ^= 0x01;
        String hashAdulterado = partes[0] + ":" + partes[1] + ":" + Base64.getEncoder().encodeToString(adulterado);

        assertFalse(PasswordUtils.verificar("segredo123", hashAdulterado));
    }

    @Test
    void verificarUsaAsIteracoesGuardadasNoHash() throws Exception {
        // Um hash antigo, feito com menos iterações, continua a ser reconhecido.
        byte[] salt = new byte[16];
        for (int i = 0; i < salt.length; i++) {
            salt[i] = (byte) i;
        }
        int iteracoes = 1_000;
        byte[] derivado = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(new PBEKeySpec("antiga".toCharArray(), salt, iteracoes, 256)).getEncoded();
        Base64.Encoder b64 = Base64.getEncoder();
        String hashAntigo = iteracoes + ":" + b64.encodeToString(salt) + ":" + b64.encodeToString(derivado);

        assertTrue(PasswordUtils.verificar("antiga", hashAntigo));
        assertFalse(PasswordUtils.verificar("nova", hashAntigo));
    }
}