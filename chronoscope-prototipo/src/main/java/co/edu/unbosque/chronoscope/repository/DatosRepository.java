package co.edu.unbosque.chronoscope.repository;

import co.edu.unbosque.chronoscope.model.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Repositorio en memoria con datos de prueba del Programa de Ingenieria de Sistemas.
 * En el producto final se reemplaza por repositorios sobre la base de datos;
 * los servicios no cambian porque solo dependen de estos metodos.
 */
@Component
public class DatosRepository {

    private final Map<Long, Docente> docentes = new LinkedHashMap<>();
    private final Map<Long, Aula> aulas = new LinkedHashMap<>();
    private final Map<Long, Curso> cursos = new LinkedHashMap<>();
    private final Map<Long, Grupo> grupos = new LinkedHashMap<>();
    private final List<Sesion> sesiones = new ArrayList<>();
    private final List<Notificacion> notificaciones = new ArrayList<>();
    private long secuenciaSesion = 1;
    private long secuenciaNotificacion = 1;

    public DatosRepository() {
        cargarDatosIniciales();
    }

    /** Vuelve todo al estado inicial (boton "Reiniciar demo"). */
    public synchronized void reiniciar() {
        docentes.clear();
        aulas.clear();
        cursos.clear();
        grupos.clear();
        sesiones.clear();
        notificaciones.clear();
        secuenciaSesion = 1;
        secuenciaNotificacion = 1;
        cargarDatosIniciales();
    }

    private void cargarDatosIniciales() {
        // ---------- Docentes ----------
        Set<String> soloNoches = new HashSet<>();
        for (String d : List.of("LUN", "MAR", "MIE", "JUE", "VIE")) {
            for (int f = 0; f <= 4; f++) soloNoches.add(Calendario.clave(d, f));
        }
        Set<String> sinMarJue = new HashSet<>();
        for (String d : List.of("MAR", "JUE")) {
            for (int f = 0; f < Calendario.FRANJAS.size(); f++) sinMarJue.add(Calendario.clave(d, f));
        }
        Set<String> sinViernes = new HashSet<>();
        for (int f = 0; f < Calendario.FRANJAS.size(); f++) sinViernes.add(Calendario.clave("VIE", f));

        docente(1L, "Ana María Torres", "Tiempo completo", 16, Set.of());
        docente(2L, "Carlos Pineda", "Tiempo completo", 16, sinViernes);
        docente(3L, "Luisa Fernanda Gómez", "Tiempo completo", 16, Set.of());
        docente(4L, "Jorge Ramírez", "Medio tiempo", 8, sinMarJue);
        docente(5L, "Diana Castillo", "Tiempo completo", 16, Set.of());
        docente(6L, "Andrés Molina", "Cátedra", 8, soloNoches);
        docente(7L, "Paola Rincón", "Tiempo completo", 16, Set.of());
        docente(8L, "Felipe Arango", "Medio tiempo", 10, Set.of());
        docente(9L, "Natalia Suárez", "Tiempo completo", 16, Set.of());
        docente(10L, "Mauricio Vega", "Cátedra", 8, soloNoches);

        // ---------- Aulas ----------
        aula(1L, "A-201", 40, "SALON");
        aula(2L, "A-202", 40, "SALON");
        aula(3L, "A-305", 45, "SALON");
        aula(4L, "B-101", 50, "SALON");
        aula(5L, "LAB-1", 30, "LABORATORIO");
        aula(6L, "LAB-2", 35, "LABORATORIO");
        aula(7L, "LAB-3", 40, "LABORATORIO");
        aula(8L, "AUD-1", 80, "AUDITORIO");

        // ---------- Cursos ----------
        curso(1L, "CAL1", "Cálculo Diferencial", 1, "SALON");
        curso(2L, "PRB", "Programación Básica", 1, "LABORATORIO");
        curso(3L, "LMD", "Lógica y Matemáticas Discretas", 1, "SALON");
        curso(4L, "INI", "Introducción a la Ingeniería", 1, "SALON");
        curso(5L, "CAL2", "Cálculo Integral", 2, "SALON");
        curso(6L, "POO", "Programación Orientada a Objetos", 2, "LABORATORIO");
        curso(7L, "ALG", "Álgebra Lineal", 2, "SALON");
        curso(8L, "FIS1", "Física Mecánica", 2, "SALON");
        curso(9L, "EDD", "Estructuras de Datos", 3, "LABORATORIO");
        curso(10L, "BD1", "Bases de Datos I", 3, "LABORATORIO");
        curso(11L, "PYE", "Probabilidad y Estadística", 3, "SALON");
        curso(12L, "CAL3", "Cálculo Vectorial", 3, "SALON");
        curso(13L, "ISW1", "Ingeniería de Software I", 4, "SALON");
        curso(14L, "BD2", "Bases de Datos II", 4, "LABORATORIO");
        curso(15L, "RED", "Redes de Computadores", 4, "LABORATORIO");
        curso(16L, "MYS", "Modelos y Simulación", 4, "SALON");

        // ---------- Grupos (curso, numero, docente, inscritos planeados) ----------
        grupo(1L, 1L, 1, 1L, 35);
        grupo(2L, 1L, 2, 1L, 32);
        grupo(3L, 2L, 1, 2L, 28);
        grupo(4L, 2L, 2, 7L, 30);
        grupo(5L, 3L, 1, 3L, 38);
        grupo(6L, 4L, 1, 6L, 43);
        grupo(7L, 5L, 1, 1L, 30);
        grupo(8L, 6L, 1, 2L, 30);
        grupo(9L, 6L, 2, 8L, 26);
        grupo(10L, 7L, 1, 3L, 36);
        grupo(11L, 8L, 1, 4L, 34);
        grupo(12L, 9L, 1, 7L, 32);
        grupo(13L, 10L, 1, 5L, 28);
        grupo(14L, 10L, 2, 10L, 22);
        grupo(15L, 11L, 1, 9L, 38);
        grupo(16L, 12L, 1, 1L, 36);
        grupo(17L, 12L, 2, 4L, 20);
        grupo(18L, 13L, 1, 5L, 35);
        grupo(19L, 14L, 1, 8L, 30);
        grupo(20L, 15L, 1, 9L, 25);
        grupo(21L, 16L, 1, 10L, 24);
        grupo(22L, 16L, 2, 6L, 18);
    }

    private void docente(Long id, String nombre, String dedicacion, int maxHoras, Set<String> noDisponible) {
        String correo = nombre.toLowerCase()
                .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
                .replace(" ", ".") + "@unbosque.edu.co";
        docentes.put(id, new Docente(id, nombre, correo, dedicacion, maxHoras,
                new HashSet<>(noDisponible), new HashSet<>()));
    }

    private void aula(Long id, String codigo, int capacidad, String tipo) {
        aulas.put(id, new Aula(id, codigo, capacidad, tipo));
    }

    private void curso(Long id, String codigo, String nombre, int semestre, String tipoAula) {
        cursos.put(id, new Curso(id, codigo, nombre, semestre, tipoAula, 2));
    }

    private void grupo(Long id, Long cursoId, int numero, Long docenteId, int inscritos) {
        String codigo = cursos.get(cursoId).getCodigo() + "-0" + numero;
        grupos.put(id, new Grupo(id, cursoId, codigo, numero, docenteId, inscritos, EstadoGrupo.ABIERTO));
    }

    // ---------- Consultas ----------
    public Collection<Docente> docentes() { return docentes.values(); }
    public Collection<Aula> aulas() { return aulas.values(); }
    public Collection<Curso> cursos() { return cursos.values(); }
    public Collection<Grupo> grupos() { return grupos.values(); }

    public Docente docente(Long id) { return docentes.get(id); }
    public Aula aula(Long id) { return aulas.get(id); }
    public Curso curso(Long id) { return cursos.get(id); }
    public Grupo grupo(Long id) { return grupos.get(id); }

    public Optional<Grupo> grupoPorCodigo(String codigo) {
        return grupos.values().stream().filter(g -> g.getCodigo().equalsIgnoreCase(codigo.trim())).findFirst();
    }

    public Curso cursoDeGrupo(Grupo g) { return cursos.get(g.getCursoId()); }

    // ---------- Sesiones ----------
    public List<Sesion> sesiones() { return sesiones; }

    public Optional<Sesion> sesion(Long id) {
        return sesiones.stream().filter(s -> s.getId().equals(id)).findFirst();
    }

    public Sesion nuevaSesion(Long grupoId, String dia, int franja, Long aulaId) {
        Sesion s = new Sesion(secuenciaSesion++, grupoId, dia, franja, aulaId);
        sesiones.add(s);
        return s;
    }

    public void reemplazarSesiones(List<Sesion> nuevas) {
        sesiones.clear();
        for (Sesion s : nuevas) sesiones.add(s.copia());
    }

    // ---------- Notificaciones ----------
    public List<Notificacion> notificaciones() { return notificaciones; }

    public void notificar(String para, String asunto, String cuerpo) {
        notificaciones.add(0, new Notificacion(secuenciaNotificacion++, java.time.LocalDateTime.now(),
                para, asunto, cuerpo));
    }
}
