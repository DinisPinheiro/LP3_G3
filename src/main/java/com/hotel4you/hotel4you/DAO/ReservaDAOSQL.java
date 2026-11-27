package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.DAO.db.DBConnection;
import com.hotel4you.hotel4you.Model.Artigo;
import com.hotel4you.hotel4you.Model.CartaoMagnetico;
import com.hotel4you.hotel4you.Model.Cliente;
import com.hotel4you.hotel4you.Model.ConsumoServico;
import com.hotel4you.hotel4you.Model.ProdutoReserva;
import com.hotel4you.hotel4you.Model.Quarto;
import com.hotel4you.hotel4you.Model.Reserva;
import com.hotel4you.hotel4you.Model.enums.EstadoReserva;
import com.hotel4you.hotel4you.Model.enums.TipoCobranca;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@link ReservaDAO} sobre a tabela {@code dbo.Reserva}. O cliente, o quarto e o cartão de cada
 * reserva são lidos na mesma ligação da consulta das reservas (com o {@link ClienteDAOSQL}, o
 * {@link QuartoDAOSQL} e o {@link CartaoDAOSQL}), para que cada consulta abra uma só ligação (S6-01).
 * <p>
 * Os serviços e extras da reserva ficam em {@code dbo.ConsumoServico} e os produtos do stock em
 * {@code dbo.ReservaProduto} (S5-06). Os preços em vigor na criação ficam guardados em
 * {@code dbo.Reserva} (PrecoBaseNoite, ValorVistaNoite) e em {@code dbo.ConsumoServico}
 * (ValorUnitario, Cobranca), para não mudarem quando o gestor altera os valores (S5-09).
 * Estas colunas e tabelas são criadas por {@code DATABASE/EsquemaDB_S5_servicos_produtos_reserva.sql}.
 */
public class ReservaDAOSQL implements ReservaDAO {

    private static final String COLUNAS =
            "IdReserva, IdCliente, IdQuarto, DataEntrada, DataSaida, Estado, DataCheckIn, DataCheckOut, IdCartao, "
                    + "PrecoBaseNoite, ValorVistaNoite";
    private static final String ESTADOS_ATIVOS = "('PENDENTE', 'CHECK_IN')";
    private static final String SQL_SOBREPOSICAO = "SELECT COUNT(*) FROM dbo.Reserva WHERE IdQuarto = ? AND Estado IN "
            + ESTADOS_ATIVOS + " AND DataEntrada < ? AND DataSaida > ?";
    private static final String SQL_SOBREPOSICAO_OUTRAS = SQL_SOBREPOSICAO + " AND IdReserva <> ?";
    private static final String SQL_INSERIR_SERVICO = "INSERT INTO dbo.ConsumoServico "
            + "(IdReserva, IdServico, Quantidade, Data, ValorUnitario, Cobranca) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String SQL_INSERIR_PRODUTO =
            "INSERT INTO dbo.ReservaProduto (IdReserva, IdArtigo, Quantidade) VALUES (?, ?, ?)";
    /** Primeiro artigo da reserva cujo stock do hotel não chega para tudo o que está usado em reservas. */
    private static final String SQL_ARTIGO_SEM_STOCK = "SELECT TOP 1 a.Nome FROM dbo.Artigo a "
            + "WHERE a.IdArtigo IN (SELECT IdArtigo FROM dbo.ReservaProduto WHERE IdReserva = ?) "
            + "AND a.QuantidadeAtual < " + StockDAOSQL.SQL_USADO_EM_RESERVAS;
    private static final String SQL_SERVICOS_DA_RESERVA = "SELECT cs.Quantidade, cs.Data, "
            + "cs.ValorUnitario AS ValorGuardado, cs.Cobranca AS CobrancaGuardada, "
            + ServicoDAOSQL.COLUNAS + " "
            + "FROM dbo.ConsumoServico cs JOIN dbo.Servico s ON s.IdServico = cs.IdServico"
            + ServicoDAOSQL.JUNCAO_PRODUTO + " "
            + "WHERE cs.IdReserva = ? ORDER BY cs.IdConsumo";
    private static final String SQL_PRODUTOS_DA_RESERVA = "SELECT rp.Quantidade, "
            + "a.IdArtigo, a.Nome, a.Unidade, a.QuantidadeAtual "
            + "FROM dbo.ReservaProduto rp JOIN dbo.Artigo a ON a.IdArtigo = rp.IdArtigo "
            + "WHERE rp.IdReserva = ? ORDER BY rp.IdReservaProduto";

    /**
     * Linha lida da base de dados, antes de se juntar o cliente e o quarto. Os preços vêm a
     * {@code null} nas reservas guardadas antes de existirem as colunas de preço.
     */
    private record Linha(int id, int idCliente, int idQuarto, LocalDate entrada, LocalDate saida,
                         EstadoReserva estado, LocalDateTime checkIn, LocalDateTime checkOut, Integer idCartao,
                         BigDecimal precoBase, BigDecimal valorVista,
                         List<ConsumoServico> servicos, List<ProdutoReserva> produtos) {
    }

    /** Lê um registo (cliente, quarto ou cartão) pelo id, com uma ligação já aberta. */
    @FunctionalInterface
    private interface Leitor<T> {
        Optional<T> ler(Connection conn, int id) throws SQLException;
    }

    @Override
    public Reserva inserir(Reserva reserva) {
        if (reserva.getCliente().getId() <= 0 || reserva.getQuarto().getId() <= 0) {
            throw new DAOException("O cliente e o quarto da reserva têm de estar guardados.");
        }
        String sql = "INSERT INTO dbo.Reserva (IdCliente, IdQuarto, DataEntrada, DataSaida, Estado, ValorTotal, "
                + "DataCheckIn, DataCheckOut, PrecoBaseNoite, ValorVistaNoite) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try {
                if (contarSobreposicoes(conn, reserva.getQuarto().getId(), reserva.getDataEntrada(),
                        reserva.getDataSaida()) > 0) {
                    conn.rollback();
                    throw new DAOException("O " + reserva.getQuarto() + " já está reservado entre "
                            + reserva.getDataEntrada() + " e " + reserva.getDataSaida() + ".");
                }
                int id = SqlUtil.inserirComId(conn, sql, ps -> {
                    ps.setInt(1, reserva.getCliente().getId());
                    ps.setInt(2, reserva.getQuarto().getId());
                    ps.setDate(3, Date.valueOf(reserva.getDataEntrada()));
                    ps.setDate(4, Date.valueOf(reserva.getDataSaida()));
                    ps.setString(5, reserva.getEstado().name());
                    ps.setBigDecimal(6, valorTotal(reserva));
                    SqlUtil.definirDataHora(ps, 7, reserva.getDataCheckIn());
                    SqlUtil.definirDataHora(ps, 8, reserva.getDataCheckOut());
                    ps.setBigDecimal(9, reserva.getPrecoBaseNoite());
                    ps.setBigDecimal(10, reserva.getValorVistaNoite());
                });
                gravarServicos(conn, id, reserva.getConsumos());
                gravarProdutos(conn, id, reserva.getProdutos());
                // Com os produtos já gravados, confirma dentro da transação que o stock chega.
                String semStock = procurarArtigoSemStock(conn, id);
                if (semStock != null) {
                    conn.rollback();
                    throw new DAOException("Não há stock disponível suficiente de '" + semStock + "'.");
                }
                conn.commit();
                reserva.setId(id);
                return reserva;
            } catch (SQLException e) {
                SqlUtil.reverter(conn, e);
                throw new DAOException("Não foi possível guardar a reserva.", e);
            }
        } catch (SQLException e) {
            throw new DAOException("Não foi possível guardar a reserva.", e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Numa reserva pendente ou com check-in os produtos também são regravados (S6-05: no check-in recebe
     * os equipamentos do quarto e na edição podem mudar as quantidades) e, na mesma transação, confirma-se
     * que o stock real chega para tudo o que está em uso.
     */
    @Override
    public void atualizar(Reserva reserva) {
        CartaoMagnetico cartao = reserva.getCartao();
        if (cartao != null && cartao.getId() <= 0) {
            throw new DAOException("O cartão da reserva tem de estar guardado.");
        }
        String sql = "UPDATE dbo.Reserva SET Estado = ?, ValorTotal = ?, DataCheckIn = ?, DataCheckOut = ?, "
                + "IdCartao = ? WHERE IdReserva = ?";
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int linhas = SqlUtil.executar(conn, sql, ps -> {
                    ps.setString(1, reserva.getEstado().name());
                    ps.setBigDecimal(2, valorTotal(reserva));
                    SqlUtil.definirDataHora(ps, 3, reserva.getDataCheckIn());
                    SqlUtil.definirDataHora(ps, 4, reserva.getDataCheckOut());
                    if (cartao == null) {
                        ps.setNull(5, Types.INTEGER);
                    } else {
                        ps.setInt(5, cartao.getId());
                    }
                    ps.setInt(6, reserva.getId());
                });
                if (linhas == 0) {
                    conn.rollback();
                    throw new DAOException("A reserva com id " + reserva.getId() + " não existe.");
                }
                if (reserva.podeEditarProdutos()) {
                    regravarProdutos(conn, reserva);
                }
                conn.commit();
            } catch (SQLException e) {
                SqlUtil.reverter(conn, e);
                throw new DAOException("Não foi possível atualizar a reserva.", e);
            } catch (RuntimeException e) {
                conn.rollback();   // ex.: o stock não chega; nada fica guardado
                throw e;
            }
        } catch (SQLException e) {
            throw new DAOException("Não foi possível atualizar a reserva.", e);
        }
    }

    /**
     * Guarda os serviços atualizados e o novo valor total.
     */
    @Override
    public void atualizarServicos(Reserva reserva) {

        // Verifica o estado da reserva.
        if (!reserva.podeEditarServicos()) {
            throw new IllegalStateException("Esta reserva não pode ser editada.");
        }

        try (Connection conn = DBConnection.getConnection()) {

            // Inicia a transação.
            conn.setAutoCommit(false);
            try {
                // 1. Verifica o estado atual na base de dados.
                String sqlEstado = "SELECT Estado FROM dbo.Reserva WITH (UPDLOCK, HOLDLOCK) " + "WHERE IdReserva = ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlEstado)) {
                    ps.setInt(1, reserva.getId());

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new DAOException("A reserva não existe.");
                        }
                        String estado = rs.getString("Estado");
                        if (!estado.equals("CHECK_IN")) {
                            throw new DAOException("A reserva não está ativa.");
                        }
                    }
                }

                // 2. Remove os serviços antigos da reserva.
                String sqlRemover = "DELETE FROM dbo.ConsumoServico " + "WHERE IdReserva = ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlRemover)) {
                    ps.setInt(1, reserva.getId());
                    ps.executeUpdate();
                }

                // 3. Guarda os serviços da lista atualizada e os produtos do stock (S7-01: os serviços
                //    ligados ao stock, como as bebidas, mudam os produtos da reserva).
                gravarServicos(conn, reserva.getId(), reserva.getConsumos());
                regravarProdutos(conn, reserva);

                // 4. Atualiza o valor total da reserva.
                String sqlTotal = "UPDATE dbo.Reserva " + "SET ValorTotal = ? " + "WHERE IdReserva = ? AND Estado = 'CHECK_IN'";
                try (PreparedStatement ps = conn.prepareStatement(sqlTotal)) {
                    ps.setBigDecimal(1, valorTotal(reserva));
                    ps.setInt(2, reserva.getId());
                    int linhas = ps.executeUpdate();
                    if (linhas != 1) {throw new DAOException("Não foi possível atualizar a reserva.");
                    }
                }

                // 5. Confirma todas as alterações.
                conn.commit();

            } catch (SQLException | RuntimeException e) {
                // Se alguma operação falhar,
                // cancela as alterações na base de dados.
                conn.rollback();

                throw e;
            }

        } catch (SQLException e) {
            throw new DAOException("Erro ao atualizar os serviços da reserva.", e);
        }
    }

    @Override
    public void remover(int id) {
        Reserva reserva = procurarPorId(id)
                .orElseThrow(() -> new DAOException("A reserva com id " + id + " não existe."));
        // Regra do check-in (definida no model): uma reserva que já teve check-in nunca se apaga.
        if (!reserva.podeSerApagada()) {
            throw new DAOException("A reserva n.º " + id + " já teve check-in e não pode ser apagada.");
        }
        // "DataCheckIn IS NULL" repete a regra na própria instrução: se o check-in for feito
        // entre a leitura acima e o DELETE, a reserva não é apagada.
        // Os serviços e os produtos da reserva são apagados com ela, na mesma transação.
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                SqlUtil.executar(conn, "DELETE FROM dbo.ConsumoServico WHERE IdReserva = ?", ps -> ps.setInt(1, id));
                SqlUtil.executar(conn, "DELETE FROM dbo.ReservaProduto WHERE IdReserva = ?", ps -> ps.setInt(1, id));
                int linhas = SqlUtil.executar(conn,
                        "DELETE FROM dbo.Reserva WHERE IdReserva = ? AND DataCheckIn IS NULL", ps -> ps.setInt(1, id));
                if (linhas == 0) {
                    conn.rollback();
                    throw new DAOException("A reserva n.º " + id + " já teve check-in ou deixou de existir "
                            + "e não pode ser apagada.");
                }
                conn.commit();
            } catch (SQLException e) {
                SqlUtil.reverter(conn, e);
                if (SqlUtil.violaChaveEstrangeira(e)) {
                    throw new DAOException("A reserva tem registos associados (reclamações) e não pode ser removida.", e);
                }
                throw new DAOException("Não foi possível remover a reserva.", e);
            }
        } catch (SQLException e) {
            throw new DAOException("Não foi possível remover a reserva.", e);
        }
    }

    @Override
    public Optional<Reserva> procurarPorId(int id) {
        List<Reserva> encontradas = listar("SELECT " + COLUNAS + " FROM dbo.Reserva WHERE IdReserva = ?",
                ps -> ps.setInt(1, id));
        return encontradas.isEmpty() ? Optional.empty() : Optional.of(encontradas.get(0));
    }

    @Override
    public List<Reserva> listarTodos() {
        return listar("SELECT " + COLUNAS + " FROM dbo.Reserva ORDER BY DataEntrada DESC, IdReserva DESC", ps -> { });
    }

    @Override
    public List<Reserva> listarPorCliente(int idCliente) {
        return listar("SELECT " + COLUNAS + " FROM dbo.Reserva WHERE IdCliente = ? "
                + "ORDER BY DataEntrada DESC, IdReserva DESC", ps -> ps.setInt(1, idCliente));
    }

    @Override
    public boolean existeSobreposicao(int idQuarto, LocalDate dataEntrada, LocalDate dataSaida) {
        try (Connection conn = DBConnection.getConnection()) {
            return contarSobreposicoes(conn, idQuarto, dataEntrada, dataSaida) > 0;
        } catch (SQLException e) {
            throw new DAOException("Não foi possível verificar a disponibilidade do quarto.", e);
        }
    }

    @Override
    public boolean existeSobreposicao(int idQuarto, LocalDate dataEntrada, LocalDate dataSaida,
                                      int idReservaIgnorada) {
        try (Connection conn = DBConnection.getConnection()) {
            return contarSobreposicoes(conn, idQuarto, dataEntrada, dataSaida, idReservaIgnorada) > 0;
        } catch (SQLException e) {
            throw new DAOException("Não foi possível verificar a disponibilidade do quarto.", e);
        }
    }

    /**
     * Guarda as novas datas e o novo valor total. Dentro da mesma transação confirma que a reserva
     * continua ativa e que nenhuma outra reserva ocupa o quarto nas novas datas.
     */
    @Override
    public void atualizarDatas(Reserva reserva) {
        if (!reserva.podeEditarDatas()) {
            throw new DAOException("A reserva não está ativa.");
        }
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            try {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT Estado FROM dbo.Reserva WITH (UPDLOCK, HOLDLOCK) WHERE IdReserva = ?")) {
                    ps.setInt(1, reserva.getId());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new DAOException("A reserva n.º " + reserva.getId() + " não existe.");
                        }
                        String estado = rs.getString("Estado");
                        if (!estado.equals("PENDENTE") && !estado.equals("CHECK_IN")) {
                            throw new DAOException("A reserva não está ativa.");
                        }
                    }
                }
                if (contarSobreposicoes(conn, reserva.getQuarto().getId(), reserva.getDataEntrada(),
                        reserva.getDataSaida(), reserva.getId()) > 0) {
                    throw new DAOException("O " + reserva.getQuarto() + " já está reservado entre "
                            + reserva.getDataEntrada() + " e " + reserva.getDataSaida() + ".");
                }
                int linhas = SqlUtil.executar(conn, "UPDATE dbo.Reserva SET DataEntrada = ?, DataSaida = ?, "
                        + "ValorTotal = ? WHERE IdReserva = ? AND Estado IN " + ESTADOS_ATIVOS, ps -> {
                    ps.setDate(1, Date.valueOf(reserva.getDataEntrada()));
                    ps.setDate(2, Date.valueOf(reserva.getDataSaida()));
                    ps.setBigDecimal(3, valorTotal(reserva));
                    ps.setInt(4, reserva.getId());
                });
                if (linhas != 1) {
                    throw new DAOException("Não foi possível atualizar as datas da reserva.");
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DAOException("Não foi possível atualizar as datas da reserva.", e);
        }
    }

    private static int contarSobreposicoes(Connection conn, int idQuarto, LocalDate entrada, LocalDate saida)
            throws SQLException {
        return contarSobreposicoes(conn, idQuarto, entrada, saida, 0);
    }

    private static int contarSobreposicoes(Connection conn, int idQuarto, LocalDate entrada, LocalDate saida,
                                           int idReservaIgnorada) throws SQLException {
        // IdReserva <> 0 é sempre verdadeiro, por isso sem reserva a ignorar a consulta conta todas.
        try (PreparedStatement ps = conn.prepareStatement(SQL_SOBREPOSICAO_OUTRAS)) {
            ps.setInt(1, idQuarto);
            ps.setDate(2, Date.valueOf(saida));
            ps.setDate(3, Date.valueOf(entrada));
            ps.setInt(4, idReservaIgnorada);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static BigDecimal valorTotal(Reserva reserva) {
        return reserva.calcularTotal().setScale(2, RoundingMode.HALF_UP);
    }

    private static void gravarServicos(Connection conn, int idReserva, List<ConsumoServico> servicos)
            throws SQLException {
        for (ConsumoServico servico : servicos) {
            SqlUtil.executar(conn, SQL_INSERIR_SERVICO, ps -> {
                ps.setInt(1, idReserva);
                ps.setInt(2, servico.getServico().getId());
                ps.setInt(3, servico.getQuantidade());
                ps.setDate(4, Date.valueOf(servico.getData()));
                ps.setBigDecimal(5, servico.getValorUnitario());
                ps.setString(6, servico.getCobranca().name());
            });
        }
    }

    private static void gravarProdutos(Connection conn, int idReserva, List<ProdutoReserva> produtos)
            throws SQLException {
        for (ProdutoReserva produto : produtos) {
            SqlUtil.executar(conn, SQL_INSERIR_PRODUTO, ps -> {
                ps.setInt(1, idReserva);
                ps.setInt(2, produto.getArtigo().getId());
                ps.setInt(3, produto.getQuantidade());
            });
        }
    }

    /**
     * Volta a gravar os produtos do stock da reserva. Se a reserva tiver check-in (stock em uso, S7-01),
     * confirma na mesma transação que o stock real chega para tudo o que está em uso.
     */
    private static void regravarProdutos(Connection conn, Reserva reserva) throws SQLException {
        SqlUtil.executar(conn, "DELETE FROM dbo.ReservaProduto WHERE IdReserva = ?",
                ps -> ps.setInt(1, reserva.getId()));
        gravarProdutos(conn, reserva.getId(), reserva.getProdutos());
        if (reserva.usaStock()) {
            String semStock = procurarArtigoSemStock(conn, reserva.getId());
            if (semStock != null) {
                throw new DAOException("Não há stock disponível suficiente de '" + semStock + "'.");
            }
        }
    }

    /** @return o nome de um artigo da reserva sem stock suficiente, ou {@code null} se o stock chega */
    private static String procurarArtigoSemStock(Connection conn, int idReserva) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_ARTIGO_SEM_STOCK)) {
            ps.setInt(1, idReserva);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("Nome") : null;
            }
        }
    }

    /**
     * Lê as reservas e tudo o que elas têm (serviços, produtos, cliente, quarto e cartão) numa só ligação.
     * Antes da S6-01 o cliente, o quarto e o cartão eram lidos com uma ligação nova cada um, o que numa
     * página de 10 reservas chegava a abrir mais de 20 ligações ao servidor.
     */
    private List<Reserva> listar(String sql, SqlUtil.Preparador p) {
        List<Linha> linhas = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            p.preparar(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idCartao = rs.getInt("IdCartao");
                    boolean semCartao = rs.wasNull();
                    linhas.add(new Linha(rs.getInt("IdReserva"), rs.getInt("IdCliente"), rs.getInt("IdQuarto"),
                            rs.getDate("DataEntrada").toLocalDate(), rs.getDate("DataSaida").toLocalDate(),
                            EstadoReserva.valueOf(rs.getString("Estado")),
                            SqlUtil.lerDataHora(rs, "DataCheckIn"), SqlUtil.lerDataHora(rs, "DataCheckOut"),
                            semCartao ? null : idCartao,
                            rs.getBigDecimal("PrecoBaseNoite"), rs.getBigDecimal("ValorVistaNoite"),
                            new ArrayList<>(), new ArrayList<>()));
                }
            }
            for (Linha linha : linhas) {
                carregarServicos(conn, linha);
                carregarProdutos(conn, linha);
            }
            return montar(conn, linhas);
        } catch (SQLException e) {
            throw new DAOException("Não foi possível consultar as reservas.", e);
        }
    }

    /** Lê os serviços e extras da reserva, com o valor e a forma de cobrança guardados na criação. */
    private static void carregarServicos(Connection conn, Linha linha) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_SERVICOS_DA_RESERVA)) {
            ps.setInt(1, linha.id());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    linha.servicos().add(new ConsumoServico(ServicoDAOSQL.mapear(rs), rs.getInt("Quantidade"),
                            rs.getDate("Data").toLocalDate(), rs.getBigDecimal("ValorGuardado"),
                            TipoCobranca.valueOf(rs.getString("CobrancaGuardada"))));
                }
            }
        }
    }

    /** Lê os produtos do stock usados pela reserva. */
    private static void carregarProdutos(Connection conn, Linha linha) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_PRODUTOS_DA_RESERVA)) {
            ps.setInt(1, linha.id());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Artigo artigo = new Artigo(rs.getInt("IdArtigo"), rs.getString("Nome"),
                            rs.getString("Unidade"), rs.getInt("QuantidadeAtual"));
                    linha.produtos().add(new ProdutoReserva(artigo, rs.getInt("Quantidade")));
                }
            }
        }
    }

    /**
     * Junta a cada linha o cliente, o quarto e o cartão, lidos com a ligação {@code conn}
     * (cada um é lido uma só vez, mesmo que apareça em várias reservas).
     */
    private static List<Reserva> montar(Connection conn, List<Linha> linhas) throws SQLException {
        Map<Integer, Cliente> clientes = new HashMap<>();
        Map<Integer, Quarto> quartos = new HashMap<>();
        Map<Integer, CartaoMagnetico> cartoes = new HashMap<>();
        List<Reserva> reservas = new ArrayList<>();
        for (Linha l : linhas) {
            Cliente cliente = lerUmaVez(conn, clientes, l.idCliente(), ClienteDAOSQL::lerPorId,
                    "O cliente " + l.idCliente() + " da reserva " + l.id() + " não existe.");
            Quarto quarto = lerUmaVez(conn, quartos, l.idQuarto(), QuartoDAOSQL::lerPorId,
                    "O quarto " + l.idQuarto() + " da reserva " + l.id() + " não existe.");
            CartaoMagnetico cartao = l.idCartao() == null ? null
                    : lerUmaVez(conn, cartoes, l.idCartao(), CartaoDAOSQL::lerPorId,
                    "O cartão " + l.idCartao() + " da reserva " + l.id() + " não existe.");
            // Reserva guardada antes de existirem as colunas de preço: usa os preços atuais do quarto.
            BigDecimal precoBase = l.precoBase() != null ? l.precoBase() : quarto.getPrecoBase();
            BigDecimal valorVista = l.valorVista() != null ? l.valorVista() : quarto.getSuplementoVista();
            reservas.add(new Reserva(l.id(), cliente, quarto, l.entrada(), l.saida(), precoBase, valorVista,
                    l.estado(), cartao, l.checkIn(), l.checkOut(), l.servicos(), l.produtos()));
        }
        return reservas;
    }

    /** Devolve o registo já lido com este id ou lê-o agora (e guarda-o em {@code lidos}). */
    private static <T> T lerUmaVez(Connection conn, Map<Integer, T> lidos, int id, Leitor<T> leitor,
                                   String erroSeNaoExistir) throws SQLException {
        T registo = lidos.get(id);
        if (registo == null) {
            registo = leitor.ler(conn, id).orElseThrow(() -> new DAOException(erroSeNaoExistir));
            lidos.put(id, registo);
        }
        return registo;
    }

    @Override
    public List<Reserva> listarPorEstados(List<EstadoReserva> estados, int pagina, int tamanhoPagina) {
        validarPaginacao(estados, pagina, tamanhoPagina);
        String marcas = String.join(", ", java.util.Collections.nCopies(estados.size(), "?"));
        String sql = "SELECT " + COLUNAS + " FROM dbo.Reserva WHERE Estado IN (" + marcas + ") " + "ORDER BY DataEntrada DESC, IdReserva DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        return listar(sql, ps -> {
            int indice = 1;
            for (EstadoReserva estado : estados) {
                ps.setString(indice, estado.name());
                indice++;
            }
            ps.setInt(indice, (pagina - 1) * tamanhoPagina);
            ps.setInt(indice + 1, tamanhoPagina);
        });
    }

    @Override
    public int contarPorEstados(List<EstadoReserva> estados) {
        validarPaginacao(estados, 1, 1);
        String marcas = String.join(", ", java.util.Collections.nCopies(estados.size(), "?"));
        String sql = "SELECT COUNT(*) FROM dbo.Reserva WHERE Estado IN (" + marcas + ")";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int indice = 1;
            for (EstadoReserva estado : estados) {
                ps.setString(indice, estado.name());
                indice++;
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
                return 0;
            }
        } catch (SQLException e) {
            throw new DAOException("Não foi possível contar as reservas.", e);
        }
    }

    // Não se usa estados.contains(null): numa lista criada com List.of(...) isso lança NullPointerException.
    private static boolean temEstadoNulo(List<EstadoReserva> estados) {
        for (EstadoReserva estado : estados) {
            if (estado == null) {
                return true;
            }
        }
        return false;
    }

    private void validarPaginacao(List<EstadoReserva> estados, int pagina, int tamanhoPagina) {
        if (estados == null || estados.isEmpty() || temEstadoNulo(estados)) {
            throw new IllegalArgumentException("Escolha um estado válido.");
        }
        if (pagina < 1 || tamanhoPagina < 1 || tamanhoPagina > 100
                || ((long) pagina - 1) * tamanhoPagina > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Paginação inválida.");
        }
    }

}
