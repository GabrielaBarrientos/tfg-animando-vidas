package org.tfg.backend.web;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.tfg.backend.encuesta.EncuestasController;
import org.tfg.backend.programa.ProgramaController;
import org.tfg.backend.proyecto.ProyectoController;
import org.tfg.backend.taller.TallerController;

/** Devuelve errores descriptivos para las operaciones de integración educativa. */
@RestControllerAdvice(assignableTypes = {
        ProyectoController.class, ProgramaController.class,
        TallerController.class, EncuestasController.class
})
public class IntegracionesErrorHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> informarError(ResponseStatusException error) {
        ProblemDetail detalle = ProblemDetail.forStatus(error.getStatusCode());
        String motivo = error.getReason();
        detalle.setDetail(motivo != null && !motivo.isBlank()
                ? motivo : "No fue posible completar la operación solicitada");
        return ResponseEntity.status(error.getStatusCode()).body(detalle);
    }
}
