
package org.tfg.backend.planificacion;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/proyectos/{idProyecto}/planificacion")
public class PlanificacionProyectoController {

    private final PlanificacionProyectoService planificacionService;

    public PlanificacionProyectoController(PlanificacionProyectoService planificacionService) {

        this.planificacionService = planificacionService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<PlanificacionProyectoResponse> obtenerPlanificacion(@PathVariable Long idProyecto) {

        PlanificacionProyectoResponse respuesta = planificacionService.obtener(idProyecto);

        return ResponseEntity.ok(respuesta);
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<PlanificacionProyectoResponse> guardarPlanificacion(
            @PathVariable Long idProyecto, @RequestBody PlanificacionProyectoRequest request) {

        PlanificacionProyectoResponse respuesta = planificacionService.guardar(idProyecto, request);

        return ResponseEntity.ok(respuesta);
    }
}
