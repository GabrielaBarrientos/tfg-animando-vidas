package org.tfg.backend.docente;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocenteRepository extends JpaRepository<Docente, Long> {
    Optional<Docente> findByEmailIgnoreCase(String email);
}
