package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.DAOException;
import com.hotel4you.hotel4you.DAO.XsdDAO;
import com.hotel4you.hotel4you.Model.FicheiroXsd;
import com.hotel4you.hotel4you.Model.enums.Perfil;
import com.hotel4you.hotel4you.Utils.ValidadorXml;
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
 * Importação e atualização do XSD pelo gestor (S5-05).
 * <p>
 * O XSD importado fica guardado e passa a ser o que valida os XML dos fornecedores, sem alterar o
 * código. Enquanto o gestor não importar nenhum, usa-se o XSD que vem com a aplicação. Um ficheiro que
 * não seja um XSD válido é recusado com uma mensagem de erro e nada muda.
 */
public class GestaoXsdController {

    private static final DateTimeFormatter FORMATO_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final XsdDAO xsdDAO;
    private final Sessao sessao;

    /**
     * @param xsdDAO onde o XSD importado é guardado
     * @param sessao sessão com o utilizador autenticado
     */
    public GestaoXsdController(XsdDAO xsdDAO, Sessao sessao) {
        this.xsdDAO = Objects.requireNonNull(xsdDAO);
        this.sessao = Objects.requireNonNull(sessao);
    }

    /**
     * Valida um ficheiro XSD e, se for válido, guarda-o como o XSD em vigor.
     *
     * @param ficheiro caminho do ficheiro XSD escolhido
     * @return o ficheiro guardado
     * @throws XsdInvalidoException     se o ficheiro não for um XSD válido (a mensagem diz o motivo)
     * @throws IllegalArgumentException se o ficheiro não for indicado, estiver vazio ou não puder ser lido
     * @throws SecurityException        se o utilizador não for um gestor
     * @throws AcessoDadosException     se não for possível guardar
     */
    public FicheiroXsd importarXsd(Path ficheiro) {
        Permissoes.exigirPerfil(sessao, "Apenas o gestor pode importar o ficheiro XSD.", Perfil.GESTOR);
        if (ficheiro == null) {
            throw new IllegalArgumentException("Escolha um ficheiro XSD.");
        }
        byte[] conteudo = lerBytes(ficheiro);
        FicheiroXsd xsd = new FicheiroXsd(0, ficheiro.getFileName().toString(), conteudo, LocalDateTime.now());
        ValidadorXml.carregarSchema(new ByteArrayInputStream(conteudo));
        try {
            xsdDAO.guardar(xsd, sessao.getUtilizadorAtual().orElseThrow().getId());
        } catch (DAOException e) {
            throw new AcessoDadosException(e.getMessage(), e);
        }
        return xsd;
    }

    /**
     * Devolve o esquema que valida os XML dos fornecedores: o último XSD importado pelo gestor ou, se
     * não houver nenhum, o que vem com a aplicação.
     *
     * @return o esquema em vigor
     * @throws IllegalStateException se o XSD guardado deixou de ser válido
     * @throws AcessoDadosException  se não for possível ler o XSD guardado
     */
    public Schema obterSchemaEmVigor() {
        Optional<FicheiroXsd> importado = obterXsdImportado();
        if (importado.isEmpty()) {
            return ValidadorXml.schemaPadrao();
        }
        try {
            return ValidadorXml.carregarSchema(new ByteArrayInputStream(importado.get().getConteudo()));
        } catch (XsdInvalidoException e) {
            throw new IllegalStateException("O XSD importado ('" + importado.get().getNomeFicheiro()
                    + "') deixou de ser válido: " + e.getMessage(), e);
        }
    }

    /**
     * @return o texto que diz qual é o XSD em vigor, pronto a mostrar ao utilizador
     * @throws AcessoDadosException se não for possível ler o XSD guardado
     */
    public String descreverXsdEmVigor() {
        return obterXsdImportado()
                .map(xsd -> "XSD importado: " + xsd.getNomeFicheiro() + " (em "
                        + xsd.getDataImportacao().format(FORMATO_DATA_HORA) + ")")
                .orElse("XSD que vem com a aplicação (ainda não foi importado nenhum).");
    }

    private Optional<FicheiroXsd> obterXsdImportado() {
        try {
            return xsdDAO.obterAtual();
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
