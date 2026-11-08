package com.hotel4you.hotel4you.View;

import com.hotel4you.hotel4you.Controller.GestaoVistasController;
import com.hotel4you.hotel4you.Model.enums.Vista;
import com.hotel4you.hotel4you.Utils.Navegador;
import com.hotel4you.hotel4you.Utils.Utils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * ViewController do ecrã dos valores das vistas especiais ({@code GestaoVistasView.fxml}).
 * <p>
 * O gestor escolhe a vista (mar ou montanha) na lista, escreve o novo valor e guarda. Só lê o campo e
 * mostra mensagens; as regras e a permissão estão no {@link GestaoVistasController}.
 * <p>
 */
public class GestaoVistasViewController {

    private static final String COR_ERRO = "-fx-text-fill: #b03a2e;";
    private static final String COR_SUCESSO = "-fx-text-fill: #1e8449;";

    private final GestaoVistasController controller;
    /** Valores em vigor, para escrever na lista. */
    private Map<Vista, BigDecimal> valores = new LinkedHashMap<>();

    @FXML
    private ListView<Vista> listaVistas;
    @FXML
    private Label labelMensagem;
    @FXML
    private Label labelModo;
    @FXML
    private TextField campoValor;

    /**
     * @param controller controller que edita os valores das vistas
     */
    public GestaoVistasViewController(GestaoVistasController controller) {
        this.controller = controller;
    }

    /** Chamado pelo JavaFX depois de os campos {@code @FXML} serem preenchidos. */
    @FXML
    private void initialize() {
        listaVistas.setCellFactory(lista -> new ListCell<Vista>() {
            @Override
            protected void updateItem(Vista vista, boolean vazio) {
                super.updateItem(vista, vazio);
                setText(vazio || vista == null ? null
                        : vista.getNome() + "  |  + " + Utils.formatarPreco(valores.get(vista)) + " por noite");
            }
        });
        listaVistas.getSelectionModel().selectedItemProperty().addListener((observavel, antigo, nova) -> {
            if (nova != null) {
                labelMensagem.setText("");
                campoValor.setText(valores.get(nova).toPlainString().replace('.', ','));
                labelModo.setText("A editar: " + nova.getNome());
            }
        });
        atualizarLista();
        labelModo.setText("Escolha uma vista");
    }

    /** Chamado pelo botão "Guardar". */
    @FXML
    private void onGuardar() {
        Vista vista = listaVistas.getSelectionModel().getSelectedItem();
        if (vista == null) {
            mostrar("Escolha primeiro uma vista na lista.", COR_ERRO);
            return;
        }
        Optional<BigDecimal> valor = Utils.converterDecimal(campoValor.getText());
        if (valor.isEmpty()) {
            mostrar("Valor inválido. Use um número, por exemplo 20,00.", COR_ERRO);
            return;
        }
        try {
            controller.editarValor(vista, valor.get());
            atualizarLista();
            listaVistas.getSelectionModel().select(vista);
            mostrar(vista.getNome() + " guardada.", COR_SUCESSO);
        } catch (IllegalArgumentException | SecurityException e) {
            mostrar(e.getMessage(), COR_ERRO);
        } catch (RuntimeException e) {
            mostrar("Não foi possível guardar: " + e.getMessage(), COR_ERRO);
        }
    }

    /** Chamado pelo botão "Voltar ao menu". */
    @FXML
    private void onVoltar() {
        Navegador.mudarPara("MenuPrincipalView.fxml", "Hotel4You");
    }

    private void atualizarLista() {
        try {
            valores = controller.consultarValores();
            listaVistas.setItems(FXCollections.observableArrayList(valores.keySet()));
        } catch (RuntimeException e) {
            mostrar(e.getMessage(), COR_ERRO);
        }
    }

    private void mostrar(String texto, String estilo) {
        labelMensagem.setText(texto);
        labelMensagem.setStyle(estilo);
    }
}
