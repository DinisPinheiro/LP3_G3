package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.ClienteDAO;
import com.hotel4you.hotel4you.Model.Cliente;
import com.hotel4you.hotel4you.Model.enums.Perfil;
import com.hotel4you.hotel4you.Utils.Utils;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Registo e consulta de clientes na receção (S3-02).
 * <p>
 * Só o rececionista e o gestor (que tem as permissões do rececionista) podem usar este controller.
 * Os campos são validados com {@link Utils} e o NIF/documento não pode repetir-se.
 * <p>
 * Apoio de IA: Claude (Anthropic), revisto pela equipa.
 */
public class GestaoClientesController {

    /** Tamanhos máximos das colunas de {@code dbo.Cliente} (Nome, Nif e Contacto). */
    private static final int MAX_NOME = 100;
    private static final int MAX_DOCUMENTO = 20;
    private static final int MAX_CONTACTO = 100;

    private final ClienteDAO clienteDAO;
    private final Sessao sessao;

    /**
     * @param clienteDAO onde os clientes são guardados
     * @param sessao     sessão com o utilizador autenticado
     */
    public GestaoClientesController(ClienteDAO clienteDAO, Sessao sessao) {
        this.clienteDAO = Objects.requireNonNull(clienteDAO);
        this.sessao = Objects.requireNonNull(sessao);
    }

    /**
     * Regista um novo cliente. O NIF/documento é guardado sem espaços e em maiúsculas,
     * para que "pt 123" e "PT123" sejam reconhecidos como o mesmo documento.
     *
     * @param nome      nome completo (obrigatório)
     * @param documento NIF ou documento de identificação (obrigatório, único)
     * @param contacto  telefone ou email (opcional)
     * @return o cliente guardado, já com identificador
     * @throws IllegalArgumentException se faltar o nome ou o documento, se algum texto for demasiado
     *                                  longo para a base de dados ou se o documento já existir
     * @throws SecurityException        se o utilizador não for rececionista nem gestor
     * @throws com.hotel4you.hotel4you.DAO.DAOException se não for possível guardar
     */
    public Cliente registarCliente(String nome, String documento, String contacto) {
        exigirRececao();
        if (!Utils.estaPreenchido(nome)) {
            throw new IllegalArgumentException("Introduza o nome do cliente.");
        }
        if (!Utils.estaPreenchido(documento)) {
            throw new IllegalArgumentException("Introduza o NIF ou documento de identificação.");
        }
        String documentoNormalizado = normalizarDocumento(documento);
        validarTamanhos(nome, documentoNormalizado, contacto);
        Optional<Cliente> existente = clienteDAO.procurarPorDocumento(documentoNormalizado);
        if (existente.isPresent()) {
            throw new IllegalArgumentException("Já existe um cliente com o NIF/documento "
                    + documentoNormalizado + " (" + existente.get().getNome() + ").");
        }
        return clienteDAO.inserir(new Cliente(0, nome, documentoNormalizado, contacto));
    }

    /**
     * Consulta os clientes, por ordem alfabética do nome.
     *
     * @param pesquisa parte do nome ou do NIF/documento; vazio ou nulo devolve todos
     * @return os clientes que correspondem à pesquisa
     * @throws SecurityException se o utilizador não for rececionista nem gestor
     * @throws com.hotel4you.hotel4you.DAO.DAOException se não for possível ler
     */
    public List<Cliente> consultarClientes(String pesquisa) {
        exigirRececao();
        List<Cliente> clientes = clienteDAO.listarTodos();
        if (Utils.estaPreenchido(pesquisa)) {
            String nome = pesquisa.trim().toLowerCase(Locale.ROOT);
            String documento = normalizarDocumento(pesquisa).toLowerCase(Locale.ROOT);
            clientes = clientes.stream()
                    .filter(c -> c.getNome().toLowerCase(Locale.ROOT).contains(nome)
                            || c.getDocumento().toLowerCase(Locale.ROOT).contains(documento))
                    .collect(Collectors.toList());
        }
        clientes.sort(Comparator.comparing(Cliente::getNome, String.CASE_INSENSITIVE_ORDER));
        return clientes;
    }

    /**
     * @param documento NIF ou documento como foi escrito
     * @return o documento sem espaços e em maiúsculas (vazio se o texto for nulo)
     */
    public static String normalizarDocumento(String documento) {
        if (documento == null) {
            return "";
        }
        return documento.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    private static void validarTamanhos(String nome, String documento, String contacto) {
        if (nome.trim().length() > MAX_NOME) {
            throw new IllegalArgumentException("O nome não pode ter mais de " + MAX_NOME + " caracteres.");
        }
        if (documento.length() > MAX_DOCUMENTO) {
            throw new IllegalArgumentException("O NIF/documento não pode ter mais de " + MAX_DOCUMENTO + " caracteres.");
        }
        if (contacto != null && contacto.trim().length() > MAX_CONTACTO) {
            throw new IllegalArgumentException("O contacto não pode ter mais de " + MAX_CONTACTO + " caracteres.");
        }
    }

    private void exigirRececao() {
        Permissoes.exigirPerfil(sessao, "Apenas o rececionista ou o gestor podem gerir clientes.",
                Perfil.RECECIONISTA, Perfil.GESTOR);
    }
}
