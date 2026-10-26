package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.DAOException;
import com.hotel4you.hotel4you.DAO.UtilizadorDAO;
import com.hotel4you.hotel4you.Model.Utilizador;

import java.util.Objects;
import java.util.Optional;

/**
 * Autentica utilizadores (login com username e palavra-chave).
 * <p>
 * É o único ponto por onde o LoginViewController acede ao {@link UtilizadorDAO}: a View
 * não conhece o DAO nem o hash das palavras-chave.
 */
public class LoginController {

    private final UtilizadorDAO utilizadorDAO;
    private final Sessao sessao;

    /**
     * @param utilizadorDAO DAO onde estão os utilizadores
     * @param sessao        sessão onde fica o utilizador autenticado
     */
    public LoginController(UtilizadorDAO utilizadorDAO, Sessao sessao) {
        this.utilizadorDAO = Objects.requireNonNull(utilizadorDAO);
        this.sessao = Objects.requireNonNull(sessao);
    }

    /**
     * Valida as credenciais. Se estiverem certas e o utilizador estiver ativo, inicia a sessão
     * associada ao seu perfil. A palavra-chave nunca é comparada em claro.
     *
     * @param username username introduzido
     * @param password palavra-chave introduzida
     * @return {@code true} se o login foi bem-sucedido; {@code false} se as credenciais forem inválidas
     * @throws AcessoDadosException se não for possível consultar os utilizadores
     */
    public boolean autenticar(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            return false;
        }
        try {
            Optional<Utilizador> encontrado = utilizadorDAO.procurarPorUsername(username.trim())
                    .filter(Utilizador::isAtivo)
                    .filter(u -> u.verificarPassword(password));
            encontrado.ifPresent(sessao::iniciar);
            return encontrado.isPresent();
        } catch (DAOException e) {
            throw new AcessoDadosException(e.getMessage(), e);
        }
    }

    /** Termina a sessão do utilizador atual. */
    public void terminarSessao() {
        sessao.terminar();
    }
}
