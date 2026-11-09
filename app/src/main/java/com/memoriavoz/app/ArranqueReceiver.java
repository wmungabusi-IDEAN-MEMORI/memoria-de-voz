package com.memoriavoz.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Android borra las alarmas al apagar el celular; aquí se restauran. */
public class ArranqueReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent intent) {
        Alarmas.reprogramarTodo(c);
    }
}
