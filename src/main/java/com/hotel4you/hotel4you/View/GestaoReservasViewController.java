package com.hotel4you.hotel4you.View;

import com.hotel4you.hotel4you.Controller.GestaoClientesController;
import com.hotel4you.hotel4you.Controller.GestaoReservasController;
import com.hotel4you.hotel4you.Model.Cliente;
import com.hotel4you.hotel4you.Model.Quarto;
import com.hotel4you.hotel4you.Model.Reserva;
import com.hotel4you.hotel4you.Model.enums.EstadoReserva;
import com.hotel4you.hotel4you.Model.enums.TipoQuarto;
import com.hotel4you.hotel4you.Utils.Navegador;
import com.hotel4you.hotel4you.Utils.Utils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * ViewController do ecrã de criação de reservas ({@code GestaoReservasView.fxml}).
 * <p>
 * Passos: escolher o cliente, indicar as datas (dd/mm/aaaa) e o tipo de quarto, ver os quartos
 * disponíveis, escolher um e criar a reserva. Só lê os campos e mostra mensagens; as regras
 * (disponibilidade, datas, valor, permissões) estão no {@link GestaoReservasController}.
 * Escrever "0" num campo de data e carregar Enter (ou o botão Limpar) cancela o que estava a ser preenchido.
 * <p>
 * Apoio de IA: Claude (Anthropic), revisto pela equipa.
 */
public class GestaoReservasViewController {

    private static final String COR_ERRO = "-fx-text-fill: #b03a2e;";
    private static final String COR_SUCESSO = "-fx-text-fill: #1e8449;";
    private static final String COR_NEUTRA = "-fx-text-fill: #555555;";
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final GestaoReservasController controller;
    private final GestaoClientesController clientesController;

    @FXML
    private TextField campoPesquisaCliente;
    @FXML
    private ListView<Cliente> listaClientes;
    @FXML
    private TextField campoEntrada;
    @FXML
    private TextField campoSaida;
    @FXML
    private ComboBox<TipoQuarto> campoTipo;
    @FXML
    private ListView<Quarto> listaQuartos;
    @FXML
    private Label labelValor;
    @FXML
    private ListView<Reserva> listaReservas;
    @FXML
    private Label labelMensagem;

    /**
     * @param controller         controller que cria e consulta reservas
     * @param clientesController controller usado para procurar o cliente da reserva
     */
    public GestaoReservasViewController(GestaoReservasController controller,
                                        GestaoClientesController clientesController) {
        this.controller = controller;
        this.clientesController = clientesController;
    }

    /** Chamado pelo JavaFX depois de os campos {@code @FXML} serem preenchidos. */
    @FXML
    private void initialize() {
        campoTipo.setItems(FXCollections.observableArrayList(TipoQuarto.values()));
        campoTipo.setConverter(new StringConverter<TipoQuarto>() {
            @Override
            public String toString(TipoQuarto tipo) {
                return tipo == null ? "" : tipo.getNome();
            }

            @Override
            public TipoQuarto fromString(String texto) {
                return null;
            }
        });
        listaClientes.setCellFactory(lista -> new ListCell<Cliente>() {
            @Override
            protected void updateItem(Cliente cliente, boolean vazio) {
                super.updateItem(cliente, vazio);
                setText(vazio || cliente == null ? null : cliente.getNome() + "  |  NIF/Doc: " + cliente.getDocumento());
            }
        });
        listaQuartos.setCellFactory(lista -> new ListCell<Quarto>() {
            @Override
            protected void updateItem(Quarto quarto, boolean vazio) {
                super.updateItem(quarto, vazio);
                setText(vazio || quarto == null ? null : descrever(quarto));
            }
        });
        listaReservas.setCellFactory(lista -> new ListCell<Reserva>() {
            @Override
            protected void updateItem(Reserva reserva, boolean vazio) {
                super.updateItem(reserva, vazio);
                setText(vazio || reserva == null ? null : descrever(reserva));
            }
        });
        campoPesquisaCliente.textProperty().addListener((observavel, antigo, novo) -> atualizarClientes());
        // Se as datas ou o tipo mudam, a lista de quartos disponíveis deixa de estar certa.
        campoEntrada.textProperty().addListener((observavel, antigo, novo) -> limparQuartos());
        campoSaida.textProperty().addListener((observavel, antigo, novo) -> limparQuartos());
        campoTipo.setOnAction(evento -> limparQuartos());
        listaQuartos.getSelectionModel().selectedItemProperty()
                .addListener((observavel, antigo, novo) -> atualizarValor());
        atualizarClientes();
        atualizarReservas();
    }

    /** Chamado pelo botão "Ver quartos disponíveis". */
    @FXML
    private void onVerQuartos() {
        if (pediuCancelamento()) {
            onLimpar();
            return;
        }
        Optional<LocalDate> entrada = Utils.converterData(campoEntrada.getText());
        Optional<LocalDate> saida = Utils.converterData(campoSaida.getText());
        if (entrada.isEmpty() || saida.isEmpty()) {
            mostrar("Datas inválidas. Use o formato dd/mm/aaaa, por exemplo 15/12/2026.", COR_ERRO);
            return;
        }
        try {
            List<Quarto> disponiveis = controller.consultarQuartosDisponiveis(
                    campoTipo.getValue(), entrada.get(), saida.get());
            listaQuartos.setItems(FXCollections.observableArrayList(disponiveis));
            if (disponiveis.isEmpty()) {
                mostrar("Não há quartos disponíveis nesse período.", COR_ERRO);
            } else {
                mostrar(disponiveis.size() + " quarto(s) disponível(eis). Escolha um na lista.", COR_NEUTRA);
            }
        } catch (IllegalArgumentException | SecurityException e) {
            mostrar(e.getMessage(), COR_ERRO);
        } catch (RuntimeException e) {
            mostrar("Não foi possível consultar os quartos: " + e.getMessage(), COR_ERRO);
        }
    }

    /** Chamado pelo botão "Criar reserva". */
    @FXML
    private void onCriar() {
        if (pediuCancelamento()) {
            onLimpar();
            return;
        }
        Cliente cliente = listaClientes.getSelectionModel().getSelectedItem();
        Quarto quarto = listaQuartos.getSelectionModel().getSelectedItem();
        if (cliente == null) {
            mostrar("Escolha um cliente na lista.", COR_ERRO);
            return;
        }
        Optional<LocalDate> entrada = Utils.converterData(campoEntrada.getText());
        Optional<LocalDate> saida = Utils.converterData(campoSaida.getText());
        if (entrada.isEmpty() || saida.isEmpty()) {
            mostrar("Datas inválidas. Use o formato dd/mm/aaaa, por exemplo 15/12/2026.", COR_ERRO);
            return;
        }
        if (quarto == null) {
            mostrar("Escolha um quarto disponível. Use primeiro \"Ver quartos disponíveis\".", COR_ERRO);
            return;
        }
        try {
            Reserva reserva = controller.criarReserva(cliente, quarto, entrada.get(), saida.get());
            mostrar("Reserva n.º " + reserva.getId() + " criada: " + cliente.getNome() + ", " + quarto + ", de "
                    + entrada.get().format(FORMATO_DATA) + " a " + saida.get().format(FORMATO_DATA)
                    + ". Total estimado: " + Utils.formatarPreco(reserva.calcularTotal())
                    + ". Estado: " + descreverEstado(reserva.getEstado()) + ".", COR_SUCESSO);
            limparQuartos();
            atualizarReservas();
        } catch (IllegalArgumentException | SecurityException e) {
            mostrar(e.getMessage(), COR_ERRO);
        } catch (RuntimeException e) {
            mostrar("Não foi possível criar a reserva: " + e.getMessage(), COR_ERRO);
        }
    }

    /** Chamado pelo botão "Limpar": apaga as datas, o tipo, a pesquisa e a escolha de quarto. */
    @FXML
    private void onLimpar() {
        campoEntrada.clear();
        campoSaida.clear();
        campoTipo.setValue(null);
        campoPesquisaCliente.clear();
        limparQuartos();
        mostrar("", COR_NEUTRA);
    }

    /** Chamado pelo botão "Voltar ao menu". */
    @FXML
    private void onVoltar() {
        Navegador.mudarPara("MenuPrincipalView.fxml", "Hotel4You");
    }

    private boolean pediuCancelamento() {
        return Utils.eCancelamento(campoEntrada.getText()) || Utils.eCancelamento(campoSaida.getText());
    }

    private void limparQuartos() {
        listaQuartos.setItems(FXCollections.observableArrayList());
        labelValor.setText("");
    }

    /** Mostra o valor total estimado do quarto escolhido, se as datas já estiverem escritas. */
    private void atualizarValor() {
        Quarto quarto = listaQuartos.getSelectionModel().getSelectedItem();
        Optional<LocalDate> entrada = Utils.converterData(campoEntrada.getText());
        Optional<LocalDate> saida = Utils.converterData(campoSaida.getText());
        if (quarto == null || entrada.isEmpty() || saida.isEmpty()) {
            labelValor.setText("");
            return;
        }
        try {
            BigDecimal total = controller.estimarValor(quarto, entrada.get(), saida.get());
            long noites = ChronoUnit.DAYS.between(entrada.get(), saida.get());
            labelValor.setText("Valor total estimado: " + Utils.formatarPreco(total) + "  (" + noites
                    + " noite(s) x " + Utils.formatarPreco(quarto.precoNoite()) + ")");
        } catch (IllegalArgumentException e) {
            labelValor.setText("");
        }
    }

    private void atualizarClientes() {
        try {
            listaClientes.setItems(FXCollections.observableArrayList(
                    clientesController.consultarClientes(campoPesquisaCliente.getText())));
        } catch (RuntimeException e) {
            mostrar(e.getMessage(), COR_ERRO);
        }
    }

    private void atualizarReservas() {
        try {
            listaReservas.setItems(FXCollections.observableArrayList(controller.consultarReservas()));
        } catch (RuntimeException e) {
            mostrar(e.getMessage(), COR_ERRO);
        }
    }

    private void mostrar(String texto, String estilo) {
        labelMensagem.setText(texto);
        labelMensagem.setStyle(estilo);
    }

    private static String descrever(Quarto quarto) {
        return quarto + "  |  " + quarto.getVista().getNome() + "  |  "
                + Utils.formatarPreco(quarto.precoNoite()) + " por noite";
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