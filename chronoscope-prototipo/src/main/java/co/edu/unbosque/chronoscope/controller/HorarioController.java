package co.edu.unbosque.chronoscope.controller;

import co.edu.unbosque.chronoscope.dto.Dtos.*;
import co.edu.unbosque.chronoscope.service.HorarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/horario")
@Tag(name = "Épica 1 · Ajuste y publicación del horario", description = "RF-25, RF-26, RF-27, RF-29, RF-31")
public class HorarioController {

    private final HorarioService servicio;

    public HorarioController(HorarioService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Horario actual con sus sesiones y conflictos (RF-25)")
    public HorarioVista obtener() {
        return servicio.vista();
    }

    @PutMapping("/sesiones/{id}")
    @Operation(summary = "Mover una sesión a otro día, franja o aula y validar conflictos (RF-26)")
    public HorarioVista mover(@PathVariable Long id, @RequestBody MoverSesionRequest req) {
        return servicio.mover(id, req);
    }

    @PostMapping("/deshacer")
    @Operation(summary = "Deshacer el último cambio (RF-27)")
    public HorarioVista deshacer() {
        return servicio.deshacer();
    }

    @PostMapping("/rehacer")
    @Operation(summary = "Rehacer el último cambio deshecho (RF-27)")
    public HorarioVista rehacer() {
        return servicio.rehacer();
    }

    @PostMapping("/aprobar")
    @Operation(summary = "Aprobar y publicar; falla si hay conflictos duros (RF-29)")
    public HorarioVista aprobar() {
        return servicio.aprobarYPublicar();
    }

    @PostMapping("/generar")
    @Operation(summary = "Volver a generar el borrador con el motor (RF-22)")
    public HorarioVista generar() {
        return servicio.regenerar();
    }

    @GetMapping("/versiones")
    @Operation(summary = "Versiones guardadas del horario (RF-31)")
    public List<VersionResumen> versiones() {
        return servicio.versiones();
    }
}
