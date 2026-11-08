package com.hotel4you.hotel4you.View;

import com.hotel4you.hotel4you.Controller.GestaoClientesController;
import com.hotel4you.hotel4you.Model.Cliente;
import com.hotel4you.hotel4you.Model.Pagina;
import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.util.function.Consumer;

/**
 * Liga uma lista de clientes aos botões "Anterior" e "Seguinte" e ao texto "Página x de y", pedindo ao
 * {@link GestaoClientesController} só uma página de cada vez (S4-07). É usado nos ecrãs de clientes e de
 * reservas para a lista aparecer depressa mesmo com muitos clientes registados.
 * Não tem regras de negócio: só mostra a página que o controller devolve.
 */
final class PaginadorClientes {

    private final GestaoClientesController controller;
    private final ListView<Cliente> lista;
    private final Button botaoAnterior;
    private final Label etiquetaPagina;
    private final Button botaoSeguinte;
    private final int porPagina;
    private final Consumer<String> aoFalhar;

    private String pesquisa = "";
    private int paginaAtual = 0;

    /**
     * @param controller     controller que devolve as páginas de clientes
     * @param lista          lista onde a página é mostrada
     * @param botaoAnterior  botão da página anterior
     * @param etiquetaPagina texto "Página x de y"
     * @param botaoSeguinte  botão da página seguinte
     * @param porPagina      clientes por página
     * @param aoFalhar       recebe a mensagem de erro se não for possível consultar os clientes
     */
    PaginadorClientes(GestaoClientesController controller, ListView<Cliente> lista, Button botaoAnterior,
                      Label etiquetaPagina, Button botaoSeguinte, int porPagina, Consumer<String> aoFalhar) {
        this.controller = controller;
        this.lista = lista;
        this.botaoAnterior = botaoAnterior;
        this.etiquetaPagina = etiquetaPagina;
        this.botaoSeguinte = botaoSeguinte;
        this.porPagina = porPagina;
        this.aoFalhar = aoFalhar;
    }

    /**
     * Mostra a primeira página dos clientes que correspondem à pesquisa.
     *
     * @param texto parte do nome ou do NIF/documento; vazio mostra todos
     */
    void pesquisar(String texto) {
        pesquisa = texto == null ? "" : texto;
        paginaAtual = 0;
        mostrar();
    }

    /** Volta a ler a página atual (por exemplo, depois de registar um cliente). */
    void recarregar() {
        mostrar();
    }

    /** Vai para a página anterior, se existir. */
    void anterior() {
        paginaAtual = Math.max(0, paginaAtual - 1);
        mostrar();
    }

    /** Vai para a página seguinte, se existir. */
    void seguinte() {
        paginaAtual++;
        mostrar();
    }

    private void mostrar() {
        try {
            Pagina<Cliente> pagina = controller.consultarPagina(pesquisa, paginaAtual, porPagina);
            paginaAtual = pagina.numero();
            lista.setItems(FXCollections.observableArrayList(pagina.itens()));
            etiquetaPagina.setText("Página " + (pagina.numero() + 1) + " de " + pagina.totalPaginas()
                    + "  (" + pagina.total() + " cliente(s))");
            botaoAnterior.setDisable(!pagina.temAnterior());
            botaoSeguinte.setDisable(!pagina.temSeguinte());
        } catch (RuntimeException e) {
            lista.setItems(FXCollections.observableArrayList());
            botaoAnterior.setDisable(true);
            botaoSeguinte.setDisable(true);
            aoFalhar.accept(e.getMessage());
        }
    }
}
