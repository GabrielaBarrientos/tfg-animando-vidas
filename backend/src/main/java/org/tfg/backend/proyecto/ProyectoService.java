
package org.tfg.backend.proyecto;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.tfg.backend.empresa.Empresa;
import org.tfg.backend.empresa.EmpresaRepository;

import java.time.Year;

@Service
public class ProyectoService {

    private final ProyectoRepository proyectoRepository;
    private final EmpresaRepository empresaRepository;

    public ProyectoService(ProyectoRepository proyectoRepository, EmpresaRepository empresaRepository) {

        this.proyectoRepository = proyectoRepository;
        this.empresaRepository = empresaRepository;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Proyecto crearProyecto(
            String nombre,
            Short anio,
            String descripcion,
            Long idEmpresa) {

        // Validar los datos obligatorios
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del proyecto es obligatorio");
        }

        int anioActual = Year.now().getValue();

        if (anio == null || anio < anioActual) {
            throw new IllegalArgumentException("El año del proyecto debe ser igual o posterior al año actual");
        }

        if (idEmpresa == null) {
            throw new IllegalArgumentException("La empresa financiadora es obligatoria");
        }

        // Buscar la empresa financiadora
        Optional<Empresa> empresaEncontrada = empresaRepository.findById(idEmpresa);

        if (empresaEncontrada.isEmpty()) {
            throw new NoSuchElementException("La empresa financiadora no existe");
        }

        // Preparar los datos
        String nombreProyecto = nombre.trim();

        String descripcionProyecto = descripcion;

        if (descripcionProyecto != null) {
            descripcionProyecto = descripcionProyecto.trim();

            if (descripcionProyecto.isEmpty()) {
                descripcionProyecto = null;
            }
        }

        // Crear y guardar el proyecto
        Proyecto proyecto = new Proyecto(
                nombreProyecto,
                anio,
                descripcionProyecto,
                empresaEncontrada.get()
        );

        return proyectoRepository.save(proyecto);
    }
}
