package org.tfg.backend.comunicacion;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/proyectos/{idProyecto}/talleres/{idTaller}/comunicaciones")
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERATIVO')")
public class ComunicacionTallerController {
    private final ComunicacionTallerService servicio;

    public ComunicacionTallerController(ComunicacionTallerService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<ComunicacionResponse> listar(@PathVariable Long idProyecto,
            @PathVariable String idTaller, @AuthenticationPrincipal OidcUser usuarioGoogle) {
        return servicio.listar(idProyecto, idTaller, usuarioGoogle);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ComunicacionResponse crear(@PathVariable Long idProyecto, @PathVariable String idTaller,
            @RequestBody CrearComunicacionRequest datos,
            @AuthenticationPrincipal OidcUser usuarioGoogle) {
        return servicio.crear(idProyecto, idTaller, usuarioGoogle, datos);
    }

    @PatchMapping("/{idComunicacion}")
    public ComunicacionResponse actualizar(@PathVariable Long idProyecto,
            @PathVariable String idTaller, @PathVariable Long idComunicacion,
            @RequestBody ActualizarSeguimientoRequest datos,
            @AuthenticationPrincipal OidcUser usuarioGoogle) {
        return servicio.actualizarSeguimiento(idProyecto, idTaller, idComunicacion, usuarioGoogle, datos);
    }
}
