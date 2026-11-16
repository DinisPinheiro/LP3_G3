package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.DAOException;
import com.hotel4you.hotel4you.DAO.FornecedorDAO;
import com.hotel4you.hotel4you.DAO.ImportacaoDAO;
import com.hotel4you.hotel4you.Model.Fornecedor;
import com.hotel4you.hotel4you.Model.ImportacaoFornecedor;
import com.hotel4you.hotel4you.Model.encomenda.EncomendaFornecedor;
import com.hotel4you.hotel4you.Model.encomenda.FornecedorEncomenda;
import com.hotel4you.hotel4you.Model.enums.Perfil;
import com.hotel4you.hotel4you.Utils.LeitorEncomendaXml;
import com.hotel4you.hotel4you.Utils.XmlInvalidoException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Importação de ficheiros XML pelo próprio fornecedor (S5-02).
 * <p>
 * O fornecedor entra com a sua conta (perfil Fornecedor) e só pode importar ficheiros seus: o código
 * do fornecedor no XML tem de ser o da sua ficha. O XML é validado com o XSD em vigor (ver
 * {@link GestaoXsdController}); um ficheiro inválido é recusado com a linha e o motivo de cada erro.
 * Um ficheiro válido fica {@code PENDENTE} à espera de aprovação (ver {@link AprovacaoImportacoesController});
 * nada dá entrada em stock aqui.
 */
public class ImportacaoFornecedorController {

    private static final int MAX_NOME_FICHEIRO = 260;

    private final ImportacaoDAO importacaoDAO;
    private final FornecedorDAO fornecedorDAO;
    private final GestaoXsdController gestaoXsd;
    private final Sessao sessao;

    /**
     * @param importacaoDAO onde os ficheiros importados são guardados
     * @param fornecedorDAO onde as fichas dos fornecedores são guardadas
     * @param gestaoXsd     fornece o XSD em vigor
     * @param sessao        sessão com o utilizador autenticado (tem de ser fornecedor)
     */
    public ImportacaoFornecedorController(ImportacaoDAO importacaoDAO, FornecedorDAO fornecedorDAO,
                                          GestaoXsdController gestaoXsd, Sessao sessao) {
        this.importacaoDAO = Objects.requireNonNull(importacaoDAO);
        this.fornecedorDAO = Objects.requireNonNull(fornecedorDAO);
        this.gestaoXsd = Objects.requireNonNull(gestaoXsd);
        this.sessao = Objects.requireNonNull(sessao);
    }

    /**
     * @return a ficha do fornecedor a que pertence a conta com sessão iniciada
     * @throws SecurityException    se o utilizador não for fornecedor ou a conta não estiver ligada a nenhum fornecedor
     * @throws AcessoDadosException se não for possível ler
     */
    public Fornecedor obterFornecedorAtual() {
        Permissoes.exigirPerfil(sessao, "Apenas um fornecedor pode importar ficheiros XML.", Perfil.FORNECEDOR);
        int idConta = sessao.getUtilizadorAtual().orElseThrow().getId();
        try {
            return fornecedorDAO.procurarPorUtilizador(idConta).orElseThrow(() -> new SecurityException(
                    "A sua conta não está associada a nenhum fornecedor. Fale com o gestor."));
        } catch (DAOException e) {
            throw new AcessoDadosException(e.getMessage(), e);
        }
    }

    /**
     * Valida um XML e confirma que é do fornecedor com sessão iniciada, sem guardar nada.
     *
     * @param ficheiro caminho do XML escolhido
     * @return a encomenda lida do ficheiro
     * @throws XmlInvalidoException     se o XML não cumprir o XSD (traz a linha e o motivo de cada erro)
     * @throws IllegalArgumentException se o ficheiro não for indicado, não puder ser lido ou for de outro fornecedor
     * @throws SecurityException        se o utilizador não for um fornecedor com ficha
     */
    public EncomendaFornecedor lerEncomenda(Path ficheiro) {
        Fornecedor fornecedor = obterFornecedorAtual();
        return lerEValidar(lerBytes(ficheiro), fornecedor);
    }

    /**
     * Envia o XML ao hotel: valida-o, confirma que é do fornecedor e guarda-o no estado pendente.
     * Opcionalmente atualiza a ficha do fornecedor com os dados que vêm no XML.
     *
     * @param ficheiro       caminho do XML escolhido
     * @param atualizarFicha {@code true} para atualizar o nome, a morada, o código postal e a cidade do
     *                       fornecedor com os dados do XML
     * @return a importação guardada, no estado pendente
     * @throws XmlInvalidoException     se o XML não cumprir o XSD (nada é guardado)
     * @throws IllegalArgumentException se o ficheiro for de outro fornecedor ou os dados não couberem na ficha
     * @throws SecurityException        se o utilizador não for um fornecedor com ficha
     * @throws AcessoDadosException     se não for possível guardar
     */
    public ImportacaoFornecedor enviar(Path ficheiro, boolean atualizarFicha) {
        Fornecedor fornecedor = obterFornecedorAtual();
        byte[] conteudo = lerBytes(ficheiro);
        EncomendaFornecedor encomenda = lerEValidar(conteudo, fornecedor);
        ValidacaoFornecedor.limitar(ficheiro.getFileName().toString(), MAX_NOME_FICHEIRO, "O nome do ficheiro");
        if (atualizarFicha) {
            validarDadosDoXml(encomenda.fornecedor());
        }
        ImportacaoFornecedor importacao = new ImportacaoFornecedor(fornecedor, ficheiro.getFileName().toString(),
                encomenda.numero(), conteudo, LocalDateTime.now());
        try {
            importacaoDAO.inserir(importacao);
        } catch (DAOException e) {
            throw new AcessoDadosException(e.getMessage(), e);
        }
        if (atualizarFicha) {
            atualizarFicha(fornecedor, encomenda.fornecedor());
        }
        return importacao;
    }

    /**
     * @return os ficheiros enviados pelo fornecedor com sessão iniciada, do mais recente para o mais antigo
     * @throws SecurityException    se o utilizador não for um fornecedor com ficha
     * @throws AcessoDadosException se não for possível ler
     */
    public List<ImportacaoFornecedor> listarMinhasImportacoes() {
        Fornecedor fornecedor = obterFornecedorAtual();
        try {
            return importacaoDAO.listarDoFornecedor(fornecedor.getId());
        } catch (DAOException e) {
            throw new AcessoDadosException(e.getMessage(), e);
        }
    }

    private EncomendaFornecedor lerEValidar(byte[] conteudo, Fornecedor fornecedor) {
        EncomendaFornecedor encomenda;
        try {
            encomenda = LeitorEncomendaXml.ler(conteudo, gestaoXsd.obterSchemaEmVigor());
        } catch (IOException e) {
            throw new IllegalArgumentException("Não foi possível ler o ficheiro: " + e.getMessage(), e);
        }
        GestaoStockController.validarValores(encomenda);
        FornecedorEncomenda doXml = encomenda.fornecedor();
        if (!fornecedor.getIdentificador().equalsIgnoreCase(doXml.identificador().trim())) {
            throw new IllegalArgumentException("Este ficheiro é do fornecedor '" + doXml.identificador()
                    + "' e a sua conta é do fornecedor '" + fornecedor.getIdentificador()
                    + "'. Só pode importar ficheiros seus.");
        }
        return encomenda;
    }

    private static void validarDadosDoXml(FornecedorEncomenda dados) {
        ValidacaoFornecedor.validar(dados.identificador(), dados.nome(),
                Fornecedor.juntarMorada(dados.morada1(), dados.morada2()), dados.codigoPostal(), dados.cidade());
    }

    private void atualizarFicha(Fornecedor fornecedor, FornecedorEncomenda dados) {
        try {
            fornecedor.atualizarDadosDe(dados);
            fornecedorDAO.atualizar(fornecedor);
        } catch (DAOException e) {
            throw new AcessoDadosException("O ficheiro foi enviado, mas não foi possível atualizar a ficha: "
                    + e.getMessage(), e);
        }
    }

    private static byte[] lerBytes(Path ficheiro) {
        if (ficheiro == null) {
            throw new IllegalArgumentException("Escolha um ficheiro XML.");
        }
        try {
            return Files.readAllBytes(ficheiro);
        } catch (IOException e) {
            throw new IllegalArgumentException("Não foi possível ler o ficheiro '" + ficheiro.getFileName()
                    + "': " + e.getMessage(), e);
        }
    }
}
