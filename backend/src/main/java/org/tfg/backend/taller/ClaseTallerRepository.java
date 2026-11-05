package org.tfg.backend.taller;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaseTallerRepository extends JpaRepository<ClaseTaller, Long> {
    List<ClaseTaller> findByTaller_IdTallerPlataformaOrderByNumeroClaseAsc(String idTallerPlataforma);
    Optional<ClaseTaller> findByIdClasePlataforma(Long idClasePlataforma);
    void deleteByTaller_IdTallerPlataforma(String idTallerPlataforma);
}
