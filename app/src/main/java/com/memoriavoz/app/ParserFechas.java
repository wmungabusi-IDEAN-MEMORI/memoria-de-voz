package com.memoriavoz.app;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Entiende fechas y horas dichas en español. No usa internet ni depende de Android. */
public final class ParserFechas {

    public static final class Resultado {
        public String texto;
        public LocalDate fecha;
        public LocalTime hora;
        public int repite;
    }

    public static final int REPITE_NO = 0, REPITE_DIA = 1, REPITE_SEMANA = 2, REPITE_MES = 3, REPITE_ANIO = 4;

    private static final Map<String, Integer> NUMEROS = new HashMap<>();
    static {
        String[] n = {"cero", "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve", "diez",
                "once", "doce", "trece", "catorce", "quince", "dieciseis", "diecisiete", "dieciocho", "diecinueve",
                "veinte", "veintiuno", "veintidos", "veintitres", "veinticuatro", "veinticinco", "veintiseis",
                "veintisiete", "veintiocho", "veintinueve", "treinta"};
        for (int i = 0; i < n.length; i++) NUMEROS.put(n[i], i);
        NUMEROS.put("un", 1);
        NUMEROS.put("una", 1);
        NUMEROS.put("primero", 1);
        NUMEROS.put("treinta y uno", 31);
        NUMEROS.put("cuarenta", 40);
        NUMEROS.put("cuarenta y cinco", 45);
        NUMEROS.put("cincuenta", 50);
    }

    private static final String NUM = "\\d{1,2}|treinta y uno|treinta|veintiuno|veintidos|veintitres|veinticuatro"
            + "|veinticinco|veintiseis|veintisiete|veintiocho|veintinueve|veinte|diecinueve|dieciocho|diecisiete"
            + "|dieciseis|quince|catorce|trece|doce|once|diez|nueve|ocho|siete|seis|cinco|cuatro|tres|dos|una|uno"
            + "|un|primero";
    private static final String MIN = "media|cuarto|\\d{1,2}|cincuenta|cuarenta y cinco|cuarenta|treinta|veinticinco"
            + "|veinte|quince|diez|cinco";
    private static final String DIAS = "lunes|martes|miercoles|jueves|viernes|sabado|domingo";
    private static final String MESES_RE = "enero|febrero|marzo|abril|mayo|junio|julio|agosto|septiembre|setiembre"
            + "|octubre|noviembre|diciembre";
    private static final List<String> MESES = Arrays.asList("enero", "febrero", "marzo", "abril", "mayo", "junio",
            "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre");
    private static final List<String> SEMANA = Arrays.asList("lunes", "martes", "miercoles", "jueves", "viernes",
            "sabado", "domingo");

    private static final String FIN = "(?![a-z0-9])";
    private static final String SUF = "(?:\\s*,?\\s*((?:de|en|por)\\s+la\\s+(?:manana|tarde|noche|madrugada)"
            + "|[ap]\\.?\\s?m\\.?))";
    private static final String DET = "(?:\\s*[:.h]\\s*(\\d{2})|\\s+y\\s+(" + MIN + ")|\\s+menos\\s+(" + MIN + "))?"
            + "(?:\\s+horas?)?";
    private static final String P = "(?:(?:para|desde|hasta)\\s+)?(?:(?:el|este|dia|del)\\s+)*";

    private static Pattern re(String s) {
        return Pattern.compile(s);
    }

    private static final Pattern[] HORAS = {
            re("\\b(?:(?:para|a|desde|tipo|como)\\s+)+las?\\s+(" + NUM + ")" + DET + SUF + "?" + FIN),
            re("\\blas?\\s+(" + NUM + ")" + DET + SUF + FIN),
            re("\\b(\\d{1,2})\\s*[:h]\\s*(\\d{2})()()" + SUF + "?" + FIN),
            re("\\b(" + NUM + ")()()()" + SUF + FIN),
    };
    private static final Pattern PERIODO = re("\\b(?:(?:en|por)\\s+la\\s+(manana|tarde|noche)|(esta)\\s+"
            + "(manana|tarde|noche)|(?:al\\s+|a\\s+)?(mediodia)|(?:a\\s+(?:la\\s+)?)?(medianoche))" + FIN);
    private static final Pattern RELATIVO = re("\\b(?:en|dentro\\s+de)\\s+(?:(media)\\s+hora|(\\d{1,3}|" + NUM + ")\\s+"
            + "(minutos?|horas?|dias?|semanas?|mes(?:es)?)(\\s+y\\s+media)?)" + FIN);
    private static final Pattern BARRAS = re("\\b" + P + "(\\d{1,2})[/-](\\d{1,2})(?:[/-](\\d{2,4}))?" + FIN);
    private static final Pattern COMPLETA = re("\\b" + P + "(?:(?:" + DIAS + ")\\s+)?(" + NUM + ")\\s+de\\s+(" + MESES_RE
            + ")(?:\\s+(?:de|del)\\s+(\\d{4}))?" + FIN);
    private static final Pattern HOY = re("\\b(?:para\\s+)?(?:el\\s+dia\\s+de\\s+)?hoy" + FIN);
    private static final Pattern PASADO = re("\\b(?:para\\s+)?pasado\\s+manana" + FIN);
    private static final Pattern MANANA = re("\\b(?:para\\s+)?(?:el\\s+dia\\s+de\\s+)?manana" + FIN);
    private static final Pattern DIA_NUM = re("\\b" + P + "(?:" + DIAS + ")\\s+(" + NUM + ")" + FIN);
    private static final Pattern DIA_SEMANA = re("\\b" + P + "(?:(proximo)\\s+)?(" + DIAS + ")(?:\\s+(proximo|que\\s+viene"
            + "|de\\s+la\\s+(?:proxima|otra|siguiente)\\s+semana|de\\s+la\\s+semana\\s+que\\s+viene))?" + FIN);
    private static final Pattern SEMANA_PROX = re("\\b(?:para\\s+)?(?:la\\s+)?(?:(?:proxima|otra|siguiente)\\s+semana"
            + "|semana\\s+que\\s+viene)" + FIN);
    private static final Pattern MES_PROX = re("\\b(?:para\\s+)?(?:el\\s+)?(?:proximo\\s+mes|mes\\s+que\\s+viene)" + FIN);
    private static final Pattern SOLO_DIA = re("\\b(?:para\\s+)?(?:el|dia)\\s+(?:dia\\s+)?(" + NUM + ")" + FIN);

    private static final Pattern REP_DIA = re("\\b(?:todos\\s+los\\s+dias|cada\\s+dia|diariamente|a\\s+diario)" + FIN);
    private static final Pattern REP_DIA_SEMANA = re("\\b(?:todos\\s+los|cada)\\s+(" + DIAS + ")(?:\\s+de\\s+cada\\s+semana)?" + FIN);
    private static final Pattern REP_SEMANA = re("\\b(?:todas\\s+las\\s+semanas|cada\\s+semana|semanalmente)" + FIN);
    private static final Pattern REP_NUM = re("\\b(?:todos\\s+los|cada)\\s+(\\d{1,2}|" + NUM + ")(?:\\s+de\\s+cada\\s+mes)?"
            + "(?!\\s+(?:minutos?|horas?|dias?|semanas?|meses|anos))" + FIN);
    private static final Pattern REP_MES = re("\\b(?:todos\\s+los\\s+meses|cada\\s+mes|mensualmente)" + FIN);
    private static final Pattern REP_ANIO = re("\\b(?:todos\\s+los\\s+anos|cada\\s+ano|anualmente)" + FIN);

    private static final Pattern INICIO = re("^\\s*(?:por favor\\s*,?\\s*)?(?:recuerdame|recordarme|recordar|acuerdate"
            + "|recordatorio|no olvidar|no olvides|no me dejes olvidar|avisame|guarda|guardar|anota|anotar|apunta)"
            + "(?:\\s+(?:que|de|para))?\\s*,?\\s+");
    private static final Pattern ORILLA_INI = re("^(?:(?:de|del|para|a|al|que|y|en)\\s+|[,.;:\\s]+)");
    private static final Pattern ORILLA_FIN = re("(?:\\s+(?:el|la|los|las|de|del|para|a|al|que|y|en|este|esta)|[,.;:\\s]+)$");

    private final String original;
    private final StringBuilder trabajo;
    private final boolean[] borrar;
    private final LocalDateTime ahora;

    private ParserFechas(String texto, LocalDateTime ahora) {
        this.original = texto;
        this.ahora = ahora;
        this.trabajo = new StringBuilder(normalizar(texto));
        this.borrar = new boolean[texto.length()];
    }

    public static Resultado analizar(String texto, LocalDateTime ahora) {
        return new ParserFechas(texto == null ? "" : texto.trim(), ahora).ejecutar();
    }

    /** Misma longitud que el original: así se puede borrar el tramo exacto de la frase. */
    private static String normalizar(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            char l = Character.toLowerCase(c);
            int i = "áéíóúüñ".indexOf(l);
            b.append(i >= 0 ? "aeiouun".charAt(i) : l);
        }
        return b.toString();
    }

    private static int valor(String s) {
        if (s == null || s.isEmpty()) return -1;
        if (Character.isDigit(s.charAt(0))) return Integer.parseInt(s);
        Integer v = NUMEROS.get(s);
        return v == null ? -1 : v;
    }

    private static int minutos(String s) {
        if ("media".equals(s)) return 30;
        if ("cuarto".equals(s)) return 15;
        return valor(s);
    }

    private static boolean vacio(String s) {
        return s == null || s.isEmpty();
    }

    private void marcar(Matcher m) {
        for (int i = m.start(); i < m.end(); i++) {
            borrar[i] = true;
            trabajo.setCharAt(i, '#');
        }
    }

    private Matcher buscar(Pattern p) {
        Matcher m = p.matcher(trabajo);
        return m.find() ? m : null;
    }

    private Resultado ejecutar() {
        Resultado r = new Resultado();
        boolean[] sinPeriodo = {false};
        repeticion(r);
        relativo(r);
        if (r.hora == null) hora(r, sinPeriodo);
        periodo(r, sinPeriodo);
        if (r.fecha == null) fecha(r);

        if (r.hora != null && sinPeriodo[0] && r.hora.getHour() < 12) {
            boolean paraHoy = r.fecha == null || r.fecha.equals(ahora.toLocalDate());
            if (paraHoy && r.hora.isBefore(ahora.toLocalTime())
                    && r.hora.plusHours(12).isAfter(ahora.toLocalTime())) {
                r.hora = r.hora.plusHours(12);
            }
        }
        if (r.fecha == null && r.hora != null) {
            r.fecha = r.hora.isAfter(ahora.toLocalTime()) ? ahora.toLocalDate() : ahora.toLocalDate().plusDays(1);
        }
        if (r.fecha == null && r.repite != REPITE_NO) {
            LocalDate hoy = ahora.toLocalDate();
            r.fecha = r.repite == REPITE_DIA ? hoy : r.repite == REPITE_SEMANA ? hoy.plusWeeks(1)
                    : r.repite == REPITE_MES ? hoy.plusMonths(1) : hoy.plusYears(1);
        }
        r.texto = limpiar();
        return r;
    }

    private void repeticion(Resultado r) {
        LocalDate hoy = ahora.toLocalDate();
        Matcher m;
        if ((m = buscar(REP_DIA)) != null) {
            r.repite = REPITE_DIA;
        } else if ((m = buscar(REP_DIA_SEMANA)) != null) {
            r.repite = REPITE_SEMANA;
            r.fecha = hoy.with(TemporalAdjusters.nextOrSame(DayOfWeek.of(SEMANA.indexOf(m.group(1)) + 1)));
        } else if ((m = buscar(REP_SEMANA)) != null) {
            r.repite = REPITE_SEMANA;
        } else if ((m = buscar(REP_MES)) != null) {
            r.repite = REPITE_MES;
        } else if ((m = buscar(REP_ANIO)) != null) {
            r.repite = REPITE_ANIO;
        } else if ((m = buscar(REP_NUM)) != null) {
            LocalDate d = diaDelMes(valor(m.group(1)), hoy);
            if (d == null) return;
            r.repite = REPITE_MES;
            r.fecha = d;
        } else {
            return;
        }
        marcar(m);
    }

    private void relativo(Resultado r) {
        Matcher m = buscar(RELATIVO);
        if (m == null) return;
        if (m.group(1) != null) {
            fijar(r, ahora.plusMinutes(30));
            marcar(m);
            return;
        }
        int n = valor(m.group(2));
        if (n <= 0) return;
        String u = m.group(3);
        boolean media = m.group(4) != null;
        if (u.startsWith("minuto")) fijar(r, ahora.plusMinutes(n));
        else if (u.startsWith("hora")) fijar(r, ahora.plusMinutes(n * 60L + (media ? 30 : 0)));
        else if (u.startsWith("dia")) r.fecha = ahora.toLocalDate().plusDays(n);
        else if (u.startsWith("semana")) r.fecha = ahora.toLocalDate().plusWeeks(n);
        else r.fecha = ahora.toLocalDate().plusMonths(n);
        marcar(m);
    }

    private static void fijar(Resultado r, LocalDateTime t) {
        r.fecha = t.toLocalDate();
        r.hora = t.toLocalTime().withSecond(0).withNano(0);
    }

    private void hora(Resultado r, boolean[] sinPeriodo) {
        for (Pattern p : HORAS) {
            Matcher m = buscar(p);
            while (m != null) {
                LocalTime t = construirHora(m, sinPeriodo);
                if (t != null) {
                    r.hora = t;
                    marcar(m);
                    return;
                }
                m = m.find() ? m : null;
            }
        }
    }

    private LocalTime construirHora(Matcher m, boolean[] sinPeriodo) {
        int h = valor(m.group(1));
        int min = 0;
        if (!vacio(m.group(2))) min = Integer.parseInt(m.group(2));
        else if (!vacio(m.group(3))) min = minutos(m.group(3));
        else if (!vacio(m.group(4))) {
            min = 60 - minutos(m.group(4));
            h = h - 1;
            if (h == 0) h = 12;
        }
        if (h < 0 || h > 24 || min < 0 || min > 59) return null;
        String suf = m.group(5);
        int final24 = aplicarPeriodo(h, suf);
        sinPeriodo[0] = vacio(suf) && h <= 12;
        if (final24 == 24) final24 = 0;
        return LocalTime.of(final24, min);
    }

    private static int aplicarPeriodo(int h, String suf) {
        if (vacio(suf)) {
            if (h >= 1 && h <= 7) return h + 12;  // "a las 3" casi siempre es por la tarde
            return h;
        }
        boolean pm = suf.startsWith("p") && !suf.startsWith("por");
        boolean am = suf.startsWith("a");
        if (am || suf.contains("manana") || suf.contains("madrugada")) return h == 12 ? 0 : h;
        if (pm || suf.contains("tarde")) return h < 12 ? h + 12 : h;
        // noche
        if (h == 12) return 0;
        if (h <= 4) return h;
        return h < 12 ? h + 12 : h;
    }

    private void periodo(Resultado r, boolean[] sinPeriodo) {
        Matcher m = buscar(PERIODO);
        if (m == null) return;
        String nombre = m.group(1) != null ? m.group(1) : m.group(3);
        if (m.group(4) != null) {
            if (r.hora == null) r.hora = LocalTime.NOON;
        } else if (m.group(5) != null) {
            if (r.hora == null) r.hora = LocalTime.of(23, 59);
        } else if (r.hora == null) {
            r.hora = "manana".equals(nombre) ? LocalTime.of(9, 0)
                    : "tarde".equals(nombre) ? LocalTime.of(15, 0) : LocalTime.of(19, 0);
        } else if (sinPeriodo[0]) {
            int original = r.hora.getHour() % 12;
            if ("manana".equals(nombre)) r.hora = r.hora.withHour(original);
            else r.hora = r.hora.withHour(original + 12);
            sinPeriodo[0] = false;
        }
        if (m.group(2) != null && r.fecha == null) r.fecha = ahora.toLocalDate();
        marcar(m);
    }

    private void fecha(Resultado r) {
        LocalDate hoy = ahora.toLocalDate();
        Matcher m;

        if ((m = buscar(BARRAS)) != null) {
            int anio = vacio(m.group(3)) ? -1 : Integer.parseInt(m.group(3));
            if (anio >= 0 && anio < 100) anio += 2000;
            LocalDate d = fechaSegura(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), anio, hoy);
            if (d != null) {
                r.fecha = d;
                marcar(m);
                return;
            }
        }
        if ((m = buscar(COMPLETA)) != null) {
            String mes = m.group(2).equals("setiembre") ? "septiembre" : m.group(2);
            int anio = vacio(m.group(3)) ? -1 : Integer.parseInt(m.group(3));
            LocalDate d = fechaSegura(valor(m.group(1)), MESES.indexOf(mes) + 1, anio, hoy);
            if (d != null) {
                r.fecha = d;
                marcar(m);
                return;
            }
        }
        if ((m = buscar(HOY)) != null) { r.fecha = hoy; marcar(m); return; }
        if ((m = buscar(PASADO)) != null) { r.fecha = hoy.plusDays(2); marcar(m); return; }
        if ((m = buscar(MANANA)) != null) { r.fecha = hoy.plusDays(1); marcar(m); return; }
        if ((m = buscar(DIA_NUM)) != null) {
            LocalDate d = diaDelMes(valor(m.group(1)), hoy);
            if (d != null) { r.fecha = d; marcar(m); return; }
        }
        if ((m = buscar(DIA_SEMANA)) != null) {
            DayOfWeek dia = DayOfWeek.of(SEMANA.indexOf(m.group(2)) + 1);
            String tras = m.group(3);
            if (tras != null && tras.contains("semana")) {
                r.fecha = hoy.with(TemporalAdjusters.next(DayOfWeek.MONDAY)).with(TemporalAdjusters.nextOrSame(dia));
            } else {
                r.fecha = hoy.with(TemporalAdjusters.next(dia));
            }
            marcar(m);
            return;
        }
        if ((m = buscar(SEMANA_PROX)) != null) {
            r.fecha = hoy.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
            marcar(m);
            return;
        }
        if ((m = buscar(MES_PROX)) != null) {
            r.fecha = hoy.plusMonths(1).withDayOfMonth(1);
            marcar(m);
            return;
        }
        m = buscar(SOLO_DIA);
        while (m != null) {
            String g = m.group(1);
            if (!g.equals("un") && !g.equals("una") && !g.equals("uno")) {
                LocalDate d = diaDelMes(valor(g), hoy);
                if (d != null) { r.fecha = d; marcar(m); return; }
            }
            m = m.find() ? m : null;
        }
    }

    private static LocalDate fechaSegura(int dia, int mes, int anio, LocalDate hoy) {
        try {
            if (anio > 0) return LocalDate.of(anio, mes, dia);
            LocalDate d = LocalDate.of(hoy.getYear(), mes, dia);
            return d.isBefore(hoy) ? d.plusYears(1) : d;
        } catch (DateTimeException e) {
            return null;
        }
    }

    private static LocalDate diaDelMes(int dia, LocalDate hoy) {
        if (dia < 1 || dia > 31) return null;
        for (int i = 0; i < 3; i++) {
            LocalDate base = hoy.plusMonths(i).withDayOfMonth(1);
            if (dia <= base.lengthOfMonth()) {
                LocalDate d = base.withDayOfMonth(dia);
                if (!d.isBefore(hoy)) return d;
            }
        }
        return null;
    }

    private String limpiar() {
        StringBuilder queda = new StringBuilder();
        for (int i = 0; i < original.length(); i++) {
            if (!borrar[i]) queda.append(original.charAt(i));
        }
        String t = queda.toString().replaceAll("\\s+", " ").replaceAll(" ([,.;:])", "$1")
                .replaceAll("([,.;:])(?:\\s*[,.;:])+", "$1").trim();
        Matcher m = INICIO.matcher(normalizar(t));
        if (m.find()) t = t.substring(m.end());
        boolean cambio = true;
        while (cambio && !t.isEmpty()) {
            cambio = false;
            Matcher a = ORILLA_INI.matcher(normalizar(t));
            if (a.find()) { t = t.substring(a.end()); cambio = true; }
            Matcher b = ORILLA_FIN.matcher(normalizar(t));
            if (b.find()) { t = t.substring(0, b.start()); cambio = true; }
        }
        if (t.isEmpty()) t = original;
        return t.isEmpty() ? t : t.substring(0, 1).toUpperCase() + t.substring(1);
    }
}
