
package org.tfg.backend.planificacion;

import java.time.LocalDate;
import java.time.Year;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.tfg.backend.empresa.Empresa;
import org.tfg.backend.empresa.EmpresaRepository;
import org.tfg.backend.proyecto.EstadoProyecto;
import org.tfg.backend.proyecto.Proyecto;
import org.tfg.backend.proyecto.ProyectoRepository;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PlanificacionProyectoServiceTests {

    @Autowired
    private PlanificacionProyectoService planificacionService;

    @Autowired
    private PlanificacionProyectoRepository planificacionRepository;

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

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

    private PlanificacionProyectoRequest crearSolicitudValida() {

        return new PlanificacionProyectoRequest(
                LocalDate.of(2027, 3, 15),
                LocalDate.of(2027, 9, 14),
                6,
                FrecuenciaClases.MENSUAL);
    }

    private void verificarSolicitudInvalida(Long idProyecto, PlanificacionProyectoRequest solicitud) {

        try {
            planificacionService.guardar(idProyecto, solicitud);

            fail("Se esperaba un error de validación");

        } catch (ResponseStatusException excepcion) {

            assertEquals(HttpStatus.BAD_REQUEST, excepcion.getStatusCode());
        }
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void administradorPuedeCrearPlanificacion() {

        Proyecto proyecto = crearProyectoPrueba();

        PlanificacionProyectoRequest solicitud = crearSolicitudValida();

        PlanificacionProyectoResponse respuesta =
                planificacionService.guardar(
                        proyecto.getIdProyecto(),
                        solicitud);

        assertNotNull(respuesta.idPlanificacionProyecto());

        assertEquals(proyecto.getIdProyecto(), respuesta.idProyecto());

        assertEquals(6, respuesta.cantidadClases());
        assertTrue(respuesta.configurada());

        assertEquals(FrecuenciaClases.MENSUAL, respuesta.frecuenciaClases());

        assertEquals(7L, respuesta.duracionMeses());

        Proyecto proyectoActualizado = proyectoRepository.findById(proyecto.getIdProyecto()).get();

        assertEquals(EstadoProyecto.PENDIENTE_PLANIFICACION, proyectoActualizado.getEstado());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void administradorPuedeActualizarPlanificacion() {

        Proyecto proyecto = crearProyectoPrueba();

        PlanificacionProyectoResponse primeraRespuesta = planificacionService.guardar(proyecto.getIdProyecto(),
                        crearSolicitudValida());

        PlanificacionProyectoRequest nuevaSolicitud = new PlanificacionProyectoRequest(
                        LocalDate.of(2027, 4, 1),
                        LocalDate.of(2027, 10, 31),
                        12,
                        FrecuenciaClases.QUINCENAL);

        PlanificacionProyectoResponse segundaRespuesta = planificacionService.guardar(proyecto.getIdProyecto(),
                        nuevaSolicitud);

        assertEquals(primeraRespuesta.idPlanificacionProyecto(), segundaRespuesta.idPlanificacionProyecto());

        assertEquals(12, segundaRespuesta.cantidadClases());

        assertEquals(FrecuenciaClases.QUINCENAL, segundaRespuesta.frecuenciaClases());

        assertEquals(7L, segundaRespuesta.duracionMeses());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void administradorPuedeConsultarPlanificacion() {

        Proyecto proyecto = crearProyectoPrueba();

        planificacionService.guardar(proyecto.getIdProyecto(), crearSolicitudValida());

        PlanificacionProyectoResponse respuesta = planificacionService.obtener(proyecto.getIdProyecto());

        assertTrue(respuesta.configurada());
        assertEquals(LocalDate.of(2027, 3, 15), respuesta.fechaInicio());

        assertEquals(LocalDate.of(2027, 9, 14), respuesta.fechaFinalizacion());

        assertEquals(7L, respuesta.duracionMeses());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void calculaDuracionIncluyendoAmbosMesesCalendario() {

        Proyecto proyecto = crearProyectoPrueba();

        PlanificacionProyectoRequest solicitud = new PlanificacionProyectoRequest(
                        LocalDate.of(2027, 3, 31),
                        LocalDate.of(2027, 4, 1),
                        1,
                        FrecuenciaClases.SEMANAL);

        PlanificacionProyectoResponse respuesta = planificacionService.guardar(
                        proyecto.getIdProyecto(),
                        solicitud);

        assertEquals(2L, respuesta.duracionMeses());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void noPermiteFechasIguales() {

        Proyecto proyecto = crearProyectoPrueba();

        PlanificacionProyectoRequest solicitud = new PlanificacionProyectoRequest(
                        LocalDate.of(2027, 3, 15),
                        LocalDate.of(2027, 3, 15),
                        6,
                        FrecuenciaClases.MENSUAL);

        verificarSolicitudInvalida(proyecto.getIdProyecto(), solicitud);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void noPermiteFechaFinalizacionAnterior() {

        Proyecto proyecto = crearProyectoPrueba();

        PlanificacionProyectoRequest solicitud = new PlanificacionProyectoRequest(
                        LocalDate.of(2027, 9, 15),
                        LocalDate.of(2027, 3, 15),
                        6,
                        FrecuenciaClases.MENSUAL);

        verificarSolicitudInvalida(proyecto.getIdProyecto(), solicitud);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void noPermiteCantidadClasesIgualACero() {

        Proyecto proyecto = crearProyectoPrueba();

        PlanificacionProyectoRequest solicitud = new PlanificacionProyectoRequest(
                        LocalDate.of(2027, 3, 15),
                        LocalDate.of(2027, 9, 14),
                        0,
                        FrecuenciaClases.MENSUAL);

        verificarSolicitudInvalida(proyecto.getIdProyecto(), solicitud);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void noPermiteFrecuenciaSinInformar() {

        Proyecto proyecto = crearProyectoPrueba();

        PlanificacionProyectoRequest solicitud = new PlanificacionProyectoRequest(
                        LocalDate.of(2027, 3, 15),
                        LocalDate.of(2027, 9, 14),
                        6,
                        null);

        verificarSolicitudInvalida(proyecto.getIdProyecto(), solicitud);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void noPermiteGuardarPlanificacionDeProyectoInexistente() {

        try {
            planificacionService.guardar(-1L, crearSolicitudValida());

            fail("Se esperaba un error por proyecto inexistente");

        } catch (ResponseStatusException excepcion) {

            assertEquals(HttpStatus.NOT_FOUND, excepcion.getStatusCode());
        }
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void proyectoExistenteSinPlanificacionDevuelveRespuestaPendiente() {

        Proyecto proyecto = crearProyectoPrueba();

        Optional<PlanificacionProyecto> resultado = planificacionRepository.findByProyecto_IdProyecto(
                        proyecto.getIdProyecto());
        assertTrue(resultado.isEmpty());

        PlanificacionProyectoResponse respuesta = planificacionService.obtener(proyecto.getIdProyecto());

        assertFalse(respuesta.configurada());
        assertEquals(proyecto.getIdProyecto(), respuesta.idProyecto());
        assertNull(respuesta.idPlanificacionProyecto());
        assertNull(respuesta.fechaInicio());
        assertNull(respuesta.fechaFinalizacion());
        assertNull(respuesta.cantidadClases());
        assertNull(respuesta.frecuenciaClases());
        assertNull(respuesta.duracionMeses());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void proyectoInexistenteNoTienePlanificacion() {

        try {
            planificacionService.obtener(-1L);
            fail("Se esperaba un error por proyecto inexistente");
        } catch (ResponseStatusException excepcion) {
            assertEquals(HttpStatus.NOT_FOUND, excepcion.getStatusCode());
        }
    }

    @Test
    @WithMockUser(roles = "OPERATIVO")
    void operativoNoPuedeGuardarPlanificacion() {

        Proyecto proyecto = crearProyectoPrueba();

        try {
            planificacionService.guardar(proyecto.getIdProyecto(), crearSolicitudValida());

            fail("El usuario operativo no debe guardar planificaciones");

        } catch (AccessDeniedException excepcion) {
            // Acceso denegado correctamente.
        }
    }

    @Test
    @WithMockUser(roles = "EXTERNO")
    void externoNoPuedeGuardarPlanificacion() {

        Proyecto proyecto = crearProyectoPrueba();

        try {
            planificacionService.guardar(proyecto.getIdProyecto(), crearSolicitudValida());

            fail("El usuario externo no debe guardar planificaciones");

        } catch (AccessDeniedException excepcion) {
            // Acceso denegado correctamente.
        }
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void noPermiteExcederClasesMensuales() {

        Proyecto proyecto = crearProyectoPrueba();

        PlanificacionProyectoRequest solicitud = new PlanificacionProyectoRequest(
                        LocalDate.of(2027, 3, 15),
                        LocalDate.of(2027, 9, 14),
                        7,
                        FrecuenciaClases.MENSUAL);

        verificarSolicitudInvalida(proyecto.getIdProyecto(), solicitud);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void noPermiteExcederClasesSemanales() {

        Proyecto proyecto = crearProyectoPrueba();

        PlanificacionProyectoRequest solicitud = new PlanificacionProyectoRequest(
                        LocalDate.of(2027, 3, 1),
                        LocalDate.of(2027, 3, 28),
                        5,
                        FrecuenciaClases.SEMANAL);

        verificarSolicitudInvalida(proyecto.getIdProyecto(), solicitud);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void noPermiteExcederClasesQuincenales() {

        Proyecto proyecto = crearProyectoPrueba();

        PlanificacionProyectoRequest solicitud = new PlanificacionProyectoRequest(
                        LocalDate.of(2027, 3, 1),
                        LocalDate.of(2027, 3, 28),
                        3,
                        FrecuenciaClases.QUINCENAL);

        verificarSolicitudInvalida(proyecto.getIdProyecto(), solicitud);
    }

}
