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
 * Permite consultar e editar os serviços de uma reserva.
 * <p>
 * S6-05: também se alteram os produtos do stock da reserva (por exemplo, uma só pessoa num quarto duplo
 * fica com 1 toalha em vez de 2). A alteração ajusta o stock disponível; a regra está no model
 * ({@link Reserva#alterarProduto(Artigo, int)}).
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
     * Muda a quantidade de um produto do stock na reserva e ajusta o stock disponível (S6-05).
     * Com quantidade 0 o produto sai da reserva.
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
     * Acrescenta um serviço à reserva.
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
        reserva.adicionarServicoEdicao(consumo);
        reservaDAO.atualizarServicos(reserva);
    }

    /**
     * Remove um serviço da reserva.
     *
     * O índice corresponde à posição do serviço na lista.
     */
    public void removerServico(int idReserva, int indice) {
        verificarPermissao();
        Reserva reserva = consultarReserva(idReserva);
        reserva.removerServicoEdicao(indice);
        reservaDAO.atualizarServicos(reserva);
    }

    /**
     * Verifica se o utilizador é rececionista ou gestor.
     */
    private void verificarPermissao() {
        Permissoes.exigirPerfil(sessao, "Apenas o rececionista ou gestor pode gerir reservas.", Perfil.RECECIONISTA, Perfil.GESTOR);
    }
}