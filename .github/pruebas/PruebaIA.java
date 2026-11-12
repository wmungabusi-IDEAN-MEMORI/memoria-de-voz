import com.yuyay.app.RespuestaIA;

public class PruebaIA {
    static int mal = 0;

    static void ok(boolean c, String que) {
        if (!c) mal++;
        System.out.println((c ? "ok   " : "MAL  ") + que);
    }

    public static void main(String[] a) {
        RespuestaIA r = RespuestaIA.interpretar("```json\n{\"respuesta\":\"Listo, anotado.\",\"acciones\":["
                + "{\"tipo\":\"recordatorio\",\"texto\":\"Pagar la luz\",\"fecha\":\"2026-11-13\",\"hora\":null,"
                + "\"repite\":\"no\",\"google\":false},"
                + "{\"tipo\":\"lista\",\"lista\":\"compras\",\"items\":[\"leche\",\"pan\",null]},"
                + "{\"tipo\":\"Borrar\",\"id\":1762970000000},{\"sin\":\"tipo\"}]}\n```");
        ok(r.respuesta.equals("Listo, anotado."), "respuesta");
        ok(r.acciones.size() == 3, "tres acciones validas: " + r.acciones.size());
        ok(r.acciones.get(0).hora.isEmpty() && r.acciones.get(0).fecha.equals("2026-11-13"), "hora null queda vacia");
        ok(r.acciones.get(1).items.size() == 2 && r.acciones.get(1).lista.equals("compras"), "items de lista");
        ok(r.acciones.get(2).tipo.equals("borrar") && r.acciones.get(2).id == 1762970000000L, "borrar con id");
        RespuestaIA t = RespuestaIA.interpretar("Hola, no entendí bien.");
        ok(t.respuesta.equals("Hola, no entendí bien.") && t.acciones.isEmpty(), "texto sin JSON");
        RespuestaIA m = RespuestaIA.interpretar("{respuesta: roto");
        ok(m.acciones.isEmpty(), "JSON roto no falla");
        System.out.println(mal == 0 ? "TODO OK" : "FALLOS: " + mal);
        System.exit(mal == 0 ? 0 : 1);
    }
}
