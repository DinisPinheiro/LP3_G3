package com.hotel4you.hotel4you.View;

import com.hotel4you.hotel4you.Controller.PassagemController;
import com.hotel4you.hotel4you.Model.LocalHotel;
import com.hotel4you.hotel4you.Model.Passagem;
import com.hotel4you.hotel4you.Utils.Navegador;
import com.hotel4you.hotel4you.Utils.SegundoPlano;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.time.format.DateTimeFormatter;

/**
 * ViewController da janela de picagem ({@code PicagemCartaoView.fxml}), aberta a partir do login
 * (S6-06). Só lê os campos e mostra mensagens; as regras de acesso estão no {@link PassagemController}.
 * <p>
 * Qualquer pessoa pode picar sem ter feito login: o cliente indica o cartão e o NIF e escolhe o quarto
 * ou o local; os funcionários e o cartão mestre indicam só o cartão e o local.
 */
public class PicagemCartaoViewController {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final String ESTILO_ERRO = "-fx-text-fill: #c0392b;";
    private static final String ESTILO_SUCESSO = "-fx-text-fill: #1e8449;";

    private final PassagemController controller;

    @FXML
    private TextField campoCartao;

    @FXML
    private TextField campoNif;

    @FXML
    private ComboBox<LocalHotel> campoLocal;

    @FXML
    private TextField campoQuarto;

    @FXML
    private Button botaoPicar;

    @FXML
    private Label labelMensagem;

    /**
     * @param controller controller que aplica as regras de acesso e regista as passagens
     */
    public PicagemCartaoViewController(PassagemController controller) {
        this.controller = controller;
    }

    @FXML
    private void initialize() {
        // O número do quarto só faz sentido quando o local escolhido é o quarto.
        campoLocal.valueProperty().addListener((observavel, anterior, escolhido) -> {
            boolean quarto = escolhido != null && escolhido.isQuarto();
            campoQuarto.setDisable(!quarto);
            if (!quarto) {
                campoQuarto.clear();
            }
        });

        SegundoPlano.executar(
                "Locais do hotel",
                controller::listarLocais,
                locais -> {
                    campoLocal.getItems().setAll(locais);
                    campoLocal.getSelectionModel().selectFirst();
                },
                erro -> mostrarErro(erro.getMessage())
        );
    }

    /** Chamado pelo botão "Picar cartão" (ou Enter). */
    @FXML
    private void picar() {
        int cartao;
        Integer quarto = null;
        try {
            cartao = Integer.parseInt(campoCartao.getText().trim());
            if (!campoQuarto.getText().isBlank()) {
                quarto = Integer.parseInt(campoQuarto.getText().trim());
            }
        } catch (NumberFormatException e) {
            mostrarErro("Introduza números válidos para o cartão e o quarto.");
            return;
        }

        LocalHotel local = campoLocal.getValue();
        if (local == null) {
            mostrarErro("Escolha um local.");
            return;
        }

        String nif = campoNif.getText();
        String codigoLocal = local.getCodigo();
        Integer numeroQuarto = quarto;

        botaoPicar.setDisable(true);
        SegundoPlano.executar(
                "Picagem de cartão",
                () -> controller.picar(cartao, nif, codigoLocal, numeroQuarto),
                passagem -> {
                    botaoPicar.setDisable(false);
                    mostrarSucesso(passagem, local);
                    // Limpa os dados do cartão para a pessoa seguinte.
                    campoCartao.clear();
                    campoNif.clear();
                    campoQuarto.clear();
                },
                erro -> {
                    botaoPicar.setDisable(false);
                    mostrarErro(erro.getMessage());
                }
        );
    }

    /** Chamado pelo botão "Voltar ao login". */
    @FXML
    private void voltar() {
        Navegador.mudarPara("LoginView.fxml", "Hotel4You - Login");
    }

    private void mostrarSucesso(Passagem passagem, LocalHotel local) {
        String onde = local.isQuarto() && passagem.getNumeroQuarto() != null
                ? "quarto " + passagem.getNumeroQuarto()
                : local.getNome();
        labelMensagem.setStyle(ESTILO_SUCESSO);
        labelMensagem.setText("Acesso permitido: " + onde + ", às " + passagem.getDataHora().format(HORA) + ".");
    }

    private void mostrarErro(String mensagem) {
        labelMensagem.setStyle(ESTILO_ERRO);
        labelMensagem.setText(mensagem);
    }
}