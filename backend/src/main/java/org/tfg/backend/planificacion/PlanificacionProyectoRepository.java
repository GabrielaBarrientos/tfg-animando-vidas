
package org.tfg.backend.planificacion;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanificacionProyectoRepository extends JpaRepository<PlanificacionProyecto, Long> {

    Optional<PlanificacionProyecto> findByProyecto_IdProyecto(Long idProyecto);
}
