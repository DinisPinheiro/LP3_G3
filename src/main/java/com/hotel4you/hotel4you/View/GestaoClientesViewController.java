package com.hotel4you.hotel4you.View;

import com.hotel4you.hotel4you.Controller.GestaoClientesController;
import com.hotel4you.hotel4you.Model.Cliente;
import com.hotel4you.hotel4you.Utils.Navegador;
import com.hotel4you.hotel4you.Utils.Utils;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

/**
 * ViewController do ecrã de registo e consulta de clientes ({@code GestaoClientesView.fxml}).
 * <p>
 * Só lê os campos e mostra mensagens; as regras (campos obrigatórios, NIF/documento único,
 * permissões) estão no {@link GestaoClientesController}. Enter passa ao campo seguinte e,
 * no último, regista; escrever "0" num campo e carregar Enter (ou o botão Cancelar) cancela o registo.
 * <p>
 * Apoio de IA: Claude (Anthropic), revisto pela equipa.
 */
public class GestaoClientesViewController {

    private static final String COR_ERRO = "-fx-text-fill: #b03a2e;";
    private static final String COR_SUCESSO = "-fx-text-fill: #1e8449;";
    private static final String COR_NEUTRA = "-fx-text-fill: #555555;";

    private final GestaoClientesController controller;

    @FXML
    private TextField campoNome;
    @FXML
    private TextField campoDocumento;
    @FXML
    private TextField campoContacto;
    @FXML
    private TextField campoPesquisa;
    @FXML
    private ListView<Cliente> listaClientes;
    @FXML
    private Label labelMensagem;

    /**
     * @param controller controller que regista e consulta clientes
     */
    public GestaoClientesViewController(GestaoClientesController controller) {
        this.controller = controller;
    }

    /** Chamado pelo JavaFX depois de os campos {@code @FXML} serem preenchidos. */
    @FXML
    private void initialize() {
        listaClientes.setCellFactory(lista -> new ListCell<Cliente>() {
            @Override
            protected void updateItem(Cliente cliente, boolean vazio) {
                super.updateItem(cliente, vazio);
                setText(vazio || cliente == null ? null : descrever(cliente));
            }
        });
        campoNome.setOnAction(evento -> avancarPara(campoDocumento));
        campoDocumento.setOnAction(evento -> avancarPara(campoContacto));
        campoContacto.setOnAction(evento -> onRegistar());
        campoPesquisa.textProperty().addListener((observavel, antigo, novo) -> atualizarLista());
        atualizarLista();
    }

    /** Chamado pelo botão "Registar cliente". */
    @FXML
    private void onRegistar() {
        if (pediuCancelamento()) {
            onCancelar();
            return;
        }
        try {
            Cliente cliente = controller.registarCliente(
                    campoNome.getText(), campoDocumento.getText(), campoContacto.getText());
            mostrar("Cliente registado: " + cliente.getNome() + ".", COR_SUCESSO);
            limparFormulario();
            atualizarLista();
            campoNome.requestFocus();
        } catch (IllegalArgumentException | SecurityException e) {
            mostrar(e.getMessage(), COR_ERRO);
        } catch (RuntimeException e) {
            mostrar("Não foi possível registar o cliente: " + e.getMessage(), COR_ERRO);
        }
    }

    /** Chamado pelo botão "Cancelar": limpa o formulário. */
    @FXML
    private void onCancelar() {
        limparFormulario();
        mostrar("Registo cancelado.", COR_NEUTRA);
        campoNome.requestFocus();
    }

    /** Chamado pelo botão "Voltar ao menu". */
    @FXML
    private void onVoltar() {
        Navegador.mudarPara("MenuPrincipalView.fxml", "Hotel4You");
    }

    private void avancarPara(TextField destino) {
        if (pediuCancelamento()) {
            onCancelar();
        } else {
            destino.requestFocus();
        }
    }

    private boolean pediuCancelamento() {
        return Utils.eCancelamento(campoNome.getText())
                || Utils.eCancelamento(campoDocumento.getText())
                || Utils.eCancelamento(campoContacto.getText());
    }

    private void atualizarLista() {
        try {
            listaClientes.setItems(FXCollections.observableArrayList(
                    controller.consultarClientes(campoPesquisa.getText())));
        } catch (RuntimeException e) {
            mostrar(e.getMessage(), COR_ERRO);
        }
    }

    private void limparFormulario() {
        campoNome.clear();
        campoDocumento.clear();
        campoContacto.clear();
    }

    private void mostrar(String texto, String estilo) {
        labelMensagem.setText(texto);
        labelMensagem.setStyle(estilo);
    }

    private static String descrever(Cliente cliente) {
        String contacto = cliente.getContacto().isBlank() ? "" : "  |  " + cliente.getContacto();
        return cliente.getNome() + "  |  NIF/Doc: " + cliente.getDocumento() + contacto;
    }
}
