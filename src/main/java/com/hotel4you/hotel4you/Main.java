package com.hotel4you.hotel4you;

import com.hotel4you.hotel4you.Controller.LoginController;
import com.hotel4you.hotel4you.Controller.Sessao;
import com.hotel4you.hotel4you.DAO.UtilizadorDAO;
import com.hotel4you.hotel4you.DAO.UtilizadorDAOMemoria;
import com.hotel4you.hotel4you.Model.enums.Perfil;
import com.hotel4you.hotel4you.Model.Utilizador;
import com.hotel4you.hotel4you.Utils.Navegador;
import com.hotel4you.hotel4you.View.LoginViewController;
import com.hotel4you.hotel4you.View.MenuPrincipalViewController;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Ponto de entrada da aplicação Hotel4You.
 * <p>
 * Aqui (e só aqui) se constroem os DAO e os Controllers e se ligam aos ViewControllers.
 * Dependências: View → Controller → Model/DAO; o Model não depende de nenhuma camada acima.
 */
public class Main extends Application {

    @Override
    public void start(Stage stage) {
        UtilizadorDAO utilizadorDAO = new UtilizadorDAOMemoria();
        // TODO S2-04/S2-05: substituir pelo DAO SQL Server; o gestor inicial passa a vir do script SQL.
        utilizadorDAO.inserir(Utilizador.novo("gestor", "gestor123", "Gestor Inicial", Perfil.GESTOR));

        Sessao sessao = new Sessao();
        LoginController loginController = new LoginController(utilizadorDAO, sessao);

        Navegador.iniciar(stage, tipo -> {
            if (tipo == LoginViewController.class) {
                return new LoginViewController(loginController);
            }
            if (tipo == MenuPrincipalViewController.class) {
                return new MenuPrincipalViewController(sessao);
            }
            throw new IllegalArgumentException("View desconhecida: " + tipo.getName());
        });
        Navegador.mudarPara("LoginView.fxml", "Hotel4You - Login");
        stage.show();
    }

    /**
     * @param args argumentos da linha de comandos (não usados)
     */
    public static void main(String[] args) {
        launch(args);
    }
}
