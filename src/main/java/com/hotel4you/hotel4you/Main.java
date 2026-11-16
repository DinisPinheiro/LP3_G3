
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

public class Main extends Application {

    @Override
    public void start(Stage stage) {

        // ---- 1. DAO (acesso à base de dados) ------------------

        UtilizadorDAO utilizadorDAO = new UtilizadorDAOSQL();

        ClienteDAO clienteDAO = new ClienteDAOSQL();

        QuartoDAO quartoDAO = new QuartoDAOSQL();

        EquipamentoDAO equipamentoDAO = new EquipamentoDAOSQL();

        SuplementoVistaDAO suplementoVistaDAO =
                new SuplementoVistaDAOSQL();

        ServicoDAO servicoDAO = new ServicoDAOSQL();

        CartaoDAO cartaoDAO = new CartaoDAOSQL();

        ReservaDAO reservaDAO =
                new ReservaDAOSQL(
                        clienteDAO,
                        quartoDAO,
                        cartaoDAO
                );

        // DAO das passagens de cartões
        PassagemDAO passagemDAO = new PassagemDAOSQL();

        StockDAO stockDAO = new StockDAOSQL();

        FornecedorDAO fornecedorDAO = new FornecedorDAOSQL();

        ImportacaoDAO importacaoDAO = new ImportacaoDAOSQL();

        XsdDAO xsdDAO = new XsdDAOSQL();

        Sessao sessao = new Sessao();


        // ---- 2. Controllers (regras de negócio) ---------------

        // Utilizadores e menu
        LoginController loginController =
                new LoginController(utilizadorDAO, sessao);

        MenuController menuController = new MenuController();

        GestaoFuncionariosController gestaoFuncionariosController =
                new GestaoFuncionariosController(
                        utilizadorDAO,
                        sessao
                );


        // Clientes
        GestaoClientesController gestaoClientesController =
                new GestaoClientesController(
                        clienteDAO,
                        sessao
                );


        // Quartos, vistas e serviços
        GestaoQuartosController gestaoQuartosController =
                new GestaoQuartosController(
                        quartoDAO,
                        equipamentoDAO,
                        suplementoVistaDAO,
                        sessao
                );

        GestaoVistasController gestaoVistasController =
                new GestaoVistasController(
                        suplementoVistaDAO,
                        sessao
                );

        GestaoServicosController gestaoServicosController =
                new GestaoServicosController(
                        servicoDAO,
                        sessao
                );


        // Reservas
        GestaoReservasController gestaoReservasController =
                new GestaoReservasController(
                        reservaDAO,
                        clienteDAO,
                        quartoDAO,
                        servicoDAO,
                        stockDAO,
                        sessao
                );


        // Edição e detalhes de reservas
        EdicaoReservaController edicaoReservaController =
                new EdicaoReservaController(
                        reservaDAO,
                        servicoDAO,
                        sessao
                );


        // Check-in e check-out
        CheckInCheckOutController checkInCheckOutController =
                new CheckInCheckOutController(
                        reservaDAO,
                        cartaoDAO,
                        sessao
                );


        // Picagem de cartão e registo de acessos
        PassagemController passagemController =
                new PassagemController(
                        passagemDAO,
                        cartaoDAO,
                        reservaDAO,
                        quartoDAO,
                        sessao
                );


        // Stock
        GestaoStockController gestaoStockController =
                new GestaoStockController(
                        stockDAO,
                        sessao
                );


        // Fornecedores, XSD e aprovação das entregas
        GestaoFornecedoresController gestaoFornecedoresController =
                new GestaoFornecedoresController(
                        fornecedorDAO,
                        utilizadorDAO,
                        sessao
                );

        GestaoXsdController gestaoXsdController =
                new GestaoXsdController(
                        xsdDAO,
                        sessao
                );

        ImportacaoFornecedorController importacaoFornecedorController =
                new ImportacaoFornecedorController(
                        importacaoDAO,
                        fornecedorDAO,
                        gestaoXsdController,
                        sessao
                );

        AprovacaoImportacoesController aprovacaoImportacoesController =
                new AprovacaoImportacoesController(
                        importacaoDAO,
                        gestaoStockController,
                        gestaoXsdController,
                        sessao
                );


        // ---- 3. ViewControllers (ecrãs) -----------------------

        Navegador.iniciar(stage, tipo -> {

            // Utilizadores e menu
            if (tipo == LoginViewController.class) {

                return new LoginViewController(
                        loginController
                );
            }

            if (tipo == MenuPrincipalViewController.class) {

                return new MenuPrincipalViewController(
                        sessao,
                        menuController
                );
            }

            if (tipo == GestaoFuncionariosViewController.class) {

                return new GestaoFuncionariosViewController(
                        gestaoFuncionariosController
                );
            }


            // Clientes
            if (tipo == GestaoClientesViewController.class) {

                return new GestaoClientesViewController(
                        gestaoClientesController
                );
            }


            // Quartos, vistas e serviços
            if (tipo == GestaoQuartosViewController.class) {

                return new GestaoQuartosViewController(
                        gestaoQuartosController
                );
            }

            if (tipo == GestaoVistasViewController.class) {

                return new GestaoVistasViewController(
                        gestaoVistasController
                );
            }

            if (tipo == GestaoServicosViewController.class) {

                return new GestaoServicosViewController(
                        gestaoServicosController
                );
            }


            // Reservas
            if (tipo == GestaoReservasViewController.class) {

                return new GestaoReservasViewController(
                        gestaoReservasController,
                        gestaoClientesController
                );
            }


            // Editar e consultar reservas
            if (tipo == EdicaoReservaViewController.class) {

                return new EdicaoReservaViewController(
                        edicaoReservaController
                );
            }

            if (tipo == ReservasAtivasViewController.class) {

                return new ReservasAtivasViewController(
                        gestaoReservasController
                );
            }

            if (tipo == HistoricoReservasViewController.class) {

                return new HistoricoReservasViewController(
                        gestaoReservasController
                );
            }


            // Consultar disponibilidade
            if (tipo == ConsultaDisponibilidadeViewController.class) {

                return new ConsultaDisponibilidadeViewController(
                        gestaoReservasController
                );
            }


            // Cancelar e apagar reservas
            if (tipo == GestaoCancelamentoReservasViewController.class) {

                return new GestaoCancelamentoReservasViewController(
                        gestaoReservasController
                );
            }


            // Check-in e check-out
            if (tipo == CheckInCheckOutViewController.class) {

                return new CheckInCheckOutViewController(
                        checkInCheckOutController,
                        gestaoReservasController
                );
            }


            // Picagem de cartão
            if (tipo == PicagemCartaoViewController.class) {

                return new PicagemCartaoViewController(
                        passagemController
                );
            }


            // Consulta de passagens por cartão e quarto
            if (tipo == ConsultaPassagensViewController.class) {

                return new ConsultaPassagensViewController(
                        passagemController
                );
            }


            // Stock
            if (tipo == ConsultaStockViewController.class) {

                return new ConsultaStockViewController(
                        gestaoStockController
                );
            }


            // Fornecedores, XSD e aprovação das entregas
            if (tipo == ImportacaoFornecedorViewController.class) {

                return new ImportacaoFornecedorViewController(
                        importacaoFornecedorController
                );
            }

            if (tipo == AprovacaoImportacoesViewController.class) {

                return new AprovacaoImportacoesViewController(
                        aprovacaoImportacoesController
                );
            }

            if (tipo == GestaoFornecedoresViewController.class) {

                return new GestaoFornecedoresViewController(
                        gestaoFornecedoresController
                );
            }

            if (tipo == ImportacaoXsdViewController.class) {

                return new ImportacaoXsdViewController(
                        gestaoXsdController
                );
            }


            throw new IllegalArgumentException(
                    "View desconhecida: " + tipo.getName()
            );
        });


        // ---- Arranque ----------------------------------------

        Navegador.mudarPara(
                "LoginView.fxml",
                "Hotel4You - Login"
        );

        stage.show();

        avisarSeNaoHouverLigacao();
    }


    // Verifica a ligação à base de dados.
    private void avisarSeNaoHouverLigacao() {

        try {

            DBConnection.verificarLigacao();

        } catch (DAOException e) {

            Platform.runLater(() -> {

                Alert alerta = new Alert(
                        Alert.AlertType.ERROR,
                        e.getMessage()
                );

                alerta.setTitle("Hotel4You");

                alerta.setHeaderText(
                        "Sem ligação à base de dados"
                );

                alerta.show();
            });
        }
    }


    public static void main(String[] args) {

        launch(args);
    }
}
