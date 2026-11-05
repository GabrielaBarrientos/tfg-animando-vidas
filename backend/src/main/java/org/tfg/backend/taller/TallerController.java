package org.tfg.backend.taller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/proyectos/{idProyecto}/talleres")
public class TallerController {
    private final TallerService tallerService;

    public TallerController(TallerService tallerService) { this.tallerService = tallerService; }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERATIVO', 'EXTERNO')")
    public List<TallerResumenResponse> listar(@PathVariable Long idProyecto,
            @AuthenticationPrincipal OidcUser usuarioGoogle) {
        return tallerService.listar(idProyecto, usuarioGoogle);
    }

    @GetMapping("/{idTaller}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERATIVO', 'EXTERNO')")
    public DetalleTallerResponse obtenerDetalle(@PathVariable Long idProyecto,
            @PathVariable String idTaller, @AuthenticationPrincipal OidcUser usuarioGoogle) {
        return tallerService.obtenerDetalle(idProyecto, idTaller, usuarioGoogle);
    }

    @GetMapping("/metadatos")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERATIVO', 'EXTERNO')")
    public TalleresMetadatosResponse obtenerMetadatos(@PathVariable Long idProyecto,
            @AuthenticationPrincipal OidcUser usuarioGoogle) {
        return tallerService.obtenerMetadatos(idProyecto, usuarioGoogle);
    }

    @PostMapping("/sincronizar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERATIVO')")
    public SincronizacionResponse sincronizar(@PathVariable Long idProyecto,
            @AuthenticationPrincipal OidcUser usuarioGoogle) {
        int cantidad = tallerService.sincronizar(idProyecto, usuarioGoogle);
        return new SincronizacionResponse(cantidad);
    }

    @PostMapping("/{idTaller}/clases/sincronizar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERATIVO')")
    public SincronizacionClasesResponse sincronizarClases(@PathVariable Long idProyecto,
            @PathVariable String idTaller, @AuthenticationPrincipal OidcUser usuarioGoogle) {
        int cantidad = tallerService.sincronizarClases(idProyecto, idTaller, usuarioGoogle);
        return new SincronizacionClasesResponse(cantidad);
    }

    @DeleteMapping("/{idTaller}/copia-local")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERATIVO')")
    public org.springframework.http.ResponseEntity<Void> eliminarCopiaLocal(
            @PathVariable Long idProyecto, @PathVariable String idTaller,
            @AuthenticationPrincipal OidcUser usuarioGoogle) {
        tallerService.eliminarCopiaLocal(idProyecto, idTaller, usuarioGoogle);
        return org.springframework.http.ResponseEntity.noContent().build();
    }

    public record SincronizacionResponse(int cantidadTalleres) { }
    public record SincronizacionClasesResponse(int cantidadClases) { }
}
