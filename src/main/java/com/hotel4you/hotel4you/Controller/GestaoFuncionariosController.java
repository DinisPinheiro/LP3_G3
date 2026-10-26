package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.UtilizadorDAO;
import com.hotel4you.hotel4you.Model.Utilizador;
import com.hotel4you.hotel4you.Model.enums.Perfil;

import java.util.List;
import java.util.Objects;

/**
 * Controlador responsável pela gestão de funcionários.
 * Permite criar, consultar e desativar utilizadores.
 */
public class GestaoFuncionariosController {

    private final UtilizadorDAO utilizadorDAO;
    private final Sessao sessao;

    public GestaoFuncionariosController(
            UtilizadorDAO utilizadorDAO,
            Sessao sessao) {

        this.utilizadorDAO = Objects.requireNonNull(utilizadorDAO);
        this.sessao = Objects.requireNonNull(sessao);
    }

    /**
     * Cria um funcionário com o perfil escolhido pelo gestor.
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

        // Verifica se já existe um utilizador com esse nome.
        if (utilizadorDAO.procurarPorUsername(nomeUtilizador).isPresent()) {
            throw new IllegalArgumentException(
                    "Já existe um utilizador com esse nome de utilizador.");
        }

        // Cria um utilizador ativo.
        // A classe Utilizador trata da proteção da palavra-chave.
        Utilizador novoUtilizador = Utilizador.novo(
                nomeUtilizador,
                password,
                nome.trim(),
                perfil
        );

        utilizadorDAO.inserir(novoUtilizador);
    }

    /**
     * Devolve todos os utilizadores existentes.
     */
    public List<Utilizador> listarFuncionarios() {
        exigirGestor();

        return utilizadorDAO.listarTodos();
    }

    /**
     * Desativa um utilizador sem o apagar.
     */
    public void desativarFuncionario(int id) {
        exigirGestor();

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