package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.CartaoDAO;
import com.hotel4you.hotel4you.DAO.PassagemDAO;
import com.hotel4you.hotel4you.DAO.QuartoDAO;
import com.hotel4you.hotel4you.DAO.ReservaDAO;
import com.hotel4you.hotel4you.DAO.UtilizadorDAO;
import com.hotel4you.hotel4you.Model.CartaoMagnetico;
import com.hotel4you.hotel4you.Model.LocalHotel;
import com.hotel4you.hotel4you.Model.Passagem;
import com.hotel4you.hotel4you.Model.PoliticaAcesso;
import com.hotel4you.hotel4you.Model.Quarto;
import com.hotel4you.hotel4you.Model.Reserva;
import com.hotel4you.hotel4you.Model.Utilizador;
import com.hotel4you.hotel4you.Model.enums.EstadoReserva;
import com.hotel4you.hotel4you.Model.enums.Perfil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Picagem de cartões e consulta das passagens (S6-06).
 * <p>
 * A picagem ({@link #picar}) <b>não exige sessão</b>: é feita na janela que abre antes do login, por
 * clientes, funcionários e pelo cartão mestre. Quem é o dono do cartão decide as regras:
 * mestre, funcionário (pelo perfil) ou cliente (reserva em check-in e NIF). As regras em si estão em
 * {@link PoliticaAcesso}; aqui só se reúnem os dados. Só se grava a passagem se o acesso for permitido.
 * <p>
 * A consulta das passagens por cartão, quarto e local é só do gestor.
 */
public class PassagemController {

    private final PassagemDAO passagemDAO;
    private final CartaoDAO cartaoDAO;
    private final ReservaDAO reservaDAO;
    private final QuartoDAO quartoDAO;
    private final UtilizadorDAO utilizadorDAO;
    private final Sessao sessao;

    /**
     * @param passagemDAO   locais e passagens
     * @param cartaoDAO     cartões
     * @param reservaDAO    reservas (para achar a reserva de um cartão de cliente)
     * @param quartoDAO     quartos
     * @param utilizadorDAO utilizadores (donos dos cartões de funcionário)
     * @param sessao        sessão da aplicação (só usada nas consultas do gestor)
     */
    public PassagemController(
            PassagemDAO passagemDAO,
            CartaoDAO cartaoDAO,
            ReservaDAO reservaDAO,
            QuartoDAO quartoDAO,
            UtilizadorDAO utilizadorDAO,
            Sessao sessao) {

        this.passagemDAO = Objects.requireNonNull(passagemDAO);
        this.cartaoDAO = Objects.requireNonNull(cartaoDAO);
        this.reservaDAO = Objects.requireNonNull(reservaDAO);
        this.quartoDAO = Objects.requireNonNull(quartoDAO);
        this.utilizadorDAO = Objects.requireNonNull(utilizadorDAO);
        this.sessao = Objects.requireNonNull(sessao);
    }

    /**
     * @return os locais do hotel onde se pode picar um cartão (não exige sessão)
     */
    public List<LocalHotel> listarLocais() {
        return passagemDAO.listarLocais();
    }

    /**
     * Pica um cartão num local. Não exige sessão iniciada.
     *
     * @param numeroCartao número do cartão
     * @param nif          NIF do titular; obrigatório nos cartões de cliente, ignorado nos outros
     * @param codigoLocal  código do local (ex.: {@code RESTAURANTE}, {@code QUARTO})
     * @param numeroQuarto número do quarto; obrigatório se o local for o {@code QUARTO}, ignorado nos outros
     * @return a passagem registada
     * @throws IllegalArgumentException se o local, o cartão, o quarto ou o NIF forem inválidos ou não existirem
     * @throws AcessoRecusadoException  se as regras de acesso recusarem o cartão nesse local
     */
    public Passagem picar(int numeroCartao, String nif, String codigoLocal, Integer numeroQuarto) {
        LocalHotel local = procurarLocal(codigoLocal);
        CartaoMagnetico cartao = cartaoDAO.procurarPorNumero(numeroCartao)
                .orElseThrow(() -> new IllegalArgumentException("Cartão não encontrado."));
        Quarto quarto = local.isQuarto() ? procurarQuarto(numeroQuarto) : null;

        PoliticaAcesso.Decisao decisao;
        if (cartao.isMestre()) {
            decisao = PoliticaAcesso.paraMestre(cartao, local);
        } else if (cartao.isDeFuncionario()) {
            Utilizador funcionario = utilizadorDAO.procurarPorId(cartao.getIdUtilizador())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "O cartão não está associado a um funcionário válido."));
            decisao = PoliticaAcesso.paraFuncionario(cartao, funcionario, local, quarto);
        } else {
            if (nif == null || nif.isBlank()) {
                throw new IllegalArgumentException("Indique o NIF do titular do cartão.");
            }
            Reserva reserva = reservaEmCheckIn(cartao);
            if (reserva != null && !mesmoNif(reserva.getCliente().getDocumento(), nif)) {
                throw new AcessoRecusadoException("O NIF não corresponde ao titular do cartão.");
            }
            decisao = PoliticaAcesso.paraCliente(cartao, reserva, local, quarto);
        }

        if (!decisao.permitido()) {
            throw new AcessoRecusadoException(decisao.motivo());
        }

        Passagem passagem = new Passagem(
                0,
                cartao.getId(),
                cartao.getNumero(),
                quarto == null ? null : quarto.getId(),
                quarto == null ? null : quarto.getNumero(),
                local.getCodigo(),
                LocalDateTime.now());
        passagemDAO.inserir(passagem);
        return passagem;
    }

    /**
     * @param numeroCartao número do cartão
     * @return as passagens do cartão, da mais recente para a mais antiga
     * @throws SecurityException se quem chama não for um gestor ativo
     */
    public List<Passagem> consultarCartao(int numeroCartao) {
        exigirGestor();
        return passagemDAO.listarPorCartao(numeroCartao);
    }

    /**
     * @param numeroQuarto número do quarto (ex.: 202)
     * @return as passagens de cartões nesse quarto, da mais recente para a mais antiga
     * @throws SecurityException se quem chama não for um gestor ativo
     */
    public List<Passagem> consultarQuarto(int numeroQuarto) {
        exigirGestor();
        return passagemDAO.listarPorQuarto(numeroQuarto);
    }

    /**
     * @param codigoLocal código do local (ex.: {@code PISCINA})
     * @return as passagens de cartões nesse local, da mais recente para a mais antiga
     * @throws SecurityException        se quem chama não for um gestor ativo
     * @throws IllegalArgumentException se o local não existir
     */
    public List<Passagem> consultarLocal(String codigoLocal) {
        exigirGestor();
        return passagemDAO.listarPorLocal(procurarLocal(codigoLocal).getCodigo());
    }

    private LocalHotel procurarLocal(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("Escolha um local válido.");
        }
        return passagemDAO.procurarLocal(codigo.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new IllegalArgumentException("Escolha um local válido."));
    }

    private Quarto procurarQuarto(Integer numero) {
        if (numero == null || numero <= 0) {
            throw new IllegalArgumentException("Indique o número do quarto.");
        }
        return quartoDAO.procurarPorNumero(numero)
                .orElseThrow(() -> new IllegalArgumentException("Quarto não encontrado."));
    }

    /** A reserva em check-in a que este cartão foi entregue, ou {@code null} se não houver. */
    private Reserva reservaEmCheckIn(CartaoMagnetico cartao) {
        // No máximo uma reserva em check-in por quarto (20 quartos), cabe numa só página.
        for (Reserva reserva : reservaDAO.listarPorEstados(List.of(EstadoReserva.CHECK_IN), 1, 100)) {
            if (reserva.getCartao() != null && reserva.getCartao().getId() == cartao.getId()) {
                return reserva;
            }
        }
        return null;
    }

    /** Compara NIF ignorando espaços e maiúsculas/minúsculas (ex.: "123 456 789" = "123456789"). */
    private static boolean mesmoNif(String guardado, String indicado) {
        return guardado.replaceAll("\\s+", "").equalsIgnoreCase(indicado.replaceAll("\\s+", ""));
    }

    private void exigirGestor() {
        Permissoes.exigirPerfil(sessao, "Só o gestor pode consultar passagens.", Perfil.GESTOR);
    }
}