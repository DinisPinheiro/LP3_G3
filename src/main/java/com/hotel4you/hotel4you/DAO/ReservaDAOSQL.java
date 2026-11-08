package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.DAO.db.DBConnection;
import com.hotel4you.hotel4you.Model.CartaoMagnetico;
import com.hotel4you.hotel4you.Model.Cliente;
import com.hotel4you.hotel4you.Model.Quarto;
import com.hotel4you.hotel4you.Model.Reserva;
import com.hotel4you.hotel4you.Model.enums.EstadoReserva;

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
import java.util.Objects;
import java.util.Optional;

/**
 * {@link ReservaDAO} sobre a tabela {@code dbo.Reserva}. O cliente, o quarto e o cartão de cada
 * reserva são lidos através do {@link ClienteDAO}, do {@link QuartoDAO} e do {@link CartaoDAO}.
 */
public class ReservaDAOSQL implements ReservaDAO {

    private static final String COLUNAS =
            "IdReserva, IdCliente, IdQuarto, DataEntrada, DataSaida, Estado, DataCheckIn, DataCheckOut, IdCartao";
    private static final String ESTADOS_ATIVOS = "('PENDENTE', 'CHECK_IN')";
    private static final String SQL_SOBREPOSICAO = "SELECT COUNT(*) FROM dbo.Reserva WHERE IdQuarto = ? AND Estado IN "
            + ESTADOS_ATIVOS + " AND DataEntrada < ? AND DataSaida > ?";

    /** Linha lida da base de dados, antes de se juntar o cliente e o quarto. */
    private record Linha(int id, int idCliente, int idQuarto, LocalDate entrada, LocalDate saida,
                         EstadoReserva estado, LocalDateTime checkIn, LocalDateTime checkOut, Integer idCartao) {
    }

    private final ClienteDAO clienteDAO;
    private final QuartoDAO quartoDAO;
    private final CartaoDAO cartaoDAO;

    /**
     * @param clienteDAO DAO usado para ler o cliente de cada reserva
     * @param quartoDAO  DAO usado para ler o quarto de cada reserva
     * @param cartaoDAO  DAO usado para ler o cartão entregue no check-in
     */
    public ReservaDAOSQL(ClienteDAO clienteDAO, QuartoDAO quartoDAO, CartaoDAO cartaoDAO) {
        this.clienteDAO = Objects.requireNonNull(clienteDAO);
        this.quartoDAO = Objects.requireNonNull(quartoDAO);
        this.cartaoDAO = Objects.requireNonNull(cartaoDAO);
    }

    @Override
    public Reserva inserir(Reserva reserva) {
        if (reserva.getCliente().getId() <= 0 || reserva.getQuarto().getId() <= 0) {
            throw new DAOException("O cliente e o quarto da reserva têm de estar guardados.");
        }
        String sql = "INSERT INTO dbo.Reserva (IdCliente, IdQuarto, DataEntrada, DataSaida, Estado, ValorTotal, "
                + "DataCheckIn, DataCheckOut) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
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
                });
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

    @Override
    public void atualizar(Reserva reserva) {
        CartaoMagnetico cartao = reserva.getCartao();
        if (cartao != null && cartao.getId() <= 0) {
            throw new DAOException("O cartão da reserva tem de estar guardado.");
        }
        String sql = "UPDATE dbo.Reserva SET Estado = ?, ValorTotal = ?, DataCheckIn = ?, DataCheckOut = ?, "
                + "IdCartao = ? WHERE IdReserva = ?";
        try (Connection conn = DBConnection.getConnection()) {
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
                throw new DAOException("A reserva com id " + reserva.getId() + " não existe.");
            }
        } catch (SQLException e) {
            throw new DAOException("Não foi possível atualizar a reserva.", e);
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
        try (Connection conn = DBConnection.getConnection()) {
            int linhas = SqlUtil.executar(conn,
                    "DELETE FROM dbo.Reserva WHERE IdReserva = ? AND DataCheckIn IS NULL", ps -> ps.setInt(1, id));
            if (linhas == 0) {
                throw new DAOException("A reserva n.º " + id + " já teve check-in ou deixou de existir "
                        + "e não pode ser apagada.");
            }
        } catch (SQLException e) {
            if (SqlUtil.violaChaveEstrangeira(e)) {
                throw new DAOException("A reserva tem registos associados (consumos, reclamações) e não pode ser removida.", e);
            }
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

    private static int contarSobreposicoes(Connection conn, int idQuarto, LocalDate entrada, LocalDate saida)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_SOBREPOSICAO)) {
            ps.setInt(1, idQuarto);
            ps.setDate(2, Date.valueOf(saida));
            ps.setDate(3, Date.valueOf(entrada));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static BigDecimal valorTotal(Reserva reserva) {
        return reserva.calcularTotal().setScale(2, RoundingMode.HALF_UP);
    }

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
                            semCartao ? null : idCartao));
                }
            }
        } catch (SQLException e) {
            throw new DAOException("Não foi possível consultar as reservas.", e);
        }
        return montar(linhas);
    }

    /** Junta a cada linha o cliente, o quarto e o cartão (cada um é lido uma só vez). */
    private List<Reserva> montar(List<Linha> linhas) {
        Map<Integer, Cliente> clientes = new HashMap<>();
        Map<Integer, Quarto> quartos = new HashMap<>();
        Map<Integer, CartaoMagnetico> cartoes = new HashMap<>();
        List<Reserva> reservas = new ArrayList<>();
        for (Linha l : linhas) {
            Cliente cliente = clientes.computeIfAbsent(l.idCliente(), id -> clienteDAO.procurarPorId(id)
                    .orElseThrow(() -> new DAOException("O cliente " + id + " da reserva " + l.id() + " não existe.")));
            Quarto quarto = quartos.computeIfAbsent(l.idQuarto(), id -> quartoDAO.procurarPorId(id)
                    .orElseThrow(() -> new DAOException("O quarto " + id + " da reserva " + l.id() + " não existe.")));
            CartaoMagnetico cartao = l.idCartao() == null ? null : cartoes.computeIfAbsent(l.idCartao(),
                    id -> cartaoDAO.procurarPorId(id)
                            .orElseThrow(() -> new DAOException("O cartão " + id + " da reserva " + l.id() + " não existe.")));
            reservas.add(new Reserva(l.id(), cliente, quarto, l.entrada(), l.saida(), l.estado(), cartao,
                    l.checkIn(), l.checkOut(), List.of()));
        }
        return reservas;
    }
}