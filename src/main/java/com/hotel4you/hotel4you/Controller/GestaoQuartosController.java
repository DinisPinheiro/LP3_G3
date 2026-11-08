package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.EquipamentoDAO;
import com.hotel4you.hotel4you.DAO.QuartoDAO;
import com.hotel4you.hotel4you.Model.Equipamento;
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

/**
 * Configuração e gestão dos quartos do hotel (S3-01): tipo, preço base, equipamentos e vista.
 * <p>
 * Só o gestor pode usar este controller. O número do quarto segue o enunciado (piso 2: 201 a 210,
 * piso 3: 301 a 310), por isso nunca há mais de 10 quartos por piso. O número e o piso não se
 * alteram depois de criado o quarto; para o retirar de uso, desativa-se.
 * <p>
 * Apoio de IA: Claude (Anthropic), revisto pela equipa.
 */
public class GestaoQuartosController {

    /** Limites das colunas: dbo.Quarto (Local, PrecoBase DECIMAL(10,2)) e dbo.Equipamento (Nome). */
    private static final int MAX_LOCAL = 100;
    private static final int MAX_EQUIPAMENTO = 60;
    private static final BigDecimal PRECO_MAXIMO = new BigDecimal("99999999.99");

    private final QuartoDAO quartoDAO;
    private final EquipamentoDAO equipamentoDAO;
    private final Sessao sessao;

    /**
     * @param quartoDAO      onde os quartos são guardados
     * @param equipamentoDAO catálogo de equipamentos
     * @param sessao         sessão com o utilizador autenticado
     */
    public GestaoQuartosController(QuartoDAO quartoDAO, EquipamentoDAO equipamentoDAO, Sessao sessao) {
        this.quartoDAO = Objects.requireNonNull(quartoDAO);
        this.equipamentoDAO = Objects.requireNonNull(equipamentoDAO);
        this.sessao = Objects.requireNonNull(sessao);
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
        exigirGestor();
        int piso = pisoDoNumero(numero);
        if (quartoDAO.procurarPorNumero(numero).isPresent()) {
            throw new IllegalArgumentException("Já existe o quarto " + numero + ".");
        }
        BigDecimal preco = validarDados(local, tipo, precoBase, vista);
        List<Equipamento> escolhidos = validarEquipamentos(equipamentos);

        Quarto quarto = new Quarto(0, numero, piso, local, tipo, preco, vista, true);
        escolhidos.forEach(quarto::adicionarEquipamento);
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
        exigirGestor();
        Quarto quarto = quartoDAO.procurarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("O quarto selecionado não existe."));
        BigDecimal preco = validarDados(local, tipo, precoBase, vista);
        List<Equipamento> escolhidos = validarEquipamentos(equipamentos);

        quarto.setLocal(local);
        quarto.setTipo(tipo);
        quarto.setPrecoBase(preco);
        quarto.setVista(vista);
        quarto.setAtivo(ativo);
        new ArrayList<>(quarto.getEquipamentos()).forEach(quarto::removerEquipamento);
        escolhidos.forEach(quarto::adicionarEquipamento);
        quartoDAO.atualizar(quarto);
    }

    /**
     * Calcula o preço por noite (preço base mais o suplemento da vista), para mostrar no ecrã
     * antes de guardar.
     *
     * @param precoBase preço base por noite
     * @param vista     vista do quarto
     * @return o preço por noite
     */
    public BigDecimal calcularPrecoNoite(BigDecimal precoBase, Vista vista) {
        return Quarto.calcularPrecoNoite(precoBase, vista);
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

    private void exigirGestor() {
        Permissoes.exigirPerfil(sessao, "Apenas o gestor pode configurar os quartos.", Perfil.GESTOR);
    }
}
