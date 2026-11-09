package com.hotel4you.hotel4you.View;

import com.hotel4you.hotel4you.Controller.GestaoReservasController;
import com.hotel4you.hotel4you.Model.Equipamento;
import com.hotel4you.hotel4you.Model.Quarto;
import com.hotel4you.hotel4you.Model.enums.TipoQuarto;
import com.hotel4you.hotel4you.Utils.Navegador;
import com.hotel4you.hotel4you.Utils.Utils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * ViewController do ecrã de consulta de disponibilidade de quartos
 * ({@code ConsultaDisponibilidadeView.fxml}).
 * <p>
 * O utilizador indica o intervalo de datas (no calendário ou escritas como dd/mm/aaaa) e, opcionalmente, o tipo de quarto
 * (individual, duplo ou família), e vê os quartos livres nesse período. Só lê os campos e mostra
 * mensagens; as regras (disponibilidade, datas, permissões) estão no {@link GestaoReservasController}.
 * O botão Limpar cancela o que estava a ser preenchido.
 */
public class ConsultaDisponibilidadeViewController {

    private static final String COR_ERRO = "-fx-text-fill: #b03a2e;";
    private static final String COR_NEUTRA = "-fx-text-fill: #555555;";
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final GestaoReservasController controller;

    @FXML
    private DatePicker campoEntrada;
    @FXML
    private DatePicker campoSaida;
    @FXML
    private ComboBox<TipoQuarto> campoTipo;
    @FXML
    private TableView<Quarto> tabelaQuartos;
    @FXML
    private TableColumn<Quarto, String> colunaNumero;
    @FXML
    private TableColumn<Quarto, String> colunaTipo;
    @FXML
    private TableColumn<Quarto, String> colunaCapacidade;
    @FXML
    private TableColumn<Quarto, String> colunaVista;
    @FXML
    private TableColumn<Quarto, String> colunaPreco;
    @FXML
    private TableColumn<Quarto, String> colunaEquipamentos;
    @FXML
    private Label labelMensagem;

    /**
     * @param controller controller que consulta a disponibilidade dos quartos
     */
    public ConsultaDisponibilidadeViewController(GestaoReservasController controller) {
        this.controller = controller;
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
        colunaNumero.setCellValueFactory(c -> texto(String.valueOf(c.getValue().getNumero())));
        colunaTipo.setCellValueFactory(c -> texto(c.getValue().getTipo().getNome()));
        colunaCapacidade.setCellValueFactory(c -> texto(descreverCapacidade(c.getValue().getTipo())));
        colunaVista.setCellValueFactory(c -> texto(c.getValue().getVista().getNome()));
        colunaPreco.setCellValueFactory(c -> texto(Utils.formatarPreco(c.getValue().precoNoite())));
        colunaEquipamentos.setCellValueFactory(c -> texto(c.getValue().getEquipamentos().stream()
                .map(Equipamento::getNome).collect(Collectors.joining(", "))));
        tabelaQuartos.setPlaceholder(new Label("Indique as datas e carregue em \"Consultar\"."));
        // Se as datas ou o tipo mudam, a lista mostrada deixa de estar certa.
        SeletorData.configurar(campoEntrada, 0, this::limparResultados);  // entrada: a partir de hoje
        SeletorData.configurar(campoSaida, 1, this::limparResultados);    // saída: a partir de amanhã
        campoTipo.setOnAction(evento -> limparResultados());
    }

    /** Chamado pelo botão "Consultar". */
    @FXML
    private void onConsultar() {
        if (pediuCancelamento()) {
            onLimpar();
            return;
        }
        Optional<LocalDate> entrada = SeletorData.ler(campoEntrada);
        Optional<LocalDate> saida = SeletorData.ler(campoSaida);
        if (entrada.isEmpty() || saida.isEmpty()) {
            mostrar("Datas inválidas. Escolha-as no calendário ou escreva-as no formato dd/mm/aaaa.", COR_ERRO);
            return;
        }
        try {
            List<Quarto> disponiveis = controller.consultarQuartosDisponiveis(
                    campoTipo.getValue(), entrada.get(), saida.get());
            tabelaQuartos.setItems(FXCollections.observableArrayList(disponiveis));
            String tipo = campoTipo.getValue() == null ? "de qualquer tipo"
                    : "do tipo " + campoTipo.getValue().getNome().toLowerCase();
            String periodo = entrada.get().format(FORMATO_DATA) + " a " + saida.get().format(FORMATO_DATA)
                    + " (" + ChronoUnit.DAYS.between(entrada.get(), saida.get()) + " noite(s))";
            if (disponiveis.isEmpty()) {
                mostrar("Não há quartos " + tipo + " disponíveis entre " + periodo + ".", COR_ERRO);
            } else {
                mostrar(disponiveis.size() + " quarto(s) " + tipo + " disponível(eis) entre " + periodo + ".",
                        COR_NEUTRA);
            }
        } catch (IllegalArgumentException | SecurityException e) {
            mostrar(e.getMessage(), COR_ERRO);
        } catch (RuntimeException e) {
            mostrar("Não foi possível consultar os quartos: " + e.getMessage(), COR_ERRO);
        }
    }

    /** Chamado pelo botão "Limpar": apaga as datas, o tipo e os resultados. */
    @FXML
    private void onLimpar() {
        SeletorData.limpar(campoEntrada);
        SeletorData.limpar(campoSaida);
        campoTipo.setValue(null);
        limparResultados();
    }

    /** Chamado pelo botão "Voltar ao menu". */
    @FXML
    private void onVoltar() {
        Navegador.mudarPara("MenuPrincipalView.fxml", "Hotel4You");
    }

    private boolean pediuCancelamento() {
        return Utils.eCancelamento(campoEntrada.getEditor().getText())
                || Utils.eCancelamento(campoSaida.getEditor().getText());
    }

    private void limparResultados() {
        tabelaQuartos.setItems(FXCollections.observableArrayList());
        mostrar("", COR_NEUTRA);
    }

    private void mostrar(String texto, String estilo) {
        labelMensagem.setText(texto);
        labelMensagem.setStyle(estilo);
    }

    private static SimpleStringProperty texto(String valor) {
        return new SimpleStringProperty(valor);
    }

    private static String descreverCapacidade(TipoQuarto tipo) {
        return tipo.getCapacidadeMinima() == tipo.getCapacidadeMaxima()
                ? String.valueOf(tipo.getCapacidadeMaxima())
                : tipo.getCapacidadeMinima() + " a " + tipo.getCapacidadeMaxima();
    }
}
