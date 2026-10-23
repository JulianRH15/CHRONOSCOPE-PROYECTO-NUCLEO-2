package co.edu.unbosque.chronoscope.model;

/** Regla incumplida por una o varias sesiones (restriccion dura o blanda). */
public class Conflicto {

    private String tipo;
    private String severidad;
    private String mensaje;
    private java.util.List<Long> sesionIds;

    public Conflicto() { }

    public Conflicto(String tipo, String severidad, String mensaje, java.util.List<Long> sesionIds) {
        this.tipo = tipo;
        this.severidad = severidad;
        this.mensaje = mensaje;
        this.sesionIds = sesionIds;
    }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getSeveridad() { return severidad; }
    public void setSeveridad(String severidad) { this.severidad = severidad; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public java.util.List<Long> getSesionIds() { return sesionIds; }
    public void setSesionIds(java.util.List<Long> sesionIds) { this.sesionIds = sesionIds; }
}
