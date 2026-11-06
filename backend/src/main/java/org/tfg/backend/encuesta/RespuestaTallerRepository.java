package org.tfg.backend.encuesta;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RespuestaTallerRepository extends JpaRepository<RespuestaTaller, Long> {
    @Query("select r from RespuestaTaller r join fetch r.clase c "
            + "join fetch r.pregunta p where c.taller.idTaller = :idTaller "
            + "order by c.numeroClase, p.orden, p.idPreguntaProyecto")
    List<RespuestaTaller> buscarPorTaller(@Param("idTaller") Long idTaller);
}
