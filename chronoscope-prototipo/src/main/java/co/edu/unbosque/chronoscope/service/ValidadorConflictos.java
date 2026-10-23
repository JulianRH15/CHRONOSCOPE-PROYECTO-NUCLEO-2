package co.edu.unbosque.chronoscope.service;

import co.edu.unbosque.chronoscope.model.*;
import co.edu.unbosque.chronoscope.repository.DatosRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Valida un horario completo contra las restricciones duras y blandas (RF-26, CU-12).
 * El mismo validador revisa lo que genera el motor y cada cambio de la cuadricula.
 */
@Service
public class ValidadorConflictos {

    public static final String DURA = "DURA";
    public static final String BLANDA = "BLANDA";

    private final DatosRepository repo;

    public ValidadorConflictos(DatosRepository repo) {
        this.repo = repo;
    }

    public List<Conflicto> validar(List<Sesion> sesiones) {
        List<Conflicto> conflictos = new ArrayList<>();
        Map<String, List<Sesion>> porFranja = new LinkedHashMap<>();
        for (Sesion s : sesiones) {
            porFranja.computeIfAbsent(Calendario.clave(s.getDia(), s.getFranja()), k -> new ArrayList<>()).add(s);
        }

        // --- Cruces en la misma franja: docente, aula y grupo ---
        for (List<Sesion> mismas : porFranja.values()) {
            for (int i = 0; i < mismas.size(); i++) {
                for (int j = i + 1; j < mismas.size(); j++) {
                    Sesion a = mismas.get(i);
                    Sesion b = mismas.get(j);
                    Grupo ga = repo.grupo(a.getGrupoId());
                    Grupo gb = repo.grupo(b.getGrupoId());
                    String cuando = Calendario.etiqueta(a.getDia(), a.getFranja());
                    if (ga.getDocenteId().equals(gb.getDocenteId())) {
                        conflictos.add(new Conflicto("CRUCE_DOCENTE", DURA,
                                repo.docente(ga.getDocenteId()).getNombre() + " tendría " + ga.getCodigo()
                                        + " y " + gb.getCodigo() + " al mismo tiempo (" + cuando + ").",
                                List.of(a.getId(), b.getId())));
                    }
                    if (a.getAulaId().equals(b.getAulaId())) {
                        conflictos.add(new Conflicto("CRUCE_AULA", DURA,
                                "El aula " + repo.aula(a.getAulaId()).getCodigo() + " ya está ocupada por "
                                        + (ga == gb ? ga.getCodigo() : gb.getCodigo() + " y " + ga.getCodigo())
                                        + " (" + cuando + ").",
                                List.of(a.getId(), b.getId())));
                    }
                    if (ga.getId().equals(gb.getId())) {
                        conflictos.add(new Conflicto("CRUCE_GRUPO", DURA,
                                "El grupo " + ga.getCodigo() + " tiene dos sesiones en " + cuando + ".",
                                List.of(a.getId(), b.getId())));
                    }
                }
            }
        }

        // --- Reglas por sesion ---
        for (Sesion s : sesiones) {
            Grupo g = repo.grupo(s.getGrupoId());
            Curso c = repo.cursoDeGrupo(g);
            Aula aula = repo.aula(s.getAulaId());
            Docente d = repo.docente(g.getDocenteId());
            String cuando = Calendario.etiqueta(s.getDia(), s.getFranja());

            if (Calendario.esBloqueada(s.getDia(), s.getFranja())) {
                String motivo = s.getFranja() == Calendario.FRANJA_ALMUERZO
                        ? "la franja de almuerzo (12:00 - 13:00) está bloqueada"
                        : "el sábado solo hay clase hasta las 13:00";
                conflictos.add(new Conflicto("FRANJA_BLOQUEADA", DURA,
                        g.getCodigo() + ": " + motivo + ".", List.of(s.getId())));
            }
            if (d.getNoDisponible().contains(Calendario.clave(s.getDia(), s.getFranja()))) {
                conflictos.add(new Conflicto("DISPONIBILIDAD_DOCENTE", DURA,
                        d.getNombre() + " registró que no está disponible el " + cuando + ".",
                        List.of(s.getId())));
            }
            if (g.getInscritos() > aula.getCapacidad()) {
                conflictos.add(new Conflicto("CAPACIDAD_AULA", DURA,
                        g.getCodigo() + " tiene " + g.getInscritos() + " estudiantes y el aula "
                                + aula.getCodigo() + " solo tiene " + aula.getCapacidad() + " puestos.",
                        List.of(s.getId())));
            }
            boolean auditorioParaSalon = "AUDITORIO".equals(aula.getTipo()) && "SALON".equals(c.getTipoAula());
            if (!aula.getTipo().equals(c.getTipoAula()) && !auditorioParaSalon) {
                conflictos.add(new Conflicto("TIPO_AULA", DURA,
                        c.getNombre() + " requiere " + c.getTipoAula().toLowerCase() + " y "
                                + aula.getCodigo() + " es " + aula.getTipo().toLowerCase() + ".",
                        List.of(s.getId())));
            }
        }

        // --- Carga maxima del docente ---
        Map<Long, List<Sesion>> porDocente = new HashMap<>();
        for (Sesion s : sesiones) {
            porDocente.computeIfAbsent(repo.grupo(s.getGrupoId()).getDocenteId(), k -> new ArrayList<>()).add(s);
        }
        porDocente.forEach((docenteId, lista) -> {
            Docente d = repo.docente(docenteId);
            int horas = lista.size() * 2;
            if (horas > d.getMaxHorasSemana()) {
                conflictos.add(new Conflicto("CARGA_MAXIMA", DURA,
                        d.getNombre() + " quedaría con " + horas + " h/semana y su máximo es "
                                + d.getMaxHorasSemana() + " h.",
                        lista.stream().map(Sesion::getId).toList()));
            }
        });

        // --- Restriccion blanda: sesiones de un grupo en dias seguidos o el mismo dia ---
        Map<Long, List<Sesion>> porGrupo = new HashMap<>();
        for (Sesion s : sesiones) porGrupo.computeIfAbsent(s.getGrupoId(), k -> new ArrayList<>()).add(s);
        porGrupo.forEach((grupoId, lista) -> {
            for (int i = 0; i < lista.size(); i++) {
                for (int j = i + 1; j < lista.size(); j++) {
                    int di = Calendario.DIAS.indexOf(lista.get(i).getDia());
                    int dj = Calendario.DIAS.indexOf(lista.get(j).getDia());
                    if (Math.abs(di - dj) <= 1) {
                        conflictos.add(new Conflicto("DIAS_CONSECUTIVOS", BLANDA,
                                repo.grupo(grupoId).getCodigo() + " tiene sus sesiones en días "
                                        + (di == dj ? "iguales" : "seguidos") + "; se recomienda dejar un día entre ellas.",
                                List.of(lista.get(i).getId(), lista.get(j).getId())));
                    }
                }
            }
        });

        return conflictos;
    }

    public boolean tieneConflictosDuros(List<Conflicto> conflictos) {
        return conflictos.stream().anyMatch(c -> DURA.equals(c.getSeveridad()));
    }
}
