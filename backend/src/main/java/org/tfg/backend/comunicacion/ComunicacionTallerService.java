package org.tfg.backend.comunicacion;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.tfg.backend.taller.Taller;
import org.tfg.backend.taller.TallerService;
import org.tfg.backend.usuario.RolUsuario;
import org.tfg.backend.usuario.Usuario;
import org.tfg.backend.usuario.UsuarioAutorizacionService;

@Service
public class ComunicacionTallerService {
    private final TallerService tallerService;
    private final UsuarioAutorizacionService usuarios;
    private final ComunicacionTallerRepository comunicaciones;

    public ComunicacionTallerService(TallerService tallerService, UsuarioAutorizacionService usuarios,
            ComunicacionTallerRepository comunicaciones) {
        this.tallerService = tallerService;
        this.usuarios = usuarios;
        this.comunicaciones = comunicaciones;
    }

    @Transactional(readOnly = true)
    public List<ComunicacionResponse> listar(Long idProyecto, String idTaller, OidcUser principal) {
        verificarUsuarioInterno(principal);
        Taller taller = tallerService.buscarTallerAutorizado(idProyecto, idTaller, principal);
        List<ComunicacionResponse> resultado = new ArrayList<>();
        for (ComunicacionTaller comunicacion :
                comunicaciones.findByTaller_IdTallerOrderByFechaContactoDescIdComunicacionDesc(taller.getIdTaller())) {
            resultado.add(ComunicacionResponse.desdeEntidad(comunicacion));
        }
        return resultado;
    }

    @Transactional
    public ComunicacionResponse crear(Long idProyecto, String idTaller, OidcUser principal,
            CrearComunicacionRequest datos) {
        Usuario usuario = verificarUsuarioInterno(principal);
        Taller taller = tallerService.buscarTallerAutorizado(idProyecto, idTaller, principal);
        if (datos == null) {
            throw errorValidacion("Los datos de la comunicación son obligatorios");
        }
        validarFechaContacto(datos.fechaContacto());
        if (datos.canal() == null || datos.motivo() == null || datos.accionRealizada() == null) {
            throw errorValidacion("Canal, motivo y acción realizada son obligatorios");
        }
        String canalOtro = validarTextoOtro(datos.canal() == CanalComunicacion.OTRO,
                datos.canalOtro(), "canal");
        String motivoOtro = validarTextoOtro(datos.motivo() == MotivoComunicacion.OTRO,
                datos.motivoOtro(), "motivo");
        String observaciones = normalizarTexto(datos.observaciones(), 5000, "Observaciones");
        validarSeguimiento(datos.fechaContacto(), datos.resultado(), null, null, datos.fechaCompromiso());
        ComunicacionTaller entidad = new ComunicacionTaller(taller, usuario, datos,
                canalOtro, motivoOtro, observaciones);
        comunicaciones.saveAndFlush(entidad);
        return ComunicacionResponse.desdeEntidad(entidad);
    }

    @Transactional
    public ComunicacionResponse actualizarSeguimiento(Long idProyecto, String idTaller,
            Long idComunicacion, OidcUser principal, ActualizarSeguimientoRequest datos) {
        verificarUsuarioInterno(principal);
        Taller taller = tallerService.buscarTallerAutorizado(idProyecto, idTaller, principal);
        if (idComunicacion == null || idComunicacion <= 0) {
            throw errorValidacion("Identificador de comunicación inválido");
        }
        Optional<ComunicacionTaller> encontrada = comunicaciones
                .findByIdComunicacionAndTaller_IdTaller(idComunicacion, taller.getIdTaller());
        if (encontrada.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "La comunicación no pertenece al taller solicitado");
        }
        if (datos == null) {
            throw errorValidacion("Los datos del seguimiento son obligatorios");
        }
        ComunicacionTaller entidad = encontrada.get();
        String respuesta = normalizarTexto(datos.respuestaDocente(), 5000, "Respuesta del docente");
        validarSeguimiento(entidad.getFechaContacto(), datos.resultado(),
                datos.fechaRespuestaDocente(), respuesta, datos.fechaCompromiso());
        entidad.actualizarSeguimiento(datos, respuesta);
        comunicaciones.saveAndFlush(entidad);
        return ComunicacionResponse.desdeEntidad(entidad);
    }

    private Usuario verificarUsuarioInterno(OidcUser principal) {
        if (principal == null || principal.getSubject() == null) {
            throw new AccessDeniedException("Usuario no autenticado");
        }
        Optional<Usuario> encontrado = usuarios.buscarUsuarioAutorizado(principal.getSubject());
        if (encontrado.isEmpty()) {
            throw new AccessDeniedException("Usuario no autorizado");
        }
        Usuario usuario = encontrado.get();
        if (usuario.getRol() != RolUsuario.ADMINISTRADOR && usuario.getRol() != RolUsuario.OPERATIVO) {
            throw new AccessDeniedException("Las comunicaciones son exclusivas de usuarios internos");
        }
        return usuario;
    }

    private void validarFechaContacto(LocalDate fecha) {
        if (fecha == null) {
            throw errorValidacion("La fecha de contacto es obligatoria");
        }
        if (fecha.isAfter(LocalDate.now())) {
            throw errorValidacion("La fecha de contacto no puede ser futura");
        }
    }

    private String validarTextoOtro(boolean esOtro, String valor, String campo) {
        String texto = normalizarTexto(valor, 20, "Detalle de " + campo);
        if (esOtro && texto == null) {
            throw errorValidacion("Debés especificar el valor de Otro para " + campo);
        }
        if (!esOtro && texto != null) {
            throw errorValidacion("El detalle de " + campo + " solo se admite al elegir Otro");
        }
        return texto;
    }

    private String normalizarTexto(String texto, int maximo, String nombre) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpio = texto.trim();
        if (limpio.length() > maximo) {
            throw errorValidacion(nombre + " supera el máximo de " + maximo + " caracteres");
        }
        return limpio;
    }

    private void validarSeguimiento(LocalDate contacto, ResultadoComunicacion resultado,
            LocalDate fechaRespuesta, String respuesta, LocalDate compromiso) {
        if (resultado == null) {
            throw errorValidacion("El resultado es obligatorio");
        }
        if (fechaRespuesta != null && fechaRespuesta.isBefore(contacto)) {
            throw errorValidacion("La respuesta no puede ser anterior al contacto");
        }
        if (fechaRespuesta != null && fechaRespuesta.isAfter(LocalDate.now())) {
            throw errorValidacion("La fecha de respuesta no puede ser futura");
        }
        if ((respuesta == null) != (fechaRespuesta == null)) {
            throw errorValidacion("Ingresá la respuesta del docente y su fecha conjuntamente");
        }
        if ((resultado == ResultadoComunicacion.ESPERANDO_RESPUESTA
                || resultado == ResultadoComunicacion.NO_SE_PUDO_CONTACTAR)
                && fechaRespuesta != null) {
            throw errorValidacion("El resultado seleccionado no admite una respuesta del docente");
        }
        if (compromiso != null && compromiso.isBefore(contacto)) {
            throw errorValidacion("La fecha de compromiso no puede ser anterior al contacto");
        }
        if (compromiso != null && fechaRespuesta != null && compromiso.isBefore(fechaRespuesta)) {
            throw errorValidacion("La fecha de compromiso no puede ser anterior a la respuesta");
        }
        if (compromiso != null && resultado != ResultadoComunicacion.COMPROMISO_DE_REGULARIZACION) {
            throw errorValidacion("La fecha de compromiso exige el resultado Compromiso de regularización");
        }
    }

    private ResponseStatusException errorValidacion(String detalle) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, detalle);
    }
}
