package org.tfg.backend.encuesta;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/proyectos/{idProyecto}/talleres/{idTaller}/encuestas")
public class EncuestasController {
    private final EncuestasService servicio;

    public EncuestasController(EncuestasService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERATIVO', 'EXTERNO')")
    public EncuestasTallerResponse consultar(@PathVariable Long idProyecto,
            @PathVariable String idTaller, @AuthenticationPrincipal OidcUser usuarioGoogle) {
        return servicio.consultar(idProyecto, idTaller, usuarioGoogle);
    }

    @PostMapping("/sincronizar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERATIVO')")
    public SincronizacionResponse sincronizar(@PathVariable Long idProyecto,
            @PathVariable String idTaller, @AuthenticationPrincipal OidcUser usuarioGoogle) {
        int cantidad = servicio.sincronizar(idProyecto, idTaller, usuarioGoogle);
        return new SincronizacionResponse(cantidad);
    }

    public record SincronizacionResponse(int cantidadRespuestas) { }
}
