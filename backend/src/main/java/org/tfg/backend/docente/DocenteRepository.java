package org.tfg.backend.docente;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocenteRepository extends JpaRepository<Docente, Long> {
    List<Docente> findByProyecto_IdProyectoOrderByApellidosAscNombresAsc(Long idProyecto);

    Optional<Docente> findByIdDocenteAndProyecto_IdProyecto(Long idDocente, Long idProyecto);

    boolean existsByProyecto_IdProyectoAndEmailIgnoreCase(Long idProyecto, String email);

    boolean existsByProyecto_IdProyectoAndEmailIgnoreCaseAndIdDocenteNot(
            Long idProyecto, String email, Long idDocente);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM asociacion_taller_docente "
            + "WHERE id_docente = :idDocente)", nativeQuery = true)
    boolean tieneTalleresAsociados(@Param("idDocente") Long idDocente);
}
