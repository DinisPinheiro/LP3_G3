package com.hotel4you.hotel4you.View;

import com.hotel4you.hotel4you.Controller.GestaoXsdController;
import com.hotel4you.hotel4you.Model.FicheiroXsd;
import com.hotel4you.hotel4you.Utils.XsdInvalidoException;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;

import java.io.File;

/**
 * ViewController do ecrã em que o gestor importa ou atualiza o ficheiro XSD
 * ({@code ImportacaoXsdView.fxml}, S5-05).
 * <p>
 * Só escolhe o ficheiro (FileChooser) e mostra o resultado; a validação do XSD e a sua gravação
 * estão no {@link GestaoXsdController}. Um XSD inválido mostra a mensagem de erro e nada muda.
 */
public class ImportacaoXsdViewController {

    private static final String COR_ERRO = "-fx-text-fill: #b03a2e;";
    private static final String COR_SUCESSO = "-fx-text-fill: #1e8449;";

    private final GestaoXsdController controller;

    @FXML
    private Label labelXsdAtual;
    @FXML
    private Label labelMensagem;

    /**
     * @param controller controller que valida e guarda o XSD
     */
    public ImportacaoXsdViewController(GestaoXsdController controller) {
        this.controller = controller;
    }

    /** Chamado pelo JavaFX depois de os campos {@code @FXML} serem preenchidos. */
    @FXML
    private void initialize() {
        mostrarXsdAtual();
    }

    /** Chamado pelo botão "Escolher ficheiro XSD...". */
    @FXML
    private void onEscolherFicheiro() {
        FileChooser escolha = new FileChooser();
        escolha.setTitle("Escolher o ficheiro XSD");
        escolha.getExtensionFilters().add(new FileChooser.ExtensionFilter("Ficheiros XSD", "*.xsd"));
        File ficheiro = escolha.showOpenDialog(labelMensagem.getScene().getWindow());
        if (ficheiro == null) {
            return;
        }
        try {
            FicheiroXsd importado = controller.importarXsd(ficheiro.toPath());
            mostrar("XSD '" + importado.getNomeFicheiro() + "' importado. Passa a validar os ficheiros dos fornecedores.",
                    COR_SUCESSO);
        } catch (XsdInvalidoException e) {
            mostrar(e.getMessage() + " O XSD anterior continua em vigor.", COR_ERRO);
        } catch (RuntimeException e) {
            mostrar(e.getMessage(), COR_ERRO);
        }
        mostrarXsdAtual();
    }

    private void mostrarXsdAtual() {
        try {
            labelXsdAtual.setText("Em vigor: " + controller.descreverXsdEmVigor());
        } catch (RuntimeException e) {
            mostrar(e.getMessage(), COR_ERRO);
        }
    }

    private void mostrar(String texto, String estilo) {
        labelMensagem.setText(texto);
        labelMensagem.setStyle(estilo);
    }
}
