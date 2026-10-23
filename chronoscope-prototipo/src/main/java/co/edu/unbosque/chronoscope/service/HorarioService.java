package co.edu.unbosque.chronoscope.service;

import co.edu.unbosque.chronoscope.dto.Dtos.*;
import co.edu.unbosque.chronoscope.model.*;
import co.edu.unbosque.chronoscope.repository.DatosRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Epica 1 - Ajuste y publicacion del horario.
 * RF-25 cuadricula con filtros, RF-26 mover con arrastrar y soltar y ver conflictos,
 * RF-27 deshacer/rehacer, RF-29 aprobar y publicar, RF-31 versiones.
 */
@Service
public class HorarioService {

    public static final String BORRADOR = "BORRADOR";
    public static final String PUBLICADO = "PUBLICADO";
    public static final String ARCHIVADO = "ARCHIVADO";

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final DatosRepository repo;
    private final ValidadorConflictos validador;
    private final MotorHorarios motor;

    private int version;
    private String estado;
    private String descripcionBorrador;
    private final Deque<Movimiento> pilaDeshacer = new ArrayDeque<>();
    private final Deque<Movimiento> pilaRehacer = new ArrayDeque<>();
    private final List<VersionHorario> historial = new ArrayList<>();
    private List<Sesion> ultimaPublicada = List.of();
    private String mensaje;

    public HorarioService(DatosRepository repo, ValidadorConflictos validador, MotorHorarios motor) {
        this.repo = repo;
        this.validador = validador;
        this.motor = motor;
    }

    @PostConstruct
    public synchronized void iniciar() {
        pilaDeshacer.clear();
        pilaRehacer.clear();
        historial.clear();
        ultimaPublicada = List.of();
        motor.generar();
        version = 1;
        estado = BORRADOR;
        descripcionBorrador = "Generado por el motor";
        mensaje = "Horario generado sin conflictos duros. Revísalo y ajústalo antes de publicar.";
    }

    public synchronized void reiniciarDemo() {
        repo.reiniciar();
        iniciar();
    }

    // ---------------------------------------------------------------- consultas

    public synchronized HorarioVista vista() {
        List<Conflicto> conflictos = validador.validar(repo.sesiones());
        return new HorarioVista(version, estado, sesionesVista(), conflictos,
                !pilaDeshacer.isEmpty(), !pilaRehacer.isEmpty(), cambiadasVsPublicado(), mensaje);
    }

    public synchronized List<VersionResumen> versiones() {
        List<VersionResumen> lista = new ArrayList<>();
        for (VersionHorario v : historial) {
            lista.add(new VersionResumen(v.getNumero(), v.getEstado(), v.getFecha().format(FORMATO),
                    v.getDescripcion(), v.getSesiones().size()));
        }
        if (BORRADOR.equals(estado)) {
            lista.add(new VersionResumen(version, BORRADOR, "en edición", descripcionBorrador,
                    repo.sesiones().size()));
        }
        Collections.reverse(lista);
        return lista;
    }

    public String getEstado() { return estado; }

    public int getVersion() { return version; }

    // ---------------------------------------------------------------- comandos

    /** Mueve una sesion a otro dia/franja y/o aula. Los conflictos se muestran, no se bloquean. */
    public synchronized HorarioVista mover(Long sesionId, MoverSesionRequest req) {
        Sesion s = repo.sesion(sesionId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "La sesión " + sesionId + " no existe"));
        String dia = req.dia() != null ? req.dia() : s.getDia();
        int franja = req.franja() != null ? req.franja() : s.getFranja();
        Long aula = req.aulaId() != null ? req.aulaId() : s.getAulaId();
        if (!Calendario.DIAS.contains(dia) || franja < 0 || franja >= Calendario.FRANJAS.size()
                || repo.aula(aula) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Día, franja o aula no válidos");
        }
        if (dia.equals(s.getDia()) && franja == s.getFranja() && aula.equals(s.getAulaId())) {
            mensaje = "La sesión quedó en el mismo lugar.";
            return vista();
        }
        abrirBorradorSiEstaPublicado("Ajuste manual del Comité de Área");
        Movimiento m = new Movimiento(s.getId(), s.getDia(), s.getFranja(), s.getAulaId(), dia, franja, aula);
        aplicar(m, false);
        pilaDeshacer.push(m);
        pilaRehacer.clear();
        mensaje = describir(m, "Moviste");
        return vista();
    }

    public synchronized HorarioVista deshacer() {
        if (pilaDeshacer.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No hay cambios para deshacer");
        }
        Movimiento m = pilaDeshacer.pop();
        aplicar(m, true);
        pilaRehacer.push(m);
        Grupo g = repo.grupo(repo.sesion(m.sesionId()).orElseThrow().getGrupoId());
        mensaje = "Se deshizo el cambio: " + g.getCodigo() + " volvió a "
                + Calendario.etiqueta(m.diaAntes(), m.franjaAntes()) + ".";
        return vista();
    }

    public synchronized HorarioVista rehacer() {
        if (pilaRehacer.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No hay cambios para rehacer");
        }
        Movimiento m = pilaRehacer.pop();
        aplicar(m, false);
        pilaDeshacer.push(m);
        mensaje = describir(m, "Se rehízo:");
        return vista();
    }

    /** Aprueba y publica. Solo se puede si no hay conflictos duros (compuerta de calidad). */
    public synchronized HorarioVista aprobarYPublicar() {
        if (PUBLICADO.equals(estado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La versión " + version + " ya está publicada");
        }
        List<Conflicto> conflictos = validador.validar(repo.sesiones());
        long duros = conflictos.stream().filter(c -> ValidadorConflictos.DURA.equals(c.getSeveridad())).count();
        if (duros > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede publicar: hay " + duros + " conflicto(s) duro(s) sin resolver");
        }
        for (VersionHorario v : historial) {
            if (PUBLICADO.equals(v.getEstado())) v.setEstado(ARCHIVADO);
        }
        List<Sesion> copia = repo.sesiones().stream().map(Sesion::copia).toList();
        historial.add(new VersionHorario(version, PUBLICADO, LocalDateTime.now(), descripcionBorrador, copia));
        ultimaPublicada = copia;
        estado = PUBLICADO;
        pilaDeshacer.clear();
        pilaRehacer.clear();
        repo.notificar("Docentes del programa (" + repo.docentes().size() + ")",
                "Horario 2026-2 publicado (versión " + version + ")",
                "La Dirección de Programa aprobó y publicó la versión " + version
                        + " del horario. Puede consultarlo en ChronoScope.");
        mensaje = "Versión " + version + " aprobada y publicada. Se notificó a los docentes.";
        return vista();
    }

    /** Vuelve a generar el borrador con el motor (solo en borrador). */
    public synchronized HorarioVista regenerar() {
        if (PUBLICADO.equals(estado)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El horario está publicado; para cambios use el ajuste manual o el cierre de grupos");
        }
        motor.generar();
        pilaDeshacer.clear();
        pilaRehacer.clear();
        descripcionBorrador = "Generado por el motor";
        mensaje = "Se volvió a generar el horario.";
        return vista();
    }

    // ---------------------------------------------------------------- usados por el cierre de grupos

    /** Si lo que se va a modificar es la version publicada, se crea un nuevo borrador. */
    public synchronized void abrirBorradorSiEstaPublicado(String descripcion) {
        if (PUBLICADO.equals(estado)) {
            version++;
            estado = BORRADOR;
            descripcionBorrador = descripcion;
            pilaDeshacer.clear();
            pilaRehacer.clear();
        }
    }

    public synchronized void limpiarPilas() {
        pilaDeshacer.clear();
        pilaRehacer.clear();
    }

    public synchronized void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    // ---------------------------------------------------------------- auxiliares

    private void aplicar(Movimiento m, boolean alReves) {
        Sesion s = repo.sesion(m.sesionId()).orElseThrow();
        s.setDia(alReves ? m.diaAntes() : m.diaDespues());
        s.setFranja(alReves ? m.franjaAntes() : m.franjaDespues());
        s.setAulaId(alReves ? m.aulaAntes() : m.aulaDespues());
    }

    private String describir(Movimiento m, String verbo) {
        Sesion s = repo.sesion(m.sesionId()).orElseThrow();
        Grupo g = repo.grupo(s.getGrupoId());
        long n = validador.validar(repo.sesiones()).stream()
                .filter(c -> ValidadorConflictos.DURA.equals(c.getSeveridad()))
                .filter(c -> c.getSesionIds().contains(s.getId()))
                .count();
        String destino = Calendario.etiqueta(m.diaDespues(), m.franjaDespues()) + " en "
                + repo.aula(m.aulaDespues()).getCodigo();
        return verbo + " " + g.getCodigo() + " a " + destino + ". "
                + (n == 0 ? "Sin conflictos." : "Genera " + n + " conflicto(s); puedes deshacer el cambio.");
    }

    private List<SesionVista> sesionesVista() {
        List<SesionVista> lista = new ArrayList<>();
        for (Sesion s : repo.sesiones()) {
            Grupo g = repo.grupo(s.getGrupoId());
            Curso c = repo.cursoDeGrupo(g);
            Docente d = repo.docente(g.getDocenteId());
            Aula a = repo.aula(s.getAulaId());
            lista.add(new SesionVista(s.getId(), g.getId(), g.getCodigo(), c.getCodigo(), c.getNombre(),
                    c.getSemestre(), d.getId(), d.getNombre(), a.getId(), a.getCodigo(), a.getCapacidad(),
                    g.getInscritos(), s.getDia(), s.getFranja()));
        }
        return lista;
    }

    private Set<Long> cambiadasVsPublicado() {
        Set<Long> cambiadas = new HashSet<>();
        if (ultimaPublicada.isEmpty() || PUBLICADO.equals(estado)) return cambiadas;
        Map<Long, Sesion> publicadas = new HashMap<>();
        for (Sesion p : ultimaPublicada) publicadas.put(p.getId(), p);
        for (Sesion s : repo.sesiones()) {
            Sesion p = publicadas.get(s.getId());
            if (p == null || !p.getDia().equals(s.getDia()) || p.getFranja() != s.getFranja()
                    || !p.getAulaId().equals(s.getAulaId())) {
                cambiadas.add(s.getId());
            }
        }
        return cambiadas;
    }
}
