package com.memoriavoz.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Llega cuando toca un aviso: deja la notificación, programa el siguiente y arranca la voz. */
public class AlarmaReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        long id = intent.getLongExtra(Alarmas.EXTRA_ID, -1);
        Recordatorio r = Almacen.buscar(c, id);
        if (r == null) return;
        r.ultimoAviso = System.currentTimeMillis();
        Almacen.guardar(c, r);
        Alarmas.notificar(c, r, "Recordatorio");
        Alarmas.programar(c, r);
        try {
            c.startForegroundService(Alarmas.intentVoz(c, id, 2));
        } catch (RuntimeException e) {
            // La notificación ya quedó visible aunque el sistema no permita leerla en voz alta.
            android.util.Log.e("MemoriaVoz", "No se pudo iniciar la voz", e);
        }
    }
}
