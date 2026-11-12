package com.yuyay.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

/** Guarda todo únicamente en el celular. */
public final class Almacen {
    private static final String ARCHIVO = "memoria";
    private static final String CLAVE = "recordatorios";

    private Almacen() {}

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE);
    }

    public static synchronized List<Recordatorio> todos(Context c) {
        List<Recordatorio> lista = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(prefs(c).getString(CLAVE, "[]"));
            for (int i = 0; i < a.length(); i++) lista.add(Recordatorio.deJson(a.getJSONObject(i)));
        } catch (JSONException e) {
            android.util.Log.e("MemoriaVoz", "Datos dañados", e);
        }
        return lista;
    }

    public static synchronized Recordatorio buscar(Context c, long id) {
        for (Recordatorio r : todos(c)) {
            if (r.id == id) return r;
        }
        return null;
    }

    public static synchronized void guardar(Context c, Recordatorio nuevo) {
        List<Recordatorio> lista = todos(c);
        boolean cambiado = false;
        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).id == nuevo.id) {
                lista.set(i, nuevo);
                cambiado = true;
                break;
            }
        }
        if (!cambiado) lista.add(nuevo);
        escribir(c, lista);
    }

    public static synchronized void borrar(Context c, long id) {
        List<Recordatorio> lista = todos(c);
        lista.removeIf(r -> r.id == id);
        escribir(c, lista);
    }

    private static void escribir(Context c, List<Recordatorio> lista) {
        try {
            JSONArray a = new JSONArray();
            for (Recordatorio r : lista) a.put(r.aJson());
            prefs(c).edit().putString(CLAVE, a.toString()).commit();
        } catch (JSONException e) {
            android.util.Log.e("MemoriaVoz", "No se pudo guardar", e);
        }
    }
}
