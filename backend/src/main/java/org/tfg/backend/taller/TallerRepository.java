package org.tfg.backend.taller;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TallerRepository extends JpaRepository<Taller, Long> {
    Optional<Taller> findByIdTallerPlataforma(String idTallerPlataforma);
    List<Taller> findByAsignacion_Proyecto_IdProyectoOrderByIdTallerPlataformaAsc(Long idProyecto);
    boolean existsByAsignacion_Proyecto_IdProyecto(Long idProyecto);
    boolean existsByAsignacion_Docente_IdDocenteAndAsignacion_Proyecto_IdProyecto(Long idDocente, Long idProyecto);
}
