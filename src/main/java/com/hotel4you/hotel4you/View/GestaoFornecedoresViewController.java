package com.hotel4you.hotel4you.View;

import com.hotel4you.hotel4you.Controller.GestaoFornecedoresController;
import com.hotel4you.hotel4you.Model.Fornecedor;
import com.hotel4you.hotel4you.Utils.SegundoPlano;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

/**
 * ViewController do ecrã em que o gestor cria fornecedores com a sua conta de login
 * ({@code GestaoFornecedoresView.fxml}, S5-02).
 * <p>
 * Só lê os campos e mostra o resultado; as validações e a criação estão no
 * {@link GestaoFornecedoresController}. A lista e a criação correm em segundo plano (S6-01), para o
 * ecrã não bloquear enquanto espera pela base de dados.
 */
public class GestaoFornecedoresViewController {

    private static final String COR_ERRO = "-fx-text-fill: #b03a2e;";
    private static final String COR_SUCESSO = "-fx-text-fill: #1e8449;";

    private final GestaoFornecedoresController controller;

    @FXML
    private TextField campoCodigo;
    @FXML
    private TextField campoNome;
    @FXML
    private TextField campoMorada;
    @FXML
    private TextField campoCodigoPostal;
    @FXML
    private TextField campoCidade;
    @FXML
    private TextField campoUsername;
    @FXML
    private PasswordField campoPassword;
    @FXML
    private Label labelMensagem;
    @FXML
    private TableView<Fornecedor> tabelaFornecedores;
    @FXML
    private TableColumn<Fornecedor, String> colunaCodigo;
    @FXML
    private TableColumn<Fornecedor, String> colunaNome;
    @FXML
    private TableColumn<Fornecedor, String> colunaMorada;
    @FXML
    private TableColumn<Fornecedor, String> colunaCodigoPostal;
    @FXML
    private TableColumn<Fornecedor, String> colunaCidade;
    @FXML
    private TableColumn<Fornecedor, String> colunaConta;

    /**
     * @param controller controller que cria e lista os fornecedores
     */
    public GestaoFornecedoresViewController(GestaoFornecedoresController controller) {
        this.controller = controller;
    }

    /** Chamado pelo JavaFX depois de os campos {@code @FXML} serem preenchidos. */
    @FXML
    private void initialize() {
        colunaCodigo.setCellValueFactory(c -> texto(c.getValue().getIdentificador()));
        colunaNome.setCellValueFactory(c -> texto(c.getValue().getNome()));
        colunaMorada.setCellValueFactory(c -> texto(c.getValue().getMorada()));
        colunaCodigoPostal.setCellValueFactory(c -> texto(c.getValue().getCodigoPostal()));
        colunaCidade.setCellValueFactory(c -> texto(c.getValue().getCidade()));
        colunaConta.setCellValueFactory(c -> texto(c.getValue().temConta() ? "Sim" : "Não"));
        tabelaFornecedores.setPlaceholder(new Label("Ainda não há fornecedores."));
        atualizarLista();
    }

    /** Chamado pelo botão "Criar fornecedor". */
    @FXML
    private void onCriar() {
        // Os campos são lidos aqui, na thread do JavaFX; só a gravação corre em segundo plano.
        String codigo = campoCodigo.getText();
        String nome = campoNome.getText();
        String morada = campoMorada.getText();
        String codigoPostal = campoCodigoPostal.getText();
        String cidade = campoCidade.getText();
        String username = campoUsername.getText();
        String password = campoPassword.getText();
        mostrar("A criar o fornecedor...", "");
        SegundoPlano.executar("Criar fornecedor",
                () -> controller.criarFornecedor(codigo, nome, morada, codigoPostal, cidade, username, password),
                criado -> {
                    limparCampos();
                    atualizarLista();
                    mostrar("Fornecedor '" + criado.getNome() + "' criado. Já pode entrar com a sua conta.",
                            COR_SUCESSO);
                },
                erro -> mostrar(erro.getMessage(), COR_ERRO));
    }

    private void atualizarLista() {
        SegundoPlano.executar("Lista de fornecedores", controller::listarFornecedores,
                fornecedores -> tabelaFornecedores.setItems(FXCollections.observableArrayList(fornecedores)),
                erro -> mostrar(erro.getMessage(), COR_ERRO));
    }

    private void limparCampos() {
        campoCodigo.clear();
        campoNome.clear();
        campoMorada.clear();
        campoCodigoPostal.clear();
        campoCidade.clear();
        campoUsername.clear();
        campoPassword.clear();
    }

    private void mostrar(String texto, String estilo) {
        labelMensagem.setText(texto);
        labelMensagem.setStyle(estilo);
    }

    private static SimpleStringProperty texto(String valor) {
        return new SimpleStringProperty(valor);
    }
}
