package com.hotel4you.hotel4you.View;

import com.hotel4you.hotel4you.Controller.AcessoDadosException;
import com.hotel4you.hotel4you.Controller.GestaoStockController;
import com.hotel4you.hotel4you.Model.encomenda.EncomendaFornecedor;
import com.hotel4you.hotel4you.Model.encomenda.FornecedorEncomenda;
import com.hotel4you.hotel4you.Model.encomenda.LinhaEncomenda;
import com.hotel4you.hotel4you.Utils.ErroXml;
import com.hotel4you.hotel4you.Utils.Navegador;
import com.hotel4you.hotel4you.Utils.XmlInvalidoException;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ViewController do ecrã de validação do XML do fornecedor e entrada em stock
 * ({@code ValidacaoXmlView.fxml}, S4-03 e S4-04).
 * <p>
 * Só escolhe o ficheiro (FileChooser) e mostra o resultado; a validação contra o XSD, a leitura do XML
 * e a entrada em stock estão no {@link GestaoStockController}. Um ficheiro válido mostra os seus dados;
 * um inválido mostra a linha e o motivo de cada erro. Nenhum erro fecha a aplicação.
 */
public class ValidacaoXmlViewController {

    private static final String COR_ERRO = "-fx-text-fill: #b03a2e;";
    private static final String COR_SUCESSO = "-fx-text-fill: #1e8449;";
    private static final String COR_NEUTRA = "-fx-text-fill: #555555;";
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final GestaoStockController controller;

    /** Encomenda do último ficheiro válido, à espera de dar entrada em stock; {@code null} se não houver. */
    private EncomendaFornecedor encomendaValida;

    @FXML
    private Label labelFicheiro;
    @FXML
    private Label labelMensagem;
    @FXML
    private Label labelCabecalho;
    @FXML
    private TableView<LinhaEncomenda> tabelaLinhas;
    @FXML
    private TableColumn<LinhaEncomenda, String> colunaNumero;
    @FXML
    private TableColumn<LinhaEncomenda, String> colunaCodigo;
    @FXML
    private TableColumn<LinhaEncomenda, String> colunaDescricao;
    @FXML
    private TableColumn<LinhaEncomenda, String> colunaQuantidade;
    @FXML
    private TextArea areaErros;
    @FXML
    private Button botaoEntrada;

    /**
     * @param controller controller que valida o XML e dá entrada em stock
     */
    public ValidacaoXmlViewController(GestaoStockController controller) {
        this.controller = controller;
    }

    /** Chamado pelo JavaFX depois de os campos {@code @FXML} serem preenchidos. */
    @FXML
    private void initialize() {
        colunaNumero.setCellValueFactory(c -> texto(String.valueOf(c.getValue().numero())));
        colunaCodigo.setCellValueFactory(c -> texto(c.getValue().codigoProduto()));
        colunaDescricao.setCellValueFactory(c -> texto(c.getValue().descricao()));
        colunaQuantidade.setCellValueFactory(c -> texto(c.getValue().quantidade().valor()
                + " " + c.getValue().quantidade().unidade().getAbreviatura()));
        tabelaLinhas.setPlaceholder(new Label("Escolha um ficheiro XML para ver os seus dados."));
    }

    /** Chamado pelo botão "Escolher ficheiro XML...". */
    @FXML
    private void onEscolherFicheiro() {
        FileChooser escolha = new FileChooser();
        escolha.setTitle("Escolher o XML do fornecedor");
        escolha.getExtensionFilters().add(new FileChooser.ExtensionFilter("Ficheiros XML", "*.xml"));
        File ficheiro = escolha.showOpenDialog(labelMensagem.getScene().getWindow());
        if (ficheiro != null) {
            validar(ficheiro.toPath());
        }
    }

    /** Chamado pelo botão "Dar entrada em stock". */
    @FXML
    private void onDarEntrada() {
        if (encomendaValida == null) {
            return;
        }
        try {
            int linhas = controller.darEntrada(encomendaValida);
            mostrar("Entrada em stock registada: " + linhas + " linha(s) da encomenda "
                    + encomendaValida.numero() + ".", COR_SUCESSO);
            // Evita dar entrada duas vezes do mesmo ficheiro; é preciso escolher o ficheiro de novo.
            encomendaValida = null;
            botaoEntrada.setDisable(true);
        } catch (IllegalArgumentException | SecurityException | AcessoDadosException e) {
            mostrar(e.getMessage(), COR_ERRO);
        } catch (RuntimeException e) {
            mostrar("Não foi possível dar entrada em stock: " + e.getMessage(), COR_ERRO);
        }
    }

    /** Chamado pelo botão "Voltar ao stock". */
    @FXML
    private void onVoltar() {
        Navegador.mudarPara("ConsultaStockView.fxml", "Hotel4You - Stock");
    }

    private void validar(Path caminho) {
        limparResultado();
        labelFicheiro.setText(caminho.getFileName().toString());
        try {
            mostrarDados(controller.lerEncomendaXml(caminho));
        } catch (XmlInvalidoException e) {
            mostrarErros(e.getErros());
        } catch (IllegalArgumentException | SecurityException e) {
            mostrar(e.getMessage(), COR_ERRO);
        } catch (RuntimeException e) {
            mostrar("Não foi possível validar o ficheiro: " + e.getMessage(), COR_ERRO);
        }
    }

    private void mostrarDados(EncomendaFornecedor encomenda) {
        FornecedorEncomenda fornecedor = encomenda.fornecedor();
        labelCabecalho.setText("Encomenda " + encomenda.numero() + " de " + encomenda.data().format(FORMATO_DATA)
                + "  |  Fornecedor: " + fornecedor.nome() + " (" + fornecedor.identificador() + ")");
        tabelaLinhas.setItems(FXCollections.observableArrayList(encomenda.linhas()));
        encomendaValida = encomenda;
        botaoEntrada.setDisable(false);
        mostrar("Ficheiro válido: " + encomenda.linhas().size() + " linha(s). Reveja os dados e dê entrada em stock.",
                COR_SUCESSO);
    }

    private void mostrarErros(List<ErroXml> erros) {
        areaErros.setText(erros.stream().map(ErroXml::toString).collect(Collectors.joining(System.lineSeparator())));
        areaErros.setVisible(true);
        areaErros.setManaged(true);
        mostrar("Ficheiro inválido (" + erros.size() + " erro(s)). Corrija o XML e escolha-o de novo.", COR_ERRO);
    }

    private void limparResultado() {
        encomendaValida = null;
        botaoEntrada.setDisable(true);
        labelCabecalho.setText("");
        tabelaLinhas.getItems().clear();
        areaErros.clear();
        areaErros.setVisible(false);
        areaErros.setManaged(false);
        mostrar("", COR_NEUTRA);
    }

    private void mostrar(String texto, String estilo) {
        labelMensagem.setText(texto);
        labelMensagem.setStyle(estilo);
    }

    private static SimpleStringProperty texto(String valor) {
        return new SimpleStringProperty(valor);
    }
}
