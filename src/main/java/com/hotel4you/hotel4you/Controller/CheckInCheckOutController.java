package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.CartaoDAO;
import com.hotel4you.hotel4you.DAO.ReservaDAO;
import com.hotel4you.hotel4you.Model.CartaoMagnetico;
import com.hotel4you.hotel4you.Model.Reserva;
import com.hotel4you.hotel4you.Model.enums.EstadoReserva;
import com.hotel4you.hotel4you.Model.enums.Perfil;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Check-in e check-out de reservas na receção.
 * <p>
 * <b>Check-in:</b> só em reservas {@code PENDENTE}, a partir do dia de entrada e antes do dia de
 * saída. Cria um cartão magnético novo, com um número que ainda não existe, entrega-o à reserva e
 * muda o estado para {@code CHECK_IN}.
 * <p>
 * <b>Check-out:</b> só em reservas com check-in. Regista a saída, desativa o cartão e muda o estado
 * para {@code CHECK_OUT}; como só as reservas pendentes ou com check-in ocupam o quarto, o quarto
 * fica livre (mesmo que o cliente saia antes da data prevista).
 * <p>
 * Só o rececionista e o gestor podem usar este controller.
 * <p>
 * Apoio de IA: Claude (Anthropic), revisto pela equipa.
 */
public class CheckInCheckOutController {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ReservaDAO reservaDAO;
    private final CartaoDAO cartaoDAO;
    private final Sessao sessao;

    /**
     * @param reservaDAO onde as reservas estão guardadas
     * @param cartaoDAO  onde os cartões magnéticos são guardados
     * @param sessao     sessão com o utilizador autenticado
     */
    public CheckInCheckOutController(ReservaDAO reservaDAO, CartaoDAO cartaoDAO, Sessao sessao) {
        this.reservaDAO = Objects.requireNonNull(reservaDAO);
        this.cartaoDAO = Objects.requireNonNull(cartaoDAO);
        this.sessao = Objects.requireNonNull(sessao);
    }

    /**
     * Faz o check-in de uma reserva e atribui-lhe um cartão magnético novo.
     *
     * @param idReserva identificador da reserva
     * @return o cartão atribuído (ativo, com número único)
     * @throws IllegalArgumentException se a reserva não existir, não estiver pendente, ainda não
     *                                  tiver chegado o dia de entrada ou o período já tiver terminado
     * @throws SecurityException        se o utilizador não for rececionista nem gestor
     * @throws com.hotel4you.hotel4you.DAO.DAOException se não for possível guardar
     */
    public CartaoMagnetico fazerCheckIn(int idReserva) {
        exigirRececao();
        Reserva reserva = procurarReserva(idReserva);
        validarEstadoParaCheckIn(reserva);
        validarDatasParaCheckIn(reserva);

        CartaoMagnetico cartao = cartaoDAO.inserir(new CartaoMagnetico(cartaoDAO.proximoNumero(), false));
        try {
            reserva.fazerCheckIn(cartao);
            reservaDAO.atualizar(reserva);
        } catch (RuntimeException e) {
            removerCartaoSemErro(cartao);   // não deixa um cartão "perdido" se a reserva não foi guardada
            throw e;
        }
        return cartao;
    }

    /**
     * Faz o check-out de uma reserva: regista a saída, desativa o cartão e liberta o quarto.
     *
     * @param idReserva identificador da reserva
     * @return a reserva já com o estado {@code CHECK_OUT}
     * @throws IllegalArgumentException se a reserva não existir ou não tiver check-in ativo
     * @throws SecurityException        se o utilizador não for rececionista nem gestor
     * @throws com.hotel4you.hotel4you.DAO.DAOException se não for possível guardar
     */
    public Reserva fazerCheckOut(int idReserva) {
        exigirRececao();
        Reserva reserva = procurarReserva(idReserva);
        validarEstadoParaCheckOut(reserva);

        CartaoMagnetico cartao = reserva.getCartao();
        reserva.fazerCheckOut();                // muda o estado, regista a saída e desativa o cartão
        if (cartao != null) {
            cartaoDAO.atualizar(cartao);        // primeiro o cartão: se falhar, a reserva continua em check-in
        }
        reservaDAO.atualizar(reserva);
        return reserva;
    }

    private static void validarEstadoParaCheckIn(Reserva reserva) {
        EstadoReserva estado = reserva.getEstado();
        if (estado == EstadoReserva.CHECK_IN) {
            throw new IllegalArgumentException("A reserva n.º " + reserva.getId() + " já tem check-in efetuado.");
        }
        if (estado == EstadoReserva.CHECK_OUT) {
            throw new IllegalArgumentException("A reserva n.º " + reserva.getId()
                    + " já terminou (check-out efetuado).");
        }
        if (estado == EstadoReserva.CANCELADA) {
            throw new IllegalArgumentException("A reserva n.º " + reserva.getId()
                    + " está cancelada e não permite check-in.");
        }
    }

    private static void validarDatasParaCheckIn(Reserva reserva) {
        LocalDate hoje = LocalDate.now();
        if (hoje.isBefore(reserva.getDataEntrada())) {
            throw new IllegalArgumentException("O check-in da reserva n.º " + reserva.getId()
                    + " só é possível a partir de " + reserva.getDataEntrada().format(FORMATO_DATA) + ".");
        }
        if (!hoje.isBefore(reserva.getDataSaida())) {
            throw new IllegalArgumentException("A reserva n.º " + reserva.getId() + " terminou em "
                    + reserva.getDataSaida().format(FORMATO_DATA) + " e já não permite check-in.");
        }
    }

    private static void validarEstadoParaCheckOut(Reserva reserva) {
        EstadoReserva estado = reserva.getEstado();
        if (estado == EstadoReserva.PENDENTE) {
            throw new IllegalArgumentException("A reserva n.º " + reserva.getId()
                    + " ainda não tem check-in, por isso não pode ter check-out.");
        }
        if (estado == EstadoReserva.CHECK_OUT) {
            throw new IllegalArgumentException("A reserva n.º " + reserva.getId() + " já tem check-out efetuado.");
        }
        if (estado == EstadoReserva.CANCELADA) {
            throw new IllegalArgumentException("A reserva n.º " + reserva.getId() + " está cancelada.");
        }
    }

    private Reserva procurarReserva(int idReserva) {
        return reservaDAO.procurarPorId(idReserva)
                .orElseThrow(() -> new IllegalArgumentException("A reserva n.º " + idReserva + " não existe."));
    }

    private void removerCartaoSemErro(CartaoMagnetico cartao) {
        try {
            cartaoDAO.remover(cartao.getId());
        } catch (RuntimeException ignorado) {
            // O erro importante para o utilizador é o original; aqui só se tentou limpar.
        }
    }

    private void exigirRececao() {
        Permissoes.exigirPerfil(sessao, "Apenas o rececionista ou o gestor podem fazer check-in e check-out.",
                Perfil.RECECIONISTA, Perfil.GESTOR);
    }
}