package org.tfg.backend.evidencia;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.tfg.backend.integration.educationalplatform.EducationalPlatformClient;
import org.tfg.backend.integration.educationalplatform.PlatformEvidenciasResponse;
import org.tfg.backend.taller.ClaseTaller;
import org.tfg.backend.taller.ClaseTallerRepository;
import org.tfg.backend.taller.Taller;
import org.tfg.backend.taller.TallerService;

@Service
public class EvidenciasService {
    private final TallerService talleres;
    private final ClaseTallerRepository clases;
    private final EvidenciaRepository evidencias;
    private final EducationalPlatformClient plataforma;

    public EvidenciasService(TallerService talleres, ClaseTallerRepository clases,
            EvidenciaRepository evidencias, EducationalPlatformClient plataforma) {
        this.talleres = talleres;
        this.clases = clases;
        this.evidencias = evidencias;
        this.plataforma = plataforma;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERATIVO', 'EXTERNO')")
    public EvidenciasResponse consultar(Long idProyecto, String idTaller, OidcUser google) {
        Taller taller = talleres.buscarTallerAutorizado(idProyecto, idTaller, google);
        List<EvidenciasResponse.Archivo> archivos = new ArrayList<>();
        List<Evidencia> guardadas = evidencias.findByTallerOrdenadas(
                taller.getIdTaller());
        for (Evidencia evidencia : guardadas) {
            archivos.add(convertir(evidencia));
        }
        return new EvidenciasResponse(taller.getUltimaSincronizacionEvidencias(), archivos);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERATIVO')")
    public int sincronizar(Long idProyecto, String idTaller, OidcUser google) {
        Taller taller = talleres.buscarTallerAutorizado(idProyecto, idTaller, google);
        if (taller.isPendienteBaja()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Taller pendiente de baja");
        }
        int externo = identificadorNumerico(taller.getIdTallerPlataforma());
        PlatformEvidenciasResponse remota;
        try {
            remota = plataforma.obtenerEvidencias(externo);
        } catch (Exception excepcion) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudieron consultar evidencias de la plataforma educativa", excepcion);
        }
        if (remota == null || remota.id_taller() == null || remota.id_taller() != externo
                || remota.evidencias() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "La plataforma devolvió evidencias incompletas o de otro taller");
        }
        Map<Long, ClaseTaller> porExterno = indexarClasesDelTaller(idTaller);
        validarEvidenciasRemotas(remota, porExterno);
        OffsetDateTime instante = OffsetDateTime.now(ZoneOffset.UTC);
        guardarMetadatos(remota, porExterno, instante);
        // Nunca se eliminan evidencias ni se alteran las valoraciones existentes.
        taller.registrarSincronizacionEvidencias(instante);
        return remota.evidencias().size();
    }

    private Map<Long, ClaseTaller> indexarClasesDelTaller(String idTaller) {
        List<ClaseTaller> clasesLocales = clases.findByTaller_IdTallerPlataformaOrderByNumeroClaseAsc(idTaller);
        Map<Long, ClaseTaller> porExterno = new HashMap<>();
        for (ClaseTaller clase : clasesLocales) {
            if (clase.getIdClasePlataforma() != null) {
                porExterno.put(clase.getIdClasePlataforma(), clase);
            }
        }
        return porExterno;
    }

    private void validarEvidenciasRemotas(PlatformEvidenciasResponse remota,
            Map<Long, ClaseTaller> porExterno) {
        Set<Long> identificadores = new HashSet<>();
        // Validar todas las evidencias antes de iniciar la persistencia.
        for (PlatformEvidenciasResponse.ArchivoPlataforma dato : remota.evidencias()) {
            validarMetadatos(dato, porExterno, identificadores);
            Optional<Evidencia> anterior = evidencias.findByIdArchivoPlataforma(dato.id_archivo_plataforma());
            if (anterior.isPresent()
                    && !anterior.get().getClase().getIdClasePlataforma().equals(dato.id_clase_plataforma())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El archivo ya pertenece a otra clase");
            }
        }
    }

    private void validarMetadatos(PlatformEvidenciasResponse.ArchivoPlataforma dato,
            Map<Long, ClaseTaller> porExterno, Set<Long> identificadores) {
        if (dato == null || dato.id_archivo_plataforma() == null
                || dato.id_archivo_plataforma() <= 0
                || dato.id_clase_plataforma() == null
                || !porExterno.containsKey(dato.id_clase_plataforma())
                || dato.nombre_fisico() == null || dato.nombre_fisico().isBlank()
                || dato.nombre_fisico().length() > 500
                || dato.mime_type() == null || dato.mime_type().length() > 120
                || dato.tamanio() == null || dato.tamanio() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "La plataforma devolvió metadatos de evidencias inválidos; actualizá primero las clases");
        }
        if (!identificadores.add(dato.id_archivo_plataforma())) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "La plataforma devolvió identificadores de evidencias duplicados");
        }
        if (!tipoPermitido(dato.mime_type())) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Tipo de archivo no permitido en evidencias");
        }
    }

    private void guardarMetadatos(PlatformEvidenciasResponse remota,
            Map<Long, ClaseTaller> porExterno, OffsetDateTime instante) {
        for (PlatformEvidenciasResponse.ArchivoPlataforma dato : remota.evidencias()) {
            ClaseTaller clase = porExterno.get(dato.id_clase_plataforma());
            Optional<Evidencia> existente = evidencias.findByIdArchivoPlataforma(dato.id_archivo_plataforma());
            Evidencia evidencia;
            if (existente.isPresent()) {
                evidencia = existente.get();
                evidencia.actualizar(dato.nombre_fisico(), dato.mime_type(), dato.tamanio(), instante);
            } else {
                evidencia = new Evidencia(clase, dato.id_archivo_plataforma(), dato.nombre_fisico(),
                        dato.mime_type(), dato.tamanio(), instante);
            }
            evidencias.save(evidencia);
        }
    }

    public static boolean tipoPermitido(String tipo) {
        return "image/jpeg".equals(tipo) || "image/png".equals(tipo)
                || "image/webp".equals(tipo) || "image/gif".equals(tipo)
                || "video/mp4".equals(tipo) || "video/webm".equals(tipo);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERATIVO')")
    public EvidenciasResponse.Archivo actualizarMarcadores(Long idProyecto, String idTaller,
            Long idEvidencia, MarcadoresRequest solicitud, OidcUser google) {
        Taller taller = talleres.buscarTallerAutorizado(idProyecto, idTaller, google);
        Evidencia evidencia = buscarEvidencia(taller, idEvidencia);
        if (solicitud == null || solicitud.esUtil() == null || solicitud.esDestacada() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los dos marcadores son obligatorios");
        }
        evidencia.marcar(solicitud.esUtil(), solicitud.esDestacada());
        return convertir(evidencia);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERATIVO', 'EXTERNO')")
    public Evidencia obtenerArchivoAutorizado(Long idProyecto, String idTaller,
            Long idEvidencia, OidcUser google) {
        Taller taller = talleres.buscarTallerAutorizado(idProyecto, idTaller, google);
        return buscarEvidencia(taller, idEvidencia);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'OPERATIVO')")
    public Evidencia obtenerArchivoParaDescargar(Long idProyecto, String idTaller,
            Long idEvidencia, OidcUser google) {
        Taller taller = talleres.buscarTallerAutorizado(idProyecto, idTaller, google);
        return buscarEvidencia(taller, idEvidencia);
    }

    private Evidencia buscarEvidencia(Taller taller, Long idEvidencia) {
        Optional<Evidencia> encontrada = evidencias.findById(idEvidencia);
        if (encontrada.isEmpty() || !taller.getIdTaller().equals(
                encontrada.get().getClase().getTaller().getIdTaller())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Evidencia no encontrada en el taller");
        }
        return encontrada.get();
    }

    private EvidenciasResponse.Archivo convertir(Evidencia evidencia) {
        ClaseTaller clase = evidencia.getClase();
        return new EvidenciasResponse.Archivo(evidencia.getIdEvidencia(), clase.getIdClaseTaller(),
                clase.getNumeroClase(), clase.getNombreClase(), evidencia.getNombreFisico(),
                evidencia.getMimeType(), evidencia.getTamanioBytes(),
                evidencia.isEsUtil(), evidencia.isEsDestacada());
    }

    private int identificadorNumerico(String identificador) {
        try {
            int valor = Integer.parseInt(identificador);
            if (valor > 0) {
                return valor;
            }
        } catch (NumberFormatException excepcion) {
            // La plataforma reserva identificadores numéricos.
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                "El identificador del taller debe ser numérico en la plataforma");
    }
}
