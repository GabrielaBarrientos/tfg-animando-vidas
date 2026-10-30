
package org.tfg.backend.usuario;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UsuarioAutorizacionServiceTests {

    private final UsuarioRepository repository = mock(UsuarioRepository.class);

    private final UsuarioAutorizacionService service = new UsuarioAutorizacionService(repository);

    @Test
    void debeAutorizarUsuarioHabilitado() {
        Usuario usuario = crearUsuario(true);

        when(repository.findByGoogleSub("google-123"))
                .thenReturn(Optional.of(usuario));

        var resultado = service.buscarUsuarioAutorizado("google-123");

        assertTrue(resultado.isPresent());
        assertEquals(usuario.getIdUsuario(), resultado.get().getIdUsuario());
    }

    @Test
    void debeRechazarUsuarioDeshabilitado() {
        Usuario usuario = crearUsuario(false);

        when(repository.findByGoogleSub("google-123"))
                .thenReturn(Optional.of(usuario));

        var resultado = service.buscarUsuarioAutorizado("google-123");

        assertTrue(resultado.isEmpty());
    }

    @Test
    void debeRechazarUsuarioNoRegistrado() {
        when(repository.findByGoogleSub("desconocido"))
                .thenReturn(Optional.empty());

        var resultado = service.buscarUsuarioAutorizado("desconocido");

        assertTrue(resultado.isEmpty());
    }

    @Test
    void debeRechazarIdentificadorNuloOVacio() {
        assertTrue(service.buscarUsuarioAutorizado(null).isEmpty()
        );

        assertTrue(service.buscarUsuarioAutorizado("   ").isEmpty()
        );

        verifyNoInteractions(repository);
    }

    private Usuario crearUsuario(boolean habilitado) {
        return new Usuario(
                1L,
                "google-123",
                "usuario@example.com",
                "Usuario",
                "Prueba",
                RolUsuario.OPERATIVO,
                habilitado,
                null,
                null
        );
    }
}
