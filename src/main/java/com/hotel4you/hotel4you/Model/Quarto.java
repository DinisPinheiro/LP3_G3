package com.hotel4you.hotel4you.Model;

import com.hotel4you.hotel4you.Utils.Constantes;
import com.hotel4you.hotel4you.Model.enums.TipoQuarto;
import com.hotel4you.hotel4you.Model.enums.Vista;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Quarto do hotel. Existem 20 quartos nos pisos 2 e 3 (10 por piso); o número
 * segue o padrão do enunciado (ex.: 202 e 301).
 * <p>
 * S6-09: o preço base e os equipamentos podem vir da configuração do tipo de quarto
 * ({@link ConfiguracaoTipoQuarto}): o quarto herda-os e só deixa de os herdar se o gestor lhe der uma
 * exceção ({@link #personalizarPreco(BigDecimal)}, {@link #personalizarEquipamentos(List, List)}). Um quarto
 * criado com valores próprios tem as duas exceções.
 */
public class Quarto {

    private int id;
    private final int numero;
    private final int piso;
    private String local;
    private TipoQuarto tipo;
    private BigDecimal precoBase;
    private Vista vista;
    /** Valor da vista em vigor (o configurado pelo gestor); por omissão, o valor inicial da vista. */
    private BigDecimal suplementoVista;
    private boolean ativo;
    private final List<Equipamento> equipamentos = new ArrayList<>();
    /** Equipamentos do quarto escolhidos a partir dos produtos do stock, com a quantidade (S6-05). */
    private final List<ProdutoQuarto> produtos = new ArrayList<>();
    /** {@code true} se o preço base é uma exceção ao do tipo; {@code false} se é o do tipo (S6-09). */
    private boolean precoPersonalizado = true;
    /** {@code true} se os equipamentos e produtos são uma exceção aos do tipo (S6-09). */
    private boolean equipamentosPersonalizados = true;

    /**
     * @param id        identificador (0 se ainda não foi guardado)
     * @param numero    número do quarto (ex.: 202)
     * @param piso      piso de dormidas (2 ou 3)
     * @param local     descrição da localização (ex.: "Ala nascente"); pode ser vazia
     * @param tipo      tipo de quarto
     * @param precoBase preço base por noite, não negativo
     * @param vista     vista do quarto
     * @param ativo     se está disponível para reservas
     * @throws IllegalArgumentException se o piso não tiver quartos, o número for inválido ou o preço for negativo
     */
    public Quarto(int id, int numero, int piso, String local, TipoQuarto tipo,
                  BigDecimal precoBase, Vista vista, boolean ativo) {
        if (!Constantes.pisoDeDormidas(piso)) {
            throw new IllegalArgumentException("O piso " + piso + " não tem quartos (só os pisos 2 e 3).");
        }
        if (numero <= 0) {
            throw new IllegalArgumentException("O número do quarto deve ser positivo.");
        }
        this.id = id;
        this.numero = numero;
        this.piso = piso;
        this.local = local == null ? "" : local.trim();
        this.tipo = Objects.requireNonNull(tipo, "O tipo de quarto é obrigatório.");
        this.precoBase = validarPreco(precoBase);
        this.vista = Objects.requireNonNull(vista, "A vista é obrigatória.");
        this.suplementoVista = vista.getSuplemento();
        this.ativo = ativo;
    }

    /**
     * Preço de uma noite: preço base mais o valor da vista em vigor.
     * Consumos (bebidas, lavandaria) são somados na {@link Reserva}.
     *
     * @return preço por noite
     */
    public BigDecimal precoNoite() {
        return calcularPrecoNoite(precoBase, suplementoVista);
    }

    /**
     * Calcula o preço de uma noite: preço base mais o suplemento da vista.
     * Existe como método estático para o ecrã de configuração mostrar o preço antes de guardar o quarto.
     *
     * @param precoBase preço base por noite
     * @param vista     vista do quarto
     * @return preço por noite
     */
    public static BigDecimal calcularPrecoNoite(BigDecimal precoBase, Vista vista) {
        return precoBase.add(vista.getSuplemento());
    }

    /**
     * Calcula o preço de uma noite com um valor de vista já conhecido (o configurado pelo gestor).
     *
     * @param precoBase  preço base por noite
     * @param suplemento valor da vista a somar
     * @return preço por noite
     */
    public static BigDecimal calcularPrecoNoite(BigDecimal precoBase, BigDecimal suplemento) {
        return precoBase.add(suplemento);
    }

    /**
     * @param equipamento equipamento a colocar no quarto
     */
    public void adicionarEquipamento(Equipamento equipamento) {
        equipamentos.add(Objects.requireNonNull(equipamento, "O equipamento é obrigatório."));
    }

    /**
     * @param equipamento equipamento a retirar do quarto
     * @return {@code true} se o quarto o tinha
     */
    public boolean removerEquipamento(Equipamento equipamento) {
        return equipamentos.remove(equipamento);
    }

    /** @return lista só de leitura dos equipamentos do quarto */
    public List<Equipamento> getEquipamentos() {
        return Collections.unmodifiableList(equipamentos);
    }

    /**
     * Põe um produto do stock no quarto. Se o quarto já tiver esse produto, a quantidade passa a ser
     * a indicada (não soma).
     *
     * @param produto produto do stock e quantidade
     */
    public void definirProduto(ProdutoQuarto produto) {
        Objects.requireNonNull(produto, "O produto é obrigatório.");
        removerProduto(produto.getArtigo().getId());
        produtos.add(produto);
    }

    /**
     * @param idArtigo identificador do produto do stock
     * @return {@code true} se o quarto tinha o produto
     */
    public boolean removerProduto(int idArtigo) {
        return produtos.removeIf(produto -> produto.getArtigo().getId() == idArtigo);
    }

    /** Retira todos os produtos do stock do quarto. */
    public void limparProdutos() {
        produtos.clear();
    }

    // CONFIGURAÇÃO POR TIPO DE QUARTO (S6-09)

    /** @return {@code true} se o preço base deste quarto é uma exceção ao preço do seu tipo */
    public boolean isPrecoPersonalizado() {
        return precoPersonalizado;
    }

    /** @return {@code true} se os equipamentos e produtos deste quarto são uma exceção aos do seu tipo */
    public boolean isEquipamentosPersonalizados() {
        return equipamentosPersonalizados;
    }

    /** Usado pelo DAO ao ler o quarto: indica se o preço é uma exceção. */
    public void setPrecoPersonalizado(boolean precoPersonalizado) {
        this.precoPersonalizado = precoPersonalizado;
    }

    /** Usado pelo DAO ao ler o quarto: indica se os equipamentos e produtos são uma exceção. */
    public void setEquipamentosPersonalizados(boolean equipamentosPersonalizados) {
        this.equipamentosPersonalizados = equipamentosPersonalizados;
    }

    /**
     * Dá ao quarto um preço base próprio (exceção ao do tipo).
     *
     * @param precoBase preço base por noite, não negativo
     */
    public void personalizarPreco(BigDecimal precoBase) {
        setPrecoBase(precoBase);
        this.precoPersonalizado = true;
    }

    /**
     * Dá ao quarto equipamentos e produtos próprios (exceção aos do tipo). Substitui os atuais.
     *
     * @param novosEquipamentos equipamentos do catálogo
     * @param novosProdutos     produtos do stock com a quantidade
     */
    public void personalizarEquipamentos(List<Equipamento> novosEquipamentos, List<ProdutoQuarto> novosProdutos) {
        substituirEquipamentos(novosEquipamentos, novosProdutos);
        this.equipamentosPersonalizados = true;
    }

    /**
     * Faz o quarto voltar a herdar o preço base do seu tipo.
     *
     * @param configuracao configuração do tipo do quarto
     * @throws IllegalArgumentException se a configuração for de outro tipo de quarto
     */
    public void herdarPreco(ConfiguracaoTipoQuarto configuracao) {
        exigirDoMesmoTipo(configuracao);
        setPrecoBase(configuracao.getPrecoBase());
        this.precoPersonalizado = false;
    }

    /**
     * Faz o quarto voltar a herdar os equipamentos e produtos do seu tipo.
     *
     * @param configuracao configuração do tipo do quarto
     * @throws IllegalArgumentException se a configuração for de outro tipo de quarto
     */
    public void herdarEquipamentos(ConfiguracaoTipoQuarto configuracao) {
        exigirDoMesmoTipo(configuracao);
        substituirEquipamentos(configuracao.getEquipamentos(), configuracao.getProdutos());
        this.equipamentosPersonalizados = false;
    }

    /**
     * Aplica a configuração do tipo às partes que o quarto herda; as exceções ficam como estão.
     * Usa-se quando o gestor muda a configuração do tipo ou o tipo do quarto.
     *
     * @param configuracao configuração do tipo do quarto
     * @return {@code true} se alguma coisa mudou no quarto
     * @throws IllegalArgumentException se a configuração for de outro tipo de quarto
     */
    public boolean aplicarConfiguracao(ConfiguracaoTipoQuarto configuracao) {
        exigirDoMesmoTipo(configuracao);
        boolean mudou = false;
        if (!precoPersonalizado && precoBase.compareTo(configuracao.getPrecoBase()) != 0) {
            herdarPreco(configuracao);
            mudou = true;
        }
        if (!equipamentosPersonalizados
                && !mesmosEquipamentos(configuracao.getEquipamentos(), configuracao.getProdutos())) {
            herdarEquipamentos(configuracao);
            mudou = true;
        }
        return mudou;
    }

    private void exigirDoMesmoTipo(ConfiguracaoTipoQuarto configuracao) {
        Objects.requireNonNull(configuracao, "A configuração do tipo é obrigatória.");
        if (configuracao.getTipo() != tipo) {
            throw new IllegalArgumentException("A configuração é do tipo " + configuracao.getTipo().getNome()
                    + " e o quarto é do tipo " + tipo.getNome() + ".");
        }
    }

    private void substituirEquipamentos(List<Equipamento> novosEquipamentos, List<ProdutoQuarto> novosProdutos) {
        List<Equipamento> eq = novosEquipamentos == null ? List.of() : new ArrayList<>(novosEquipamentos);
        List<ProdutoQuarto> pr = novosProdutos == null ? List.of() : new ArrayList<>(novosProdutos);
        equipamentos.clear();
        eq.forEach(this::adicionarEquipamento);
        produtos.clear();
        pr.forEach(this::definirProduto);
    }

    private boolean mesmosEquipamentos(List<Equipamento> outrosEquipamentos, List<ProdutoQuarto> outrosProdutos) {
        if (equipamentos.size() != outrosEquipamentos.size() || produtos.size() != outrosProdutos.size()) {
            return false;
        }
        for (Equipamento equipamento : outrosEquipamentos) {
            if (equipamentos.stream().noneMatch(e -> e.getId() == equipamento.getId())) {
                return false;
            }
        }
        for (ProdutoQuarto produto : outrosProdutos) {
            if (produtos.stream().noneMatch(p -> p.getArtigo().getId() == produto.getArtigo().getId()
                    && p.getQuantidade() == produto.getQuantidade())) {
                return false;
            }
        }
        return true;
    }

    /** @return lista só de leitura dos produtos do stock que o quarto tem (S6-05) */
    public List<ProdutoQuarto> getProdutos() {
        return Collections.unmodifiableList(produtos);
    }

    public int getId() {
        return id;
    }

    /** Usado pelo DAO ao guardar o quarto pela primeira vez. */
    public void setId(int id) {
        this.id = id;
    }

    public int getNumero() {
        return numero;
    }

    public int getPiso() {
        return piso;
    }

    public String getLocal() {
        return local;
    }

    public void setLocal(String local) {
        this.local = local == null ? "" : local.trim();
    }

    public TipoQuarto getTipo() {
        return tipo;
    }

    public void setTipo(TipoQuarto tipo) {
        this.tipo = Objects.requireNonNull(tipo, "O tipo de quarto é obrigatório.");
    }

    public BigDecimal getPrecoBase() {
        return precoBase;
    }

    public void setPrecoBase(BigDecimal precoBase) {
        this.precoBase = validarPreco(precoBase);
    }

    public Vista getVista() {
        return vista;
    }

    /** Muda a vista; o valor da vista volta ao valor inicial até o DAO ou o controller indicar o em vigor. */
    public void setVista(Vista vista) {
        this.vista = Objects.requireNonNull(vista, "A vista é obrigatória.");
        this.suplementoVista = vista.getSuplemento();
    }

    /** @return valor da vista em vigor, somado ao preço base em {@link #precoNoite()} */
    public BigDecimal getSuplementoVista() {
        return suplementoVista;
    }

    /**
     * Indica o valor da vista em vigor (lido da base de dados pelo DAO).
     *
     * @param suplementoVista valor a somar ao preço base por noite, não negativo
     * @throws IllegalArgumentException se for nulo ou negativo
     */
    public void setSuplementoVista(BigDecimal suplementoVista) {
        if (suplementoVista == null || suplementoVista.signum() < 0) {
            throw new IllegalArgumentException("O valor da vista não pode ser negativo.");
        }
        this.suplementoVista = suplementoVista;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    @Override
    public String toString() {
        return "Quarto " + numero + " (" + tipo.getNome() + ")";
    }

    private static BigDecimal validarPreco(BigDecimal preco) {
        if (preco == null || preco.signum() < 0) {
            throw new IllegalArgumentException("O preço base não pode ser negativo.");
        }
        return preco;
    }
}
