
package org.tfg.backend.proyecto;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProyectoRepository extends JpaRepository<Proyecto, Long> {

    // Proyectos asignados a un usuario operativo.
    @Query("SELECT p FROM Usuario u " +
           "JOIN u.proyectosAsignados p " +
           "WHERE u.idUsuario = :idUsuario")

    List<Proyecto> buscarProyectosAsignados(@Param("idUsuario") Long idUsuario);

    // Proyectos financiados por una empresa.
    List<Proyecto> findByEmpresa_IdEmpresa(Long idEmpresa);
}
