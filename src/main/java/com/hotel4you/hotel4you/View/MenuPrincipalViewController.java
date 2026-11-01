package com.hotel4you.hotel4you.View;

import com.hotel4you.hotel4you.Controller.MenuController;
import com.hotel4you.hotel4you.Controller.Sessao;
import com.hotel4you.hotel4you.Model.OpcaoMenu;
import com.hotel4you.hotel4you.Model.enums.Perfil;
import com.hotel4you.hotel4you.Utils.Navegador;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * ViewController do menu principal ({@code MenuPrincipalView.fxml}).
 * <p>
 * Mostra um botão por cada opção a que o perfil do utilizador autenticado tem acesso.
 * Quem decide as opções é o {@link MenuController}; esta classe só desenha os botões.
 * Opções ainda sem ecrã mostram "em desenvolvimento".
 */
public class MenuPrincipalViewController {

    private final Sessao sessao;
    private final MenuController menuController;

    @FXML
    private Label labelBoasVindas;
    @FXML
    private VBox caixaOpcoes;
    @FXML
    private Label labelMensagem;

    /**
     * @param sessao         sessão com o utilizador autenticado
     * @param menuController controller que decide as opções de cada perfil
     */
    public MenuPrincipalViewController(Sessao sessao, MenuController menuController) {
        this.sessao = sessao;
        this.menuController = menuController;
    }

    /** Chamado pelo JavaFX depois de os campos {@code @FXML} serem preenchidos. */
    @FXML
    private void initialize() {
        sessao.getUtilizadorAtual().ifPresent(utilizador -> {
            Perfil perfil = utilizador.getPerfil();
            labelBoasVindas.setText("Bem-vindo, " + utilizador.getNome() + " (" + perfil.getNome() + ")");
            for (OpcaoMenu opcao : menuController.obterOpcoes(perfil)) {
                Button botao = new Button(opcao.getTexto());
                botao.setMaxWidth(Double.MAX_VALUE);
                botao.setOnAction(evento -> onOpcao(perfil, opcao));
                caixaOpcoes.getChildren().add(botao);
            }
        });
    }

    /** Chamado pelo botão "Terminar sessão". */
    @FXML
    private void onTerminarSessao() {
        sessao.terminar();
        Navegador.mudarPara("LoginView.fxml", "Hotel4You - Login");
    }

    private void onOpcao(Perfil perfil, OpcaoMenu opcao) {
        if (!menuController.temAcesso(perfil, opcao)) {
            labelMensagem.setText("Não tem permissões para esta opção.");
            return;
        }
        if (opcao == OpcaoMenu.GERIR_FUNCIONARIOS) {
            Navegador.mudarPara("GestaoFuncionariosView.fxml", "Hotel4You - Gestão de funcionários");
            return;
        }
        // Cada story acrescenta aqui o seu ecrã, por exemplo:
        // if (opcao == OpcaoMenu.REGISTAR_CLIENTE) {
        //     Navegador.mudarPara("RegistarClienteView.fxml", "Registar cliente");
        //     return;
        // }
        labelMensagem.setText("\"" + opcao.getTexto() + "\" está em desenvolvimento.");
    }
}