package org.tfg.backend.docente;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocenteProyectoRepository extends JpaRepository<DocenteProyecto, DocenteProyectoId> {
    long countByProyecto_IdProyecto(Long idProyecto);
    List<DocenteProyecto> findByProyecto_IdProyectoOrderByDocente_ApellidosAscDocente_NombresAsc(Long idProyecto);
    Optional<DocenteProyecto> findByDocente_IdDocenteAndProyecto_IdProyecto(Long idDocente, Long idProyecto);
    boolean existsByProyecto_IdProyectoAndDocente_EmailIgnoreCase(Long idProyecto, String email);
    boolean existsByProyecto_IdProyectoAndUsuarioPlataformaIgnoreCase(Long idProyecto, String usuario);
    boolean existsByProyecto_IdProyectoAndUsuarioPlataformaIgnoreCaseAndDocente_IdDocenteNot(
            Long idProyecto, String usuario, Long idDocente);
    boolean existsByDocente_IdDocente(Long idDocente);
}
