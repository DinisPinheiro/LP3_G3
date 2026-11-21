
package com.hotel4you.hotel4you.View;

import com.hotel4you.hotel4you.Controller.MenuController;
import com.hotel4you.hotel4you.Controller.Sessao;
import com.hotel4you.hotel4you.Model.AreaMenu;
import com.hotel4you.hotel4you.Model.OpcaoMenu;
import com.hotel4you.hotel4you.Model.enums.Perfil;
import com.hotel4you.hotel4you.Utils.Navegador;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ViewController do layout principal (MenuPrincipalView.fxml).
 * Mostra o menu lateral e os ecrãs das funcionalidades.
 */
public class MenuPrincipalViewController {

    /** Ecrã de uma opção do menu. */
    private record Destino(String fxml, String titulo, String rotulo) {
    }

    private static final Destino CHECK_IN_OUT =
            new Destino(
                    "CheckInCheckOutView.fxml",
                    "Hotel4You - Check-in e check-out",
                    "Check-in e check-out"
            );

    private static final Destino CANCELAR_APAGAR =
            new Destino(
                    "GestaoCancelamentoReservasView.fxml",
                    "Hotel4You - Cancelar e apagar reservas",
                    "Cancelar e apagar reservas"
            );

    private static final Map<OpcaoMenu, Destino> DESTINOS =
            new EnumMap<>(OpcaoMenu.class);

    static {

        DESTINOS.put(
                OpcaoMenu.REGISTAR_CLIENTE,
                new Destino(
                        "GestaoClientesView.fxml",
                        "Hotel4You - Clientes",
                        "Clientes"
                )
        );

        DESTINOS.put(
                OpcaoMenu.CONSULTAR_DISPONIBILIDADE,
                new Destino(
                        "ConsultaDisponibilidadeView.fxml",
                        "Hotel4You - Disponibilidade de quartos",
                        "Disponibilidade de quartos"
                )
        );

        DESTINOS.put(
                OpcaoMenu.CRIAR_RESERVA,
                new Destino(
                        "GestaoReservasView.fxml",
                        "Hotel4You - Criar reserva",
                        "Criar reserva"
                )
        );

        DESTINOS.put(
                OpcaoMenu.EDITAR_RESERVA,
                new Destino(
                        "EdicaoReservaView.fxml",
                        "Hotel4You - Edição e detalhes da reserva",
                        "Editar e consultar reservas"
                )
        );

        DESTINOS.put(
                OpcaoMenu.RESERVAS_ATIVAS,
                new Destino(
                        "ReservasAtivasView.fxml",
                        "Hotel4You - Reservas ativas",
                        "Reservas ativas"
                )
        );

        DESTINOS.put(
                OpcaoMenu.HISTORICO_RESERVAS,
                new Destino(
                        "HistoricoReservasView.fxml",
                        "Hotel4You - Histórico",
                        "Histórico de reservas"
                )
        );

        DESTINOS.put(
                OpcaoMenu.CANCELAR_RESERVA,
                CANCELAR_APAGAR
        );

        DESTINOS.put(
                OpcaoMenu.APAGAR_RESERVAS,
                CANCELAR_APAGAR
        );

        DESTINOS.put(
                OpcaoMenu.CHECK_IN,
                CHECK_IN_OUT
        );

        DESTINOS.put(
                OpcaoMenu.CHECK_OUT,
                CHECK_IN_OUT
        );

        DESTINOS.put(
                OpcaoMenu.SERVICOS_E_EXTRAS,
                new Destino(
                        "GestaoServicosView.fxml",
                        "Hotel4You - Serviços e extras",
                        "Serviços e extras"
                )
        );

        // Picagem de cartão
        DESTINOS.put(
                OpcaoMenu.PICAGEM_CARTAO,
                new Destino(
                        "PicagemCartaoView.fxml",
                        "Hotel4You - Picagem de cartão",
                        "Picagem de cartão"
                )
        );

        // Consulta de passagens por cartão
        DESTINOS.put(
                OpcaoMenu.LISTAGEM_POR_CARTAO,
                new Destino(
                        "ConsultaPassagensView.fxml",
                        "Hotel4You - Passagens por cartão",
                        "Passagens por cartão"
                )
        );

        // Consulta de passagens por quarto
        DESTINOS.put(
                OpcaoMenu.LISTAGEM_POR_QUARTO,
                new Destino(
                        "ConsultaPassagensView.fxml",
                        "Hotel4You - Passagens por quarto",
                        "Passagens por quarto"
                )
        );

        DESTINOS.put(
                OpcaoMenu.GERIR_STOCK,
                new Destino(
                        "ConsultaStockView.fxml",
                        "Hotel4You - Stock",
                        "Stock"
                )
        );

        DESTINOS.put(
                OpcaoMenu.APROVAR_ENTREGAS,
                new Destino(
                        "AprovacaoImportacoesView.fxml",
                        "Hotel4You - Aprovar entregas",
                        "Aprovar entregas"
                )
        );

        DESTINOS.put(
                OpcaoMenu.ENVIAR_ENTREGA_XML,
                new Destino(
                        "ImportacaoFornecedorView.fxml",
                        "Hotel4You - Enviar entrega",
                        "Enviar entrega (XML)"
                )
        );

        DESTINOS.put(
                OpcaoMenu.GERIR_FORNECEDORES,
                new Destino(
                        "GestaoFornecedoresView.fxml",
                        "Hotel4You - Fornecedores",
                        "Gerir fornecedores"
                )
        );

        DESTINOS.put(
                OpcaoMenu.GERIR_FUNCIONARIOS,
                new Destino(
                        "GestaoFuncionariosView.fxml",
                        "Hotel4You - Gestão de funcionários",
                        "Funcionários"
                )
        );

        DESTINOS.put(
                OpcaoMenu.CONFIGURAR_QUARTOS,
                new Destino(
                        "GestaoQuartosView.fxml",
                        "Hotel4You - Quartos",
                        "Quartos"
                )
        );

        DESTINOS.put(
                OpcaoMenu.EDITAR_VISTAS,
                new Destino(
                        "GestaoVistasView.fxml",
                        "Hotel4You - Vistas especiais",
                        "Vistas especiais"
                )
        );
    }

    private final Sessao sessao;
    private final MenuController menuController;

    private final ToggleGroup grupoAreas = new ToggleGroup();
    private final ToggleGroup grupoSeparadores = new ToggleGroup();

    @FXML
    private VBox caixaAreas;

    @FXML
    private Label labelUtilizador;

    @FXML
    private Label labelTitulo;

    @FXML
    private HBox caixaSeparadores;

    @FXML
    private StackPane areaConteudo;

    public MenuPrincipalViewController(
            Sessao sessao,
            MenuController menuController) {

        this.sessao = sessao;
        this.menuController = menuController;
    }

    @FXML
    private void initialize() {

        Navegador.registarLayout(
                areaConteudo,
                this::mostrarInicio
        );

        sessao.getUtilizadorAtual().ifPresent(utilizador -> {

            Perfil perfil = utilizador.getPerfil();

            labelUtilizador.setText(
                    utilizador.getNome()
                            + " (" + perfil.getNome() + ")"
            );

            for (AreaMenu area : menuController.obterAreas(perfil)) {

                ToggleButton botao =
                        novoBotao(area.getTexto(), grupoAreas);

                botao.setOnAction(evento ->
                        abrirArea(perfil, area, botao)
                );

                caixaAreas.getChildren().add(botao);
            }
        });

        mostrarInicio();
    }

    @FXML
    private void onTerminarSessao() {

        sessao.terminar();

        Navegador.mudarPara(
                "LoginView.fxml",
                "Hotel4You - Login"
        );
    }

    private void mostrarInicio() {

        grupoAreas.selectToggle(null);

        limparSeparadores();

        labelTitulo.setText("Hotel4You");

        areaConteudo.getChildren().setAll(
                mensagem("Escolha uma opção no menu.")
        );
    }

    private void abrirArea(
            Perfil perfil,
            AreaMenu area,
            ToggleButton botao) {

        botao.setSelected(true);

        if (!menuController.obterAreas(perfil).contains(area)) {

            mostrarErro(
                    "Não tem permissões para esta opção."
            );

            return;
        }

        labelTitulo.setText(area.getTitulo());

        limparSeparadores();

        // Opções com o mesmo FXML ficam num só separador.
        Map<String, OpcaoMenu> separadores =
                new LinkedHashMap<>();

        List<OpcaoMenu> opcoes =
                menuController.obterOpcoes(perfil, area);

        for (OpcaoMenu opcao : opcoes) {

            Destino destino = DESTINOS.get(opcao);

            separadores.putIfAbsent(
                    destino == null
                            ? "sem-ecra:" + opcao
                            : destino.fxml(),
                    opcao
            );
        }

        if (separadores.size() > 1) {

            for (OpcaoMenu opcao : separadores.values()) {

                Destino destino = DESTINOS.get(opcao);

                ToggleButton separador = novoBotao(
                        destino == null
                                ? opcao.getTexto()
                                : destino.rotulo(),
                        grupoSeparadores
                );

                separador.setOnAction(evento -> {

                    separador.setSelected(true);

                    abrirOpcao(perfil, opcao);
                });

                caixaSeparadores.getChildren().add(separador);
            }

            ((ToggleButton) caixaSeparadores
                    .getChildren()
                    .get(0)).setSelected(true);
        }

        abrirOpcao(
                perfil,
                separadores.values().iterator().next()
        );
    }

    private void abrirOpcao(
            Perfil perfil,
            OpcaoMenu opcao) {

        if (!menuController.temAcesso(perfil, opcao)) {

            mostrarErro(
                    "Não tem permissões para esta opção."
            );

            return;
        }

        Destino destino = DESTINOS.get(opcao);

        if (destino == null) {

            areaConteudo.getChildren().setAll(
                    mensagem(
                            "\"" + opcao.getTexto()
                                    + "\" está em desenvolvimento."
                    )
            );

            return;
        }

        Navegador.mudarPara(
                destino.fxml(),
                destino.titulo()
        );
    }

    private void limparSeparadores() {

        caixaSeparadores.getChildren().clear();

        grupoSeparadores.getToggles().clear();
    }

    private void mostrarErro(String texto) {

        Label erro = mensagem(texto);

        erro.setStyle("-fx-text-fill: #c0392b;");

        areaConteudo.getChildren().setAll(erro);
    }

    private static Label mensagem(String texto) {

        Label label = new Label(texto);

        label.setPadding(new Insets(18));

        label.setWrapText(true);

        return label;
    }

    private static ToggleButton novoBotao(
            String texto,
            ToggleGroup grupo) {

        ToggleButton botao = new ToggleButton(texto);

        botao.setToggleGroup(grupo);

        botao.setMaxWidth(Double.MAX_VALUE);

        botao.setWrapText(true);

        return botao;
    }
}
