package com.hotel4you.hotel4you.Model;

import com.hotel4you.hotel4you.Model.enums.EstadoReserva;
import com.hotel4you.hotel4you.Model.enums.Perfil;

import java.util.Locale;
import java.util.Objects;

/**
 * Regras de acesso a cada local do hotel (S6-06), para os três tipos de cartão:
 * <ul>
 *   <li><b>Cartão mestre</b> (código próprio): acede a todos os locais;</li>
 *   <li><b>Cartão de funcionário</b>: depende do perfil do dono. O gestor acede a tudo; o rececionista
 *       aos pisos 0 e 1; a empresa de restauração <i>só</i> à cozinha e ao restaurante; o fornecedor ao
 *       armazém (piso -1). Reutiliza {@link Utilizador#podeAcederAoPiso(int)};</li>
 *   <li><b>Cartão de cliente</b>: só com uma reserva em check-in. Abre o quarto da reserva e, se o
 *       serviço estiver incluído na reserva, o restaurante e a piscina; nunca a cozinha nem os pisos
 *       0 e -1 ({@link CartaoMagnetico#temAcessoAoPiso(int)}).</li>
 * </ul>
 * É só lógica, sem base de dados nem ecrãs: o controller reúne os dados e esta classe decide.
 */
public final class PoliticaAcesso {

    private PoliticaAcesso() {
    }

    /**
     * Resultado de um pedido de acesso.
     *
     * @param permitido {@code true} se o acesso é permitido
     * @param motivo    razão da recusa, para mostrar ao utilizador (vazia se permitido)
     */
    public record Decisao(boolean permitido, String motivo) {

        /** @return decisão a permitir o acesso */
        public static Decisao permitir() {
            return new Decisao(true, "");
        }

        /**
         * @param motivo razão da recusa
         * @return decisão a recusar o acesso
         */
        public static Decisao recusar(String motivo) {
            return new Decisao(false, motivo);
        }
    }

    /**
     * Cartão mestre: acede a todos os locais, desde que esteja ativo.
     *
     * @param cartao cartão picado
     * @param local  local pedido
     * @return a decisão
     */
    public static Decisao paraMestre(CartaoMagnetico cartao, LocalHotel local) {
        Objects.requireNonNull(cartao, "O cartão é obrigatório.");
        Objects.requireNonNull(local, "O local é obrigatório.");
        if (!cartao.isMestre()) {
            return Decisao.recusar("O cartão não é um cartão mestre.");
        }
        if (!cartao.isAtivo()) {
            return Decisao.recusar("O cartão está desativado.");
        }
        return Decisao.permitir();
    }

    /**
     * Cartão de funcionário: depende do perfil do dono do cartão.
     *
     * @param cartao      cartão picado
     * @param funcionario dono do cartão
     * @param local       local pedido
     * @param quarto      quarto escolhido (obrigatório se o local for o {@link LocalHotel#QUARTO}, senão pode ser nulo)
     * @return a decisão
     */
    public static Decisao paraFuncionario(CartaoMagnetico cartao, Utilizador funcionario, LocalHotel local, Quarto quarto) {
        Objects.requireNonNull(cartao, "O cartão é obrigatório.");
        Objects.requireNonNull(funcionario, "O funcionário é obrigatório.");
        Objects.requireNonNull(local, "O local é obrigatório.");
        if (!cartao.isAtivo()) {
            return Decisao.recusar("O cartão está desativado.");
        }
        if (!funcionario.isAtivo()) {
            return Decisao.recusar("A conta do funcionário está desativada.");
        }
        if (local.isQuarto() && quarto == null) {
            return Decisao.recusar("Indique o número do quarto.");
        }
        Perfil perfil = funcionario.getPerfil();
        return switch (perfil) {
            case GESTOR -> Decisao.permitir();
            case EMPRESA_RESTAURACAO -> LocalHotel.COZINHA.equals(local.getCodigo())
                    || LocalHotel.RESTAURANTE.equals(local.getCodigo())
                    ? Decisao.permitir()
                    : Decisao.recusar("A restauração só acede à cozinha e ao restaurante.");
            case RECECIONISTA, FORNECEDOR -> {
                int piso = local.isQuarto() ? quarto.getPiso() : local.getPiso();
                yield funcionario.podeAcederAoPiso(piso)
                        ? Decisao.permitir()
                        : Decisao.recusar("O perfil " + perfil.getNome() + " não tem acesso a este local.");
            }
        };
    }

    /**
     * Cartão de cliente: precisa de uma reserva com check-in feito com este cartão.
     *
     * @param cartao  cartão picado
     * @param reserva reserva a que o cartão foi entregue no check-in (pode ser nula se não houver)
     * @param local   local pedido
     * @param quarto  quarto escolhido (obrigatório se o local for o {@link LocalHotel#QUARTO}, senão pode ser nulo)
     * @return a decisão
     */
    public static Decisao paraCliente(CartaoMagnetico cartao, Reserva reserva, LocalHotel local, Quarto quarto) {
        Objects.requireNonNull(cartao, "O cartão é obrigatório.");
        Objects.requireNonNull(local, "O local é obrigatório.");
        if (!cartao.isAtivo()) {
            return Decisao.recusar("O cartão está desativado.");
        }
        if (reserva == null || reserva.getEstado() != EstadoReserva.CHECK_IN) {
            return Decisao.recusar("O cartão não tem uma reserva com check-in ativo.");
        }
        if (local.isQuarto()) {
            return quarto != null && quarto.getNumero() == reserva.getQuarto().getNumero()
                    ? Decisao.permitir()
                    : Decisao.recusar("O cartão só permite entrar no quarto da reserva.");
        }
        if (LocalHotel.COZINHA.equals(local.getCodigo())) {
            return Decisao.recusar("Um cartão de cliente não permite acesso à cozinha.");
        }
        if (!cartao.temAcessoAoPiso(local.getPiso())) {
            return Decisao.recusar("Um cartão de cliente não dá acesso a este local.");
        }
        boolean precisaDeServico = LocalHotel.RESTAURANTE.equals(local.getCodigo())
                || LocalHotel.PISCINA.equals(local.getCodigo());
        if (precisaDeServico && !servicoIncluido(reserva, local)) {
            return Decisao.recusar("Este local não está incluído nos serviços da reserva.");
        }
        return Decisao.permitir();
    }

    /** O restaurante e a piscina só abrem se algum serviço da reserva tiver o nome do local. */
    private static boolean servicoIncluido(Reserva reserva, LocalHotel local) {
        for (ConsumoServico consumo : reserva.getConsumos()) {
            String nome = consumo.getServico().getNome();
            if (nome != null && nome.toUpperCase(Locale.ROOT).contains(local.getCodigo())) {
                return true;
            }
        }
        return false;
    }
}