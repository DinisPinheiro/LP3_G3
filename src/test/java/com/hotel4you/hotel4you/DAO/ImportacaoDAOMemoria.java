package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.Model.ImportacaoFornecedor;
import com.hotel4you.hotel4you.Model.enums.EstadoImportacao;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@link ImportacaoDAO} em memória, <b>só para testes unitários</b>. Como o DAO real, guarda a decisão
 * apenas se a importação ainda estiver pendente na "base de dados" (cópia guardada), e devolve cópias
 * para que alterar um objeto não altere o que está guardado.
 */
public class ImportacaoDAOMemoria implements ImportacaoDAO {

    private final Map<Integer, ImportacaoFornecedor> importacoes = new LinkedHashMap<>();
    private int proximoId = 1;

    @Override
    public ImportacaoFornecedor inserir(ImportacaoFornecedor importacao) {
        importacao.setId(proximoId++);
        importacoes.put(importacao.getId(), copiar(importacao));
        return importacao;
    }

    @Override
    public Optional<ImportacaoFornecedor> procurarPorId(int id) {
        return Optional.ofNullable(importacoes.get(id)).map(ImportacaoDAOMemoria::copiar);
    }

    @Override
    public List<ImportacaoFornecedor> listar(EstadoImportacao estado) {
        return importacoes.values().stream()
                .filter(i -> estado == null || i.getEstado() == estado)
                .sorted(Comparator.comparing(ImportacaoFornecedor::getDataImportacao).reversed()
                        .thenComparing(Comparator.comparingInt(ImportacaoFornecedor::getId).reversed()))
                .map(ImportacaoDAOMemoria::copiar)
                .toList();
    }

    @Override
    public List<ImportacaoFornecedor> listarDoFornecedor(int idFornecedor) {
        List<ImportacaoFornecedor> lista = new ArrayList<>(listar(null));
        lista.removeIf(i -> i.getFornecedor().getId() != idFornecedor);
        return lista;
    }

    @Override
    public void guardarDecisao(ImportacaoFornecedor importacao) {
        ImportacaoFornecedor guardada = importacoes.get(importacao.getId());
        if (guardada == null || guardada.getEstado() != EstadoImportacao.PENDENTE) {
            throw new DAOException("O ficheiro não existe ou já foi aprovado ou rejeitado por outro utilizador.");
        }
        importacoes.put(importacao.getId(), copiar(importacao));
    }

    private static ImportacaoFornecedor copiar(ImportacaoFornecedor i) {
        return new ImportacaoFornecedor(i.getId(), i.getFornecedor(), i.getNomeFicheiro(), i.getFormato(),
                i.getNumeroEncomenda(), i.getConteudo(), i.getDataImportacao(), i.getEstado(), i.getIdDecisor(), i.getDataDecisao());
    }
}
