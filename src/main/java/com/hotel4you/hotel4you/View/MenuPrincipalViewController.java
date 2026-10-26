package com.hotel4you.hotel4you.View;

import com.hotel4you.hotel4you.Controller.Sessao;
import com.hotel4you.hotel4you.Utils.Navegador;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * ViewController do menu principal ({@code MenuPrincipalView.fxml}).
 * <p>
 * <b>Provisório:</b> por agora só mostra o utilizador e o perfil e permite terminar sessão.
 * As opções por perfil são da S2-07. Não tem Controller próprio porque ainda não trata dados.
 */
public class MenuPrincipalViewController {

    private final Sessao sessao;

    @FXML
    private Label labelBoasVindas;

    /**
     * @param sessao sessão com o utilizador autenticado
     */
    public MenuPrincipalViewController(Sessao sessao) {
        this.sessao = sessao;
    }

    /** Chamado pelo JavaFX depois de os campos {@code @FXML} serem preenchidos. */
    @FXML
    private void initialize() {
        sessao.getUtilizadorAtual().ifPresent(u ->
                labelBoasVindas.setText("Bem-vindo, " + u.getNome() + " (" + u.getPerfil().getNome() + ")"));
    }

    /** Chamado pelo botão "Terminar sessão". */
    @FXML
    private void onTerminarSessao() {
        sessao.terminar();
        Navegador.mudarPara("LoginView.fxml", "Hotel4You - Login");
    }
}
