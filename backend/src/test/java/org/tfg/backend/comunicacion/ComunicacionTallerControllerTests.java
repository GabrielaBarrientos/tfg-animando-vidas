package org.tfg.backend.comunicacion;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;

class ComunicacionTallerControllerTests {
    @Test
    void todosLosEndpointsRequierenUsuarioInterno() throws Exception {
        PreAuthorize regla = ComunicacionTallerController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(regla);
        assertEquals("hasAnyRole('ADMINISTRADOR', 'OPERATIVO')", regla.value());

        Method consulta = ComunicacionTallerController.class.getMethod("listar",
                Long.class, String.class, OidcUser.class);
        Method creacion = ComunicacionTallerController.class.getMethod("crear",
                Long.class, String.class, CrearComunicacionRequest.class, OidcUser.class);
        Method cambio = ComunicacionTallerController.class.getMethod("actualizar",
                Long.class, String.class, Long.class, ActualizarSeguimientoRequest.class, OidcUser.class);
        assertNotNull(consulta);
        assertNotNull(creacion);
        assertNotNull(cambio);
    }
}
