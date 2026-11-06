import { useEffect, useState } from 'react';
import type { ChangeEvent, ReactElement } from 'react';
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome';
import {
  Alert, Box, Button, CircularProgress, MenuItem, Snackbar, Table, TableBody,
  TableCell, TableContainer, TableHead, TableRow, TextField,
  Tooltip, Typography
} from '@mui/material';
import {
  obtenerEncuestas, sincronizarRespuestas,
  type ClaseEncuesta, type EncuestasTallerResponse
} from '../services/encuestasApi';
import { SesionExpiradaError } from '../services/erroresApi';
import { colores } from '../theme';

interface EncuestasTallerProps {
  idProyecto: number;
  idTaller: string;
  puedeActualizar: boolean;
  onSesionExpirada: () => void;
}

function mostrarFechaHora(valor: string | null): string {
  if (valor === null || valor === '') {
    return 'Sin sincronizar';
  }
  const fecha = new Date(valor);
  if (Number.isNaN(fecha.getTime())) {
    return 'Sin sincronizar';
  }
  return new Intl.DateTimeFormat('es-AR', {
    timeZone: 'America/Argentina/Buenos_Aires',
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit', second: '2-digit', hourCycle: 'h23'
  }).format(fecha);
}

function TablaPreguntas({ clases }: { clases: ClaseEncuesta[] }) {
  const filas: ReactElement[] = [];
  for (const clase of clases) {
    filas.push(
      <TableRow key={`grupo-${clase.idClaseTaller}`} sx={{ backgroundColor: '#F9FAFB' }}>
        <TableCell colSpan={2} sx={{ fontWeight: 600, py: 1 }}>
          {clase.nombreClase || `Clase ${clase.numeroClase}`}
        </TableCell>
      </TableRow>
    );
    if (clase.respuestas.length === 0) {
      filas.push(
        <TableRow key={`sin-${clase.idClaseTaller}`}>
          <TableCell colSpan={2} sx={{ color: 'text.secondary', fontStyle: 'italic' }}>
            {clase.estado === 'PENDIENTE' ? 'Clase pendiente, sin respuestas.' : 'Sin respuestas sincronizadas.'}
          </TableCell>
        </TableRow>
      );
    }
    for (const respuesta of clase.respuestas) {
      filas.push(
        <TableRow key={`${clase.idClaseTaller}-${respuesta.idPreguntaProyecto}`}>
          <TableCell sx={{ width: '50%', color: '#667085', verticalAlign: 'top', py: 1.5,
            overflowWrap: 'anywhere' }}>{respuesta.pregunta}</TableCell>
          <TableCell sx={{ width: '50%', verticalAlign: 'top', py: 1.5,
            overflowWrap: 'anywhere' }}>{respuesta.respuesta}</TableCell>
        </TableRow>
      );
    }
  }
  return (
    <TableContainer sx={{ maxWidth: '100%', overflowX: 'auto' }}>
      <Table size="small" aria-label="Preguntas y respuestas del taller" sx={{ tableLayout: 'fixed', minWidth: 640 }}>
        <TableHead>
          <TableRow>
            <TableCell sx={{ width: '50%', fontWeight: 500 }}>Pregunta</TableCell>
            <TableCell sx={{ width: '50%', fontWeight: 500 }}>Respuesta</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>{filas}</TableBody>
      </Table>
    </TableContainer>
  );
}

export default function EncuestasTaller({ idProyecto, idTaller, puedeActualizar,
  onSesionExpirada }: EncuestasTallerProps) {
  const [datos, setDatos] = useState<EncuestasTallerResponse | null>(null);
  const [cargando, setCargando] = useState(true);
  const [actualizando, setActualizando] = useState(false);
  const [filtro, setFiltro] = useState('todas');
  const [error, setError] = useState('');
  const [mensaje, setMensaje] = useState('');

  useEffect(function consultarAlAbrirSolapa() {
    let activo = true;
    async function cargar() {
      try {
        const respuesta = await obtenerEncuestas(idProyecto, idTaller);
        if (activo) {
          setDatos(respuesta);
        }
      } catch (excepcion) {
        if (activo) {
          if (excepcion instanceof SesionExpiradaError) {
            onSesionExpirada();
          } else {
            setError(excepcion instanceof Error ? excepcion.message : 'No se pudo consultar.');
          }
        }
      } finally {
        if (activo) {
          setCargando(false);
        }
      }
    }
    void cargar();
    return function cancelarConsulta() {
      activo = false;
    };
  }, [idProyecto, idTaller, onSesionExpirada]);

  async function actualizarRespuestas(): Promise<void> {
    if (actualizando) {
      return;
    }
    setActualizando(true);
    setError('');
    setMensaje('');
    try {
      const cantidad = await sincronizarRespuestas(idProyecto, idTaller);
      const respuesta = await obtenerEncuestas(idProyecto, idTaller);
      setDatos(respuesta);
      setMensaje(`${cantidad} respuestas procesadas correctamente.`);
    } catch (excepcion) {
      if (excepcion instanceof SesionExpiradaError) {
        onSesionExpirada();
      } else {
        setError(excepcion instanceof Error ? excepcion.message : 'No se pudieron actualizar las respuestas.');
      }
    } finally {
      setActualizando(false);
    }
  }

  function cerrarMensaje(): void {
    setMensaje('');
  }

  function cambiarFiltro(evento: ChangeEvent<HTMLInputElement>): void {
    setFiltro(evento.target.value);
  }

  function limpiarFiltro(): void {
    setFiltro('todas');
  }

  const opciones: ReactElement[] = [<MenuItem key="todas" value="todas">Todas las clases</MenuItem>];
  const clasesFiltradas: ClaseEncuesta[] = [];
  if (datos !== null) {
    for (const clase of datos.clases) {
      opciones.push(<MenuItem key={clase.numeroClase} value={String(clase.numeroClase)}>
        {clase.nombreClase || `Clase ${clase.numeroClase}`}</MenuItem>);
      if (filtro === 'todas' || filtro === String(clase.numeroClase)) {
        clasesFiltradas.push(clase);
      }
    }
  }

  return (
    <Box sx={{ width: '100%', minWidth: 0 }}>
      <Box sx={{ borderBottom: '1px solid #DCE3E9', px: 2, py: 1.5 }}>
        <Typography variant="h6" sx={{ fontWeight: 400 }}>Preguntas y respuestas</Typography>
      </Box>
      <Box sx={{ display: 'flex', alignItems: 'center', flexWrap: 'wrap', gap: 2,
        p: 2, borderBottom: '1px solid #DCE3E9' }}>
        <TextField select size="small" label="Clase" value={filtro} onChange={cambiarFiltro}
          sx={{ width: '500px', maxWidth: '100%' }}>{opciones}</TextField>
        <Button variant="text" color="secondary" onClick={limpiarFiltro}
          disabled={filtro === 'todas'}
          sx={{ flexShrink: 0, '&:not(.Mui-disabled)': { color: '#257991' } }}>
          LIMPIAR FILTRO
        </Button>
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end',
          gap: 2, flexWrap: 'wrap', ml: 'auto' }}>
          <Box sx={{ display: 'flex', flexDirection: 'column' }}>
            <Typography variant="caption" color="text.secondary" sx={{ whiteSpace: 'nowrap' }}>
              Última sincronización:
            </Typography>
            <Typography variant="body2" sx={{ whiteSpace: 'nowrap' }}>
              {mostrarFechaHora(datos?.ultimaSincronizacion ?? null)}
            </Typography>
          </Box>
          {puedeActualizar && (
            <Button variant="outlined" disabled={actualizando} onClick={actualizarRespuestas}>
              {actualizando ? 'Actualizando...' : 'ACTUALIZAR RESPUESTAS'}
            </Button>
          )}
          <Tooltip title="El análisis con IA se implementará en un incremento posterior">
            <span>
              <Button variant="contained" disabled startIcon={<AutoAwesomeIcon />}>
                ANALIZAR CON IA
              </Button>
            </span>
          </Tooltip>
        </Box>
      </Box>
      {error !== '' && <Alert severity="error" sx={{ m: 2 }}>{error}</Alert>}
      {cargando && <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
        <CircularProgress size={28} />
      </Box>}
      {!cargando && datos !== null && clasesFiltradas.length === 0 && (
        <Alert severity="info" sx={{ m: 2 }}>No hay clases disponibles para consultar.</Alert>
      )}
      {!cargando && datos !== null && clasesFiltradas.length > 0 && (
        <TablaPreguntas clases={clasesFiltradas} />
      )}
      {!cargando && datos !== null && datos.ultimaSincronizacion === null && (
        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', p: 2 }}>
          Para incorporar respuestas nuevas, utilizá ACTUALIZAR RESPUESTAS.
        </Typography>
        )}
      <Snackbar
        open={mensaje !== ''}
        autoHideDuration={5000}
        onClose={cerrarMensaje}
        anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
        sx={{ top: { xs: 64, sm: 64 }, right: { xs: 16, sm: 24 } }}
      >
        <Alert severity="success" variant="filled" role="status" onClose={cerrarMensaje}
          sx={{
            backgroundColor: colores.verdeToastExito,
            color: '#FFFFFF',
            minWidth: { xs: 0, sm: 320 },
            maxWidth: 420,
            boxShadow: 3,
            '& .MuiAlert-icon': { color: '#FFFFFF' }
          }}>
          {mensaje}
        </Alert>
      </Snackbar>
    </Box>
  );
}
