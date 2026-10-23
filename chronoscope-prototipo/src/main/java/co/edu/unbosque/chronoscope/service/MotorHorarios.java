package co.edu.unbosque.chronoscope.service;

import co.edu.unbosque.chronoscope.model.*;
import co.edu.unbosque.chronoscope.repository.DatosRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Motor de horarios del prototipo.
 *
 * - generar(): construye un horario que cumple las restricciones duras. En el producto
 *   final esta estrategia es programacion entera + GRASP con busqueda tabu (RF-22, RF-23);
 *   aqui se usa una heuristica voraz para que el prototipo arranque con un horario valido.
 * - reoptimizar(): re-optimizacion de minima perturbacion (RF-35, RF-36). Solo mueve las
 *   sesiones afectadas y deja intacto el resto del horario publicado.
 */
@Service
public class MotorHorarios {

    private final DatosRepository repo;

    public MotorHorarios(DatosRepository repo) {
        this.repo = repo;
    }

    /** Genera un horario completo desde cero. */
    public List<Sesion> generar() {
        repo.sesiones().clear();
        List<Grupo> orden = new ArrayList<>(repo.grupos().stream()
                .filter(g -> g.getEstado() != EstadoGrupo.CERRADO).toList());
        // Primero los mas dificiles: docentes con poca disponibilidad, luego laboratorios, luego grupos grandes.
        orden.sort(Comparator
                .comparingInt((Grupo g) -> -repo.docente(g.getDocenteId()).getNoDisponible().size())
                .thenComparing(g -> !"LABORATORIO".equals(repo.cursoDeGrupo(g).getTipoAula()))
                .thenComparingInt(g -> -g.getInscritos()));

        for (Grupo g : orden) {
            Curso c = repo.cursoDeGrupo(g);
            for (int k = 0; k < c.getSesionesSemana(); k++) {
                Ubicacion mejor = mejorUbicacion(g, repo.sesiones(), null);
                if (mejor != null) {
                    repo.nuevaSesion(g.getId(), mejor.dia, mejor.franja, mejor.aulaId);
                }
            }
        }
        return repo.sesiones();
    }

    /**
     * Mueve solo las sesiones indicadas que tengan conflictos duros.
     * Primero intenta cambiar solo el aula (misma hora); si no puede, busca otra franja.
     *
     * @return mapa sesionId -> copia de la sesion antes del cambio
     */
    public Map<Long, Sesion> reoptimizar(Set<Long> afectadas, ValidadorConflictos validador) {
        Map<Long, Sesion> antes = new LinkedHashMap<>();
        Set<Long> conProblema = new LinkedHashSet<>();
        for (Conflicto c : validador.validar(repo.sesiones())) {
            if (ValidadorConflictos.DURA.equals(c.getSeveridad())) {
                for (Long id : c.getSesionIds()) if (afectadas.contains(id)) conProblema.add(id);
            }
        }
        for (Long id : conProblema) {
            Sesion s = repo.sesion(id).orElseThrow();
            Grupo g = repo.grupo(s.getGrupoId());
            List<Sesion> otras = repo.sesiones().stream().filter(o -> !o.getId().equals(id)).toList();

            // 1) Minima perturbacion: misma franja, otra aula
            Long aula = mejorAula(g, s.getDia(), s.getFranja(), otras);
            Ubicacion nueva = (aula != null && esFactible(g, s.getDia(), s.getFranja(), otras))
                    ? new Ubicacion(s.getDia(), s.getFranja(), aula, 0)
                    : mejorUbicacion(g, otras, s);
            if (nueva != null) {
                antes.put(id, s.copia());
                s.setDia(nueva.dia);
                s.setFranja(nueva.franja);
                s.setAulaId(nueva.aulaId);
            }
        }
        return antes;
    }

    // ------------------------------------------------------------------

    private record Ubicacion(String dia, int franja, Long aulaId, int costo) { }

    /** Busca la mejor franja y aula para una sesion del grupo, sin romper restricciones duras. */
    private Ubicacion mejorUbicacion(Grupo g, List<Sesion> otras, Sesion original) {
        Curso c = repo.cursoDeGrupo(g);
        Ubicacion mejor = null;
        for (int di = 0; di < Calendario.DIAS.size(); di++) {
            String dia = Calendario.DIAS.get(di);
            for (int f = 0; f < Calendario.FRANJAS.size(); f++) {
                if (!esFactible(g, dia, f, otras)) continue;
                Long aula = mejorAula(g, dia, f, otras);
                if (aula == null) continue;

                int costo = f; // preferir la manana
                for (Sesion o : otras) {
                    if (o.getGrupoId().equals(g.getId())) {
                        int dOtra = Calendario.DIAS.indexOf(o.getDia());
                        if (Math.abs(dOtra - di) <= 1) costo += 100; // dejar un dia entre sesiones
                    }
                    Grupo og = repo.grupo(o.getGrupoId());
                    if (o.getDia().equals(dia) && o.getFranja() == f
                            && repo.cursoDeGrupo(og).getSemestre() == c.getSemestre()) {
                        costo += 25; // repartir los cursos del mismo semestre
                    }
                }
                if (original != null) {
                    // minima perturbacion: cuanto mas lejos de donde estaba, mas caro
                    costo += 10 * Math.abs(Calendario.DIAS.indexOf(original.getDia()) - di)
                            + 5 * Math.abs(original.getFranja() - f);
                }
                if (mejor == null || costo < mejor.costo) mejor = new Ubicacion(dia, f, aula, costo);
            }
        }
        return mejor;
    }

    private boolean esFactible(Grupo g, String dia, int franja, List<Sesion> otras) {
        if (Calendario.esBloqueada(dia, franja)) return false;
        Docente d = repo.docente(g.getDocenteId());
        if (d.getNoDisponible().contains(Calendario.clave(dia, franja))) return false;
        for (Sesion o : otras) {
            if (!o.getDia().equals(dia) || o.getFranja() != franja) continue;
            Grupo og = repo.grupo(o.getGrupoId());
            if (og.getId().equals(g.getId())) return false;
            if (og.getDocenteId().equals(g.getDocenteId())) return false;
        }
        return true;
    }

    /** Aula libre mas pequena que cumpla tipo y capacidad (mejor ajuste). */
    private Long mejorAula(Grupo g, String dia, int franja, List<Sesion> otras) {
        Curso c = repo.cursoDeGrupo(g);
        Set<Long> ocupadas = new HashSet<>();
        for (Sesion o : otras) {
            if (o.getDia().equals(dia) && o.getFranja() == franja) ocupadas.add(o.getAulaId());
        }
        return repo.aulas().stream()
                .filter(a -> a.getTipo().equals(c.getTipoAula())
                        || ("AUDITORIO".equals(a.getTipo()) && "SALON".equals(c.getTipoAula())))
                .filter(a -> a.getCapacidad() >= g.getInscritos())
                .filter(a -> !ocupadas.contains(a.getId()))
                .min(Comparator.comparingInt(Aula::getCapacidad))
                .map(Aula::getId)
                .orElse(null);
    }
}
