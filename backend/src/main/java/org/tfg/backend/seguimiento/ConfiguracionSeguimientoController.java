package org.tfg.backend.seguimiento;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/proyectos/{idProyecto}")
public class ConfiguracionSeguimientoController {

    private final ConfiguracionSeguimientoService seguimientoService;

    public ConfiguracionSeguimientoController(ConfiguracionSeguimientoService seguimientoService) {
        this.seguimientoService = seguimientoService;
    }

    @GetMapping("/responsables-elegibles")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<ResponsableElegibleResponse>> listarResponsables(@PathVariable Long idProyecto) {
        return ResponseEntity.ok(seguimientoService.listarResponsablesElegibles(idProyecto));
    }

    @GetMapping("/seguimiento")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ConfiguracionSeguimientoResponse> obtener(@PathVariable Long idProyecto) {
        return ResponseEntity.ok(seguimientoService.obtener(idProyecto));
    }

    @PutMapping("/seguimiento")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ConfiguracionSeguimientoResponse> guardar(
            @PathVariable Long idProyecto, @RequestBody ConfiguracionSeguimientoRequest request) {
        return ResponseEntity.ok(seguimientoService.guardar(idProyecto, request));
    }
}
