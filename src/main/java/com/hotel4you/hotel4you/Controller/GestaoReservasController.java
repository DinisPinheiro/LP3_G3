package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.ClienteDAO;
import com.hotel4you.hotel4you.DAO.QuartoDAO;
import com.hotel4you.hotel4you.DAO.ReservaDAO;
import com.hotel4you.hotel4you.DAO.ServicoDAO;
import com.hotel4you.hotel4you.DAO.StockDAO;
import com.hotel4you.hotel4you.Model.Artigo;
import com.hotel4you.hotel4you.Model.Cliente;
import com.hotel4you.hotel4you.Model.ConsumoServico;
import com.hotel4you.hotel4you.Model.ProdutoReserva;
import com.hotel4you.hotel4you.Model.Quarto;
import com.hotel4you.hotel4you.Model.Reserva;
import com.hotel4you.hotel4you.Model.Servico;
import com.hotel4you.hotel4you.Model.enums.EstadoReserva;
import com.hotel4you.hotel4you.Model.enums.Perfil;
import com.hotel4you.hotel4you.Model.enums.TipoQuarto;
import com.hotel4you.hotel4you.Utils.Utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Criação, consulta, cancelamento e eliminação de reservas na receção.
 * <p>
 * Só o rececionista e o gestor (que tem as permissões do rececionista) podem usar este controller.
 * O rececionista cancela reservas mas não as apaga (o histórico fica guardado); o gestor apaga
 * reservas, mas a regra "só sem check-in" é aplicada pelo model e pelo DAO, não por este controller.
 * Uma reserva só é criada se o quarto estiver disponível no período pedido. A reserva fica
 * guardada com o cliente, o quarto, as datas, o valor total estimado e o estado
 * {@code PENDENTE} (reservado, à espera de check-in).
 * <p>
 * Na criação podem juntar-se serviços, extras e produtos do stock, com a quantidade (S5-06). O total
 * é calculado pelo model ({@link Reserva#calcularTotal()}) e os preços em vigor ficam guardados na
 * reserva (S5-09).
 * <p>
 */
public class GestaoReservasController {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ReservaDAO reservaDAO;
    private final ClienteDAO clienteDAO;
    private final QuartoDAO quartoDAO;
    private final ServicoDAO servicoDAO;
    private final StockDAO stockDAO;
    private final Sessao sessao;

    /**
     * @param reservaDAO onde as reservas são guardadas
     * @param clienteDAO onde os clientes estão guardados
     * @param quartoDAO  onde os quartos estão guardados
     * @param servicoDAO onde os serviços e extras estão guardados
     * @param stockDAO   onde os produtos do stock estão guardados
     * @param sessao     sessão com o utilizador autenticado
     */
    public GestaoReservasController(ReservaDAO reservaDAO, ClienteDAO clienteDAO, QuartoDAO quartoDAO,
                                    ServicoDAO servicoDAO, StockDAO stockDAO, Sessao sessao) {
        this.reservaDAO = Objects.requireNonNull(reservaDAO);
        this.clienteDAO = Objects.requireNonNull(clienteDAO);
        this.quartoDAO = Objects.requireNonNull(quartoDAO);
        this.servicoDAO = Objects.requireNonNull(servicoDAO);
        this.stockDAO = Objects.requireNonNull(stockDAO);
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
     * Consulta os serviços e extras que podem ser juntos a uma reserva (só os ativos).
     *
     * @return os serviços e extras ativos, com o valor e a forma de cobrança em vigor
     * @throws SecurityException se o utilizador não for rececionista nem gestor
     * @throws com.hotel4you.hotel4you.DAO.DAOException se não for possível ler
     */
    public List<Servico> consultarServicosDisponiveis() {
        exigirRececao();
        return servicoDAO.listarAtivos();
    }

    /**
     * Consulta os produtos do stock que podem ser juntos a uma reserva: os que ainda têm stock
     * disponível (stock do hotel menos o que já está usado noutras reservas).
     *
     * @return os produtos com stock disponível, ordenados por nome
     * @throws SecurityException se o utilizador não for rececionista nem gestor
     * @throws com.hotel4you.hotel4you.DAO.DAOException se não for possível ler
     */
    public List<Artigo> consultarProdutosDisponiveis() {
        exigirRececao();
        return stockDAO.listarArtigos().stream()
                .filter(artigo -> artigo.getQuantidadeDisponivel() > 0)
                .collect(Collectors.toList());
    }

    /**
     * Calcula o valor total estimado de uma estadia sem serviços nem extras.
     *
     * @param quarto  quarto escolhido
     * @param entrada primeira noite
     * @param saida   dia de saída
     * @return o valor total estimado, com duas casas decimais
     * @throws IllegalArgumentException se o quarto for nulo ou as datas forem inválidas
     */
    public BigDecimal estimarValor(Quarto quarto, LocalDate entrada, LocalDate saida) {
        return estimarValor(quarto, entrada, saida, List.of());
    }

    /**
     * Calcula o valor total estimado de uma estadia: preço base do quarto, vista e serviços e extras
     * escolhidos. A conta é feita pelo model ({@link Reserva#calcularTotal(BigDecimal, long, List)}).
     *
     * @param quarto   quarto escolhido
     * @param entrada  primeira noite
     * @param saida    dia de saída
     * @param servicos serviços e extras escolhidos, com a quantidade
     * @return o valor total estimado, com duas casas decimais
     * @throws IllegalArgumentException se o quarto for nulo ou as datas forem inválidas
     */
    public BigDecimal estimarValor(Quarto quarto, LocalDate entrada, LocalDate saida, List<ConsumoServico> servicos) {
        if (quarto == null) {
            throw new IllegalArgumentException("Escolha um quarto.");
        }
        validarDatas(entrada, saida);
        long noites = ChronoUnit.DAYS.between(entrada, saida);
        return Reserva.calcularTotal(quarto.precoNoite(), noites, servicos).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Cria uma reserva sem serviços nem produtos (ver
     * {@link #criarReserva(Cliente, Quarto, LocalDate, LocalDate, List, List)}).
     *
     * @param cliente cliente que reserva (tem de estar registado)
     * @param quarto  quarto pretendido (tem de estar ativo e livre no período)
     * @param entrada primeira noite
     * @param saida   dia de saída (depois da entrada)
     * @return a reserva guardada, já com identificador
     * @throws IllegalArgumentException se algum dado for inválido
     * @throws SecurityException        se o utilizador não for rececionista nem gestor
     * @throws com.hotel4you.hotel4you.DAO.DAOException se não for possível guardar
     */
    public Reserva criarReserva(Cliente cliente, Quarto quarto, LocalDate entrada, LocalDate saida) {
        return criarReserva(cliente, quarto, entrada, saida, List.of(), List.of());
    }

    /**
     * Cria uma reserva para um cliente num quarto disponível, com os serviços, extras e produtos
     * escolhidos. A reserva fica no estado {@code PENDENTE} (reservado, pendente de check-in) e é
     * guardada na base de dados com o valor total estimado.
     * <p>
     * Os preços em vigor neste momento (preço base do quarto, vista e serviços) ficam guardados na
     * reserva. Os produtos escolhidos passam a contar como usados e reduzem o stock disponível.
     *
     * @param cliente  cliente que reserva (tem de estar registado)
     * @param quarto   quarto pretendido (tem de estar ativo e livre no período)
     * @param entrada  primeira noite
     * @param saida    dia de saída (depois da entrada)
     * @param servicos serviços e extras escolhidos, com a quantidade (têm de estar ativos)
     * @param produtos produtos do stock escolhidos, com a quantidade (tem de haver stock disponível)
     * @return a reserva guardada, já com identificador
     * @throws IllegalArgumentException se faltar o cliente ou o quarto, se as datas forem inválidas,
     *                                  se o cliente não estiver registado, se o quarto estiver inativo
     *                                  ou já reservado no período, se um serviço já não estiver
     *                                  disponível ou se não houver stock suficiente de um produto
     * @throws SecurityException        se o utilizador não for rececionista nem gestor
     * @throws com.hotel4you.hotel4you.DAO.DAOException se não for possível guardar
     */
    public Reserva criarReserva(Cliente cliente, Quarto quarto, LocalDate entrada, LocalDate saida,
                                List<ConsumoServico> servicos, List<ProdutoReserva> produtos) {
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
        // Reserva nova: nasce com o estado PENDENTE e fica com os preços em vigor agora. O DAO volta
        // a confirmar a disponibilidade e o stock dentro da transação e guarda o valor total.
        return reservaDAO.inserir(new Reserva(0, clienteGuardado, quartoGuardado, entrada, saida,
                comValoresEmVigor(servicos), comStockDisponivel(produtos)));
    }

    /** Volta a ler cada serviço, para a reserva guardar o valor em vigor e não aceitar serviços desativados. */
    private List<ConsumoServico> comValoresEmVigor(List<ConsumoServico> servicos) {
        List<ConsumoServico> atuais = new ArrayList<>();
        for (ConsumoServico escolhido : servicos) {
            Servico servico = servicoDAO.procurarPorId(escolhido.getServico().getId())
                    .filter(Servico::isAtivo)
                    .orElseThrow(() -> new IllegalArgumentException("O serviço ou extra '"
                            + escolhido.getServico().getNome() + "' já não está disponível."));
            atuais.add(new ConsumoServico(servico, escolhido.getQuantidade(), LocalDate.now()));
        }
        return atuais;
    }

    /** Volta a ler o stock e confirma que há quantidade disponível para cada produto escolhido. */
    private List<ProdutoReserva> comStockDisponivel(List<ProdutoReserva> produtos) {
        if (produtos.isEmpty()) {
            return List.of();
        }
        Map<Integer, Artigo> stock = new HashMap<>();
        for (Artigo artigo : stockDAO.listarArtigos()) {
            stock.put(artigo.getId(), artigo);
        }
        Map<Integer, Integer> pedido = new HashMap<>();
        List<ProdutoReserva> atuais = new ArrayList<>();
        for (ProdutoReserva escolhido : produtos) {
            Artigo artigo = stock.get(escolhido.getArtigo().getId());
            if (artigo == null) {
                throw new IllegalArgumentException("O produto '" + escolhido.getArtigo().getNome()
                        + "' já não existe em stock.");
            }
            int total = pedido.merge(artigo.getId(), escolhido.getQuantidade(), Integer::sum);
            if (total > artigo.getQuantidadeDisponivel()) {
                throw new IllegalArgumentException("Stock insuficiente de '" + artigo.getNome() + "': há "
                        + artigo.getQuantidadeDisponivel() + " disponível(eis) e foram pedidos " + total + ".");
            }
            atuais.add(new ProdutoReserva(artigo, escolhido.getQuantidade()));
        }
        return atuais;
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
     * Apaga o registo de uma reserva. Só o gestor pode apagar; o rececionista apenas cancela
     * e o histórico fica guardado.
     * <p>
     * O controller só verifica a <b>permissão</b>. A regra do check-in (só se apaga uma reserva
     * que nunca teve check-in) é do model ({@code Reserva.podeSerApagada()}) e é aplicada
     * pelo DAO em {@code ReservaDAO.remover}.
     *
     * @param idReserva identificador da reserva
     * @throws SecurityException se o utilizador não for o gestor
     * @throws com.hotel4you.hotel4you.DAO.DAOException se a reserva não existir, se já teve
     *                                                  check-in ou se não for possível apagar
     */
    public void apagarReserva(int idReserva) {
        Permissoes.exigirPerfil(sessao, "Apenas o gestor pode apagar reservas. "
                + "O rececionista pode cancelar, mas o registo fica guardado no histórico.", Perfil.GESTOR);
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