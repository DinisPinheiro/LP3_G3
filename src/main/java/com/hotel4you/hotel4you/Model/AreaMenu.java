package com.hotel4you.hotel4you.Model;

import static com.hotel4you.hotel4you.Model.OpcaoMenu.*;

import com.hotel4you.hotel4you.Model.enums.Perfil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Áreas temáticas do menu lateral (S5-01).
 * <p>
 * Cada área agrupa várias {@link OpcaoMenu}. Uma área só aparece a um perfil se ele tiver
 * acesso a pelo menos uma das suas opções; dentro da área só se mostram as opções permitidas.
 * A ordem de declaração é a ordem no menu.
 */
public enum AreaMenu {

    CLIENTES("Registar e consultar clientes", "Clientes", REGISTAR_CLIENTE),
    DISPONIBILIDADE("Consultar disponibilidade de quarto", "Disponibilidade de quartos", CONSULTAR_DISPONIBILIDADE),
    RESERVAS("Reservas", "Reservas", CRIAR_RESERVA, CANCELAR_RESERVA, APAGAR_RESERVAS, CHECK_IN, CHECK_OUT),
    SERVICOS("Serviços e extras", "Serviços e extras", SERVICOS_E_EXTRAS, SERVICOS_QUARTO),
    STOCK("Stock", "Stock", GERIR_STOCK),
    FORNECEDORES("Fornecedores", "Fornecedores", ENVIAR_ENTREGA_XML),
    COZINHA("Cozinha e restaurante", "Cozinha e restaurante", COZINHA_RESTAURANTE),
    FUNCIONARIOS("Gerir funcionários", "Funcionários", GERIR_FUNCIONARIOS),
    SUGESTOES("Sugestões e queixas", "Sugestões e queixas", SUGESTOES_QUEIXAS),
    CARTOES("Movimento de cartão", "Movimento de cartão", LISTAGEM_POR_CARTAO, LISTAGEM_POR_QUARTO),
    QUARTOS("Configurar quartos", "Configurar quartos", CONFIGURAR_QUARTOS, EDITAR_VISTAS);

    private final String texto;
    private final String titulo;
    private final List<OpcaoMenu> opcoes;

    AreaMenu(String texto, String titulo, OpcaoMenu... opcoes) {
        this.texto = texto;
        this.titulo = titulo;
        this.opcoes = List.of(opcoes);
    }

    /** @return texto do botão no menu lateral */
    public String getTexto() {
        return texto;
    }

    /** @return título mostrado no topo da área de conteúdo */
    public String getTitulo() {
        return titulo;
    }

    /**
     * @param perfil perfil do utilizador
     * @return opções desta área a que o perfil tem acesso, pela ordem de declaração
     */
    public List<OpcaoMenu> opcoesPara(Perfil perfil) {
        List<OpcaoMenu> permitidas = new ArrayList<>();
        for (OpcaoMenu opcao : opcoes) {
            if (opcao.permite(perfil)) {
                permitidas.add(opcao);
            }
        }
        return permitidas;
    }

    /**
     * @param perfil perfil do utilizador
     * @return {@code true} se o perfil tem acesso a pelo menos uma opção da área
     */
    public boolean permite(Perfil perfil) {
        return !opcoesPara(perfil).isEmpty();
    }

    /** @return todas as opções da área, independentemente do perfil */
    public List<OpcaoMenu> getOpcoes() {
        return opcoes;
    }
}
