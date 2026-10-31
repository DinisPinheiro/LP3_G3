package com.hotel4you.hotel4you;

import com.hotel4you.hotel4you.Controller.LoginController;
import com.hotel4you.hotel4you.Controller.MenuController;
import com.hotel4you.hotel4you.Controller.Sessao;
import com.hotel4you.hotel4you.DAO.DAOException;
import com.hotel4you.hotel4you.DAO.db.DBConnection;
import com.hotel4you.hotel4you.DAO.UtilizadorDAO;
import com.hotel4you.hotel4you.DAO.UtilizadorDAOSQL;
import com.hotel4you.hotel4you.Utils.Navegador;
import com.hotel4you.hotel4you.View.LoginViewController;
import com.hotel4you.hotel4you.View.MenuPrincipalViewController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
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
        // Os utilizadores (incluindo o gestor inicial) vêm da base de dados: ver db/Hotel4You.sql.
        UtilizadorDAO utilizadorDAO = new UtilizadorDAOSQL();

        Sessao sessao = new Sessao();
        LoginController loginController = new LoginController(utilizadorDAO, sessao);
        MenuController menuController = new MenuController();

        Navegador.iniciar(stage, tipo -> {
            if (tipo == LoginViewController.class) {
                return new LoginViewController(loginController);
            }
            if (tipo == MenuPrincipalViewController.class) {
                return new MenuPrincipalViewController(sessao, menuController);
            }
            throw new IllegalArgumentException("View desconhecida: " + tipo.getName());
        });
        Navegador.mudarPara("LoginView.fxml", "Hotel4You - Login");
        stage.show();
        avisarSeNaoHouverLigacao();
    }

    /**
     * Testa a ligação ao SQL Server no arranque. Se falhar, avisa o utilizador mas a aplicação
     * não fecha: o ecrã de login continua disponível e volta a mostrar o erro a cada tentativa.
     */
    private void avisarSeNaoHouverLigacao() {
        try {
            DBConnection.verificarLigacao();
        } catch (DAOException e) {
            Platform.runLater(() -> {
                Alert alerta = new Alert(Alert.AlertType.ERROR, e.getMessage());
                alerta.setTitle("Hotel4You");
                alerta.setHeaderText("Sem ligação à base de dados");
                alerta.show();
            });
        }
    }

    /**
     * @param args argumentos da linha de comandos (não usados)
     */
    public static void main(String[] args) {
        launch(args);
    }
}
