package com.hotel4you.hotel4you.View;

import com.hotel4you.hotel4you.Controller.GestaoStockController;
import com.hotel4you.hotel4you.Model.Artigo;
import com.hotel4you.hotel4you.Utils.Navegador;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

/**
 * ViewController do ecrã de consulta do stock ({@code ConsultaStockView.fxml}, S4-04).
 * <p>
 * Só mostra o que o {@link GestaoStockController} devolve; daqui vai-se para a entrada de stock por XML.
 */
public class ConsultaStockViewController {

    private static final String COR_ERRO = "-fx-text-fill: #b03a2e;";

    private final GestaoStockController controller;

    @FXML
    private TableView<Artigo> tabelaStock;
    @FXML
    private TableColumn<Artigo, String> colunaArtigo;
    @FXML
    private TableColumn<Artigo, String> colunaQuantidade;
    @FXML
    private TableColumn<Artigo, String> colunaUnidade;
    @FXML
    private TableColumn<Artigo, String> colunaFornecedor;
    @FXML
    private Label labelMensagem;

    /**
     * @param controller controller que consulta o stock
     */
    public ConsultaStockViewController(GestaoStockController controller) {
        this.controller = controller;
    }

    /** Chamado pelo JavaFX depois de os campos {@code @FXML} serem preenchidos. */
    @FXML
    private void initialize() {
        colunaArtigo.setCellValueFactory(c -> texto(c.getValue().getNome()));
        colunaQuantidade.setCellValueFactory(c -> texto(String.valueOf(c.getValue().getQuantidadeAtual())));
        colunaUnidade.setCellValueFactory(c -> texto(c.getValue().getUnidade()));
        colunaFornecedor.setCellValueFactory(c -> texto(c.getValue().getFornecedor() == null
                ? "" : c.getValue().getFornecedor().getNome()));
        tabelaStock.setPlaceholder(new Label("Ainda não há artigos em stock."));
        onAtualizar();
    }

    /** Chamado pelo botão "Atualizar" e ao abrir o ecrã. */
    @FXML
    private void onAtualizar() {
        try {
            tabelaStock.setItems(FXCollections.observableArrayList(controller.consultarStock()));
            labelMensagem.setText("");
        } catch (RuntimeException e) {
            labelMensagem.setText(e.getMessage());
            labelMensagem.setStyle(COR_ERRO);
        }
    }

    /** Chamado pelo botão "Entrada de stock (XML do fornecedor)". */
    @FXML
    private void onEntradaXml() {
        Navegador.mudarPara("ValidacaoXmlView.fxml", "Hotel4You - Entrada de stock");
    }

    /** Chamado pelo botão "Voltar ao menu". */
    @FXML
    private void onVoltar() {
        Navegador.mudarPara("MenuPrincipalView.fxml", "Hotel4You");
    }

    private static SimpleStringProperty texto(String valor) {
        return new SimpleStringProperty(valor);
    }
}
