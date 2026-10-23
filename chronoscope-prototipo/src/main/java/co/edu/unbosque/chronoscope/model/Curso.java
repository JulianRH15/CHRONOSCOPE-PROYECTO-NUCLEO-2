package co.edu.unbosque.chronoscope.model;

/** Curso del plan de estudios con el tipo de aula que requiere (RF-10). */
public class Curso {

    private Long id;
    private String codigo;
    private String nombre;
    private int semestre;
    private String tipoAula;
    private int sesionesSemana;

    public Curso() { }

    public Curso(Long id, String codigo, String nombre, int semestre, String tipoAula, int sesionesSemana) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.semestre = semestre;
        this.tipoAula = tipoAula;
        this.sesionesSemana = sesionesSemana;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public int getSemestre() { return semestre; }
    public void setSemestre(int semestre) { this.semestre = semestre; }

    public String getTipoAula() { return tipoAula; }
    public void setTipoAula(String tipoAula) { this.tipoAula = tipoAula; }

    public int getSesionesSemana() { return sesionesSemana; }
    public void setSesionesSemana(int sesionesSemana) { this.sesionesSemana = sesionesSemana; }
}
