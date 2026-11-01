package com.hotel4you.hotel4you.View;

import com.hotel4you.hotel4you.Controller.AcessoDadosException;
import com.hotel4you.hotel4you.Controller.GestaoFuncionariosController;
import com.hotel4you.hotel4you.Model.Utilizador;
import com.hotel4you.hotel4you.Model.enums.Perfil;
import com.hotel4you.hotel4you.Utils.Navegador;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.List;

/**
 * Controla o ecrã de gestão de funcionários.
 */
public class GestaoFuncionariosViewController {

    private final GestaoFuncionariosController controller;

    @FXML
    private TextField campoNome;

    @FXML
    private TextField campoUsername;

    @FXML
    private PasswordField campoPassword;

    @FXML
    private ComboBox<Perfil> campoPerfil;

    @FXML
    private ListView<Utilizador> listaUtilizadores;

    @FXML
    private Label labelMensagem;

    /**
     * @param controller controller que trata das regras de gestão de funcionários
     */
    public GestaoFuncionariosViewController(
            GestaoFuncionariosController controller) {

        this.controller = controller;
    }

    /**
     * Inicializa os perfis e a lista de utilizadores.
     */
    @FXML
    private void initialize() {

        campoPerfil.setItems(
                FXCollections.observableArrayList(Perfil.values())
        );

        // Apresenta o nome do perfil em vez do nome técnico do enum.
        campoPerfil.setCellFactory(lista -> new ListCell<Perfil>() {
            @Override
            protected void updateItem(Perfil perfil, boolean vazio) {
                super.updateItem(perfil, vazio);

                if (vazio || perfil == null) {
                    setText(null);
                } else {
                    setText(perfil.getNome());
                }
            }
        });

        campoPerfil.setButtonCell(new ListCell<Perfil>() {
            @Override
            protected void updateItem(Perfil perfil, boolean vazio) {
                super.updateItem(perfil, vazio);

                if (vazio || perfil == null) {
                    setText(null);
                } else {
                    setText(perfil.getNome());
                }
            }
        });

        atualizarLista();
    }

    /**
     * Cria um funcionário quando o gestor carrega no botão.
     */
    @FXML
    private void onCriar() {

        try {
            controller.criarFuncionario(
                    campoNome.getText(),
                    campoUsername.getText(),
                    campoPassword.getText(),
                    campoPerfil.getValue()
            );

            labelMensagem.setText(
                    "Funcionário criado com sucesso."
            );

            limparCampos();
            atualizarLista();

        } catch (IllegalArgumentException | SecurityException e) {

            labelMensagem.setText(e.getMessage());

        } catch (RuntimeException e) {

            labelMensagem.setText(
                    "Não foi possível criar o funcionário: "
                            + e.getMessage()
            );
        }
    }

    /**
     * Desativa o funcionário selecionado na lista.
     */
    @FXML
    private void onDesativar() {

        Utilizador selecionado = listaUtilizadores
                .getSelectionModel()
                .getSelectedItem();

        if (selecionado == null) {
            labelMensagem.setText(
                    "Selecione um utilizador da lista."
            );
            return;
        }

        try {
            controller.desativarFuncionario(
                    selecionado.getId()
            );

            labelMensagem.setText(
                    "Utilizador desativado. Já não pode iniciar sessão."
            );

            atualizarLista();

        } catch (IllegalArgumentException | SecurityException e) {

            labelMensagem.setText(e.getMessage());

        } catch (RuntimeException e) {

            labelMensagem.setText(
                    "Não foi possível desativar o utilizador: "
                            + e.getMessage()
            );
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

    /**
     * Atualiza a lista de utilizadores.
     */
    private void atualizarLista() {

        List<Utilizador> utilizadores;

        try {
            utilizadores = controller.listarFuncionarios();
        } catch (AcessoDadosException e) {
            labelMensagem.setText(
                    "Não foi possível consultar os utilizadores: "
                            + e.getMessage()
            );
            utilizadores = List.of();
        }

        listaUtilizadores.setItems(
                FXCollections.observableArrayList(utilizadores)
        );

        listaUtilizadores.setCellFactory(lista ->
                new ListCell<Utilizador>() {

                    @Override
                    protected void updateItem(
                            Utilizador utilizador,
                            boolean vazio) {

                        super.updateItem(utilizador, vazio);

                        if (vazio || utilizador == null) {
                            setText(null);
                        } else {

                            String estado = utilizador.isAtivo()
                                    ? "Ativo"
                                    : "Desativado";

                            setText(
                                    utilizador.getId()
                                            + " - "
                                            + utilizador.getNome()
                                            + " | "
                                            + utilizador.getUsername()
                                            + " | "
                                            + utilizador.getPerfil().getNome()
                                            + " | "
                                            + estado
                            );
                        }
                    }
                }
        );
    }

    /**
     * Limpa os campos depois de criar um funcionário.
     */
    private void limparCampos() {

        campoNome.clear();
        campoUsername.clear();
        campoPassword.clear();

        campoPerfil.getSelectionModel().clearSelection();
    }
}