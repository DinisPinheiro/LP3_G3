package com.hotel4you.hotel4you.View;

import com.hotel4you.hotel4you.Controller.GestaoQuartosController;
import com.hotel4you.hotel4you.Model.Equipamento;
import com.hotel4you.hotel4you.Model.Quarto;
import com.hotel4you.hotel4you.Model.enums.TipoQuarto;
import com.hotel4you.hotel4you.Model.enums.Vista;
import com.hotel4you.hotel4you.Utils.Navegador;
import com.hotel4you.hotel4you.Utils.Utils;
import javafx.collections.FXCollections;
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
 * ViewController do ecrã de configuração de quartos ({@code GestaoQuartosView.fxml}).
 * <p>
 * Escolher um quarto na lista abre-o para edição; "Novo quarto" limpa o formulário para criar outro.
 * Só lê os campos e mostra mensagens; as regras (número válido, preço, permissões) estão no
 * {@link GestaoQuartosController}.
 * <p>
 * Apoio de IA: Claude (Anthropic), revisto pela equipa.
 */
public class GestaoQuartosViewController {

    private static final String COR_ERRO = "-fx-text-fill: #b03a2e;";
    private static final String COR_SUCESSO = "-fx-text-fill: #1e8449;";

    private final GestaoQuartosController controller;
    private final List<CheckBox> caixasEquipamentos = new ArrayList<>();
    /** Quarto aberto para edição; {@code null} quando se está a criar um quarto novo. */
    private Quarto quartoSelecionado;

    @FXML
    private ListView<Quarto> listaQuartos;
    @FXML
    private Label labelModo;
    @FXML
    private TextField campoNumero;
    @FXML
    private TextField campoLocal;
    @FXML
    private ComboBox<TipoQuarto> campoTipo;
    @FXML
    private TextField campoPreco;
    @FXML
    private ComboBox<Vista> campoVista;
    @FXML
    private CheckBox campoAtivo;
    @FXML
    private Label labelPrecoNoite;
    @FXML
    private VBox caixaEquipamentos;
    @FXML
    private TextField campoNovoEquipamento;
    @FXML
    private Label labelMensagem;

    /**
     * @param controller controller que configura os quartos
     */
    public GestaoQuartosViewController(GestaoQuartosController controller) {
        this.controller = controller;
    }

    /** Chamado pelo JavaFX depois de os campos {@code @FXML} serem preenchidos. */
    @FXML
    private void initialize() {
        configurarCombos();
        listaQuartos.setCellFactory(lista -> new ListCell<Quarto>() {
            @Override
            protected void updateItem(Quarto quarto, boolean vazio) {
                super.updateItem(quarto, vazio);
                setText(vazio || quarto == null ? null : descrever(quarto));
            }
        });
        listaQuartos.getSelectionModel().selectedItemProperty().addListener((observavel, antigo, novo) -> {
            if (novo != null) {
                labelMensagem.setText("");
                carregarNoFormulario(novo);
            }
        });
        campoPreco.textProperty().addListener((observavel, antigo, novo) -> atualizarPrecoNoite());
        campoVista.setOnAction(evento -> atualizarPrecoNoite());
        try {
            atualizarLista();
            atualizarEquipamentos(new HashSet<>());
        } catch (RuntimeException e) {
            mostrar(e.getMessage(), COR_ERRO);
        }
        limparFormulario();
    }

    /** Chamado pelo botão "Novo quarto". */
    @FXML
    private void onNovo() {
        limparFormulario();
        labelMensagem.setText("");
    }

    /** Chamado pelo botão "Guardar": cria o quarto novo ou grava as alterações do quarto aberto. */
    @FXML
    private void onGuardar() {
        Optional<BigDecimal> preco = Utils.converterDecimal(campoPreco.getText());
        if (preco.isEmpty()) {
            mostrar("Preço base inválido. Use um número, por exemplo 60,00.", COR_ERRO);
            return;
        }
        try {
            if (quartoSelecionado == null) {
                criar(preco.get());
            } else {
                editar(preco.get());
            }
        } catch (IllegalArgumentException | SecurityException e) {
            mostrar(e.getMessage(), COR_ERRO);
        } catch (RuntimeException e) {
            mostrar("Não foi possível guardar o quarto: " + e.getMessage(), COR_ERRO);
        }
    }

    /** Chamado pelo botão "Adicionar equipamento": acrescenta ao catálogo e marca-o no quarto. */
    @FXML
    private void onAdicionarEquipamento() {
        try {
            Equipamento novo = controller.criarEquipamento(campoNovoEquipamento.getText());
            Set<Integer> marcados = idsMarcados();
            marcados.add(novo.getId());
            atualizarEquipamentos(marcados);
            campoNovoEquipamento.clear();
            mostrar("Equipamento '" + novo.getNome() + "' adicionado. Carregue em Guardar para o associar ao quarto.",
                    COR_SUCESSO);
        } catch (IllegalArgumentException | SecurityException e) {
            mostrar(e.getMessage(), COR_ERRO);
        } catch (RuntimeException e) {
            mostrar("Não foi possível adicionar o equipamento: " + e.getMessage(), COR_ERRO);
        }
    }

    /** Chamado pelo botão "Voltar ao menu". */
    @FXML
    private void onVoltar() {
        Navegador.mudarPara("MenuPrincipalView.fxml", "Hotel4You");
    }

    private void criar(BigDecimal preco) {
        Optional<Integer> numero = Utils.converterInteiro(campoNumero.getText());
        if (numero.isEmpty()) {
            mostrar("Número de quarto inválido. Escreva só o número, por exemplo 205.", COR_ERRO);
            return;
        }
        Quarto criado = controller.criarQuarto(numero.get(), campoLocal.getText(), campoTipo.getValue(),
                preco, campoVista.getValue(), equipamentosMarcados());
        atualizarLista();
        selecionarPorId(criado.getId());
        mostrar("Quarto " + criado.getNumero() + " criado.", COR_SUCESSO);
    }

    private void editar(BigDecimal preco) {
        controller.editarQuarto(quartoSelecionado.getId(), campoLocal.getText(), campoTipo.getValue(), preco,
                campoVista.getValue(), campoAtivo.isSelected(), equipamentosMarcados());
        int id = quartoSelecionado.getId();
        int numero = quartoSelecionado.getNumero();
        atualizarLista();
        selecionarPorId(id);
        mostrar("Quarto " + numero + " guardado.", COR_SUCESSO);
    }

    private void configurarCombos() {
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
        campoVista.setItems(FXCollections.observableArrayList(Vista.values()));
        campoVista.setConverter(new StringConverter<Vista>() {
            @Override
            public String toString(Vista vista) {
                return vista == null ? "" : vista.getNome();
            }

            @Override
            public Vista fromString(String texto) {
                return null;
            }
        });
    }

    private void limparFormulario() {
        quartoSelecionado = null;
        listaQuartos.getSelectionModel().clearSelection();
        campoNumero.clear();
        campoNumero.setDisable(false);
        campoLocal.clear();
        campoTipo.setValue(null);
        campoPreco.clear();
        campoVista.setValue(Vista.NENHUMA);
        campoAtivo.setSelected(true);
        campoAtivo.setDisable(true);
        marcarEquipamentos(new HashSet<>());
        labelModo.setText("Novo quarto");
        atualizarPrecoNoite();
    }

    private void carregarNoFormulario(Quarto quarto) {
        quartoSelecionado = quarto;
        campoNumero.setText(String.valueOf(quarto.getNumero()));
        campoNumero.setDisable(true);
        campoLocal.setText(quarto.getLocal());
        campoTipo.setValue(quarto.getTipo());
        campoPreco.setText(quarto.getPrecoBase().toPlainString().replace('.', ','));
        campoVista.setValue(quarto.getVista());
        campoAtivo.setSelected(quarto.isAtivo());
        campoAtivo.setDisable(false);
        Set<Integer> ids = new HashSet<>();
        quarto.getEquipamentos().forEach(equipamento -> ids.add(equipamento.getId()));
        marcarEquipamentos(ids);
        labelModo.setText("A editar o quarto " + quarto.getNumero() + " (piso " + quarto.getPiso() + ")");
        atualizarPrecoNoite();
    }

    private void atualizarLista() {
        listaQuartos.setItems(FXCollections.observableArrayList(controller.consultarQuartos()));
    }

    private void selecionarPorId(int id) {
        for (Quarto quarto : listaQuartos.getItems()) {
            if (quarto.getId() == id) {
                listaQuartos.getSelectionModel().select(quarto);
                return;
            }
        }
    }

    /** Reconstrói as caixas de seleção a partir do catálogo, marcando os equipamentos indicados. */
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

    private Set<Integer> idsMarcados() {
        Set<Integer> ids = new HashSet<>();
        equipamentosMarcados().forEach(equipamento -> ids.add(equipamento.getId()));
        return ids;
    }

    private void atualizarPrecoNoite() {
        Optional<BigDecimal> preco = Utils.converterDecimal(campoPreco.getText());
        Vista vista = campoVista.getValue();
        if (preco.isEmpty() || preco.get().signum() < 0 || vista == null) {
            labelPrecoNoite.setText("-");
            return;
        }
        labelPrecoNoite.setText(Utils.formatarPreco(controller.calcularPrecoNoite(preco.get(), vista))
                + " (preço base + " + Utils.formatarPreco(vista.getSuplemento()) + " da vista)");
    }

    private void mostrar(String texto, String estilo) {
        labelMensagem.setText(texto);
        labelMensagem.setStyle(estilo);
    }

    private static String descrever(Quarto quarto) {
        return "Quarto " + quarto.getNumero() + "  |  " + quarto.getTipo().getNome() + "  |  piso " + quarto.getPiso()
                + "\n" + quarto.getVista().getNome() + "  |  " + Utils.formatarPreco(quarto.precoNoite()) + "/noite  |  "
                + (quarto.isAtivo() ? "Ativo" : "Inativo") + "  |  " + quarto.getEquipamentos().size() + " equip.";
    }
}
