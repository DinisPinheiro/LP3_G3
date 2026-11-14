package com.hotel4you.hotel4you.Utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
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
    private static final double LARGURA = 1000;
    private static final double ALTURA = 600;

    private static final String ECRA_LOGIN = "LoginView.fxml";
    private static final String ECRA_MENU = "MenuPrincipalView.fxml";

    private static Stage janela;
    private static Pane areaConteudo;
    private static Runnable aoVoltarAoInicio;
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
     * Regista a área de conteúdo do layout principal (menu fixo). Enquanto estiver registada,
     * os ecrãs de funcionalidades abrem nessa área em vez de substituírem a janela inteira,
     * e pedir o menu principal apenas volta ao ecrã inicial do layout.
     *
     * @param area            área onde os ecrãs são mostrados
     * @param voltarAoInicio  chamado quando se pede o menu principal com o layout já aberto
     */
    public static void registarLayout(Pane area, Runnable voltarAoInicio) {
        areaConteudo = Objects.requireNonNull(area);
        aoVoltarAoInicio = Objects.requireNonNull(voltarAoInicio);
    }

    /**
     * Mostra um ecrã (ficheiro FXML).
     * <p>
     * O login e o layout principal ocupam a janela; todos os outros ecrãs abrem na área de
     * conteúdo do layout, se este estiver ativo.
     *
     * @param ficheiroFxml nome do ficheiro em {@code resources/com/hotel4you/hotel4you/} (ex.: "LoginView.fxml")
     * @param titulo       título da janela
     * @throws IllegalStateException se o navegador não foi iniciado ou o FXML não puder ser carregado
     */
    public static void mudarPara(String ficheiroFxml, String titulo) {
        if (janela == null) {
            throw new IllegalStateException("O Navegador não foi iniciado.");
        }
        if (ECRA_LOGIN.equals(ficheiroFxml)) {
            areaConteudo = null;
            aoVoltarAoInicio = null;
        } else if (areaConteudo != null) {
            if (ECRA_MENU.equals(ficheiroFxml)) {
                aoVoltarAoInicio.run();
            } else {
                areaConteudo.getChildren().setAll(carregar(ficheiroFxml));
            }
            return;
        }
        Parent raiz = carregar(ficheiroFxml);
        if (janela.getScene() == null) {
            janela.setScene(new Scene(raiz, LARGURA, ALTURA));
        } else {
            janela.getScene().setRoot(raiz);
        }
        janela.setTitle(titulo);
    }

    private static Parent carregar(String ficheiroFxml) {
        URL url = Navegador.class.getResource(PASTA_FXML + ficheiroFxml);
        if (url == null) {
            throw new IllegalStateException("Ecrã não encontrado: " + ficheiroFxml);
        }
        try {
            FXMLLoader loader = new FXMLLoader(url);
            loader.setControllerFactory(fabrica);
            return loader.load();
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao carregar o ecrã " + ficheiroFxml, e);
        }
    }
}
