package org.tfg.backend.programa;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/proyectos/{idProyecto}/programa")
public class ProgramaController {
    private final ProgramaService service;
    public ProgramaController(ProgramaService service) { this.service = service; }
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERATIVO', 'EXTERNO')")
    public ProgramaResponse consultar(@PathVariable Long idProyecto,
            @AuthenticationPrincipal OidcUser usuario) { return service.consultar(idProyecto, usuario); }
    @PostMapping("/sincronizar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERATIVO')")
    public ProgramaResponse actualizar(@PathVariable Long idProyecto,
            @AuthenticationPrincipal OidcUser usuario) { return service.actualizar(idProyecto, usuario); }
}
