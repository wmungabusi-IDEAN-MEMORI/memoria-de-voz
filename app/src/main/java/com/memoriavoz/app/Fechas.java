package com.memoriavoz.app;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/** Convierte fechas en frases naturales para mostrar y para decir en voz alta. */
public final class Fechas {
    private static final Locale ES = new Locale("es", "EC");
    private static final DateTimeFormatter LARGA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ES);

    private Fechas() {}

    public static String repeticion(int repite) {
        switch (repite) {
            case ParserFechas.REPITE_DIA: return "Todos los días";
            case ParserFechas.REPITE_SEMANA: return "Cada semana";
            case ParserFechas.REPITE_MES: return "Cada mes";
            case ParserFechas.REPITE_ANIO: return "Cada año";
            default: return "";
        }
    }

    public static String hoyLargo(LocalDate d) {
        String s = d.format(LARGA);
        return s.substring(0, 1).toUpperCase(ES) + s.substring(1);
    }

    public static String mesCorto(LocalDate d) {
        return d.format(DateTimeFormatter.ofPattern("MMM", ES)).replace(".", "").toUpperCase(ES);
    }

    public static String hora(LocalTime t) {
        int h24 = t.getHour();
        int h = h24 % 12 == 0 ? 12 : h24 % 12;
        String periodo = h24 < 12 ? "de la mañana" : h24 < 19 ? "de la tarde" : "de la noche";
        if (h24 == 12) periodo = "del mediodía";
        String art = h == 1 ? "la 1" : "las " + h;
        String min = t.getMinute() == 0 ? "" : " y " + t.getMinute();
        return art + min + " " + periodo;
    }

    public static String dia(LocalDate d, LocalDate hoy) {
        long n = ChronoUnit.DAYS.between(hoy, d);
        if (n == 0) return "hoy";
        if (n == 1) return "mañana";
        if (n == 2) return "pasado mañana";
        return "el " + d.format(LARGA);
    }

    public static String completa(LocalDateTime ev, boolean conHora, LocalDate hoy) {
        String s = dia(ev.toLocalDate(), hoy);
        return conHora ? s + " a " + hora(ev.toLocalTime()) : s;
    }

    /** La frase que dirá el celular al llegar el aviso. */
    public static String frase(String texto, LocalDateTime evento, boolean conHora, LocalDateTime ahora) {
        long minutos = ChronoUnit.MINUTES.between(ahora, evento);
        String cuando;
        if (conHora && minutos <= 1 && minutos >= -5) {
            cuando = "ahora";
        } else if (conHora && minutos > 1 && minutos <= 90) {
            cuando = minutos >= 55 && minutos <= 65 ? "dentro de una hora" : "dentro de " + minutos + " minutos";
        } else {
            cuando = completa(evento, conHora, ahora.toLocalDate());
        }
        return "Recordatorio para " + cuando + ": " + texto + ".";
    }
}
