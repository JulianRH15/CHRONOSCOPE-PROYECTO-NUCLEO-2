package co.edu.unbosque.chronoscope.model;

/** Docente del programa con su dedicacion y disponibilidad por franja (RF-05, RF-07, RF-08). */
public class Docente {

    private Long id;
    private String nombre;
    private String correo;
    private String dedicacion;
    private int maxHorasSemana;
    private java.util.Set<String> noDisponible;
    private java.util.Set<String> preferidas;

    public Docente() { }

    public Docente(Long id, String nombre, String correo, String dedicacion, int maxHorasSemana, java.util.Set<String> noDisponible, java.util.Set<String> preferidas) {
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.dedicacion = dedicacion;
        this.maxHorasSemana = maxHorasSemana;
        this.noDisponible = noDisponible;
        this.preferidas = preferidas;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getDedicacion() { return dedicacion; }
    public void setDedicacion(String dedicacion) { this.dedicacion = dedicacion; }

    public int getMaxHorasSemana() { return maxHorasSemana; }
    public void setMaxHorasSemana(int maxHorasSemana) { this.maxHorasSemana = maxHorasSemana; }

    public java.util.Set<String> getNoDisponible() { return noDisponible; }
    public void setNoDisponible(java.util.Set<String> noDisponible) { this.noDisponible = noDisponible; }

    public java.util.Set<String> getPreferidas() { return preferidas; }
    public void setPreferidas(java.util.Set<String> preferidas) { this.preferidas = preferidas; }
}
