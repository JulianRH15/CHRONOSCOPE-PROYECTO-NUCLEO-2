package co.edu.unbosque.chronoscope.model;

import java.util.List;

/**
 * Dias y franjas del periodo (RF-17). En el producto final son parametros
 * configurables; en el prototipo se dejan fijos para simplificar.
 */
public final class Calendario {

    public static final List<String> DIAS = List.of("LUN", "MAR", "MIE", "JUE", "VIE", "SAB");

    public static final List<String> FRANJAS = List.of(
            "08:00 - 10:00",
            "10:00 - 12:00",
            "12:00 - 13:00",
            "13:00 - 15:00",
            "15:00 - 17:00",
            "17:00 - 19:00",
            "19:00 - 21:00");

    /** Franja de almuerzo bloqueada para todos (restriccion dura). */
    public static final int FRANJA_ALMUERZO = 2;

    private Calendario() { }

    /** El sabado solo hay clase hasta las 13:00. */
    public static boolean esBloqueada(String dia, int franja) {
        if (franja == FRANJA_ALMUERZO) {
            return true;
        }
        return "SAB".equals(dia) && franja > 1;
    }

    public static String clave(String dia, int franja) {
        return dia + "-" + franja;
    }

    public static String etiqueta(String dia, int franja) {
        return dia + " " + FRANJAS.get(franja);
    }
}
