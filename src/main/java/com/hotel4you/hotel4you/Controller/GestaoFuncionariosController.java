package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.DAOException;
import com.hotel4you.hotel4you.DAO.UtilizadorDAO;
import com.hotel4you.hotel4you.Model.Utilizador;
import com.hotel4you.hotel4you.Model.enums.Perfil;

import java.util.List;
import java.util.Objects;

/**
 * Controlador responsável pela gestão de funcionários.
 * Permite criar, consultar e desativar utilizadores, que ficam guardados na base de dados
 * através do {@link UtilizadorDAO}. As falhas de acesso aos dados são devolvidas à View como
 * {@link AcessoDadosException}.
 */
public class GestaoFuncionariosController {

    private final UtilizadorDAO utilizadorDAO;
    private final Sessao sessao;

    /**
     * @param utilizadorDAO DAO onde os utilizadores são guardados
     * @param sessao        sessão com o utilizador autenticado (tem de ser gestor)
     */
    public GestaoFuncionariosController(
            UtilizadorDAO utilizadorDAO,
            Sessao sessao) {

        this.utilizadorDAO = Objects.requireNonNull(utilizadorDAO);
        this.sessao = Objects.requireNonNull(sessao);
    }

    /**
     * Cria um funcionário com o perfil escolhido pelo gestor.
     *
     * @param nome     nome completo
     * @param username nome de utilizador (único)
     * @param password palavra-chave em claro (só o hash é guardado)
     * @param perfil   perfil do novo utilizador
     * @throws IllegalArgumentException se faltar algum campo ou o username já existir
     * @throws SecurityException        se quem chama não for um gestor ativo
     * @throws AcessoDadosException     se não for possível aceder à base de dados
     */
    public void criarFuncionario(
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
        } catch (DAOException e) {
            throw new AcessoDadosException(e.getMessage(), e);
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