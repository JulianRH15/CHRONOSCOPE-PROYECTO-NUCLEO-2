package co.edu.unbosque.chronoscope.model;

/** Grupo de un curso, con su docente asignado e inscritos (RF-11, RF-12). */
public class Grupo {

    private Long id;
    private Long cursoId;
    private String codigo;
    private int numero;
    private Long docenteId;
    private int inscritos;
    private EstadoGrupo estado;

    public Grupo() { }

    public Grupo(Long id, Long cursoId, String codigo, int numero, Long docenteId, int inscritos, EstadoGrupo estado) {
        this.id = id;
        this.cursoId = cursoId;
        this.codigo = codigo;
        this.numero = numero;
        this.docenteId = docenteId;
        this.inscritos = inscritos;
        this.estado = estado;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCursoId() { return cursoId; }
    public void setCursoId(Long cursoId) { this.cursoId = cursoId; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public int getNumero() { return numero; }
    public void setNumero(int numero) { this.numero = numero; }

    public Long getDocenteId() { return docenteId; }
    public void setDocenteId(Long docenteId) { this.docenteId = docenteId; }

    public int getInscritos() { return inscritos; }
    public void setInscritos(int inscritos) { this.inscritos = inscritos; }

    public EstadoGrupo getEstado() { return estado; }
    public void setEstado(EstadoGrupo estado) { this.estado = estado; }
}
