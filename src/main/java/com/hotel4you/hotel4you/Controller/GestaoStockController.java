package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.DAOException;
import com.hotel4you.hotel4you.DAO.StockDAO;
import com.hotel4you.hotel4you.Model.Artigo;
import com.hotel4you.hotel4you.Model.Fornecedor;
import com.hotel4you.hotel4you.Model.MovimentoStock;
import com.hotel4you.hotel4you.Model.Utilizador;
import com.hotel4you.hotel4you.Model.encomenda.EncomendaFornecedor;
import com.hotel4you.hotel4you.Model.encomenda.FornecedorEncomenda;
import com.hotel4you.hotel4you.Model.encomenda.LinhaEncomenda;
import com.hotel4you.hotel4you.Model.enums.Perfil;
import com.hotel4you.hotel4you.Model.enums.TipoMovimento;
import com.hotel4you.hotel4you.Utils.LeitorEncomendaXml;
import com.hotel4you.hotel4you.Utils.XmlInvalidoException;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Entrada de stock a partir do XML do fornecedor (S4-03 e S4-04) e consulta do stock.
 * <p>
 * Só o rececionista (funcionário) e o gestor podem usar este controller. O XML é sempre validado
 * contra o XSD antes de ser lido (ver {@link LeitorEncomendaXml}); um XML inválido nunca chega a
 * gerar entradas. A entrada regista, por cada linha da encomenda, o artigo (descrição do produto),
 * a quantidade, o fornecedor e a data/hora do registo, e é guardada por inteiro ou não é guardada.
 */
public class GestaoStockController {

    /** Limites das colunas: dbo.Artigo (Nome, Unidade) e dbo.Fornecedor (Nome, Nif). */
    private static final int MAX_NOME = 100;
    private static final int MAX_UNIDADE = 20;
    private static final int MAX_IDENTIFICADOR_FORNECEDOR = 20;

    private final StockDAO stockDAO;
    private final Sessao sessao;

    /**
     * @param stockDAO onde o stock, os artigos e os fornecedores são guardados
     * @param sessao   sessão com o utilizador autenticado
     */
    public GestaoStockController(StockDAO stockDAO, Sessao sessao) {
        this.stockDAO = Objects.requireNonNull(stockDAO);
        this.sessao = Objects.requireNonNull(sessao);
    }

    /**
     * Valida um ficheiro XML do fornecedor contra o XSD e, se for válido, lê os seus dados.
     * Não guarda nada.
     *
     * @param ficheiro caminho do ficheiro XML escolhido
     * @return a encomenda lida do ficheiro
     * @throws XmlInvalidoException     se o XML não cumprir o XSD (traz a linha e o motivo de cada erro)
     * @throws IllegalArgumentException se o ficheiro não for indicado ou não puder ser lido
     * @throws SecurityException        se o utilizador não for rececionista nem gestor
     */
    public EncomendaFornecedor lerEncomendaXml(Path ficheiro) {
        exigirStock();
        if (ficheiro == null) {
            throw new IllegalArgumentException("Escolha um ficheiro XML.");
        }
        try {
            return LeitorEncomendaXml.ler(ficheiro);
        } catch (IOException e) {
            throw new IllegalArgumentException("Não foi possível ler o ficheiro '" + ficheiro.getFileName()
                    + "': " + e.getMessage(), e);
        }
    }

    /**
     * Dá entrada em stock do material de uma encomenda já validada. Cada linha soma a sua quantidade
     * ao artigo com a mesma descrição (que é criado se ainda não existir) e fica registada como um
     * movimento de entrada, feito pelo utilizador autenticado.
     *
     * @param encomenda encomenda lida de um XML válido (ver {@link #lerEncomendaXml(Path)})
     * @return o número de linhas que deram entrada
     * @throws IllegalArgumentException se alguma linha for inválida (quantidade a zero, textos longos
     *                                  demais ou unidade diferente da do artigo já em stock); nada é guardado
     * @throws SecurityException        se o utilizador não for rececionista nem gestor
     * @throws AcessoDadosException     se não for possível guardar
     */
    public int darEntrada(EncomendaFornecedor encomenda) {
        exigirStock();
        Objects.requireNonNull(encomenda, "A encomenda é obrigatória.");
        Fornecedor fornecedor = criarFornecedor(encomenda.fornecedor());
        List<MovimentoStock> entradas = criarEntradas(encomenda.linhas());
        try {
            stockDAO.registarEntrada(fornecedor, entradas);
        } catch (DAOException e) {
            throw new AcessoDadosException(e.getMessage(), e);
        }
        return entradas.size();
    }

    /**
     * Consulta o stock atual de todos os artigos.
     *
     * @return os artigos com a quantidade em stock e o fornecedor, ordenados por nome
     * @throws SecurityException    se o utilizador não for rececionista nem gestor
     * @throws AcessoDadosException se não for possível ler
     */
    public List<Artigo> consultarStock() {
        exigirStock();
        try {
            return stockDAO.listarArtigos();
        } catch (DAOException e) {
            throw new AcessoDadosException(e.getMessage(), e);
        }
    }

    private static Fornecedor criarFornecedor(FornecedorEncomenda dados) {
        limitar(dados.identificador(), MAX_IDENTIFICADOR_FORNECEDOR, "O identificador do fornecedor");
        limitar(dados.nome(), MAX_NOME, "O nome do fornecedor");
        return new Fornecedor(0, dados.nome(), dados.identificador(), "");
    }

    private List<MovimentoStock> criarEntradas(List<LinhaEncomenda> linhas) {
        Map<String, String> unidades = unidadesEmStock();
        LocalDateTime agora = LocalDateTime.now();
        List<MovimentoStock> entradas = new ArrayList<>();
        for (LinhaEncomenda linha : linhas) {
            String nome = linha.descricao();
            String unidade = linha.quantidade().unidade().getAbreviatura();
            String prefixo = "Linha " + linha.numero() + ": ";
            if (linha.quantidade().valor() < 1) {
                throw new IllegalArgumentException(prefixo + "a quantidade tem de ser pelo menos 1.");
            }
            limitar(nome, MAX_NOME, prefixo + "a descrição do produto");
            limitar(unidade, MAX_UNIDADE, prefixo + "a unidade");
            String existente = unidades.putIfAbsent(nome.trim().toLowerCase(Locale.ROOT), unidade);
            if (existente != null && !existente.equals(unidade)) {
                throw new IllegalArgumentException(prefixo + "'" + nome.trim() + "' está em stock em '" + existente
                        + "' e o XML indica '" + unidade + "'. Não se somam unidades diferentes.");
            }
            entradas.add(new MovimentoStock(TipoMovimento.ENTRADA, linha.quantidade().valor(), agora,
                    utilizadorAtual(), new Artigo(0, nome, unidade, 0)));
        }
        return entradas;
    }

    /** Unidade de cada artigo já em stock, pelo nome em minúsculas. */
    private Map<String, String> unidadesEmStock() {
        Map<String, String> unidades = new HashMap<>();
        for (Artigo artigo : consultarStock()) {
            unidades.put(artigo.getNome().toLowerCase(Locale.ROOT), artigo.getUnidade());
        }
        return unidades;
    }

    private Utilizador utilizadorAtual() {
        return sessao.getUtilizadorAtual().orElseThrow(() -> new SecurityException("Inicie sessão."));
    }

    private static void limitar(String texto, int maximo, String descricao) {
        if (texto != null && texto.trim().length() > maximo) {
            throw new IllegalArgumentException(descricao + " não pode ter mais de " + maximo + " caracteres.");
        }
    }

    private void exigirStock() {
        Permissoes.exigirPerfil(sessao, "Apenas o rececionista ou o gestor podem gerir o stock.",
                Perfil.RECECIONISTA, Perfil.GESTOR);
    }
}
