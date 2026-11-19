
package com.hotel4you.hotel4you.View;

import com.hotel4you.hotel4you.Controller.GestaoReservasController;
import com.hotel4you.hotel4you.Model.Pagina;
import com.hotel4you.hotel4you.Model.Reserva;
import com.hotel4you.hotel4you.Model.enums.EstadoReserva;
import com.hotel4you.hotel4you.Utils.Navegador;
import com.hotel4you.hotel4you.Utils.SegundoPlano;

import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;

/**
 * ViewController do ecrã do histórico de reservas ({@code HistoricoReservasView.fxml}). As consultas
 * correm em segundo plano (S6-01), para o ecrã não bloquear enquanto espera pela base de dados.
 */
public class HistoricoReservasViewController {

    private static final int POR_PAGINA = 10;

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
    // A consulta corre em segundo plano e o ecrã é atualizado quando a resposta chega.
    @FXML
    private void atualizar() {

        EstadoReserva filtro = filtroAtual;
        boolean comFiltro = filtroAplicado;
        int paginaPedida = pagina;

        aCarregar(true);

        SegundoPlano.executar(
                "Histórico de reservas",
                () -> carregarPagina(filtro, comFiltro, paginaPedida),
                this::mostrar,
                erro -> {

                    aCarregar(false);

                    listaReservas.getItems().clear();

                    labelMensagem.setText(textoDoErro(erro));
                }
        );
    }

    // Corre em segundo plano: conta e lê a página pedida (não pode mexer no ecrã).
    private Pagina<Reserva> carregarPagina(EstadoReserva filtro, boolean comFiltro, int paginaPedida) {

        int total = controller.contarHistoricoComFiltro(filtro, comFiltro);

        int totalPaginas = Math.max(1, (total + POR_PAGINA - 1) / POR_PAGINA);

        int paginaValida = Math.min(paginaPedida, totalPaginas);

        List<Reserva> reservas =
                controller.listarHistoricoComFiltro(
                        filtro,
                        comFiltro,
                        paginaValida
                );

        return new Pagina<>(reservas, paginaValida - 1, POR_PAGINA, total);
    }

    // Corre na thread do JavaFX: mostra a página já lida.
    private void mostrar(Pagina<Reserva> resultado) {

        pagina = resultado.numero() + 1;

        listaReservas.getItems().clear();

        for (Reserva reserva : resultado.itens()) {

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
                        + " de " + resultado.totalPaginas()
                        + " (" + resultado.total() + " reservas)"
        );

        aCarregar(false);

        botaoAnterior.setDisable(!resultado.temAnterior());

        botaoSeguinte.setDisable(!resultado.temSeguinte());

        if (resultado.itens().isEmpty()) {

            labelMensagem.setText(
                    "Não existem reservas com este filtro."
            );

        } else {

            labelMensagem.setText("");
        }
    }

    // Enquanto a consulta corre, mostra uma mensagem e não deixa mudar de página nem de filtro.
    private void aCarregar(boolean sim) {

        filtroEstado.setDisable(sim);

        botaoAnterior.setDisable(sim);

        botaoSeguinte.setDisable(sim);

        if (sim) {
            labelMensagem.setText("A carregar...");
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
