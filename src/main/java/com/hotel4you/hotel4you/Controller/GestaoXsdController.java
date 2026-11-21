package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.DAOException;
import com.hotel4you.hotel4you.DAO.XsdDAO;
import com.hotel4you.hotel4you.Model.FicheiroXsd;
import com.hotel4you.hotel4you.Model.Fornecedor;
import com.hotel4you.hotel4you.Model.enums.Perfil;
import com.hotel4you.hotel4you.Utils.ValidadorXml;
import com.hotel4you.hotel4you.Utils.XsdEmFaltaException;
import com.hotel4you.hotel4you.Utils.XsdInvalidoException;

import javax.xml.validation.Schema;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

/**
 * Importação do XSD pelo fornecedor (S5-05, S6-02).
 * <p>
 * Cada fornecedor importa o seu XSD, a seguir ao XML, e esse XSD fica guardado e passa a validar todos os
 * XML <b>desse</b> fornecedor, sem alterar o código, até ele importar outro (que o substitui). Não existe XSD
 * geral nem XSD que venha com a aplicação: sem XSD importado não há validação (ver {@link XsdEmFaltaException}).
 * Um ficheiro que não seja um XSD válido é recusado com uma mensagem de erro e nada muda.
 */
public class GestaoXsdController {

    private static final DateTimeFormatter FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final XsdDAO xsdDAO;
    private final Sessao sessao;

    /**
     * @param xsdDAO onde os XSD importados são guardados
     * @param sessao sessão com o utilizador autenticado
     */
    public GestaoXsdController(XsdDAO xsdDAO, Sessao sessao) {
        this.xsdDAO = Objects.requireNonNull(xsdDAO);
        this.sessao = Objects.requireNonNull(sessao);
    }

    /**
     * Valida um ficheiro XSD e, se for válido, guarda-o como o XSD em vigor do fornecedor.
     *
     * @param fornecedor ficha do fornecedor com sessão iniciada, a quem o XSD pertence
     * @param ficheiro   caminho do ficheiro XSD escolhido
     * @return o ficheiro guardado
     * @throws XsdInvalidoException     se o ficheiro não for um XSD válido (a mensagem diz o motivo)
     * @throws IllegalArgumentException se o ficheiro não for indicado, estiver vazio ou não puder ser lido
     * @throws SecurityException        se o utilizador não for fornecedor
     * @throws AcessoDadosException     se não for possível guardar
     */
    public FicheiroXsd importarXsd(Fornecedor fornecedor, Path ficheiro) {
        Permissoes.exigirPerfil(sessao, "Apenas o fornecedor pode importar o ficheiro XSD.", Perfil.FORNECEDOR);
        Objects.requireNonNull(fornecedor, "O fornecedor é obrigatório.");
        if (ficheiro == null) {
            throw new IllegalArgumentException("Escolha um ficheiro XSD.");
        }
        byte[] conteudo = lerBytes(ficheiro);
        FicheiroXsd xsd = new FicheiroXsd(0, fornecedor.getId(), ficheiro.getFileName().toString(), conteudo,
                LocalDateTime.now());
        ValidadorXml.carregarSchema(new ByteArrayInputStream(conteudo));
        try {
            xsdDAO.guardar(xsd, sessao.getUtilizadorAtual().orElseThrow().getId());
        } catch (DAOException e) {
            throw new AcessoDadosException(e.getMessage(), e);
        }
        return xsd;
    }

    /**
     * Devolve o esquema que valida os XML do fornecedor: o último XSD que ele importou.
     *
     * @param fornecedor fornecedor cujos XML se vão validar
     * @return o esquema em vigor desse fornecedor
     * @throws XsdEmFaltaException  se o fornecedor ainda não importou nenhum XSD
     * @throws IllegalStateException se o XSD guardado deixou de ser válido
     * @throws AcessoDadosException  se não for possível ler o XSD guardado
     */
    public Schema obterSchema(Fornecedor fornecedor) {
        FicheiroXsd xsd = obterXsdImportado(fornecedor).orElseThrow(() -> new XsdEmFaltaException(
                "O fornecedor '" + fornecedor.getNome() + "' ainda não importou o ficheiro XSD, por isso não é "
                        + "possível validar o XML. Escolha primeiro o ficheiro XSD."));
        try {
            return ValidadorXml.carregarSchema(new ByteArrayInputStream(xsd.getConteudo()));
        } catch (XsdInvalidoException e) {
            throw new IllegalStateException("O XSD importado ('" + xsd.getNomeFicheiro()
                    + "') deixou de ser válido: " + e.getMessage(), e);
        }
    }

    /**
     * @param fornecedor fornecedor a consultar
     * @return o texto que diz qual é o XSD em vigor do fornecedor, pronto a mostrar ao utilizador
     * @throws AcessoDadosException se não for possível ler o XSD guardado
     */
    public String descreverXsd(Fornecedor fornecedor) {
        return obterXsdImportado(fornecedor)
                .map(xsd -> "XSD em uso: " + xsd.getNomeFicheiro() + " (importado em "
                        + xsd.getDataImportacao().format(FORMATO_DATA_HORA) + ")")
                .orElse("Ainda não importou nenhum XSD. Escolha o ficheiro XSD para validar o XML.");
    }

    private Optional<FicheiroXsd> obterXsdImportado(Fornecedor fornecedor) {
        try {
            return xsdDAO.obterAtual(fornecedor.getId());
        } catch (DAOException e) {
            throw new AcessoDadosException(e.getMessage(), e);
        }
    }

    private static byte[] lerBytes(Path ficheiro) {
        try {
            return Files.readAllBytes(ficheiro);
        } catch (IOException e) {
            throw new IllegalArgumentException("Não foi possível ler o ficheiro '" + ficheiro.getFileName()
                    + "': " + e.getMessage(), e);
        }
    }
}
