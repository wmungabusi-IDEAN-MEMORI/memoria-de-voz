package com.yuyay.app;

import android.Manifest;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/** Lee y crea eventos en los calendarios del celular, incluido Google Calendar. */
final class Agenda {
    static final class Evento {
        final String titulo;
        final LocalDateTime inicio;
        final boolean todoDia;

        Evento(String titulo, LocalDateTime inicio, boolean todoDia) {
            this.titulo = titulo;
            this.inicio = inicio;
            this.todoDia = todoDia;
        }
    }

    private Agenda() {}

    static boolean puedeLeer(Context c) {
        return c.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED;
    }

    static boolean puedeEscribir(Context c) {
        return c.checkSelfPermission(Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED;
    }

    static List<Evento> eventos(Context c, long desde, long hasta) {
        List<Evento> l = new ArrayList<>();
        if (!puedeLeer(c)) return l;
        Uri.Builder b = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(b, desde);
        ContentUris.appendId(b, hasta);
        String[] cols = {CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.ALL_DAY};
        try (Cursor cur = c.getContentResolver().query(b.build(), cols,
                CalendarContract.Instances.VISIBLE + "=1", null, CalendarContract.Instances.BEGIN + " ASC")) {
            while (cur != null && cur.moveToNext()) {
                String titulo = cur.getString(0);
                long inicio = cur.getLong(1);
                boolean todoDia = cur.getInt(2) == 1;
                LocalDateTime t = todoDia
                        ? Instant.ofEpochMilli(inicio).atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay()
                        : LocalDateTime.ofInstant(Instant.ofEpochMilli(inicio), ZoneId.systemDefault());
                l.add(new Evento(titulo == null || titulo.isEmpty() ? "(Sin título)" : titulo, t, todoDia));
            }
        } catch (RuntimeException e) {
            android.util.Log.e("Yuyay", "No se pudo leer el calendario", e);
        }
        return l;
    }

    private static long calendarioPrincipal(Context c) {
        String[] cols = {CalendarContract.Calendars._ID, CalendarContract.Calendars.IS_PRIMARY,
                CalendarContract.Calendars.ACCOUNT_TYPE};
        long elegido = -1;
        int mejor = -1;
        try (Cursor cur = c.getContentResolver().query(CalendarContract.Calendars.CONTENT_URI, cols,
                CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL + ">=" + CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR
                        + " AND " + CalendarContract.Calendars.VISIBLE + "=1", null, null)) {
            while (cur != null && cur.moveToNext()) {
                int puntos = (cur.getInt(1) == 1 ? 2 : 0) + ("com.google".equals(cur.getString(2)) ? 1 : 0);
                if (puntos > mejor) {
                    mejor = puntos;
                    elegido = cur.getLong(0);
                }
            }
        } catch (RuntimeException e) {
            android.util.Log.e("Yuyay", "No se pudo buscar el calendario", e);
        }
        return elegido;
    }

    /** Crea un evento; devuelve false si no hay permiso o calendario disponible. */
    static boolean crear(Context c, String titulo, LocalDate fecha, LocalDateTime inicio) {
        if (!puedeEscribir(c)) return false;
        long cal = calendarioPrincipal(c);
        if (cal < 0) return false;
        ContentValues v = new ContentValues();
        v.put(CalendarContract.Events.CALENDAR_ID, cal);
        v.put(CalendarContract.Events.TITLE, titulo);
        v.put(CalendarContract.Events.DESCRIPTION, "Creado con Yuyay");
        if (inicio != null) {
            long ms = Recordatorio.aMillis(inicio);
            v.put(CalendarContract.Events.DTSTART, ms);
            v.put(CalendarContract.Events.DTEND, ms + 3_600_000L);
            v.put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
        } else {
            long ms = fecha.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
            v.put(CalendarContract.Events.DTSTART, ms);
            v.put(CalendarContract.Events.DTEND, ms + 86_400_000L);
            v.put(CalendarContract.Events.ALL_DAY, 1);
            v.put(CalendarContract.Events.EVENT_TIMEZONE, "UTC");
        }
        try {
            return c.getContentResolver().insert(CalendarContract.Events.CONTENT_URI, v) != null;
        } catch (RuntimeException e) {
            android.util.Log.e("Yuyay", "No se pudo crear el evento", e);
            return false;
        }
    }
}
