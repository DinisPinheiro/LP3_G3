package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.Model.Artigo;
import com.hotel4you.hotel4you.Model.Fornecedor;
import com.hotel4you.hotel4you.Model.MovimentoStock;
import com.hotel4you.hotel4you.Model.encomenda.EncomendaFornecedor;
import com.hotel4you.hotel4you.Model.encomenda.LinhaEncomenda;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link StockDAO} em memória, <b>só para testes unitários</b>. Respeita o contrato do DAO real:
 * o fornecedor é único pelo identificador, o artigo pelo nome, e a entrada guarda-se toda ou nada.
 * Com encomenda, guarda-a e atualiza o último preço e taxa de cada artigo (S5-04).
 */
public class StockDAOMemoria implements StockDAO {

    private final Map<String, Fornecedor> fornecedores = new LinkedHashMap<>();
    private final Map<String, Artigo> artigos = new LinkedHashMap<>();
    private final List<MovimentoStock> movimentos = new ArrayList<>();
    private final List<EncomendaFornecedor> encomendas = new ArrayList<>();
    private int proximoIdFornecedor = 1;
    private int proximoIdArtigo = 1;

    @Override
    public void registarEntrada(Fornecedor fornecedor, EncomendaFornecedor encomenda,
                                List<MovimentoStock> entradas) {
        if (encomenda != null && encomenda.linhas().size() != entradas.size()) {
            throw new IllegalArgumentException("Tem de haver uma entrada por cada linha da encomenda.");
        }
        Map<String, Artigo> copia = new LinkedHashMap<>();
        artigos.forEach((nome, artigo) -> copia.put(nome, copiar(artigo)));
        Fornecedor guardado = fornecedores.get(fornecedor.getIdentificador().toLowerCase());
        if (guardado == null) {
            fornecedor.setId(proximoIdFornecedor++);
            guardado = fornecedor;
        } else {
            fornecedor.setId(guardado.getId());
        }
        int idArtigo = proximoIdArtigo;
        for (int i = 0; i < entradas.size(); i++) {
            MovimentoStock entrada = entradas.get(i);
            String chave = entrada.getArtigo().getNome().toLowerCase();
            Artigo artigo = copia.get(chave);
            if (artigo == null) {
                Artigo pedido = entrada.getArtigo();
                artigo = new Artigo(idArtigo++, pedido.getNome(), pedido.getUnidade(), 0);
                copia.put(chave, artigo);
            }
            artigo.aplicar(entrada);
            artigo.setFornecedor(guardado);
            if (encomenda != null) {
                LinhaEncomenda linha = encomenda.linhas().get(i);
                artigo.setUltimoPreco(linha.precoUnitario().preco().valor(),
                        linha.precoUnitario().unidade().getAbreviatura(), linha.imposto().percentagem());
            }
        }
        // Só chega aqui se nenhuma entrada falhou.
        fornecedores.putIfAbsent(guardado.getIdentificador().toLowerCase(), guardado);
        artigos.clear();
        artigos.putAll(copia);
        proximoIdArtigo = idArtigo;
        movimentos.addAll(entradas);
        if (encomenda != null) {
            encomendas.add(encomenda);
        }
    }

    @Override
    public List<Artigo> listarArtigos() {
        List<Artigo> lista = new ArrayList<>(artigos.values());
        lista.sort((a, b) -> a.getNome().compareToIgnoreCase(b.getNome()));
        return lista;
    }

    /** @return os movimentos guardados, pela ordem de registo (para os testes confirmarem) */
    public List<MovimentoStock> getMovimentos() {
        return List.copyOf(movimentos);
    }

    /** @return as encomendas guardadas, pela ordem de entrada (para os testes confirmarem) */
    public List<EncomendaFornecedor> getEncomendas() {
        return List.copyOf(encomendas);
    }

    /**
     * Indica quanto do artigo está usado em reservas (o DAO real calcula-o a partir de dbo.ReservaProduto).
     *
     * @param nome       nome do artigo
     * @param quantidade quantidade usada em reservas não canceladas
     */
    public void definirReservado(String nome, int quantidade) {
        artigos.get(nome.toLowerCase()).setQuantidadeReservada(quantidade);
    }

    /** @return os fornecedores guardados (para os testes confirmarem) */
    public List<Fornecedor> getFornecedores() {
        return new ArrayList<>(fornecedores.values());
    }

    private static Artigo copiar(Artigo original) {
        Artigo copia = new Artigo(original.getId(), original.getNome(), original.getUnidade(),
                original.getQuantidadeAtual());
        copia.setFornecedor(original.getFornecedor());
        copia.setQuantidadeReservada(original.getQuantidadeReservada());
        copia.setUltimoPreco(original.getPrecoUnitario(), original.getUnidadePreco(), original.getTaxaPercentagem());
        return copia;
    }
}
