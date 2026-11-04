package org.tfg.backend.taller;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaseTallerRepository extends JpaRepository<ClaseTaller, ClaseTallerId> {
    List<ClaseTaller> findByTaller_IdTallerPlataformaOrderByNumeroClaseAsc(String idTallerPlataforma);
    void deleteByTaller_IdTallerPlataforma(String idTallerPlataforma);
}
