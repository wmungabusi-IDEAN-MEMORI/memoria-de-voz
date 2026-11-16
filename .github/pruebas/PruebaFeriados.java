import com.yuyay.app.Feriados;
import java.time.LocalDate;
import java.util.List;

public class PruebaFeriados {
    static int mal = 0;

    static void ok(boolean c, String que) {
        if (!c) mal++;
        System.out.println((c ? "ok   " : "MAL  ") + que);
    }

    static boolean hay(List<Feriados.Dia> l, String fecha, String nombre) {
        for (Feriados.Dia d : l) if (d.fecha.toString().equals(fecha) && d.nombre.equals(nombre)) return true;
        return false;
    }

    public static void main(String[] a) {
        for (int y = 2026; y <= 2045; y++) {
            List<Feriados.Dia> l = Feriados.feriados(y);
            ok(l.size() == 11, y + ": 11 dias de feriado (" + l.size() + ")");
            for (Feriados.Dia d : l) {
                boolean fijo = d.nombre.equals("Año Nuevo") || d.nombre.equals("Navidad");
                ok(fijo || d.fecha.getDayOfWeek().getValue() <= 5, y + " " + d.nombre + " cae entre semana: " + d.fecha);
            }
        }
        ok(Feriados.pascua(2026).toString().equals("2026-04-05"), "Pascua 2026");
        ok(Feriados.pascua(2031).toString().equals("2031-04-13"), "Pascua 2031");
        List<Feriados.Dia> l27 = Feriados.feriados(2027);
        ok(hay(l27, "2027-04-30", "Día del Trabajo"), "Trabajo 2027 trasladado");
        ok(hay(l27, "2027-11-02", "Independencia de Cuenca"), "Cuenca 2027");
        ok(hay(Feriados.feriados(2031), "2031-02-24", "Carnaval"), "Carnaval 2031 calculado");
        List<Feriados.Dia> e = Feriados.especiales(2027);
        ok(e.get(2).fecha.toString().equals("2027-05-09"), "Dia de la Madre 2027: " + e.get(2).fecha);
        ok(e.get(4).fecha.toString().equals("2027-06-20"), "Dia del Padre 2027: " + e.get(4).fecha);
        List<Feriados.Dia> prox = Feriados.entre(LocalDate.of(2026, 11, 16), LocalDate.of(2027, 1, 2), true, true);
        ok(prox.size() == 4 && prox.get(0).nombre.equals("Nochebuena"), "proximas: " + prox.size());
        ok(Feriados.feriados(2026).get(5).titulo().equals("Feriado: Batalla del Pichincha (se traslada del domingo 24)"),
                Feriados.feriados(2026).get(5).titulo());
        System.out.println(mal == 0 ? "TODO OK" : "FALLOS: " + mal);
        System.exit(mal == 0 ? 0 : 1);
    }
}
