package com.yuyay.app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Conversa con Claude para entender lo que la persona dice y convertirlo en acciones. */
final class Asistente {
    private static final String MODELO = "claude-sonnet-5-5";
    private static final Locale ES = new Locale("es", "EC");
    private static final ExecutorService HILO = Executors.newSingleThreadExecutor();
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    private Asistente() {}

    static void preguntar(Context ctx, List<Chat.Mensaje> historial, Consumer<RespuestaIA> listo) {
        Context c = ctx.getApplicationContext();
        Handler principal = new Handler(Looper.getMainLooper());
        HILO.execute(() -> {
            RespuestaIA r;
            try {
                r = llamar(c, historial);
            } catch (Exception e) {
                android.util.Log.e("Yuyay", "Fallo al hablar con Claude", e);
                r = RespuestaIA.conError("No pude conectarme con Claude. Revisa tu internet e inténtalo otra vez.");
            }
            RespuestaIA fin = r;
            principal.post(() -> listo.accept(fin));
        });
    }

    private static String nombreRepite(int r) {
        switch (r) {
            case ParserFechas.REPITE_DIA: return "diario";
            case ParserFechas.REPITE_SEMANA: return "semanal";
            case ParserFechas.REPITE_MES: return "mensual";
            case ParserFechas.REPITE_ANIO: return "anual";
            default: return "no";
        }
    }

    private static int codigoRepite(String s) {
        switch (s) {
            case "diario": return ParserFechas.REPITE_DIA;
            case "semanal": return ParserFechas.REPITE_SEMANA;
            case "mensual": return ParserFechas.REPITE_MES;
            case "anual": return ParserFechas.REPITE_ANIO;
            default: return ParserFechas.REPITE_NO;
        }
    }

    private static String instrucciones(Context c) throws JSONException {
        LocalDateTime ahora = LocalDateTime.now();
        JSONArray memoria = new JSONArray();
        List<Recordatorio> todos = Almacen.todos(c);
        todos.sort(Comparator.comparingLong((Recordatorio r) -> -r.creado));
        for (Recordatorio r : todos.subList(0, Math.min(300, todos.size()))) {
            JSONObject o = new JSONObject().put("id", r.id).put("texto", r.texto);
            o.put("fecha", r.tieneFecha() ? r.fechaEvento().toLocalDate().toString() : JSONObject.NULL);
            o.put("hora", r.conHora ? r.fechaEvento().toLocalTime().format(HM) : JSONObject.NULL);
            o.put("repite", nombreRepite(r.repite));
            if (!r.lista.isEmpty()) o.put("lista", r.lista).put("hecho", r.hecho);
            memoria.put(o);
        }
        String calendario = "no conectado";
        if (Agenda.puedeLeer(c)) {
            long desde = System.currentTimeMillis() - 86_400_000L;
            JSONArray ev = new JSONArray();
            for (Agenda.Evento e : Agenda.eventos(c, desde, desde + 61L * 86_400_000L)) {
                if (ev.length() >= 120) break;
                ev.put(new JSONObject().put("titulo", e.titulo).put("fecha", e.inicio.toLocalDate().toString())
                        .put("hora", e.todoDia ? JSONObject.NULL : e.inicio.toLocalTime().format(HM)));
            }
            calendario = ev.toString();
        }
        return "Eres Yuyay, la asistente de memoria y recordatorios de una persona en Ecuador. Hablas español "
                + "sencillo y cálido. Hoy es " + ahora.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", ES))
                + " (" + ahora.toLocalDate() + "), hora local " + ahora.format(HM) + ".\n\n"
                + "Recibes lo que la persona dice o escribe; a veces es una nota de voz transcrita con errores. "
                + "Tu trabajo: guardar lo que quiere recordar, crear recordatorios con fecha y hora, manejar listas "
                + "y responder preguntas usando su memoria y su calendario.\n\n"
                + "Responde SOLO con un objeto JSON, sin texto antes ni después, con esta forma:\n"
                + "{\"respuesta\": \"...\", \"acciones\": [ ... ]}\n\n"
                + "\"respuesta\": una o dos frases cortas que se leerán en voz alta. Sin emojis, sin markdown y sin "
                + "listas. Confirma lo que hiciste (por ejemplo: «Listo, te recuerdo la reunión con Juan mañana a las "
                + "3 de la tarde») o responde la pregunta.\n\n"
                + "Acciones posibles; usa una por cada tarea si la persona menciona varias:\n"
                + "- {\"tipo\":\"recordatorio\",\"texto\":\"...\",\"fecha\":\"AAAA-MM-DD\",\"hora\":\"HH:MM\" o null,"
                + "\"repite\":\"no\"|\"diario\"|\"semanal\"|\"mensual\"|\"anual\",\"google\":true|false}\n"
                + "- {\"tipo\":\"nota\",\"texto\":\"...\"}  información sin fecha que quiere recordar (claves, datos, ideas).\n"
                + "- {\"tipo\":\"lista\",\"lista\":\"Compras\",\"items\":[\"leche\",\"pan\"]}  agregar elementos a una lista.\n"
                + "- {\"tipo\":\"marcar\",\"id\":123}  marcar como hecho un elemento de una lista.\n"
                + "- {\"tipo\":\"borrar\",\"id\":123}\n"
                + "- {\"tipo\":\"mover\",\"id\":123,\"fecha\":\"AAAA-MM-DD\",\"hora\":\"HH:MM\" o null}  cambiar fecha u hora.\n\n"
                + "Reglas:\n"
                + "- \"texto\" es breve y claro, sin la fecha ni la hora (por ejemplo «Reunión con Juan»).\n"
                + "- Calcula las fechas relativas («mañana», «el viernes», «en 3 días») desde hoy. Una hora del 1 al 7 "
                + "sin aclarar es de la tarde.\n"
                + "- Si tiene fecha pero no hora, usa \"hora\": null.\n"
                + "- \"google\": true solo si menciona su calendario o agenda, o si es una cita o reunión con hora.\n"
                + "- Si hace una pregunta, responde con su MEMORIA y su CALENDARIO, sin acciones. Si no está, dilo.\n"
                + "- Para borrar, marcar o mover usa el \"id\" exacto de MEMORIA. Si no está claro cuál es, pregunta "
                + "y no hagas acciones.\n"
                + "- No inventes datos.\n\n"
                + "MEMORIA: " + memoria + "\n\nCALENDARIO (próximos 60 días): " + calendario;
    }

    private static JSONArray mensajes(List<Chat.Mensaje> historial) throws JSONException {
        List<Chat.Mensaje> ultimos = historial.subList(Math.max(0, historial.size() - 12), historial.size());
        List<boolean[]> roles = new ArrayList<>();
        List<StringBuilder> textos = new ArrayList<>();
        for (Chat.Mensaje m : ultimos) {
            if (textos.isEmpty() && !m.mio) continue;
            if (!roles.isEmpty() && roles.get(roles.size() - 1)[0] == m.mio) {
                textos.get(textos.size() - 1).append("\n").append(m.texto);
            } else {
                roles.add(new boolean[]{m.mio});
                textos.add(new StringBuilder(m.texto));
            }
        }
        JSONArray a = new JSONArray();
        for (int i = 0; i < textos.size(); i++) {
            a.put(new JSONObject().put("role", roles.get(i)[0] ? "user" : "assistant")
                    .put("content", textos.get(i).toString()));
        }
        return a;
    }

    private static RespuestaIA llamar(Context c, List<Chat.Mensaje> historial) throws Exception {
        JSONObject cuerpo = new JSONObject()
                .put("model", MODELO)
                .put("max_tokens", 1500)
                .put("system", instrucciones(c))
                .put("messages", mensajes(historial));
        HttpURLConnection con = (HttpURLConnection) new URL("https://api.anthropic.com/v1/messages").openConnection();
        con.setRequestMethod("POST");
        con.setConnectTimeout(15000);
        con.setReadTimeout(90000);
        con.setDoOutput(true);
        con.setRequestProperty("content-type", "application/json");
        con.setRequestProperty("x-api-key", Ajustes.clave(c));
        con.setRequestProperty("anthropic-version", "2023-06-01");
        try (OutputStream o = con.getOutputStream()) {
            o.write(cuerpo.toString().getBytes(StandardCharsets.UTF_8));
        }
        int codigo = con.getResponseCode();
        InputStream in = codigo < 300 ? con.getInputStream() : con.getErrorStream();
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        if (in != null) {
            byte[] b = new byte[8192];
            int n;
            while ((n = in.read(b)) > 0) buf.write(b, 0, n);
            in.close();
        }
        String texto = buf.toString("UTF-8");
        con.disconnect();
        if (codigo == 401) return RespuestaIA.conError("Tu clave de Claude no es válida. Revísala en Ajustes.");
        if (codigo == 429) return RespuestaIA.conError("Claude está recibiendo demasiadas solicitudes. Espera un momento.");
        if (codigo >= 300) {
            String detalle = texto.toLowerCase(Locale.ROOT);
            if (detalle.contains("credit") || detalle.contains("billing")) {
                return RespuestaIA.conError("Tu cuenta de Claude no tiene saldo. Recárgala en console.anthropic.com.");
            }
            android.util.Log.e("Yuyay", "Claude respondió " + codigo + ": " + texto);
            return RespuestaIA.conError("Claude no pudo responder (error " + codigo + "). Inténtalo otra vez.");
        }
        JSONArray contenido = new JSONObject(texto).optJSONArray("content");
        StringBuilder s = new StringBuilder();
        for (int i = 0; contenido != null && i < contenido.length(); i++) {
            JSONObject b = contenido.getJSONObject(i);
            if ("text".equals(b.optString("type"))) s.append(b.optString("text"));
        }
        RespuestaIA r = RespuestaIA.interpretar(s.toString());
        if (r.respuesta.isEmpty()) r.respuesta = "Listo.";
        return r;
    }

    private static LocalDate fecha(String s) {
        try {
            return s.isEmpty() ? null : LocalDate.parse(s);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static LocalTime hora(String s) {
        try {
            return s.isEmpty() ? null : LocalTime.parse(s.length() == 4 ? "0" + s : s);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String capitalizar(String s) {
        return s.isEmpty() ? s : s.substring(0, 1).toUpperCase(ES) + s.substring(1);
    }

    /** Ejecuta las acciones y devuelve un resumen de lo hecho, una línea por acción. */
    static List<String> aplicar(Context c, List<RespuestaIA.Accion> acciones) {
        List<String> hecho = new ArrayList<>();
        long base = System.currentTimeMillis();
        int n = 0;
        for (RespuestaIA.Accion a : acciones) {
            switch (a.tipo) {
                case "recordatorio": {
                    LocalDate f = fecha(a.fecha);
                    if (a.texto.isEmpty()) break;
                    if (f == null) {
                        Recordatorio r = nuevo(base, n++, a.texto);
                        Almacen.guardar(c, r);
                        hecho.add("Guardado: " + a.texto);
                        break;
                    }
                    LocalTime h = hora(a.hora);
                    Recordatorio r = nuevo(base, n++, a.texto);
                    r.conHora = h != null;
                    r.exacta = h != null;
                    r.repite = codigoRepite(a.repite);
                    r.diaBase = f.getDayOfMonth();
                    r.evento = Recordatorio.aMillis(f.atTime(h != null ? h : LocalTime.of(9, 0)));
                    if (r.evento <= base) {
                        if (r.repite == ParserFechas.REPITE_NO) {
                            hecho.add("No guardé «" + a.texto + "» porque esa fecha ya pasó");
                            break;
                        }
                        r.avanzarRepeticion(base);
                    }
                    Almacen.guardar(c, r);
                    Alarmas.programar(c, r);
                    String linea = "Recordatorio: " + a.texto + ", " + Alarmas.cuando(r);
                    if (r.repite != ParserFechas.REPITE_NO) linea += " (" + Fechas.repeticion(r.repite).toLowerCase(ES) + ")";
                    if ((a.google || (Ajustes.copiarGoogle(c) && h != null))
                            && Agenda.crear(c, a.texto, f, h != null ? f.atTime(h) : null)) {
                        linea += ", también en tu calendario";
                    }
                    hecho.add(linea);
                    break;
                }
                case "nota": {
                    if (a.texto.isEmpty()) break;
                    Almacen.guardar(c, nuevo(base, n++, a.texto));
                    hecho.add("Guardado: " + a.texto);
                    break;
                }
                case "lista": {
                    String nombre = capitalizar(a.lista.isEmpty() ? "Pendientes" : a.lista);
                    for (String item : a.items) {
                        Recordatorio r = nuevo(base, n++, capitalizar(item));
                        r.lista = nombre;
                        Almacen.guardar(c, r);
                    }
                    if (!a.items.isEmpty()) hecho.add("Agregado a " + nombre + ": " + String.join(", ", a.items));
                    break;
                }
                case "marcar": {
                    Recordatorio r = Almacen.buscar(c, a.id);
                    if (r == null) break;
                    r.hecho = true;
                    Almacen.guardar(c, r);
                    hecho.add("Hecho: " + r.texto);
                    break;
                }
                case "borrar": {
                    Recordatorio r = Almacen.buscar(c, a.id);
                    if (r == null) break;
                    Alarmas.cancelar(c, r);
                    Almacen.borrar(c, r.id);
                    hecho.add("Borrado: " + r.texto);
                    break;
                }
                case "mover": {
                    Recordatorio r = Almacen.buscar(c, a.id);
                    LocalDate f = fecha(a.fecha);
                    if (r == null || f == null) break;
                    LocalTime h = hora(a.hora);
                    r.conHora = h != null;
                    r.exacta = r.exacta && h != null;
                    r.diaBase = f.getDayOfMonth();
                    r.evento = Recordatorio.aMillis(f.atTime(h != null ? h : LocalTime.of(9, 0)));
                    r.creado = base;
                    r.ultimoAviso = 0;
                    r.pospuesto = 0;
                    if (r.evento <= base && r.repite == ParserFechas.REPITE_NO) {
                        hecho.add("No moví «" + r.texto + "» porque esa fecha ya pasó");
                        break;
                    }
                    r.avanzarRepeticion(base);
                    Almacen.guardar(c, r);
                    Alarmas.programar(c, r);
                    hecho.add("Movido: " + r.texto + ", " + Alarmas.cuando(r));
                    break;
                }
                default:
                    break;
            }
        }
        return hecho;
    }

    private static Recordatorio nuevo(long base, int n, String texto) {
        Recordatorio r = new Recordatorio();
        r.id = base + n;
        r.texto = texto;
        r.creado = base;
        return r;
    }
}
