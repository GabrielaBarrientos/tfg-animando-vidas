package org.tfg.backend.comunicacion;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComunicacionTallerRepository extends JpaRepository<ComunicacionTaller, Long> {
    List<ComunicacionTaller> findByTaller_IdTallerOrderByFechaContactoDescIdComunicacionDesc(Long idTaller);
    Optional<ComunicacionTaller> findByIdComunicacionAndTaller_IdTaller(Long idComunicacion, Long idTaller);
}
