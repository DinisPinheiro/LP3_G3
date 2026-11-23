package com.hotel4you.hotel4you.View;

import com.hotel4you.hotel4you.Controller.GestaoQuartosController;
import com.hotel4you.hotel4you.Model.Artigo;
import com.hotel4you.hotel4you.Model.ConfiguracaoTipoQuarto;
import com.hotel4you.hotel4you.Model.Equipamento;
import com.hotel4you.hotel4you.Model.ProdutoQuarto;
import com.hotel4you.hotel4you.Model.Quarto;
import com.hotel4you.hotel4you.Model.enums.TipoQuarto;
import com.hotel4you.hotel4you.Model.enums.Vista;
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
 * ViewController do ecrã de configuração de quartos ({@code GestaoQuartosView.fxml}).
 * <p>
 * Escolher um quarto na lista abre-o para edição; "Novo quarto" limpa o formulário para criar outro.
 * Só lê os campos e mostra mensagens; as regras (número válido, preço, permissões) estão no
 * {@link GestaoQuartosController}.
 * <p>
 * S6-05: os equipamentos do quarto escolhem-se a partir dos produtos que existem em stock, com a
 * quantidade; ficam guardados quando se carrega em "Guardar".
 * <p>
 * S6-09: o preço base e os equipamentos do quarto vêm, por omissão, da configuração do seu tipo. Quem marcar
 * "próprio" dá ao quarto uma exceção; desmarcar volta a herdar do tipo.
 */
public class GestaoQuartosViewController {

    private static final String COR_ERRO = "-fx-text-fill: #b03a2e;";
    private static final String COR_SUCESSO = "-fx-text-fill: #1e8449;";

    private final GestaoQuartosController controller;
    private final List<CheckBox> caixasEquipamentos = new ArrayList<>();
    /** Produtos do stock escolhidos para o quarto aberto (S6-05). */
    private final ObservableList<ProdutoQuarto> produtosEscolhidos = FXCollections.observableArrayList();
    /** Quarto aberto para edição; {@code null} quando se está a criar um quarto novo. */
    private Quarto quartoSelecionado;
    /** Enquanto se preenche o formulário com um quarto, as caixas de exceção não mexem nos campos. */
    private boolean aCarregar;

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
    private ComboBox<Artigo> campoProdutoStock;
    @FXML
    private TextField campoQuantidadeProduto;
    @FXML
    private ListView<ProdutoQuarto> listaProdutosQuarto;
    @FXML
    private CheckBox campoPrecoPersonalizado;
    @FXML
    private CheckBox campoEquipPersonalizados;
    @FXML
    private Label labelHeranca;
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
        campoTipo.valueProperty().addListener((observavel, antigo, novo) -> atualizarHeranca(true));
        campoPrecoPersonalizado.selectedProperty().addListener((observavel, antigo, novo) -> atualizarHeranca(true));
        campoEquipPersonalizados.selectedProperty().addListener((observavel, antigo, novo) -> atualizarHeranca(true));
        campoVista.setOnAction(evento -> atualizarPrecoNoite());
        configurarProdutosStock();
        try {
            atualizarLista();
            atualizarEquipamentos(new HashSet<>());
            campoProdutoStock.setItems(FXCollections.observableArrayList(controller.consultarProdutosStock()));
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
        // Sem preço próprio, o quarto herda o preço base do tipo.
        BigDecimal preco = null;
        if (campoPrecoPersonalizado.isSelected()) {
            Optional<BigDecimal> lido = Utils.converterDecimal(campoPreco.getText());
            if (lido.isEmpty()) {
                mostrar("Preço base inválido. Use um número, por exemplo 60,00.", COR_ERRO);
                return;
            }
            preco = lido.get();
        }
        try {
            if (quartoSelecionado == null) {
                criar(preco);
            } else {
                editar(preco);
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

    /**
     * Chamado pelo botão "Pôr no quarto": junta o produto do stock à lista do quarto (se já lá estiver,
     * fica com a quantidade nova). Só fica guardado ao carregar em "Guardar".
     */
    @FXML
    private void onAdicionarProduto() {
        if (!campoEquipPersonalizados.isSelected()) {
            mostrar("Os equipamentos vêm do tipo de quarto. Marque 'Equipamentos próprios' para os alterar.", COR_ERRO);
            return;
        }
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
        mostrar("'" + artigo.getNome() + "' x" + quantidade.get()
                + " no quarto. Carregue em Guardar para gravar.", COR_SUCESSO);
    }

    /** Chamado pelo botão "Tirar do quarto": retira o produto selecionado na lista. */
    @FXML
    private void onRemoverProduto() {
        if (!campoEquipPersonalizados.isSelected()) {
            mostrar("Os equipamentos vêm do tipo de quarto. Marque 'Equipamentos próprios' para os alterar.", COR_ERRO);
            return;
        }
        ProdutoQuarto produto = listaProdutosQuarto.getSelectionModel().getSelectedItem();
        if (produto == null) {
            mostrar("Selecione um produto na lista do quarto.", COR_ERRO);
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

    private void criar(BigDecimal precoPersonalizado) {
        Optional<Integer> numero = Utils.converterInteiro(campoNumero.getText());
        if (numero.isEmpty()) {
            mostrar("Número de quarto inválido. Escreva só o número, por exemplo 205.", COR_ERRO);
            return;
        }
        Quarto criado = controller.criarQuartoDoTipo(numero.get(), campoLocal.getText(), campoTipo.getValue(),
                campoVista.getValue(), precoPersonalizado, equipamentosProprios(), new ArrayList<>(produtosEscolhidos));
        atualizarLista();
        selecionarPorId(criado.getId());
        mostrar("Quarto " + criado.getNumero() + " criado.", COR_SUCESSO);
    }

    private void editar(BigDecimal precoPersonalizado) {
        controller.editarQuartoDoTipo(quartoSelecionado.getId(), campoLocal.getText(), campoTipo.getValue(),
                campoVista.getValue(), campoAtivo.isSelected(), precoPersonalizado, equipamentosProprios(),
                new ArrayList<>(produtosEscolhidos));
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
        aCarregar = true;
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
        produtosEscolhidos.clear();
        // Um quarto novo herda do tipo, a não ser que se marquem exceções.
        campoPrecoPersonalizado.setSelected(false);
        campoEquipPersonalizados.setSelected(false);
        aCarregar = false;
        labelModo.setText("Novo quarto");
        atualizarHeranca(true);
        atualizarPrecoNoite();
    }

    private void carregarNoFormulario(Quarto quarto) {
        quartoSelecionado = quarto;
        aCarregar = true;
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
        produtosEscolhidos.setAll(quarto.getProdutos());
        campoPrecoPersonalizado.setSelected(quarto.isPrecoPersonalizado());
        campoEquipPersonalizados.setSelected(quarto.isEquipamentosPersonalizados());
        aCarregar = false;
        labelModo.setText("A editar o quarto " + quarto.getNumero() + " (piso " + quarto.getPiso() + ")");
        atualizarHeranca(false);
        atualizarPrecoNoite();
    }

    /**
     * Mostra o que o quarto herda do tipo: o que não é exceção fica bloqueado e, se {@code sobrescrever},
     * com os valores do tipo. O que é exceção pode ser editado.
     */
    private void atualizarHeranca(boolean sobrescrever) {
        if (aCarregar) {
            return;
        }
        boolean precoProprio = campoPrecoPersonalizado.isSelected();
        boolean equipamentosProprios = campoEquipPersonalizados.isSelected();
        campoPreco.setDisable(!precoProprio);
        aplicarEstadoEquipamentos(!equipamentosProprios);
        TipoQuarto tipo = campoTipo.getValue();
        if (tipo == null) {
            labelHeranca.setText("Escolha o tipo: o preço base e os equipamentos vêm da configuração do tipo.");
            return;
        }
        Optional<ConfiguracaoTipoQuarto> configuracao = configuracaoDoTipo(tipo);
        if (configuracao.isEmpty()) {
            labelHeranca.setText("O tipo " + tipo.getNome() + " ainda não tem configuração. Defina-a em "
                    + "'Configuração por tipo de quarto' ou marque o preço e os equipamentos como próprios.");
            return;
        }
        labelHeranca.setText("Tipo " + tipo.getNome() + ": preço base " + Utils.formatarPreco(configuracao.get().getPrecoBase())
                + ", " + configuracao.get().getEquipamentos().size() + " equip. e "
                + configuracao.get().getProdutos().size() + " prod. stock. O que não for próprio vem do tipo.");
        if (!sobrescrever) {
            return;
        }
        if (!precoProprio) {
            campoPreco.setText(configuracao.get().getPrecoBase().toPlainString().replace('.', ','));
        }
        if (!equipamentosProprios) {
            Set<Integer> ids = new HashSet<>();
            configuracao.get().getEquipamentos().forEach(equipamento -> ids.add(equipamento.getId()));
            marcarEquipamentos(ids);
            produtosEscolhidos.setAll(configuracao.get().getProdutos());
        }
    }

    private Optional<ConfiguracaoTipoQuarto> configuracaoDoTipo(TipoQuarto tipo) {
        try {
            return controller.consultarConfiguracaoTipo(tipo);
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    /** Bloqueia ou liberta os campos dos equipamentos (bloqueados enquanto o quarto os herda do tipo). */
    private void aplicarEstadoEquipamentos(boolean bloqueados) {
        caixasEquipamentos.forEach(caixa -> caixa.setDisable(bloqueados));
        campoProdutoStock.setDisable(bloqueados);
        campoQuantidadeProduto.setDisable(bloqueados);
        listaProdutosQuarto.setDisable(bloqueados);
    }

    /** @return os equipamentos próprios do quarto, ou {@code null} se o quarto herda os do tipo */
    private List<Equipamento> equipamentosProprios() {
        return campoEquipPersonalizados.isSelected() ? equipamentosMarcados() : null;
    }

    private void configurarProdutosStock() {
        listaProdutosQuarto.setItems(produtosEscolhidos);
        listaProdutosQuarto.setCellFactory(lista -> new ListCell<ProdutoQuarto>() {
            @Override
            protected void updateItem(ProdutoQuarto produto, boolean vazio) {
                super.updateItem(produto, vazio);
                setText(vazio || produto == null ? null : produto.getArtigo().getNome() + "  x"
                        + produto.getQuantidade() + " " + produto.getArtigo().getUnidade()
                        + (produto.getArtigo().isConsumivel() ? "  (consumível)" : ""));
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
            caixa.setDisable(!campoEquipPersonalizados.isSelected());
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
        try {
            labelPrecoNoite.setText(Utils.formatarPreco(controller.calcularPrecoNoite(preco.get(), vista))
                    + " (preço base + " + Utils.formatarPreco(controller.obterSuplemento(vista)) + " da vista)");
        } catch (RuntimeException e) {
            labelPrecoNoite.setText("-");
        }
    }

    private void mostrar(String texto, String estilo) {
        labelMensagem.setText(texto);
        labelMensagem.setStyle(estilo);
    }

    private static String descrever(Quarto quarto) {
        return "Quarto " + quarto.getNumero() + "  |  " + quarto.getTipo().getNome() + "  |  piso " + quarto.getPiso()
                + "\n" + quarto.getVista().getNome() + "  |  " + Utils.formatarPreco(quarto.precoNoite()) + "/noite  |  "
                + (quarto.isAtivo() ? "Ativo" : "Inativo") + "  |  " + quarto.getEquipamentos().size() + " equip.  |  "
                + quarto.getProdutos().size() + " prod. stock"
                + (quarto.isPrecoPersonalizado() || quarto.isEquipamentosPersonalizados() ? "  |  com exceções" : "");
    }
}
