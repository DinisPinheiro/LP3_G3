package com.hotel4you.hotel4you;

import com.hotel4you.hotel4you.Controller.*;
import com.hotel4you.hotel4you.DAO.*;
import com.hotel4you.hotel4you.DAO.db.DBConnection;
import com.hotel4you.hotel4you.Utils.Navegador;
import com.hotel4you.hotel4you.View.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

/**
 * Ponto de entrada da aplicação Hotel4You.
 * <p>
 * Aqui (e só aqui) se constroem os DAO e os Controllers e se ligam aos ViewControllers.
 * Dependências: View → Controller → Model/DAO; o Model não depende de nenhuma camada acima.
 * <p>
 * O {@link #start(Stage)} está dividido em três passos, e em cada passo as áreas aparecem
 * sempre pela mesma ordem (utilizadores e menu, clientes, quartos e serviços, reservas, stock,
 * fornecedores):
 * <ol>
 *     <li>DAO: acesso à base de dados;</li>
 *     <li>Controllers: regras de negócio, que recebem os DAO;</li>
 *     <li>ViewControllers: ecrãs, que recebem os Controllers.</li>
 * </ol>
 */
public class Main extends Application {

    /**
     * Constrói os DAO, os Controllers e os ViewControllers e abre o ecrã de login.
     *
     * @param stage janela principal da aplicação
     */
    @Override
    public void start(Stage stage) {
        // ---- 1. DAO (acesso à base de dados) ----------------------------------------------------
        // Os utilizadores (incluindo o gestor inicial) vêm da base de dados: ver DATABASE/EsquemaDB_2_1.sql.
        UtilizadorDAO utilizadorDAO = new UtilizadorDAOSQL();
        ClienteDAO clienteDAO = new ClienteDAOSQL();
        QuartoDAO quartoDAO = new QuartoDAOSQL();
        EquipamentoDAO equipamentoDAO = new EquipamentoDAOSQL();
        SuplementoVistaDAO suplementoVistaDAO = new SuplementoVistaDAOSQL();
        ServicoDAO servicoDAO = new ServicoDAOSQL();
        CartaoDAO cartaoDAO = new CartaoDAOSQL();
        ReservaDAO reservaDAO = new ReservaDAOSQL(clienteDAO, quartoDAO, cartaoDAO);
        StockDAO stockDAO = new StockDAOSQL();
        FornecedorDAO fornecedorDAO = new FornecedorDAOSQL();
        ImportacaoDAO importacaoDAO = new ImportacaoDAOSQL();
        XsdDAO xsdDAO = new XsdDAOSQL();
        Sessao sessao = new Sessao();

        // Utilizadores e menu
        LoginController loginController = new LoginController(utilizadorDAO, sessao);
        MenuController menuController = new MenuController();
        GestaoFuncionariosController gestaoFuncionariosController =
                new GestaoFuncionariosController(utilizadorDAO, sessao);

        // Clientes
        GestaoClientesController gestaoClientesController = new GestaoClientesController(clienteDAO, sessao);

        // Quartos, vistas e serviços
        GestaoQuartosController gestaoQuartosController =
                new GestaoQuartosController(quartoDAO, equipamentoDAO, suplementoVistaDAO, sessao);
        GestaoVistasController gestaoVistasController = new GestaoVistasController(suplementoVistaDAO, sessao);
        GestaoServicosController gestaoServicosController = new GestaoServicosController(servicoDAO, sessao);

        // Reservas
        GestaoReservasController gestaoReservasController =
                new GestaoReservasController(reservaDAO, clienteDAO, quartoDAO, servicoDAO, stockDAO, sessao);

        // Edição e detalhes de reservas
        EdicaoReservaController edicaoReservaController =
                new EdicaoReservaController(reservaDAO, servicoDAO, sessao);

        // Check-in e check-out
        CheckInCheckOutController checkInCheckOutController =
                new CheckInCheckOutController(reservaDAO, cartaoDAO, sessao);

        // Stock
        GestaoStockController gestaoStockController = new GestaoStockController(stockDAO, sessao);

        // Fornecedores, XSD e aprovação das entregas
        GestaoFornecedoresController gestaoFornecedoresController =
                new GestaoFornecedoresController(fornecedorDAO, utilizadorDAO, sessao);
        GestaoXsdController gestaoXsdController = new GestaoXsdController(xsdDAO, sessao);
        ImportacaoFornecedorController importacaoFornecedorController =
                new ImportacaoFornecedorController(importacaoDAO, fornecedorDAO, gestaoXsdController, sessao);
        AprovacaoImportacoesController aprovacaoImportacoesController =
                new AprovacaoImportacoesController(importacaoDAO, gestaoStockController, gestaoXsdController, sessao);

        // ---- 3. ViewControllers (ecrãs) ---------------------------------------------------------
        // Quando abre um ecrã, o Navegador indica a classe do ViewController e recebe aqui a instância.
        Navegador.iniciar(stage, tipo -> {
            // Utilizadores e menu
            if (tipo == LoginViewController.class) {
                return new LoginViewController(loginController);
            }
            if (tipo == MenuPrincipalViewController.class) {
                return new MenuPrincipalViewController(sessao, menuController);
            }
            if (tipo == GestaoFuncionariosViewController.class) {
                return new GestaoFuncionariosViewController(gestaoFuncionariosController);
            }

            // Clientes
            if (tipo == GestaoClientesViewController.class) {
                return new GestaoClientesViewController(gestaoClientesController);
            }

            // Quartos, vistas e serviços
            if (tipo == GestaoQuartosViewController.class) {
                return new GestaoQuartosViewController(gestaoQuartosController);
            }
            if (tipo == GestaoVistasViewController.class) {
                return new GestaoVistasViewController(gestaoVistasController);
            }
            if (tipo == GestaoServicosViewController.class) {
                return new GestaoServicosViewController(gestaoServicosController);
            }

            // Reservas
            if (tipo == GestaoReservasViewController.class) {
                return new GestaoReservasViewController(gestaoReservasController, gestaoClientesController);
            }

            // Editar e consultar reservas
            if (tipo == EdicaoReservaViewController.class) {
                return new EdicaoReservaViewController(edicaoReservaController);
            }

            if (tipo == ReservasAtivasViewController.class) {
                return new ReservasAtivasViewController(gestaoReservasController);
            }
            if (tipo == HistoricoReservasViewController.class) {
                return new HistoricoReservasViewController(gestaoReservasController);
            }

            // Consultar disponibilidade
            if (tipo == ConsultaDisponibilidadeViewController.class) {
                return new ConsultaDisponibilidadeViewController(gestaoReservasController);
            }
            if (tipo == GestaoCancelamentoReservasViewController.class) {
                return new GestaoCancelamentoReservasViewController(gestaoReservasController);
            }
            if (tipo == CheckInCheckOutViewController.class) {
                return new CheckInCheckOutViewController(checkInCheckOutController, gestaoReservasController);
            }

            // Stock
            if (tipo == ConsultaStockViewController.class) {
                return new ConsultaStockViewController(gestaoStockController);
            }

            // Fornecedores, XSD e aprovação das entregas
            if (tipo == ImportacaoFornecedorViewController.class) {
                return new ImportacaoFornecedorViewController(importacaoFornecedorController);
            }
            if (tipo == AprovacaoImportacoesViewController.class) {
                return new AprovacaoImportacoesViewController(aprovacaoImportacoesController);
            }
            if (tipo == GestaoFornecedoresViewController.class) {
                return new GestaoFornecedoresViewController(gestaoFornecedoresController);
            }
            if (tipo == ImportacaoXsdViewController.class) {
                return new ImportacaoXsdViewController(gestaoXsdController);
            }

            throw new IllegalArgumentException("View desconhecida: " + tipo.getName());
        });

        // ---- Arranque ---------------------------------------------------------------------------
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