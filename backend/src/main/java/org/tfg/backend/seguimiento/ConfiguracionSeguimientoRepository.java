package org.tfg.backend.seguimiento;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracionSeguimientoRepository extends JpaRepository<ConfiguracionSeguimiento, Long> {

    Optional<ConfiguracionSeguimiento> findByProyecto_IdProyecto(Long idProyecto);
}
