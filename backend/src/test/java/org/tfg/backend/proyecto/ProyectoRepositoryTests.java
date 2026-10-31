
package org.tfg.backend.proyecto;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import org.tfg.backend.empresa.Empresa;
import org.tfg.backend.empresa.EmpresaRepository;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ProyectoRepositoryTests {

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void debeGuardarProyectoConEstadoInicial() {

        String identificador = UUID.randomUUID().toString();

        Empresa empresa = new Empresa("Empresa de prueba " + identificador);

        empresa = empresaRepository.saveAndFlush(empresa);

        Proyecto proyecto = new Proyecto(
                "Proyecto de prueba",
                (short) 2026,
                "Descripción del proyecto",
                empresa
        );

        proyecto = proyectoRepository.saveAndFlush(proyecto);

        Long idProyecto = proyecto.getIdProyecto();

        entityManager.clear();

        Optional<Proyecto> resultado = proyectoRepository.findById(idProyecto);

        assertTrue(resultado.isPresent());

        Proyecto proyectoRecuperado = resultado.get();

        assertEquals("Proyecto de prueba", proyectoRecuperado.getNombre());

        assertEquals((short) 2026, proyectoRecuperado.getAnio());

        assertEquals("Descripción del proyecto", proyectoRecuperado.getDescripcion());

        assertEquals(EstadoProyecto.PENDIENTE_PLANIFICACION, proyectoRecuperado.getEstado());

        assertEquals(empresa.getIdEmpresa(), proyectoRecuperado.getEmpresa().getIdEmpresa());

        assertNotNull(proyectoRecuperado.getFechaCreacion());

        assertNotNull(proyectoRecuperado.getFechaActualizacion());
    }

    @Test
    void debeRetornarVacioCuandoProyectoNoExiste() {

        Optional<Proyecto> resultado = proyectoRepository.findById(-1L);

        assertTrue(resultado.isEmpty());
    }
}
