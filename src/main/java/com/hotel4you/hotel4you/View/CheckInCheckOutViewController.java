package com.hotel4you.hotel4you.View;

import com.hotel4you.hotel4you.Controller.CheckInCheckOutController;
import com.hotel4you.hotel4you.Controller.GestaoReservasController;
import com.hotel4you.hotel4you.Model.CartaoMagnetico;
import com.hotel4you.hotel4you.Model.Reserva;
import com.hotel4you.hotel4you.Model.enums.EstadoReserva;
import com.hotel4you.hotel4you.Utils.Navegador;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.time.format.DateTimeFormatter;

/**
 * ViewController do ecrã de check-in e check-out ({@code CheckInCheckOutView.fxml}).
 * <p>
 * Escolher uma reserva na lista e carregar em "Fazer check-in" (reservas pendentes) ou em
 * "Fazer check-out" (reservas com check-in). Só lê a escolha e mostra mensagens; as regras
 * (estados, datas, cartão, permissões) estão no {@link CheckInCheckOutController}.
 * <p>
 * Apoio de IA: Claude (Anthropic), revisto pela equipa.
 */
public class CheckInCheckOutViewController {

    private static final String COR_ERRO = "-fx-text-fill: #b03a2e;";
    private static final String COR_SUCESSO = "-fx-text-fill: #1e8449;";
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CheckInCheckOutController controller;
    private final GestaoReservasController reservasController;

    @FXML
    private TextField campoPesquisa;
    @FXML
    private ListView<Reserva> listaReservas;
    @FXML
    private Label labelMensagem;

    /**
     * @param controller         controller que faz o check-in e o check-out
     * @param reservasController controller usado para listar as reservas
     */
    public CheckInCheckOutViewController(CheckInCheckOutController controller,
                                         GestaoReservasController reservasController) {
        this.controller = controller;
        this.reservasController = reservasController;
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
        atualizarLista();
    }

    /** Chamado pelo botão "Fazer check-in". */
    @FXML
    private void onCheckIn() {
        Reserva reserva = listaReservas.getSelectionModel().getSelectedItem();
        if (reserva == null) {
            mostrar("Escolha uma reserva na lista.", COR_ERRO);
            return;
        }
        try {
            CartaoMagnetico cartao = controller.fazerCheckIn(reserva.getId());
            mostrar("Check-in efetuado na reserva n.º " + reserva.getId() + ". Cartão atribuído: n.º "
                    + cartao.getNumero() + " (" + reserva.getQuarto() + ").", COR_SUCESSO);
            atualizarLista();
        } catch (IllegalArgumentException | SecurityException e) {
            mostrar(e.getMessage(), COR_ERRO);
        } catch (RuntimeException e) {
            mostrar("Não foi possível fazer o check-in: " + e.getMessage(), COR_ERRO);
        }
    }

    /** Chamado pelo botão "Fazer check-out". */
    @FXML
    private void onCheckOut() {
        Reserva reserva = listaReservas.getSelectionModel().getSelectedItem();
        if (reserva == null) {
            mostrar("Escolha uma reserva na lista.", COR_ERRO);
            return;
        }
        try {
            Reserva atualizada = controller.fazerCheckOut(reserva.getId());
            String cartao = atualizada.getCartao() == null
                    ? "" : "O cartão n.º " + atualizada.getCartao().getNumero() + " foi desativado. ";
            mostrar("Check-out efetuado na reserva n.º " + atualizada.getId() + ". " + cartao
                    + "O " + atualizada.getQuarto() + " ficou livre.", COR_SUCESSO);
            atualizarLista();
        } catch (IllegalArgumentException | SecurityException e) {
            mostrar(e.getMessage(), COR_ERRO);
        } catch (RuntimeException e) {
            mostrar("Não foi possível fazer o check-out: " + e.getMessage(), COR_ERRO);
        }
    }

    /** Chamado pelo botão "Voltar ao menu". */
    @FXML
    private void onVoltar() {
        Navegador.mudarPara("MenuPrincipalView.fxml", "Hotel4You");
    }

    private void atualizarLista() {
        try {
            listaReservas.setItems(FXCollections.observableArrayList(
                    reservasController.consultarReservas(campoPesquisa.getText())));
        } catch (RuntimeException e) {
            mostrar(e.getMessage(), COR_ERRO);
        }
    }

    private void mostrar(String texto, String estilo) {
        labelMensagem.setText(texto);
        labelMensagem.setStyle(estilo);
    }

    private static String descrever(Reserva reserva) {
        String cartao = reserva.getCartao() == null ? "" : "  |  Cartão " + reserva.getCartao().getNumero()
                                                           + (reserva.getCartao().isAtivo() ? "" : " (desativado)");
        return "N.º " + reserva.getId() + "  |  " + reserva.getCliente().getNome() + "  |  " + reserva.getQuarto()
                + "  |  " + reserva.getDataEntrada().format(FORMATO_DATA) + " a "
                + reserva.getDataSaida().format(FORMATO_DATA)
                + "  |  " + descreverEstado(reserva.getEstado()) + cartao;
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