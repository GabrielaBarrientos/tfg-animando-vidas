package org.tfg.backend.docente;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/proyectos/{idProyecto}/docentes")
public class DocenteController {
    private final DocenteService docenteService;

    public DocenteController(DocenteService docenteService) {
        this.docenteService = docenteService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERATIVO')")
    public List<DocenteResponse> listar(@PathVariable Long idProyecto,
            @AuthenticationPrincipal OidcUser usuarioGoogle) {
        return docenteService.listar(idProyecto, usuarioGoogle);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<DocenteResponse> crear(@PathVariable Long idProyecto,
            @RequestBody DocenteRequest datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(docenteService.crear(idProyecto, datos));
    }

    @PutMapping("/{idDocente}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public DocenteResponse actualizar(@PathVariable Long idProyecto,
            @PathVariable Long idDocente, @RequestBody DocenteRequest datos) {
        return docenteService.actualizar(idProyecto, idDocente, datos);
    }

    @DeleteMapping("/{idDocente}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long idProyecto,
            @PathVariable Long idDocente) {
        docenteService.eliminar(idProyecto, idDocente);
        return ResponseEntity.noContent().build();
    }
}
