package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.DAOException;
import com.hotel4you.hotel4you.DAO.FornecedorDAO;
import com.hotel4you.hotel4you.DAO.UtilizadorDAO;
import com.hotel4you.hotel4you.Model.Fornecedor;
import com.hotel4you.hotel4you.Model.Utilizador;
import com.hotel4you.hotel4you.Model.enums.Perfil;
import com.hotel4you.hotel4you.Utils.Utils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Criação e consulta de fornecedores pelo gestor (S5-02).
 * <p>
 * Ao criar um fornecedor o gestor indica o código, o nome, a morada, o código postal e a cidade, e a
 * conta de login (utilizador e palavra-chave) com que o fornecedor entra com o perfil Fornecedor.
 * Se o fornecedor já existir sem conta (porque deu entrada um XML dele antes de haver este perfil),
 * a ficha é completada e a conta fica ligada a ele em vez de se criar um duplicado.
 */
public class GestaoFornecedoresController {

    private final FornecedorDAO fornecedorDAO;
    private final UtilizadorDAO utilizadorDAO;
    private final Sessao sessao;

    /**
     * @param fornecedorDAO onde os fornecedores são guardados
     * @param utilizadorDAO onde as contas de login são guardadas
     * @param sessao        sessão com o utilizador autenticado (tem de ser gestor)
     */
    public GestaoFornecedoresController(FornecedorDAO fornecedorDAO, UtilizadorDAO utilizadorDAO, Sessao sessao) {
        this.fornecedorDAO = Objects.requireNonNull(fornecedorDAO);
        this.utilizadorDAO = Objects.requireNonNull(utilizadorDAO);
        this.sessao = Objects.requireNonNull(sessao);
    }

    /**
     * Cria um fornecedor com a sua conta de login.
     *
     * @param identificador código do fornecedor (único; é o {@code PartyIdentifier} dos seus XML)
     * @param nome          nome
     * @param morada        morada
     * @param codigoPostal  código postal
     * @param cidade        cidade
     * @param username      nome de utilizador da conta (único)
     * @param password      palavra-chave em claro (só o hash é guardado)
     * @return o fornecedor guardado, já ligado à conta
     * @throws IllegalArgumentException se faltar algum campo, um texto for longo demais, o código já
     *                                  tiver conta ou o nome de utilizador já existir
     * @throws SecurityException        se quem chama não for um gestor ativo
     * @throws AcessoDadosException     se não for possível guardar
     */
    public Fornecedor criarFornecedor(String identificador, String nome, String morada, String codigoPostal,
                                      String cidade, String username, String password) {
        exigirGestor();
        ValidacaoFornecedor.validar(identificador, nome, morada, codigoPostal, cidade);
        if (!Utils.estaPreenchido(username)) {
            throw new IllegalArgumentException("Introduza o nome de utilizador da conta do fornecedor.");
        }
        if (!Utils.estaPreenchido(password)) {
            throw new IllegalArgumentException("Introduza a palavra-chave da conta do fornecedor.");
        }
        try {
            Optional<Fornecedor> existente = fornecedorDAO.procurarPorIdentificador(identificador);
            if (existente.isPresent() && existente.get().temConta()) {
                throw new IllegalArgumentException("Já existe um fornecedor com o código '"
                        + identificador.trim() + "' e com conta própria.");
            }
            if (utilizadorDAO.procurarPorUsername(username).isPresent()) {
                throw new IllegalArgumentException("Já existe um utilizador com esse nome de utilizador.");
            }
            Utilizador conta = utilizadorDAO.inserir(Utilizador.novo(username.trim(), password, nome.trim(),
                    Perfil.FORNECEDOR));
            try {
                return guardarLigadoA(conta, existente, identificador, nome, morada, codigoPostal, cidade);
            } catch (DAOException e) {
                desfazerConta(conta, e);
                throw e;
            }
        } catch (DAOException e) {
            throw new AcessoDadosException(e.getMessage(), e);
        }
    }

    /**
     * @return todos os fornecedores, ordenados por nome
     * @throws SecurityException    se quem chama não for um gestor ativo
     * @throws AcessoDadosException se não for possível ler
     */
    public List<Fornecedor> listarFornecedores() {
        exigirGestor();
        try {
            return fornecedorDAO.listarTodos();
        } catch (DAOException e) {
            throw new AcessoDadosException(e.getMessage(), e);
        }
    }

    private Fornecedor guardarLigadoA(Utilizador conta, Optional<Fornecedor> existente, String identificador,
                                      String nome, String morada, String codigoPostal, String cidade) {
        if (existente.isPresent()) {
            Fornecedor fornecedor = existente.get();
            fornecedor.atualizarFicha(nome, morada, codigoPostal, cidade);
            fornecedor.setIdUtilizador(conta.getId());
            fornecedorDAO.atualizar(fornecedor);
            return fornecedor;
        }
        Fornecedor novo = new Fornecedor(0, nome, identificador, "", morada, codigoPostal, cidade, conta.getId());
        return fornecedorDAO.inserir(novo);
    }

    /** Se o fornecedor não ficou guardado, a conta acabada de criar não deve ficar sem dono. */
    private void desfazerConta(Utilizador conta, DAOException original) {
        try {
            utilizadorDAO.remover(conta.getId());
        } catch (DAOException e) {
            original.addSuppressed(e);
        }
    }

    private void exigirGestor() {
        Permissoes.exigirPerfil(sessao, "Apenas o gestor pode gerir fornecedores.", Perfil.GESTOR);
    }
}
