package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.Model.enums.Perfil;

import java.util.Arrays;

/**
 * Verificação de permissões por perfil (RF-03), partilhada pelos controllers.
 * <p>
 * Apoio de IA: Claude (Anthropic), revisto pela equipa.
 */
final class Permissoes {

    private Permissoes() {
    }

    /**
     * Confirma que há um utilizador ativo com sessão iniciada e com um dos perfis permitidos.
     *
     * @param sessao    sessão da aplicação
     * @param mensagem  mensagem do erro, caso o acesso seja recusado
     * @param permitidos perfis que podem aceder
     * @throws SecurityException se não houver sessão, o utilizador estiver desativado ou o perfil não for permitido
     */
    static void exigirPerfil(Sessao sessao, String mensagem, Perfil... permitidos) {
        boolean permitido = sessao.getUtilizadorAtual()
                .map(utilizador -> utilizador.isAtivo() && Arrays.asList(permitidos).contains(utilizador.getPerfil()))
                .orElse(false);
        if (!permitido) {
            throw new SecurityException(mensagem);
        }
    }
}
