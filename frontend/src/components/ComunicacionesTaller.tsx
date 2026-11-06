import { useEffect, useState } from 'react';
import type { ReactElement } from 'react';
import ChatBubbleOutlinedIcon from '@mui/icons-material/ChatBubbleOutlined';
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown';
import KeyboardArrowUpIcon from '@mui/icons-material/KeyboardArrowUp';
import {
  Alert, Box, Button, CircularProgress, Collapse, IconButton, Paper,
  Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Tooltip, Typography
} from '@mui/material';
import {
  acciones, actualizarSeguimiento, canales, etiquetaDe, listarComunicaciones,
  motivos, mostrarFecha, mostrarFechaHora, resultados,
  type ActualizarSeguimientoRequest, type ComunicacionTaller
} from '../services/comunicacionesApi';
import { SesionExpiradaError } from '../services/erroresApi';
import ModalModificarComunicacion from './ModalModificarComunicacion';

interface Props {
  idProyecto: number;
  idTaller: string;
  onNuevaComunicacion: () => void;
  onSesionExpirada: () => void;
  onAviso: (mensaje: string) => void;
}

function DatoSeguimiento({ titulo, fecha, texto }: { titulo: string; fecha?: string | null; texto?: string | null }) {
  return (
    <Box sx={{ mb: 1.75 }}>
      <Typography variant="subtitle2">{titulo}</Typography>
      {fecha && <Typography variant="caption" color="text.secondary">{mostrarFecha(fecha)}</Typography>}
      {texto && <Box sx={{ display: 'flex', gap: 1, alignItems: 'flex-start', mt: 0.5 }}>
        <ChatBubbleOutlinedIcon sx={{ fontSize: 18, color: 'text.secondary' }} />
        <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}>{texto}</Typography>
      </Box>}
      {!texto && <Typography variant="body2" color="text.secondary">Sin información registrada.</Typography>}
    </Box>
  );
}

export default function ComunicacionesTaller({ idProyecto, idTaller, onNuevaComunicacion,
  onSesionExpirada, onAviso }: Props) {
  const [lista, setLista] = useState<ComunicacionTaller[]>([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');
  const [expandida, setExpandida] = useState<number | null>(null);
  const [modificando, setModificando] = useState<ComunicacionTaller | null>(null);

  useEffect(function cargarHistorial() {
    let activo = true;
    async function consultar() {
      try {
        const datos = await listarComunicaciones(idProyecto, idTaller);
        if (activo) setLista(datos);
      } catch (excepcion) {
        if (!activo) return;
        if (excepcion instanceof SesionExpiradaError) onSesionExpirada();
        else setError(excepcion instanceof Error ? excepcion.message : 'No se pudo consultar el historial.');
      } finally {
        if (activo) setCargando(false);
      }
    }
    void consultar();
    return function cancelar() { activo = false; };
  }, [idProyecto, idTaller, onSesionExpirada]);

  async function guardarSeguimiento(datos: ActualizarSeguimientoRequest): Promise<void> {
    if (modificando === null) return;
    const guardada = await actualizarSeguimiento(idProyecto, idTaller, modificando.idComunicacion, datos);
    const actualizadas: ComunicacionTaller[] = [];
    for (const existente of lista) {
      if (existente.idComunicacion === guardada.idComunicacion) actualizadas.push(guardada);
      else actualizadas.push(existente);
    }
    setLista(actualizadas);
    setExpandida(modificando.idComunicacion);
    setModificando(null);
    onAviso('Seguimiento actualizado correctamente.');
  }

  const filas: ReactElement[] = [];
  for (const comunicacion of lista) {
    const abierta = expandida === comunicacion.idComunicacion;
    filas.push(
      <TableRow hover key={`fila-${comunicacion.idComunicacion}`}
        sx={{ backgroundColor: abierta ? 'rgba(37,121,145,0.08)' : undefined }}>
        <TableCell>{mostrarFecha(comunicacion.fechaContacto)}</TableCell>
        <TableCell>{comunicacion.canal === 'OTRO' ? comunicacion.canalOtro : etiquetaDe(canales, comunicacion.canal)}</TableCell>
        <TableCell>{comunicacion.motivo === 'OTRO' ? comunicacion.motivoOtro : etiquetaDe(motivos, comunicacion.motivo)}</TableCell>
        <TableCell>{etiquetaDe(acciones, comunicacion.accionRealizada)}</TableCell>
        <TableCell>{etiquetaDe(resultados, comunicacion.resultado)}</TableCell>
        <TableCell>{comunicacion.registradoPor}</TableCell>
        <TableCell padding="checkbox">
          <Tooltip title={abierta ? 'Contraer comunicación' : 'Expandir comunicación'}>
            <IconButton size="small" aria-label={abierta ? 'Contraer comunicación' : 'Expandir comunicación'}
              aria-expanded={abierta} onClick={() => setExpandida(abierta ? null : comunicacion.idComunicacion)}>
              {abierta ? <KeyboardArrowUpIcon /> : <KeyboardArrowDownIcon />}
            </IconButton>
          </Tooltip>
        </TableCell>
      </TableRow>
    );
    filas.push(
      <TableRow key={`detalle-${comunicacion.idComunicacion}`}>
        <TableCell colSpan={7} sx={{ padding: 0, borderBottom: abierta ? undefined : 'none' }}>
          <Collapse in={abierta} timeout="auto" unmountOnExit>
            <Box sx={{ px: 2, py: 2, backgroundColor: '#FFFFFF' }}>
              <Typography variant="subtitle2" sx={{ mb: 2, fontWeight: 600 }}>
                Comunicación por {comunicacion.motivo === 'OTRO'
                  ? comunicacion.motivoOtro : etiquetaDe(motivos, comunicacion.motivo).toLowerCase()}
              </Typography>
              <DatoSeguimiento titulo="Contacto inicial" fecha={comunicacion.fechaContacto}
                texto={comunicacion.observaciones} />
              <DatoSeguimiento titulo="Respuesta del docente" fecha={comunicacion.fechaRespuestaDocente}
                texto={comunicacion.respuestaDocente} />
              {comunicacion.fechaCompromiso && <Box sx={{ mb: 2 }}>
                <Typography variant="subtitle2">Fecha de compromiso</Typography>
                <Typography variant="body2">{mostrarFecha(comunicacion.fechaCompromiso)}</Typography>
              </Box>}
              <Box sx={{ display: 'flex', alignItems: 'end', justifyContent: 'space-between', gap: 2, flexWrap: 'wrap' }}>
                <Typography variant="caption" color="text.secondary">
                  Registrado: {mostrarFechaHora(comunicacion.fechaCreacion)}
                  {' · '} Última modificación: {mostrarFechaHora(comunicacion.fechaUltimaModificacion)}
                </Typography>
                <Button variant="outlined" onClick={() => setModificando(comunicacion)}
                  sx={{ color: '#257991', borderColor: '#257991',
                    '&:hover': { borderColor: '#257991', backgroundColor: 'rgba(37, 121, 145, 0.06)' } }}>
                  ACTUALIZAR SEGUIMIENTO
                </Button>
              </Box>
            </Box>
          </Collapse>
        </TableCell>
      </TableRow>
    );
  }

  return (
    <Box sx={{ minWidth: 0 }}>
      <Paper variant="outlined" sx={{ borderRadius: 1, overflow: 'hidden' }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center',
          flexWrap: 'wrap', gap: 2, p: 2 }}>
          <Box>
            <Typography variant="h6">Historial de comunicaciones</Typography>
            <Typography variant="body2" color="text.secondary">Contactos registrados con el docente</Typography>
          </Box>
          <Button variant="contained" onClick={onNuevaComunicacion}>NUEVA COMUNICACIÓN</Button>
        </Box>
        {error !== '' && <Alert severity="error" sx={{ m: 2 }}>{error}</Alert>}
        {cargando && <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}><CircularProgress /></Box>}
        {!cargando && lista.length === 0 && error === '' &&
          <Typography color="text.secondary" sx={{ p: 3 }}>Todavía no hay comunicaciones registradas.</Typography>}
        {!cargando && lista.length > 0 && <TableContainer>
          <Table size="small" sx={{ minWidth: 990 }} aria-label="Historial de comunicaciones con el docente">
            <TableHead sx={{ backgroundColor: '#FAFAFA' }}><TableRow>
              <TableCell>Fecha</TableCell><TableCell>Canal</TableCell><TableCell>Motivo</TableCell>
              <TableCell>Acción realizada</TableCell><TableCell>Resultado</TableCell>
              <TableCell>Registrado por</TableCell><TableCell />
            </TableRow></TableHead>
            <TableBody>{filas}</TableBody>
          </Table>
        </TableContainer>}
      </Paper>
      {modificando && <ModalModificarComunicacion comunicacion={modificando}
        onCancelar={() => setModificando(null)} onGuardar={guardarSeguimiento}
        onSesionExpirada={onSesionExpirada} />}
    </Box>
  );
}
