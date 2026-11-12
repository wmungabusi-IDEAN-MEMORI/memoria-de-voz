package com.yuyay.app;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Historial de la conversación con Yuyay, guardado en el celular. */
final class Chat {
    static final class Mensaje {
        final boolean mio;
        final String texto;
        final long tiempo;

        Mensaje(boolean mio, String texto, long tiempo) {
            this.mio = mio;
            this.texto = texto;
            this.tiempo = tiempo;
        }
    }

    private static final int MAXIMO = 80;

    private Chat() {}

    private static SharedPreferences p(Context c) {
        return c.getApplicationContext().getSharedPreferences("chat", Context.MODE_PRIVATE);
    }

    static synchronized List<Mensaje> todos(Context c) {
        List<Mensaje> l = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(p(c).getString("mensajes", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                l.add(new Mensaje(o.getBoolean("mio"), o.getString("texto"), o.getLong("tiempo")));
            }
        } catch (JSONException e) {
            android.util.Log.e("Yuyay", "Chat dañado", e);
        }
        return l;
    }

    static synchronized void agregar(Context c, boolean mio, String texto) {
        List<Mensaje> l = todos(c);
        l.add(new Mensaje(mio, texto, System.currentTimeMillis()));
        while (l.size() > MAXIMO) l.remove(0);
        try {
            JSONArray a = new JSONArray();
            for (Mensaje m : l) {
                a.put(new JSONObject().put("mio", m.mio).put("texto", m.texto).put("tiempo", m.tiempo));
            }
            p(c).edit().putString("mensajes", a.toString()).apply();
        } catch (JSONException e) {
            android.util.Log.e("Yuyay", "No se pudo guardar el chat", e);
        }
    }

    static synchronized void borrar(Context c) {
        p(c).edit().remove("mensajes").apply();
    }
}
