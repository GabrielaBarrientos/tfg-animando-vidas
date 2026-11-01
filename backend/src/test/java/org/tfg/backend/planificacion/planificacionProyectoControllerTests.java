
package org.tfg.backend.planificacion;

import java.time.LocalDate;
import java.time.Year;
import java.util.Optional;
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
import org.tfg.backend.proyecto.Proyecto;
import org.tfg.backend.proyecto.ProyectoRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PlanificacionProyectoControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private PlanificacionProyectoRepository planificacionRepository;

    private Proyecto crearProyectoPrueba() {

        Empresa empresa = new Empresa("Empresa " + UUID.randomUUID());

        empresa = empresaRepository.saveAndFlush(empresa);

        short anio = (short) Year.now().getValue();

        Proyecto proyecto = new Proyecto(
                "Proyecto de prueba",
                anio,
                "Talleres educativos",
                empresa);

        return proyectoRepository.saveAndFlush(proyecto);
    }

    private PlanificacionProyecto crearPlanificacionPrueba(Proyecto proyecto) {

        PlanificacionProyecto planificacion =
                new PlanificacionProyecto(
                        proyecto,
                        LocalDate.of(2027, 3, 15),
                        LocalDate.of(2027, 9, 14),
                        6,
                        FrecuenciaClases.MENSUAL);

        return planificacionRepository.saveAndFlush(planificacion);
    }

    private String crearJson(
            String fechaInicio,
            String fechaFinalizacion,
            int cantidadClases,
            String frecuenciaClases) {

        return "{"
                + "\"fechaInicio\":\"" + fechaInicio + "\","
                + "\"fechaFinalizacion\":\"" + fechaFinalizacion + "\","
                + "\"cantidadClases\":" + cantidadClases + ","
                + "\"frecuenciaClases\":\"" + frecuenciaClases + "\""
                + "}";
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void administradorPuedeRegistrarPlanificacion() throws Exception {

        Proyecto proyecto = crearProyectoPrueba();

        String cuerpo = crearJson(
                "2027-03-15",
                "2027-09-14",
                6,
                "MENSUAL");

        mockMvc.perform(
                        put("/api/proyectos/{idProyecto}/planificacion",
                                proyecto.getIdProyecto())
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPlanificacionProyecto").isNumber())
                .andExpect(jsonPath("$.idProyecto").isNumber())
                .andExpect(jsonPath("$.fechaInicio").value("2027-03-15"))
                .andExpect(jsonPath("$.fechaFinalizacion").value("2027-09-14"))
                .andExpect(jsonPath("$.cantidadClases").value(6))
                .andExpect(jsonPath("$.frecuenciaClases").value("MENSUAL"))
                .andExpect(jsonPath("$.duracionMeses").value(7));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void administradorPuedeConsultarPlanificacion() throws Exception {

        Proyecto proyecto = crearProyectoPrueba();
        crearPlanificacionPrueba(proyecto);

        mockMvc.perform(
                        get("/api/proyectos/{idProyecto}/planificacion",
                                proyecto.getIdProyecto()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechaInicio").value("2027-03-15"))
                .andExpect(jsonPath("$.fechaFinalizacion").value("2027-09-14"))
                .andExpect(jsonPath("$.cantidadClases").value(6))
                .andExpect(jsonPath("$.frecuenciaClases").value("MENSUAL"))
                .andExpect(jsonPath("$.duracionMeses").value(7));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void administradorPuedeActualizarPlanificacion() throws Exception {

        Proyecto proyecto = crearProyectoPrueba();

        PlanificacionProyecto planificacionOriginal = crearPlanificacionPrueba(proyecto);

        Long idOriginal = planificacionOriginal.getIdPlanificacionProyecto();

        String cuerpo = crearJson(
                "2027-04-01",
                "2027-10-31",
                12,
                "QUINCENAL");

        mockMvc.perform(
                        put("/api/proyectos/{idProyecto}/planificacion",
                                proyecto.getIdProyecto())
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadClases").value(12))
                .andExpect(jsonPath("$.frecuenciaClases").value("QUINCENAL"))
                .andExpect(jsonPath("$.duracionMeses").value(7));

        Optional<PlanificacionProyecto> resultado =
                planificacionRepository.findByProyecto_IdProyecto(
                        proyecto.getIdProyecto());

        assertTrue(resultado.isPresent());

        PlanificacionProyecto actualizada = resultado.get();

        assertEquals(idOriginal, actualizada.getIdPlanificacionProyecto());

        assertEquals(12, actualizada.getCantidadClases());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void rechazaFechasIguales() throws Exception {

        Proyecto proyecto = crearProyectoPrueba();

        String cuerpo = crearJson(
                "2027-03-15",
                "2027-03-15",
                6,
                "MENSUAL");

        mockMvc.perform(
                        put("/api/proyectos/{idProyecto}/planificacion",
                                proyecto.getIdProyecto())
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cuerpo))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void rechazaCantidadClasesIgualACero() throws Exception {

        Proyecto proyecto = crearProyectoPrueba();

        String cuerpo = crearJson(
                "2027-03-15",
                "2027-09-14",
                0,
                "MENSUAL");

        mockMvc.perform(
                        put("/api/proyectos/{idProyecto}/planificacion",
                                proyecto.getIdProyecto())
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cuerpo))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void rechazaFrecuenciaNoPermitida() throws Exception {

        Proyecto proyecto = crearProyectoPrueba();

        String cuerpo = crearJson(
                "2027-03-15",
                "2027-09-14",
                6,
                "DIARIA");

        mockMvc.perform(
                        put("/api/proyectos/{idProyecto}/planificacion",
                                proyecto.getIdProyecto())
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cuerpo))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void rechazaPlanificacionDeProyectoInexistente() throws Exception {

        String cuerpo = crearJson(
                "2027-03-15",
                "2027-09-14",
                6,
                "MENSUAL");

        mockMvc.perform(
                        put("/api/proyectos/{idProyecto}/planificacion", -1L)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cuerpo))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void devuelveErrorSiNoExistePlanificacion() throws Exception {

        Proyecto proyecto = crearProyectoPrueba();

        mockMvc.perform(
                        get("/api/proyectos/{idProyecto}/planificacion",
                                proyecto.getIdProyecto()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "OPERATIVO")
    void operativoNoPuedeConsultarPlanificacion() throws Exception {

        Proyecto proyecto = crearProyectoPrueba();
        crearPlanificacionPrueba(proyecto);

        mockMvc.perform(
                        get("/api/proyectos/{idProyecto}/planificacion",
                                proyecto.getIdProyecto()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EXTERNO")
    void externoNoPuedeConsultarPlanificacion() throws Exception {

        Proyecto proyecto = crearProyectoPrueba();
        crearPlanificacionPrueba(proyecto);

        mockMvc.perform(
                        get("/api/proyectos/{idProyecto}/planificacion",
                                proyecto.getIdProyecto()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OPERATIVO")
    void operativoNoPuedeGuardarPlanificacion() throws Exception {

        Proyecto proyecto = crearProyectoPrueba();

        String cuerpo = crearJson(
                "2027-03-15",
                "2027-09-14",
                6,
                "MENSUAL");

        mockMvc.perform(
                        put("/api/proyectos/{idProyecto}/planificacion",
                                proyecto.getIdProyecto())
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cuerpo))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EXTERNO")
    void externoNoPuedeGuardarPlanificacion() throws Exception {

        Proyecto proyecto = crearProyectoPrueba();

        String cuerpo = crearJson(
                "2027-03-15",
                "2027-09-14",
                6,
                "MENSUAL");

        mockMvc.perform(
                        put("/api/proyectos/{idProyecto}/planificacion",
                                proyecto.getIdProyecto())
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cuerpo))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void rechazaGuardarPlanificacionSinTokenCsrf() throws Exception {

        Proyecto proyecto = crearProyectoPrueba();

        String cuerpo = crearJson(
                "2027-03-15",
                "2027-09-14",
                6,
                "MENSUAL");

        mockMvc.perform(
                        put("/api/proyectos/{idProyecto}/planificacion",
                                proyecto.getIdProyecto())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cuerpo))
                .andExpect(status().isForbidden());
    }
}
