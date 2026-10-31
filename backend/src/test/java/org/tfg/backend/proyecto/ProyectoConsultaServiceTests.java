
package org.tfg.backend.proyecto;

import java.time.Year;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import org.tfg.backend.empresa.Empresa;
import org.tfg.backend.empresa.EmpresaRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
class ProyectoConsultaServiceTests {

    @Autowired
    private ProyectoConsultaService proyectoConsultaService;

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Empresa crearEmpresa() {

        String nombre = "Empresa " + UUID.randomUUID();

        Empresa empresa = new Empresa(nombre);

        return empresaRepository.saveAndFlush(empresa);
    }

    private Proyecto crearProyecto(String nombre, Empresa empresa) {

        short anio = (short) Year.now().getValue();

        Proyecto proyecto = new Proyecto(
                nombre,
                anio,
                null,
                empresa
        );

        return proyectoRepository.saveAndFlush(proyecto);
    }

    private Long crearUsuario(
            String googleSub,
            String rol,
            boolean habilitado,
            Long idEmpresa) {

        String email = "usuario."
                + UUID.randomUUID()
                + "@example.com";

        String sql =
                "INSERT INTO usuario "
                + "(google_sub, email, nombres, apellidos, "
                + "rol, habilitado, id_empresa, "
                + "f_creacion, f_actualizacion) "
                + "VALUES (?, ?, 'Usuario', 'Prueba', "
                + "?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) "
                + "RETURNING id_usuario";

        return jdbcTemplate.queryForObject(
                sql,
                Long.class,
                googleSub,
                email,
                rol,
                habilitado,
                idEmpresa
        );
    }

    private OidcUser simularUsuarioGoogle(String googleSub) {

        OidcUser usuarioGoogle = mock(OidcUser.class);

        when(usuarioGoogle.getSubject()).thenReturn(googleSub);

        return usuarioGoogle;
    }

    private boolean contieneProyecto(List<ProyectoResponse> proyectos, Long idProyecto) {

        for (ProyectoResponse proyecto : proyectos) {

            if (proyecto.getIdProyecto().equals(idProyecto)) {
                return true;
            }
        }

        return false;
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void administradorPuedeConsultarTodosLosProyectos() {

        Empresa empresaA = crearEmpresa();
        Empresa empresaB = crearEmpresa();

        Proyecto proyectoA = crearProyecto("Proyecto A", empresaA);
        Proyecto proyectoB = crearProyecto("Proyecto B", empresaB);

        String googleSub = UUID.randomUUID().toString();

        crearUsuario(
                googleSub,
                "ADMINISTRADOR",
                true,
                null
        );

        OidcUser usuarioGoogle = simularUsuarioGoogle(googleSub);

        List<ProyectoResponse> proyectos = proyectoConsultaService.listarProyectos(usuarioGoogle);

        assertTrue(contieneProyecto(proyectos, proyectoA.getIdProyecto()));

        assertTrue(contieneProyecto(proyectos, proyectoB.getIdProyecto()));
    }

    @Test
    @WithMockUser(roles = "OPERATIVO")
    void operativoSoloConsultaProyectosAsignados() {

        Empresa empresa = crearEmpresa();

        Proyecto proyectoAsignado = crearProyecto("Proyecto asignado", empresa);

        Proyecto proyectoNoAsignado = crearProyecto("Proyecto no asignado", empresa);

        String googleSub = UUID.randomUUID().toString();

        Long idUsuario = crearUsuario(
                googleSub,
                "OPERATIVO",
                true,
                null
        );

        jdbcTemplate.update(
                "INSERT INTO usuario_proyecto "
                + "(id_usuario, id_proyecto) VALUES (?, ?)",
                idUsuario,
                proyectoAsignado.getIdProyecto()
        );

        OidcUser usuarioGoogle = simularUsuarioGoogle(googleSub);

        List<ProyectoResponse> proyectos = proyectoConsultaService.listarProyectos(usuarioGoogle);

        assertEquals(1, proyectos.size());

        assertTrue(contieneProyecto(proyectos, proyectoAsignado.getIdProyecto()));

        assertFalse(contieneProyecto(proyectos, proyectoNoAsignado.getIdProyecto()));
    }

    @Test
    @WithMockUser(roles = "EXTERNO")
    void externoSoloConsultaProyectosDeSuEmpresa() {

        Empresa empresaA = crearEmpresa();
        Empresa empresaB = crearEmpresa();

        Proyecto proyectoA = crearProyecto("Proyecto empresa A", empresaA);

        Proyecto proyectoB = crearProyecto("Proyecto empresa B", empresaB);

        String googleSub = UUID.randomUUID().toString();

        crearUsuario(
                googleSub,
                "EXTERNO",
                true,
                empresaA.getIdEmpresa()
        );

        OidcUser usuarioGoogle = simularUsuarioGoogle(googleSub);

        List<ProyectoResponse> proyectos = proyectoConsultaService.listarProyectos(usuarioGoogle);

        assertEquals(1, proyectos.size());

        assertTrue(contieneProyecto(proyectos, proyectoA.getIdProyecto()));

        assertFalse(contieneProyecto(proyectos, proyectoB.getIdProyecto()));
    }

    @Test
    @WithMockUser(roles = "OPERATIVO")
    void usuarioDeshabilitadoNoPuedeConsultarProyectos() {

        String googleSub = UUID.randomUUID().toString();

        crearUsuario(
                googleSub,
                "OPERATIVO",
                false,
                null
        );

        OidcUser usuarioGoogle = simularUsuarioGoogle(googleSub);

        try {

            proyectoConsultaService.listarProyectos(usuarioGoogle);

            fail("El usuario deshabilitado no debe acceder");

        } catch (AccessDeniedException excepcion) {
            // Acceso denegado correctamente.
        }
    }

    @Test
    @WithMockUser(roles = "OPERATIVO")
    void usuarioNoRegistradoNoPuedeConsultarProyectos() {

        String googleSub = UUID.randomUUID().toString();

        OidcUser usuarioGoogle = simularUsuarioGoogle(googleSub);

        try {

            proyectoConsultaService.listarProyectos(usuarioGoogle);

            fail("El usuario no registrado no debe acceder");

        } catch (AccessDeniedException excepcion) {
            // Acceso denegado correctamente.
        }
    }

    @Test
    @WithMockUser(roles = "EXTERNO")
    void externoSinEmpresaNoPuedeConsultarProyectos() {

        String googleSub = UUID.randomUUID().toString();

        crearUsuario(
                googleSub,
                "EXTERNO",
                true,
                null
        );

        OidcUser usuarioGoogle = simularUsuarioGoogle(googleSub);

        try {

            proyectoConsultaService.listarProyectos(usuarioGoogle);

            fail("El usuario externo debe tener empresa");

        } catch (AccessDeniedException excepcion) {
            // Acceso denegado correctamente.
        }
    }
}
