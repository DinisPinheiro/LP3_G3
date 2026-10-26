package com.hotel4you.hotel4you.View;

import com.hotel4you.hotel4you.Controller.LoginController;
import com.hotel4you.hotel4you.Controller.AcessoDadosException;
import com.hotel4you.hotel4you.Utils.Navegador;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * ViewController do ecrã de login ({@code LoginView.fxml}). Só trata de ler os campos e mostrar mensagens;
 * a validação das credenciais é feita pelo {@link LoginController}.
 */
public class LoginViewController {

    private final LoginController loginController;

    @FXML
    private TextField campoUsername;
    @FXML
    private PasswordField campoPassword;
    @FXML
    private Label labelErro;

    /**
     * @param loginController controller que autentica os utilizadores
     */
    public LoginViewController(LoginController loginController) {
        this.loginController = loginController;
    }

    /** Chamado pelo botão "Entrar" (ou Enter na palavra-chave). */
    @FXML
    private void onEntrar() {
        try {
            if (loginController.autenticar(campoUsername.getText(), campoPassword.getText())) {
                Navegador.mudarPara("MenuPrincipalView.fxml", "Hotel4You");
            } else {
                labelErro.setText("Utilizador ou palavra-chave inválidos. Tente novamente.");
                campoPassword.clear();
                campoPassword.requestFocus();
            }
        } catch (AcessoDadosException e) {
            labelErro.setText("Não foi possível aceder aos dados: " + e.getMessage());
        }
    }

    /** Chamado pelo botão "Sair". */
    @FXML
    private void onSair() {
        Platform.exit();
    }
}
