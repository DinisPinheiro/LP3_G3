
package com.hotel4you.hotel4you.View;

import com.hotel4you.hotel4you.Controller.GestaoReservasController;
import com.hotel4you.hotel4you.Model.Reserva;
import com.hotel4you.hotel4you.Model.enums.EstadoReserva;
import com.hotel4you.hotel4you.Utils.Navegador;

import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;

public class HistoricoReservasViewController {

    private final GestaoReservasController controller;

    private int pagina = 1;

    // Guarda o estado escolhido.
    private EstadoReserva filtroAtual = null;

    // Indica se foi escolhido um filtro.
    private boolean filtroAplicado = false;

    @FXML
    private ComboBox<String> filtroEstado;

    @FXML
    private ListView<String> listaReservas;

    @FXML
    private Label labelPagina;

    @FXML
    private Label labelMensagem;

    @FXML
    private Button botaoAnterior;

    @FXML
    private Button botaoSeguinte;

    public HistoricoReservasViewController(
            GestaoReservasController controller) {

        this.controller = controller;
    }

    // Executado quando o ecrã abre.
    @FXML
    private void initialize() {

        filtroEstado.getItems().add("Todos");
        filtroEstado.getItems().add("Reservada");
        filtroEstado.getItems().add("Check-in efetuado");
        filtroEstado.getItems().add("Check-out efetuado");
        filtroEstado.getItems().add("Cancelada");

        filtroEstado.getSelectionModel().selectFirst();

        // Atualiza automaticamente ao escolher um estado.
        filtroEstado.setOnAction(evento -> {

            String estado = filtroEstado.getValue();

            filtroAplicado = true;

            if (estado == null || estado.equals("Todos")) {

                filtroAtual = null;

            } else if (estado.equals("Reservada")) {

                filtroAtual = EstadoReserva.PENDENTE;

            } else if (estado.equals("Check-in efetuado")) {

                filtroAtual = EstadoReserva.CHECK_IN;

            } else if (estado.equals("Check-out efetuado")) {

                filtroAtual = EstadoReserva.CHECK_OUT;

            } else if (estado.equals("Cancelada")) {

                filtroAtual = EstadoReserva.CANCELADA;
            }

            // Volta à primeira página.
            pagina = 1;

            atualizar();
        });

        // Mostra o histórico quando o ecrã abre.
        atualizar();
    }

    // Atualiza as reservas apresentadas.
    @FXML
    private void atualizar() {

        try {

            int total = controller.contarHistoricoComFiltro(
                    filtroAtual,
                    filtroAplicado
            );

            int totalPaginas = (total + 9) / 10;

            if (totalPaginas == 0) {
                totalPaginas = 1;
            }

            if (pagina > totalPaginas) {
                pagina = totalPaginas;
            }

            List<Reserva> reservas =
                    controller.listarHistoricoComFiltro(
                            filtroAtual,
                            filtroAplicado,
                            pagina
                    );

            listaReservas.getItems().clear();

            for (Reserva reserva : reservas) {

                String estado = "";

                if (reserva.getEstado() == EstadoReserva.PENDENTE) {
                    estado = "Reservada";
                }

                if (reserva.getEstado() == EstadoReserva.CHECK_IN) {
                    estado = "Check-in efetuado";
                }

                if (reserva.getEstado() == EstadoReserva.CHECK_OUT) {
                    estado = "Check-out efetuado";
                }

                if (reserva.getEstado() == EstadoReserva.CANCELADA) {
                    estado = "Cancelada";
                }

                String cliente = "Cliente indisponível";
                String quarto = "?";

                if (reserva.getCliente() != null) {
                    cliente = reserva.getCliente().getNome();
                }

                if (reserva.getQuarto() != null) {
                    quarto = String.valueOf(
                            reserva.getQuarto().getNumero()
                    );
                }

                String linha =
                        "Reserva " + reserva.getId()
                                + " | " + cliente
                                + " | Quarto " + quarto
                                + " | " + reserva.getDataEntrada()
                                + " a " + reserva.getDataSaida()
                                + " | " + estado;

                listaReservas.getItems().add(linha);
            }

            labelPagina.setText(
                    "Página " + pagina
                            + " de " + totalPaginas
                            + " (" + total + " reservas)"
            );

            botaoAnterior.setDisable(pagina <= 1);

            botaoSeguinte.setDisable(pagina >= totalPaginas);

            if (reservas.isEmpty()) {

                labelMensagem.setText(
                        "Não existem reservas com este filtro."
                );

            } else {

                labelMensagem.setText("");
            }

        } catch (RuntimeException e) {

            listaReservas.getItems().clear();

            labelMensagem.setText(textoDoErro(e));

            e.printStackTrace();
        }
    }

    // Mostra uma mensagem caso ocorra algum erro.
    private String textoDoErro(RuntimeException e) {

        if (e.getMessage() == null) {
            return "Não foi possível consultar as reservas.";
        }

        return e.getMessage();
    }

    // Página anterior.
    @FXML
    private void anterior() {

        if (pagina > 1) {

            pagina--;
            atualizar();
        }
    }

    // Página seguinte.
    @FXML
    private void seguinte() {

        if (!botaoSeguinte.isDisabled()) {

            pagina++;
            atualizar();
        }
    }

    // Voltar ao menu principal.
    @FXML
    private void voltar() {

        Navegador.mudarPara(
                "MenuPrincipalView.fxml",
                "Hotel4You"
        );
    }
}
