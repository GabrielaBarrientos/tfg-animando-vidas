
package org.tfg.backend.proyecto;

import java.time.Year;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import org.tfg.backend.empresa.Empresa;
import org.tfg.backend.empresa.EmpresaRepository;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ProyectoConsultasRepositoryTests {

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Empresa crearEmpresa(String nombre) {

        Empresa empresa = new Empresa(nombre);

        return empresaRepository.saveAndFlush(empresa);
    }

    private Proyecto crearProyecto(String nombre, Empresa empresa) {

        short anioActual = (short) Year.now().getValue();

        Proyecto proyecto = new Proyecto(
                nombre,
                anioActual,
                null,
                empresa
        );

        return proyectoRepository.saveAndFlush(proyecto);
    }

    private Long crearUsuarioOperativo() {

        String email = "operativo."
                + UUID.randomUUID()
                + "@example.com";

        String sql =
                "INSERT INTO usuario "
                + "(email, nombres, apellidos, rol, "
                + "habilitado, f_creacion, f_actualizacion) "
                + "VALUES (?, ?, ?, 'OPERATIVO', TRUE, "
                + "CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) "
                + "RETURNING id_usuario";

        return jdbcTemplate.queryForObject(
                sql,
                Long.class,
                email,
                "Usuario",
                "Prueba"
        );
    }

    private boolean contieneProyecto(List<Proyecto> proyectos, Long idProyecto) {

        for (Proyecto proyecto : proyectos) {

            if (proyecto.getIdProyecto().equals(idProyecto)) {
                return true;
            }
        }

        return false;
    }

    @Test
    void debeConsultarSoloProyectosAsignadosAlUsuario() {

        Empresa empresa = crearEmpresa("Empresa operativa " + UUID.randomUUID());

        Proyecto proyectoAsignado = crearProyecto("Proyecto asignado",empresa);

        Proyecto proyectoNoAsignado = crearProyecto("Proyecto no asignado",empresa);

        Long idUsuario = crearUsuarioOperativo();

        jdbcTemplate.update(
                "INSERT INTO usuario_proyecto "
                + "(id_usuario, id_proyecto) VALUES (?, ?)",
                idUsuario,
                proyectoAsignado.getIdProyecto()
        );

        List<Proyecto> proyectos = proyectoRepository.buscarProyectosAsignados(idUsuario);

        assertEquals(1, proyectos.size());

        assertTrue(contieneProyecto(proyectos, proyectoAsignado.getIdProyecto()));

        assertFalse(contieneProyecto(proyectos, proyectoNoAsignado.getIdProyecto()));
    }

    @Test
    void debeConsultarSoloProyectosDeLaEmpresa() {

        Empresa empresaA = crearEmpresa("Empresa A " + UUID.randomUUID());

        Empresa empresaB = crearEmpresa("Empresa B " + UUID.randomUUID());

        Proyecto proyecto1 = crearProyecto("Proyecto 1", empresaA);

        Proyecto proyecto2 = crearProyecto("Proyecto 2", empresaA);

        Proyecto proyecto3 = crearProyecto("Proyecto 3", empresaB);

        List<Proyecto> proyectos = proyectoRepository.findByEmpresa_IdEmpresa(empresaA.getIdEmpresa());

        assertEquals(2, proyectos.size());

        assertTrue(contieneProyecto(proyectos, proyecto1.getIdProyecto()));

        assertTrue(contieneProyecto(proyectos, proyecto2.getIdProyecto()));

        assertFalse(contieneProyecto(proyectos, proyecto3.getIdProyecto()));
    }
}
