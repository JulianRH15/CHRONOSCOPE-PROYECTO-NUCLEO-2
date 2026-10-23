package co.edu.unbosque.chronoscope.model;

/** Aula fisica con capacidad y tipo (RF-13). */
public class Aula {

    private Long id;
    private String codigo;
    private int capacidad;
    private String tipo;

    public Aula() { }

    public Aula(Long id, String codigo, int capacidad, String tipo) {
        this.id = id;
        this.codigo = codigo;
        this.capacidad = capacidad;
        this.tipo = tipo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public int getCapacidad() { return capacidad; }
    public void setCapacidad(int capacidad) { this.capacidad = capacidad; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
}
