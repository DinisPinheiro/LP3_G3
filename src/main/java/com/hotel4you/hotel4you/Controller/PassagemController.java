package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.CartaoDAO;
import com.hotel4you.hotel4you.DAO.PassagemDAO;
import com.hotel4you.hotel4you.DAO.QuartoDAO;
import com.hotel4you.hotel4you.DAO.ReservaDAO;

import com.hotel4you.hotel4you.Model.*;
import com.hotel4you.hotel4you.Model.enums.EstadoReserva;
import com.hotel4you.hotel4you.Model.enums.Perfil;

import java.time.LocalDateTime;
import java.util.List;

public class PassagemController {

    private final PassagemDAO passagemDAO;
    private final CartaoDAO cartaoDAO;
    private final ReservaDAO reservaDAO;
    private final QuartoDAO quartoDAO;
    private final Sessao sessao;

    public PassagemController(
            PassagemDAO passagemDAO,
            CartaoDAO cartaoDAO,
            ReservaDAO reservaDAO,
            QuartoDAO quartoDAO,
            Sessao sessao) {

        this.passagemDAO = passagemDAO;
        this.cartaoDAO = cartaoDAO;
        this.reservaDAO = reservaDAO;
        this.quartoDAO = quartoDAO;
        this.sessao = sessao;
    }

    public Passagem registar(
            int numeroCartao,
            String local,
            Integer numeroQuarto) {

        // Verifica o perfil do funcionário.
        Perfil perfil = sessao.getUtilizadorAtual()
                .filter(Utilizador::isAtivo)
                .map(Utilizador::getPerfil)
                .orElseThrow(() ->
                        new SecurityException(
                                "É necessário iniciar sessão."));

        if (perfil != Perfil.GESTOR
                && perfil != Perfil.RECECIONISTA
                && perfil != Perfil.EMPRESA_RESTAURACAO) {

            throw new SecurityException(
                    "Este perfil não pode registar passagens.");
        }

        // Verifica o local escolhido.
        if (local == null
                || (!local.equals("QUARTO")
                && !local.equals("RESTAURANTE")
                && !local.equals("PISCINA")
                && !local.equals("COZINHA"))) {

            throw new IllegalArgumentException(
                    "Escolha um local válido.");
        }

        // A restauração só pode registar estes locais.
        if (perfil == Perfil.EMPRESA_RESTAURACAO
                && !local.equals("RESTAURANTE")
                && !local.equals("COZINHA")) {

            throw new SecurityException(
                    "A restauração só pode registar " +
                            "acessos à cozinha/restaurante.");
        }

        // Procura o cartão.
        CartaoMagnetico cartao =
                cartaoDAO.procurarPorNumero(numeroCartao)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Cartão não encontrado."));

        if (!cartao.isAtivo()) {
            throw new IllegalArgumentException(
                    "O cartão está desativado.");
        }

        Integer idQuarto = null;

        // Cartão mestre.
        if (cartao.isMestre()) {

            if (local.equals("QUARTO")
                    && (numeroQuarto == null
                    || numeroQuarto <= 0)) {

                throw new IllegalArgumentException(
                        "Indique o número do quarto " +
                                "para o cartão mestre.");
            }

            if (numeroQuarto != null && numeroQuarto > 0) {

                for (Quarto quarto : quartoDAO.listarTodos()) {

                    if (quarto.getNumero() == numeroQuarto) {

                        idQuarto = quarto.getId();
                        break;
                    }
                }

                if (idQuarto == null) {
                    throw new IllegalArgumentException(
                            "Quarto não encontrado.");
                }
            }

        } else {

            // Cartão normal: procura a reserva ativa.
            Reserva reservaCartao = null;

            for (Reserva reserva : reservaDAO.listarTodos()) {

                if (reserva.getCartao() != null
                        && reserva.getCartao().getId()
                        == cartao.getId()
                        && reserva.getEstado()
                        == EstadoReserva.CHECK_IN) {

                    reservaCartao = reserva;
                    break;
                }
            }

            if (reservaCartao == null) {

                throw new IllegalArgumentException(
                        "O cartão não tem uma reserva " +
                                "com check-in ativo.");
            }

            idQuarto = reservaCartao.getQuarto().getId();

            // Só pode entrar no quarto reservado.
            if (local.equals("QUARTO")) {

                if (numeroQuarto == null
                        || numeroQuarto
                        != reservaCartao.getQuarto().getNumero()) {

                    throw new IllegalArgumentException(
                            "O cartão só permite entrar " +
                                    "no quarto da reserva.");
                }

            } else if (local.equals("COZINHA")) {

                throw new IllegalArgumentException(
                        "Um cartão de cliente não permite " +
                                "acesso à cozinha.");

            } else {

                // Restaurante e piscina:
                // verifica os serviços da reserva.
                boolean incluido = false;

                for (ConsumoServico consumo :
                        reservaCartao.getConsumos()) {

                    String nome =
                            consumo.getServico().getNome();

                    if (nome != null
                            && nome.toUpperCase()
                            .contains(local)) {

                        incluido = true;
                    }
                }

                if (!incluido) {

                    throw new IllegalArgumentException(
                            "Este local não está incluído " +
                                    "nos serviços da reserva.");
                }
            }
        }

        // Só grava se todas as verificações passaram.
        Passagem passagem = new Passagem(
                0,
                cartao.getId(),
                cartao.getNumero(),
                idQuarto,
                local,
                LocalDateTime.now()
        );

        passagemDAO.inserir(passagem);

        return passagem;
    }

    public List<Passagem> consultarCartao(int numero) {

        exigirGestor();

        return passagemDAO.listarPorCartao(numero);
    }

    public List<Passagem> consultarQuarto(int numero) {

        exigirGestor();

        return passagemDAO.listarPorQuarto(numero);
    }

    private void exigirGestor() {

        Permissoes.exigirPerfil(
                sessao,
                "Só o gestor pode consultar passagens.",
                Perfil.GESTOR
        );
    }
}
