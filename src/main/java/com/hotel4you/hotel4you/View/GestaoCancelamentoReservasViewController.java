package com.hotel4you.hotel4you.View;

import com.hotel4you.hotel4you.Controller.GestaoReservasController;
import com.hotel4you.hotel4you.Model.Reserva;
import com.hotel4you.hotel4you.Model.enums.EstadoReserva;
import com.hotel4you.hotel4you.Utils.Navegador;
import com.hotel4you.hotel4you.Utils.Utils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.time.format.DateTimeFormatter;

/**
 * ViewController do ecrã de cancelamento e eliminação de reservas
 * ({@code GestaoCancelamentoReservasView.fxml}).
 * <p>
 * Escolher uma reserva na lista e carregar em "Cancelar reserva" (rececionista e gestor) ou em
 * "Apagar registo" (só o gestor, e só se o check-in não foi efetuado). Só lê a escolha e mostra
 * mensagens; as regras e as permissões estão no {@link GestaoReservasController}.
 * <p>
 * Apoio de IA: Claude (Anthropic), revisto pela equipa.
 */
public class GestaoCancelamentoReservasViewController {

    private static final String COR_ERRO = "-fx-text-fill: #b03a2e;";
    private static final String COR_SUCESSO = "-fx-text-fill: #1e8449;";
    private static final String COR_NEUTRA = "-fx-text-fill: #555555;";
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final GestaoReservasController controller;

    @FXML
    private TextField campoPesquisa;
    @FXML
    private ListView<Reserva> listaReservas;
    @FXML
    private Button botaoApagar;
    @FXML
    private Label labelPermissoes;
    @FXML
    private Label labelMensagem;

    /**
     * @param controller controller que cancela, apaga e consulta reservas
     */
    public GestaoCancelamentoReservasViewController(GestaoReservasController controller) {
        this.controller = controller;
    }

    /** Chamado pelo JavaFX depois de os campos {@code @FXML} serem preenchidos. */
    @FXML
    private void initialize() {
        listaReservas.setCellFactory(lista -> new ListCell<Reserva>() {
            @Override
            protected void updateItem(Reserva reserva, boolean vazio) {
                super.updateItem(reserva, vazio);
                setText(vazio || reserva == null ? null : descrever(reserva));
            }
        });
        campoPesquisa.textProperty().addListener((observavel, antigo, novo) -> atualizarLista());
        boolean gestor = controller.podeApagarReservas();
        botaoApagar.setDisable(!gestor);
        labelPermissoes.setText(gestor
                ? "Como gestor, pode cancelar reservas e apagar o registo das que ainda não tiveram check-in."
                : "O rececionista pode cancelar reservas a pedido do cliente. "
                  + "Só o gestor pode apagar o registo (o histórico fica guardado).");
        atualizarLista();
    }

    /** Chamado pelo botão "Cancelar reserva". */
    @FXML
    private void onCancelar() {
        Reserva reserva = listaReservas.getSelectionModel().getSelectedItem();
        if (reserva == null) {
            mostrar("Escolha uma reserva na lista.", COR_ERRO);
            return;
        }
        if (!confirmar("Cancelar a reserva n.º " + reserva.getId() + " de " + reserva.getCliente().getNome()
                + " (" + reserva.getQuarto() + ")?")) {
            mostrar("Cancelamento anulado.", COR_NEUTRA);
            return;
        }
        try {
            controller.cancelarReserva(reserva.getId());
            mostrar("Reserva n.º " + reserva.getId() + " cancelada. O " + reserva.getQuarto()
                    + " ficou disponível entre " + reserva.getDataEntrada().format(FORMATO_DATA) + " e "
                    + reserva.getDataSaida().format(FORMATO_DATA) + ".", COR_SUCESSO);
            atualizarLista();
        } catch (IllegalArgumentException | SecurityException e) {
            mostrar(e.getMessage(), COR_ERRO);
        } catch (RuntimeException e) {
            mostrar("Não foi possível cancelar a reserva: " + e.getMessage(), COR_ERRO);
        }
    }

    /** Chamado pelo botão "Apagar registo" (só o gestor). */
    @FXML
    private void onApagar() {
        Reserva reserva = listaReservas.getSelectionModel().getSelectedItem();
        if (reserva == null) {
            mostrar("Escolha uma reserva na lista.", COR_ERRO);
            return;
        }
        if (!confirmar("Apagar o registo da reserva n.º " + reserva.getId() + " de "
                + reserva.getCliente().getNome() + "? Esta ação não se pode desfazer.")) {
            mostrar("Eliminação anulada.", COR_NEUTRA);
            return;
        }
        try {
            controller.apagarReserva(reserva.getId());
            mostrar("Registo da reserva n.º " + reserva.getId() + " apagado.", COR_SUCESSO);
            atualizarLista();
        } catch (IllegalArgumentException | SecurityException e) {
            mostrar(e.getMessage(), COR_ERRO);
        } catch (RuntimeException e) {
            mostrar("Não foi possível apagar a reserva: " + e.getMessage(), COR_ERRO);
        }
    }

    /** Chamado pelo botão "Voltar ao menu". */
    @FXML
    private void onVoltar() {
        Navegador.mudarPara("MenuPrincipalView.fxml", "Hotel4You");
    }

    /** Mostra uma caixa de confirmação com os botões "Sim" e "Não". */
    private boolean confirmar(String texto) {
        ButtonType sim = new ButtonType("Sim", ButtonBar.ButtonData.YES);
        ButtonType nao = new ButtonType("Não", ButtonBar.ButtonData.NO);
        Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, texto, sim, nao);
        alerta.setTitle("Hotel4You");
        alerta.setHeaderText(null);
        return alerta.showAndWait().filter(botao -> botao == sim).isPresent();
    }

    private void atualizarLista() {
        try {
            listaReservas.setItems(FXCollections.observableArrayList(
                    controller.consultarReservas(campoPesquisa.getText())));
        } catch (RuntimeException e) {
            mostrar(e.getMessage(), COR_ERRO);
        }
    }

    private void mostrar(String texto, String estilo) {
        labelMensagem.setText(texto);
        labelMensagem.setStyle(estilo);
    }

    private static String descrever(Reserva reserva) {
        return "N.º " + reserva.getId() + "  |  " + reserva.getCliente().getNome() + "  |  " + reserva.getQuarto()
                + "  |  " + reserva.getDataEntrada().format(FORMATO_DATA) + " a "
                + reserva.getDataSaida().format(FORMATO_DATA)
                + "  |  " + Utils.formatarPreco(reserva.calcularTotal())
                + "  |  " + descreverEstado(reserva.getEstado());
    }

    private static String descreverEstado(EstadoReserva estado) {
        return switch (estado) {
            case PENDENTE -> "Reservado (pendente de check-in)";
            case CHECK_IN -> "Check-in efetuado";
            case CHECK_OUT -> "Check-out efetuado";
            case CANCELADA -> "Cancelada";
        };
    }
}