package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.Model.enums.Perfil;

import java.util.Arrays;

/**
 * Verificação de permissões por perfil (RF-03), partilhada pelos controllers.
 */
final class Permissoes {

    private Permissoes() {
    }

    /**
     * Indica se há um utilizador ativo com sessão iniciada e com um dos perfis indicados.
     *
     * @param sessao  sessão da aplicação
     * @param perfis  perfis a verificar
     * @return {@code true} se o utilizador atual é ativo e tem um desses perfis
     */
    static boolean temPerfil(Sessao sessao, Perfil... perfis) {
        return sessao.getUtilizadorAtual()
                .map(utilizador -> utilizador.isAtivo() && Arrays.asList(perfis).contains(utilizador.getPerfil()))
                .orElse(false);
    }

    /**
     * Confirma que há um utilizador ativo com sessão iniciada e com um dos perfis permitidos.
     *
     * @param sessao     sessão da aplicação
     * @param mensagem   mensagem do erro, caso o acesso seja recusado
     * @param permitidos perfis que podem aceder
     * @throws SecurityException se não houver sessão, o utilizador estiver desativado ou o perfil não for permitido
     */
    static void exigirPerfil(Sessao sessao, String mensagem, Perfil... permitidos) {
        if (!temPerfil(sessao, permitidos)) {
            throw new SecurityException(mensagem);
        }
    }
}
