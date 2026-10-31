
package org.tfg.backend.proyecto;

import java.time.Year;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import org.tfg.backend.empresa.Empresa;
import org.tfg.backend.empresa.EmpresaRepository;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ProyectoServiceTests {

    @Autowired
    private ProyectoService proyectoService;

    @Autowired
    private EmpresaRepository empresaRepository;

    private Empresa crearEmpresaPrueba() {

        String nombre = "Empresa " + UUID.randomUUID();

        Empresa empresa = new Empresa(nombre);

        return empresaRepository.save(empresa);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void administradorPuedeCrearProyectoDelAnioActual() {

        Empresa empresa = crearEmpresaPrueba();

        short anioActual = (short) Year.now().getValue();

        Proyecto proyecto = proyectoService.crearProyecto(
                "Proyecto educativo",
                anioActual,
                "Talleres de animación",
                empresa.getIdEmpresa()
        );

        assertNotNull(proyecto.getIdProyecto());
        assertEquals("Proyecto educativo", proyecto.getNombre());
        assertEquals(anioActual, proyecto.getAnio());
        assertEquals(EstadoProyecto.PENDIENTE_PLANIFICACION, proyecto.getEstado());
        assertEquals(empresa.getIdEmpresa(), proyecto.getEmpresa().getIdEmpresa());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void administradorPuedeCrearProyectoDeAnioFuturo() {

        Empresa empresa = crearEmpresaPrueba();

        short anioFuturo = (short) (Year.now().getValue() + 1);

        Proyecto proyecto = proyectoService.crearProyecto(
                "Proyecto futuro",
                anioFuturo,
                null,
                empresa.getIdEmpresa()
        );

        assertNotNull(proyecto.getIdProyecto());
        assertEquals(anioFuturo, proyecto.getAnio());
        assertNull(proyecto.getDescripcion());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void noPermiteCrearProyectoConAnioAnterior() {

        Empresa empresa = crearEmpresaPrueba();

        short anioAnterior = (short) (Year.now().getValue() - 1);

        try {
            proyectoService.crearProyecto(
                    "Proyecto anterior",
                    anioAnterior,
                    null,
                    empresa.getIdEmpresa()
            );

            fail("Se esperaba un error de validación");

        } catch (IllegalArgumentException excepcion) {

            assertEquals(
                    "El año del proyecto debe ser igual o posterior al año actual", excepcion.getMessage()
            );
        }
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void noPermiteCrearProyectoSinNombre() {

        Empresa empresa = crearEmpresaPrueba();

        short anioActual = (short) Year.now().getValue();

        try {
            proyectoService.crearProyecto(
                    "   ",
                    anioActual,
                    null,
                    empresa.getIdEmpresa()
            );

            fail("Se esperaba un error de validación");

        } catch (IllegalArgumentException excepcion) {

            assertEquals(
                    "El nombre del proyecto es obligatorio", excepcion.getMessage()
            );
        }
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void noPermiteCrearProyectoConEmpresaInexistente() {

        short anioActual = (short) Year.now().getValue();

        try {
            proyectoService.crearProyecto(
                    "Proyecto educativo",
                    anioActual,
                    null,
                    -1L
            );

            fail("Se esperaba un error por empresa inexistente");

        } catch (NoSuchElementException excepcion) {

            assertEquals(
                    "La empresa financiadora no existe", excepcion.getMessage()
            );
        }
    }

    @Test
    @WithMockUser(roles = "OPERATIVO")
    void operativoNoPuedeCrearProyecto() {

        short anioActual = (short) Year.now().getValue();

        try {
            proyectoService.crearProyecto(
                    "Proyecto no autorizado",
                    anioActual,
                    null,
                    1L
            );

            fail("El usuario operativo no debe poder crear proyectos");

        } catch (AccessDeniedException excepcion) {
            // Acceso denegado correctamente.
        }
    }

    @Test
    @WithMockUser(roles = "EXTERNO")
    void externoNoPuedeCrearProyecto() {

        short anioActual = (short) Year.now().getValue();

        try {
            proyectoService.crearProyecto(
                    "Proyecto no autorizado",
                    anioActual,
                    null,
                    1L
            );

            fail("El usuario externo no debe poder crear proyectos");

        } catch (AccessDeniedException excepcion) {
            // Acceso denegado correctamente.
        }
    }
}
