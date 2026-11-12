package com.yuyay.app;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Interpreta la respuesta en JSON que devuelve Claude. No depende de Android. */
public final class RespuestaIA {
    public static final class Accion {
        public String tipo = "";
        public String texto = "";
        public String fecha = "";
        public String hora = "";
        public String repite = "no";
        public String lista = "";
        public long id;
        public boolean google;
        public final List<String> items = new ArrayList<>();
    }

    public String respuesta = "";
    public String error;
    public final List<Accion> acciones = new ArrayList<>();

    public static RespuestaIA conError(String mensaje) {
        RespuestaIA r = new RespuestaIA();
        r.error = mensaje;
        return r;
    }

    private static String cadena(JSONObject o, String k) {
        return o.isNull(k) ? "" : o.optString(k, "").trim();
    }

    public static RespuestaIA interpretar(String s) {
        RespuestaIA r = new RespuestaIA();
        if (s == null) s = "";
        int a = s.indexOf('{');
        int b = s.lastIndexOf('}');
        if (a < 0 || b <= a) {
            r.respuesta = s.trim();
            return r;
        }
        try {
            JSONObject o = new JSONObject(s.substring(a, b + 1));
            r.respuesta = cadena(o, "respuesta");
            JSONArray arr = o.optJSONArray("acciones");
            for (int i = 0; arr != null && i < arr.length(); i++) {
                JSONObject x = arr.optJSONObject(i);
                if (x == null) continue;
                Accion ac = new Accion();
                ac.tipo = cadena(x, "tipo").toLowerCase();
                ac.texto = cadena(x, "texto");
                ac.fecha = cadena(x, "fecha");
                ac.hora = cadena(x, "hora");
                ac.lista = cadena(x, "lista");
                String rep = cadena(x, "repite");
                ac.repite = rep.isEmpty() ? "no" : rep.toLowerCase();
                ac.id = x.optLong("id", 0);
                ac.google = x.optBoolean("google", false);
                JSONArray it = x.optJSONArray("items");
                for (int j = 0; it != null && j < it.length(); j++) {
                    String v = it.isNull(j) ? "" : it.optString(j, "").trim();
                    if (!v.isEmpty()) ac.items.add(v);
                }
                if (!ac.tipo.isEmpty()) r.acciones.add(ac);
            }
        } catch (JSONException e) {
            r.respuesta = s.trim();
        }
        return r;
    }
}
