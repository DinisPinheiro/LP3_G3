package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.ReservaDAO;
import com.hotel4you.hotel4you.DAO.ServicoDAO;
import com.hotel4you.hotel4you.DAO.StockDAO;
import com.hotel4you.hotel4you.Model.Artigo;
import com.hotel4you.hotel4you.Model.ConsumoServico;
import com.hotel4you.hotel4you.Model.Reserva;
import com.hotel4you.hotel4you.Model.Servico;
import com.hotel4you.hotel4you.Model.enums.Perfil;

import java.time.LocalDate;
import java.util.List;

/**
 * Permite consultar e editar os serviços e as datas de uma reserva.
 * <p>
 * S6-05: também se alteram os produtos do stock da reserva (por exemplo, uma só pessoa num quarto duplo
 * fica com 1 toalha em vez de 2). A alteração ajusta o stock disponível; a regra está no model
 * ({@link Reserva#alterarProduto(Artigo, int)}).
 * <p>
 * S7-01: o stock só baixa no check-in. Numa reserva pendente os produtos mudam sem mexer no stock; com
 * check-in a diferença sai do stock ou volta a ele. Acrescentar ou remover um serviço ligado ao stock
 * (ex.: bebidas) também dá baixa ou devolve o produto.
 */
public class EdicaoReservaController {

    private final ReservaDAO reservaDAO;
    private final ServicoDAO servicoDAO;
    /** Stock do hotel, para alterar os produtos da reserva (S6-05); pode ser {@code null}. */
    private final StockDAO stockDAO;
    private final Sessao sessao;

    public EdicaoReservaController(ReservaDAO reservaDAO, ServicoDAO servicoDAO, Sessao sessao) {
        this(reservaDAO, servicoDAO, null, sessao);
    }

    /**
     * @param reservaDAO onde as reservas estão guardadas
     * @param servicoDAO onde os serviços estão guardados
     * @param stockDAO   stock do hotel, para alterar os produtos da reserva (S6-05)
     * @param sessao     sessão com o utilizador autenticado
     */
    public EdicaoReservaController(ReservaDAO reservaDAO, ServicoDAO servicoDAO, StockDAO stockDAO, Sessao sessao) {
        this.reservaDAO = reservaDAO;
        this.servicoDAO = servicoDAO;
        this.stockDAO = stockDAO;
        this.sessao = sessao;
    }

    /**
     * @return os produtos do stock, com o stock real e o disponível, para escolher na reserva
     */
    public List<Artigo> listarProdutosStock() {
        verificarPermissao();
        return stockDAO == null ? List.of() : stockDAO.listarArtigos();
    }

    /**
     * Muda a quantidade de um produto do stock na reserva. Com check-in ajusta o stock disponível
     * (S6-05); numa reserva pendente o stock ainda não baixou (S7-01). Com quantidade 0 o produto sai da reserva.
     *
     * @param idReserva  identificador da reserva
     * @param idArtigo   identificador do produto
     * @param quantidade nova quantidade (0 retira o produto)
     * @throws IllegalArgumentException se a reserva ou o produto não existirem, a quantidade for negativa,
     *                                  a reserva não estiver pendente nem com check-in ou o stock não chegar
     */
    public void alterarProduto(int idReserva, int idArtigo, int quantidade) {
        verificarPermissao();
        if (stockDAO == null) {
            throw new IllegalStateException("O stock não está disponível neste ecrã.");
        }
        Reserva reserva = consultarReserva(idReserva);
        Artigo artigo = null;
        for (Artigo doStock : stockDAO.listarArtigos()) {
            if (doStock.getId() == idArtigo) {
                artigo = doStock;
            }
        }
        if (artigo == null) {
            throw new IllegalArgumentException("Escolha um produto que exista em stock.");
        }
        if (quantidade > 0) {
            artigo.exigirUnidadePrincipal();   // S7-02: as reservas contam na unidade principal
        }
        try {
            reserva.alterarProduto(artigo, quantidade);
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
        reservaDAO.atualizar(reserva);
    }

    /**
     * Lista as reservas existentes.
     */
    public List<Reserva> listarReservas() {
        verificarPermissao();
        return reservaDAO.listarTodos();
    }

    /**
     * Consulta os detalhes de uma reserva.
     */
    public Reserva consultarReserva(int id) {
        verificarPermissao();
        return reservaDAO.procurarPorId(id).orElseThrow(() -> new IllegalArgumentException("Reserva não encontrada."));
    }

    /**
     * Lista os serviços que estão disponíveis.
     */
    public List<Servico> listarServicos() {
        verificarPermissao();return servicoDAO.listarAtivos();
    }

    /**
     * Acrescenta um serviço à reserva. Se o serviço estiver ligado a um produto do stock (ex.: bebidas),
     * esse produto dá baixa no stock (S7-01).
     *
     * @throws IllegalArgumentException se o serviço não existir ou não estiver ativo, a quantidade for
     *                                  inválida ou não houver stock disponível do produto do serviço
     */
    public void adicionarServico(int idReserva, Servico servico, int quantidade) {
        verificarPermissao();
        Reserva reserva = consultarReserva(idReserva);

        if (servico == null) {
            throw new IllegalArgumentException("Selecione um serviço.");
        }

        if (quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser superior a zero.");
        }

        // Consulta o serviço atual na base de dados.
        Servico servicoGuardado = servicoDAO.procurarPorId(servico.getId()).orElseThrow(() -> new IllegalArgumentException("O serviço não existe."));

        if (!servicoGuardado.isAtivo()) {
            throw new IllegalArgumentException("O serviço já não está disponível.");
        }

        ConsumoServico consumo = new ConsumoServico(servicoGuardado, quantidade, LocalDate.now());
        if (stockDAO == null) {
            reserva.adicionarServicoEdicao(consumo);
        } else {
            List<Artigo> stock = stockDAO.listarArtigos();
            exigirServicosEditaveis(reserva);
            try {
                reserva.adicionarServicoEdicao(consumo, stock);   // a baixa do produto é feita no model
            } catch (IllegalStateException e) {
                throw new IllegalArgumentException(e.getMessage(), e);   // stock disponível insuficiente
            }
        }
        reservaDAO.atualizarServicos(reserva);
    }

    /**
     * Remove um serviço da reserva. Se o serviço usava um produto do stock, esse produto volta ao stock
     * disponível (S7-01).
     *
     * O índice corresponde à posição do serviço na lista.
     */
    public void removerServico(int idReserva, int indice) {
        verificarPermissao();
        Reserva reserva = consultarReserva(idReserva);
        if (stockDAO == null) {
            reserva.removerServicoEdicao(indice);
        } else {
            exigirServicosEditaveis(reserva);
            reserva.removerServicoEdicao(indice, stockDAO.listarArtigos());
        }
        reservaDAO.atualizarServicos(reserva);
    }

    /** Os serviços só se editam com check-in (o erro é o mesmo com ou sem stock). */
    private static void exigirServicosEditaveis(Reserva reserva) {
        if (!reserva.podeEditarServicos()) {
            throw new IllegalStateException("Não é possível editar os serviços desta reserva.");
        }
    }

    /**
     * Altera as datas de uma reserva ativa (S6-08), para acomodar alterações do cliente.
     * <p>
     * A alteração é recusada se o quarto estiver ocupado por outra reserva nas novas datas. A
     * disponibilidade é a mesma regra usada ao criar reservas ({@link ReservaDAO#existeSobreposicao}) e
     * o valor total é recalculado com o novo número de noites e guardado.
     *
     * @param idReserva reserva a alterar
     * @param entrada   nova primeira noite (não pode ser anterior a hoje se mudar)
     * @param saida     novo dia de saída (depois da entrada)
     * @return a reserva com as novas datas e o total recalculado
     * @throws IllegalArgumentException se a reserva não existir, se as datas forem inválidas ou se o quarto
     *                                  estiver ocupado nas novas datas
     * @throws IllegalStateException    se a reserva não estiver ativa (pendente ou com check-in)
     * @throws SecurityException        se o utilizador não for rececionista nem gestor
     * @throws com.hotel4you.hotel4you.DAO.DAOException se não for possível guardar
     */
    public Reserva alterarDatas(int idReserva, LocalDate entrada, LocalDate saida) {
        verificarPermissao();
        Reserva reserva = consultarReserva(idReserva);

        if (entrada == null) {
            throw new IllegalArgumentException("Indique a data de entrada.");
        }
        if (saida == null) {
            throw new IllegalArgumentException("Indique a data de saída.");
        }
        if (!saida.isAfter(entrada)) {
            throw new IllegalArgumentException("A data de saída deve ser posterior à data de entrada.");
        }
        if (!entrada.equals(reserva.getDataEntrada()) && entrada.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("A data de entrada não pode ser anterior a hoje.");
        }

        LocalDate entradaAnterior = reserva.getDataEntrada();
        LocalDate saidaAnterior = reserva.getDataSaida();
        reserva.alterarDatas(entrada, saida, reservaDAO::existeSobreposicao);
        try {
            reservaDAO.atualizarDatas(reserva);
        } catch (RuntimeException e) {
            // Se não ficou guardada, a reserva volta às datas anteriores.
            reserva.alterarDatas(entradaAnterior, saidaAnterior, (quarto, e1, s1, ignorada) -> false);
            throw e;
        }
        return reserva;
    }

    /**
     * Verifica se o utilizador é rececionista ou gestor.
     */
    private void verificarPermissao() {
        Permissoes.exigirPerfil(sessao, "Apenas o rececionista ou gestor pode gerir reservas.", Perfil.RECECIONISTA, Perfil.GESTOR);
    }
}
