package org.tfg.backend.encuesta;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EncuestaTallerRepository extends JpaRepository<EncuestaTaller, Long> {
    List<EncuestaTaller> findByClase_Taller_IdTaller(Long idTaller);
}
