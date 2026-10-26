package com.hotel4you.hotel4you;

import com.hotel4you.hotel4you.Controller.GestaoFuncionariosController;
import com.hotel4you.hotel4you.Controller.LoginController;
import com.hotel4you.hotel4you.Controller.Sessao;

import com.hotel4you.hotel4you.DAO.UtilizadorDAO;
import com.hotel4you.hotel4you.DAO.UtilizadorDAOMemoria;

import com.hotel4you.hotel4you.Model.Utilizador;
import com.hotel4you.hotel4you.Model.enums.Perfil;

import com.hotel4you.hotel4you.Utils.Navegador;

import com.hotel4you.hotel4you.View.GestaoFuncionariosViewController;
import com.hotel4you.hotel4you.View.LoginViewController;
import com.hotel4you.hotel4you.View.MenuPrincipalViewController;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Ponto de entrada da aplicação Hotel4You.
 */
public class Main extends Application {

    @Override
    public void start(Stage stage) {

        // Cria o acesso aos utilizadores em memória.
        UtilizadorDAO utilizadorDAO = new UtilizadorDAOMemoria();

        // Cria um gestor inicial para permitir iniciar sessão.
        utilizadorDAO.inserir(
                Utilizador.novo(
                        "gestor",
                        "gestor123",
                        "Gestor Inicial",
                        Perfil.GESTOR
                )
        );

        // Cria a sessão partilhada pela aplicação.
        Sessao sessao = new Sessao();

        // Cria o controlador do login.
        LoginController loginController =
                new LoginController(utilizadorDAO, sessao);

        // Cria o controlador da gestão de funcionários.
        GestaoFuncionariosController gestaoFuncionariosController =
                new GestaoFuncionariosController(
                        utilizadorDAO,
                        sessao
                );

        // Configura a navegação entre os ecrãs.
        Navegador.iniciar(stage, tipo -> {

            if (tipo == LoginViewController.class) {
                return new LoginViewController(loginController);
            }

            if (tipo == MenuPrincipalViewController.class) {
                return new MenuPrincipalViewController(sessao);
            }

            if (tipo == GestaoFuncionariosViewController.class) {
                return new GestaoFuncionariosViewController(
                        gestaoFuncionariosController
                );
            }

            throw new IllegalArgumentException(
                    "View desconhecida: " + tipo.getName()
            );
        });

        // Abre o ecrã de login.
        Navegador.mudarPara(
                "LoginView.fxml",
                "Hotel4You - Login"
        );

        stage.show();
    }

    /**
     * Método principal da aplicação.
     */
    public static void main(String[] args) {
        launch(args);
    }
}