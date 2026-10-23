package co.edu.unbosque.chronoscope.controller;

import co.edu.unbosque.chronoscope.dto.Dtos.*;
import co.edu.unbosque.chronoscope.model.Notificacion;
import co.edu.unbosque.chronoscope.service.CierreGruposService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/cierre")
@Tag(name = "Épica 2 · Cierre de grupos", description = "RF-32, RF-33, RF-34, RF-35, RF-36")
public class CierreGruposController {

    private final CierreGruposService servicio;

    public CierreGruposController(CierreGruposService servicio) {
        this.servicio = servicio;
    }

    @PostMapping(value = "/inscritos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Importar el CSV de inscritos por grupo de Matrícula (RF-32)")
    public AnalisisCierre cargar(@RequestParam("archivo") MultipartFile archivo) {
        try {
            return servicio.cargarInscritos(archivo.getInputStream(), archivo.getOriginalFilename());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo leer el archivo");
        }
    }

    @PostMapping("/inscritos/ejemplo")
    @Operation(summary = "Cargar el archivo de ejemplo de Matrícula incluido en el prototipo")
    public AnalisisCierre cargarEjemplo() {
        return servicio.cargarEjemplo();
    }

    @GetMapping(value = "/inscritos/ejemplo.csv", produces = "text/csv")
    @Operation(summary = "Descargar el CSV de ejemplo")
    public ResponseEntity<byte[]> descargarEjemplo() throws IOException {
        byte[] datos = getClass().getResourceAsStream("/matricula_inscritos_2026-2.csv").readAllBytes();
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=matricula_inscritos_2026-2.csv")
                .body(datos);
    }

    @GetMapping("/analisis")
    @Operation(summary = "Grupos bajo el mínimo o sobre el cupo (RF-33)")
    public AnalisisCierre analisis() {
        return servicio.analisis();
    }

    @PostMapping("/grupos/{id}/cerrar")
    @Operation(summary = "Cerrar un grupo y re-optimizar solo las sesiones afectadas (RF-34, RF-35, RF-36)")
    public ResultadoCierre cerrar(@PathVariable Long id) {
        return servicio.cerrar(id);
    }

    @GetMapping("/resultado")
    @Operation(summary = "Resultado del último cierre pendiente de publicar")
    public ResponseEntity<ResultadoCierre> resultado() {
        ResultadoCierre r = servicio.ultimoResultado();
        return r == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(r);
    }

    @PostMapping("/publicar")
    @Operation(summary = "Validar y publicar el ajuste; envía las notificaciones (RF-34)")
    public List<Notificacion> publicar() {
        return servicio.publicarAjuste();
    }
}
