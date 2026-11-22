package com.hotel4you.hotel4you.View;

import com.hotel4you.hotel4you.Controller.PassagemController;
import com.hotel4you.hotel4you.Model.LocalHotel;
import com.hotel4you.hotel4you.Model.Passagem;
import com.hotel4you.hotel4you.Utils.Navegador;
import com.hotel4you.hotel4you.Utils.SegundoPlano;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ViewController da consulta de passagens do gestor ({@code ConsultaPassagensView.fxml}):
 * por cartão, por quarto e por local (S6-06). A consulta corre em segundo plano (S6-01).
 */
public class ConsultaPassagensViewController {

    private static final String POR_CARTAO = "Cartão";
    private static final String POR_QUARTO = "Quarto";
    private static final String POR_LOCAL = "Local";
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final PassagemController controller;

    /** Nome de cada local pelo seu código, para mostrar "Restaurante" em vez de "RESTAURANTE". */
    private final Map<String, String> nomesDosLocais = new HashMap<>();

    @FXML
    private TextField campoNumero;

    @FXML
    private ComboBox<String> tipoPesquisa;

    @FXML
    private ComboBox<LocalHotel> campoLocal;

    @FXML
    private ListView<String> listaPassagens;

    @FXML
    private Label labelMensagem;

    /**
     * @param controller controller que consulta as passagens (só o gestor tem acesso)
     */
    public ConsultaPassagensViewController(PassagemController controller) {
        this.controller = controller;
    }

    @FXML
    private void initialize() {
        tipoPesquisa.getItems().addAll(POR_CARTAO, POR_QUARTO, POR_LOCAL);
        tipoPesquisa.getSelectionModel().selectFirst();

        // Por local escolhe-se de uma lista; por cartão e por quarto escreve-se o número.
        tipoPesquisa.valueProperty().addListener((observavel, anterior, tipo) -> {
            boolean porLocal = POR_LOCAL.equals(tipo);
            campoLocal.setVisible(porLocal);
            campoLocal.setManaged(porLocal);
            campoNumero.setVisible(!porLocal);
            campoNumero.setManaged(!porLocal);
        });

        SegundoPlano.executar(
                "Locais do hotel",
                controller::listarLocais,
                locais -> {
                    campoLocal.getItems().setAll(locais);
                    campoLocal.getSelectionModel().selectFirst();
                    for (LocalHotel local : locais) {
                        nomesDosLocais.put(local.getCodigo(), local.getNome());
                    }
                },
                erro -> labelMensagem.setText(erro.getMessage())
        );
    }

    @FXML
    private void pesquisar() {
        String tipo = tipoPesquisa.getValue();
        LocalHotel local = campoLocal.getValue();
        int numero = 0;

        try {
            if (POR_LOCAL.equals(tipo)) {
                if (local == null) {
                    labelMensagem.setText("Escolha um local.");
                    return;
                }
            } else {
                numero = Integer.parseInt(campoNumero.getText().trim());
            }
        } catch (NumberFormatException e) {
            labelMensagem.setText("Introduza um número válido.");
            return;
        }

        int numeroPesquisado = numero;
        SegundoPlano.executar(
                "Passagens por " + tipo,
                () -> {
                    if (POR_CARTAO.equals(tipo)) {
                        return controller.consultarCartao(numeroPesquisado);
                    }
                    if (POR_QUARTO.equals(tipo)) {
                        return controller.consultarQuarto(numeroPesquisado);
                    }
                    return controller.consultarLocal(local.getCodigo());
                },
                this::mostrar,
                erro -> {
                    listaPassagens.getItems().clear();
                    labelMensagem.setText(erro.getMessage());
                }
        );
    }

    @FXML
    private void voltar() {
        Navegador.mudarPara("MenuPrincipalView.fxml", "Hotel4You");
    }

    private void mostrar(List<Passagem> passagens) {
        listaPassagens.getItems().clear();
        for (Passagem passagem : passagens) {
            listaPassagens.getItems().add(
                    "Cartão " + passagem.getNumeroCartao()
                            + " | " + descreverLocal(passagem)
                            + " | " + passagem.getDataHora().format(HORA));
        }
        labelMensagem.setText(passagens.size() + " passagem(ns) encontrada(s).");
    }

    private String descreverLocal(Passagem passagem) {
        if (LocalHotel.QUARTO.equals(passagem.getLocal()) && passagem.getNumeroQuarto() != null) {
            return "Quarto " + passagem.getNumeroQuarto();
        }
        return nomesDosLocais.getOrDefault(passagem.getLocal(), passagem.getLocal());
    }
}