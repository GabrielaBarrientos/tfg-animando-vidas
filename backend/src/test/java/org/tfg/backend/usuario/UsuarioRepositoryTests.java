
package org.tfg.backend.usuario;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class UsuarioRepositoryTests {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void debeEncontrarUsuarioPorGoogleSub() {
        String identificador = UUID.randomUUID().toString();

        Usuario usuario = new Usuario(
                null,
                identificador,
                "test-" + identificador + "@example.com",
                "Usuario",
                "Prueba",
                RolUsuario.OPERATIVO,
                true,
                null,
                null
        );

        usuarioRepository.saveAndFlush(usuario);

        var resultado =
                usuarioRepository.findByGoogleSub(identificador);

        assertTrue(resultado.isPresent());
        assertEquals(identificador, resultado.get().getGoogleSub());
        assertEquals(RolUsuario.OPERATIVO, resultado.get().getRol());
        assertTrue(resultado.get().isHabilitado());
        assertNotNull(resultado.get().getFechaCreacion());
        assertNotNull(resultado.get().getFechaActualizacion());
    }

    @Test
    void debeRetornarVacioCuandoUsuarioNoExiste() {
        String identificador =
                "inexistente-" + UUID.randomUUID();

        var resultado =
                usuarioRepository.findByGoogleSub(identificador);

        assertTrue(resultado.isEmpty());
    }
}
