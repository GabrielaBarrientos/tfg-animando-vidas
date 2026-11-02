package org.tfg.backend.seguimiento;

import java.time.LocalDate;
import java.time.Year;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import org.tfg.backend.empresa.Empresa;
import org.tfg.backend.empresa.EmpresaRepository;
import org.tfg.backend.planificacion.FrecuenciaClases;
import org.tfg.backend.planificacion.PlanificacionProyecto;
import org.tfg.backend.planificacion.PlanificacionProyectoRepository;
import org.tfg.backend.proyecto.Proyecto;
import org.tfg.backend.proyecto.ProyectoRepository;
import org.tfg.backend.usuario.RolUsuario;
import org.tfg.backend.usuario.Usuario;
import org.tfg.backend.usuario.UsuarioRepository;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ConfiguracionSeguimientoControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private PlanificacionProyectoRepository planificacionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Proyecto crearProyecto() {
        Empresa empresa = empresaRepository.saveAndFlush(new Empresa("Empresa " + UUID.randomUUID()));
        Proyecto proyecto = proyectoRepository.saveAndFlush(new Proyecto(
                "Proyecto prueba", (short) Year.now().getValue(), "Prueba", empresa));
        planificacionRepository.saveAndFlush(new PlanificacionProyecto(
                proyecto,
                LocalDate.of(2027, 1, 1),
                LocalDate.of(2027, 7, 1),
                6, FrecuenciaClases.MENSUAL));
        return proyecto;
    }

    private Usuario crearAdmin() {
        return usuarioRepository.saveAndFlush(new Usuario(
                null, null, "admin-" + UUID.randomUUID() + "@example.org", "Ana", "Pérez",
                RolUsuario.ADMINISTRADOR, true, null, null));
    }

    private String solicitudJson(Long idResponsable) {
        return "{"
                + "\"umbralProximoAtrasoSemanas\":2,"
                + "\"umbralAtrasoSemanas\":3,"
                + "\"idResponsableSeguimiento\":" + idResponsable + ","
                + "\"diaInformeSemanal\":\"VIERNES\","
                + "\"idResponsableProyecto\":" + idResponsable + ","
                + "\"tipoDiaInformeMensual\":\"ULTIMO_DIA_HABIL\","
                + "\"diaInformeMensual\":null"
                + "}";
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void guardaYConsultaSeguimiento() throws Exception {
        Proyecto proyecto = crearProyecto();
        Usuario admin = crearAdmin();

        mockMvc.perform(put("/api/proyectos/{id}/seguimiento", proyecto.getIdProyecto())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(solicitudJson(admin.getIdUsuario())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoProyecto").value("PLANIFICADO"))
                .andExpect(jsonPath("$.umbralAtrasoSemanas").value(3));

        mockMvc.perform(get("/api/proyectos/{id}/seguimiento", proyecto.getIdProyecto()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idResponsableSeguimiento").value(admin.getIdUsuario()));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void sinTokenCsrfRechazaGuardado() throws Exception {
        Proyecto proyecto = crearProyecto();
        Usuario admin = crearAdmin();

        mockMvc.perform(put("/api/proyectos/{id}/seguimiento", proyecto.getIdProyecto())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(solicitudJson(admin.getIdUsuario())))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OPERATIVO")
    void operativoNoPuedeGuardarNiConsultarDestinatarios() throws Exception {
        Proyecto proyecto = crearProyecto();
        Usuario admin = crearAdmin();

        mockMvc.perform(put("/api/proyectos/{id}/seguimiento", proyecto.getIdProyecto())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(solicitudJson(admin.getIdUsuario())))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/proyectos/{id}/responsables-elegibles", proyecto.getIdProyecto()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void consultaConfiguracionNoExistenteRetorna404() throws Exception {
        Proyecto proyecto = crearProyecto();
        mockMvc.perform(get("/api/proyectos/{id}/seguimiento", proyecto.getIdProyecto()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void muestraResponsablesElegibles() throws Exception {
        Proyecto proyecto = crearProyecto();
        Usuario admin = crearAdmin();
        mockMvc.perform(get("/api/proyectos/{id}/responsables-elegibles", proyecto.getIdProyecto()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.idUsuario == " + admin.getIdUsuario() + ")]").exists());
    }
}
