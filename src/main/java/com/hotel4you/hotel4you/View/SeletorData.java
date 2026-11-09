package com.hotel4you.hotel4you.View;

import com.hotel4you.hotel4you.Utils.Utils;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Configuração comum dos seletores de data (calendário) dos ecrãs de reservas e de disponibilidade (S4-07).
 * A data pode ser escolhida no calendário ou escrita como dd/mm/aaaa.
 */
final class SeletorData {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private SeletorData() {
    }

    /**
     * Prepara um seletor: mostra a data como dd/mm/aaaa, não deixa escolher dias anteriores ao primeiro
     * permitido e avisa quando o texto muda (escrito à mão ou escolhido no calendário).
     *
     * @param campo                    seletor a preparar
     * @param diasMinimosAPartirDeHoje 0 = pode ser hoje; 1 = só a partir de amanhã
     * @param aoMudar                  executado sempre que a data muda
     */
    static void configurar(DatePicker campo, int diasMinimosAPartirDeHoje, Runnable aoMudar) {
        campo.setConverter(new StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate data) {
                return data == null ? "" : data.format(FORMATO_DATA);
            }

            @Override
            public LocalDate fromString(String texto) {
                return Utils.converterData(texto).orElse(null);
            }
        });
        LocalDate primeiroDia = LocalDate.now().plusDays(diasMinimosAPartirDeHoje);
        campo.setDayCellFactory(seletor -> new DateCell() {
            @Override
            public void updateItem(LocalDate data, boolean vazio) {
                super.updateItem(data, vazio);
                setDisable(vazio || data.isBefore(primeiroDia));
            }
        });
        campo.getEditor().textProperty().addListener((observavel, antigo, novo) -> aoMudar.run());
    }

    /**
     * Lê a data da caixa de texto do seletor (funciona também se ainda não carregou em Enter).
     *
     * @param campo seletor de data
     * @return a data, ou vazio se o texto não for uma data válida
     */
    static Optional<LocalDate> ler(DatePicker campo) {
        return Utils.converterData(campo.getEditor().getText());
    }

    /**
     * Apaga a data escolhida e o texto escrito.
     *
     * @param campo seletor de data
     */
    static void limpar(DatePicker campo) {
        campo.setValue(null);
        campo.getEditor().clear();
    }
}
