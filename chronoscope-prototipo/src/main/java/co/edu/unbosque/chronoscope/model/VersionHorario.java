package co.edu.unbosque.chronoscope.model;

/** Version guardada del horario (RF-31). */
public class VersionHorario {

    private int numero;
    private String estado;
    private java.time.LocalDateTime fecha;
    private String descripcion;
    private java.util.List<Sesion> sesiones;

    public VersionHorario() { }

    public VersionHorario(int numero, String estado, java.time.LocalDateTime fecha, String descripcion, java.util.List<Sesion> sesiones) {
        this.numero = numero;
        this.estado = estado;
        this.fecha = fecha;
        this.descripcion = descripcion;
        this.sesiones = sesiones;
    }

    public int getNumero() { return numero; }
    public void setNumero(int numero) { this.numero = numero; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public java.time.LocalDateTime getFecha() { return fecha; }
    public void setFecha(java.time.LocalDateTime fecha) { this.fecha = fecha; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public java.util.List<Sesion> getSesiones() { return sesiones; }
    public void setSesiones(java.util.List<Sesion> sesiones) { this.sesiones = sesiones; }
}
