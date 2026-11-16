package com.yuyay.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/** Mantiene guardados los feriados y fechas especiales del próximo año, con aviso un día antes. */
final class Festivos {
    static final String FERIADO = "feriado";
    static final String ESPECIAL = "especial";

    private Festivos() {}

    private static SharedPreferences p(Context c) {
        return c.getApplicationContext().getSharedPreferences("festivos", Context.MODE_PRIVATE);
    }

    static void ocultar(Context c, long id) {
        Set<String> s = new HashSet<>(p(c).getStringSet("ocultos", new HashSet<>()));
        s.add(String.valueOf(id));
        p(c).edit().putStringSet("ocultos", s).apply();
    }

    static synchronized void sincronizar(Context c) {
        boolean fer = Ajustes.feriados(c);
        boolean esp = Ajustes.especiales(c);
        Set<String> ocultos = p(c).getStringSet("ocultos", new HashSet<>());
        LocalDate hoy = LocalDate.now();
        long limitePasado = Recordatorio.aMillis(hoy.minusDays(3).atStartOfDay());

        for (Recordatorio r : Almacen.todos(c)) {
            if (r.origen.isEmpty()) continue;
            boolean activo = FERIADO.equals(r.origen) ? fer : esp;
            if (!activo || r.evento < limitePasado) {
                Alarmas.cancelar(c, r);
                Almacen.borrar(c, r.id);
            }
        }
        long ahora = System.currentTimeMillis();
        for (Feriados.Dia d : Feriados.entre(hoy, hoy.plusDays(400), fer, esp)) {
            long id = Feriados.id(d);
            if (ocultos.contains(String.valueOf(id)) || Almacen.buscar(c, id) != null) continue;
            Recordatorio r = new Recordatorio();
            r.id = id;
            r.texto = d.titulo();
            r.origen = d.feriado ? FERIADO : ESPECIAL;
            r.evento = Recordatorio.aMillis(d.fecha.atTime(9, 0));
            r.creado = Math.min(ahora, Recordatorio.aMillis(d.fecha.minusDays(1).atTime(8, 0)));
            r.diaBase = d.fecha.getDayOfMonth();
            r.ultimoAviso = ahora;
            r.diaAntes = true;
            r.mismoDia = false;
            r.exacta = false;
            Almacen.guardar(c, r);
            Alarmas.programar(c, r);
        }
    }
}
