package org.tfg.backend.evidencia;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EvidenciaRepository extends JpaRepository<Evidencia, Long> {
    @Query("SELECT e FROM Evidencia e JOIN e.clase c "
            + "WHERE c.taller.idTaller = :idTaller "
            + "ORDER BY c.numeroClase ASC, e.idEvidencia ASC")
    List<Evidencia> findByTallerOrdenadas(@Param("idTaller") Long idTaller);
    List<Evidencia> findByClase_IdClaseTallerOrderByIdEvidenciaAsc(Long idClaseTaller);
    Optional<Evidencia> findByIdArchivoPlataforma(Long idArchivoPlataforma);
}
