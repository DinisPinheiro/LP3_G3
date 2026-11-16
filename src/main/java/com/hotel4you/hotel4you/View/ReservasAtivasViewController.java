package com.hotel4you.hotel4you.View;

import com.hotel4you.hotel4you.Controller.GestaoReservasController;
import com.hotel4you.hotel4you.Model.Reserva;
import com.hotel4you.hotel4you.Model.enums.EstadoReserva;
import com.hotel4you.hotel4you.Utils.Navegador;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.List;

public class ReservasAtivasViewController {
    private final GestaoReservasController controller;
    private int pagina = 1;

    @FXML private ComboBox<String> filtroEstado;
    @FXML private ListView<String> listaReservas;
    @FXML private Label labelPagina;
    @FXML private Label labelMensagem;
    @FXML private Button botaoAnterior;
    @FXML private Button botaoSeguinte;

    public ReservasAtivasViewController(GestaoReservasController controller) {
        this.controller = controller;
    }

    @FXML
    private void initialize() {
        filtroEstado.getItems().addAll("Todos", "Reservada", "Check-in efetuado");
        filtroEstado.getSelectionModel().selectFirst();
        filtroEstado.setOnAction(evento -> {
            pagina = 1;
            atualizar();
        });
        atualizar();
    }

    private EstadoReserva estadoEscolhido() {
        int indice = filtroEstado.getSelectionModel().getSelectedIndex();
        if (indice == 1) return EstadoReserva.PENDENTE;
        if (indice == 2) return EstadoReserva.CHECK_IN;
        return null;
    }

    @FXML
    private void atualizar() {
        try {
            int total = controller.contarReservasFiltradas(false, estadoEscolhido());
            int totalPaginas = Math.max(1, (total + 9) / 10);
            if (pagina > totalPaginas) pagina = totalPaginas;
            List<Reserva> reservas = controller.listarReservasFiltradas(false, estadoEscolhido(), pagina);
            listaReservas.getItems().clear();
            for (Reserva reserva : reservas) {
                String linha = "Reserva " + reserva.getId()
                        + " | " + reserva.getCliente().getNome()
                        + " | Quarto " + reserva.getQuarto().getNumero()
                        + " | " + reserva.getDataEntrada() + " a " + reserva.getDataSaida()
                        + " | " + reserva.getEstado();
                listaReservas.getItems().add(linha);
            }
            labelPagina.setText("Página " + pagina + " de " + totalPaginas + " (" + total + " reservas)");
            botaoAnterior.setDisable(pagina <= 1);
            botaoSeguinte.setDisable(pagina >= totalPaginas);
            if (reservas.isEmpty()) labelMensagem.setText("Não existem reservas com este filtro.");
            else labelMensagem.setText("");
        } catch (RuntimeException e) {
            labelMensagem.setText(textoDoErro(e));
        }
    }

    // Se o erro não tiver texto, mostra uma mensagem para o ecrã não ficar em branco.
    private String textoDoErro(RuntimeException e) {
        if (e.getMessage() == null) {
            return "Não foi possível consultar as reservas.";
        }
        return e.getMessage();
    }

    @FXML
    private void anterior() {
        if (pagina > 1) {
            pagina--;
            atualizar();
        }
    }

    @FXML
    private void seguinte() {
        if (!botaoSeguinte.isDisabled()) {
            pagina++;
            atualizar();
        }
    }

    @FXML
    private void voltar() {
        Navegador.mudarPara("MenuPrincipalView.fxml", "Hotel4You");
    }
}