package com.yuyay.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Llega cuando toca un aviso o cuando se usa un botón de la notificación. */
public class AlarmaReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        long id = intent.getLongExtra(Alarmas.EXTRA_ID, -1);
        String accion = intent.getAction();
        if (Alarmas.ACCION_POSPONER.equals(accion)) {
            Alarmas.posponer(c, id);
            return;
        }
        if (Alarmas.ACCION_LISTO.equals(accion)) {
            Alarmas.cerrarAviso(c, id);
            return;
        }

        Recordatorio r = Almacen.buscar(c, id);
        if (r == null) return;
        long ahora = System.currentTimeMillis();
        boolean eraPospuesto = r.pospuesto > 0 && r.pospuesto <= ahora;
        String frase = eraPospuesto ? "Recordatorio pospuesto: " + r.texto + "." : Alarmas.frase(r);

        // La notificación se arma antes de avanzar, para mostrar la fecha de esta ocurrencia.
        Alarmas.notificar(c, r, "Recordatorio", true);
        r.ultimoAviso = ahora;
        if (eraPospuesto) r.pospuesto = 0;
        r.avanzarRepeticion(ahora);
        Almacen.guardar(c, r);
        Alarmas.programar(c, r);
        try {
            c.startForegroundService(Alarmas.intentVoz(c, id, 2, frase));
        } catch (RuntimeException e) {
            // La notificación ya quedó visible aunque el sistema no permita leerla en voz alta.
            android.util.Log.e("MemoriaVoz", "No se pudo iniciar la voz", e);
        }
    }
}
