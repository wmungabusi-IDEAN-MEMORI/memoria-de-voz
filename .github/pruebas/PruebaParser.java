import com.memoriavoz.app.*;
import java.time.*;

public class PruebaParser {
    static int mal = 0;
    static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 9, 12, 30); // lunes

    static void c(String f, String fecha, String hora, String texto) {
        ParserFechas.Resultado r = ParserFechas.analizar(f, AHORA);
        String gf = String.valueOf(r.fecha), gh = String.valueOf(r.hora);
        boolean ok = gf.equals(fecha) && gh.equals(hora) && r.texto.equals(texto);
        if (!ok) mal++;
        System.out.println((ok ? "ok   " : "MAL  ") + f + " => " + gf + " " + gh + " | " + r.texto
                + (ok ? "" : "   ESPERADO: " + fecha + " " + hora + " | " + texto));
    }

    static void rep(String f, String fecha, String hora, int repite, String texto) {
        ParserFechas.Resultado r = ParserFechas.analizar(f, AHORA);
        String gf = String.valueOf(r.fecha), gh = String.valueOf(r.hora);
        boolean ok = gf.equals(fecha) && gh.equals(hora) && r.repite == repite && r.texto.equals(texto);
        if (!ok) mal++;
        System.out.println((ok ? "ok   " : "MAL  ") + f + " => " + gf + " " + gh + " rep=" + r.repite + " | " + r.texto
                + (ok ? "" : "   ESPERADO: " + fecha + " " + hora + " rep=" + repite + " | " + texto));
    }

    public static void main(String[] a) {
        c("Recuérdame que tengo reunión con Juan mañana a las 3 de la tarde", "2026-11-10", "15:00", "Tengo reunión con Juan");
        c("mañana por la mañana llevar el carro al taller", "2026-11-10", "09:00", "Llevar el carro al taller");
        c("Mañana a las 8 de la mañana cita con el dentista", "2026-11-10", "08:00", "Cita con el dentista");
        c("pasado mañana a las 10 pagar el arriendo", "2026-11-11", "10:00", "Pagar el arriendo");
        c("el viernes a las tres y media reunión con el cliente", "2026-11-13", "15:30", "Reunión con el cliente");
        c("el 15 de noviembre cumpleaños de mamá", "2026-11-15", "null", "Cumpleaños de mamá");
        c("reunión el quince de noviembre a las 15h30 en la oficina", "2026-11-15", "15:30", "Reunión en la oficina");
        c("el 20 de enero renovar la matrícula", "2027-01-20", "null", "Renovar la matrícula");
        c("primero de diciembre pagar impuestos", "2026-12-01", "null", "Pagar impuestos");
        c("en 3 días llamar al contador", "2026-11-12", "null", "Llamar al contador");
        c("en media hora sacar la comida del horno", "2026-11-09", "13:00", "Sacar la comida del horno");
        c("dentro de dos semanas revisar el contrato", "2026-11-23", "null", "Revisar el contrato");
        c("en una hora y media llamar a Pedro", "2026-11-09", "14:00", "Llamar a Pedro");
        c("hoy a las 5 p. m. entregar el diseño", "2026-11-09", "17:00", "Entregar el diseño");
        c("a las 9 enviar la cotización", "2026-11-09", "21:00", "Enviar la cotización");
        c("a las 11 de la mañana enviar la cotización", "2026-11-10", "11:00", "Enviar la cotización");
        c("esta tarde comprar tinta", "2026-11-09", "15:00", "Comprar tinta");
        c("esta noche llamar a Ana", "2026-11-09", "19:00", "Llamar a Ana");
        c("el lunes de la próxima semana a la una reunión", "2026-11-16", "13:00", "Reunión");
        c("el próximo martes a las 4 menos cuarto visita a Ambato", "2026-11-10", "15:45", "Visita a Ambato");
        c("la próxima semana comprar repuestos", "2026-11-16", "null", "Comprar repuestos");
        c("el 25 pagar a proveedores", "2026-11-25", "null", "Pagar a proveedores");
        c("el 3 pagar la luz", "2026-12-03", "null", "Pagar la luz");
        c("el viernes 20 entregar pedido", "2026-11-20", "null", "Entregar pedido");
        c("15/11 a las 16:00 reunión", "2026-11-15", "16:00", "Reunión");
        c("25/12/2026 cena de navidad", "2026-12-25", "null", "Cena de navidad");
        c("al mediodía almuerzo con Carla", "2026-11-10", "12:00", "Almuerzo con Carla");
        c("mañana en la tarde a las 4 reunión", "2026-11-10", "16:00", "Reunión");
        c("mañana a las 12:15 pm junta", "2026-11-10", "12:15", "Junta");
        c("La clave del wifi de la oficina es naranja", "null", "null", "La clave del wifi de la oficina es naranja");
        c("Compra las dos camisas azules", "null", "null", "Compra las dos camisas azules");
        c("el miércoles", "2026-11-11", "null", "El miércoles");
        rep("Cada lunes a las 8 revisar inventario", "2026-11-09", "20:00", 2, "Revisar inventario");
        rep("todos los días a las 7 de la mañana tomar agua", "2026-11-10", "07:00", 1, "Tomar agua");
        rep("todos los 15 pagar a proveedores", "2026-11-15", "null", 3, "Pagar a proveedores");
        rep("cada mes el 20 pagar el internet", "2026-11-20", "null", 3, "Pagar el internet");
        rep("cada año el 3 de marzo aniversario de la empresa", "2027-03-03", "null", 4, "Aniversario de la empresa");
        rep("cada 3 días regar las plantas", "2026-11-12", "null", 0, "Cada regar las plantas");
        System.out.println(mal == 0 ? "TODO OK" : "FALLOS: " + mal);
        LocalDateTime ev = LocalDateTime.of(2026, 11, 10, 15, 0);
        System.out.println(Fechas.frase("Reunión con Juan", ev, true, AHORA));
        System.out.println(Fechas.frase("Reunión con Juan", ev, true, ev.minusHours(1)));
        System.out.println(Fechas.frase("Reunión con Juan", ev, true, ev));
        System.out.println(Fechas.frase("Pagar", LocalDateTime.of(2026,11,20,9,0), false, AHORA));
        System.out.println(Fechas.hora(LocalTime.of(13, 30)) + " / " + Fechas.hora(LocalTime.of(12, 0)) + " / " + Fechas.hora(LocalTime.of(0, 5)));
        System.exit(mal == 0 ? 0 : 1);
    }
}
