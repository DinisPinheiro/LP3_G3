package com.hotel4you.hotel4you.View;

import com.hotel4you.hotel4you.Controller.Sessao;
import com.hotel4you.hotel4you.Model.enums.Perfil;
import com.hotel4you.hotel4you.Utils.Navegador;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

/**
 * Controla o menu principal da aplicação.
 */
public class MenuPrincipalViewController {

    private final Sessao sessao;

    @FXML
    private Label labelBoasVindas;

    @FXML
    private Button botaoGestaoFuncionarios;

    /**
     * Recebe a sessão do utilizador.
     */
    public MenuPrincipalViewController(Sessao sessao) {
        this.sessao = sessao;
    }

    /**
     * Apresenta o utilizador e controla as opções do menu.
     */
    @FXML
    private void initialize() {

        sessao.getUtilizadorAtual().ifPresent(utilizador -> {

            labelBoasVindas.setText(
                    "Bem-vindo, "
                            + utilizador.getNome()
                            + " ("
                            + utilizador.getPerfil().getNome()
                            + ")"
            );

            boolean gestor =
                    utilizador.getPerfil() == Perfil.GESTOR
                            && utilizador.isAtivo();

            // Só o gestor vê esta opção.
            botaoGestaoFuncionarios.setVisible(gestor);
            botaoGestaoFuncionarios.setManaged(gestor);
        });
    }

    /**
     * Abre o ecrã de gestão de funcionários.
     */
    @FXML
    private void onGestaoFuncionarios() {

        boolean gestor = sessao.getUtilizadorAtual()
                .map(utilizador ->
                        utilizador.getPerfil() == Perfil.GESTOR
                                && utilizador.isAtivo())
                .orElse(false);

        if (gestor) {

            Navegador.mudarPara(
                    "GestaoFuncionariosView.fxml",
                    "Hotel4You - Gestão de funcionários"
            );
        }
    }

    /**
     * Termina a sessão atual.
     */
    @FXML
    private void onTerminarSessao() {

        sessao.terminar();

        Navegador.mudarPara(
                "LoginView.fxml",
                "Hotel4You - Login"
        );
    }
}