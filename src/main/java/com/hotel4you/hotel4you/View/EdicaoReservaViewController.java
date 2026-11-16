package com.hotel4you.hotel4you.View;

import com.hotel4you.hotel4you.Controller.EdicaoReservaController;
import com.hotel4you.hotel4you.Model.ConsumoServico;
import com.hotel4you.hotel4you.Model.ProdutoReserva;
import com.hotel4you.hotel4you.Model.Reserva;
import com.hotel4you.hotel4you.Model.Servico;
import com.hotel4you.hotel4you.Utils.Navegador;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import java.util.List;

/**
 * Janela de consulta e edição de reservas.
 */
public class EdicaoReservaViewController {

    private final EdicaoReservaController controller;
    private Reserva reservaSelecionada;

    @FXML
    private ListView<Reserva> listaReservas;
    @FXML
    private Label labelHospede;
    @FXML
    private Label labelQuarto;
    @FXML
    private Label labelDatas;
    @FXML
    private Label labelEstado;
    @FXML
    private Label labelTotal;
    @FXML
    private Label labelMensagem;
    @FXML
    private ListView<String> listaServicos;
    @FXML
    private ListView<String> listaProdutos;
    @FXML
    private ComboBox<Servico> campoServico;
    @FXML
    private TextField campoQuantidade;
    @FXML
    private Button botaoAdicionar;
    @FXML
    private Button botaoRemover;

    public EdicaoReservaViewController(EdicaoReservaController controller) {
        this.controller = controller;
    }

    /**
     * Prepara a janela.
     */
    @FXML
    private void initialize() {

        campoServico.setConverter(new StringConverter<Servico>() {

            @Override
            public String toString(Servico servico) {
                if (servico == null) {
                    return "";
                }

                return servico.getNome() + " - " + servico.getValor() + " €";
            }

            @Override
            public Servico fromString(String texto) {
                return null;
            }
        });

        listaReservas.setCellFactory(lista ->
                new javafx.scene.control.ListCell<Reserva>() {

                    @Override
                    protected void updateItem(Reserva reserva, boolean vazio) {
                        super.updateItem(reserva, vazio);

                        if (vazio || reserva == null) {
                            setText(null);
                        } else {
                            setText("Reserva " + reserva.getId() + " - " + reserva.getCliente().getNome() + " - Quarto " + reserva.getQuarto().getNumero() + " - " + reserva.getEstado());
                        }
                    }
                }
        );

        listaReservas.getSelectionModel().selectedItemProperty().addListener((observavel, anterior, atual) -> {
                    if (atual != null) {
                        mostrarReserva(atual.getId());
                    }
                });

        atualizarReservas();
    }

    /**
     * Atualiza a lista de reservas.
     */
    @FXML
    private void atualizarReservas() {

        try {
            List<Reserva> reservas = controller.listarReservas();
            listaReservas.setItems(FXCollections.observableArrayList(reservas));
        }

        catch (RuntimeException e) {
            labelMensagem.setText(e.getMessage());
        }
    }

    /**
     * Mostra todos os detalhes da reserva.
     */
    private void mostrarReserva(int id) {

        try {
            reservaSelecionada = controller.consultarReserva(id);
            labelHospede.setText("Hóspede: " + reservaSelecionada.getCliente().getNome());
            labelQuarto.setText("Quarto: " + reservaSelecionada.getQuarto().getNumero() + " - " + reservaSelecionada.getQuarto().getTipo());
            labelDatas.setText("Entrada: " + reservaSelecionada.getDataEntrada() + " | Saída: " + reservaSelecionada.getDataSaida());
            labelEstado.setText("Estado: " + reservaSelecionada.getEstado());

            atualizarDetalhes();
            boolean podeEditar = reservaSelecionada.podeEditarServicos();

            botaoAdicionar.setDisable(!podeEditar);
            botaoRemover.setDisable(!podeEditar);
            campoServico.setDisable(!podeEditar);
            campoQuantidade.setDisable(!podeEditar);
            campoServico.setItems(FXCollections.observableArrayList(controller.listarServicos()));
            labelMensagem.setText(podeEditar ? "Pode acrescentar ou remover serviços." : "Esta reserva não pode ser editada.");
        }

        catch (RuntimeException e) {
            labelMensagem.setText(e.getMessage());
        }
    }

    /**
     * Atualiza os serviços, produtos e preço.
     */
    private void atualizarDetalhes() {
        listaServicos.getItems().clear();
        listaProdutos.getItems().clear();

        for (ConsumoServico consumo : reservaSelecionada.getConsumos()) {
            listaServicos.getItems().add(consumo.getServico().getNome() + " | Quantidade: " + consumo.getQuantidade() + " | Total: " + consumo.total(reservaSelecionada.getNoites()) + " €");
        }

        for (ProdutoReserva produto : reservaSelecionada.getProdutos()) {
            listaProdutos.getItems().add(produto.getArtigo().getNome() + " | Quantidade: " + produto.getQuantidade());
        }

        labelTotal.setText("Valor total: " + reservaSelecionada.calcularTotal() + " €");
    }

    /**
     * Acrescenta um serviço à reserva.
     */
    @FXML
    private void onAdicionarServico() {

        if (reservaSelecionada == null) {
            labelMensagem.setText("Selecione uma reserva.");
            return;
        }

        try {
            Servico servico = campoServico.getValue();
            int quantidade = Integer.parseInt(campoQuantidade.getText().trim());
            controller.adicionarServico(reservaSelecionada.getId(), servico, quantidade);
            mostrarReserva(reservaSelecionada.getId());
            campoQuantidade.clear();
            campoServico.getSelectionModel().clearSelection();
            labelMensagem.setText("Serviço adicionado com sucesso.");
        }

        catch (NumberFormatException e) {
            labelMensagem.setText("Introduza uma quantidade válida.");
        }

        catch (RuntimeException e) {
            labelMensagem.setText(e.getMessage());
        }
    }

    /**
     * Remove o serviço selecionado.
     */
    @FXML
    private void onRemoverServico() {

        if (reservaSelecionada == null) {
            labelMensagem.setText("Selecione uma reserva.");
            return;
        }

        int indice = listaServicos.getSelectionModel()
                .getSelectedIndex();

        if (indice < 0) {
            labelMensagem.setText("Selecione um serviço para remover.");
            return;
        }

        try {
            controller.removerServico(reservaSelecionada.getId(), indice);
            mostrarReserva(reservaSelecionada.getId());
            labelMensagem.setText("Serviço removido com sucesso.");

        }

        catch (RuntimeException e) {
            labelMensagem.setText(e.getMessage());
        }
    }

    /**
     * Volta ao menu principal.
     */
    @FXML
    private void onVoltar() {

        Navegador.mudarPara(
                "MenuPrincipalView.fxml",
                "Hotel4You"
        );
    }
}