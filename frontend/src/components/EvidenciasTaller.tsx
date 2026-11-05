import { useEffect, useState } from 'react';
import type { ReactElement } from 'react';
import { Alert, Box, CircularProgress, Snackbar, Typography } from '@mui/material';
import type { ClaseDetalle } from '../services/detalleTallerApi';
import {
  consultarEvidencias, sincronizarEvidencias, actualizarMarcadoresEvidencia,
  type EvidenciaTaller, type EvidenciasTallerResponse
} from '../services/evidenciasApi';
import { SesionExpiradaError } from '../services/erroresApi';
import BarraEvidencias from './BarraEvidencias';
import GrupoEvidenciasClase from './GrupoEvidenciasClase';
import VisorEvidencia from './VisorEvidencia';

interface EvidenciasTallerProps {
  idProyecto: number;
  idTaller: string;
  clases: ClaseDetalle[];
  puedeEditar: boolean;
  onSesionExpirada: () => void;
}

/** Consulta solo PostgreSQL al abrir; sincroniza FastAPI únicamente por acción del usuario. */
export default function EvidenciasTaller({ idProyecto, idTaller, clases, puedeEditar,
  onSesionExpirada }: EvidenciasTallerProps) {
  const [datos, setDatos] = useState<EvidenciasTallerResponse | null>(null);
  const [filtro, setFiltro] = useState('todas');
  const [cargando, setCargando] = useState(true);
  const [actualizando, setActualizando] = useState(false);
  const [marcandoId, setMarcandoId] = useState<number | null>(null);
  const [abierta, setAbierta] = useState<EvidenciaTaller | null>(null);
  const [error, setError] = useState('');
  const [aviso, setAviso] = useState('');

  useEffect(function cargarInicial() {
    let activo = true;

    async function consultar() {
      try {
        const respuesta = await consultarEvidencias(idProyecto, idTaller);
        if (activo) {
          setDatos(respuesta);
        }
      } catch (excepcion) {
        if (!activo) {
          return;
        }
        if (excepcion instanceof SesionExpiradaError) {
          onSesionExpirada();
        } else {
          setError(excepcion instanceof Error ? excepcion.message : 'No se pudieron consultar evidencias.');
        }
      } finally {
        if (activo) {
          setCargando(false);
        }
      }
    }

    void consultar();
    return function cancelarConsulta() {
      activo = false;
    };
  }, [idProyecto, idTaller, onSesionExpirada]);

  function informarError(excepcion: unknown, mensaje: string): void {
    if (excepcion instanceof SesionExpiradaError) {
      onSesionExpirada();
    } else {
      setError(excepcion instanceof Error ? excepcion.message : mensaje);
    }
  }

  async function actualizar(): Promise<void> {
    if (actualizando) {
      return;
    }
    setActualizando(true);
    setError('');
    try {
      await sincronizarEvidencias(idProyecto, idTaller);
      const respuesta = await consultarEvidencias(idProyecto, idTaller);
      setDatos(respuesta);
      setAviso('Evidencias actualizadas correctamente.');
    } catch (excepcion) {
      informarError(excepcion, 'No se pudieron actualizar evidencias.');
    } finally {
      setActualizando(false);
    }
  }

  async function marcar(id: number, util: boolean, destacada: boolean): Promise<void> {
    if (marcandoId !== null) {
      return;
    }
    setMarcandoId(id);
    setError('');
    try {
      const nueva = await actualizarMarcadoresEvidencia(idProyecto, idTaller, id, util, destacada);
      if (datos !== null) {
        const actualizadas: EvidenciaTaller[] = [];
        for (const evidencia of datos.evidencias) {
          if (evidencia.idEvidencia === id) {
            actualizadas.push(nueva);
          } else {
            actualizadas.push(evidencia);
          }
        }
        setDatos({ ...datos, evidencias: actualizadas });
      }
    } catch (excepcion) {
      informarError(excepcion, 'No se pudo guardar la valoración.');
    } finally {
      setMarcandoId(null);
    }
  }

  function abrir(archivo: EvidenciaTaller): void {
    setAbierta(archivo);
  }

  function cerrarVisor(): void {
    setAbierta(null);
  }

  function cerrarAviso(): void {
    setAviso('');
  }

  const grupos: ReactElement[] = [];
  if (datos !== null) {
    for (const clase of clases) {
      if (filtro !== 'todas' && filtro !== String(clase.numeroClase)) {
        continue;
      }
      const archivos: EvidenciaTaller[] = [];
      for (const archivo of datos.evidencias) {
        if (archivo.numeroClase === clase.numeroClase) {
          archivos.push(archivo);
        }
      }
      grupos.push(
        <GrupoEvidenciasClase key={clase.numeroClase} clase={clase} archivos={archivos}
          idProyecto={idProyecto} idTaller={idTaller} puedeEditar={puedeEditar}
          bloqueado={marcandoId !== null} onAbrir={abrir} onMarcar={marcar} />
      );
    }
  }

  return (
    <Box sx={{ minWidth: 0 }}>
      <Typography variant="h6" sx={{ mb: 1.5 }}>Fotos y videos</Typography>
      <BarraEvidencias clases={clases} filtro={filtro}
        ultimaSincronizacion={datos?.ultimaSincronizacion || null}
        puedeEditar={puedeEditar} actualizando={actualizando}
        onCambiarFiltro={setFiltro} onActualizar={actualizar} />
      {error !== '' && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
      {cargando && (
        <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
          <CircularProgress />
        </Box>
      )}
      {!cargando && datos !== null && grupos}
      <VisorEvidencia archivo={abierta} idProyecto={idProyecto} idTaller={idTaller}
        onCerrar={cerrarVisor} />
      <Snackbar open={aviso !== ''} autoHideDuration={4500} onClose={cerrarAviso}
        message={aviso} anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }} />
    </Box>
  );
}
