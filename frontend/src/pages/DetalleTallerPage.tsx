import { useEffect, useState } from 'react';
import type { ReactElement, SyntheticEvent } from 'react';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';
import RefreshIcon from '@mui/icons-material/Refresh';
import {
  Alert, Box, Breadcrumbs, Button, Chip, CircularProgress, Divider, Link, Paper,
  Snackbar, Tab, Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  Tabs, Tooltip, Typography
} from '@mui/material';
import type { Proyecto } from '../services/proyectosApi';
import { obtenerDetalleTaller, sincronizarClasesTaller, type ClaseDetalle, type DetalleTaller } from '../services/detalleTallerApi';
import { SesionExpiradaError } from '../services/erroresApi';
import { colores } from '../theme';
import EvidenciasTaller from '../components/EvidenciasTaller';
import EncuestasTaller from '../components/EncuestasTaller';
import ComunicacionesTaller from '../components/ComunicacionesTaller';
import ModalNuevaComunicacion from '../components/ModalNuevaComunicacion';
import { crearComunicacion, type CrearComunicacionRequest } from '../services/comunicacionesApi';

type SolapaTaller = 'resumen' | 'evidencias' | 'encuestas' | 'comunicaciones';

interface DetalleTallerPageProps {
  proyecto: Proyecto;
  idTaller: string;
  puedeActualizar: boolean;
  onVolverAProyectos: () => void;
  onVolverAProyecto: () => void;
  onVolverATalleres: () => void;
  onSesionExpirada: () => void;
}

interface CampoResumenProps {
  titulo: string;
  valor: string;
}

function CampoResumen({ titulo, valor }: CampoResumenProps) {
  return (
    <Box sx={{ minWidth: 0 }}>
      <Typography variant="body2" sx={{ color: '#667085', mb: 0.5 }}>{titulo}</Typography>
      <Typography variant="body1" sx={{ overflowWrap: 'anywhere' }}>{valor}</Typography>
    </Box>
  );
}

function formatearFecha(fecha: string | null): string {
  if (fecha === null || fecha === '') return '—';
  const partes = fecha.split('-');
  if (partes.length !== 3) return fecha;
  return `${partes[2]}-${partes[1]}-${partes[0]}`;
}

function formatearFechaHora(fecha: string | null): string {
  if (!fecha) return 'Sin sincronizar';
  const instante = new Date(fecha);
  if (Number.isNaN(instante.getTime())) return 'Sin sincronizar';
  const partes = new Intl.DateTimeFormat('es-AR', {
    timeZone: 'America/Argentina/Buenos_Aires', year: 'numeric', month: '2-digit',
    day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit', hourCycle: 'h23'
  }).formatToParts(instante);
  const valores: Record<string, string> = {};
  for (const parte of partes) valores[parte.type] = parte.value;
  return `${valores.day}-${valores.month}-${valores.year} ${valores.hour}:${valores.minute}:${valores.second}`;
}

function nombreEstado(estado: DetalleTaller['estado']): string {
  if (estado === 'NO_INICIADO') return 'No iniciado';
  if (estado === 'EN_PLAZO') return 'En plazo';
  if (estado === 'PROXIMO_ATRASO') return 'Próximo a atraso';
  return 'Atrasado';
}

function nombreEvidencias(estado: DetalleTaller['evidencias']): string {
  if (estado === 'SIN_CLASES_REALIZADAS') return 'Sin clases realizadas';
  if (estado === 'COMPLETAS') return 'Completas';
  return 'Pendientes';
}

function textoEvidenciasClase(clase: ClaseDetalle): string {
  if (clase.estado === 'PENDIENTE') return '—';
  if (clase.cantidadEvidencias === 0) return 'Pendientes';
  return `${clase.cantidadEvidencias} registrada${clase.cantidadEvidencias === 1 ? '' : 's'}`;
}

function TablaClases({ clases }: { clases: ClaseDetalle[] }) {
  const filas: ReactElement[] = [];
  for (const clase of clases) {
    let textoParticipantes = '—';
    if (clase.cantidadParticipantes !== null) {
      textoParticipantes = String(clase.cantidadParticipantes);
    }
    filas.push(
      <TableRow key={clase.numeroClase} hover>
        <TableCell>
          <Typography variant="body2" sx={{ fontWeight: 500 }}>
            {clase.nombreClase ?? `Clase ${clase.numeroClase}`}
          </Typography>
        </TableCell>
        <TableCell>
          <Typography variant="body2" sx={{ overflowWrap: 'anywhere' }}>
            {clase.nombreModulo || '—'}
          </Typography>
        </TableCell>
        <TableCell>
          <Tooltip title={`Fecha prevista: ${formatearFecha(clase.fechaPrevista)}`}>
            <Box component="span">{formatearFecha(clase.fechaRealizacion)}</Box>
          </Tooltip>
        </TableCell>
        <TableCell>{textoParticipantes}</TableCell>
        <TableCell>{clase.estado === 'REALIZADA' ? 'Realizada' : 'Pendiente'}</TableCell>
        <TableCell sx={{ color: 'text.secondary' }}>Sin datos</TableCell>
        <TableCell>{textoEvidenciasClase(clase)}</TableCell>
      </TableRow>
    );
  }
  return (
    <TableContainer component={Paper} sx={{ border: '1px solid #E5E7EB', borderRadius: 1, boxShadow: 'none' }}>
      <Table size="small" aria-label="Seguimiento de clases del taller" sx={{ minWidth: 1080 }}>
        <TableHead sx={{ backgroundColor: '#F9FAFB' }}>
          <TableRow>
            <TableCell>Clase</TableCell>
            <TableCell>Módulo</TableCell>
            <TableCell>Fecha</TableCell>
            <TableCell>Alumnos</TableCell>
            <TableCell>Estado</TableCell>
            <TableCell>Encuesta</TableCell>
            <TableCell>Evidencias</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>{filas}</TableBody>
      </Table>
    </TableContainer>
  );
}

export default function DetalleTallerPage({ proyecto, idTaller, puedeActualizar,
  onVolverAProyectos, onVolverAProyecto, onVolverATalleres,
  onSesionExpirada }: DetalleTallerPageProps) {
  const [detalle, setDetalle] = useState<DetalleTaller | null>(null);
  const [cargando, setCargando] = useState(true);
  const [actualizando, setActualizando] = useState(false);
  const [error, setError] = useState('');
  const [aviso, setAviso] = useState('');
  const [solapa, setSolapa] = useState<SolapaTaller>('resumen');
  const [nuevaComunicacionAbierta, setNuevaComunicacionAbierta] = useState(false);
  const [versionComunicaciones, setVersionComunicaciones] = useState(0);

  useEffect(function consultarDetalleAlIngresar() {
    let activo = true;
    async function cargarDetalle() {
      try {
        const datos = await obtenerDetalleTaller(proyecto.idProyecto, idTaller);
        if (activo) setDetalle(datos);
      } catch (excepcion) {
        if (!activo) return;
        if (excepcion instanceof SesionExpiradaError) onSesionExpirada();
        else setError(excepcion instanceof Error ? excepcion.message : 'No se pudo consultar el taller.');
      } finally {
        if (activo) setCargando(false);
      }
    }
    void cargarDetalle();
    return function cancelarConsulta() { activo = false; };
  }, [proyecto.idProyecto, idTaller, onSesionExpirada]);

  async function actualizarClases(): Promise<void> {
    if (actualizando) return;
    setActualizando(true);
    setError('');
    try {
      await sincronizarClasesTaller(proyecto.idProyecto);
      const datos = await obtenerDetalleTaller(proyecto.idProyecto, idTaller);
      setDetalle(datos);
      setAviso('Talleres y clases actualizados correctamente.');
    } catch (excepcion) {
      if (excepcion instanceof SesionExpiradaError) onSesionExpirada();
      else setError(excepcion instanceof Error ? excepcion.message : 'No se pudieron actualizar las clases.');
    } finally {
      setActualizando(false);
    }
  }

  function seleccionarSolapa(_evento: SyntheticEvent, siguiente: string): void {
    if (siguiente === 'resumen' || siguiente === 'evidencias'
        || siguiente === 'encuestas' || (siguiente === 'comunicaciones' && puedeActualizar)) {
      setSolapa(siguiente);
    }
  }

  function abrirNuevaComunicacion(): void {
    if (puedeActualizar) setNuevaComunicacionAbierta(true);
  }

  async function guardarNuevaComunicacion(datos: CrearComunicacionRequest): Promise<void> {
    await crearComunicacion(proyecto.idProyecto, idTaller, datos);
    setNuevaComunicacionAbierta(false);
    setVersionComunicaciones(version => version + 1);
    setSolapa('comunicaciones');
    setAviso('Comunicación registrada correctamente.');
  }

  function cerrarAviso(): void { setAviso(''); }

  return (
    <Box component="section" sx={{ width: '100%', boxSizing: 'border-box', padding: '12px 24px' }}>
      <Breadcrumbs separator={<ChevronRightIcon sx={{ fontSize: 16, color: '#667085' }} />}
        sx={{ mb: 2 }} aria-label="Navegación">
        <Link component="button" type="button" underline="hover" onClick={onVolverAProyectos} sx={{ color: '#667085', fontSize: 14 }}>Inicio</Link>
        <Link component="button" type="button" underline="hover" onClick={onVolverAProyectos} sx={{ color: '#667085', fontSize: 14 }}>Proyectos</Link>
        <Link component="button" type="button" underline="hover" onClick={onVolverAProyecto} sx={{ color: '#667085', fontSize: 14 }}>{proyecto.nombreCompleto}</Link>
        <Link component="button" type="button" underline="hover" onClick={onVolverATalleres} sx={{ color: '#667085', fontSize: 14 }}>Talleres</Link>
        <Typography sx={{ color: '#257991', fontSize: 14 }}>Detalle del taller</Typography>
      </Breadcrumbs>

      <Box sx={{ display: 'flex', alignItems: 'center', flexWrap: 'wrap', gap: 2, mb: 1 }}>
        <Typography component="h1" variant="h5" >
          Taller {proyecto.nombreCompleto}
        </Typography>
        {detalle !== null && (
          <>
            <Chip label={detalle.usuarioPlataforma} sx={{
              backgroundColor: 'rgba(127, 217, 216, 0.25)', color: colores.textoPrimario,
              borderRadius: '6px', height: 32, fontSize: 14, fontWeight: 500
            }} />
            <Chip label={detalle.pais} sx={{
              backgroundColor: 'rgba(237, 77, 110, 0.25)', color: colores.textoPrimario,
              borderRadius: '6px', height: 32, fontSize: 14, fontWeight: 500
            }} />
            <Chip label={nombreEstado(detalle.estado)} sx={{
              backgroundColor: 'rgba(255, 205, 2, 0.50)', color: colores.textoPrimario,
              borderRadius: '6px', height: 32, fontSize: 14, fontWeight: 500
            }} />
          </>
        )}
      </Box>

      <Paper sx={{ height: 'calc(100dvh - 195px)', minHeight: 320,
        display: 'flex', flexDirection: 'column', overflow: 'hidden', backgroundColor: '#FFFFFF' }}>
        <Tabs value={solapa} onChange={seleccionarSolapa} variant="scrollable" scrollButtons="auto"
          sx={{ borderBottom: 1, borderColor: 'divider', flexShrink: 0, backgroundColor: '#FFFFFF' }}>
        <Tab label="Resumen" value="resumen" />
        <Tab label="Evidencias" value="evidencias" />
        <Tab label="Encuestas" value="encuestas" />
        {puedeActualizar && <Tab label="Comunicaciones" value="comunicaciones" />}
        </Tabs>
        <Box sx={{ padding: '12px 24px', flex: 1, minHeight: 0, overflowY: 'auto' }}>

      {cargando && <Box sx={{ display: 'flex', justifyContent: 'center', p: 5 }}><CircularProgress /></Box>}
      {!cargando && error !== '' && detalle === null && <Alert severity="error">{error}</Alert>}
      {!cargando && detalle !== null && (
        <>
          {detalle.pendienteBaja && (
            <Alert severity="warning" sx={{ mb: 2 }}>
              Este taller ya no aparece en la plataforma. Su copia local está pendiente de confirmación de baja.
            </Alert>
          )}
          {solapa === 'resumen' && (
            <>
              <Paper variant="outlined" sx={{ p: '12px', mb: 3, borderRadius: 1 }}>
                <Box sx={{ display: 'grid', gridTemplateColumns: {
                  xs: 'repeat(2, minmax(0, 1fr))', md: 'repeat(3, minmax(0, 1fr))',
                  xl: 'repeat(6, minmax(0, 1fr))'
                }, columnGap: 3, rowGap: '12px' }}>
                  <CampoResumen titulo="Nombre del docente" valor={detalle.nombreDocente} />
                  <CampoResumen titulo="Usuario de plataforma" valor={detalle.usuarioPlataforma} />
                  <CampoResumen titulo="Total de clases" valor={String(detalle.totalClases)} />
                  <CampoResumen titulo="Avance esperado" valor={`${detalle.avanceEsperado}/${detalle.totalClases}`} />
                  <CampoResumen titulo="Avance real" valor={`${detalle.avanceReal}/${detalle.totalClases}`} />
                  <CampoResumen titulo="Diferencia" valor={String(detalle.diferencia)} />
                  <Divider sx={{ gridColumn: '1 / -1' }} />
                  <CampoResumen titulo="Centro educativo" valor={detalle.centroEducativo} />
                  <CampoResumen titulo="Provincia/Región/Departamento" valor={detalle.provincia} />
                  <CampoResumen titulo="País" valor={detalle.pais} />
                  <CampoResumen titulo="Cantidad de alumnos" valor={String(detalle.cantidadAlumnos)} />
                  <CampoResumen titulo="Estado" valor={nombreEstado(detalle.estado)} />
                  <CampoResumen titulo="Evidencias" valor={nombreEvidencias(detalle.evidencias)} />
                </Box>
              </Paper>

              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', mb: 1.5, gap: 2 }}>
                <Typography variant="h6">Seguimiento de clases</Typography>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                  {puedeActualizar && (
                    <Button variant="outlined" startIcon={<RefreshIcon />}
                      onClick={actualizarClases} disabled={actualizando}>
                      {actualizando ? 'Actualizando...' : 'ACTUALIZAR CLASES'}
                    </Button>
                  )}
                  {puedeActualizar && <Button variant="outlined" onClick={abrirNuevaComunicacion}>
                    REGISTRAR COMUNICACIÓN
                  </Button>}
                </Box>
              </Box>
              <TablaClases clases={detalle.clases} />
              <Typography color="text.secondary" variant="caption" sx={{ display: 'block', mt: 1 }}>
                Los estados de encuesta se incorporarán en un próximo incremento. Las clases pendientes no tienen fecha ni participantes.
              </Typography>
              <Divider sx={{ mt: 2, mb: 1 }} />
              <Typography variant="caption" color="text.secondary">
                Última sincronización: {formatearFechaHora(detalle.ultimaSincronizacion)}
              </Typography>
            </>
          )}
          {solapa === 'evidencias' && (
            <EvidenciasTaller idProyecto={proyecto.idProyecto} idTaller={idTaller}
              clases={detalle.clases} puedeEditar={puedeActualizar}
              onSesionExpirada={onSesionExpirada}
              onRegistrarComunicacion={abrirNuevaComunicacion} />
          )}
          {solapa === 'encuestas' && (
            <EncuestasTaller idProyecto={proyecto.idProyecto} idTaller={idTaller}
              puedeActualizar={puedeActualizar} onSesionExpirada={onSesionExpirada} />
          )}
          {puedeActualizar && solapa === 'comunicaciones' && (
            <ComunicacionesTaller key={versionComunicaciones}
              idProyecto={proyecto.idProyecto} idTaller={idTaller}
              onNuevaComunicacion={abrirNuevaComunicacion}
              onSesionExpirada={onSesionExpirada} onAviso={setAviso} />
          )}
        </>
      )}
        </Box>
      </Paper>

      {puedeActualizar && nuevaComunicacionAbierta && (
        <ModalNuevaComunicacion onCancelar={() => setNuevaComunicacionAbierta(false)}
          onGuardar={guardarNuevaComunicacion} onSesionExpirada={onSesionExpirada} />
      )}

      <Snackbar open={aviso !== ''} autoHideDuration={4500} onClose={cerrarAviso}
        anchorOrigin={{ vertical: 'top', horizontal: 'right' }}>
        <Alert severity="success" variant="filled" onClose={cerrarAviso} sx={{ color: '#FFFFFF' }}>{aviso}</Alert>
      </Snackbar>
      <Snackbar open={error !== '' && detalle !== null} autoHideDuration={6000}
        onClose={function cerrarError() { setError(''); }}
        anchorOrigin={{ vertical: 'top', horizontal: 'right' }}>
        <Alert severity="error" variant="filled" onClose={function cerrarErrorAlert() { setError(''); }}>
          {error}
        </Alert>
      </Snackbar>
    </Box>
  );
}
