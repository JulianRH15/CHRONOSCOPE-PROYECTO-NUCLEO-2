package co.edu.unbosque.chronoscope.controller;

import co.edu.unbosque.chronoscope.dto.Dtos.Parametros;
import co.edu.unbosque.chronoscope.model.*;
import co.edu.unbosque.chronoscope.repository.DatosRepository;
import co.edu.unbosque.chronoscope.service.CierreGruposService;
import co.edu.unbosque.chronoscope.service.HorarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Catálogos y utilidades")
public class GeneralController {

    private final DatosRepository repo;
    private final HorarioService horario;
    private final CierreGruposService cierre;

    public GeneralController(DatosRepository repo, HorarioService horario, CierreGruposService cierre) {
        this.repo = repo;
        this.horario = horario;
        this.cierre = cierre;
    }

    @GetMapping("/parametros")
    @Operation(summary = "Parámetros del periodo: cupo, tolerancia, mínimo, días y franjas (RF-17, RF-18)")
    public Parametros parametros() {
        return cierre.parametros();
    }

    @GetMapping("/catalogos")
    @Operation(summary = "Docentes, aulas, cursos y grupos")
    public Map<String, Collection<?>> catalogos() {
        return Map.of("docentes", repo.docentes(), "aulas", repo.aulas(),
                "cursos", repo.cursos(), "grupos", repo.grupos());
    }

    @GetMapping("/notificaciones")
    @Operation(summary = "Correos simulados enviados por el sistema")
    public List<Notificacion> notificaciones() {
        return repo.notificaciones();
    }

    @PostMapping("/demo/reiniciar")
    @Operation(summary = "Volver los datos al estado inicial (útil para grabar el video)")
    public Map<String, String> reiniciar() {
        horario.reiniciarDemo();
        cierre.reiniciar();
        return Map.of("mensaje", "Datos de demostración reiniciados");
    }
}
