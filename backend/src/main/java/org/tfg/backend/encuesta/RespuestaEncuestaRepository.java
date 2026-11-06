package org.tfg.backend.encuesta;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RespuestaEncuestaRepository extends JpaRepository<RespuestaEncuesta, Long> {
    @Query("select r from RespuestaEncuesta r join fetch r.encuesta e join fetch e.clase c "
            + "join fetch r.pregunta p where c.taller.idTaller = :idTaller "
            + "order by c.numeroClase, p.orden, p.idPreguntaProyecto")
    List<RespuestaEncuesta> buscarPorTaller(@Param("idTaller") Long idTaller);
}
