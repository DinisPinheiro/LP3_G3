package com.hotel4you.hotel4you.View;

import com.hotel4you.hotel4you.Controller.ImportacaoFornecedorController;
import com.hotel4you.hotel4you.Model.Fornecedor;
import com.hotel4you.hotel4you.Model.ImportacaoFornecedor;
import com.hotel4you.hotel4you.Model.encomenda.EncomendaFornecedor;
import com.hotel4you.hotel4you.Model.encomenda.FornecedorEncomenda;
import com.hotel4you.hotel4you.Model.encomenda.LinhaEncomenda;
import com.hotel4you.hotel4you.Model.encomenda.TotaisEncomenda;
import com.hotel4you.hotel4you.Utils.ErroXml;
import com.hotel4you.hotel4you.Utils.Utils;
import com.hotel4you.hotel4you.Utils.XmlInvalidoException;
import com.hotel4you.hotel4you.Utils.XsdEmFaltaException;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ViewController do ecrã em que o fornecedor importa os seus ficheiros XML ou JSON e o XSD que valida os XML
 * ({@code ImportacaoFornecedorView.fxml}, S5-02, S6-02, S6-03 e S7-03).
 * <p>
 * Só escolhe os ficheiros (FileChooser) e mostra o resultado; a validação contra o XSD, a verificação de
 * que o ficheiro é do fornecedor, a regra da encomenda repetida e o envio estão no
 * {@link ImportacaoFornecedorController}. Primeiro escolhe o ficheiro (XML ou JSON, na mesma janela) e, para
 * os XML, a seguir o XSD (que fica guardado para os XML seguintes; o JSON não usa XSD). Um ficheiro inválido mostra a linha e o motivo de cada erro; diferenças de
 * IVA ou de totais mostram um aviso mas deixam enviar. Nenhum erro fecha a aplicação.
 */
public class ImportacaoFornecedorViewController {

    private static final String COR_ERRO = "-fx-text-fill: #b03a2e;";
    private static final String COR_SUCESSO = "-fx-text-fill: #1e8449;";
    private static final String COR_NEUTRA = "-fx-text-fill: #555555;";
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ImportacaoFornecedorController controller;

    /** Ficheiro válido e do fornecedor, à espera de ser enviado; {@code null} se não houver. */
    private Path ficheiroValido;

    /** Último ficheiro escolhido, para o voltar a validar depois de o fornecedor importar o XSD. */
    private Path ficheiroEscolhido;

    @FXML
    private Label labelFornecedor;
    @FXML
    private Button botaoEscolher;
    @FXML
    private Label labelFicheiro;
    @FXML
    private Button botaoEscolherXsd;
    @FXML
    private Label labelXsd;
    @FXML
    private Label labelAvisos;
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
    private TableColumn<LinhaEncomenda, String> colunaCaixas;
    @FXML
    private TableColumn<LinhaEncomenda, String> colunaUnidades;
    @FXML
    private TableColumn<LinhaEncomenda, String> colunaPreco;
    @FXML
    private TableColumn<LinhaEncomenda, String> colunaTaxa;
    @FXML
    private TableColumn<LinhaEncomenda, String> colunaSemTaxa;
    @FXML
    private TableColumn<LinhaEncomenda, String> colunaValorTaxa;
    @FXML
    private TableColumn<LinhaEncomenda, String> colunaTotalLinha;
    @FXML
    private Label labelTotais;
    @FXML
    private TextArea areaErros;
    @FXML
    private CheckBox checkAtualizarFicha;
    @FXML
    private Button botaoEnviar;
    @FXML
    private TableView<ImportacaoFornecedor> tabelaImportacoes;
    @FXML
    private TableColumn<ImportacaoFornecedor, String> colunaFicheiro;
    @FXML
    private TableColumn<ImportacaoFornecedor, String> colunaFormato;
    @FXML
    private TableColumn<ImportacaoFornecedor, String> colunaEncomenda;
    @FXML
    private TableColumn<ImportacaoFornecedor, String> colunaEnviadoEm;
    @FXML
    private TableColumn<ImportacaoFornecedor, String> colunaEstado;

    /**
     * @param controller controller que valida e envia os ficheiros do fornecedor
     */
    public ImportacaoFornecedorViewController(ImportacaoFornecedorController controller) {
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
        colunaCaixas.setCellValueFactory(c -> texto(c.getValue().caixas().isPresent()
                ? String.valueOf(c.getValue().caixas().getAsInt()) : ""));
        colunaUnidades.setCellValueFactory(c -> texto(c.getValue().unidades().isPresent()
                ? String.valueOf(c.getValue().unidades().getAsInt()) : ""));
        colunaPreco.setCellValueFactory(c -> texto(dinheiro(c.getValue().precoUnitario().preco().valor(), c.getValue())
                + " / " + c.getValue().precoUnitario().unidade().getAbreviatura()));
        colunaTaxa.setCellValueFactory(c -> texto(c.getValue().imposto().percentagem() + "%"));
        colunaSemTaxa.setCellValueFactory(c -> texto(dinheiro(c.getValue().subtotalCalculado(), c.getValue())));
        colunaValorTaxa.setCellValueFactory(c -> texto(dinheiro(c.getValue().taxaCalculada(), c.getValue())));
        colunaTotalLinha.setCellValueFactory(c -> texto(dinheiro(c.getValue().totalCalculado(), c.getValue())));
        tabelaLinhas.setPlaceholder(new Label("Escolha um ficheiro XML ou JSON para ver os seus dados."));

        colunaFicheiro.setCellValueFactory(c -> texto(c.getValue().getNomeFicheiro()));
        colunaFormato.setCellValueFactory(c -> texto(c.getValue().getFormato().getNome()));
        colunaEncomenda.setCellValueFactory(c -> texto(c.getValue().getNumeroEncomenda()));
        colunaEnviadoEm.setCellValueFactory(c -> texto(c.getValue().getDataImportacao().format(FORMATO_DATA_HORA)));
        colunaEstado.setCellValueFactory(c -> texto(c.getValue().getEstado().getNome()));
        tabelaImportacoes.setPlaceholder(new Label("Ainda não enviou nenhum ficheiro."));

        mostrarFornecedor();
    }

    /** Chamado pelo botão "Escolher ficheiro XML ou JSON...". */
    @FXML
    private void onEscolherFicheiro() {
        FileChooser escolha = new FileChooser();
        escolha.setTitle("Escolher o ficheiro da entrega (XML ou JSON)");
        escolha.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Ficheiros XML e JSON", "*.xml", "*.json"),
                new FileChooser.ExtensionFilter("Ficheiros XML", "*.xml"),
                new FileChooser.ExtensionFilter("Ficheiros JSON", "*.json"));
        File ficheiro = escolha.showOpenDialog(labelMensagem.getScene().getWindow());
        if (ficheiro != null) {
            validar(ficheiro.toPath());
        }
    }

    /** Chamado pelo botão "Escolher ficheiro XSD...": importa o XSD e volta a validar o XML escolhido. */
    @FXML
    private void onEscolherXsd() {
        FileChooser escolha = new FileChooser();
        escolha.setTitle("Escolher o XSD de validação");
        escolha.getExtensionFilters().add(new FileChooser.ExtensionFilter("Ficheiros XSD", "*.xsd"));
        File ficheiro = escolha.showOpenDialog(labelMensagem.getScene().getWindow());
        if (ficheiro == null) {
            return;
        }
        try {
            controller.importarXsd(ficheiro.toPath());
            mostrarXsd();
            if (ficheiroEscolhido != null) {
                validar(ficheiroEscolhido);
            } else {
                limparResultado();
                mostrar("XSD importado. Escolha agora o ficheiro XML ou JSON.", COR_SUCESSO);
            }
        } catch (RuntimeException e) {
            // Inclui o XSD inválido: a mensagem diz o motivo.
            mostrar(e.getMessage(), COR_ERRO);
        }
    }

    /** Chamado pelo botão "Enviar ao hotel". */
    @FXML
    private void onEnviar() {
        if (ficheiroValido == null) {
            return;
        }
        try {
            ImportacaoFornecedor enviada = controller.enviar(ficheiroValido, checkAtualizarFicha.isSelected());
            limparResultado();
            ficheiroEscolhido = null;
            labelFicheiro.setText("Nenhum ficheiro escolhido.");
            mostrar("Ficheiro enviado: encomenda " + enviada.getNumeroEncomenda()
                    + ". Fica pendente até o hotel o aprovar.", COR_SUCESSO);
            mostrarFornecedor();
        } catch (XmlInvalidoException e) {
            mostrarErros(e.getErros());
        } catch (RuntimeException e) {
            mostrar(e.getMessage(), COR_ERRO);
        }
    }

    private void validar(Path caminho) {
        limparResultado();
        ficheiroEscolhido = caminho;
        labelFicheiro.setText(caminho.getFileName().toString());
        try {
            mostrarDados(controller.lerEncomenda(caminho));
            ficheiroValido = caminho;
            botaoEnviar.setDisable(false);
        } catch (XsdEmFaltaException e) {
            mostrar(e.getMessage(), COR_ERRO);
        } catch (XmlInvalidoException e) {
            mostrarErros(e.getErros());
        } catch (RuntimeException e) {
            mostrar(e.getMessage(), COR_ERRO);
        }
    }

    private void mostrarFornecedor() {
        try {
            Fornecedor fornecedor = controller.obterFornecedorAtual();
            labelFornecedor.setText("Fornecedor: " + fornecedor.getNome() + " (" + fornecedor.getIdentificador() + ")"
                    + moradaEmTexto(fornecedor));
            // Conta base ainda sem morada: sugere preencher a ficha com os dados do primeiro XML.
            checkAtualizarFicha.setSelected(fornecedor.getMorada().isEmpty() && fornecedor.getCidade().isEmpty());
            tabelaImportacoes.setItems(FXCollections.observableArrayList(controller.listarMinhasImportacoes()));
            mostrarXsd();
        } catch (RuntimeException e) {
            labelFornecedor.setText(e.getMessage());
            labelFornecedor.setStyle(COR_ERRO);
            botaoEscolher.setDisable(true);
            botaoEscolherXsd.setDisable(true);
        }
    }

    private void mostrarXsd() {
        labelXsd.setText(controller.descreverXsd());
    }

    private static String moradaEmTexto(Fornecedor fornecedor) {
        String morada = String.join(" ", fornecedor.getMorada(), fornecedor.getCodigoPostal(),
                fornecedor.getCidade()).trim();
        return morada.isEmpty() ? "" : "  |  " + morada;
    }

    private void mostrarDados(EncomendaFornecedor encomenda) {
        FornecedorEncomenda fornecedor = encomenda.fornecedor();
        labelCabecalho.setText("Encomenda " + encomenda.numero() + " de " + encomenda.data().format(FORMATO_DATA)
                + "  |  Fornecedor: " + fornecedor.nome() + " (" + fornecedor.identificador() + ")");
        TotaisEncomenda totais = encomenda.totais();
        labelTotais.setText("Total das linhas: " + Utils.formatarMontante(totais.totalLinhas(), totais.moeda())
                + "  |  Total das taxas: " + Utils.formatarMontante(totais.totalTaxas(), totais.moeda())
                + "  |  Total com taxas: " + Utils.formatarMontante(totais.totalComTaxas(), totais.moeda()));
        tabelaLinhas.setItems(FXCollections.observableArrayList(encomenda.linhas()));
        mostrar("Ficheiro válido: " + encomenda.linhas().size() + " linha(s). Reveja os dados e envie ao hotel.",
                COR_SUCESSO);
        mostrarAvisos(encomenda.avisos());
    }

    /** Diferenças entre o IVA e os totais do ficheiro e os calculados: só avisam, o envio pode continuar. */
    private void mostrarAvisos(List<String> avisos) {
        if (avisos.isEmpty()) {
            return;
        }
        labelAvisos.setText("Aviso: o ficheiro tem valores que diferem dos calculados (o envio pode continuar):"
                + System.lineSeparator() + String.join(System.lineSeparator(), avisos));
        labelAvisos.setVisible(true);
        labelAvisos.setManaged(true);
    }

    private void mostrarErros(List<ErroXml> erros) {
        areaErros.setText(erros.stream().map(ErroXml::toString).collect(Collectors.joining(System.lineSeparator())));
        areaErros.setVisible(true);
        areaErros.setManaged(true);
        mostrar("Ficheiro inválido (" + erros.size() + " erro(s)). Corrija o ficheiro e escolha-o de novo.", COR_ERRO);
    }

    private void limparResultado() {
        ficheiroValido = null;
        botaoEnviar.setDisable(true);
        labelCabecalho.setText("");
        labelTotais.setText("");
        tabelaLinhas.getItems().clear();
        labelAvisos.setText("");
        labelAvisos.setVisible(false);
        labelAvisos.setManaged(false);
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

    private static String dinheiro(BigDecimal valor, LinhaEncomenda linha) {
        return Utils.formatarMontante(valor, linha.precoUnitario().preco().moeda());
    }
}