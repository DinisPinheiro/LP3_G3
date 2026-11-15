package com.hotel4you.hotel4you.View;

import com.hotel4you.hotel4you.Controller.GestaoServicosController;
import com.hotel4you.hotel4you.Model.Servico;
import com.hotel4you.hotel4you.Model.enums.TipoCobranca;
import com.hotel4you.hotel4you.Model.enums.TipoServico;
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
import java.util.Optional;

/**
 * ViewController do ecrã de serviços e extras ({@code GestaoServicosView.fxml}).
 * <p>
 * O gestor escolhe um item na lista para o editar (incluindo o preço, a forma de cobrança e o estado
 * ativo) ou carrega em "Novo". O rececionista só vê a lista (o formulário fica escondido). Só lê os campos e mostra
 * mensagens; as regras e as permissões estão no {@link GestaoServicosController}.
 */
public class GestaoServicosViewController {

    private static final String COR_ERRO = "-fx-text-fill: #b03a2e;";
    private static final String COR_SUCESSO = "-fx-text-fill: #1e8449;";

    private final GestaoServicosController controller;
    /** Item aberto para edição; {@code null} quando se está a criar um item novo. */
    private Servico servicoSelecionado;

    @FXML
    private Label labelTitulo;
    @FXML
    private ListView<Servico> listaServicos;
    @FXML
    private Label labelMensagem;
    @FXML
    private VBox painelFormulario;
    @FXML
    private Label labelModo;
    @FXML
    private ComboBox<TipoServico> campoTipo;
    @FXML
    private TextField campoNome;
    @FXML
    private TextField campoCategoria;
    @FXML
    private TextField campoDescricao;
    @FXML
    private TextField campoPreco;
    @FXML
    private ComboBox<TipoCobranca> campoCobranca;
    @FXML
    private CheckBox campoAtivo;

    /**
     * @param controller controller que configura os serviços e extras
     */
    public GestaoServicosViewController(GestaoServicosController controller) {
        this.controller = controller;
    }

    /** Chamado pelo JavaFX depois de os campos {@code @FXML} serem preenchidos. */
    @FXML
    private void initialize() {
        boolean configura = controller.podeConfigurar();
        painelFormulario.setVisible(configura);
        painelFormulario.setManaged(configura);
        labelTitulo.setText(configura ? "Serviços e extras" : "Serviços e extras (consulta)");

        listaServicos.setCellFactory(lista -> new ListCell<Servico>() {
            @Override
            protected void updateItem(Servico servico, boolean vazio) {
                super.updateItem(servico, vazio);
                setText(vazio || servico == null ? null : descrever(servico));
            }
        });
        atualizarLista();
        if (configura) {
            configurarTipos();
            listaServicos.getSelectionModel().selectedItemProperty().addListener((observavel, antigo, novo) -> {
                if (novo != null) {
                    labelMensagem.setText("");
                    carregarNoFormulario(novo);
                }
            });
            limparFormulario();
        }
    }

    /** Chamado pelo botão "Novo". */
    @FXML
    private void onNovo() {
        limparFormulario();
        labelMensagem.setText("");
    }

    /** Chamado pelo botão "Guardar": cria o item novo ou grava as alterações do item aberto. */
    @FXML
    private void onGuardar() {
        Optional<BigDecimal> preco = Utils.converterDecimal(campoPreco.getText());
        if (preco.isEmpty()) {
            mostrar("Preço inválido. Use um número, por exemplo 8,50.", COR_ERRO);
            return;
        }
        try {
            if (servicoSelecionado == null) {
                Servico criado = controller.criarServico(campoTipo.getValue(), campoNome.getText(),
                        campoCategoria.getText(), campoDescricao.getText(), preco.get(), campoCobranca.getValue());
                atualizarLista();
                selecionarPorId(criado.getId());
                mostrar("'" + criado.getNome() + "' criado.", COR_SUCESSO);
            } else {
                int id = servicoSelecionado.getId();
                controller.editarServico(id, campoTipo.getValue(), campoNome.getText(), campoCategoria.getText(),
                        campoDescricao.getText(), preco.get(), campoCobranca.getValue(), campoAtivo.isSelected());
                String nome = campoNome.getText().trim();
                atualizarLista();
                selecionarPorId(id);
                mostrar("'" + nome + "' guardado.", COR_SUCESSO);
            }
        } catch (IllegalArgumentException | SecurityException e) {
            mostrar(e.getMessage(), COR_ERRO);
        } catch (RuntimeException e) {
            mostrar("Não foi possível guardar: " + e.getMessage(), COR_ERRO);
        }
    }

    /** Chamado pelo botão "Voltar ao menu". */
    @FXML
    private void onVoltar() {
        Navegador.mudarPara("MenuPrincipalView.fxml", "Hotel4You");
    }

    private void configurarTipos() {
        campoTipo.setItems(FXCollections.observableArrayList(TipoServico.values()));
        campoTipo.setConverter(new StringConverter<TipoServico>() {
            @Override
            public String toString(TipoServico tipo) {
                return tipo == null ? "" : tipo.getNome();
            }

            @Override
            public TipoServico fromString(String texto) {
                return null;
            }
        });
        campoCobranca.setItems(FXCollections.observableArrayList(TipoCobranca.values()));
        campoCobranca.setConverter(new StringConverter<TipoCobranca>() {
            @Override
            public String toString(TipoCobranca cobranca) {
                return cobranca == null ? "" : cobranca.getNome();
            }

            @Override
            public TipoCobranca fromString(String texto) {
                return null;
            }
        });
    }

    private void atualizarLista() {
        try {
            listaServicos.setItems(FXCollections.observableArrayList(controller.consultarServicos()));
        } catch (RuntimeException e) {
            mostrar(e.getMessage(), COR_ERRO);
        }
    }

    private void selecionarPorId(int id) {
        for (Servico servico : listaServicos.getItems()) {
            if (servico.getId() == id) {
                listaServicos.getSelectionModel().select(servico);
                return;
            }
        }
    }

    private void limparFormulario() {
        servicoSelecionado = null;
        listaServicos.getSelectionModel().clearSelection();
        campoTipo.setValue(null);
        campoNome.clear();
        campoCategoria.clear();
        campoDescricao.clear();
        campoPreco.clear();
        campoCobranca.setValue(TipoCobranca.POR_UNIDADE);
        campoAtivo.setSelected(true);
        campoAtivo.setDisable(true);
        labelModo.setText("Novo serviço ou extra");
    }

    private void carregarNoFormulario(Servico servico) {
        servicoSelecionado = servico;
        campoTipo.setValue(servico.getTipo());
        campoNome.setText(servico.getNome());
        campoCategoria.setText(servico.getCategoria());
        campoDescricao.setText(servico.getDescricao());
        campoPreco.setText(servico.getValor().toPlainString().replace('.', ','));
        campoCobranca.setValue(servico.getCobranca());
        campoAtivo.setSelected(servico.isAtivo());
        campoAtivo.setDisable(false);
        labelModo.setText("A editar: " + servico.getNome());
    }

    private void mostrar(String texto, String estilo) {
        labelMensagem.setText(texto);
        labelMensagem.setStyle(estilo);
    }

    private static String descrever(Servico servico) {
        String categoria = servico.getCategoria().isBlank() ? "" : "  |  " + servico.getCategoria();
        String estado = servico.isAtivo() ? "" : "  |  Inativo";
        String descricao = servico.getDescricao().isBlank() ? "" : "\n" + servico.getDescricao();
        return servico.getNome() + "  |  " + servico.getTipo().getNome() + categoria + "\n"
                + Utils.formatarPreco(servico.getValor()) + " " + servico.getCobranca().getNome().toLowerCase()
                + estado + descricao;
    }
}
