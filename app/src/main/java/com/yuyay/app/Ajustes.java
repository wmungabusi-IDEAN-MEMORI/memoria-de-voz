package com.yuyay.app;

import android.content.Context;
import android.content.SharedPreferences;

/** Preferencias de la persona; la clave de Claude se guarda solo en este celular. */
final class Ajustes {
    private Ajustes() {}

    private static SharedPreferences p(Context c) {
        return c.getApplicationContext().getSharedPreferences("ajustes", Context.MODE_PRIVATE);
    }

    static final String GEMINI = "gemini";
    static final String CLAUDE = "claude";

    static String proveedor(Context c) {
        return p(c).getString("proveedor", GEMINI);
    }

    static String nombreProveedor(Context c) {
        return GEMINI.equals(proveedor(c)) ? "Gemini" : "Claude";
    }

    static String clave(Context c) {
        return claveDe(c, proveedor(c));
    }

    static String claveDe(Context c, String proveedor) {
        return p(c).getString(GEMINI.equals(proveedor) ? "clave_gemini" : "clave_claude", "").trim();
    }

    static void guardarIA(Context c, String proveedor, String clave) {
        SharedPreferences.Editor e = p(c).edit().putString("proveedor", proveedor);
        if (!clave.trim().isEmpty()) {
            e.putString(GEMINI.equals(proveedor) ? "clave_gemini" : "clave_claude", clave.trim());
        }
        e.apply();
    }

    static boolean tieneClave(Context c) {
        return !clave(c).isEmpty();
    }

    static boolean copiarGoogle(Context c) {
        return p(c).getBoolean("copiar_google", true);
    }

    static boolean feriados(Context c) {
        return p(c).getBoolean("feriados", true);
    }

    static boolean especiales(Context c) {
        return p(c).getBoolean("especiales", true);
    }

    static void guardarFestivos(Context c, boolean feriados, boolean especiales) {
        p(c).edit().putBoolean("feriados", feriados).putBoolean("especiales", especiales).apply();
    }

    static void guardarGoogle(Context c, boolean copiarGoogle) {
        p(c).edit().putBoolean("copiar_google", copiarGoogle).apply();
    }
}
