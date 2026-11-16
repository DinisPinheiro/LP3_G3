package com.hotel4you.hotel4you.View;

import com.hotel4you.hotel4you.Controller.PassagemController;
import com.hotel4you.hotel4you.Model.Passagem;
import com.hotel4you.hotel4you.Utils.Navegador;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.time.format.DateTimeFormatter;

public class PicagemCartaoViewController {

    private final PassagemController controller;

    @FXML
    private TextField campoCartao;

    @FXML
    private TextField campoQuarto;

    @FXML
    private ComboBox<String> campoLocal;

    @FXML
    private Label labelMensagem;

    public PicagemCartaoViewController(
            PassagemController controller) {

        this.controller = controller;
    }

    @FXML
    private void initialize() {

        campoLocal.getItems().addAll(
                "QUARTO",
                "RESTAURANTE",
                "PISCINA",
                "COZINHA"
        );

        campoLocal.getSelectionModel().selectFirst();
    }

    @FXML
    private void registar() {

        try {

            int cartao = Integer.parseInt(
                    campoCartao.getText().trim()
            );

            Integer quarto = null;

            if (!campoQuarto.getText().isBlank()) {

                quarto = Integer.parseInt(
                        campoQuarto.getText().trim()
                );
            }

            Passagem passagem = controller.registar(
                    cartao,
                    campoLocal.getValue(),
                    quarto
            );

            labelMensagem.setText(
                    "Acesso registado às " +
                            passagem.getDataHora().format(
                                    DateTimeFormatter.ofPattern(
                                            "dd/MM/yyyy HH:mm:ss")
                            )
            );

        } catch (NumberFormatException e) {

            labelMensagem.setText(
                    "Introduza números válidos " +
                            "para cartão e quarto."
            );

        } catch (RuntimeException e) {

            labelMensagem.setText(e.getMessage());
        }
    }

    @FXML
    private void voltar() {

        Navegador.mudarPara(
                "MenuPrincipalView.fxml",
                "Hotel4You"
        );
    }
}
