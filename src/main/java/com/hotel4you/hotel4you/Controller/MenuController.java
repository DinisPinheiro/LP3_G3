package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.Model.OpcaoMenu;
import com.hotel4you.hotel4you.Model.enums.Perfil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Decide que opções do menu principal cada perfil pode ver e usar (RF-03).
 * <p>
 * Fica no Controller para que a View só mostre o que recebe, sem regras de permissões.
 */
public class MenuController {

    /**
     * Devolve as opções de menu de um perfil, pela ordem em que devem aparecer.
     *
     * @param perfil perfil do utilizador autenticado
     * @return lista de opções a que o perfil tem acesso (nunca nula)
     * @throws NullPointerException se o perfil for nulo
     */
    public List<OpcaoMenu> obterOpcoes(Perfil perfil) {
        Objects.requireNonNull(perfil, "O perfil não pode ser nulo");
        List<OpcaoMenu> opcoes = new ArrayList<>();
        for (OpcaoMenu opcao : OpcaoMenu.values()) {
            if (opcao.permite(perfil)) {
                opcoes.add(opcao);
            }
        }
        return opcoes;
    }

    /**
     * Confirma se um perfil pode usar uma opção (verificação extra antes de executar).
     *
     * @param perfil perfil do utilizador autenticado
     * @param opcao  opção que o utilizador quer usar
     * @return {@code true} se o perfil tem acesso
     */
    public boolean temAcesso(Perfil perfil, OpcaoMenu opcao) {
        Objects.requireNonNull(perfil, "O perfil não pode ser nulo");
        Objects.requireNonNull(opcao, "A opção não pode ser nula");
        return opcao.permite(perfil);
    }
}
