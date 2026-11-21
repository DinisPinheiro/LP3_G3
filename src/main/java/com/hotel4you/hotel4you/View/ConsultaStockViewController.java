package com.hotel4you.hotel4you.View;

import com.hotel4you.hotel4you.Controller.GestaoStockController;
import com.hotel4you.hotel4you.Model.Artigo;
import com.hotel4you.hotel4you.Model.MovimentoStock;
import com.hotel4you.hotel4you.Model.enums.TipoMovimento;
import com.hotel4you.hotel4you.Utils.Navegador;
import com.hotel4you.hotel4you.Utils.SegundoPlano;
import com.hotel4you.hotel4you.Utils.Utils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ViewController do ecrã de consulta do stock ({@code ConsultaStockView.fxml}, S4-04).
 * <p>
 * Só mostra o que o {@link GestaoStockController} devolve; daqui vai-se para a entrada de stock por XML.
 * A consulta corre em segundo plano (S6-01), para o ecrã não bloquear enquanto espera pela base de dados.
 * <p>
 * S6-04: mostra o stock real e o disponível (o real menos o que está em uso nas reservas), se o produto é
 * consumível e o histórico de saídas; o botão "Dar saída de stock" abre a janela {@code SaidaStockView.fxml}.
 */
public class ConsultaStockViewController {

    private static final String COR_ERRO = "-fx-text-fill: #b03a2e;";
    private static final String COR_SUCESSO = "-fx-text-fill: #1e8449;";
    private static final DateTimeFormatter FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Stock e histórico de saídas, lidos juntos em segundo plano. */
    private record Dados(List<Artigo> artigos, List<MovimentoStock> saidas) {
    }

    private final GestaoStockController controller;

    @FXML
    private TableView<Artigo> tabelaStock;
    @FXML
    private TableColumn<Artigo, String> colunaArtigo;
    @FXML
    private TableColumn<Artigo, String> colunaQuantidade;
    @FXML
    private TableColumn<Artigo, String> colunaEmUso;
    @FXML
    private TableColumn<Artigo, String> colunaDisponivel;
    @FXML
    private TableColumn<Artigo, String> colunaConsumivel;
    @FXML
    private TableColumn<Artigo, String> colunaUnidade;
    @FXML
    private TableColumn<Artigo, String> colunaPreco;
    @FXML
    private TableColumn<Artigo, String> colunaTaxa;
    @FXML
    private TableColumn<Artigo, String> colunaFornecedor;
    @FXML
    private Label labelMensagem;
    @FXML
    private TableView<MovimentoStock> tabelaSaidas;
    @FXML
    private TableColumn<MovimentoStock, String> colunaSaidaData;
    @FXML
    private TableColumn<MovimentoStock, String> colunaSaidaProduto;
    @FXML
    private TableColumn<MovimentoStock, String> colunaSaidaTipo;
    @FXML
    private TableColumn<MovimentoStock, String> colunaSaidaQuantidade;
    @FXML
    private TableColumn<MovimentoStock, String> colunaSaidaJustificacao;
    @FXML
    private TableColumn<MovimentoStock, String> colunaSaidaUtilizador;

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
        // Stock real: o total do hotel. Disponível: o real menos o que está em uso nas reservas (S6-04).
        colunaEmUso.setCellValueFactory(c -> texto(String.valueOf(c.getValue().getQuantidadeReservada())));
        colunaDisponivel.setCellValueFactory(c -> texto(String.valueOf(c.getValue().getQuantidadeDisponivel())));
        colunaConsumivel.setCellValueFactory(c -> texto(c.getValue().isConsumivel() ? "Sim" : "Não"));
        colunaUnidade.setCellValueFactory(c -> texto(c.getValue().getUnidade()));
        // Último preço unitário (sem taxa) e taxa de imposto, vindos da última entrada por XML (S5-04).
        colunaPreco.setCellValueFactory(c -> texto(formatarPreco(c.getValue())));
        colunaTaxa.setCellValueFactory(c -> texto(c.getValue().getTaxaPercentagem() == null
                ? "" : c.getValue().getTaxaPercentagem() + "%"));
        colunaFornecedor.setCellValueFactory(c -> texto(c.getValue().getFornecedor() == null
                ? "" : c.getValue().getFornecedor().getNome()));
        tabelaStock.setPlaceholder(new Label("Ainda não há artigos em stock."));
        // Histórico de saídas (S6-04): saídas com justificação e consumos no check-out.
        colunaSaidaData.setCellValueFactory(c -> texto(c.getValue().getDataHora().format(FORMATO_DATA_HORA)));
        colunaSaidaProduto.setCellValueFactory(c -> texto(c.getValue().getArtigo().getNome()));
        colunaSaidaTipo.setCellValueFactory(c -> texto(c.getValue().getTipo() == TipoMovimento.SAIDA
                ? "Saída" : "Consumo"));
        colunaSaidaQuantidade.setCellValueFactory(c -> texto(c.getValue().getQuantidade() + " "
                + c.getValue().getArtigo().getUnidade()));
        colunaSaidaJustificacao.setCellValueFactory(c -> texto(c.getValue().getJustificacao()));
        colunaSaidaUtilizador.setCellValueFactory(c -> texto(c.getValue().getUtilizador().getNome()));
        tabelaSaidas.setPlaceholder(new Label("Ainda não há saídas de stock."));
        onAtualizar();
    }

    /** Chamado pelo botão "Dar saída de stock": abre a janela de saída e, ao fechar, atualiza. */
    @FXML
    private void onDarSaida() {
        try {
            Navegador.abrirJanela("SaidaStockView.fxml", "Hotel4You - Saída de stock");
        } catch (RuntimeException e) {
            labelMensagem.setText(e.getMessage());
            labelMensagem.setStyle(COR_ERRO);
        }
        onAtualizar();
    }

    /** Chamado pelo botão "Consumível sim/não": troca a indicação do produto selecionado. */
    @FXML
    private void onTrocarConsumivel() {
        Artigo artigo = tabelaStock.getSelectionModel().getSelectedItem();
        if (artigo == null) {
            labelMensagem.setText("Selecione um produto na tabela.");
            labelMensagem.setStyle(COR_ERRO);
            return;
        }
        boolean consumivel = !artigo.isConsumivel();
        try {
            controller.definirConsumivel(artigo.getId(), consumivel);
            onAtualizar();
            labelMensagem.setText("'" + artigo.getNome() + "' " + (consumivel
                    ? "é consumível: no check-out sai do stock." : "não é consumível: no check-out volta ao stock."));
            labelMensagem.setStyle(COR_SUCESSO);
        } catch (RuntimeException e) {
            labelMensagem.setText(e.getMessage());
            labelMensagem.setStyle(COR_ERRO);
        }
    }

    /** Chamado pelo botão "Atualizar" e ao abrir o ecrã. */
    @FXML
    private void onAtualizar() {
        labelMensagem.setStyle("");
        labelMensagem.setText("A carregar...");
        SegundoPlano.executar("Stock do hotel",
                () -> new Dados(controller.consultarStock(), controller.consultarHistoricoSaidas()),
                dados -> {
                    tabelaStock.setItems(FXCollections.observableArrayList(dados.artigos()));
                    tabelaSaidas.setItems(FXCollections.observableArrayList(dados.saidas()));
                    labelMensagem.setText("");
                },
                erro -> {
                    labelMensagem.setText(erro.getMessage());
                    labelMensagem.setStyle(COR_ERRO);
                });
    }

    /** Chamado pelo botão "Voltar ao menu". */
    @FXML
    private void onVoltar() {
        Navegador.mudarPara("MenuPrincipalView.fxml", "Hotel4You");
    }

    /** Por exemplo "2,51 € / cx"; vazio nos artigos que ainda não têm preço. */
    private static String formatarPreco(Artigo artigo) {
        if (artigo.getPrecoUnitario() == null) {
            return "";
        }
        String preco = Utils.formatarPreco(artigo.getPrecoUnitario());
        return artigo.getUnidadePreco().isEmpty() ? preco : preco + " / " + artigo.getUnidadePreco();
    }

    private static SimpleStringProperty texto(String valor) {
        return new SimpleStringProperty(valor);
    }
}
