package com.hotel4you.hotel4you.Model;

import static com.hotel4you.hotel4you.Model.enums.Perfil.EMPRESA_RESTAURACAO;
import static com.hotel4you.hotel4you.Model.enums.Perfil.FORNECEDOR;
import static com.hotel4you.hotel4you.Model.enums.Perfil.GESTOR;
import static com.hotel4you.hotel4you.Model.enums.Perfil.RECECIONISTA;

import com.hotel4you.hotel4you.Model.enums.Perfil;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * Opções do menu principal e os perfis que têm acesso a cada uma.
 * <p>
 * A ordem de declaração é a ordem em que as opções aparecem no menu.
 * Como o gestor tem as permissões do rececionista (assunção A1), as opções
 * do rececionista incluem também o {@code GESTOR}.
 */
public enum OpcaoMenu {

    // Rececionista (e gestor)
    REGISTAR_CLIENTE("Registar e consultar clientes", RECECIONISTA, GESTOR),
    CONSULTAR_DISPONIBILIDADE("Consultar disponibilidade de quartos", RECECIONISTA, GESTOR),
    CRIAR_RESERVA("Criar reserva", RECECIONISTA, GESTOR),
    CANCELAR_RESERVA("Cancelar reserva", RECECIONISTA, GESTOR),
    CHECK_IN("Check-in", RECECIONISTA, GESTOR),
    CHECK_OUT("Check-out", RECECIONISTA, GESTOR),
    SERVICOS_QUARTO("Serviços de quarto e extras", RECECIONISTA, GESTOR),
    GERIR_STOCK("Gerir stock", RECECIONISTA, GESTOR),

    // Só gestor
    GERIR_FUNCIONARIOS("Gerir funcionários", GESTOR),
    SUGESTOES_QUEIXAS("Sugestões e queixas", GESTOR),
    APAGAR_RESERVAS("Apagar reservas", GESTOR),
    LISTAGEM_POR_CARTAO("Locais por onde passou um cartão", GESTOR),
    LISTAGEM_POR_QUARTO("Passagens de cartões num quarto", GESTOR),
    CONFIGURAR_QUARTOS("Configurar quartos", GESTOR),
    CONFIGURAR_PRECOS("Configurar preços de extras e serviços", GESTOR),

    // Empresa de restauração
    COZINHA_RESTAURANTE("Cozinha e restaurante", EMPRESA_RESTAURACAO),

    // Fornecedor
    ENVIAR_ENTREGA_XML("Enviar entrega (XML)", FORNECEDOR);

    private final String texto;
    private final Set<Perfil> perfis;

    OpcaoMenu(String texto, Perfil... perfis) {
        this.texto = texto;
        this.perfis = EnumSet.copyOf(Arrays.asList(perfis));
    }

    /**
     * @return o texto da opção, pronto para mostrar no menu
     */
    public String getTexto() {
        return texto;
    }

    /**
     * Indica se um perfil pode ver e usar esta opção.
     *
     * @param perfil perfil a verificar
     * @return {@code true} se o perfil tem acesso
     */
    public boolean permite(Perfil perfil) {
        return perfis.contains(perfil);
    }
}
