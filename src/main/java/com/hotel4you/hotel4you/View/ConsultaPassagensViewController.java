
package com.hotel4you.hotel4you.View;

import com.hotel4you.hotel4you.Controller.PassagemController;
import com.hotel4you.hotel4you.Model.Passagem;
import com.hotel4you.hotel4you.Utils.Navegador;

import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class ConsultaPassagensViewController {

    private final PassagemController controller;

    @FXML
    private TextField campoNumero;

    @FXML
    private ComboBox<String> tipoPesquisa;

    @FXML
    private ListView<String> listaPassagens;

    @FXML
    private Label labelMensagem;

    public ConsultaPassagensViewController(
            PassagemController controller) {

        this.controller = controller;
    }

    @FXML
    private void initialize() {

        tipoPesquisa.getItems().addAll(
                "Cartão",
                "Quarto"
        );

        tipoPesquisa.getSelectionModel().selectFirst();
    }

    @FXML
    private void pesquisar() {

        try {

            int numero = Integer.parseInt(
                    campoNumero.getText().trim()
            );

            List<Passagem> passagens;

            if (tipoPesquisa.getValue().equals("Cartão")) {

                passagens = controller.consultarCartao(numero);

            } else {

                passagens = controller.consultarQuarto(numero);
            }

            listaPassagens.getItems().clear();

            for (Passagem passagem : passagens) {

                String linha =
                        "Cartão " + passagem.getNumeroCartao()
                                + " | " + passagem.getLocal()
                                + " | " + passagem.getDataHora().format(
                                DateTimeFormatter.ofPattern(
                                        "dd/MM/yyyy HH:mm:ss")
                        );

                listaPassagens.getItems().add(linha);
            }

            labelMensagem.setText(
                    passagens.size()
                            + " passagem(ns) encontrada(s)."
            );

        } catch (NumberFormatException e) {

            labelMensagem.setText(
                    "Introduza um número válido."
            );

        } catch (RuntimeException e) {

            listaPassagens.getItems().clear();

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
