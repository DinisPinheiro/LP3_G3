package com.hotel4you.hotel4you.DAO;

import com.hotel4you.hotel4you.Model.Quarto;
import com.hotel4you.hotel4you.Model.enums.TipoQuarto;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Acesso aos quartos e aos equipamentos que cada um tem. Os quartos nunca se apagam com
 * reservas associadas: nesse caso desativam-se ({@link Quarto#setAtivo(boolean)}).
 */
public interface QuartoDAO extends DAO<Quarto> {

    /**
     * {@inheritDoc}
     * <p>
     * Os equipamentos do quarto têm de existir já no catálogo (id diferente de 0).
     *
     * @throws DAOException se o número já existir, se algum equipamento não estiver guardado ou se não for possível guardar
     */
    @Override
    Quarto inserir(Quarto quarto);

    /**
     * {@inheritDoc}
     * <p>
     * Atualiza tipo, local, preço, vista, estado e a lista de equipamentos (o número e o piso não mudam).
     */
    @Override
    void atualizar(Quarto quarto);

    /**
     * @param numero número do quarto (ex.: 202)
     * @return o quarto, ou vazio se não existir
     * @throws DAOException se não for possível ler
     */
    Optional<Quarto> procurarPorNumero(int numero);

    /**
     * @return os quartos ativos, ordenados por número
     * @throws DAOException se não for possível ler
     */
    List<Quarto> listarAtivos();

    /**
     * Quartos ativos sem nenhuma reserva ativa (pendente ou com check-in) que se sobreponha ao
     * intervalo pedido. Uma reserva ocupa as noites de {@code dataEntrada} até à véspera de
     * {@code dataSaida}, por isso uma reserva que sai no dia em que outra entra não se sobrepõe.
     *
     * @param tipo        tipo pretendido, ou {@code null} para qualquer tipo
     * @param dataEntrada primeira noite
     * @param dataSaida   dia de saída (posterior à entrada)
     * @return quartos disponíveis, ordenados por número
     * @throws IllegalArgumentException se a saída não for posterior à entrada
     * @throws DAOException             se não for possível ler
     */
    List<Quarto> listarDisponiveis(TipoQuarto tipo, LocalDate dataEntrada, LocalDate dataSaida);
}
