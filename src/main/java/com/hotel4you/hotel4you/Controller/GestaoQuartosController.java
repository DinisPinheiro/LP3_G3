package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.ConfiguracaoTipoQuartoDAO;
import com.hotel4you.hotel4you.DAO.EquipamentoDAO;
import com.hotel4you.hotel4you.DAO.QuartoDAO;
import com.hotel4you.hotel4you.DAO.StockDAO;
import com.hotel4you.hotel4you.Model.Artigo;
import com.hotel4you.hotel4you.DAO.SuplementoVistaDAO;
import com.hotel4you.hotel4you.Model.ConfiguracaoTipoQuarto;
import com.hotel4you.hotel4you.Model.Equipamento;
import com.hotel4you.hotel4you.Model.ProdutoQuarto;
import com.hotel4you.hotel4you.Model.Quarto;
import com.hotel4you.hotel4you.Model.enums.Perfil;
import com.hotel4you.hotel4you.Model.enums.TipoQuarto;
import com.hotel4you.hotel4you.Model.enums.Vista;
import com.hotel4you.hotel4you.Utils.Constantes;
import com.hotel4you.hotel4you.Utils.Utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Configuração e gestão dos quartos do hotel (S3-01): tipo, preço base, equipamentos e vista.
 * <p>
 * Só o gestor pode usar este controller. O número do quarto segue o enunciado (piso 2: 201 a 210,
 * piso 3: 301 a 310), por isso nunca há mais de 10 quartos por piso. O número e o piso não se
 * alteram depois de criado o quarto; para o retirar de uso, desativa-se.
 * <p>
 * S6-05: os equipamentos do quarto também se escolhem a partir dos produtos que existem em stock, com a
 * quantidade (ex.: 2 toalhas e 1 televisão). No check-in passam para a reserva e saem do stock disponível.
 * <p>
 * S6-09: o gestor configura uma só vez, por tipo de quarto (individual, duplo, família), o preço base e os
 * equipamentos. Os quartos herdam essa configuração e podem ter exceções (preço ou equipamentos próprios).
 * Quando a configuração de um tipo muda, os quartos que a herdam mudam com ela.
 */
public class GestaoQuartosController {

    /** Limites das colunas: dbo.Quarto (Local, PrecoBase DECIMAL(10,2)) e dbo.Equipamento (Nome). */
    private static final int MAX_LOCAL = 100;
    private static final int MAX_EQUIPAMENTO = 60;
    private static final BigDecimal PRECO_MAXIMO = new BigDecimal("99999999.99");

    private final QuartoDAO quartoDAO;
    private final EquipamentoDAO equipamentoDAO;
    /** Valores das vistas em vigor; {@code null} usa os valores iniciais de {@link Vista}. */
    private final SuplementoVistaDAO suplementoVistaDAO;
    /** Produtos do stock que podem ser equipamentos do quarto; {@code null} se não forem usados. */
    private final StockDAO stockDAO;
    /** Configuração por tipo de quarto (S6-09); {@code null} se não for usada. */
    private final ConfiguracaoTipoQuartoDAO configuracaoDAO;
    private final Sessao sessao;

    /**
     * @param quartoDAO      onde os quartos são guardados
     * @param equipamentoDAO catálogo de equipamentos
     * @param sessao         sessão com o utilizador autenticado
     */
    public GestaoQuartosController(QuartoDAO quartoDAO, EquipamentoDAO equipamentoDAO, Sessao sessao) {
        this(quartoDAO, equipamentoDAO, null, sessao);
    }

    /**
     * @param quartoDAO          onde os quartos são guardados
     * @param equipamentoDAO     catálogo de equipamentos
     * @param suplementoVistaDAO valores das vistas especiais, editáveis pelo gestor (S4-vistas)
     * @param sessao             sessão com o utilizador autenticado
     */
    public GestaoQuartosController(QuartoDAO quartoDAO, EquipamentoDAO equipamentoDAO,
                                   SuplementoVistaDAO suplementoVistaDAO, Sessao sessao) {
        this(quartoDAO, equipamentoDAO, suplementoVistaDAO, null, sessao);
    }

    /**
     * @param quartoDAO          onde os quartos são guardados
     * @param equipamentoDAO     catálogo de equipamentos
     * @param suplementoVistaDAO valores das vistas especiais, editáveis pelo gestor (S4-vistas)
     * @param stockDAO           produtos do stock que podem ser equipamentos do quarto (S6-05)
     * @param sessao             sessão com o utilizador autenticado
     */
    public GestaoQuartosController(QuartoDAO quartoDAO, EquipamentoDAO equipamentoDAO,
                                   SuplementoVistaDAO suplementoVistaDAO, StockDAO stockDAO, Sessao sessao) {
        this(quartoDAO, equipamentoDAO, suplementoVistaDAO, stockDAO, null, sessao);
    }

    /**
     * @param quartoDAO          onde os quartos são guardados
     * @param equipamentoDAO     catálogo de equipamentos
     * @param suplementoVistaDAO valores das vistas especiais, editáveis pelo gestor (S4-vistas)
     * @param stockDAO           produtos do stock que podem ser equipamentos do quarto (S6-05)
     * @param configuracaoDAO    configuração do preço base e dos equipamentos por tipo de quarto (S6-09)
     * @param sessao             sessão com o utilizador autenticado
     */
    public GestaoQuartosController(QuartoDAO quartoDAO, EquipamentoDAO equipamentoDAO,
                                   SuplementoVistaDAO suplementoVistaDAO, StockDAO stockDAO,
                                   ConfiguracaoTipoQuartoDAO configuracaoDAO, Sessao sessao) {
        this.configuracaoDAO = configuracaoDAO;
        this.quartoDAO = Objects.requireNonNull(quartoDAO);
        this.equipamentoDAO = Objects.requireNonNull(equipamentoDAO);
        this.suplementoVistaDAO = suplementoVistaDAO;
        this.stockDAO = stockDAO;
        this.sessao = Objects.requireNonNull(sessao);
    }

    /**
     * @return os produtos que existem em stock, para escolher os equipamentos do quarto (S6-05)
     * @throws SecurityException se o utilizador não for gestor
     * @throws com.hotel4you.hotel4you.DAO.DAOException se não for possível ler
     */
    public List<Artigo> consultarProdutosStock() {
        exigirGestor();
        return stockDAO == null ? List.of() : stockDAO.listarArtigos();
    }

    /**
     * Consulta todos os quartos, por número.
     *
     * @return os quartos dos pisos 2 e 3
     * @throws SecurityException se o utilizador não for gestor
     * @throws com.hotel4you.hotel4you.DAO.DAOException se não for possível ler
     */
    public List<Quarto> consultarQuartos() {
        exigirGestor();
        return quartoDAO.listarTodos();
    }

    /**
     * @return o catálogo de equipamentos que se podem associar aos quartos
     * @throws SecurityException se o utilizador não for gestor
     * @throws com.hotel4you.hotel4you.DAO.DAOException se não for possível ler
     */
    public List<Equipamento> consultarEquipamentos() {
        exigirGestor();
        return equipamentoDAO.listarTodos();
    }

    /**
     * Acrescenta um equipamento novo ao catálogo.
     *
     * @param nome nome do equipamento (obrigatório, sem repetir)
     * @return o equipamento guardado
     * @throws IllegalArgumentException se o nome estiver vazio ou já existir
     * @throws SecurityException        se o utilizador não for gestor
     * @throws com.hotel4you.hotel4you.DAO.DAOException se não for possível guardar
     */
    public Equipamento criarEquipamento(String nome) {
        exigirGestor();
        if (!Utils.estaPreenchido(nome)) {
            throw new IllegalArgumentException("Introduza o nome do equipamento.");
        }
        if (nome.trim().length() > MAX_EQUIPAMENTO) {
            throw new IllegalArgumentException("O nome do equipamento não pode ter mais de " + MAX_EQUIPAMENTO + " caracteres.");
        }
        boolean existe = equipamentoDAO.listarTodos().stream()
                .anyMatch(equipamento -> equipamento.getNome().equalsIgnoreCase(nome.trim()));
        if (existe) {
            throw new IllegalArgumentException("O equipamento '" + nome.trim() + "' já existe.");
        }
        return equipamentoDAO.inserir(new Equipamento(0, nome));
    }

    /**
     * Cria um quarto novo, ativo. O piso é o do número (ex.: 205 é do piso 2).
     *
     * @param numero       número do quarto (201 a 210 ou 301 a 310)
     * @param local        descrição da localização; pode ser vazia
     * @param tipo         tipo de quarto
     * @param precoBase    preço base por noite, não negativo
     * @param vista        vista do quarto
     * @param equipamentos equipamentos do catálogo a associar ao quarto
     * @return o quarto guardado, já com identificador
     * @throws IllegalArgumentException se algum dado for inválido ou o número já existir
     * @throws SecurityException        se o utilizador não for gestor
     * @throws com.hotel4you.hotel4you.DAO.DAOException se não for possível guardar
     */
    public Quarto criarQuarto(int numero, String local, TipoQuarto tipo, BigDecimal precoBase,
                              Vista vista, List<Equipamento> equipamentos) {
        return criarQuarto(numero, local, tipo, precoBase, vista, equipamentos, List.of());
    }

    /**
     * Cria um quarto novo, ativo, com equipamentos do catálogo e produtos do stock (S6-05).
     *
     * @param numero       número do quarto (201 a 210 ou 301 a 310)
     * @param local        descrição da localização; pode ser vazia
     * @param tipo         tipo de quarto
     * @param precoBase    preço base por noite, não negativo
     * @param vista        vista do quarto
     * @param equipamentos equipamentos do catálogo a associar ao quarto
     * @param produtos     produtos do stock que o quarto tem, com a quantidade
     * @return o quarto guardado, já com identificador
     * @throws IllegalArgumentException se algum dado for inválido, o número já existir ou um produto
     *                                  não existir em stock
     * @throws SecurityException        se o utilizador não for gestor
     * @throws com.hotel4you.hotel4you.DAO.DAOException se não for possível guardar
     */
    public Quarto criarQuarto(int numero, String local, TipoQuarto tipo, BigDecimal precoBase,
                              Vista vista, List<Equipamento> equipamentos, List<ProdutoQuarto> produtos) {
        exigirGestor();
        int piso = pisoDoNumero(numero);
        if (quartoDAO.procurarPorNumero(numero).isPresent()) {
            throw new IllegalArgumentException("Já existe o quarto " + numero + ".");
        }
        BigDecimal preco = validarDados(local, tipo, precoBase, vista);
        List<Equipamento> escolhidos = validarEquipamentos(equipamentos);

        Quarto quarto = new Quarto(0, numero, piso, local, tipo, preco, vista, true);
        quarto.setSuplementoVista(obterSuplemento(vista));
        escolhidos.forEach(quarto::adicionarEquipamento);
        validarProdutos(produtos).forEach(quarto::definirProduto);
        return quartoDAO.inserir(quarto);
    }

    /**
     * Edita um quarto já guardado (o número e o piso não mudam).
     *
     * @param id           identificador do quarto
     * @param local        descrição da localização; pode ser vazia
     * @param tipo         tipo de quarto
     * @param precoBase    preço base por noite, não negativo
     * @param vista        vista do quarto
     * @param ativo        se o quarto está disponível para reservas
     * @param equipamentos equipamentos do catálogo que o quarto passa a ter
     * @throws IllegalArgumentException se o quarto não existir ou algum dado for inválido
     * @throws SecurityException        se o utilizador não for gestor
     * @throws com.hotel4you.hotel4you.DAO.DAOException se não for possível guardar
     */
    public void editarQuarto(int id, String local, TipoQuarto tipo, BigDecimal precoBase,
                             Vista vista, boolean ativo, List<Equipamento> equipamentos) {
        editarQuarto(id, local, tipo, precoBase, vista, ativo, equipamentos, null);
    }

    /**
     * Edita um quarto já guardado, incluindo os produtos do stock que tem (S6-05).
     *
     * @param id           identificador do quarto
     * @param local        descrição da localização; pode ser vazia
     * @param tipo         tipo de quarto
     * @param precoBase    preço base por noite, não negativo
     * @param vista        vista do quarto
     * @param ativo        se o quarto está disponível para reservas
     * @param equipamentos equipamentos do catálogo que o quarto passa a ter
     * @param produtos     produtos do stock que o quarto passa a ter; {@code null} mantém os atuais
     * @throws IllegalArgumentException se o quarto não existir, algum dado for inválido ou um produto
     *                                  não existir em stock
     * @throws SecurityException        se o utilizador não for gestor
     * @throws com.hotel4you.hotel4you.DAO.DAOException se não for possível guardar
     */
    public void editarQuarto(int id, String local, TipoQuarto tipo, BigDecimal precoBase,
                             Vista vista, boolean ativo, List<Equipamento> equipamentos,
                             List<ProdutoQuarto> produtos) {
        exigirGestor();
        Quarto quarto = quartoDAO.procurarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("O quarto selecionado não existe."));
        BigDecimal preco = validarDados(local, tipo, precoBase, vista);
        List<Equipamento> escolhidos = validarEquipamentos(equipamentos);
        List<ProdutoQuarto> produtosValidos = produtos == null ? null : validarProdutos(produtos);

        quarto.setLocal(local);
        quarto.setTipo(tipo);
        quarto.personalizarPreco(preco);
        quarto.setVista(vista);
        quarto.setSuplementoVista(obterSuplemento(vista));
        quarto.setAtivo(ativo);
        new ArrayList<>(quarto.getEquipamentos()).forEach(quarto::removerEquipamento);
        escolhidos.forEach(quarto::adicionarEquipamento);
        if (produtosValidos != null) {
            quarto.limparProdutos();
            produtosValidos.forEach(quarto::definirProduto);
        }
        quarto.setEquipamentosPersonalizados(true);
        quartoDAO.atualizar(quarto);
    }

    // CONFIGURAÇÃO POR TIPO DE QUARTO (S6-09)

    /**
     * @param tipo tipo de quarto
     * @return a configuração do tipo, ou vazio se o gestor ainda não a definiu
     * @throws SecurityException se o utilizador não for gestor
     * @throws com.hotel4you.hotel4you.DAO.DAOException se não for possível ler
     */
    public Optional<ConfiguracaoTipoQuarto> consultarConfiguracaoTipo(TipoQuarto tipo) {
        exigirGestor();
        if (tipo == null) {
            throw new IllegalArgumentException("Escolha o tipo de quarto.");
        }
        return configuracaoDAO == null ? Optional.empty() : configuracaoDAO.procurar(tipo);
    }

    /**
     * Define o preço base e os equipamentos de um tipo de quarto. Os quartos desse tipo que herdam a
     * configuração passam a ter os valores novos; os que têm exceções ficam como estão.
     *
     * @param tipo         tipo de quarto (individual, duplo ou família)
     * @param precoBase    preço base por noite, não negativo
     * @param equipamentos equipamentos do catálogo dos quartos do tipo
     * @param produtos     produtos do stock dos quartos do tipo, com a quantidade
     * @return o número de quartos que mudaram por herdarem a configuração
     * @throws IllegalArgumentException se algum dado for inválido ou um equipamento ou produto não existir
     * @throws SecurityException        se o utilizador não for gestor
     * @throws com.hotel4you.hotel4you.DAO.DAOException se não for possível guardar
     */
    public int guardarConfiguracaoTipo(TipoQuarto tipo, BigDecimal precoBase, List<Equipamento> equipamentos,
                                       List<ProdutoQuarto> produtos) {
        exigirGestor();
        if (configuracaoDAO == null) {
            throw new IllegalStateException("A configuração por tipo de quarto não está disponível.");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("Escolha o tipo de quarto.");
        }
        BigDecimal preco = validarPreco(precoBase);
        ConfiguracaoTipoQuarto configuracao = new ConfiguracaoTipoQuarto(tipo, preco,
                validarEquipamentos(equipamentos), validarProdutos(produtos));
        configuracaoDAO.guardar(configuracao);

        int alterados = 0;
        for (Quarto quarto : quartoDAO.listarTodos()) {
            if (quarto.getTipo() == tipo && quarto.aplicarConfiguracao(configuracao)) {
                quartoDAO.atualizar(quarto);
                alterados++;
            }
        }
        return alterados;
    }

    /**
     * Cria um quarto novo, ativo, que herda do seu tipo o preço base e os equipamentos, a não ser que se
     * indiquem exceções.
     *
     * @param numero               número do quarto (201 a 210 ou 301 a 310)
     * @param local                descrição da localização; pode ser vazia
     * @param tipo                 tipo de quarto (a configuração do tipo dá o preço e os equipamentos)
     * @param vista                vista do quarto
     * @param precoPersonalizado   preço base próprio (exceção), ou {@code null} para herdar o do tipo
     * @param equipamentos         equipamentos próprios (exceção), ou {@code null} para herdar os do tipo
     * @param produtos             produtos do stock próprios; só se usam com {@code equipamentos} diferente de {@code null}
     * @return o quarto guardado, já com identificador
     * @throws IllegalArgumentException se algum dado for inválido, o número já existir ou o quarto herdar de um
     *                                  tipo que ainda não foi configurado
     * @throws SecurityException        se o utilizador não for gestor
     * @throws com.hotel4you.hotel4you.DAO.DAOException se não for possível guardar
     */
    public Quarto criarQuartoDoTipo(int numero, String local, TipoQuarto tipo, Vista vista,
                                    BigDecimal precoPersonalizado, List<Equipamento> equipamentos,
                                    List<ProdutoQuarto> produtos) {
        exigirGestor();
        int piso = pisoDoNumero(numero);
        if (quartoDAO.procurarPorNumero(numero).isPresent()) {
            throw new IllegalArgumentException("Já existe o quarto " + numero + ".");
        }
        validarLocalTipoVista(local, tipo, vista);
        Heranca heranca = prepararHeranca(tipo, precoPersonalizado, equipamentos, produtos);
        Quarto quarto = new Quarto(0, numero, piso, local, tipo, BigDecimal.ZERO, vista, true);
        quarto.setSuplementoVista(obterSuplemento(vista));
        heranca.aplicarA(quarto);
        return quartoDAO.inserir(quarto);
    }

    /**
     * Edita um quarto guardado, indicando o que herda do tipo e o que é exceção. Se o tipo do quarto mudar,
     * o que ele herda passa a ser o do tipo novo.
     *
     * @param id                 identificador do quarto
     * @param local              descrição da localização; pode ser vazia
     * @param tipo               tipo de quarto
     * @param vista              vista do quarto
     * @param ativo              se o quarto está disponível para reservas
     * @param precoPersonalizado preço base próprio (exceção), ou {@code null} para herdar o do tipo
     * @param equipamentos       equipamentos próprios (exceção), ou {@code null} para herdar os do tipo
     * @param produtos           produtos do stock próprios; só se usam com {@code equipamentos} diferente de {@code null}
     * @throws IllegalArgumentException se o quarto não existir, algum dado for inválido ou o quarto herdar de um
     *                                  tipo que ainda não foi configurado
     * @throws SecurityException        se o utilizador não for gestor
     * @throws com.hotel4you.hotel4you.DAO.DAOException se não for possível guardar
     */
    public void editarQuartoDoTipo(int id, String local, TipoQuarto tipo, Vista vista, boolean ativo,
                                   BigDecimal precoPersonalizado, List<Equipamento> equipamentos,
                                   List<ProdutoQuarto> produtos) {
        exigirGestor();
        Quarto quarto = quartoDAO.procurarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("O quarto selecionado não existe."));
        validarLocalTipoVista(local, tipo, vista);
        Heranca heranca = prepararHeranca(tipo, precoPersonalizado, equipamentos, produtos);
        BigDecimal suplemento = obterSuplemento(vista);
        quarto.setLocal(local);
        quarto.setTipo(tipo);
        quarto.setVista(vista);
        quarto.setSuplementoVista(suplemento);
        quarto.setAtivo(ativo);
        heranca.aplicarA(quarto);
        quartoDAO.atualizar(quarto);
    }

    /**
     * O que um quarto herda do tipo e o que é exceção, já validado. Só se aplica ao quarto depois de tudo
     * estar validado, para um erro não deixar o quarto a meio.
     */
    private record Heranca(ConfiguracaoTipoQuarto configuracao, BigDecimal preco,
                           List<Equipamento> equipamentos, List<ProdutoQuarto> produtos) {

        void aplicarA(Quarto quarto) {
            if (preco == null) {
                quarto.herdarPreco(configuracao);
            } else {
                quarto.personalizarPreco(preco);
            }
            if (equipamentos == null) {
                quarto.herdarEquipamentos(configuracao);
            } else {
                quarto.personalizarEquipamentos(equipamentos, produtos);
            }
        }
    }

    /** Valida as exceções e procura a configuração do tipo se o quarto herdar alguma coisa. */
    private Heranca prepararHeranca(TipoQuarto tipo, BigDecimal precoPersonalizado,
                                    List<Equipamento> equipamentos, List<ProdutoQuarto> produtos) {
        BigDecimal preco = precoPersonalizado == null ? null : validarPreco(precoPersonalizado);
        List<Equipamento> proprios = equipamentos == null ? null : validarEquipamentos(equipamentos);
        List<ProdutoQuarto> produtosProprios = equipamentos == null ? null : validarProdutos(produtos);
        ConfiguracaoTipoQuarto configuracao = null;
        if (preco == null || proprios == null) {
            configuracao = consultarConfiguracaoTipo(tipo).orElseThrow(() -> new IllegalArgumentException(
                    "O tipo " + tipo.getNome() + " ainda não tem configuração: defina-a em "
                            + "'Configuração por tipo de quarto' ou indique o preço e os equipamentos do quarto."));
        }
        return new Heranca(configuracao, preco, proprios, produtosProprios);
    }

    private void validarLocalTipoVista(String local, TipoQuarto tipo, Vista vista) {
        if (local != null && local.trim().length() > MAX_LOCAL) {
            throw new IllegalArgumentException("A localização não pode ter mais de " + MAX_LOCAL + " caracteres.");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("Escolha o tipo de quarto.");
        }
        if (vista == null) {
            throw new IllegalArgumentException("Escolha a vista do quarto.");
        }
    }

    /**
     * Calcula o preço por noite (preço base mais o valor da vista em vigor), para mostrar no ecrã
     * antes de guardar.
     *
     * @param precoBase preço base por noite
     * @param vista     vista do quarto
     * @return o preço por noite
     * @throws com.hotel4you.hotel4you.DAO.DAOException se não for possível ler o valor da vista
     */
    public BigDecimal calcularPrecoNoite(BigDecimal precoBase, Vista vista) {
        return Quarto.calcularPrecoNoite(precoBase, obterSuplemento(vista));
    }

    /**
     * @param vista vista do quarto
     * @return o valor da vista em vigor (o configurado pelo gestor; o inicial se nunca foi alterado)
     * @throws com.hotel4you.hotel4you.DAO.DAOException se não for possível ler
     */
    public BigDecimal obterSuplemento(Vista vista) {
        if (suplementoVistaDAO == null) {
            return vista.getSuplemento();
        }
        return suplementoVistaDAO.listar().getOrDefault(vista, vista.getSuplemento());
    }

    /**
     * @param numero número do quarto
     * @return o piso a que o número pertence (ex.: 205 é do piso 2)
     * @throws IllegalArgumentException se o número não for de nenhum quarto do hotel
     */
    public static int pisoDoNumero(int numero) {
        int piso = numero / 100;
        int posicao = numero % 100;
        if (!Constantes.pisoDeDormidas(piso) || posicao < 1 || posicao > Constantes.QUARTOS_POR_PISO) {
            throw new IllegalArgumentException(mensagemNumeroInvalido());
        }
        return piso;
    }

    private static String mensagemNumeroInvalido() {
        StringBuilder texto = new StringBuilder("Número de quarto inválido: use ");
        for (int piso = Constantes.PRIMEIRO_PISO_DORMIDAS; piso <= Constantes.ULTIMO_PISO_DORMIDAS; piso++) {
            if (piso > Constantes.PRIMEIRO_PISO_DORMIDAS) {
                texto.append(" ou ");
            }
            texto.append(piso * 100 + 1).append(" a ").append(piso * 100 + Constantes.QUARTOS_POR_PISO)
                    .append(" (piso ").append(piso).append(")");
        }
        return texto.append(".").toString();
    }

    private BigDecimal validarDados(String local, TipoQuarto tipo, BigDecimal precoBase, Vista vista) {
        if (local != null && local.trim().length() > MAX_LOCAL) {
            throw new IllegalArgumentException("A localização não pode ter mais de " + MAX_LOCAL + " caracteres.");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("Escolha o tipo de quarto.");
        }
        if (vista == null) {
            throw new IllegalArgumentException("Escolha a vista do quarto.");
        }
        return validarPreco(precoBase);
    }

    private BigDecimal validarPreco(BigDecimal precoBase) {
        if (precoBase == null) {
            throw new IllegalArgumentException("Introduza o preço base por noite.");
        }
        if (precoBase.signum() < 0) {
            throw new IllegalArgumentException("O preço base não pode ser negativo.");
        }
        BigDecimal preco = precoBase.setScale(2, RoundingMode.HALF_UP);
        if (preco.compareTo(PRECO_MAXIMO) > 0) {
            throw new IllegalArgumentException("O preço base não pode ser superior a "
                    + PRECO_MAXIMO.toPlainString().replace('.', ',') + ".");
        }
        return preco;
    }

    /** Confirma que os equipamentos existem no catálogo e retira repetidos. */
    private List<Equipamento> validarEquipamentos(List<Equipamento> pedidos) {
        Map<Integer, Equipamento> validos = new LinkedHashMap<>();
        if (pedidos != null) {
            for (Equipamento pedido : pedidos) {
                Equipamento existente = equipamentoDAO.procurarPorId(pedido.getId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "O equipamento '" + pedido.getNome() + "' não existe no catálogo."));
                validos.put(existente.getId(), existente);
            }
        }
        return new ArrayList<>(validos.values());
    }

    /**
     * Confirma que os produtos existem em stock (S6-05). O mesmo produto escolhido duas vezes fica com
     * a última quantidade.
     */
    private List<ProdutoQuarto> validarProdutos(List<ProdutoQuarto> pedidos) {
        Map<Integer, ProdutoQuarto> validos = new LinkedHashMap<>();
        if (pedidos == null || pedidos.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Integer, Artigo> stock = new LinkedHashMap<>();
        for (Artigo artigo : consultarProdutosStock()) {
            stock.put(artigo.getId(), artigo);
        }
        for (ProdutoQuarto pedido : pedidos) {
            Artigo artigo = stock.get(pedido.getArtigo().getId());
            if (artigo == null) {
                throw new IllegalArgumentException("O produto '" + pedido.getArtigo().getNome()
                        + "' não existe em stock.");
            }
            artigo.exigirUnidadePrincipal();   // S7-02: os quartos contam na unidade principal
            validos.put(artigo.getId(), new ProdutoQuarto(artigo, pedido.getQuantidade()));
        }
        return new ArrayList<>(validos.values());
    }

    private void exigirGestor() {
        Permissoes.exigirPerfil(sessao, "Apenas o gestor pode configurar os quartos.", Perfil.GESTOR);
    }
}
