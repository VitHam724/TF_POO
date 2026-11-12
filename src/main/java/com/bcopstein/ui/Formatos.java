package com.bcopstein.ui;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Utilitarios de exibicao usados pelas telas. */
final class Formatos {
    static final Locale BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Formatos() { }

    static String moeda(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(BR).format(valor);
    }

    static String data(LocalDate data) {
        return data.format(DATA);
    }

    static String dataHora(LocalDateTime dataHora) {
        return dataHora.format(DATA_HORA);
    }

    static String percentual(double valor) {
        return String.format(BR, "%.1f%%", valor);
    }

    static void sucesso(String mensagem) {
        Notification n = Notification.show(mensagem, 4000, Notification.Position.TOP_CENTER);
        n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    static void erro(String mensagem) {
        Notification n = Notification.show(mensagem, 6000, Notification.Position.TOP_CENTER);
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
