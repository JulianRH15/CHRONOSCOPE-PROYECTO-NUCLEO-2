package co.edu.unbosque.chronoscope.model;

/** Una sesion pone un grupo en un dia, una franja y un aula. Es el centro del horario. */
public class Sesion {

    private Long id;
    private Long grupoId;
    private String dia;
    private int franja;
    private Long aulaId;

    public Sesion() { }

    public Sesion(Long id, Long grupoId, String dia, int franja, Long aulaId) {
        this.id = id;
        this.grupoId = grupoId;
        this.dia = dia;
        this.franja = franja;
        this.aulaId = aulaId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getGrupoId() { return grupoId; }
    public void setGrupoId(Long grupoId) { this.grupoId = grupoId; }

    public String getDia() { return dia; }
    public void setDia(String dia) { this.dia = dia; }

    public int getFranja() { return franja; }
    public void setFranja(int franja) { this.franja = franja; }

    public Long getAulaId() { return aulaId; }
    public void setAulaId(Long aulaId) { this.aulaId = aulaId; }

    /** Copia independiente, usada para versiones y para deshacer. */
    public Sesion copia() { return new Sesion(id, grupoId, dia, franja, aulaId); }
}
