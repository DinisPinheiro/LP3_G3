package com.hotel4you.hotel4you.View;

import com.hotel4you.hotel4you.Controller.EdicaoReservaController;
import com.hotel4you.hotel4you.Model.Artigo;
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
 * <p>
 * S6-05: nas reservas pendentes ou com check-in também se muda a quantidade dos produtos do stock
 * (por exemplo, 1 toalha para uma só pessoa num quarto duplo); o stock disponível é ajustado.
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
    @FXML
    private ComboBox<Artigo> campoProduto;
    @FXML
    private TextField campoQuantidadeProduto;
    @FXML
    private Button botaoAlterarProduto;
    @FXML
    private Button botaoRemoverProduto;

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

        campoProduto.setConverter(new StringConverter<Artigo>() {

            @Override
            public String toString(Artigo artigo) {
                if (artigo == null) {
                    return "";
                }
                return artigo.getNome() + " (disponível: " + artigo.getQuantidadeDisponivel() + " " + artigo.getUnidade() + ")";
            }

            @Override
            public Artigo fromString(String texto) {
                return null;
            }
        });

        // Ao escolher um produto da reserva, preenche o produto e a quantidade para os alterar.
        listaProdutos.getSelectionModel().selectedIndexProperty().addListener((observavel, anterior, atual) -> {
            int indice = atual.intValue();
            if (reservaSelecionada == null || indice < 0 || indice >= reservaSelecionada.getProdutos().size()) {
                return;
            }
            ProdutoReserva produto = reservaSelecionada.getProdutos().get(indice);
            for (Artigo artigo : campoProduto.getItems()) {
                if (artigo.getId() == produto.getArtigo().getId()) {
                    campoProduto.setValue(artigo);
                }
            }
            campoQuantidadeProduto.setText(String.valueOf(produto.getQuantidade()));
        });

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

            boolean podeEditarProdutos = reservaSelecionada.podeEditarProdutos();
            botaoAlterarProduto.setDisable(!podeEditarProdutos);
            botaoRemoverProduto.setDisable(!podeEditarProdutos);
            campoProduto.setDisable(!podeEditarProdutos);
            campoQuantidadeProduto.setDisable(!podeEditarProdutos);
            campoProduto.setItems(FXCollections.observableArrayList(controller.listarProdutosStock()));

            if (podeEditar) {
                labelMensagem.setText("Pode acrescentar ou remover serviços e alterar os produtos.");
            } else if (podeEditarProdutos) {
                labelMensagem.setText("Os serviços só se editam depois do check-in; os produtos já se podem alterar.");
            } else {
                labelMensagem.setText("Esta reserva não pode ser editada.");
            }
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
     * Muda a quantidade do produto escolhido na reserva (ou junta-o, se ainda não estiver).
     * Com quantidade 0 o produto sai da reserva.
     */
    @FXML
    private void onAlterarProduto() {

        if (reservaSelecionada == null) {
            labelMensagem.setText("Selecione uma reserva.");
            return;
        }

        Artigo artigo = campoProduto.getValue();
        if (artigo == null) {
            labelMensagem.setText("Escolha um produto do stock.");
            return;
        }

        try {
            int quantidade = Integer.parseInt(campoQuantidadeProduto.getText().trim());
            controller.alterarProduto(reservaSelecionada.getId(), artigo.getId(), quantidade);
            mostrarReserva(reservaSelecionada.getId());
            campoQuantidadeProduto.clear();
            labelMensagem.setText(quantidade == 0 ? "Produto retirado da reserva." : "Quantidade do produto alterada.");
        }

        catch (NumberFormatException e) {
            labelMensagem.setText("Introduza uma quantidade válida (0 retira o produto).");
        }

        catch (RuntimeException e) {
            labelMensagem.setText(e.getMessage());
        }
    }

    /**
     * Retira da reserva o produto selecionado na lista; volta ao stock disponível.
     */
    @FXML
    private void onRemoverProduto() {

        if (reservaSelecionada == null) {
            labelMensagem.setText("Selecione uma reserva.");
            return;
        }

        int indice = listaProdutos.getSelectionModel().getSelectedIndex();
        if (indice < 0) {
            labelMensagem.setText("Selecione um produto para remover.");
            return;
        }

        try {
            ProdutoReserva produto = reservaSelecionada.getProdutos().get(indice);
            controller.alterarProduto(reservaSelecionada.getId(), produto.getArtigo().getId(), 0);
            mostrarReserva(reservaSelecionada.getId());
            labelMensagem.setText("Produto retirado da reserva.");
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