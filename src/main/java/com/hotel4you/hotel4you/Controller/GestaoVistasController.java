package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.SuplementoVistaDAO;
import com.hotel4you.hotel4you.Model.enums.Perfil;
import com.hotel4you.hotel4you.Model.enums.Vista;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Edição dos valores das vistas especiais (mar e montanha) dos quartos.
 * <p>
 * Só o gestor acede. O valor guardado aqui é somado ao preço base por noite de todos os quartos com essa
 * vista ({@link com.hotel4you.hotel4you.Model.Quarto#precoNoite()}), por isso reflete-se logo na
 * disponibilidade de quartos e no total das reservas novas, sem alterar a base de dados à mão.
 * As reservas já criadas não mudam: guardam o valor da vista que estava em vigor quando foram feitas
 * (S5-09, {@link com.hotel4you.hotel4you.Model.Reserva#getValorVistaNoite()}).
 * <p>
 */
public class GestaoVistasController {

    /** Valor máximo de DECIMAL(10,2). */
    private static final BigDecimal VALOR_MAXIMO = new BigDecimal("99999999.99");

    private final SuplementoVistaDAO suplementoVistaDAO;
    private final Sessao sessao;

    /**
     * @param suplementoVistaDAO onde os valores das vistas são guardados
     * @param sessao             sessão com o utilizador autenticado
     */
    public GestaoVistasController(SuplementoVistaDAO suplementoVistaDAO, Sessao sessao) {
        this.suplementoVistaDAO = Objects.requireNonNull(suplementoVistaDAO);
        this.sessao = Objects.requireNonNull(sessao);
    }

    /**
     * Lista as vistas especiais com o valor em vigor.
     *
     * @return mar e montanha, com o valor a somar ao preço base por noite
     * @throws SecurityException se o utilizador não for gestor
     * @throws com.hotel4you.hotel4you.DAO.DAOException se não for possível ler
     */
    public Map<Vista, BigDecimal> consultarValores() {
        exigirGestor();
        Map<Vista, BigDecimal> especiais = new LinkedHashMap<>();
        suplementoVistaDAO.listar().forEach((vista, valor) -> {
            if (vista.isEspecial()) {
                especiais.put(vista, valor);
            }
        });
        return especiais;
    }

    /**
     * Altera o valor de uma vista especial.
     *
     * @param vista vista especial (mar ou montanha)
     * @param valor novo valor a somar ao preço base por noite, não negativo
     * @throws IllegalArgumentException se a vista não for especial ou o valor for inválido
     * @throws SecurityException        se o utilizador não for gestor
     * @throws com.hotel4you.hotel4you.DAO.DAOException se não for possível guardar
     */
    public void editarValor(Vista vista, BigDecimal valor) {
        exigirGestor();
        if (vista == null) {
            throw new IllegalArgumentException("Escolha a vista.");
        }
        if (!vista.isEspecial()) {
            throw new IllegalArgumentException("Só as vistas especiais (mar e montanha) têm valor editável.");
        }
        if (valor == null) {
            throw new IllegalArgumentException("Introduza o valor.");
        }
        if (valor.signum() < 0) {
            throw new IllegalArgumentException("O valor não pode ser negativo.");
        }
        BigDecimal arredondado = valor.setScale(2, RoundingMode.HALF_UP);
        if (arredondado.compareTo(VALOR_MAXIMO) > 0) {
            throw new IllegalArgumentException("O valor não pode ser superior a "
                    + VALOR_MAXIMO.toPlainString().replace('.', ',') + ".");
        }
        suplementoVistaDAO.atualizar(vista, arredondado);
    }

    private void exigirGestor() {
        Permissoes.exigirPerfil(sessao, "Apenas o gestor pode editar os valores das vistas.", Perfil.GESTOR);
    }
}
