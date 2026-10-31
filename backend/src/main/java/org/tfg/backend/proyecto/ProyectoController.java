
package org.tfg.backend.proyecto;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@RestController
@RequestMapping("/api/proyectos")
public class ProyectoController {

    private final ProyectoService proyectoService;

    private final ProyectoConsultaService proyectoConsultaService;

    public ProyectoController(ProyectoService proyectoService, ProyectoConsultaService proyectoConsultaService) {

    this.proyectoService = proyectoService;
    this.proyectoConsultaService = proyectoConsultaService;
    }

    @PostMapping
    public ResponseEntity<ProyectoResponse> crearProyecto(@RequestBody CrearProyectoRequest request) {

        Proyecto proyecto = proyectoService.crearProyecto(
                request.getNombre(),
                request.getAnio(),
                request.getDescripcion(),
                request.getIdEmpresa()
        );

        ProyectoResponse respuesta = new ProyectoResponse(proyecto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(respuesta);
    }

    @GetMapping
    public List<ProyectoResponse> listarProyectos(@AuthenticationPrincipal OidcUser usuarioGoogle) {

        return proyectoConsultaService.listarProyectos(usuarioGoogle);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> manejarErrorValidacion(IllegalArgumentException excepcion) {

        Map<String, String> error = new HashMap<>();
        error.put("mensaje", excepcion.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> manejarEmpresaInexistente(NoSuchElementException excepcion) {

        Map<String, String> error = new HashMap<>();
        error.put("mensaje", excepcion.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }
}
