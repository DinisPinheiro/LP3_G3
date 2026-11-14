package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.ServicoDAO;
import com.hotel4you.hotel4you.Model.Servico;
import com.hotel4you.hotel4you.Model.enums.Perfil;
import com.hotel4you.hotel4you.Model.enums.TipoCobranca;
import com.hotel4you.hotel4you.Model.enums.TipoServico;
import com.hotel4you.hotel4you.Utils.Utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * Configuração dos serviços do hotel e dos extras de quarto (S4-05).
 * <p>
 * O gestor cria, edita (incluindo o preço) e desativa serviços e extras, sem mexer na base de dados.
 * O rececionista só os consulta (e só vê os ativos). Cada item é cobrado por unidade ou por noite
 * (S5-09). Os serviços juntos a uma reserva são somados ao preço do alojamento no total
 * ({@link com.hotel4you.hotel4you.Model.Reserva#calcularTotal()}) com o valor que tinham quando a
 * reserva foi criada: alterar o preço aqui só se aplica às reservas seguintes.
 * <p>
 */
public class GestaoServicosController {

    /** Tamanhos máximos das colunas de {@code dbo.Servico} e valor máximo de DECIMAL(10,2). */
    private static final int MAX_NOME = 80;
    private static final int MAX_CATEGORIA = 40;
    private static final int MAX_DESCRICAO = 200;
    private static final BigDecimal PRECO_MAXIMO = new BigDecimal("99999999.99");

    private final ServicoDAO servicoDAO;
    private final Sessao sessao;

    /**
     * @param servicoDAO onde os serviços e extras são guardados
     * @param sessao     sessão com o utilizador autenticado
     */
    public GestaoServicosController(ServicoDAO servicoDAO, Sessao sessao) {
        this.servicoDAO = Objects.requireNonNull(servicoDAO);
        this.sessao = Objects.requireNonNull(sessao);
    }

    /**
     * @return {@code true} se o utilizador atual pode criar, editar e desativar (só o gestor)
     */
    public boolean podeConfigurar() {
        return Permissoes.temPerfil(sessao, Perfil.GESTOR);
    }

    /**
     * Consulta os serviços e extras. O gestor vê todos (incluindo os desativados); o rececionista
     * só vê os ativos.
     *
     * @return os serviços e extras, agrupados por tipo e categoria
     * @throws SecurityException se o utilizador não for rececionista nem gestor
     * @throws com.hotel4you.hotel4you.DAO.DAOException se não for possível ler
     */
    public List<Servico> consultarServicos() {
        Permissoes.exigirPerfil(sessao, "Apenas o rececionista ou o gestor podem consultar os serviços e extras.",
                Perfil.RECECIONISTA, Perfil.GESTOR);
        return podeConfigurar() ? servicoDAO.listarTodos() : servicoDAO.listarAtivos();
    }

    /**
     * Cria um serviço ou extra novo, ativo.
     *
     * @param tipo      serviço do hotel ou extra de quarto
     * @param nome      nome (obrigatório, sem repetir)
     * @param categoria categoria (ex.: "Spa", "Bebidas"); pode ser vazia
     * @param descricao descrição; pode ser vazia
     * @param valor     preço acrescentado ao cliente, não negativo
     * @param cobranca  forma de cobrança: por unidade ou por noite
     * @return o item guardado, já com identificador
     * @throws IllegalArgumentException se algum dado for inválido ou o nome já existir
     * @throws SecurityException        se o utilizador não for gestor
     * @throws com.hotel4you.hotel4you.DAO.DAOException se não for possível guardar
     */
    public Servico criarServico(TipoServico tipo, String nome, String categoria, String descricao,
                                BigDecimal valor, TipoCobranca cobranca) {
        exigirGestor();
        BigDecimal preco = validar(tipo, nome, categoria, descricao, valor, cobranca);
        exigirNomeLivre(nome, 0);
        return servicoDAO.inserir(new Servico(0, tipo, nome, categoria, descricao, preco, cobranca, true));
    }

    /**
     * Edita um serviço ou extra (incluindo o preço) ou desativa-o. O preço novo só se aplica às
     * reservas criadas a partir daqui.
     *
     * @param id        identificador do item
     * @param tipo      serviço do hotel ou extra de quarto
     * @param nome      nome (obrigatório, sem repetir)
     * @param categoria categoria; pode ser vazia
     * @param descricao descrição; pode ser vazia
     * @param valor     preço acrescentado ao cliente, não negativo
     * @param cobranca  forma de cobrança: por unidade ou por noite
     * @param ativo     {@code false} para desativar o item
     * @throws IllegalArgumentException se o item não existir, algum dado for inválido ou o nome já existir noutro item
     * @throws SecurityException        se o utilizador não for gestor
     * @throws com.hotel4you.hotel4you.DAO.DAOException se não for possível guardar
     */
    public void editarServico(int id, TipoServico tipo, String nome, String categoria, String descricao,
                              BigDecimal valor, TipoCobranca cobranca, boolean ativo) {
        exigirGestor();
        Servico servico = servicoDAO.procurarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("O serviço ou extra selecionado não existe."));
        BigDecimal preco = validar(tipo, nome, categoria, descricao, valor, cobranca);
        exigirNomeLivre(nome, id);

        servico.setTipo(tipo);
        servico.setNome(nome);
        servico.setCategoria(categoria);
        servico.setDescricao(descricao);
        servico.setValor(preco);
        servico.setCobranca(cobranca);
        servico.setAtivo(ativo);
        servicoDAO.atualizar(servico);
    }

    private BigDecimal validar(TipoServico tipo, String nome, String categoria, String descricao,
                               BigDecimal valor, TipoCobranca cobranca) {
        if (tipo == null) {
            throw new IllegalArgumentException("Escolha o tipo: serviço ou extra de quarto.");
        }
        if (cobranca == null) {
            throw new IllegalArgumentException("Escolha a forma de cobrança: por unidade ou por noite.");
        }
        if (!Utils.estaPreenchido(nome)) {
            throw new IllegalArgumentException("Introduza o nome do serviço ou extra.");
        }
        if (nome.trim().length() > MAX_NOME) {
            throw new IllegalArgumentException("O nome não pode ter mais de " + MAX_NOME + " caracteres.");
        }
        if (categoria != null && categoria.trim().length() > MAX_CATEGORIA) {
            throw new IllegalArgumentException("A categoria não pode ter mais de " + MAX_CATEGORIA + " caracteres.");
        }
        if (descricao != null && descricao.trim().length() > MAX_DESCRICAO) {
            throw new IllegalArgumentException("A descrição não pode ter mais de " + MAX_DESCRICAO + " caracteres.");
        }
        if (valor == null) {
            throw new IllegalArgumentException("Introduza o preço.");
        }
        if (valor.signum() < 0) {
            throw new IllegalArgumentException("O preço não pode ser negativo.");
        }
        BigDecimal preco = valor.setScale(2, RoundingMode.HALF_UP);
        if (preco.compareTo(PRECO_MAXIMO) > 0) {
            throw new IllegalArgumentException("O preço não pode ser superior a "
                    + PRECO_MAXIMO.toPlainString().replace('.', ',') + ".");
        }
        return preco;
    }

    private void exigirNomeLivre(String nome, int idIgnorado) {
        boolean emUso = servicoDAO.listarTodos().stream()
                .anyMatch(s -> s.getId() != idIgnorado && s.getNome().equalsIgnoreCase(nome.trim()));
        if (emUso) {
            throw new IllegalArgumentException("Já existe um serviço ou extra com o nome '" + nome.trim() + "'.");
        }
    }

    private void exigirGestor() {
        Permissoes.exigirPerfil(sessao, "Apenas o gestor pode configurar os serviços e extras.", Perfil.GESTOR);
    }
}
