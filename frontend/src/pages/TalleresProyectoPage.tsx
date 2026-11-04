import { useEffect, useState } from 'react';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';
import RefreshIcon from '@mui/icons-material/Refresh';
import CloseIcon from '@mui/icons-material/Close';
import {
  Alert, Box, Breadcrumbs, Button, Chip, CircularProgress, Dialog, DialogActions,
  DialogContent, DialogContentText, DialogTitle, IconButton, InputAdornment, Link, MenuItem,
  Paper, Snackbar, Table, TableBody, TableCell, TableContainer, TableHead, TableRow,
  TextField, Typography
} from '@mui/material';
import ProyectoTabs, { type SolapaProyecto } from '../components/ProyectoTabs';
import type { Proyecto } from '../services/proyectosApi';
import { SesionExpiradaError } from '../services/erroresApi';
import {
  listarTalleres, sincronizarTalleres, esperarSincronizacionEnCurso,
  eliminarCopiaLocalTaller, obtenerMetadatosTalleres,
  type TallerResumen, type TalleresMetadatos, type EstadoTaller, type EstadoEvidencias
} from '../services/talleresApi';
import { colores } from '../theme';

interface TalleresProyectoPageProps {
  proyecto: Proyecto;
  puedeActualizar: boolean;
  onVolverAlListado: () => void;
  onVolverAlDetalle: () => void;
  onSeleccionarSolapa: (solapa: SolapaProyecto) => void;
  onVerTaller: (idTaller: string) => void;
  onSesionExpirada: () => void;
}

const estados: Record<EstadoTaller, string> = {
  NO_INICIADO: 'No iniciado', EN_PLAZO: 'En plazo',
  PROXIMO_ATRASO: 'Próximo a atraso', ATRASADO: 'Atrasado'
};

const evidencias: Record<EstadoEvidencias, string> = {
  SIN_CLASES_REALIZADAS: 'Sin clases realizadas', COMPLETAS: 'Completas', PENDIENTES: 'Pendientes'
};

type FiltroIndicador = 'TODOS' | 'PLATAFORMA' | 'EN_PLAZO' | 'PROXIMO_ATRASO' |
  'ATRASADO' | 'NO_INICIADOS';

type TipoNotificacion = 'success' | 'error';

interface Notificacion {
  tipo: TipoNotificacion;
  texto: string;
}

interface IndicadorProps {
  titulo: string;
  cantidad: number;
  color: string;
  seleccionado: boolean;
  onSeleccionar: () => void;
}

/** Tarjeta del diseño Figma: 200 x 90 px, sin íconos, completamente clicable. */
function Indicador({ titulo, cantidad, color, seleccionado, onSeleccionar }: IndicadorProps) {
  return (
    <Box component="button" type="button"
      aria-label={`Filtrar por ${titulo.toLowerCase()}`}
      aria-pressed={seleccionado} onClick={onSeleccionar}
      sx={{
        width: 200, height: 90, boxSizing: 'border-box',
        display: 'flex', alignItems: 'center', gap: '19px',
        padding: '0 13px', border: `2px solid ${seleccionado ? color : 'transparent'}`,
        borderRadius: '12px', backgroundColor: '#FFFFFF',
        textAlign: 'left', font: 'inherit', cursor: 'pointer',
        boxShadow: '0 2px 6px rgba(0, 0, 0, 0.25)',
        transition: 'box-shadow 150ms ease, border-color 150ms ease',
        '&:hover': { boxShadow: '0 4px 9px rgba(0, 0, 0, 0.32)' },
        '&:focus-visible': { outline: `2px solid ${color}`, outlineOffset: 3 },
      }}>
      <Box sx={{
        width: 54, height: 54, borderRadius: '12px', backgroundColor: color,
        display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
      }}>
        <Typography component="span" sx={{
          color: '#FFFFFF', fontFamily: 'Roboto, sans-serif', fontSize: 20,
          fontWeight: 700, lineHeight: 1.4,
        }}>
          {cantidad}
        </Typography>
      </Box>
      <Typography component="span" sx={{
        minWidth: 0, flex: 1, color: '#7B809A',
        fontFamily: 'Roboto, sans-serif', fontSize: 16,
        fontWeight: 300, lineHeight: 1.4,
      }}>
        {titulo}
      </Typography>
    </Box>
  );
}

function nombreEstadoProyecto(estado: string): string {
  if (estado === 'PENDIENTE_PLANIFICACION') return 'Pendiente';
  if (estado === 'PLANIFICADO') return 'Planificado';
  if (estado === 'EN_CURSO') return 'En curso';
  if (estado === 'FINALIZADO') return 'Finalizado';
  return estado;
}

function fechaSincronizacion(fecha: string | null | undefined): string {
  if (!fecha) return 'Sin sincronizar';
  const fechaUtc = new Date(fecha);
  if (Number.isNaN(fechaUtc.getTime())) return 'Sin sincronizar';
  // Mostrar la fecha de la institución, no depender de la zona horaria del navegador.
  const partes = new Intl.DateTimeFormat('es-AR', {
    timeZone: 'America/Argentina/Buenos_Aires', year: 'numeric', month: '2-digit',
    day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit', hourCycle: 'h23'
  }).formatToParts(fechaUtc);
  const valores: Record<string, string> = {};
  for (const parte of partes) valores[parte.type] = parte.value;
  return `${valores.day}-${valores.month}-${valores.year} ${valores.hour}:${valores.minute}:${valores.second}`;
}

function textoAvance(cantidad: number): string {
  return cantidad === 0 ? 'No iniciado' : `${cantidad} clases`;
}

function buscarBajaPendiente(datos: TallerResumen[], puedeActualizar: boolean): TallerResumen | null {
  if (!puedeActualizar) return null;
  for (const taller of datos) {
    if (taller.pendienteBaja) return taller;
  }
  return null;
}

export default function TalleresProyectoPage({ proyecto, puedeActualizar,
  onVolverAlListado, onVolverAlDetalle, onSeleccionarSolapa, onVerTaller,
  onSesionExpirada }: TalleresProyectoPageProps) {
  const [talleres, setTalleres] = useState<TallerResumen[]>([]);
  const [metadatos, setMetadatos] = useState<TalleresMetadatos | null>(null);
  const [busqueda, setBusqueda] = useState('');
  const [filtro, setFiltro] = useState('TODOS');
  const [filtroIndicador, setFiltroIndicador] = useState<FiltroIndicador>('TODOS');
  const [cargando, setCargando] = useState(true);
  const [actualizando, setActualizando] = useState(false);
  const [notificacion, setNotificacion] = useState<Notificacion | null>(null);
  const [bajaSeleccionada, setBajaSeleccionada] = useState<TallerResumen | null>(null);

  // La Pantalla 7 inicia la sincronización automática. Aquí solo se consulta
  // PostgreSQL. Si el usuario navega antes de que termine la sincronización,
  // esperamos su resultado para no mostrar una copia local desactualizada.
  useEffect(function consultarTalleresAlIngresar() {
    let activo = true;
    async function cargar() {
      let advertencia = '';
      try {
        await esperarSincronizacionEnCurso(proyecto.idProyecto);
      } catch (excepcion) {
        if (excepcion instanceof SesionExpiradaError) {
          if (activo) onSesionExpirada();
          return;
        }
        advertencia = excepcion instanceof Error
          ? excepcion.message : 'Falló la sincronización automática.';
      }
      try {
        const datos = await listarTalleres(proyecto.idProyecto);
        const resumen = await obtenerMetadatosTalleres(proyecto.idProyecto);
        if (activo) {
          setTalleres(datos);
          setMetadatos(resumen);
          setBajaSeleccionada(buscarBajaPendiente(datos, puedeActualizar));
          if (advertencia !== '') setNotificacion({ tipo: 'error', texto: advertencia });
        }
      } catch (excepcion) {
        if (!activo) return;
        if (excepcion instanceof SesionExpiradaError) onSesionExpirada();
        else setNotificacion({ tipo: 'error', texto: excepcion instanceof Error
          ? excepcion.message : 'Error al consultar talleres.' });
      } finally {
        if (activo) setCargando(false);
      }
    }
    void cargar();
    return function cancelar() { activo = false; };
  }, [proyecto.idProyecto, puedeActualizar, onSesionExpirada]);

  async function actualizar() {
    if (actualizando) return;
    setActualizando(true);
    setNotificacion(null);
    try {
      const cantidad = await sincronizarTalleres(proyecto.idProyecto);
      const datos = await listarTalleres(proyecto.idProyecto);
      const resumen = await obtenerMetadatosTalleres(proyecto.idProyecto);
      setTalleres(datos);
      setMetadatos(resumen);
      setBajaSeleccionada(buscarBajaPendiente(datos, puedeActualizar));
      setNotificacion({ tipo: 'success', texto: `Sincronización completada: ${cantidad} talleres recibidos.` });
    } catch (excepcion) {
      if (excepcion instanceof SesionExpiradaError) onSesionExpirada();
      else setNotificacion({ tipo: 'error', texto: excepcion instanceof Error ? excepcion.message : 'Error al actualizar talleres.' });
    } finally {
      setActualizando(false);
    }
  }

  async function confirmarBaja() {
    if (bajaSeleccionada === null) return;
    setActualizando(true);
    setNotificacion(null);
    try {
      await eliminarCopiaLocalTaller(proyecto.idProyecto, bajaSeleccionada.idTaller);
      const datos = await listarTalleres(proyecto.idProyecto);
      const resumen = await obtenerMetadatosTalleres(proyecto.idProyecto);
      setTalleres(datos);
      setMetadatos(resumen);
      setBajaSeleccionada(buscarBajaPendiente(datos, puedeActualizar));
      setNotificacion({ tipo: 'success', texto: 'Se eliminó la copia local del taller seleccionado.' });
    } catch (excepcion) {
      if (excepcion instanceof SesionExpiradaError) onSesionExpirada();
      else setNotificacion({ tipo: 'error', texto: excepcion instanceof Error ? excepcion.message : 'No se pudo eliminar la copia local.' });
    } finally {
      setActualizando(false);
    }
  }

  function seleccionarIndicador(indicador: FiltroIndicador): void {
    // El estado de la tabla y una tarjeta representan criterios alternativos.
    // La búsqueda textual continúa vigente, para poder refinar resultados.
    setFiltro('TODOS');
    setFiltroIndicador(function alternar(actual) {
      return actual === indicador ? 'TODOS' : indicador;
    });
  }

  function limpiarFiltros(): void {
    setBusqueda('');
    setFiltro('TODOS');
    setFiltroIndicador('TODOS');
  }

  const resultados: TallerResumen[] = [];
  const termino = busqueda.trim().toLocaleLowerCase('es');
  let enPlazo = 0;
  let proximosAtraso = 0;
  let atrasados = 0;
  let noIniciados = 0;
  let hayBajasPendientes = false;
  for (const taller of talleres) {
    if (taller.pendienteBaja) hayBajasPendientes = true;
    // Los indicadores se calculan sobre los talleres vigentes de la plataforma.
    if (!taller.pendienteBaja) {
      if (taller.estado === 'EN_PLAZO') enPlazo++;
      if (taller.estado === 'PROXIMO_ATRASO') proximosAtraso++;
      if (taller.estado === 'ATRASADO') atrasados++;
      if (taller.avanceReal === 0) noIniciados++;
    }
    const coincideTexto = termino === '' || taller.docente.toLocaleLowerCase('es').includes(termino)
      || taller.centroEducativo.toLocaleLowerCase('es').includes(termino)
      || taller.pais.toLocaleLowerCase('es').includes(termino);
    let coincideIndicador = true;
    if (filtroIndicador !== 'TODOS') {
      coincideIndicador = !taller.pendienteBaja;
      if (filtroIndicador === 'NO_INICIADOS') coincideIndicador = coincideIndicador && taller.avanceReal === 0;
      else if (filtroIndicador !== 'PLATAFORMA') coincideIndicador = coincideIndicador && taller.estado === filtroIndicador;
    }
    if (coincideTexto && coincideIndicador && (filtro === 'TODOS' || filtro === taller.estado)) {
      resultados.push(taller);
    }
  }
  const estadoProyecto = metadatos?.estadoProyecto ?? proyecto.estado;

  return (
    <Box component="section" sx={{ width: '100%', boxSizing: 'border-box', padding: '12px 24px' }}>
      <Breadcrumbs separator={<ChevronRightIcon sx={{ fontSize: 16, color: '#667085' }} />} sx={{ mb: 2 }}>
        <Link component="button" type="button" underline="hover" onClick={onVolverAlListado}
          sx={{ color: '#667085', fontSize: 14 }}>Inicio</Link>
        <Link component="button" type="button" underline="hover" onClick={onVolverAlListado}
          sx={{ color: '#667085', fontSize: 14 }}>Proyectos</Link>
        <Link component="button" type="button" underline="hover" onClick={onVolverAlDetalle}
          sx={{ color: '#667085', fontSize: 14 }}>{proyecto.nombreCompleto}</Link>
        <Typography sx={{ color: '#257991', fontSize: 14 }}>Talleres</Typography>
      </Breadcrumbs>

      <Box sx={{ display: 'flex', alignItems: 'center', flexWrap: 'wrap', gap: 2, mb: 1 }}>
        <Typography component="h1" variant="h5">{proyecto.nombreCompleto}</Typography>
        <Chip label={nombreEstadoProyecto(estadoProyecto)}
          sx={{ backgroundColor: 'rgba(255, 205, 2, 0.50)',
            color: colores.textoPrimario, borderRadius: '6px', height: 32,
            fontSize: 14, fontWeight: 500 }} />
      </Box>

      <Paper sx={{ height: 'calc(100dvh - 195px)', minHeight: 320,
        display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
        <ProyectoTabs valor="talleres" onCambiar={onSeleccionarSolapa} />
        <Box sx={{ padding: '12px 24px', flex: 1, minHeight: 0, overflowY: 'auto' }}>
          {puedeActualizar && hayBajasPendientes && (
            <Alert severity="warning" sx={{ mb: 2 }}>
              Hay talleres que ya no aparecen en la plataforma. Revisá su información antes de eliminar la copia local.
              <Button type="button" onClick={function revisar() { setBajaSeleccionada(buscarBajaPendiente(talleres, puedeActualizar)); }}>
                Revisar bajas
              </Button>
            </Alert>
          )}

          {/* Indicadores con medidas, colores y tipografía de Figma, sin Paper exterior. */}
          <Box sx={{ mb: 1, px: 1, py: 1, overflowX: 'auto' }}>
            <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(5, 200px)',
              gap: '45px', minWidth: 1188, py: 1, px: 0.5 }}>
              <Indicador titulo="Talleres en la plataforma"
                cantidad={metadatos?.cantidadTalleresPlataforma ?? 0}
                color="#7FD9D8" seleccionado={filtroIndicador === 'PLATAFORMA'}
                onSeleccionar={function filtrar() { seleccionarIndicador('PLATAFORMA'); }} />
              <Indicador titulo="Talleres en plazo" cantidad={enPlazo}
                color="#ED4D6E" seleccionado={filtroIndicador === 'EN_PLAZO'}
                onSeleccionar={function filtrar() { seleccionarIndicador('EN_PLAZO'); }} />
              <Indicador titulo="Talleres próximos a atraso" cantidad={proximosAtraso}
                color="#634B66" seleccionado={filtroIndicador === 'PROXIMO_ATRASO'}
                onSeleccionar={function filtrar() { seleccionarIndicador('PROXIMO_ATRASO'); }} />
              <Indicador titulo="Talleres atrasados" cantidad={atrasados}
                color="#FFCD02" seleccionado={filtroIndicador === 'ATRASADO'}
                onSeleccionar={function filtrar() { seleccionarIndicador('ATRASADO'); }} />
              <Indicador titulo="Talleres no iniciados" cantidad={noIniciados}
                color="#257991" seleccionado={filtroIndicador === 'NO_INICIADOS'}
                onSeleccionar={function filtrar() { seleccionarIndicador('NO_INICIADOS'); }} />
            </Box>
          </Box>

          <Box sx={{ mb: 2 }}>
            {/* Padding superior: evita que la etiqueta flotante de Estado quede recortada. */}
            <Box sx={{ overflowX: 'auto', pt: 1.5, pb: 1 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: '32px',
                flexWrap: 'nowrap', minWidth: 'max-content', width: '100%' }}>
                <TextField size="small" variant="outlined"
                  label="Buscar por Docente, Centro educativo, País..."
                  value={busqueda}
                  slotProps={{
                    inputLabel: { shrink: true },
                    input: { endAdornment: busqueda !== '' ? (
                      <InputAdornment position="end">
                        <IconButton type="button" size="small" aria-label="Limpiar búsqueda"
                          onClick={function borrarBusqueda() { setBusqueda(''); }}>
                          <CloseIcon fontSize="small" />
                        </IconButton>
                      </InputAdornment>
                    ) : undefined }
                  }}
                  onChange={function cambiar(evento) { setBusqueda(evento.target.value); }}
                  sx={{ width: 500, minWidth: 500, flexShrink: 0 }} />
                <TextField select size="small" variant="outlined" label="Estado" value={filtro}
                  slotProps={{ inputLabel: { shrink: true } }}
                  sx={{ width: 250, minWidth: 250, flexShrink: 0 }}
                  onChange={function cambiar(evento) {
                    setFiltro(evento.target.value);
                    setFiltroIndicador('TODOS');
                  }}>
                  <MenuItem value="TODOS">Todos</MenuItem>
                  <MenuItem value="NO_INICIADO">No iniciado</MenuItem>
                  <MenuItem value="EN_PLAZO">En plazo</MenuItem>
                  <MenuItem value="PROXIMO_ATRASO">Próximo a atraso</MenuItem>
                  <MenuItem value="ATRASADO">Atrasado</MenuItem>
                </TextField>
                <Button variant="text" onClick={limpiarFiltros}
                  sx={{ flexShrink: 0, whiteSpace: 'nowrap', px: 0 }}>
                  Limpiar filtros
                </Button>
                {puedeActualizar && <Button variant="outlined" startIcon={<RefreshIcon />}
                  sx={{ ml: 'auto', flexShrink: 0, whiteSpace: 'nowrap' }}
                  disabled={actualizando || cargando}
                  onClick={actualizar}>{actualizando ? 'Actualizando...' : 'Actualizar talleres'}</Button>}
              </Box>
            </Box>
          </Box>

          <Paper variant="outlined" sx={{ overflow: 'hidden' }}>
            {cargando ? <Box sx={{ display: 'flex', justifyContent: 'center', p: 5 }}>
              <CircularProgress />
            </Box> : (
              <TableContainer sx={{ minHeight: 150, overflowX: 'auto' }}>
                <Table stickyHeader size="small" aria-label="Talleres del proyecto"
                  sx={{ minWidth: 1080 }}>
                  <TableHead><TableRow>
                    <TableCell>País</TableCell><TableCell>Centro educativo</TableCell>
                    <TableCell>Docente</TableCell><TableCell align="right">Alumnos</TableCell>
                    <TableCell align="right">Avance esperado</TableCell><TableCell align="right">Avance real</TableCell>
                    <TableCell align="right">Diferencia</TableCell><TableCell>Estado</TableCell>
                    <TableCell>Evidencias</TableCell><TableCell aria-label="Detalle" />
                  </TableRow></TableHead>
                  <TableBody>
                    {resultados.map(function mostrar(taller) {
                      return <TableRow key={taller.idTaller} hover tabIndex={0} role="button"
                        onClick={function abrir() { onVerTaller(taller.idTaller); }}
                        onKeyDown={function tecla(evento) {
                          if (evento.key === 'Enter' || evento.key === ' ') {
                            evento.preventDefault(); onVerTaller(taller.idTaller);
                          }
                        }} sx={{ cursor: 'pointer' }}>
                        <TableCell>{taller.pais}</TableCell>
                        <TableCell>{taller.centroEducativo}</TableCell>
                        <TableCell>{taller.docente}{taller.pendienteBaja &&
                          <Chip label="Baja pendiente" size="small" color="warning" sx={{ ml: 1 }} />}</TableCell>
                        <TableCell align="right">{taller.cantidadAlumnos}</TableCell>
                        <TableCell align="right">{taller.avanceEsperado} clases</TableCell>
                        <TableCell align="right">{textoAvance(taller.avanceReal)}</TableCell>
                        <TableCell align="right">{taller.diferencia}</TableCell>
                        <TableCell><Chip size="small" label={estados[taller.estado]}
                          sx={{ backgroundColor: taller.estado === 'ATRASADO' ? colores.chipRosa :
                            taller.estado === 'PROXIMO_ATRASO' ? colores.chipAmarillo : colores.chipVerdeClaro }} /></TableCell>
                        <TableCell>{evidencias[taller.evidencias]}</TableCell>
                        <TableCell><ChevronRightIcon fontSize="small" /></TableCell>
                      </TableRow>;
                    })}
                    {resultados.length === 0 && <TableRow><TableCell colSpan={10} align="center">
                      {talleres.length === 0 ? 'No hay talleres sincronizados.' :
                        'No hay talleres que coincidan con los filtros.'}
                    </TableCell></TableRow>}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
            <Box sx={{ px: 2, py: 1.5, borderTop: 1, borderColor: 'divider',
              display: 'flex', justifyContent: 'space-between', alignItems: 'center',
              flexWrap: 'wrap', gap: 1 }}>
              <Typography variant="body2" color="text.secondary">
                Mostrando {resultados.length} de {talleres.length} talleres
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Última sincronización: {fechaSincronizacion(metadatos?.ultimaSincronizacion)}
              </Typography>
            </Box>
          </Paper>
        </Box>
      </Paper>

      <Snackbar anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
        open={notificacion !== null} autoHideDuration={8000}
        onClose={function cerrarNotificacion() { setNotificacion(null); }}
        sx={{ zIndex: 1500, top: { xs: 24, sm: 24 } }}>
        <Alert severity={notificacion?.tipo ?? 'info'} variant="filled" elevation={6}
          onClose={function cerrarNotificacion() { setNotificacion(null); }}
          sx={{ width: '100%', maxWidth: 460, color: '#FFFFFF',
            '& .MuiAlert-message': { color: '#FFFFFF' } }}>
          {notificacion?.texto ?? ''}
        </Alert>
      </Snackbar>

      <Dialog open={bajaSeleccionada !== null}
        onClose={function cerrar() { if (!actualizando) setBajaSeleccionada(null); }}>
        <DialogTitle>Confirmar eliminación de copia local</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Este taller dejó de aparecer tras una sincronización exitosa. Revisá el detalle antes de confirmar.
            Se eliminarán de esta aplicación el taller y sus clases guardadas, no los datos de la plataforma.
          </DialogContentText>
          {bajaSeleccionada !== null && <Box sx={{ mt: 2 }}>
            <Typography><strong>Taller:</strong> {bajaSeleccionada.idTaller}</Typography>
            <Typography><strong>Docente:</strong> {bajaSeleccionada.docente}</Typography>
            <Typography><strong>Centro educativo:</strong> {bajaSeleccionada.centroEducativo}</Typography>
            <Typography><strong>Clases realizadas:</strong> {bajaSeleccionada.avanceReal}</Typography>
            <Typography><strong>Evidencias:</strong> {evidencias[bajaSeleccionada.evidencias]}</Typography>
          </Box>}
        </DialogContent>
        <DialogActions>
          <Button onClick={function cancelar() { setBajaSeleccionada(null); }}
            disabled={actualizando}>Conservar copia</Button>
          <Button color="error" variant="contained" onClick={confirmarBaja} disabled={actualizando}>
            Eliminar copia local
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}
