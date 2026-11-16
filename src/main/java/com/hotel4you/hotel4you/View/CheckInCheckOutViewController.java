package com.hotel4you.hotel4you.View;

import com.hotel4you.hotel4you.Controller.CheckInCheckOutController;
import com.hotel4you.hotel4you.Controller.GestaoReservasController;
import com.hotel4you.hotel4you.Model.CartaoMagnetico;
import com.hotel4you.hotel4you.Model.Reserva;
import com.hotel4you.hotel4you.Model.enums.EstadoReserva;
import com.hotel4you.hotel4you.Utils.Navegador;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CheckInCheckOutViewController {

    private static final String COR_ERRO = "-fx-text-fill: #b03a2e;";
    private static final String COR_SUCESSO = "-fx-text-fill: #1e8449;";
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final CheckInCheckOutController controller;
    private final GestaoReservasController reservasController;

    // Paginação
    private int pagina = 1;
    private final int TAMANHO_PAGINA = 10;
    @FXML
    private TextField campoPesquisa;
    @FXML
    private ListView<Reserva> listaReservas;
    @FXML
    private Label labelMensagem;
    @FXML
    private Button botaoAnterior;
    @FXML
    private Button botaoSeguinte;
    @FXML
    private Label labelPagina;

    public CheckInCheckOutViewController(CheckInCheckOutController controller, GestaoReservasController reservasController) {
        this.controller = controller;
        this.reservasController = reservasController;
    }

    @FXML
    private void initialize() {
        listaReservas.setCellFactory(lista -> new ListCell<Reserva>() {
            @Override
            protected void updateItem(Reserva reserva, boolean vazio) {
                super.updateItem(reserva, vazio);

                if (vazio || reserva == null) {
                    setText(null);
                }

                else {
                    setText(descrever(reserva));
                }
            }
        });

        // Ao pesquisar, volta à primeira página.
        campoPesquisa.textProperty().addListener((observavel, antigo, novo) -> {
                    pagina = 1;
                    atualizarLista();
                });

        atualizarLista();
    }

    // Fazer check-in
    @FXML
    private void onCheckIn() {
        Reserva reserva = listaReservas.getSelectionModel().getSelectedItem();
        if (reserva == null) {
            mostrar("Escolha uma reserva na lista.", COR_ERRO);
            return;
        }
        try {
            CartaoMagnetico cartao = controller.fazerCheckIn(reserva.getId());
            mostrar("Check-in efetuado na reserva n.º " + reserva.getId() + ". Cartão atribuído: n.º " + cartao.getNumero() + " (" + reserva.getQuarto() + ").", COR_SUCESSO);
            atualizarLista();

        }

        catch (IllegalArgumentException | SecurityException e) {
            mostrar(e.getMessage(), COR_ERRO);

        }

        catch (RuntimeException e) {
            mostrar("Não foi possível fazer o check-in: " + e.getMessage(), COR_ERRO);
        }
    }

    // Fazer check-out
    @FXML
    private void onCheckOut() {
        Reserva reserva = listaReservas.getSelectionModel().getSelectedItem();
        if (reserva == null) {
            mostrar("Escolha uma reserva na lista.", COR_ERRO);
            return;
        }
        try {
            Reserva atualizada = controller.fazerCheckOut(reserva.getId());
            String cartao = "";

            if (atualizada.getCartao() != null) {
                cartao = "O cartão n.º " + atualizada.getCartao().getNumero() + " foi desativado. ";
            }

            mostrar("Check-out efetuado na reserva n.º " + atualizada.getId() + ". " + cartao + "O " + atualizada.getQuarto() + " ficou livre.", COR_SUCESSO);
            atualizarLista();

        }

        catch (IllegalArgumentException | SecurityException e) {
            mostrar(e.getMessage(), COR_ERRO);

        }

        catch (RuntimeException e) {
            mostrar("Não foi possível fazer o check-out: " + e.getMessage(), COR_ERRO);
        }
    }

    // Voltar ao menu
    @FXML
    private void onVoltar() {
        Navegador.mudarPara("MenuPrincipalView.fxml", "Hotel4You");
    }

    // Atualizar lista com paginação
    private void atualizarLista() {
        try {

            List<Reserva> reservas = reservasController.consultarReservas(campoPesquisa.getText());

            int totalReservas = reservas.size();
            int totalPaginas = (totalReservas + TAMANHO_PAGINA - 1) / TAMANHO_PAGINA;

            if (totalPaginas == 0) {
                totalPaginas = 1;
            }

            if (pagina > totalPaginas) {
                pagina = totalPaginas;
            }

            int inicio = (pagina - 1) * TAMANHO_PAGINA;
            int fim = inicio + TAMANHO_PAGINA;

            if (fim > totalReservas) {
                fim = totalReservas;
            }

            List<Reserva> reservasPagina = new ArrayList<>();

            for (int i = inicio; i < fim; i++) {
                reservasPagina.add(reservas.get(i));
            }

            listaReservas.setItems(FXCollections.observableArrayList(reservasPagina));
            labelPagina.setText("Página " + pagina + " de " + totalPaginas + " (" + totalReservas + " reservas)");
            botaoAnterior.setDisable(pagina <= 1);
            botaoSeguinte.setDisable(pagina >= totalPaginas);

        }

        catch (RuntimeException e) {
            mostrar(e.getMessage(), COR_ERRO);
        }
    }

    // Página anterior
    @FXML
    private void anterior() {
        if (pagina > 1) {
            pagina--;
            atualizarLista();
        }
    }

    // Página seguinte
    @FXML
    private void seguinte() {
        if (!botaoSeguinte.isDisabled()) {
            pagina++;
            atualizarLista();
        }
    }

    private void mostrar(String texto, String estilo) {
        labelMensagem.setText(texto);
        labelMensagem.setStyle(estilo);
    }

    private static String descrever(Reserva reserva) {
        String cartao = "";

        if (reserva.getCartao() != null) {
            cartao = "  |  Cartão " + reserva.getCartao().getNumero();

            if (!reserva.getCartao().isAtivo()) {
                cartao = cartao + " (desativado)";
            }
        }

        return "N.º " + reserva.getId() + "  |  " + reserva.getCliente().getNome() + "  |  " + reserva.getQuarto() + "  |  " + reserva.getDataEntrada().format(FORMATO_DATA) + " a " + reserva.getDataSaida().format(FORMATO_DATA) + "  |  " + descreverEstado(reserva.getEstado()) + cartao;
    }

    private static String descreverEstado(EstadoReserva estado) {
        switch (estado) {
            case PENDENTE:
                return "Reservado (pendente de check-in)";
            case CHECK_IN:
                return "Check-in efetuado";
            case CHECK_OUT:
                return "Check-out efetuado";
            case CANCELADA:
                return "Cancelada";
            default:
                return "";
        }
    }
}