package com.memoriavoz.app;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import java.time.LocalDate;

/** Programa las alarmas y muestra las notificaciones de cada aviso. */
public final class Alarmas {
    public static final String CANAL_AVISOS = "avisos";
    public static final String CANAL_VOZ = "voz";
    public static final String EXTRA_ID = "id";
    public static final String EXTRA_MOMENTO = "momento";
    public static final String EXTRA_VECES = "veces";
    public static final String EXTRA_FRASE = "frase";
    public static final String EXTRA_CUANDO = "cuando";
    public static final String ACCION_POSPONER = "com.memoriavoz.app.POSPONER";
    public static final String ACCION_LISTO = "com.memoriavoz.app.LISTO";
    public static final int MINUTOS_POSPONER = 10;
    private static final long GRACIA_MS = 120_000L;

    private Alarmas() {}

    public static void crearCanales(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        NotificationChannel avisos = new NotificationChannel(CANAL_AVISOS, "Recordatorios",
                NotificationManager.IMPORTANCE_HIGH);
        avisos.enableVibration(true);
        nm.createNotificationChannel(avisos);
        NotificationChannel voz = new NotificationChannel(CANAL_VOZ, "Lectura en voz alta",
                NotificationManager.IMPORTANCE_LOW);
        voz.setSound(null, null);
        nm.createNotificationChannel(voz);
    }

    private static int codigo(long id) {
        return (int) (id & 0x7FFFFFFFL);
    }

    private static PendingIntent intentAlarma(Context c, long id) {
        Intent i = new Intent(c, AlarmaReceiver.class);
        i.setData(Uri.parse("memoriavoz://aviso/" + id));
        i.putExtra(EXTRA_ID, id);
        return PendingIntent.getBroadcast(c, codigo(id), i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent intentAbrir(Context c, int codigo) {
        Intent i = new Intent(c, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(c, codigo, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void programar(Context c, Recordatorio r) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        PendingIntent pi = intentAlarma(c, r.id);
        am.cancel(pi);
        Long proximo = r.proximoAviso(System.currentTimeMillis());
        if (proximo == null) return;
        boolean exacta = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms();
        if (exacta) {
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(proximo, intentAbrir(c, 0)), pi);
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, proximo, pi);
        }
    }

    public static void cancelar(Context c, Recordatorio r) {
        c.getSystemService(AlarmManager.class).cancel(intentAlarma(c, r.id));
        c.getSystemService(NotificationManager.class).cancel(codigo(r.id));
    }

    /** Vuelve a programar todo; hace falta tras reiniciar el celular o si el sistema borró las alarmas. */
    public static void reprogramarTodo(Context c) {
        crearCanales(c);
        long ahora = System.currentTimeMillis();
        for (Recordatorio r : Almacen.todos(c)) {
            if (r.tuvoAvisoPerdido(ahora - GRACIA_MS)) {
                r.ultimoAviso = ahora - GRACIA_MS;
                Almacen.guardar(c, r);
                notificar(c, r, "Aviso que no pudo sonar a tiempo", false);
            }
            if (r.avanzarRepeticion(ahora)) Almacen.guardar(c, r);
            programar(c, r);
        }
    }

    public static String frase(Recordatorio r) {
        if (!r.tieneFecha()) return "Nota guardada: " + r.texto + ".";
        return Fechas.frase(r.texto, r.fechaEvento(), r.conHora, java.time.LocalDateTime.now());
    }

    public static String cuando(Recordatorio r) {
        return r.tieneFecha() ? Fechas.completa(r.fechaEvento(), r.conHora, LocalDate.now()) : "Nota guardada";
    }

    public static Intent intentVoz(Context c, long id, int veces) {
        Intent i = new Intent(c, VozService.class);
        i.setData(Uri.parse("memoriavoz://voz/" + id + "/" + veces));
        i.putExtra(EXTRA_ID, id);
        i.putExtra(EXTRA_VECES, veces);
        return i;
    }

    public static Intent intentVoz(Context c, long id, int veces, String frase) {
        return intentVoz(c, id, veces).putExtra(EXTRA_FRASE, frase);
    }

    private static PendingIntent intentAccion(Context c, long id, String accion) {
        Intent i = new Intent(c, AlarmaReceiver.class);
        i.setAction(accion);
        i.setData(Uri.parse("memoriavoz://accion/" + id + "/" + accion));
        i.putExtra(EXTRA_ID, id);
        return PendingIntent.getBroadcast(c, codigo(id), i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void posponer(Context c, long id) {
        cerrarAviso(c, id);
        Recordatorio r = Almacen.buscar(c, id);
        if (r == null) return;
        r.pospuesto = System.currentTimeMillis() + MINUTOS_POSPONER * 60_000L;
        Almacen.guardar(c, r);
        programar(c, r);
    }

    public static void cerrarAviso(Context c, long id) {
        c.getSystemService(NotificationManager.class).cancel(codigo(id));
        c.stopService(new Intent(c, VozService.class));
    }

    private static Notification.Action accion(String titulo, PendingIntent pi) {
        return new Notification.Action.Builder(null, titulo, pi).build();
    }

    public static void notificar(Context c, Recordatorio r, String titulo, boolean pantallaCompleta) {
        crearCanales(c);
        String cuando = cuando(r);
        PendingIntent escuchar = PendingIntent.getForegroundService(c, codigo(r.id), intentVoz(c, r.id, 1),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = new Notification.Builder(c, CANAL_AVISOS)
                .setSmallIcon(R.drawable.ic_notificacion)
                .setContentTitle(titulo)
                .setContentText(r.texto + " · " + cuando)
                .setStyle(new Notification.BigTextStyle().bigText(r.texto + "\n" + cuando))
                .setCategory(Notification.CATEGORY_REMINDER)
                .setContentIntent(intentAbrir(c, codigo(r.id)))
                .setAutoCancel(true)
                .addAction(accion("Escuchar", escuchar))
                .addAction(accion("Posponer " + MINUTOS_POSPONER + " min", intentAccion(c, r.id, ACCION_POSPONER)))
                .addAction(accion("Listo", intentAccion(c, r.id, ACCION_LISTO)));
        if (pantallaCompleta) {
            Intent p = new Intent(c, AlarmaActivity.class);
            p.setData(Uri.parse("memoriavoz://pantalla/" + r.id));
            p.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            p.putExtra(EXTRA_ID, r.id);
            p.putExtra(EXTRA_CUANDO, cuando);
            b.setCategory(Notification.CATEGORY_ALARM);
            b.setFullScreenIntent(PendingIntent.getActivity(c, codigo(r.id), p,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE), true);
        }
        c.getSystemService(NotificationManager.class).notify(codigo(r.id), b.build());
    }
}
