package com.hotel4you.hotel4you.Utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

/**
 * Troca de ecrãs (ficheiros FXML) na janela principal.
 * <p>
 * Os ViewControllers são criados por uma fábrica fornecida pelo {@code Main},
 * o que permite injetar-lhes os Controllers sem que a View crie DAO ou outras dependências.
 */
public final class Navegador {

    private static final String PASTA_FXML = "/com/hotel4you/hotel4you/";
    private static final double LARGURA = 800;
    private static final double ALTURA = 500;

    private static Stage janela;
    private static Callback<Class<?>, Object> fabrica;

    private Navegador() {
    }

    /**
     * Configura o navegador. Deve ser chamado uma vez, no arranque.
     *
     * @param janelaPrincipal janela da aplicação
     * @param fabricaControladores recebe a classe da View e devolve a instância a usar
     */
    public static void iniciar(Stage janelaPrincipal, Callback<Class<?>, Object> fabricaControladores) {
        janela = Objects.requireNonNull(janelaPrincipal);
        fabrica = Objects.requireNonNull(fabricaControladores);
    }

    /**
     * Mostra um ecrã.
     *
     * @param ficheiroFxml nome do ficheiro em {@code resources/com/hotel4you/hotel4you/} (ex.: "LoginView.fxml")
     * @param titulo       título da janela
     * @throws IllegalStateException se o navegador não foi iniciado ou o FXML não puder ser carregado
     */
    public static void mudarPara(String ficheiroFxml, String titulo) {
        if (janela == null) {
            throw new IllegalStateException("O Navegador não foi iniciado.");
        }
        URL url = Navegador.class.getResource(PASTA_FXML + ficheiroFxml);
        if (url == null) {
            throw new IllegalStateException("Ecrã não encontrado: " + ficheiroFxml);
        }
        try {
            FXMLLoader loader = new FXMLLoader(url);
            loader.setControllerFactory(fabrica);
            Parent raiz = loader.load();
            if (janela.getScene() == null) {
                janela.setScene(new Scene(raiz, LARGURA, ALTURA));
            } else {
                janela.getScene().setRoot(raiz);
            }
            janela.setTitle(titulo);
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao carregar o ecrã " + ficheiroFxml, e);
        }
    }
}
