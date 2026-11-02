package com.hotel4you.hotel4you;

import com.hotel4you.hotel4you.Controller.GestaoFuncionariosController;
import com.hotel4you.hotel4you.Controller.GestaoClientesController;
import com.hotel4you.hotel4you.Controller.GestaoQuartosController;
import com.hotel4you.hotel4you.Controller.GestaoReservasController;
import com.hotel4you.hotel4you.Controller.LoginController;
import com.hotel4you.hotel4you.Controller.MenuController;
import com.hotel4you.hotel4you.Controller.Sessao;
import com.hotel4you.hotel4you.DAO.ClienteDAO;
import com.hotel4you.hotel4you.DAO.ClienteDAOSQL;
import com.hotel4you.hotel4you.DAO.DAOException;
import com.hotel4you.hotel4you.DAO.EquipamentoDAO;
import com.hotel4you.hotel4you.DAO.EquipamentoDAOSQL;
import com.hotel4you.hotel4you.DAO.QuartoDAO;
import com.hotel4you.hotel4you.DAO.QuartoDAOSQL;
import com.hotel4you.hotel4you.DAO.ReservaDAO;
import com.hotel4you.hotel4you.DAO.ReservaDAOSQL;
import com.hotel4you.hotel4you.DAO.db.DBConnection;
import com.hotel4you.hotel4you.DAO.UtilizadorDAO;
import com.hotel4you.hotel4you.DAO.UtilizadorDAOSQL;
import com.hotel4you.hotel4you.Utils.Navegador;
import com.hotel4you.hotel4you.View.GestaoFuncionariosViewController;
import com.hotel4you.hotel4you.View.GestaoClientesViewController;
import com.hotel4you.hotel4you.View.GestaoCancelamentoReservasViewController;
import com.hotel4you.hotel4you.View.GestaoQuartosViewController;
import com.hotel4you.hotel4you.View.GestaoReservasViewController;
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
        ClienteDAO clienteDAO = new ClienteDAOSQL();
        QuartoDAO quartoDAO = new QuartoDAOSQL();
        EquipamentoDAO equipamentoDAO = new EquipamentoDAOSQL();
        GestaoClientesController gestaoClientesController = new GestaoClientesController(clienteDAO, sessao);
        GestaoQuartosController gestaoQuartosController =
                new GestaoQuartosController(quartoDAO, equipamentoDAO, sessao);
        GestaoFuncionariosController gestaoFuncionariosController =
                new GestaoFuncionariosController(utilizadorDAO, sessao);
        ReservaDAO reservaDAO = new ReservaDAOSQL(clienteDAO, quartoDAO);
        GestaoReservasController gestaoReservasController =
                new GestaoReservasController(reservaDAO, clienteDAO, quartoDAO, sessao);

        Navegador.iniciar(stage, tipo -> {
            if (tipo == LoginViewController.class) {
                return new LoginViewController(loginController);
            }
            if (tipo == MenuPrincipalViewController.class) {
                return new MenuPrincipalViewController(sessao, menuController);
            }
            if (tipo == GestaoFuncionariosViewController.class) {
                return new GestaoFuncionariosViewController(gestaoFuncionariosController);
            }
            if (tipo == GestaoClientesViewController.class) {
                return new GestaoClientesViewController(gestaoClientesController);
            }
            if (tipo == GestaoQuartosViewController.class) {
                return new GestaoQuartosViewController(gestaoQuartosController);
            }
            if (tipo == GestaoReservasViewController.class) {
                return new GestaoReservasViewController(gestaoReservasController, gestaoClientesController);
            }
            if (tipo == GestaoCancelamentoReservasViewController.class) {
                return new GestaoCancelamentoReservasViewController(gestaoReservasController);
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