package org.tfg.backend.docente;

import java.time.Year;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.tfg.backend.empresa.Empresa;
import org.tfg.backend.empresa.EmpresaRepository;
import org.tfg.backend.proyecto.Proyecto;
import org.tfg.backend.proyecto.ProyectoRepository;
import org.tfg.backend.usuario.RolUsuario;
import org.tfg.backend.usuario.Usuario;
import org.tfg.backend.usuario.UsuarioRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class DocenteServiceTests {
    @Autowired private DocenteService servicio;
    @Autowired private DocenteRepository docentes;
    @Autowired private ProyectoRepository proyectos;
    @Autowired private EmpresaRepository empresas;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private JdbcTemplate jdbc;

    private Proyecto crearProyecto() {
        Empresa empresa = empresas.saveAndFlush(new Empresa("Empresa docentes " + UUID.randomUUID()));
        return proyectos.saveAndFlush(new Proyecto(
                "Proyecto docentes", (short) Year.now().getValue(), "Pruebas", empresa));
    }

    private DocenteRequest datos(String email) {
        return new DocenteRequest("María", "Pérez", email, "Argentina", "Buenos Aires", "Escuela 1");
    }

    private Usuario crearUsuario(String googleSub, Proyecto proyectoAsignado) {
        Usuario usuario = new Usuario(null, googleSub, "operativo-" + UUID.randomUUID() + "@example.org",
                "Usuario", "Operativo", RolUsuario.OPERATIVO, true, null, null);
        if (proyectoAsignado != null) {
            usuario.getProyectosAsignados().add(proyectoAsignado);
        }
        return usuarios.saveAndFlush(usuario);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void crearActualizarYEliminarDocenteSinTalleres() {
        Proyecto proyecto = crearProyecto();
        Long idProyecto = proyecto.getIdProyecto();

        DocenteResponse creado = servicio.crear(idProyecto, datos("docente@example.org"));
        assertNotNull(creado.idDocente());
        assertEquals(1, docentes.findByProyecto_IdProyectoOrderByApellidosAscNombresAsc(idProyecto).size());

        DocenteResponse actualizado = servicio.actualizar(idProyecto, creado.idDocente(),
                new DocenteRequest("María", "Rodríguez", "nuevo@example.org",
                        "Argentina", "Córdoba", "Escuela 2"));
        assertEquals("Rodríguez", actualizado.apellidos());
        assertEquals("nuevo@example.org", actualizado.email());

        servicio.eliminar(idProyecto, creado.idDocente());
        assertFalse(docentes.existsById(creado.idDocente()));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void rechazaCorreoDuplicadoEnMismoProyecto() {
        Proyecto proyecto = crearProyecto();
        servicio.crear(proyecto.getIdProyecto(), datos("docente@example.org"));
        try {
            servicio.crear(proyecto.getIdProyecto(), datos("DOCENTE@example.org"));
            fail("Se esperaba un conflicto por correo duplicado");
        } catch (ResponseStatusException error) {
            assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        }
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void rechazaDatosInvalidos() {
        Proyecto proyecto = crearProyecto();
        try {
            servicio.crear(proyecto.getIdProyecto(), datos("correo-no-valido"));
            fail("Se esperaba un error de validación");
        } catch (ResponseStatusException error) {
            assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        }
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void impideEliminarDocenteConTallerAsociado() {
        Proyecto proyecto = crearProyecto();
        DocenteResponse docente = servicio.crear(proyecto.getIdProyecto(), datos("docente@example.org"));
        jdbc.update("INSERT INTO asociacion_taller_docente (id_taller_plataforma, id_docente) VALUES (?, ?)",
                "TALLER-" + UUID.randomUUID(), docente.idDocente());
        try {
            servicio.eliminar(proyecto.getIdProyecto(), docente.idDocente());
            fail("Se esperaba un conflicto por taller asociado");
        } catch (ResponseStatusException error) {
            assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        }
        assertTrue(docentes.existsById(docente.idDocente()));
    }

    @Test
    @WithMockUser(roles = "OPERATIVO")
    void operativoNoPuedeCrearModificarNiEliminar() {
        Proyecto proyecto = crearProyecto();
        try {
            servicio.crear(proyecto.getIdProyecto(), datos("docente@example.org"));
            fail("El rol operativo no debe registrar docentes");
        } catch (AccessDeniedException esperado) {
            assertNotNull(esperado);
        }
    }

    @Test
    @WithMockUser(roles = "OPERATIVO")
    void operativoConsultaUnicamenteProyectosAsignados() {
        Proyecto proyectoAsignado = crearProyecto();
        Proyecto proyectoAjeno = crearProyecto();
        crearUsuario("sub-operativo-test", proyectoAsignado);
        OidcUser usuarioGoogle = mock(OidcUser.class);
        when(usuarioGoogle.getSubject()).thenReturn("sub-operativo-test");

        List<DocenteResponse> respuesta = servicio.listar(proyectoAsignado.getIdProyecto(), usuarioGoogle);
        assertTrue(respuesta.isEmpty());
        try {
            servicio.listar(proyectoAjeno.getIdProyecto(), usuarioGoogle);
            fail("El usuario no debería consultar proyectos ajenos");
        } catch (AccessDeniedException esperado) {
            assertNotNull(esperado);
        }
    }
}
