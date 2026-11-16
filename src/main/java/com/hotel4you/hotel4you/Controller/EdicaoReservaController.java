package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.ReservaDAO;
import com.hotel4you.hotel4you.DAO.ServicoDAO;
import com.hotel4you.hotel4you.Model.ConsumoServico;
import com.hotel4you.hotel4you.Model.Reserva;
import com.hotel4you.hotel4you.Model.Servico;
import com.hotel4you.hotel4you.Model.enums.Perfil;

import java.time.LocalDate;
import java.util.List;

/**
 * Permite consultar e editar os serviços de uma reserva.
 */
public class EdicaoReservaController {

    private final ReservaDAO reservaDAO;
    private final ServicoDAO servicoDAO;
    private final Sessao sessao;

    public EdicaoReservaController(ReservaDAO reservaDAO, ServicoDAO servicoDAO, Sessao sessao) {
        this.reservaDAO = reservaDAO;
        this.servicoDAO = servicoDAO;
        this.sessao = sessao;
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