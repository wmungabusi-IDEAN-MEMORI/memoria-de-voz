package com.memoriavoz.app;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.TreeSet;

import org.json.JSONException;
import org.json.JSONObject;

/** Una nota guardada. Si evento es 0, es información sin fecha y no genera avisos. */
public final class Recordatorio {
    public long id;
    public String texto = "";
    public long evento;
    public boolean conHora;
    public boolean diaAntes = true;
    public boolean mismoDia = true;
    public boolean exacta;
    public long creado;
    public long ultimoAviso;

    public boolean tieneFecha() {
        return evento > 0;
    }

    public LocalDateTime fechaEvento() {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(evento), ZoneId.systemDefault());
    }

    public static long aMillis(LocalDateTime t) {
        return t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /** Momentos en que debe sonar, ya sin los que quedaban antes de crearlo. */
    public TreeSet<Long> momentos() {
        TreeSet<Long> m = new TreeSet<>();
        if (!tieneFecha()) return m;
        LocalDateTime ev = fechaEvento();
        if (diaAntes) m.add(aMillis(ev.minusDays(1)));
        if (mismoDia) m.add(aMillis(conHora ? ev.minusHours(1) : ev));
        if (exacta) m.add(aMillis(ev));
        m.removeIf(x -> x <= creado);
        if (m.isEmpty() && evento > creado) m.add(evento);
        return m;
    }

    public Long proximoAviso(long ahora) {
        for (long x : momentos()) {
            if (x > ahora && x > ultimoAviso) return x;
        }
        return null;
    }

    public boolean tuvoAvisoPerdido(long limite) {
        for (long x : momentos()) {
            if (x > ultimoAviso && x < limite) return true;
        }
        return false;
    }

    JSONObject aJson() throws JSONException {
        return new JSONObject().put("id", id).put("texto", texto).put("evento", evento).put("conHora", conHora)
                .put("diaAntes", diaAntes).put("mismoDia", mismoDia).put("exacta", exacta).put("creado", creado)
                .put("ultimoAviso", ultimoAviso);
    }

    static Recordatorio deJson(JSONObject o) throws JSONException {
        Recordatorio r = new Recordatorio();
        r.id = o.getLong("id");
        r.texto = o.getString("texto");
        r.evento = o.getLong("evento");
        r.conHora = o.getBoolean("conHora");
        r.diaAntes = o.getBoolean("diaAntes");
        r.mismoDia = o.getBoolean("mismoDia");
        r.exacta = o.getBoolean("exacta");
        r.creado = o.getLong("creado");
        r.ultimoAviso = o.getLong("ultimoAviso");
        return r;
    }
}
