package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.Model.Artigo;
import com.hotel4you.hotel4you.Model.Fornecedor;
import com.hotel4you.hotel4you.Model.MovimentoStock;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link StockDAO} em memória, <b>só para testes unitários</b>. Respeita o contrato do DAO real:
 * o fornecedor é único pelo identificador, o artigo pelo nome, e a entrada guarda-se toda ou nada.
 */
public class StockDAOMemoria implements StockDAO {

    private final Map<String, Fornecedor> fornecedores = new LinkedHashMap<>();
    private final Map<String, Artigo> artigos = new LinkedHashMap<>();
    private final List<MovimentoStock> movimentos = new ArrayList<>();
    private int proximoIdFornecedor = 1;
    private int proximoIdArtigo = 1;

    @Override
    public void registarEntrada(Fornecedor fornecedor, List<MovimentoStock> entradas) {
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
        for (MovimentoStock entrada : entradas) {
            String chave = entrada.getArtigo().getNome().toLowerCase();
            Artigo artigo = copia.get(chave);
            if (artigo == null) {
                Artigo pedido = entrada.getArtigo();
                artigo = new Artigo(idArtigo++, pedido.getNome(), pedido.getUnidade(), 0);
                copia.put(chave, artigo);
            }
            artigo.aplicar(entrada);
            artigo.setFornecedor(guardado);
        }
        // Só chega aqui se nenhuma entrada falhou.
        fornecedores.putIfAbsent(guardado.getIdentificador().toLowerCase(), guardado);
        artigos.clear();
        artigos.putAll(copia);
        proximoIdArtigo = idArtigo;
        movimentos.addAll(entradas);
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

    /** @return os fornecedores guardados (para os testes confirmarem) */
    public List<Fornecedor> getFornecedores() {
        return new ArrayList<>(fornecedores.values());
    }

    private static Artigo copiar(Artigo original) {
        Artigo copia = new Artigo(original.getId(), original.getNome(), original.getUnidade(),
                original.getQuantidadeAtual());
        copia.setFornecedor(original.getFornecedor());
        return copia;
    }
}
