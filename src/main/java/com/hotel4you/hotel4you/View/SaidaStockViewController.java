package com.hotel4you.hotel4you.View;

import com.hotel4you.hotel4you.Controller.GestaoStockController;
import com.hotel4you.hotel4you.Model.Artigo;
import com.hotel4you.hotel4you.Model.MovimentoStock;
import com.hotel4you.hotel4you.Utils.SegundoPlano;
import com.hotel4you.hotel4you.Utils.Utils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.Optional;

/**
 * ViewController da janela de saída de stock ({@code SaidaStockView.fxml}, S6-04).
 * <p>
 * Pede o produto, a quantidade e a justificação (ex.: televisão avariada) e mostra o stock real e o
 * disponível do produto escolhido. As regras estão no {@link GestaoStockController} e no model.
 */
public class SaidaStockViewController {

    private static final String COR_ERRO = "-fx-text-fill: #b03a2e;";
    private static final String COR_SUCESSO = "-fx-text-fill: #1e8449;";

    private final GestaoStockController controller;

    @FXML
    private ComboBox<Artigo> campoProduto;
    @FXML
    private Label labelStock;
    @FXML
    private TextField campoQuantidade;
    @FXML
    private TextArea campoJustificacao;
    @FXML
    private Label labelMensagem;

    /**
     * @param controller controller do stock
     */
    public SaidaStockViewController(GestaoStockController controller) {
        this.controller = controller;
    }

    /** Chamado pelo JavaFX depois de os campos {@code @FXML} serem preenchidos. */
    @FXML
    private void initialize() {
        campoProduto.setConverter(new StringConverter<Artigo>() {
            @Override
            public String toString(Artigo artigo) {
                return artigo == null ? "" : artigo.getNome() + " (" + artigo.getUnidade() + ")";
            }

            @Override
            public Artigo fromString(String texto) {
                return null;
            }
        });
        campoProduto.valueProperty().addListener((observavel, antigo, novo) -> mostrarStock(novo));
        mostrarStock(null);
        carregarProdutos();
    }

    /** Chamado pelo botão "Dar saída". */
    @FXML
    private void onConfirmar() {
        Artigo artigo = campoProduto.getValue();
        if (artigo == null) {
            mostrar("Escolha o produto.", COR_ERRO);
            return;
        }
        Optional<Integer> quantidade = Utils.converterInteiro(campoQuantidade.getText());
        if (quantidade.isEmpty() || quantidade.get() < 1) {
            mostrar("Quantidade inválida. Escreva um número inteiro, por exemplo 1.", COR_ERRO);
            return;
        }
        try {
            MovimentoStock saida = controller.darSaida(artigo.getId(), quantidade.get(), campoJustificacao.getText());
            mostrar("Saída registada: " + saida.getQuantidade() + " " + artigo.getUnidade() + " de '"
                    + artigo.getNome() + "'.", COR_SUCESSO);
            campoQuantidade.clear();
            campoJustificacao.clear();
            carregarProdutos();
        } catch (IllegalArgumentException | SecurityException e) {
            mostrar(e.getMessage(), COR_ERRO);
        } catch (RuntimeException e) {
            mostrar("Não foi possível registar a saída: " + e.getMessage(), COR_ERRO);
        }
    }

    /** Chamado pelo botão "Fechar". */
    @FXML
    private void onFechar() {
        ((Stage) campoProduto.getScene().getWindow()).close();
    }

    private void carregarProdutos() {
        int idEscolhido = campoProduto.getValue() == null ? 0 : campoProduto.getValue().getId();
        SegundoPlano.executar("Stock para saída", controller::consultarStock,
                artigos -> {
                    campoProduto.setItems(FXCollections.observableArrayList(artigos));
                    for (Artigo artigo : artigos) {
                        if (artigo.getId() == idEscolhido) {
                            campoProduto.setValue(artigo);
                        }
                    }
                },
                erro -> mostrar(erro.getMessage(), COR_ERRO));
    }

    private void mostrarStock(Artigo artigo) {
        if (artigo == null) {
            labelStock.setText("Stock real: -   |   Disponível: -");
            return;
        }
        labelStock.setText("Stock real: " + artigo.getQuantidadeAtual() + " " + artigo.getUnidade()
                + "   |   Em uso em reservas: " + artigo.getQuantidadeReservada()
                + "   |   Disponível: " + artigo.getQuantidadeDisponivel()
                + (artigo.isConsumivel() ? "   |   Consumível" : ""));
    }

    private void mostrar(String texto, String estilo) {
        labelMensagem.setText(texto);
        labelMensagem.setStyle(estilo);
    }
}
