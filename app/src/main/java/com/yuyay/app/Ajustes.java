package com.yuyay.app;

import android.content.Context;
import android.content.SharedPreferences;

/** Preferencias de la persona; la clave de Claude se guarda solo en este celular. */
final class Ajustes {
    private Ajustes() {}

    private static SharedPreferences p(Context c) {
        return c.getApplicationContext().getSharedPreferences("ajustes", Context.MODE_PRIVATE);
    }

    static String clave(Context c) {
        return p(c).getString("clave_claude", "").trim();
    }

    static boolean tieneClave(Context c) {
        return !clave(c).isEmpty();
    }

    static boolean copiarGoogle(Context c) {
        return p(c).getBoolean("copiar_google", true);
    }

    static void guardar(Context c, String clave, boolean copiarGoogle) {
        p(c).edit().putString("clave_claude", clave.trim()).putBoolean("copiar_google", copiarGoogle).apply();
    }
}
