package org.tfg.backend.encuesta;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreguntaProyectoRepository extends JpaRepository<PreguntaProyecto, Long> {
    List<PreguntaProyecto> findByProyecto_IdProyectoOrderByIdPreguntaProyectoAsc(Long idProyecto);
    List<PreguntaProyecto> findByPrograma_IdProgramaOrderByOrdenAsc(Long idPrograma);
}
