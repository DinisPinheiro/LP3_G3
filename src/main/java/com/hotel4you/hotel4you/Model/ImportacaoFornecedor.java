package com.hotel4you.hotel4you.Model;

import com.hotel4you.hotel4you.Model.enums.EstadoImportacao;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Objects;

/**
 * Ficheiro XML enviado por um fornecedor ao hotel, à espera de ser aprovado ou rejeitado (S5-03).
 * <p>
 * Guarda os bytes do XML tal como foram enviados. A regra de aprovação está aqui: só um ficheiro
 * {@link EstadoImportacao#PENDENTE} pode ser aprovado ou rejeitado, e a decisão fica registada com
 * quem a tomou e quando.
 */
public class ImportacaoFornecedor {

    private int id;
    private final Fornecedor fornecedor;
    private final String nomeFicheiro;
    private final String numeroEncomenda;
    private final byte[] conteudo;
    private final LocalDateTime dataImportacao;
    private EstadoImportacao estado;
    private int idDecisor;
    private LocalDateTime dataDecisao;

    /**
     * Cria uma importação nova, ainda por guardar, no estado {@link EstadoImportacao#PENDENTE}.
     *
     * @param fornecedor      fornecedor que enviou o ficheiro
     * @param nomeFicheiro    nome do ficheiro enviado
     * @param numeroEncomenda número da encomenda lido do XML
     * @param conteudo        bytes do ficheiro XML
     * @param dataImportacao  data e hora do envio
     * @throws IllegalArgumentException se faltar o nome do ficheiro, o número da encomenda ou o conteúdo
     */
    public ImportacaoFornecedor(Fornecedor fornecedor, String nomeFicheiro, String numeroEncomenda,
                                byte[] conteudo, LocalDateTime dataImportacao) {
        this(0, fornecedor, nomeFicheiro, numeroEncomenda, conteudo, dataImportacao,
                EstadoImportacao.PENDENTE, 0, null);
    }

    /**
     * Reconstrói uma importação já guardada (ex.: lida da base de dados).
     *
     * @param id              identificador (0 se ainda não foi guardada)
     * @param fornecedor      fornecedor que enviou o ficheiro
     * @param nomeFicheiro    nome do ficheiro enviado
     * @param numeroEncomenda número da encomenda
     * @param conteudo        bytes do ficheiro XML
     * @param dataImportacao  data e hora do envio
     * @param estado          estado atual
     * @param idDecisor       id de quem aprovou ou rejeitou (0 se ainda está pendente)
     * @param dataDecisao     quando foi decidido (nulo se ainda está pendente)
     * @throws IllegalArgumentException se faltar algum dado obrigatório
     */
    public ImportacaoFornecedor(int id, Fornecedor fornecedor, String nomeFicheiro, String numeroEncomenda,
                                byte[] conteudo, LocalDateTime dataImportacao, EstadoImportacao estado,
                                int idDecisor, LocalDateTime dataDecisao) {
        this.id = id;
        this.fornecedor = Objects.requireNonNull(fornecedor, "O fornecedor é obrigatório.");
        this.nomeFicheiro = exigirTexto(nomeFicheiro, "O nome do ficheiro");
        this.numeroEncomenda = exigirTexto(numeroEncomenda, "O número da encomenda");
        if (conteudo == null || conteudo.length == 0) {
            throw new IllegalArgumentException("O ficheiro está vazio.");
        }
        this.conteudo = conteudo.clone();
        this.dataImportacao = Objects.requireNonNull(dataImportacao, "A data de importação é obrigatória.");
        this.estado = Objects.requireNonNull(estado, "O estado é obrigatório.");
        this.idDecisor = idDecisor;
        this.dataDecisao = dataDecisao;
    }

    /**
     * Regra da encomenda repetida (S6-03): um fornecedor não pode enviar duas vezes a mesma encomenda.
     * A encomenda é identificada pelo número, sem distinguir maiúsculas de minúsculas nem espaços nas
     * pontas, e conta qualquer envio anterior, qualquer que seja o seu estado.
     *
     * @param numeroEncomenda número da encomenda do ficheiro que vai ser enviado
     * @param jaEnviadas      ficheiros que o mesmo fornecedor já enviou
     * @throws IllegalArgumentException se o fornecedor já enviou uma encomenda com esse número
     */
    public static void exigirEncomendaNova(String numeroEncomenda, Collection<ImportacaoFornecedor> jaEnviadas) {
        String procurado = exigirTexto(numeroEncomenda, "O número da encomenda");
        for (ImportacaoFornecedor enviada : jaEnviadas) {
            if (enviada.getNumeroEncomenda().equalsIgnoreCase(procurado)) {
                throw new IllegalArgumentException("A encomenda " + procurado + " já foi enviada em "
                        + enviada.getDataImportacao().toLocalDate() + " (ficheiro '" + enviada.getNomeFicheiro()
                        + "', " + enviada.getEstado().getNome().toLowerCase() + "). "
                        + "Não é possível enviar a mesma encomenda mais do que uma vez.");
            }
        }
    }

    /**
     * Aprova o ficheiro. Só se pode aprovar um ficheiro pendente.
     *
     * @param decisor utilizador que aprova
     * @param quando  data e hora da decisão
     * @throws IllegalStateException se o ficheiro já foi aprovado ou rejeitado
     */
    public void aprovar(Utilizador decisor, LocalDateTime quando) {
        decidir(EstadoImportacao.APROVADO, decisor, quando);
    }

    /**
     * Rejeita o ficheiro. Só se pode rejeitar um ficheiro pendente.
     *
     * @param decisor utilizador que rejeita
     * @param quando  data e hora da decisão
     * @throws IllegalStateException se o ficheiro já foi aprovado ou rejeitado
     */
    public void rejeitar(Utilizador decisor, LocalDateTime quando) {
        decidir(EstadoImportacao.REJEITADO, decisor, quando);
    }

    /**
     * Confirma que o ficheiro ainda está pendente. Quem tem de fazer algo antes de decidir (como dar
     * entrada em stock) chama isto primeiro, para não fazer nada num ficheiro que já foi decidido.
     *
     * @throws IllegalStateException se o ficheiro já foi aprovado ou rejeitado
     */
    public void exigirPendente() {
        if (estado != EstadoImportacao.PENDENTE) {
            throw new IllegalStateException("O ficheiro '" + nomeFicheiro + "' já está "
                    + estado.getNome().toLowerCase() + " e não pode mudar de estado.");
        }
    }

    private void decidir(EstadoImportacao novoEstado, Utilizador decisor, LocalDateTime quando) {
        Objects.requireNonNull(decisor, "Quem decide é obrigatório.");
        Objects.requireNonNull(quando, "A data da decisão é obrigatória.");
        exigirPendente();
        this.estado = novoEstado;
        this.idDecisor = decisor.getId();
        this.dataDecisao = quando;
    }

    public int getId() {
        return id;
    }

    /** Usado pelo DAO ao guardar a importação pela primeira vez. */
    public void setId(int id) {
        this.id = id;
    }

    public Fornecedor getFornecedor() {
        return fornecedor;
    }

    public String getNomeFicheiro() {
        return nomeFicheiro;
    }

    public String getNumeroEncomenda() {
        return numeroEncomenda;
    }

    /** @return uma cópia dos bytes do XML */
    public byte[] getConteudo() {
        return conteudo.clone();
    }

    public LocalDateTime getDataImportacao() {
        return dataImportacao;
    }

    public EstadoImportacao getEstado() {
        return estado;
    }

    /** @return id de quem aprovou ou rejeitou, ou 0 se ainda está pendente */
    public int getIdDecisor() {
        return idDecisor;
    }

    /** @return quando foi aprovado ou rejeitado, ou nulo se ainda está pendente */
    public LocalDateTime getDataDecisao() {
        return dataDecisao;
    }

    private static String exigirTexto(String valor, String descricao) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(descricao + " é obrigatório.");
        }
        return valor.trim();
    }
}
