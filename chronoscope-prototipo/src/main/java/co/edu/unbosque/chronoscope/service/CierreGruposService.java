package co.edu.unbosque.chronoscope.service;

import co.edu.unbosque.chronoscope.dto.Dtos.*;
import co.edu.unbosque.chronoscope.model.*;
import co.edu.unbosque.chronoscope.repository.DatosRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Epica 2 - Cierre de grupos por baja matricula.
 * RF-32 importar inscritos, RF-33 detectar grupos bajo el minimo o sobre el cupo,
 * RF-34 cerrar y notificar, RF-35 re-optimizar solo lo afectado, RF-36 conservar lo demas.
 */
@Service
public class CierreGruposService {

    private final DatosRepository repo;
    private final HorarioService horario;
    private final MotorHorarios motor;
    private final ValidadorConflictos validador;

    @Value("${chronoscope.cupo-maximo:40}")
    private int cupoMaximo;
    @Value("${chronoscope.tolerancia-porcentaje:10}")
    private int tolerancia;
    @Value("${chronoscope.minimo-estudiantes:12}")
    private int minimo;

    private boolean inscritosCargados;
    private String archivo;
    private ResultadoCierre ultimoResultado;
    private final List<String[]> pendientes = new ArrayList<>(); // {para, asunto, cuerpo}

    public CierreGruposService(DatosRepository repo, HorarioService horario, MotorHorarios motor,
                               ValidadorConflictos validador) {
        this.repo = repo;
        this.horario = horario;
        this.motor = motor;
        this.validador = validador;
    }

    public synchronized void reiniciar() {
        inscritosCargados = false;
        archivo = null;
        ultimoResultado = null;
        pendientes.clear();
    }

    public int cupoConTolerancia() {
        return cupoMaximo + (cupoMaximo * tolerancia) / 100;
    }

    public Parametros parametros() {
        return new Parametros(cupoMaximo, tolerancia, minimo, Calendario.DIAS, Calendario.FRANJAS,
                Calendario.FRANJA_ALMUERZO);
    }

    // ---------------------------------------------------------------- RF-32 importar inscritos

    /**
     * Lee un CSV "codigo_grupo,inscritos". Si alguna fila tiene error no se aplica nada
     * (todo o nada), igual que en el documento de arquitectura.
     */
    public synchronized AnalisisCierre cargarInscritos(InputStream csv, String nombreArchivo) {
        Map<Grupo, Integer> nuevos = new LinkedHashMap<>();
        List<String> errores = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(csv, StandardCharsets.UTF_8))) {
            String linea;
            int n = 0;
            while ((linea = br.readLine()) != null) {
                n++;
                linea = linea.replace("﻿", "").trim();
                if (linea.isEmpty() || (n == 1 && linea.toLowerCase().startsWith("codigo"))) continue;
                String[] partes = linea.split("[,;]");
                if (partes.length < 2) {
                    errores.add("Fila " + n + ": se esperaban 2 columnas");
                    continue;
                }
                Optional<Grupo> g = repo.grupoPorCodigo(partes[0]);
                if (g.isEmpty()) {
                    errores.add("Fila " + n + ": el grupo '" + partes[0].trim() + "' no existe");
                    continue;
                }
                try {
                    int inscritos = Integer.parseInt(partes[1].trim());
                    if (inscritos < 0) throw new NumberFormatException();
                    nuevos.put(g.get(), inscritos);
                } catch (NumberFormatException e) {
                    errores.add("Fila " + n + ": '" + partes[1].trim() + "' no es un número válido");
                }
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo leer el archivo");
        }
        if (!errores.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El archivo tiene errores y no se cargó: " + String.join("; ", errores));
        }
        if (nuevos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo no tiene filas de datos");
        }
        nuevos.forEach(Grupo::setInscritos);
        inscritosCargados = true;
        archivo = nombreArchivo + " (" + nuevos.size() + " grupos)";
        return analisis();
    }

    public AnalisisCierre cargarEjemplo() {
        InputStream in = getClass().getResourceAsStream("/matricula_inscritos_2026-2.csv");
        return cargarInscritos(in, "matricula_inscritos_2026-2.csv");
    }

    // ---------------------------------------------------------------- RF-33 analisis

    public synchronized AnalisisCierre analisis() {
        List<GrupoAnalisis> lista = new ArrayList<>();
        for (Grupo g : repo.grupos()) {
            Curso c = repo.cursoDeGrupo(g);
            String estadoMatricula;
            if (g.getEstado() == EstadoGrupo.CERRADO) estadoMatricula = "CERRADO";
            else if (!inscritosCargados) estadoMatricula = "SIN_DATOS";
            else if (g.getInscritos() < minimo) estadoMatricula = "BAJO_MINIMO";
            else if (g.getInscritos() > cupoConTolerancia()) estadoMatricula = "SOBRE_CUPO";
            else estadoMatricula = "OK";
            Grupo receptor = receptor(g);
            lista.add(new GrupoAnalisis(g.getId(), g.getCodigo(), c.getNombre(), c.getSemestre(),
                    repo.docente(g.getDocenteId()).getNombre(), g.getInscritos(), minimo, cupoConTolerancia(),
                    estadoMatricula, g.getEstado().name(), receptor == null ? null : receptor.getCodigo()));
        }
        return new AnalisisCierre(inscritosCargados, archivo, minimo, cupoMaximo, cupoConTolerancia(), lista);
    }

    // ---------------------------------------------------------------- RF-34, RF-35, RF-36 cerrar

    public synchronized ResultadoCierre cerrar(Long grupoId) {
        if (!inscritosCargados) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Primero cargue el archivo de inscritos de Matrícula");
        }
        if (!HorarioService.PUBLICADO.equals(horario.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El cierre de grupos se hace sobre un horario publicado. Publique o descarte el borrador actual.");
        }
        Grupo g = repo.grupo(grupoId);
        if (g == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El grupo no existe");
        if (g.getEstado() == EstadoGrupo.CERRADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El grupo " + g.getCodigo() + " ya está cerrado");
        }

        horario.abrirBorradorSiEstaPublicado("Ajuste por cierre de " + g.getCodigo());
        int totalAntes = repo.sesiones().size();

        // 1. Cerrar el grupo y pasar sus estudiantes a otro grupo del mismo curso
        g.setEstado(EstadoGrupo.CERRADO);
        Grupo receptor = receptorAbierto(g);
        int reasignados = g.getInscritos();
        if (receptor != null) receptor.setInscritos(receptor.getInscritos() + reasignados);

        // 2. Liberar las sesiones del grupo cerrado
        List<String> liberadas = new ArrayList<>();
        Iterator<Sesion> it = repo.sesiones().iterator();
        while (it.hasNext()) {
            Sesion s = it.next();
            if (s.getGrupoId().equals(g.getId())) {
                liberadas.add(Calendario.etiqueta(s.getDia(), s.getFranja()) + " · " + repo.aula(s.getAulaId()).getCodigo());
                it.remove();
            }
        }

        // 3. Re-optimizar SOLO las sesiones del grupo receptor (minima perturbacion)
        Set<Long> afectadas = new HashSet<>();
        if (receptor != null) {
            for (Sesion s : repo.sesiones()) if (s.getGrupoId().equals(receptor.getId())) afectadas.add(s.getId());
        }
        Map<Long, Sesion> antes = motor.reoptimizar(afectadas, validador);
        List<CambioSesion> cambios = new ArrayList<>();
        antes.forEach((id, viejo) -> {
            Sesion nuevo = repo.sesion(id).orElseThrow();
            Grupo gr = repo.grupo(nuevo.getGrupoId());
            String motivo = (viejo.getDia().equals(nuevo.getDia()) && viejo.getFranja() == nuevo.getFranja())
                    ? "Cambio de aula por capacidad (" + gr.getInscritos() + " estudiantes)"
                    : "Cambio de franja: no había aula libre con capacidad a la misma hora";
            cambios.add(new CambioSesion(id, gr.getCodigo(),
                    Calendario.etiqueta(viejo.getDia(), viejo.getFranja()) + " · " + repo.aula(viejo.getAulaId()).getCodigo(),
                    Calendario.etiqueta(nuevo.getDia(), nuevo.getFranja()) + " · " + repo.aula(nuevo.getAulaId()).getCodigo(),
                    motivo));
        });
        int intactas = totalAntes - liberadas.size() - cambios.size();

        // 4. Preparar notificaciones (se envian al publicar el ajuste)
        Docente d = repo.docente(g.getDocenteId());
        pendientes.add(new String[]{d.getNombre() + " <" + d.getCorreo() + ">",
                "Cierre del grupo " + g.getCodigo(),
                "El grupo " + g.getCodigo() + " se cerró por baja matrícula (" + reasignados
                        + " inscritos, mínimo " + minimo + "). Sus horas quedan liberadas."});
        pendientes.add(new String[]{"Estudiantes de " + g.getCodigo() + " (" + reasignados + ")",
                "Su grupo " + g.getCodigo() + " fue cerrado",
                receptor == null
                        ? "El grupo se cerró y el curso no tiene otro grupo abierto. Comuníquese con la Dirección de Programa."
                        : "Fueron reasignados al grupo " + receptor.getCodigo() + ". Consulte su nuevo horario en ChronoScope."});
        if (receptor != null && !cambios.isEmpty()) {
            Docente dr = repo.docente(receptor.getDocenteId());
            pendientes.add(new String[]{dr.getNombre() + " <" + dr.getCorreo() + ">",
                    "Cambio en el grupo " + receptor.getCodigo(),
                    "Su grupo recibe " + reasignados + " estudiantes y cambia: "
                            + cambios.stream().map(c -> c.antes() + " → " + c.despues()).reduce((a, b) -> a + "; " + b).orElse("")});
        }
        if (receptor != null) {
            pendientes.add(new String[]{"Estudiantes de " + receptor.getCodigo(),
                    "Actualización del grupo " + receptor.getCodigo(),
                    cambios.isEmpty() ? "Su grupo recibe estudiantes del grupo cerrado; su horario no cambia."
                            : "Su grupo cambia de aula/franja. Consulte el horario actualizado."});
        }

        horario.limpiarPilas();
        horario.setMensaje("Se cerró " + g.getCodigo() + ". " + cambios.size() + " sesión(es) re-optimizada(s), "
                + intactas + " sin cambios. Valide y publique el ajuste.");

        ultimoResultado = new ResultadoCierre(g.getCodigo(), receptor == null ? null : receptor.getCodigo(),
                reasignados, receptor == null ? 0 : receptor.getInscritos(), liberadas, cambios, intactas,
                pendientes.stream().map(p -> p[0] + ": " + p[1]).toList(), horario.getVersion());
        return ultimoResultado;
    }

    /** El Director valida el ajuste: se publica y se envian los correos pendientes. */
    public synchronized List<Notificacion> publicarAjuste() {
        horario.aprobarYPublicar();
        int antes = repo.notificaciones().size();
        for (String[] p : pendientes) repo.notificar(p[0], p[1], p[2]);
        pendientes.clear();
        ultimoResultado = null;
        return new ArrayList<>(repo.notificaciones().subList(0, repo.notificaciones().size() - antes));
    }

    public ResultadoCierre ultimoResultado() {
        return ultimoResultado;
    }

    // ---------------------------------------------------------------- auxiliares

    /** Grupo que recibiria a los estudiantes si se cierra g (para mostrarlo antes de cerrar). */
    private Grupo receptor(Grupo g) {
        return g.getEstado() == EstadoGrupo.CERRADO ? null : receptorAbierto(g);
    }

    private Grupo receptorAbierto(Grupo g) {
        return repo.grupos().stream()
                .filter(o -> o.getCursoId().equals(g.getCursoId()) && !o.getId().equals(g.getId()))
                .filter(o -> o.getEstado() != EstadoGrupo.CERRADO)
                .min(Comparator.comparingInt(Grupo::getInscritos))
                .orElse(null);
    }
}
