
package org.tfg.backend.proyecto;

import java.time.Year;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import org.tfg.backend.empresa.Empresa;
import org.tfg.backend.empresa.EmpresaRepository;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProyectoListadoControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Empresa crearEmpresa() {

        Empresa empresa = new Empresa("Empresa " + UUID.randomUUID());

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
                + "?, TRUE, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) "
                + "RETURNING id_usuario";

        return jdbcTemplate.queryForObject(
                sql,
                Long.class,
                googleSub,
                email,
                rol,
                idEmpresa
        );
    }

    private OidcUser crearUsuarioOidc(String googleSub, String rol) {

        OidcIdToken token = OidcIdToken
                .withTokenValue("token-prueba")
                .subject(googleSub)
                .build();

        SimpleGrantedAuthority autoridad =
                new SimpleGrantedAuthority("ROLE_" + rol);

        return new DefaultOidcUser(List.of(autoridad), token);
    }

    @Test
    void administradorPuedeConsultarTodosLosProyectos() throws Exception {

        Empresa empresaA = crearEmpresa();
        Empresa empresaB = crearEmpresa();

        Proyecto proyectoA = crearProyecto("Proyecto A", empresaA);

        Proyecto proyectoB = crearProyecto("Proyecto B", empresaB);

        String googleSub = UUID.randomUUID().toString();

        crearUsuario(googleSub, "ADMINISTRADOR", null);

        OidcUser usuario = crearUsuarioOidc(googleSub, "ADMINISTRADOR");

        String nombreA = proyectoA.getNombre()
                + " - " + empresaA.getNombre()
                + " - " + proyectoA.getAnio();

        String nombreB = proyectoB.getNombre()
                + " - " + empresaB.getNombre()
                + " - " + proyectoB.getAnio();

        mockMvc.perform(get("/api/proyectos")
                        .with(oidcLogin().oidcUser(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath(
                        "$[*].nombreCompleto",
                        hasItems(nombreA, nombreB)
                ));
    }

    @Test
    void operativoSoloConsultaProyectosAsignados()
            throws Exception {

        Empresa empresa = crearEmpresa();

        Proyecto asignado = crearProyecto("Proyecto asignado", empresa);

        crearProyecto("Proyecto no asignado", empresa);

        String googleSub = UUID.randomUUID().toString();

        Long idUsuario = crearUsuario(googleSub, "OPERATIVO", null);

        jdbcTemplate.update(
                "INSERT INTO usuario_proyecto "
                + "(id_usuario, id_proyecto) VALUES (?, ?)",
                idUsuario,
                asignado.getIdProyecto()
        );

        OidcUser usuario = crearUsuarioOidc(googleSub, "OPERATIVO");

        String nombreCompleto = asignado.getNombre()
                + " - " + empresa.getNombre()
                + " - " + asignado.getAnio();

        mockMvc.perform(get("/api/proyectos")
                        .with(oidcLogin().oidcUser(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$[*].nombreCompleto",
                        contains(nombreCompleto)
                ));
    }

    @Test
    void externoSoloConsultaProyectosDeSuEmpresa()
            throws Exception {

        Empresa empresaA = crearEmpresa();
        Empresa empresaB = crearEmpresa();

        Proyecto visible = crearProyecto("Proyecto empresa A", empresaA);

        crearProyecto("Proyecto empresa B", empresaB);

        String googleSub = UUID.randomUUID().toString();

        crearUsuario(googleSub, "EXTERNO", empresaA.getIdEmpresa());

        OidcUser usuario = crearUsuarioOidc(googleSub, "EXTERNO");

        String nombreCompleto = visible.getNombre()
                + " - " + empresaA.getNombre()
                + " - " + visible.getAnio();

        mockMvc.perform(get("/api/proyectos")
                        .with(oidcLogin().oidcUser(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$[*].nombreCompleto",
                        contains(nombreCompleto)
                ));
    }

    @Test
    void usuarioNoRegistradoNoPuedeConsultarProyectos()
            throws Exception {

        String googleSub = UUID.randomUUID().toString();

        OidcUser usuario = crearUsuarioOidc(googleSub, "OPERATIVO");

        mockMvc.perform(get("/api/proyectos")
                        .with(oidcLogin().oidcUser(usuario)))
                .andExpect(status().isForbidden());
    }
}
