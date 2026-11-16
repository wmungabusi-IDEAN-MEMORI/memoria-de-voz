package com.yuyay.app;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/** Feriados nacionales de Ecuador y fechas especiales. No depende de Android. */
public final class Feriados {
    public static final class Dia {
        public final LocalDate fecha;
        public final String nombre;
        public final boolean feriado;
        public final String nota;

        Dia(LocalDate fecha, String nombre, boolean feriado, String nota) {
            this.fecha = fecha;
            this.nombre = nombre;
            this.feriado = feriado;
            this.nota = nota;
        }

        public String titulo() {
            return (feriado ? "Feriado: " : "") + nombre + (nota.isEmpty() ? "" : " (" + nota + ")");
        }
    }

    private Feriados() {}

    // Calendario oficial 2026-2030 (Viceministerio de Turismo): fecha de descanso | nombre | fecha original
    private static final String[] OFICIAL = {
            "2026-01-01|Año Nuevo|", "2026-02-16|Carnaval|", "2026-02-17|Carnaval|", "2026-04-03|Viernes Santo|",
            "2026-05-01|Día del Trabajo|", "2026-05-25|Batalla del Pichincha|2026-05-24",
            "2026-08-10|Primer Grito de Independencia|", "2026-10-09|Independencia de Guayaquil|",
            "2026-11-02|Día de Difuntos|", "2026-11-03|Independencia de Cuenca|", "2026-12-25|Navidad|",
            "2027-01-01|Año Nuevo|", "2027-02-08|Carnaval|", "2027-02-09|Carnaval|", "2027-03-26|Viernes Santo|",
            "2027-04-30|Día del Trabajo|2027-05-01", "2027-05-24|Batalla del Pichincha|",
            "2027-08-09|Primer Grito de Independencia|2027-08-10", "2027-10-08|Independencia de Guayaquil|2027-10-09",
            "2027-11-01|Día de Difuntos|2027-11-02", "2027-11-02|Independencia de Cuenca|2027-11-03",
            "2027-12-25|Navidad|",
            "2028-01-01|Año Nuevo|", "2028-02-28|Carnaval|", "2028-02-29|Carnaval|", "2028-04-14|Viernes Santo|",
            "2028-05-01|Día del Trabajo|", "2028-05-26|Batalla del Pichincha|2028-05-24",
            "2028-08-11|Primer Grito de Independencia|2028-08-10", "2028-10-09|Independencia de Guayaquil|",
            "2028-11-02|Día de Difuntos|", "2028-11-03|Independencia de Cuenca|", "2028-12-25|Navidad|",
            "2029-01-01|Año Nuevo|", "2029-02-12|Carnaval|", "2029-02-13|Carnaval|", "2029-03-30|Viernes Santo|",
            "2029-04-30|Día del Trabajo|2029-05-01", "2029-05-25|Batalla del Pichincha|2029-05-24",
            "2029-08-10|Primer Grito de Independencia|", "2029-10-09|Independencia de Guayaquil|",
            "2029-11-01|Día de Difuntos|2029-11-02", "2029-11-02|Independencia de Cuenca|2029-11-03",
            "2029-12-25|Navidad|",
            "2030-01-01|Año Nuevo|", "2030-03-04|Carnaval|", "2030-03-05|Carnaval|", "2030-04-19|Viernes Santo|",
            "2030-05-03|Día del Trabajo|2030-05-01", "2030-05-24|Batalla del Pichincha|",
            "2030-08-09|Primer Grito de Independencia|2030-08-10", "2030-10-11|Independencia de Guayaquil|2030-10-09",
            "2030-11-01|Día de Difuntos|2030-11-02", "2030-11-04|Independencia de Cuenca|2030-11-03",
            "2030-12-25|Navidad|",
    };

    private static final String[] DIAS = {"lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo"};

    private static String trasladado(LocalDate original) {
        return "se traslada del " + DIAS[original.getDayOfWeek().getValue() - 1] + " " + original.getDayOfMonth();
    }

    /** Domingo de Pascua (algoritmo de Meeus para el calendario gregoriano). */
    public static LocalDate pascua(int y) {
        int a = y % 19, b = y / 100, c = y % 100, d = b / 4, e = b % 4, f = (b + 8) / 25, g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30, i = c / 4, k = c % 4, l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451, mes = (h + l - 7 * m + 114) / 31, dia = (h + l - 7 * m + 114) % 31 + 1;
        return LocalDate.of(y, mes, dia);
    }

    /** Regla de traslado: sábado a viernes, domingo a lunes, martes a lunes, miércoles y jueves a viernes. */
    static LocalDate trasladar(LocalDate d) {
        switch (d.getDayOfWeek()) {
            case SATURDAY: return d.minusDays(1);
            case SUNDAY: return d.plusDays(1);
            case TUESDAY: return d.minusDays(1);
            case WEDNESDAY: return d.plusDays(2);
            case THURSDAY: return d.plusDays(1);
            default: return d;
        }
    }

    private static void movible(List<Dia> l, LocalDate original, String nombre) {
        LocalDate d = trasladar(original);
        String nota = d.equals(original) ? "fecha estimada" : trasladado(original) + ", fecha estimada";
        l.add(new Dia(d, nombre, true, nota));
    }

    public static List<Dia> feriados(int y) {
        List<Dia> l = new ArrayList<>();
        String pref = y + "-";
        for (String s : OFICIAL) {
            if (!s.startsWith(pref)) continue;
            String[] p = s.split("\\|", -1);
            String nota = p[2].isEmpty() ? "" : trasladado(LocalDate.parse(p[2]));
            l.add(new Dia(LocalDate.parse(p[0]), p[1], true, nota));
        }
        if (!l.isEmpty()) return l;
        // Años sin calendario oficial publicado: se calculan con las reglas de la ley.
        LocalDate p = pascua(y);
        l.add(new Dia(LocalDate.of(y, 1, 1), "Año Nuevo", true, ""));
        l.add(new Dia(p.minusDays(48), "Carnaval", true, ""));
        l.add(new Dia(p.minusDays(47), "Carnaval", true, ""));
        l.add(new Dia(p.minusDays(2), "Viernes Santo", true, ""));
        movible(l, LocalDate.of(y, 5, 1), "Día del Trabajo");
        movible(l, LocalDate.of(y, 5, 24), "Batalla del Pichincha");
        movible(l, LocalDate.of(y, 8, 10), "Primer Grito de Independencia");
        movible(l, LocalDate.of(y, 10, 9), "Independencia de Guayaquil");
        movible(l, LocalDate.of(y, 11, 2), "Día de Difuntos");
        movible(l, LocalDate.of(y, 11, 3), "Independencia de Cuenca");
        l.add(new Dia(LocalDate.of(y, 12, 25), "Navidad", true, ""));
        return l;
    }

    public static List<Dia> especiales(int y) {
        List<Dia> l = new ArrayList<>();
        l.add(new Dia(LocalDate.of(y, 2, 14), "Día del Amor y la Amistad", false, ""));
        l.add(new Dia(LocalDate.of(y, 3, 8), "Día Internacional de la Mujer", false, ""));
        l.add(new Dia(LocalDate.of(y, 5, 1).with(TemporalAdjusters.dayOfWeekInMonth(2, DayOfWeek.SUNDAY)),
                "Día de la Madre", false, ""));
        l.add(new Dia(LocalDate.of(y, 6, 1), "Día del Niño", false, ""));
        l.add(new Dia(LocalDate.of(y, 6, 1).with(TemporalAdjusters.dayOfWeekInMonth(3, DayOfWeek.SUNDAY)),
                "Día del Padre", false, ""));
        l.add(new Dia(LocalDate.of(y, 12, 24), "Nochebuena", false, ""));
        l.add(new Dia(LocalDate.of(y, 12, 31), "Fin de Año", false, ""));
        return l;
    }

    /** Fechas entre desde y hasta (incluidas), ordenadas. */
    public static List<Dia> entre(LocalDate desde, LocalDate hasta, boolean feriados, boolean especiales) {
        List<Dia> l = new ArrayList<>();
        for (int y = desde.getYear(); y <= hasta.getYear(); y++) {
            if (feriados) l.addAll(feriados(y));
            if (especiales) l.addAll(especiales(y));
        }
        l.removeIf(d -> d.fecha.isBefore(desde) || d.fecha.isAfter(hasta));
        l.sort((a, b) -> a.fecha.compareTo(b.fecha));
        return l;
    }

    /** Identificador fijo para no duplicar el mismo feriado. */
    public static long id(Dia d) {
        long dia = d.fecha.getYear() * 10000L + d.fecha.getMonthValue() * 100L + d.fecha.getDayOfMonth();
        return 8_000_000_000_000L + dia * 10 + (Math.abs(d.nombre.hashCode()) % 10);
    }
}
