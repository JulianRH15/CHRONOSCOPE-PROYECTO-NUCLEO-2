package co.edu.unbosque.chronoscope.dto;

import co.edu.unbosque.chronoscope.model.Conflicto;

import java.util.List;
import java.util.Set;

/** Objetos de transferencia que la API envia y recibe (records de Java 17). */
public final class Dtos {

    private Dtos() { }

    /** Sesion con los datos ya resueltos para pintarla en la cuadricula. */
    public record SesionVista(Long id, Long grupoId, String grupoCodigo, String cursoCodigo, String cursoNombre,
                              int semestre, Long docenteId, String docenteNombre, Long aulaId, String aulaCodigo,
                              int aulaCapacidad, int inscritos, String dia, int franja) { }

    /** Estado completo del horario que esta viendo el usuario. */
    public record HorarioVista(int version, String estado, List<SesionVista> sesiones, List<Conflicto> conflictos,
                               boolean puedeDeshacer, boolean puedeRehacer, Set<Long> cambiadasVsPublicado,
                               String mensaje) { }

    /** Peticion para mover una sesion (arrastrar y soltar o cambio de aula). */
    public record MoverSesionRequest(String dia, Integer franja, Long aulaId) { }

    /** Un cambio aplicado sobre una sesion; se usa para deshacer y rehacer (RF-27). */
    public record Movimiento(Long sesionId, String diaAntes, int franjaAntes, Long aulaAntes,
                             String diaDespues, int franjaDespues, Long aulaDespues) { }

    /** Analisis de un grupo despues de cargar los inscritos de Matricula (RF-33). */
    public record GrupoAnalisis(Long grupoId, String codigo, String curso, int semestre, String docente,
                                int inscritos, int minimo, int cupoConTolerancia, String estadoMatricula,
                                String estadoGrupo, String grupoReceptor) { }

    public record AnalisisCierre(boolean inscritosCargados, String archivo, int minimo, int cupoMaximo,
                                 int cupoConTolerancia, List<GrupoAnalisis> grupos) { }

    /** Una sesion antes y despues de la re-optimizacion. */
    public record CambioSesion(Long sesionId, String grupo, String antes, String despues, String motivo) { }

    /** Resultado de cerrar un grupo (RF-34, RF-35, RF-36). */
    public record ResultadoCierre(String grupoCerrado, String grupoReceptor, int estudiantesReasignados,
                                  int inscritosReceptor, List<String> sesionesLiberadas,
                                  List<CambioSesion> sesionesReoptimizadas, int sesionesIntactas,
                                  List<String> notificacionesPendientes, int nuevaVersion) { }

    public record VersionResumen(int numero, String estado, String fecha, String descripcion, int sesiones) { }

    public record Parametros(int cupoMaximo, int toleranciaPorcentaje, int minimoEstudiantes,
                             List<String> dias, List<String> franjas, int franjaAlmuerzo) { }
}
