package org.tfg.backend.seguimiento;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;
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
import org.tfg.backend.planificacion.FrecuenciaClases;
import org.tfg.backend.planificacion.PlanificacionProyecto;
import org.tfg.backend.planificacion.PlanificacionProyectoRepository;
import org.tfg.backend.proyecto.EstadoProyecto;
import org.tfg.backend.proyecto.Proyecto;
import org.tfg.backend.proyecto.ProyectoRepository;
import org.tfg.backend.usuario.RolUsuario;
import org.tfg.backend.usuario.Usuario;
import org.tfg.backend.usuario.UsuarioRepository;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ConfiguracionSeguimientoServiceTests {

    @Autowired
    private ConfiguracionSeguimientoService seguimientoService;

    @Autowired
    private ConfiguracionSeguimientoRepository seguimientoRepository;

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private PlanificacionProyectoRepository planificacionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Proyecto crearProyecto(boolean planificado) {
        Empresa empresa = empresaRepository.saveAndFlush(new Empresa("Empresa " + UUID.randomUUID()));
        Proyecto proyecto = proyectoRepository.saveAndFlush(new Proyecto(
                "Proyecto prueba", (short) Year.now().getValue(), "Prueba", empresa));

        if (planificado) {
            PlanificacionProyecto planificacion = new PlanificacionProyecto(
                    proyecto,
                    LocalDate.of(2027, 1, 1),
                    LocalDate.of(2027, 7, 1),
                    6,
                    FrecuenciaClases.MENSUAL);
            planificacionRepository.saveAndFlush(planificacion);
        }
        return proyecto;
    }

    private Usuario crearUsuario(RolUsuario rol, boolean habilitado, Proyecto asignado) {
        Usuario usuario = new Usuario(
                null,
                null,
                "prueba-" + UUID.randomUUID() + "@example.org",
                "Ana",
                "Pérez",
                rol,
                habilitado,
                null,
                null);

        if (asignado != null) {
            usuario.getProyectosAsignados().add(asignado);
        }

        return usuarioRepository.saveAndFlush(usuario);
    }

    private ConfiguracionSeguimientoRequest solicitud(Long responsable) {
        return new ConfiguracionSeguimientoRequest(
                2, 3, responsable, DiaInformeSemanal.VIERNES,
                responsable, TipoDiaInformeMensual.ULTIMO_DIA_HABIL, null);
    }

    private void verificarBadRequest(Long idProyecto, ConfiguracionSeguimientoRequest datos) {
        try {
            seguimientoService.guardar(idProyecto, datos);
            fail("Se esperaba un error de validación");
        } catch (ResponseStatusException excepcion) {
            assertEquals(HttpStatus.BAD_REQUEST, excepcion.getStatusCode());
        }
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void guardaConfiguracionYMarcaProyectoPlanificado() {
        Proyecto proyecto = crearProyecto(true);
        Usuario usuario = crearUsuario(RolUsuario.ADMINISTRADOR, true, null);

        ConfiguracionSeguimientoResponse respuesta = seguimientoService.guardar(
                proyecto.getIdProyecto(), solicitud(usuario.getIdUsuario()));

        assertNotNull(respuesta.idConfiguracionSeguimiento());
        assertEquals(EstadoProyecto.PLANIFICADO, respuesta.estadoProyecto());
        assertEquals(2, respuesta.umbralProximoAtrasoSemanas());
        assertEquals(3, respuesta.umbralAtrasoSemanas());
        assertEquals(EstadoProyecto.PLANIFICADO,
                proyectoRepository.findById(proyecto.getIdProyecto()).get().getEstado());
        assertTrue(seguimientoRepository.findByProyecto_IdProyecto(proyecto.getIdProyecto()).isPresent());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void actualizaConfiguracionSinDuplicarRegistro() {
        Proyecto proyecto = crearProyecto(true);
        Usuario usuario = crearUsuario(RolUsuario.ADMINISTRADOR, true, null);
        ConfiguracionSeguimientoResponse primera = seguimientoService.guardar(
                proyecto.getIdProyecto(), solicitud(usuario.getIdUsuario()));

        ConfiguracionSeguimientoRequest nueva = new ConfiguracionSeguimientoRequest(
                1, 4, usuario.getIdUsuario(), DiaInformeSemanal.MARTES,
                usuario.getIdUsuario(), TipoDiaInformeMensual.DIA_DEL_MES, 15);
        ConfiguracionSeguimientoResponse segunda = seguimientoService.guardar(proyecto.getIdProyecto(), nueva);

        assertEquals(primera.idConfiguracionSeguimiento(), segunda.idConfiguracionSeguimiento());
        assertEquals(15, segunda.diaInformeMensual());
        assertEquals(DiaInformeSemanal.MARTES, segunda.diaInformeSemanal());
        assertTrue(seguimientoRepository.findByProyecto_IdProyecto(proyecto.getIdProyecto()).isPresent());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void noPermiteConfigurarProyectoSinPlanificacion() {
        Proyecto proyecto = crearProyecto(false);
        Usuario usuario = crearUsuario(RolUsuario.ADMINISTRADOR, true, null);

        verificarBadRequest(proyecto.getIdProyecto(), solicitud(usuario.getIdUsuario()));
        assertEquals(EstadoProyecto.PENDIENTE_PLANIFICACION, proyecto.getEstado());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void rechazaUmbralesInvalidos() {
        Proyecto proyecto = crearProyecto(true);
        Usuario usuario = crearUsuario(RolUsuario.ADMINISTRADOR, true, null);

        ConfiguracionSeguimientoRequest datos = new ConfiguracionSeguimientoRequest(
                3, 3, usuario.getIdUsuario(), DiaInformeSemanal.LUNES,
                usuario.getIdUsuario(), TipoDiaInformeMensual.ULTIMO_DIA_HABIL, null);
        verificarBadRequest(proyecto.getIdProyecto(), datos);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void rechazaDiaMensualFueraDeRango() {
        Proyecto proyecto = crearProyecto(true);
        Usuario usuario = crearUsuario(RolUsuario.ADMINISTRADOR, true, null);
        ConfiguracionSeguimientoRequest datos = new ConfiguracionSeguimientoRequest(
                2, 3, usuario.getIdUsuario(), DiaInformeSemanal.LUNES,
                usuario.getIdUsuario(), TipoDiaInformeMensual.DIA_DEL_MES, 32);

        verificarBadRequest(proyecto.getIdProyecto(), datos);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void rechazaDiaNumericoConUltimoDiaHabil() {
        Proyecto proyecto = crearProyecto(true);
        Usuario usuario = crearUsuario(RolUsuario.ADMINISTRADOR, true, null);
        ConfiguracionSeguimientoRequest datos = new ConfiguracionSeguimientoRequest(
                2, 3, usuario.getIdUsuario(), DiaInformeSemanal.LUNES,
                usuario.getIdUsuario(), TipoDiaInformeMensual.ULTIMO_DIA_HABIL, 12);
        verificarBadRequest(proyecto.getIdProyecto(), datos);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void permiteOperativoAsignadoPeroNoModificaAsignaciones() {
        Proyecto proyecto = crearProyecto(true);
        Usuario usuario = crearUsuario(RolUsuario.OPERATIVO, true, proyecto);

        ConfiguracionSeguimientoResponse respuesta = seguimientoService.guardar(
                proyecto.getIdProyecto(), solicitud(usuario.getIdUsuario()));

        assertEquals(usuario.getIdUsuario(), respuesta.idResponsableSeguimiento());
        assertEquals(1, usuario.getProyectosAsignados().size());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void rechazaOperativoSinAsignacionYUsuariosExternos() {
        Proyecto proyecto = crearProyecto(true);
        Usuario operativoNoAsignado = crearUsuario(RolUsuario.OPERATIVO, true, null);
        Usuario externo = crearUsuario(RolUsuario.EXTERNO, true, proyecto);

        verificarBadRequest(proyecto.getIdProyecto(), solicitud(operativoNoAsignado.getIdUsuario()));
        verificarBadRequest(proyecto.getIdProyecto(), solicitud(externo.getIdUsuario()));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void rechazaUsuarioDeshabilitado() {
        Proyecto proyecto = crearProyecto(true);
        Usuario inactivo = crearUsuario(RolUsuario.ADMINISTRADOR, false, null);
        verificarBadRequest(proyecto.getIdProyecto(), solicitud(inactivo.getIdUsuario()));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void listaSoloResponsablesElegibles() {
        Proyecto proyecto = crearProyecto(true);
        Usuario administrador = crearUsuario(RolUsuario.ADMINISTRADOR, true, null);
        Usuario operativo = crearUsuario(RolUsuario.OPERATIVO, true, proyecto);
        crearUsuario(RolUsuario.EXTERNO, true, proyecto);
        crearUsuario(RolUsuario.OPERATIVO, true, null);

        List<ResponsableElegibleResponse> elegibles =
                seguimientoService.listarResponsablesElegibles(proyecto.getIdProyecto());

        boolean administradorPresente = false;
        boolean operativoPresente = false;
        for (ResponsableElegibleResponse usuario : elegibles) {
            if (usuario.idUsuario().equals(administrador.getIdUsuario())) {
                administradorPresente = true;
            }
            if (usuario.idUsuario().equals(operativo.getIdUsuario())) {
                operativoPresente = true;
            }
            assertNotEquals(RolUsuario.EXTERNO, usuario.rol());
        }
        assertTrue(administradorPresente);
        assertTrue(operativoPresente);
    }

    @Test
    @WithMockUser(roles = "OPERATIVO")
    void operativoNoPuedeGuardar() {
        Proyecto proyecto = crearProyecto(true);
        Usuario usuario = crearUsuario(RolUsuario.ADMINISTRADOR, true, null);
        try {
            seguimientoService.guardar(proyecto.getIdProyecto(), solicitud(usuario.getIdUsuario()));
            fail("Un usuario operativo no debería modificar el seguimiento");
        } catch (AccessDeniedException excepcion) {
            assertNotNull(excepcion);
        }
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void consultaInexistenteRespondeNotFound() {
        Proyecto proyecto = crearProyecto(true);
        try {
            seguimientoService.obtener(proyecto.getIdProyecto());
            fail("Se esperaba que la configuración no existiera");
        } catch (ResponseStatusException excepcion) {
            assertEquals(HttpStatus.NOT_FOUND, excepcion.getStatusCode());
        }
    }
}
