package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.Model.Utilizador;

import java.util.Optional;

/**
 * Sessão da aplicação: guarda o utilizador autenticado e, através dele, o seu perfil.
 * Uma única instância é criada no {@code Main} e partilhada pelos controladores.
 */
public class Sessao {

    private Utilizador utilizadorAtual;

    /**
     * @param utilizador utilizador que acabou de fazer login
     */
    public void iniciar(Utilizador utilizador) {
        this.utilizadorAtual = utilizador;
    }

    /** Termina a sessão (logout). */
    public void terminar() {
        this.utilizadorAtual = null;
    }

    /** @return o utilizador autenticado, ou vazio se ninguém fez login */
    public Optional<Utilizador> getUtilizadorAtual() {
        return Optional.ofNullable(utilizadorAtual);
    }

    /** @return {@code true} se há um utilizador autenticado */
    public boolean temSessaoAtiva() {
        return utilizadorAtual != null;
    }
}
