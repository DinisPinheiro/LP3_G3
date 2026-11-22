package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.CartaoDAO;
import com.hotel4you.hotel4you.DAO.DAOException;
import com.hotel4you.hotel4you.DAO.UtilizadorDAO;
import com.hotel4you.hotel4you.Model.CartaoMagnetico;
import com.hotel4you.hotel4you.Model.Utilizador;
import com.hotel4you.hotel4you.Model.enums.Perfil;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Controlador responsável pela gestão de funcionários.
 * Permite criar, consultar e desativar utilizadores, que ficam guardados na base de dados
 * através do {@link UtilizadorDAO}. As falhas de acesso aos dados são devolvidas à View como
 * {@link AcessoDadosException}.
 */
public class GestaoFuncionariosController {

    /** Perfis dos funcionários do hotel, que recebem um cartão próprio ao serem criados (S6-06). */
    private static final Set<Perfil> PERFIS_COM_CARTAO =
            EnumSet.of(Perfil.GESTOR, Perfil.RECECIONISTA, Perfil.EMPRESA_RESTAURACAO);

    private final UtilizadorDAO utilizadorDAO;
    private final CartaoDAO cartaoDAO;
    private final Sessao sessao;

    /**
     * Cria o controller sem emissão de cartões (os funcionários criados ficam sem cartão).
     *
     * @param utilizadorDAO DAO onde os utilizadores são guardados
     * @param sessao        sessão com o utilizador autenticado (tem de ser gestor)
     */
    public GestaoFuncionariosController(
            UtilizadorDAO utilizadorDAO,
            Sessao sessao) {

        this(utilizadorDAO, null, sessao);
    }

    /**
     * @param utilizadorDAO DAO onde os utilizadores são guardados
     * @param cartaoDAO     DAO dos cartões; se não for nulo, cada gestor, rececionista ou empresa de
     *                      restauração criado recebe um cartão próprio
     * @param sessao        sessão com o utilizador autenticado (tem de ser gestor)
     */
    public GestaoFuncionariosController(
            UtilizadorDAO utilizadorDAO,
            CartaoDAO cartaoDAO,
            Sessao sessao) {

        this.utilizadorDAO = Objects.requireNonNull(utilizadorDAO);
        this.cartaoDAO = cartaoDAO;
        this.sessao = Objects.requireNonNull(sessao);
    }

    /**
     * Cria um funcionário com o perfil escolhido pelo gestor. Se este controller tiver acesso aos
     * cartões, o gestor, o rececionista e a empresa de restauração recebem um cartão próprio.
     *
     * @param nome     nome completo
     * @param username nome de utilizador (único)
     * @param password palavra-chave em claro (só o hash é guardado)
     * @param perfil   perfil do novo utilizador
     * @return o número do cartão emitido, ou 0 se o funcionário não recebeu cartão
     * @throws IllegalArgumentException se faltar algum campo ou o username já existir
     * @throws SecurityException        se quem chama não for um gestor ativo
     * @throws AcessoDadosException     se não for possível aceder à base de dados (ou emitir o cartão)
     */
    public int criarFuncionario(
            String nome,
            String username,
            String password,
            Perfil perfil) {

        exigirGestor();

        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException(
                    "Introduza o nome do funcionário.");
        }

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                    "Introduza o nome de utilizador.");
        }

        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException(
                    "Introduza a palavra-chave.");
        }

        if (perfil == null) {
            throw new IllegalArgumentException(
                    "Escolha um perfil.");
        }

        String nomeUtilizador = username.trim();

        try {
            // Verifica se já existe um utilizador com esse nome.
            if (utilizadorDAO.procurarPorUsername(nomeUtilizador).isPresent()) {
                throw new IllegalArgumentException(
                        "Já existe um utilizador com esse nome de utilizador.");
            }

            // Cria um utilizador ativo.
            // A classe Utilizador trata da proteção da palavra-chave (só o hash é guardado).
            Utilizador novoUtilizador = Utilizador.novo(
                    nomeUtilizador,
                    password,
                    nome.trim(),
                    perfil
            );

            utilizadorDAO.inserir(novoUtilizador);

            return emitirCartao(novoUtilizador);
        } catch (DAOException e) {
            throw new AcessoDadosException(e.getMessage(), e);
        }
    }

    /** Emite o cartão próprio do funcionário, se o perfil tem direito a um; devolve o número ou 0. */
    private int emitirCartao(Utilizador funcionario) {
        if (cartaoDAO == null || !PERFIS_COM_CARTAO.contains(funcionario.getPerfil())) {
            return 0;
        }
        try {
            CartaoMagnetico cartao = CartaoMagnetico.paraFuncionario(
                    cartaoDAO.proximoNumero(), funcionario.getId());
            cartaoDAO.inserir(cartao);
            return cartao.getNumero();
        } catch (DAOException e) {
            throw new AcessoDadosException(
                    "O funcionário foi criado, mas não foi possível emitir-lhe o cartão: " + e.getMessage(), e);
        }
    }

    /**
     * Devolve todos os utilizadores existentes.
     */
    public List<Utilizador> listarFuncionarios() {
        exigirGestor();

        try {
            return utilizadorDAO.listarTodos();
        } catch (DAOException e) {
            throw new AcessoDadosException(e.getMessage(), e);
        }
    }

    /**
     * Desativa um utilizador sem o apagar.
     */
    public void desativarFuncionario(int id) {
        exigirGestor();

        try {
            Utilizador utilizador = utilizadorDAO.procurarPorId(id)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "O utilizador selecionado não existe."));

            int idAtual = sessao.getUtilizadorAtual()
                    .orElseThrow(() -> new SecurityException(
                            "Não existe uma sessão iniciada."))
                    .getId();

            // Impede que o gestor desative a própria conta.
            if (utilizador.getId() == idAtual) {
                throw new IllegalArgumentException(
                        "Não pode desativar a sua própria conta enquanto tem sessão iniciada.");
            }

            utilizador.setAtivo(false);

            utilizadorDAO.atualizar(utilizador);
        } catch (DAOException e) {
            throw new AcessoDadosException(e.getMessage(), e);
        }
    }

    /**
     * Confirma se o utilizador autenticado é um gestor ativo.
     */
    private void exigirGestor() {

        boolean eGestor = sessao.getUtilizadorAtual()
                .map(utilizador ->
                        utilizador.getPerfil() == Perfil.GESTOR
                                && utilizador.isAtivo())
                .orElse(false);

        if (!eGestor) {
            throw new SecurityException(
                    "Apenas um Gestor pode aceder à gestão de funcionários.");
        }
    }
}