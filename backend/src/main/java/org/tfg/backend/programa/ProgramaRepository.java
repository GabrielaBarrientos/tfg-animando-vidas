package org.tfg.backend.programa;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgramaRepository extends JpaRepository<Programa, Long> {
    List<Programa> findByProyecto_IdProyectoOrderByNumeroClaseAsc(Long idProyecto);
}
