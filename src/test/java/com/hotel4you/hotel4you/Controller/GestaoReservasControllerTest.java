package com.hotel4you.hotel4you.Controller;

import com.hotel4you.hotel4you.DAO.ClienteDAO;
import com.hotel4you.hotel4you.DAO.DAOException;
import com.hotel4you.hotel4you.DAO.QuartoDAO;
import com.hotel4you.hotel4you.DAO.ReservaDAOMemoria;
import com.hotel4you.hotel4you.Model.Cliente;
import com.hotel4you.hotel4you.Model.Quarto;
import com.hotel4you.hotel4you.Model.Reserva;
import com.hotel4you.hotel4you.Model.Utilizador;
import com.hotel4you.hotel4you.Model.enums.EstadoReserva;
import com.hotel4you.hotel4you.Model.enums.Perfil;
import com.hotel4you.hotel4you.Model.enums.TipoQuarto;
import com.hotel4you.hotel4you.Model.enums.Vista;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GestaoReservasControllerTest {

    private ClienteDAOEmMemoria clienteDAO;
    private ReservaDAOMemoria reservaDAO;
    private QuartoDAOEmMemoria quartoDAO;
    private Sessao sessao;
    private GestaoReservasController controller;
    private Cliente maria;
    private Quarto quarto202;
    private Quarto quarto203;

    private final LocalDate entrada = LocalDate.now().plusDays(10);
    private final LocalDate saida = LocalDate.now().plusDays(13);

    @BeforeEach
    void preparar() {
        clienteDAO = new ClienteDAOEmMemoria();
        reservaDAO = new ReservaDAOMemoria();
        // O DAO de quartos em memória ignora reservas; aqui exclui os quartos já reservados no período.
        quartoDAO = new QuartoDAOEmMemoria() {
            @Override
            public List<Quarto> listarDisponiveis(TipoQuarto tipo, LocalDate dataEntrada, LocalDate dataSaida) {
                return super.listarDisponiveis(tipo, dataEntrada, dataSaida).stream()
                        .filter(q -> !reservaDAO.existeSobreposicao(q.getId(), dataEntrada, dataSaida))
                        .collect(Collectors.toList());
            }
        };
        sessao = new Sessao();
        sessao.iniciar(Utilizador.novo("rececao", "rececao123", "Ana Rececionista", Perfil.RECECIONISTA));
        controller = new GestaoReservasController(reservaDAO, clienteDAO, quartoDAO, sessao);

        maria = clienteDAO.inserir(new Cliente(0, "Maria Silva", "PT123456", "910000000"));
        // 80 (preço base) + 20 (vista mar) = 100 por noite
        quarto202 = quartoDAO.inserir(new Quarto(0, 202, 2, "", TipoQuarto.DUPLO,
                new BigDecimal("80.00"), Vista.MAR, true));
        quarto203 = quartoDAO.inserir(new Quarto(0, 203, 2, "", TipoQuarto.INDIVIDUAL,
                new BigDecimal("50.00"), Vista.NENHUMA, true));
    }

    @Test
    void criarReservaGuardaClienteQuartoDatasValorEEstadoPendente() {
        Reserva reserva = controller.criarReserva(maria, quarto202, entrada, saida);

        assertTrue(reserva.getId() > 0);
        assertEquals(maria, reserva.getCliente());
        assertEquals(quarto202, reserva.getQuarto());
        assertEquals(entrada, reserva.getDataEntrada());
        assertEquals(saida, reserva.getDataSaida());
        assertEquals(EstadoReserva.PENDENTE, reserva.getEstado());
        assertEquals(0, new BigDecimal("300.00").compareTo(reserva.calcularTotal()));
        assertEquals(1, reservaDAO.listarTodos().size());
    }

    @Test
    void estimarValorMultiplicaNoitesPeloPrecoDaNoite() {
        assertEquals(0, new BigDecimal("300.00").compareTo(controller.estimarValor(quarto202, entrada, saida)));
        assertEquals(0, new BigDecimal("150.00").compareTo(controller.estimarValor(quarto203, entrada, saida)));
    }

    @Test
    void naoReservaUmQuartoJaReservadoNoPeriodo() {
        controller.criarReserva(maria, quarto202, entrada, saida);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> controller.criarReserva(maria, quarto202, entrada.plusDays(1), saida.plusDays(1)));

        assertTrue(erro.getMessage().contains("não está disponível"));
        assertEquals(1, reservaDAO.listarTodos().size());
    }

    @Test
    void aceitaReservaQueComecaNoDiaDeSaidaDeOutra() {
        controller.criarReserva(maria, quarto202, entrada, saida);

        Reserva seguinte = controller.criarReserva(maria, quarto202, saida, saida.plusDays(2));

        assertEquals(EstadoReserva.PENDENTE, seguinte.getEstado());
        assertEquals(2, reservaDAO.listarTodos().size());
    }

    @Test
    void outroQuartoNoMesmoPeriodoEstaLivre() {
        controller.criarReserva(maria, quarto202, entrada, saida);

        Reserva outra = controller.criarReserva(maria, quarto203, entrada, saida);

        assertEquals(quarto203, outra.getQuarto());
    }

    @Test
    void rejeitaDatasInvalidas() {
        assertThrows(IllegalArgumentException.class, () -> controller.criarReserva(maria, quarto202, null, saida));
        assertThrows(IllegalArgumentException.class, () -> controller.criarReserva(maria, quarto202, entrada, null));
        assertThrows(IllegalArgumentException.class, () -> controller.criarReserva(maria, quarto202, saida, entrada));
        assertThrows(IllegalArgumentException.class, () -> controller.criarReserva(maria, quarto202, entrada, entrada));
        assertThrows(IllegalArgumentException.class, () -> controller.criarReserva(maria, quarto202,
                LocalDate.now().minusDays(2), LocalDate.now().plusDays(1)));
        assertTrue(reservaDAO.listarTodos().isEmpty());
    }

    @Test
    void exigeClienteEQuartoGuardados() {
        Cliente naoGuardado = new Cliente(0, "Sem Registo", "999", "");
        Quarto naoGuardadoQuarto = new Quarto(0, 210, 2, "", TipoQuarto.DUPLO,
                new BigDecimal("80.00"), Vista.NENHUMA, true);

        assertThrows(IllegalArgumentException.class, () -> controller.criarReserva(null, quarto202, entrada, saida));
        assertThrows(IllegalArgumentException.class, () -> controller.criarReserva(maria, null, entrada, saida));
        assertThrows(IllegalArgumentException.class, () -> controller.criarReserva(naoGuardado, quarto202, entrada, saida));
        assertThrows(IllegalArgumentException.class,
                () -> controller.criarReserva(maria, naoGuardadoQuarto, entrada, saida));
        assertTrue(reservaDAO.listarTodos().isEmpty());
    }

    @Test
    void naoReservaUmQuartoInativo() {
        quarto202.setAtivo(false);
        quartoDAO.atualizar(quarto202);

        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> controller.criarReserva(maria, quarto202, entrada, saida));

        assertTrue(erro.getMessage().contains("não está ativo"));
        assertTrue(reservaDAO.listarTodos().isEmpty());
    }

    @Test
    void consultarQuartosDisponiveisExcluiOsReservadosEFiltraPorTipo() {
        controller.criarReserva(maria, quarto202, entrada, saida);

        List<Quarto> todos = controller.consultarQuartosDisponiveis(null, entrada, saida);
        List<Quarto> duplos = controller.consultarQuartosDisponiveis(TipoQuarto.DUPLO, entrada, saida);
        List<Quarto> individuais = controller.consultarQuartosDisponiveis(TipoQuarto.INDIVIDUAL, entrada, saida);

        assertEquals(1, todos.size());
        assertEquals(203, todos.get(0).getNumero());
        assertTrue(duplos.isEmpty());
        assertEquals(1, individuais.size());
    }

    @Test
    void consultarReservasDevolveAsReservasGuardadas() {
        controller.criarReserva(maria, quarto202, entrada, saida);
        controller.criarReserva(maria, quarto203, entrada, saida);

        assertEquals(2, controller.consultarReservas().size());
    }

    @Test
    void gestorTambemPodeCriarReservas() {
        sessao.iniciar(Utilizador.novo("gestor", "gestor123", "Gestor", Perfil.GESTOR));

        Reserva reserva = controller.criarReserva(maria, quarto202, entrada, saida);

        assertTrue(reserva.getId() > 0);
    }

    @Test
    void outrosPerfisESemSessaoNaoPodemUsarAsReservas() {
        sessao.iniciar(Utilizador.novo("forn", "forn12345", "Fornecedor", Perfil.FORNECEDOR));
        assertThrows(SecurityException.class, () -> controller.criarReserva(maria, quarto202, entrada, saida));
        assertThrows(SecurityException.class, () -> controller.consultarReservas());
        assertThrows(SecurityException.class, () -> controller.consultarQuartosDisponiveis(null, entrada, saida));

        sessao.terminar();
        assertThrows(SecurityException.class, () -> controller.criarReserva(maria, quarto202, entrada, saida));
        assertTrue(reservaDAO.listarTodos().isEmpty());
    }

    // ---- DAO em memória só para este teste (não dependem de outros ficheiros) ----

    static class ClienteDAOEmMemoria implements ClienteDAO {

        private final Map<Integer, Cliente> clientes = new LinkedHashMap<>();
        private int proximoId = 1;

        @Override
        public Cliente inserir(Cliente cliente) {
            exigirDocumentoLivre(cliente, 0);
            cliente.setId(proximoId++);
            clientes.put(cliente.getId(), cliente);
            return cliente;
        }

        @Override
        public void atualizar(Cliente cliente) {
            if (!clientes.containsKey(cliente.getId())) {
                throw new DAOException("O cliente com id " + cliente.getId() + " não existe.");
            }
            exigirDocumentoLivre(cliente, cliente.getId());
            clientes.put(cliente.getId(), cliente);
        }

        @Override
        public void remover(int id) {
            if (clientes.remove(id) == null) {
                throw new DAOException("O cliente com id " + id + " não existe.");
            }
        }

        @Override
        public Optional<Cliente> procurarPorId(int id) {
            return Optional.ofNullable(clientes.get(id));
        }

        @Override
        public List<Cliente> listarTodos() {
            return new ArrayList<>(clientes.values());
        }

        @Override
        public Optional<Cliente> procurarPorDocumento(String documento) {
            if (documento == null) {
                return Optional.empty();
            }
            return clientes.values().stream()
                    .filter(c -> c.getDocumento().equalsIgnoreCase(documento.trim()))
                    .findFirst();
        }

        private void exigirDocumentoLivre(Cliente cliente, int idIgnorado) {
            boolean repetido = clientes.values().stream()
                    .anyMatch(c -> c.getId() != idIgnorado && c.getDocumento().equalsIgnoreCase(cliente.getDocumento()));
            if (repetido) {
                throw new DAOException("Já existe um cliente com o NIF/documento '" + cliente.getDocumento() + "'.");
            }
        }
    }

    static class QuartoDAOEmMemoria implements QuartoDAO {

        private final Map<Integer, Quarto> quartos = new LinkedHashMap<>();
        private int proximoId = 1;

        @Override
        public Quarto inserir(Quarto quarto) {
            if (procurarPorNumero(quarto.getNumero()).isPresent()) {
                throw new DAOException("Já existe o quarto número " + quarto.getNumero() + ".");
            }
            quarto.setId(proximoId++);
            quartos.put(quarto.getId(), quarto);
            return quarto;
        }

        @Override
        public void atualizar(Quarto quarto) {
            if (!quartos.containsKey(quarto.getId())) {
                throw new DAOException("O quarto com id " + quarto.getId() + " não existe.");
            }
            quartos.put(quarto.getId(), quarto);
        }

        @Override
        public void remover(int id) {
            if (quartos.remove(id) == null) {
                throw new DAOException("O quarto com id " + id + " não existe.");
            }
        }

        @Override
        public Optional<Quarto> procurarPorId(int id) {
            return Optional.ofNullable(quartos.get(id));
        }

        @Override
        public Optional<Quarto> procurarPorNumero(int numero) {
            return quartos.values().stream().filter(q -> q.getNumero() == numero).findFirst();
        }

        @Override
        public List<Quarto> listarTodos() {
            List<Quarto> lista = new ArrayList<>(quartos.values());
            lista.sort(Comparator.comparingInt(Quarto::getNumero));
            return lista;
        }

        @Override
        public List<Quarto> listarAtivos() {
            return listarTodos().stream().filter(Quarto::isAtivo).collect(Collectors.toList());
        }

        @Override
        public List<Quarto> listarDisponiveis(TipoQuarto tipo, LocalDate dataEntrada, LocalDate dataSaida) {
            return listarAtivos().stream().filter(q -> tipo == null || q.getTipo() == tipo).collect(Collectors.toList());
        }
    }
}