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
 * ViewController do ecrã de reservas ativas ({@code ReservasAtivasView.fxml}). As consultas correm em
 * segundo plano (S6-01), para o ecrã não bloquear enquanto espera pela base de dados.
 */
public class ReservasAtivasViewController {
    private static final int POR_PAGINA = 10;

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
        EstadoReserva estado = estadoEscolhido();
        int paginaPedida = pagina;
        aCarregar(true);
        SegundoPlano.executar("Reservas ativas",
                () -> carregarPagina(estado, paginaPedida),
                this::mostrar,
                erro -> {
                    aCarregar(false);
                    labelMensagem.setText(textoDoErro(erro));
                });
    }

    // Corre em segundo plano: não pode mexer no ecrã.
    private Pagina<Reserva> carregarPagina(EstadoReserva estado, int paginaPedida) {
        int total = controller.contarReservasFiltradas(false, estado);
        int totalPaginas = Math.max(1, (total + POR_PAGINA - 1) / POR_PAGINA);
        int paginaValida = Math.min(paginaPedida, totalPaginas);
        List<Reserva> reservas = controller.listarReservasFiltradas(false, estado, paginaValida);
        return new Pagina<>(reservas, paginaValida - 1, POR_PAGINA, total);
    }

    // Corre na thread do JavaFX, com a página já lida.
    private void mostrar(Pagina<Reserva> resultado) {
        pagina = resultado.numero() + 1;
        listaReservas.getItems().clear();
        for (Reserva reserva : resultado.itens()) {
            String linha = "Reserva " + reserva.getId()
                    + " | " + reserva.getCliente().getNome()
                    + " | Quarto " + reserva.getQuarto().getNumero()
                    + " | " + reserva.getDataEntrada() + " a " + reserva.getDataSaida()
                    + " | " + reserva.getEstado();
            listaReservas.getItems().add(linha);
        }
        labelPagina.setText("Página " + pagina + " de " + resultado.totalPaginas()
                + " (" + resultado.total() + " reservas)");
        aCarregar(false);
        botaoAnterior.setDisable(!resultado.temAnterior());
        botaoSeguinte.setDisable(!resultado.temSeguinte());
        if (resultado.itens().isEmpty()) labelMensagem.setText("Não existem reservas com este filtro.");
        else labelMensagem.setText("");
    }

    // Enquanto a consulta corre, mostra uma mensagem e não deixa mudar de página nem de filtro.
    private void aCarregar(boolean sim) {
        filtroEstado.setDisable(sim);
        botaoAnterior.setDisable(sim);
        botaoSeguinte.setDisable(sim);
        if (sim) labelMensagem.setText("A carregar...");
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