package com.hotel4you.hotel4you.View;

import com.hotel4you.hotel4you.Controller.MenuController;
import com.hotel4you.hotel4you.Model.OpcaoMenu;
import com.hotel4you.hotel4you.Model.Perfil;
import com.hotel4you.hotel4you.Utils.OperacaoCanceladaException;
import com.hotel4you.hotel4you.Utils.Utils;

import java.util.List;

/**
 * Menu principal: mostra apenas as opções do perfil do utilizador autenticado.
 * <p>
 * Só comunica com o {@link MenuController}. Termina quando o utilizador
 * escolhe "Terminar sessão", devolvendo o controlo a quem o chamou (o login).
 */
public class MenuView {

    private final MenuController controller;

    /** Cria o menu com o controller por omissão. */
    public MenuView() {
        this(new MenuController());
    }

    /**
     * @param controller controller que decide as opções de cada perfil
     */
    public MenuView(MenuController controller) {
        this.controller = controller;
    }

    /**
     * Mostra o menu do perfil e trata as escolhas até o utilizador terminar sessão.
     *
     * @param perfil perfil do utilizador autenticado
     */
    public void mostrar(Perfil perfil) {
        List<OpcaoMenu> opcoes = controller.obterOpcoes(perfil);
        int escolha;
        do {
            imprimirMenu(perfil, opcoes);
            escolha = Utils.lerInteiro("Opção: ", 0, opcoes.size());
            if (escolha != 0) {
                executar(perfil, opcoes.get(escolha - 1));
            }
        } while (escolha != 0);
        System.out.println("Sessão terminada.\n");
    }

    private void imprimirMenu(Perfil perfil, List<OpcaoMenu> opcoes) {
        System.out.println("\n=== Menu - " + perfil.getNome() + " ===");
        for (int i = 0; i < opcoes.size(); i++) {
            System.out.println((i + 1) + " - " + opcoes.get(i).getTexto());
        }
        System.out.println("0 - Terminar sessão");
    }

    private void executar(Perfil perfil, OpcaoMenu opcao) {
        if (!controller.temAcesso(perfil, opcao)) {
            System.out.println("Não tem permissões para esta opção.");
            return;
        }
        try {
            switch (opcao) {
                // Cada story acrescenta aqui o seu case, por exemplo:
                // case REGISTAR_CLIENTE:
                //     new ClienteView().registar();
                //     break;
                default:
                    System.out.println("\"" + opcao.getTexto() + "\" está em desenvolvimento.");
            }
        } catch (OperacaoCanceladaException e) {
            System.out.println("Operação cancelada.");
        }
    }
}
