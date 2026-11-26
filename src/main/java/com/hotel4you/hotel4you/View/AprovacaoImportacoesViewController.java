package com.hotel4you.hotel4you.View;

import com.hotel4you.hotel4you.Controller.AprovacaoImportacoesController;
import com.hotel4you.hotel4you.Model.ImportacaoFornecedor;
import com.hotel4you.hotel4you.Model.encomenda.EncomendaFornecedor;
import com.hotel4you.hotel4you.Model.encomenda.LinhaEncomenda;
import com.hotel4you.hotel4you.Model.enums.EstadoImportacao;
import com.hotel4you.hotel4you.Model.encomenda.TotaisEncomenda;
import com.hotel4you.hotel4you.Utils.ErroXml;
import com.hotel4you.hotel4you.Utils.Utils;
import com.hotel4you.hotel4you.Utils.XmlInvalidoException;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

/**
 * ViewController do ecrã em que o gestor ou o rececionista aprova ou rejeita os ficheiros enviados
 * pelos fornecedores ({@code AprovacaoImportacoesView.fxml}, S5-03).
 * <p>
 * Mostra a lista de ficheiros pendentes, aprovados ou rejeitados e, ao selecionar um, o seu conteúdo.
 * As regras (só um ficheiro pendente pode ser decidido, só os aprovados dão entrada em stock) estão
 * no {@link AprovacaoImportacoesController}.
 */
public class AprovacaoImportacoesViewController {

    private static final String COR_ERRO = "-fx-text-fill: #b03a2e;";
    private static final String COR_SUCESSO = "-fx-text-fill: #1e8449;";
    private static final String COR_NEUTRA = "-fx-text-fill: #555555;";
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Opção do filtro: um estado, ou todos os estados quando o estado é {@code null}. */
    private record Filtro(String texto, EstadoImportacao estado) {
        @Override
        public String toString() {
            return texto;
        }
    }

    private final AprovacaoImportacoesController controller;

    @FXML
    private ComboBox<Filtro> comboFiltro;
    @FXML
    private Label labelMensagem;
    @FXML
    private TableView<ImportacaoFornecedor> tabelaImportacoes;
    @FXML
    private TableColumn<ImportacaoFornecedor, String> colunaFornecedor;
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
    @FXML
    private TableColumn<ImportacaoFornecedor, String> colunaDecididoEm;
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
    private Button botaoAprovar;
    @FXML
    private Button botaoRejeitar;

    /**
     * @param controller controller que lista, aprova e rejeita os ficheiros
     */
    public AprovacaoImportacoesViewController(AprovacaoImportacoesController controller) {
        this.controller = controller;
    }

    /** Chamado pelo JavaFX depois de os campos {@code @FXML} serem preenchidos. */
    @FXML
    private void initialize() {
        colunaFornecedor.setCellValueFactory(c -> texto(c.getValue().getFornecedor().getNome()));
        colunaFicheiro.setCellValueFactory(c -> texto(c.getValue().getNomeFicheiro()));
        colunaFormato.setCellValueFactory(c -> texto(c.getValue().getFormato().getNome()));
        colunaEncomenda.setCellValueFactory(c -> texto(c.getValue().getNumeroEncomenda()));
        colunaEnviadoEm.setCellValueFactory(c -> texto(c.getValue().getDataImportacao().format(FORMATO_DATA_HORA)));
        colunaEstado.setCellValueFactory(c -> texto(c.getValue().getEstado().getNome()));
        colunaDecididoEm.setCellValueFactory(c -> texto(c.getValue().getDataDecisao() == null
                ? "" : c.getValue().getDataDecisao().format(FORMATO_DATA_HORA)));
        tabelaImportacoes.setPlaceholder(new Label("Não há ficheiros neste estado."));

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
        tabelaLinhas.setPlaceholder(new Label("Selecione um ficheiro para ver o seu conteúdo."));

        Filtro pendentes = new Filtro("Pendentes", EstadoImportacao.PENDENTE);
        comboFiltro.setItems(FXCollections.observableArrayList(pendentes,
                new Filtro("Aprovados", EstadoImportacao.APROVADO),
                new Filtro("Rejeitados", EstadoImportacao.REJEITADO),
                new Filtro("Todos", null)));
        comboFiltro.setValue(pendentes);
        comboFiltro.setOnAction(evento -> onAtualizar());

        tabelaImportacoes.getSelectionModel().selectedItemProperty()
                .addListener((observado, anterior, selecionada) -> mostrarSelecionada(selecionada));
        onAtualizar();
    }

    /** Chamado pelo botão "Atualizar", ao mudar o filtro e ao abrir o ecrã. */
    @FXML
    private void onAtualizar() {
        try {
            tabelaImportacoes.setItems(FXCollections.observableArrayList(
                    controller.listar(comboFiltro.getValue().estado())));
            limparDetalhe();
        } catch (RuntimeException e) {
            mostrar(e.getMessage(), COR_ERRO);
        }
    }

    /** Chamado pelo botão "Aprovar (dá entrada em stock)". */
    @FXML
    private void onAprovar() {
        ImportacaoFornecedor selecionada = tabelaImportacoes.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            return;
        }
        try {
            controller.aprovar(selecionada.getId());
            onAtualizar();
            mostrar("Ficheiro aprovado: o material da encomenda " + selecionada.getNumeroEncomenda()
                    + " deu entrada em stock.", COR_SUCESSO);
        } catch (XmlInvalidoException e) {
            mostrarErros(e);
        } catch (RuntimeException e) {
            mostrar(e.getMessage(), COR_ERRO);
        }
    }

    /** Chamado pelo botão "Rejeitar". */
    @FXML
    private void onRejeitar() {
        ImportacaoFornecedor selecionada = tabelaImportacoes.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            return;
        }
        try {
            controller.rejeitar(selecionada.getId());
            onAtualizar();
            mostrar("Ficheiro rejeitado: nada deu entrada em stock.", COR_SUCESSO);
        } catch (RuntimeException e) {
            mostrar(e.getMessage(), COR_ERRO);
        }
    }

    private void mostrarSelecionada(ImportacaoFornecedor importacao) {
        limparDetalhe();
        if (importacao == null) {
            return;
        }
        boolean pendente = importacao.getEstado() == EstadoImportacao.PENDENTE;
        botaoAprovar.setDisable(!pendente);
        botaoRejeitar.setDisable(!pendente);
        try {
            EncomendaFornecedor encomenda = controller.consultarEncomenda(importacao.getId());
            labelCabecalho.setText("Encomenda " + encomenda.numero() + " de " + encomenda.data().format(FORMATO_DATA)
                    + "  |  Fornecedor: " + importacao.getFornecedor().getNome());
            TotaisEncomenda totais = encomenda.totais();
            labelTotais.setText("Total das linhas: " + Utils.formatarMontante(totais.totalLinhas(), totais.moeda())
                    + "  |  Total das taxas: " + Utils.formatarMontante(totais.totalTaxas(), totais.moeda())
                    + "  |  Total com taxas: " + Utils.formatarMontante(totais.totalComTaxas(), totais.moeda()));
            tabelaLinhas.setItems(FXCollections.observableArrayList(encomenda.linhas()));
        } catch (XmlInvalidoException e) {
            mostrarErros(e);
        } catch (RuntimeException e) {
            mostrar(e.getMessage(), COR_ERRO);
        }
    }

    private void mostrarErros(XmlInvalidoException e) {
        areaErros.setText(e.getErros().stream().map(ErroXml::toString)
                .collect(Collectors.joining(System.lineSeparator())));
        areaErros.setVisible(true);
        areaErros.setManaged(true);
        mostrar("O ficheiro já não cumpre o XSD em vigor. Pode rejeitá-lo.", COR_ERRO);
    }

    private void limparDetalhe() {
        botaoAprovar.setDisable(true);
        botaoRejeitar.setDisable(true);
        labelCabecalho.setText("");
        labelTotais.setText("");
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

    private static String dinheiro(BigDecimal valor, LinhaEncomenda linha) {
        return Utils.formatarMontante(valor, linha.precoUnitario().preco().moeda());
    }
}
