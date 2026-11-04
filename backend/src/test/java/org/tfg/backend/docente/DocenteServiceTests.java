package org.tfg.backend.docente;

import java.time.Year;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.tfg.backend.empresa.Empresa;
import org.tfg.backend.empresa.EmpresaRepository;
import org.tfg.backend.proyecto.Proyecto;
import org.tfg.backend.proyecto.ProyectoRepository;
import org.tfg.backend.taller.Taller;
import org.tfg.backend.taller.TallerRepository;
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
    @Autowired private DocenteProyectoRepository asociaciones;
    @Autowired private TallerRepository talleres;
    @Autowired private ProyectoRepository proyectos;
    @Autowired private EmpresaRepository empresas;
    @Autowired private UsuarioRepository usuarios;

    private Proyecto crearProyecto() {
        Empresa empresa = empresas.saveAndFlush(new Empresa("Empresa docentes " + UUID.randomUUID()));
        return proyectos.saveAndFlush(new Proyecto(
                "Proyecto docentes", (short) Year.now().getValue(), "Pruebas", empresa));
    }

    private DocenteRequest datos(String email, String usuario) {
        return new DocenteRequest("María", "Pérez", email, "+54 111 2222",
                "Argentina", "Buenos Aires", "Escuela 1", "Docente", "Primero", "Lengua",
                12, 25, usuario);
    }

    private Usuario crearUsuario(String googleSub, Proyecto proyectoAsignado) {
        Usuario usuario = new Usuario(null, googleSub, "operativo-" + UUID.randomUUID() + "@example.org",
                "Usuario", "Operativo", RolUsuario.OPERATIVO, true, null, null);
        if (proyectoAsignado != null) usuario.getProyectosAsignados().add(proyectoAsignado);
        return usuarios.saveAndFlush(usuario);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void crearModificarYEliminarParticipacion() {
        Proyecto proyecto = crearProyecto();
        Long idProyecto = proyecto.getIdProyecto();
        DocenteResponse creado = servicio.crear(idProyecto, datos("docente@example.org", "maria.perez"));
        assertNotNull(creado.idDocente());
        assertEquals(1, asociaciones.countByProyecto_IdProyecto(idProyecto));
        DocenteResponse actualizado = servicio.actualizar(idProyecto, creado.idDocente(),
                new DocenteRequest("María", "Pérez", "docente@example.org", "+54 4444",
                        "Argentina", "Córdoba", "Escuela 2", "Profesor", "Segundo", "Arte",
                        10, 20, "maria.perez"));
        assertEquals("Córdoba", actualizado.provincia());
        assertEquals(20, actualizado.cantidadAlumnos());
        servicio.eliminar(idProyecto, creado.idDocente());
        assertFalse(docentes.existsById(creado.idDocente()));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void indicadoresDelProyectoSumanAlumnosYEliminanDuplicadosEnEscuelasYPaises() {
        Proyecto proyecto = crearProyecto();
        Long id = proyecto.getIdProyecto();
        servicio.crear(id, datos("docente1@example.org", "maria.perez"));
        servicio.crear(id, new DocenteRequest("Sofía", "Gómez", "docente2@example.org", "+54 2222",
                "Argentina", "Córdoba", "Escuela 1", "Docente", "Primero", "Lengua",
                8, 20, "sofia.gomez"));
        servicio.crear(id, new DocenteRequest("Lucía", "Silva", "docente3@example.org", "+598 3333",
                "Uruguay", "Montevideo", "Escuela 2", "Docente", "Segundo", "Arte",
                10, 15, "lucia.silva"));

        assertEquals(3L, asociaciones.countByProyecto_IdProyecto(id));
        assertEquals(60L, asociaciones.sumarAlumnosPorProyecto(id));
        assertEquals(2L, asociaciones.contarEscuelasDistintasPorProyecto(id));
        assertEquals(2L, asociaciones.contarPaisesDistintosPorProyecto(id));

        Proyecto otroProyecto = crearProyecto();
        servicio.crear(otroProyecto.getIdProyecto(), datos("docente4@example.org", "maria.perez"));
        assertEquals(60L, asociaciones.sumarAlumnosPorProyecto(id));
        assertEquals(2L, asociaciones.contarEscuelasDistintasPorProyecto(id));
        assertEquals(2L, asociaciones.contarPaisesDistintosPorProyecto(id));
        assertEquals(25L, asociaciones.sumarAlumnosPorProyecto(otroProyecto.getIdProyecto()));
        assertEquals(1L, asociaciones.contarEscuelasDistintasPorProyecto(otroProyecto.getIdProyecto()));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void mismoDocenteEnDosProyectosConMismoUsuario() {
        Proyecto proyectoUno = crearProyecto();
        Proyecto proyectoDos = crearProyecto();
        DocenteResponse primero = servicio.crear(proyectoUno.getIdProyecto(),
                datos("docente@example.org", "maria.perez"));
        DocenteResponse segundo = servicio.crear(proyectoDos.getIdProyecto(),
                datos("docente@example.org", "maria.perez"));
        assertEquals(primero.idDocente(), segundo.idDocente());
        servicio.eliminar(proyectoUno.getIdProyecto(), primero.idDocente());
        assertTrue(docentes.existsById(primero.idDocente()));
        assertEquals(1, asociaciones.countByProyecto_IdProyecto(proyectoDos.getIdProyecto()));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void rechazaUsuarioDuplicadoEnProyecto() {
        Proyecto proyecto = crearProyecto();
        servicio.crear(proyecto.getIdProyecto(), datos("docente@example.org", "maria.perez"));
        try {
            servicio.crear(proyecto.getIdProyecto(), datos("otra@example.org", "maria.perez"));
            fail("Se esperaba un conflicto por usuario duplicado");
        } catch (ResponseStatusException error) {
            assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        }
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void rechazaDatosInvalidos() {
        Proyecto proyecto = crearProyecto();
        try {
            servicio.crear(proyecto.getIdProyecto(), datos("correo-no-valido", "maria.perez"));
            fail("Se esperaba un error de validación");
        } catch (ResponseStatusException error) {
            assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        }
        try {
            servicio.crear(proyecto.getIdProyecto(), datos("valido@example.org", "maria1.perez"));
            fail("No se permiten dígitos");
        } catch (ResponseStatusException error) {
            assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        }
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void impideDesvincularDocenteConTaller() {
        Proyecto proyecto = crearProyecto();
        DocenteResponse creado = servicio.crear(proyecto.getIdProyecto(),
                datos("docente@example.org", "maria.perez"));
        DocenteProyecto asociacion = asociaciones.findByDocente_IdDocenteAndProyecto_IdProyecto(
                creado.idDocente(), proyecto.getIdProyecto()).get();
        talleres.saveAndFlush(new Taller("TALLER-" + UUID.randomUUID(), asociacion));
        try {
            servicio.eliminar(proyecto.getIdProyecto(), creado.idDocente());
            fail("Se esperaba conflicto por taller");
        } catch (ResponseStatusException error) {
            assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        }
        assertTrue(docentes.existsById(creado.idDocente()));
    }

    @Test
    @WithMockUser(roles = "OPERATIVO")
    void operativoNoPuedeGestionar() {
        Proyecto proyecto = crearProyecto();
        try {
            servicio.crear(proyecto.getIdProyecto(), datos("docente@example.org", "maria.perez"));
            fail("El rol operativo no debe crear docentes");
        } catch (AccessDeniedException esperado) {
            assertNotNull(esperado.getMessage());
        }
    }

    @Test
    @WithMockUser(roles = "OPERATIVO")
    void operativoConsultaSoloProyectosAsignados() {
        Proyecto asignado = crearProyecto();
        Proyecto ajeno = crearProyecto();
        crearUsuario("sub-operativo-test", asignado);
        OidcUser google = mock(OidcUser.class);
        when(google.getSubject()).thenReturn("sub-operativo-test");
        List<DocenteResponse> respuesta = servicio.listar(asignado.getIdProyecto(), google);
        assertTrue(respuesta.isEmpty());
        try {
            servicio.listar(ajeno.getIdProyecto(), google);
            fail("El operativo no debe consultar proyectos ajenos");
        } catch (AccessDeniedException esperado) {
            assertNotNull(esperado.getMessage());
        }
    }
}
