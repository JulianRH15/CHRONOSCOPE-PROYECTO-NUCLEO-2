package co.edu.unbosque.chronoscope.model;

/** Correo simulado enviado a docentes y estudiantes (RF-29, RF-34). */
public class Notificacion {

    private Long id;
    private java.time.LocalDateTime fecha;
    private String para;
    private String asunto;
    private String cuerpo;

    public Notificacion() { }

    public Notificacion(Long id, java.time.LocalDateTime fecha, String para, String asunto, String cuerpo) {
        this.id = id;
        this.fecha = fecha;
        this.para = para;
        this.asunto = asunto;
        this.cuerpo = cuerpo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public java.time.LocalDateTime getFecha() { return fecha; }
    public void setFecha(java.time.LocalDateTime fecha) { this.fecha = fecha; }

    public String getPara() { return para; }
    public void setPara(String para) { this.para = para; }

    public String getAsunto() { return asunto; }
    public void setAsunto(String asunto) { this.asunto = asunto; }

    public String getCuerpo() { return cuerpo; }
    public void setCuerpo(String cuerpo) { this.cuerpo = cuerpo; }
}
