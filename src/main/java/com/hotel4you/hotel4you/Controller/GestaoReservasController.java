package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.ClienteDAO;
import com.hotel4you.hotel4you.DAO.QuartoDAO;
import com.hotel4you.hotel4you.DAO.ReservaDAO;
import com.hotel4you.hotel4you.Model.Cliente;
import com.hotel4you.hotel4you.Model.Quarto;
import com.hotel4you.hotel4you.Model.Reserva;
import com.hotel4you.hotel4you.Model.enums.EstadoReserva;
import com.hotel4you.hotel4you.Model.enums.Perfil;
import com.hotel4you.hotel4you.Model.enums.TipoQuarto;
import com.hotel4you.hotel4you.Utils.Utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Criação, consulta, cancelamento e eliminação de reservas na receção.
 * <p>
 * Só o rececionista e o gestor (que tem as permissões do rececionista) podem usar este controller.
 * O rececionista cancela reservas mas não as apaga (o histórico fica guardado); o gestor só apaga
 * uma reserva se o check-in ainda não tiver sido efetuado.
 * Uma reserva só é criada se o quarto estiver disponível no período pedido. A reserva fica
 * guardada com o cliente, o quarto, as datas, o valor total estimado e o estado
 * {@code PENDENTE} (reservado, à espera de check-in).
 * <p>
 * Apoio de IA: Claude (Anthropic), revisto pela equipa.
 */
public class GestaoReservasController {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ReservaDAO reservaDAO;
    private final ClienteDAO clienteDAO;
    private final QuartoDAO quartoDAO;
    private final Sessao sessao;

    /**
     * @param reservaDAO onde as reservas são guardadas
     * @param clienteDAO onde os clientes estão guardados
     * @param quartoDAO  onde os quartos estão guardados
     * @param sessao     sessão com o utilizador autenticado
     */
    public GestaoReservasController(ReservaDAO reservaDAO, ClienteDAO clienteDAO,
                                    QuartoDAO quartoDAO, Sessao sessao) {
        this.reservaDAO = Objects.requireNonNull(reservaDAO);
        this.clienteDAO = Objects.requireNonNull(clienteDAO);
        this.quartoDAO = Objects.requireNonNull(quartoDAO);
        this.sessao = Objects.requireNonNull(sessao);
    }

    /**
     * Consulta os quartos livres num período. Um quarto que sai no dia em que outra reserva
     * entra continua livre (a reserva ocupa as noites até à véspera da saída).
     *
     * @param tipo    tipo de quarto pretendido, ou {@code null} para qualquer tipo
     * @param entrada primeira noite
     * @param saida   dia de saída
     * @return os quartos disponíveis, ordenados por número
     * @throws IllegalArgumentException se as datas forem inválidas
     * @throws SecurityException        se o utilizador não for rececionista nem gestor
     * @throws com.hotel4you.hotel4you.DAO.DAOException se não for possível ler
     */
    public List<Quarto> consultarQuartosDisponiveis(TipoQuarto tipo, LocalDate entrada, LocalDate saida) {
        exigirRececao();
        validarDatas(entrada, saida);
        return quartoDAO.listarDisponiveis(tipo, entrada, saida);
    }

    /**
     * Calcula o valor total estimado de uma estadia: número de noites × preço por noite do quarto.
     *
     * @param quarto  quarto escolhido
     * @param entrada primeira noite
     * @param saida   dia de saída
     * @return o valor total estimado, com duas casas decimais
     * @throws IllegalArgumentException se o quarto for nulo ou as datas forem inválidas
     */
    public BigDecimal estimarValor(Quarto quarto, LocalDate entrada, LocalDate saida) {
        if (quarto == null) {
            throw new IllegalArgumentException("Escolha um quarto.");
        }
        validarDatas(entrada, saida);
        long noites = ChronoUnit.DAYS.between(entrada, saida);
        return quarto.precoNoite().multiply(BigDecimal.valueOf(noites)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Cria uma reserva para um cliente num quarto disponível. A reserva fica no estado
     * {@code PENDENTE} (reservado, pendente de check-in) e é guardada na base de dados com o
     * valor total estimado.
     *
     * @param cliente cliente que reserva (tem de estar registado)
     * @param quarto  quarto pretendido (tem de estar ativo e livre no período)
     * @param entrada primeira noite
     * @param saida   dia de saída (depois da entrada)
     * @return a reserva guardada, já com identificador
     * @throws IllegalArgumentException se faltar o cliente ou o quarto, se as datas forem inválidas,
     *                                  se o cliente não estiver registado ou se o quarto estiver inativo
     *                                  ou já reservado no período
     * @throws SecurityException        se o utilizador não for rececionista nem gestor
     * @throws com.hotel4you.hotel4you.DAO.DAOException se não for possível guardar
     */
    public Reserva criarReserva(Cliente cliente, Quarto quarto, LocalDate entrada, LocalDate saida) {
        exigirRececao();
        if (cliente == null) {
            throw new IllegalArgumentException("Escolha um cliente.");
        }
        if (quarto == null) {
            throw new IllegalArgumentException("Escolha um quarto.");
        }
        validarDatas(entrada, saida);
        Cliente clienteGuardado = clienteDAO.procurarPorId(cliente.getId())
                .orElseThrow(() -> new IllegalArgumentException("O cliente escolhido não está registado."));
        Quarto quartoGuardado = quartoDAO.procurarPorId(quarto.getId())
                .orElseThrow(() -> new IllegalArgumentException("O quarto escolhido não existe."));
        if (!quartoGuardado.isAtivo()) {
            throw new IllegalArgumentException("O " + quartoGuardado + " não está ativo.");
        }
        if (reservaDAO.existeSobreposicao(quartoGuardado.getId(), entrada, saida)) {
            throw new IllegalArgumentException("O " + quartoGuardado + " não está disponível entre "
                    + entrada.format(FORMATO_DATA) + " e " + saida.format(FORMATO_DATA) + ".");
        }
        // Reserva nova: nasce com o estado PENDENTE. O DAO volta a confirmar a disponibilidade
        // dentro da transação e calcula e guarda o valor total.
        return reservaDAO.inserir(new Reserva(0, clienteGuardado, quartoGuardado, entrada, saida));
    }

    /**
     * Consulta todas as reservas guardadas, das mais recentes para as mais antigas.
     *
     * @return as reservas
     * @throws SecurityException se o utilizador não for rececionista nem gestor
     * @throws com.hotel4you.hotel4you.DAO.DAOException se não for possível ler
     */
    public List<Reserva> consultarReservas() {
        return consultarReservas(null);
    }

    /**
     * Consulta as reservas guardadas, filtrando pelo nome do cliente, pelo número do quarto
     * ou pelo número da reserva.
     *
     * @param pesquisa texto a procurar; vazio ou nulo devolve todas
     * @return as reservas que correspondem à pesquisa
     * @throws SecurityException se o utilizador não for rececionista nem gestor
     * @throws com.hotel4you.hotel4you.DAO.DAOException se não for possível ler
     */
    public List<Reserva> consultarReservas(String pesquisa) {
        exigirRececao();
        List<Reserva> reservas = reservaDAO.listarTodos();
        if (!Utils.estaPreenchido(pesquisa)) {
            return reservas;
        }
        String texto = pesquisa.trim().toLowerCase(Locale.ROOT);
        return reservas.stream()
                .filter(r -> r.getCliente().getNome().toLowerCase(Locale.ROOT).contains(texto)
                        || String.valueOf(r.getQuarto().getNumero()).contains(texto)
                        || String.valueOf(r.getId()).equals(texto))
                .collect(Collectors.toList());
    }

    /**
     * Cancela uma reserva a pedido do cliente. O estado passa a {@code CANCELADA}, o registo
     * fica guardado no histórico e o quarto volta a ficar disponível para esse período
     * (só as reservas pendentes ou com check-in ocupam o quarto).
     *
     * @param idReserva identificador da reserva
     * @return a reserva cancelada
     * @throws IllegalArgumentException se a reserva não existir, já estiver cancelada ou já tiver check-in
     * @throws SecurityException        se o utilizador não for rececionista nem gestor
     * @throws com.hotel4you.hotel4you.DAO.DAOException se não for possível guardar
     */
    public Reserva cancelarReserva(int idReserva) {
        exigirRececao();
        Reserva reserva = procurarReserva(idReserva);
        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            throw new IllegalArgumentException("A reserva n.º " + idReserva + " já está cancelada.");
        }
        if (reserva.getEstado() != EstadoReserva.PENDENTE) {
            throw new IllegalArgumentException("A reserva n.º " + idReserva
                    + " não pode ser cancelada porque o check-in já foi efetuado.");
        }
        reserva.cancelar();
        reservaDAO.atualizar(reserva);
        return reserva;
    }

    /**
     * Apaga o registo de uma reserva. Só o gestor pode apagar, e só se o check-in nunca foi
     * efetuado. O rececionista não pode apagar: apenas cancela, e o histórico fica guardado.
     *
     * @param idReserva identificador da reserva
     * @throws IllegalArgumentException se a reserva não existir ou o check-in já tiver sido efetuado
     * @throws SecurityException        se o utilizador não for o gestor
     * @throws com.hotel4you.hotel4you.DAO.DAOException se não for possível apagar
     */
    public void apagarReserva(int idReserva) {
        Permissoes.exigirPerfil(sessao, "Apenas o gestor pode apagar reservas. "
                + "O rececionista pode cancelar, mas o registo fica guardado no histórico.", Perfil.GESTOR);
        Reserva reserva = procurarReserva(idReserva);
        if (!reserva.podeSerApagada()) {
            throw new IllegalArgumentException("A reserva n.º " + idReserva
                    + " não pode ser apagada porque o check-in já foi efetuado.");
        }
        reservaDAO.remover(idReserva);
    }

    /**
     * @return {@code true} se o utilizador autenticado pode apagar reservas (só o gestor)
     */
    public boolean podeApagarReservas() {
        return sessao.getUtilizadorAtual()
                .map(utilizador -> utilizador.isAtivo() && utilizador.podeApagarReservas())
                .orElse(false);
    }

    private Reserva procurarReserva(int idReserva) {
        return reservaDAO.procurarPorId(idReserva)
                .orElseThrow(() -> new IllegalArgumentException("A reserva n.º " + idReserva + " não existe."));
    }

    private static void validarDatas(LocalDate entrada, LocalDate saida) {
        if (entrada == null) {
            throw new IllegalArgumentException("Indique a data de entrada.");
        }
        if (saida == null) {
            throw new IllegalArgumentException("Indique a data de saída.");
        }
        if (!saida.isAfter(entrada)) {
            throw new IllegalArgumentException("A data de saída deve ser posterior à data de entrada.");
        }
        if (entrada.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("A data de entrada não pode ser anterior a hoje.");
        }
    }

    private void exigirRececao() {
        Permissoes.exigirPerfil(sessao, "Apenas o rececionista ou o gestor podem gerir reservas.",
                Perfil.RECECIONISTA, Perfil.GESTOR);
    }
}