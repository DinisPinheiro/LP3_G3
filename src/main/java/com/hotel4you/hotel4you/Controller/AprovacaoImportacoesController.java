package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.DAOException;
import com.hotel4you.hotel4you.DAO.ImportacaoDAO;
import com.hotel4you.hotel4you.Model.ImportacaoFornecedor;
import com.hotel4you.hotel4you.Model.Utilizador;
import com.hotel4you.hotel4you.Model.encomenda.EncomendaFornecedor;
import com.hotel4you.hotel4you.Model.enums.EstadoImportacao;
import com.hotel4you.hotel4you.Model.enums.Perfil;
import com.hotel4you.hotel4you.Utils.LeitorEncomendaXml;
import com.hotel4you.hotel4you.Utils.XmlInvalidoException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Aprovação ou rejeição, pelo gestor ou rececionista, dos ficheiros XML importados pelos
 * fornecedores (S5-03).
 * <p>
 * Os ficheiros chegam {@code PENDENTE}. Só quando um ficheiro é aprovado é que o seu material dá
 * entrada em stock (através do {@link GestaoStockController}); um ficheiro rejeitado nunca dá entrada.
 * Um ficheiro já decidido não volta a mudar de estado.
 */
public class AprovacaoImportacoesController {

    private final ImportacaoDAO importacaoDAO;
    private final GestaoStockController gestaoStock;
    private final GestaoXsdController gestaoXsd;
    private final Sessao sessao;

    /**
     * @param importacaoDAO onde os ficheiros importados são guardados
     * @param gestaoStock   dá entrada em stock do material dos ficheiros aprovados
     * @param gestaoXsd     fornece o XSD em vigor
     * @param sessao        sessão com o utilizador autenticado (gestor ou rececionista)
     */
    public AprovacaoImportacoesController(ImportacaoDAO importacaoDAO, GestaoStockController gestaoStock,
                                          GestaoXsdController gestaoXsd, Sessao sessao) {
        this.importacaoDAO = Objects.requireNonNull(importacaoDAO);
        this.gestaoStock = Objects.requireNonNull(gestaoStock);
        this.gestaoXsd = Objects.requireNonNull(gestaoXsd);
        this.sessao = Objects.requireNonNull(sessao);
    }

    /**
     * Lista os ficheiros importados.
     *
     * @param estado estado a listar (pendentes, aprovados ou rejeitados), ou {@code null} para todos
     * @return os ficheiros, do mais recente para o mais antigo
     * @throws SecurityException    se o utilizador não for gestor nem rececionista
     * @throws AcessoDadosException se não for possível ler
     */
    public List<ImportacaoFornecedor> listar(EstadoImportacao estado) {
        exigirDecisor();
        try {
            return importacaoDAO.listar(estado);
        } catch (DAOException e) {
            throw new AcessoDadosException(e.getMessage(), e);
        }
    }

    /**
     * Lê o conteúdo de um ficheiro importado, para o ver antes de decidir.
     *
     * @param idImportacao identificador da importação
     * @return a encomenda lida do ficheiro
     * @throws XmlInvalidoException     se o ficheiro já não cumprir o XSD em vigor
     * @throws IllegalArgumentException se a importação não existir
     * @throws SecurityException        se o utilizador não for gestor nem rececionista
     */
    public EncomendaFornecedor consultarEncomenda(int idImportacao) {
        exigirDecisor();
        return lerEncomenda(obter(idImportacao));
    }

    /**
     * Aprova um ficheiro pendente: o seu material dá entrada em stock e o ficheiro passa a aprovado.
     * Se a entrada em stock falhar (ficheiro que já não cumpre o XSD, unidades diferentes das do stock, ...)
     * nada é guardado e o ficheiro continua pendente.
     *
     * @param idImportacao identificador da importação
     * @throws IllegalStateException    se o ficheiro já foi aprovado ou rejeitado
     * @throws IllegalArgumentException se a importação não existir ou o material não puder dar entrada
     * @throws XmlInvalidoException     se o ficheiro já não cumprir o XSD em vigor
     * @throws SecurityException        se o utilizador não for gestor nem rececionista
     * @throws AcessoDadosException     se não for possível guardar
     */
    public void aprovar(int idImportacao) {
        Utilizador decisor = exigirDecisor();
        ImportacaoFornecedor importacao = obter(idImportacao);
        importacao.exigirPendente();
        gestaoStock.darEntrada(lerEncomenda(importacao));
        importacao.aprovar(decisor, LocalDateTime.now());
        try {
            importacaoDAO.guardarDecisao(importacao);
        } catch (DAOException e) {
            throw new AcessoDadosException("O material deu entrada em stock, mas não foi possível registar a "
                    + "aprovação do ficheiro. Confirme o stock antes de tentar de novo. " + e.getMessage(), e);
        }
    }

    /**
     * Rejeita um ficheiro pendente. O material não dá entrada em stock.
     *
     * @param idImportacao identificador da importação
     * @throws IllegalStateException    se o ficheiro já foi aprovado ou rejeitado
     * @throws IllegalArgumentException se a importação não existir
     * @throws SecurityException        se o utilizador não for gestor nem rececionista
     * @throws AcessoDadosException     se não for possível guardar
     */
    public void rejeitar(int idImportacao) {
        Utilizador decisor = exigirDecisor();
        ImportacaoFornecedor importacao = obter(idImportacao);
        importacao.rejeitar(decisor, LocalDateTime.now());
        try {
            importacaoDAO.guardarDecisao(importacao);
        } catch (DAOException e) {
            throw new AcessoDadosException(e.getMessage(), e);
        }
    }

    private ImportacaoFornecedor obter(int idImportacao) {
        try {
            return importacaoDAO.procurarPorId(idImportacao)
                    .orElseThrow(() -> new IllegalArgumentException("O ficheiro selecionado não existe."));
        } catch (DAOException e) {
            throw new AcessoDadosException(e.getMessage(), e);
        }
    }

    private EncomendaFornecedor lerEncomenda(ImportacaoFornecedor importacao) {
        try {
            return LeitorEncomendaXml.ler(importacao.getConteudo(), gestaoXsd.obterSchema(importacao.getFornecedor()));
        } catch (IOException e) {
            throw new IllegalArgumentException("Não foi possível ler o ficheiro '" + importacao.getNomeFicheiro()
                    + "': " + e.getMessage(), e);
        }
    }

    private Utilizador exigirDecisor() {
        Permissoes.exigirPerfil(sessao, "Apenas o rececionista ou o gestor podem aprovar ou rejeitar ficheiros.",
                Perfil.RECECIONISTA, Perfil.GESTOR);
        return sessao.getUtilizadorAtual().orElseThrow();
    }
}
