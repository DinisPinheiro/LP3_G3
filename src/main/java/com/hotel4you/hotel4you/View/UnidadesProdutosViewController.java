package com.hotel4you.hotel4you.View;

import com.hotel4you.hotel4you.Controller.GestaoStockController;
import com.hotel4you.hotel4you.Model.Artigo;
import com.hotel4you.hotel4you.Utils.SegundoPlano;
import com.hotel4you.hotel4you.Utils.Utils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.Optional;

/**
 * ViewController da janela de unidades dos produtos ({@code UnidadesProdutosView.fxml}, S7-02).
 * <p>
 * Mostra a unidade principal e a conversão de caixas de cada produto e deixa o gestor ou o funcionário
 * definir quantas unidades tem uma caixa. Só lê os campos e mostra mensagens; as regras estão no
 * {@link GestaoStockController} e no {@link Artigo}.
 */
public class UnidadesProdutosViewController {

    private static final String COR_ERRO = "-fx-text-fill: #b03a2e;";
    private static final String COR_SUCESSO = "-fx-text-fill: #1e8449;";

    private final GestaoStockController controller;

    @FXML
    private TableView<Artigo> tabelaProdutos;
    @FXML
    private TableColumn<Artigo, String> colunaProduto;
    @FXML
    private TableColumn<Artigo, String> colunaUnidade;
    @FXML
    private TableColumn<Artigo, String> colunaConversao;
    @FXML
    private TableColumn<Artigo, String> colunaStock;
    @FXML
    private TextField campoUnidadesPorCaixa;
    @FXML
    private Label labelMensagem;

    /**
     * @param controller controller do stock, que guarda as conversões
     */
    public UnidadesProdutosViewController(GestaoStockController controller) {
        this.controller = controller;
    }

    /** Chamado pelo JavaFX depois de os campos {@code @FXML} serem preenchidos. */
    @FXML
    private void initialize() {
        colunaProduto.setCellValueFactory(c -> texto(c.getValue().getNome()));
        colunaUnidade.setCellValueFactory(c -> texto(c.getValue().estaEmCaixas()
                ? "cx (por converter)" : c.getValue().getUnidade()));
        colunaConversao.setCellValueFactory(c -> texto(c.getValue().temConversaoCaixa()
                ? c.getValue().descreverConversao() : "não definida"));
        colunaStock.setCellValueFactory(c -> texto(c.getValue().descreverQuantidade(c.getValue().getQuantidadeAtual())));
        tabelaProdutos.setPlaceholder(new Label("Ainda não há produtos em stock."));
        tabelaProdutos.getSelectionModel().selectedItemProperty().addListener((observavel, antigo, novo) -> {
            campoUnidadesPorCaixa.setText(novo != null && novo.temConversaoCaixa()
                    ? String.valueOf(novo.getUnidadesPorCaixa()) : "");
            labelMensagem.setText("");
        });
        carregar(0);
    }

    /** Chamado pelo botão "Guardar conversão". */
    @FXML
    private void onGuardar() {
        Artigo artigo = tabelaProdutos.getSelectionModel().getSelectedItem();
        if (artigo == null) {
            mostrar("Selecione um produto na tabela.", COR_ERRO);
            return;
        }
        Optional<Integer> conversao = Utils.converterInteiro(campoUnidadesPorCaixa.getText(), 1,
                Artigo.MAX_UNIDADES_POR_CAIXA);
        if (conversao.isEmpty()) {
            mostrar("Escreva um número inteiro entre 1 e " + Artigo.MAX_UNIDADES_POR_CAIXA + ".", COR_ERRO);
            return;
        }
        try {
            boolean estavaEmCaixas = artigo.estaEmCaixas();
            Artigo guardado = controller.definirConversaoCaixa(artigo.getId(), conversao.get());
            mostrar("'" + guardado.getNome() + "': " + guardado.descreverConversao()
                    + (estavaEmCaixas ? ". O stock passou para " + guardado.descreverQuantidade(
                    guardado.getQuantidadeAtual()) + "." : "."), COR_SUCESSO);
            carregar(guardado.getId());
        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            mostrar(e.getMessage(), COR_ERRO);
        } catch (RuntimeException e) {
            mostrar("Não foi possível guardar a conversão: " + e.getMessage(), COR_ERRO);
        }
    }

    /** Chamado pelo botão "Fechar". */
    @FXML
    private void onFechar() {
        ((Stage) tabelaProdutos.getScene().getWindow()).close();
    }

    /** Lê os produtos em segundo plano e volta a selecionar o produto indicado (0 para nenhum). */
    private void carregar(int idSelecionado) {
        SegundoPlano.executar("Unidades dos produtos", controller::consultarStock,
                artigos -> {
                    tabelaProdutos.setItems(FXCollections.observableArrayList(artigos));
                    for (Artigo artigo : artigos) {
                        if (artigo.getId() == idSelecionado) {
                            tabelaProdutos.getSelectionModel().select(artigo);
                        }
                    }
                },
                erro -> mostrar(erro.getMessage(), COR_ERRO));
    }

    private void mostrar(String texto, String estilo) {
        labelMensagem.setText(texto);
        labelMensagem.setStyle(estilo);
    }

    private static SimpleStringProperty texto(String valor) {
        return new SimpleStringProperty(valor);
    }
}
