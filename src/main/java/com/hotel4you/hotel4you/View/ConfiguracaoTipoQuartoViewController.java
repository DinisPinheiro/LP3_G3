package com.hotel4you.hotel4you.View;

import com.hotel4you.hotel4you.Controller.GestaoQuartosController;
import com.hotel4you.hotel4you.Model.Artigo;
import com.hotel4you.hotel4you.Model.ConfiguracaoTipoQuarto;
import com.hotel4you.hotel4you.Model.Equipamento;
import com.hotel4you.hotel4you.Model.ProdutoQuarto;
import com.hotel4you.hotel4you.Model.enums.TipoQuarto;
import com.hotel4you.hotel4you.Utils.Navegador;
import com.hotel4you.hotel4you.Utils.Utils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * ViewController do ecrã de configuração por tipo de quarto ({@code ConfiguracaoTipoQuartoView.fxml}, S6-09).
 * <p>
 * O gestor escolhe um tipo (individual, duplo ou família) e define o preço base e os equipamentos dos
 * quartos desse tipo. Os quartos herdam esta configuração; os que têm exceções não mudam. Só lê os campos e
 * mostra mensagens; as regras estão no {@link GestaoQuartosController}.
 */
public class ConfiguracaoTipoQuartoViewController {

    private static final String COR_ERRO = "-fx-text-fill: #b03a2e;";
    private static final String COR_SUCESSO = "-fx-text-fill: #1e8449;";

    private final GestaoQuartosController controller;
    private final List<CheckBox> caixasEquipamentos = new ArrayList<>();
    private final ObservableList<ProdutoQuarto> produtosEscolhidos = FXCollections.observableArrayList();

    @FXML
    private ComboBox<TipoQuarto> campoTipo;
    @FXML
    private Label labelEstado;
    @FXML
    private TextField campoPreco;
    @FXML
    private VBox caixaEquipamentos;
    @FXML
    private ComboBox<Artigo> campoProdutoStock;
    @FXML
    private TextField campoQuantidadeProduto;
    @FXML
    private ListView<ProdutoQuarto> listaProdutos;
    @FXML
    private Label labelMensagem;

    /**
     * @param controller controller que configura os quartos e os tipos de quarto
     */
    public ConfiguracaoTipoQuartoViewController(GestaoQuartosController controller) {
        this.controller = controller;
    }

    /** Chamado pelo JavaFX depois de os campos {@code @FXML} serem preenchidos. */
    @FXML
    private void initialize() {
        campoTipo.setItems(FXCollections.observableArrayList(TipoQuarto.values()));
        campoTipo.setConverter(new StringConverter<TipoQuarto>() {
            @Override
            public String toString(TipoQuarto tipo) {
                return tipo == null ? "" : tipo.getNome();
            }

            @Override
            public TipoQuarto fromString(String texto) {
                return null;
            }
        });
        campoTipo.setOnAction(evento -> carregarTipo());
        listaProdutos.setItems(produtosEscolhidos);
        listaProdutos.setCellFactory(lista -> new ListCell<ProdutoQuarto>() {
            @Override
            protected void updateItem(ProdutoQuarto produto, boolean vazio) {
                super.updateItem(produto, vazio);
                setText(vazio || produto == null ? null : produto.getArtigo().getNome() + "  x"
                        + produto.getQuantidade() + " " + produto.getArtigo().getUnidade());
            }
        });
        campoProdutoStock.setConverter(new StringConverter<Artigo>() {
            @Override
            public String toString(Artigo artigo) {
                return artigo == null ? "" : artigo.getNome() + "  (disponível: " + artigo.getQuantidadeDisponivel()
                        + " " + artigo.getUnidade() + ")";
            }

            @Override
            public Artigo fromString(String texto) {
                return null;
            }
        });
        try {
            atualizarEquipamentos(new HashSet<>());
            campoProdutoStock.setItems(FXCollections.observableArrayList(controller.consultarProdutosStock()));
        } catch (RuntimeException e) {
            mostrar(e.getMessage(), COR_ERRO);
        }
        labelEstado.setText("Escolha um tipo de quarto.");
    }

    /** Chamado pelo botão "Guardar configuração do tipo". */
    @FXML
    private void onGuardar() {
        TipoQuarto tipo = campoTipo.getValue();
        if (tipo == null) {
            mostrar("Escolha o tipo de quarto.", COR_ERRO);
            return;
        }
        Optional<BigDecimal> preco = Utils.converterDecimal(campoPreco.getText());
        if (preco.isEmpty()) {
            mostrar("Preço base inválido. Use um número, por exemplo 60,00.", COR_ERRO);
            return;
        }
        try {
            int alterados = controller.guardarConfiguracaoTipo(tipo, preco.get(), equipamentosMarcados(),
                    new ArrayList<>(produtosEscolhidos));
            carregarTipo();
            mostrar("Configuração do tipo " + tipo.getNome() + " guardada. "
                    + (alterados == 0 ? "Nenhum quarto mudou."
                    : alterados + (alterados == 1 ? " quarto herdou" : " quartos herdaram") + " a configuração nova.")
                    + " Os quartos com exceções não mudam.", COR_SUCESSO);
        } catch (IllegalArgumentException | SecurityException e) {
            mostrar(e.getMessage(), COR_ERRO);
        } catch (RuntimeException e) {
            mostrar("Não foi possível guardar a configuração: " + e.getMessage(), COR_ERRO);
        }
    }

    /** Chamado pelo botão "Pôr no tipo": junta o produto do stock à configuração (se já lá estiver, muda a quantidade). */
    @FXML
    private void onAdicionarProduto() {
        Artigo artigo = campoProdutoStock.getValue();
        if (artigo == null) {
            mostrar("Escolha um produto do stock.", COR_ERRO);
            return;
        }
        Optional<Integer> quantidade = Utils.converterInteiro(campoQuantidadeProduto.getText());
        if (quantidade.isEmpty() || quantidade.get() < 1) {
            mostrar("Quantidade inválida. Escreva um número inteiro, por exemplo 2.", COR_ERRO);
            return;
        }
        produtosEscolhidos.removeIf(produto -> produto.getArtigo().getId() == artigo.getId());
        produtosEscolhidos.add(new ProdutoQuarto(artigo, quantidade.get()));
        campoQuantidadeProduto.clear();
        mostrar("'" + artigo.getNome() + "' x" + quantidade.get() + ". Carregue em Guardar para gravar.", COR_SUCESSO);
    }

    /** Chamado pelo botão "Tirar do tipo": retira o produto selecionado. */
    @FXML
    private void onRemoverProduto() {
        ProdutoQuarto produto = listaProdutos.getSelectionModel().getSelectedItem();
        if (produto == null) {
            mostrar("Selecione um produto na lista.", COR_ERRO);
            return;
        }
        produtosEscolhidos.remove(produto);
        mostrar("'" + produto.getArtigo().getNome() + "' retirado. Carregue em Guardar para gravar.", COR_SUCESSO);
    }

    /** Chamado pelo botão "Voltar ao menu". */
    @FXML
    private void onVoltar() {
        Navegador.mudarPara("MenuPrincipalView.fxml", "Hotel4You");
    }

    /** Mostra no formulário a configuração guardada do tipo escolhido (ou limpa-o se ainda não existe). */
    private void carregarTipo() {
        TipoQuarto tipo = campoTipo.getValue();
        if (tipo == null) {
            return;
        }
        try {
            Optional<ConfiguracaoTipoQuarto> configuracao = controller.consultarConfiguracaoTipo(tipo);
            Set<Integer> ids = new HashSet<>();
            if (configuracao.isPresent()) {
                campoPreco.setText(configuracao.get().getPrecoBase().toPlainString().replace('.', ','));
                configuracao.get().getEquipamentos().forEach(equipamento -> ids.add(equipamento.getId()));
                produtosEscolhidos.setAll(configuracao.get().getProdutos());
                labelEstado.setText("Configuração do tipo " + tipo.getNome() + " (os quartos deste tipo herdam-na).");
            } else {
                campoPreco.clear();
                produtosEscolhidos.clear();
                labelEstado.setText("O tipo " + tipo.getNome() + " ainda não tem configuração.");
            }
            marcarEquipamentos(ids);
            labelMensagem.setText("");
        } catch (RuntimeException e) {
            mostrar(e.getMessage(), COR_ERRO);
        }
    }

    private void atualizarEquipamentos(Set<Integer> marcados) {
        caixasEquipamentos.clear();
        caixaEquipamentos.getChildren().clear();
        for (Equipamento equipamento : controller.consultarEquipamentos()) {
            CheckBox caixa = new CheckBox(equipamento.getNome());
            caixa.setUserData(equipamento);
            caixa.setSelected(marcados.contains(equipamento.getId()));
            caixasEquipamentos.add(caixa);
            caixaEquipamentos.getChildren().add(caixa);
        }
    }

    private void marcarEquipamentos(Set<Integer> ids) {
        for (CheckBox caixa : caixasEquipamentos) {
            caixa.setSelected(ids.contains(((Equipamento) caixa.getUserData()).getId()));
        }
    }

    private List<Equipamento> equipamentosMarcados() {
        List<Equipamento> marcados = new ArrayList<>();
        for (CheckBox caixa : caixasEquipamentos) {
            if (caixa.isSelected()) {
                marcados.add((Equipamento) caixa.getUserData());
            }
        }
        return marcados;
    }

    private void mostrar(String texto, String estilo) {
        labelMensagem.setText(texto);
        labelMensagem.setStyle(estilo);
    }
}
