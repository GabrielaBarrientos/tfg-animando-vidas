
package org.tfg.backend.planificacion;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.prepost.PreAuthorize;

import org.tfg.backend.proyecto.Proyecto;
import org.tfg.backend.proyecto.ProyectoRepository;

@Service
public class PlanificacionProyectoService {

    private final PlanificacionProyectoRepository planificacionRepository;
    private final ProyectoRepository proyectoRepository;

    public PlanificacionProyectoService(
            PlanificacionProyectoRepository planificacionRepository,
            ProyectoRepository proyectoRepository) {

        this.planificacionRepository = planificacionRepository;
        this.proyectoRepository = proyectoRepository;
    }

    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public PlanificacionProyectoResponse guardar(Long idProyecto, PlanificacionProyectoRequest request) {

        validarDatos(request);

        Optional<Proyecto> proyectoEncontrado = proyectoRepository.findById(idProyecto);

        if (proyectoEncontrado.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado");
        }

        Proyecto proyecto = proyectoEncontrado.get();

        Optional<PlanificacionProyecto> existente = planificacionRepository.findByProyecto_IdProyecto(idProyecto);

        PlanificacionProyecto planificacion;

        if (existente.isPresent()) {
            planificacion = existente.get();

            planificacion.actualizarDatos(
                    request.fechaInicio(),
                    request.fechaFinalizacion(),
                    request.cantidadClases(),
                    request.frecuenciaClases());

        } else {
            planificacion = new PlanificacionProyecto(
                    proyecto,
                    request.fechaInicio(),
                    request.fechaFinalizacion(),
                    request.cantidadClases(),
                    request.frecuenciaClases());
        }

        planificacion = planificacionRepository.save(planificacion);

        return convertirRespuesta(planificacion);
    }

    @Transactional(readOnly = true)
    public PlanificacionProyectoResponse obtener(Long idProyecto) {

        Optional<PlanificacionProyecto> resultado = planificacionRepository.findByProyecto_IdProyecto(idProyecto);

        if (resultado.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Planificación no encontrada");
        }

        PlanificacionProyecto planificacion = resultado.get();

        return convertirRespuesta(planificacion);
    }

    private void validarDatos(PlanificacionProyectoRequest request) {

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los datos de planificación son obligatorios");
        }

        if (request.fechaInicio() == null ||
                request.fechaFinalizacion() == null) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las fechas de inicio y finalización son obligatorias");
        }

        if (!request.fechaFinalizacion().isAfter(request.fechaInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha de finalización debe ser posterior a la de inicio");
        }

        if (request.cantidadClases() == null ||
                request.cantidadClases() <= 0) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad de clases debe ser mayor que cero");
        }

        if (request.frecuenciaClases() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La frecuencia de clases es obligatoria");
        }

        long maximoClases = calcularMaximoClases(request.fechaInicio(), request.fechaFinalizacion(), request.frecuenciaClases());

        if (request.cantidadClases() > maximoClases) {

            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad de clases supera el máximo permitido de "
                            + maximoClases
                            + " para el período y la frecuencia seleccionados");
        }

    }

    private PlanificacionProyectoResponse convertirRespuesta(PlanificacionProyecto planificacion) {

        long duracionMeses = calcularDuracionMeses(
                planificacion.getFechaInicio(),
                planificacion.getFechaFinalizacion());

        return new PlanificacionProyectoResponse(
                planificacion.getIdPlanificacionProyecto(),
                planificacion.getProyecto().getIdProyecto(),
                planificacion.getFechaInicio(),
                planificacion.getFechaFinalizacion(),
                planificacion.getCantidadClases(),
                planificacion.getFrecuenciaClases(),
                duracionMeses);
    }

    private long calcularDuracionMeses(LocalDate fechaInicio, LocalDate fechaFinalizacion) {

        YearMonth mesInicio = YearMonth.from(fechaInicio);
        YearMonth mesFinalizacion = YearMonth.from(fechaFinalizacion);

        return ChronoUnit.MONTHS.between(mesInicio, mesFinalizacion) + 1;
    }

    private long calcularMaximoClases(LocalDate fechaInicio, LocalDate fechaFinalizacion, FrecuenciaClases frecuencia) {

        long diasTotales = ChronoUnit.DAYS.between(fechaInicio, fechaFinalizacion) + 1;

        if (frecuencia == FrecuenciaClases.SEMANAL) {
            return (diasTotales + 6) / 7;
        }

        if (frecuencia == FrecuenciaClases.QUINCENAL) {
            return (diasTotales + 13) / 14;
        }

        if (frecuencia == FrecuenciaClases.MENSUAL) {

            YearMonth mesInicio = YearMonth.from(fechaInicio);
            YearMonth mesFinalizacion = YearMonth.from(fechaFinalizacion);

            long mesesTranscurridos = ChronoUnit.MONTHS.between(mesInicio, mesFinalizacion);

            int diaClase = Math.min(fechaInicio.getDayOfMonth(), mesFinalizacion.lengthOfMonth());

            LocalDate ultimaFechaPosible = mesFinalizacion.atDay(diaClase);

            long maximoClases = mesesTranscurridos;

            if (!ultimaFechaPosible.isAfter(fechaFinalizacion)) {
                maximoClases++;
            }

            return maximoClases;
        }

        throw new IllegalArgumentException("Frecuencia de clases no permitida");
    }

}
