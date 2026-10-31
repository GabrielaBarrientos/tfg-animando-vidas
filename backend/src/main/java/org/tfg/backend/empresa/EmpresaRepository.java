
package org.tfg.backend.empresa;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    Optional<Empresa> findByNombreIgnoreCase(String nombre);
}
