import { useEffect, useState } from 'react';

import {
  Alert, Box, Breadcrumbs, Button, Chip, CircularProgress,
  Divider, Link, Paper, Typography
} from '@mui/material';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';

import { obtenerDetalleProyecto, type DetalleProyecto } from '../services/detalleProyectoApi';
import { SesionExpiradaError } from '../services/erroresApi';
import {
  listarTalleres, sincronizarTalleres, obtenerMetadatosTalleres,
  type TallerResumen, type TalleresMetadatos
} from '../services/talleresApi';
import type { Proyecto } from '../services/proyectosApi';
import { colores } from '../theme';
import ProyectoTabs, { type SolapaProyecto } from '../components/ProyectoTabs';
import ProgramaProyecto from '../components/ProgramaProyecto';


interface DetalleProyectoPageProps {
  proyecto: Proyecto;
  rol: 'ADMINISTRADOR' | 'OPERATIVO' | 'EXTERNO';
  onVolverAlListado: () => void;
  onIrADocentes: () => void;
  onIrAPlanificacion: () => void;
  onIrASeguimiento: () => void;
  onIrATalleres: () => void;
  onSesionExpirada: () => void;
  solapaInicial?: SolapaProyecto;
}

function obtenerNombreEstado(estado: string): string {
  if (estado === 'PENDIENTE_PLANIFICACION') return 'Pendiente';
  if (estado === 'PLANIFICADO') return 'Planificado';
  if (estado === 'EN_CURSO') return 'En curso';
  if (estado === 'FINALIZADO') return 'Finalizado';
  return estado;
}

function mostrarFecha(fecha: string | null | undefined): string {
  if (!fecha) return 'Sin definir';
  const fechaSimple = fecha.substring(0, 10);
  const partes = fechaSimple.split('-');
  if (partes.length !== 3) return fechaSimple;
  return `${partes[2]}/${partes[1]}/${partes[0]}`;
}

/** La fecha de sincronización se muestra con el mismo criterio que la Pantalla 8. */
function mostrarFechaSincronizacion(fecha: string | null | undefined): string {
  if (!fecha) return 'Sin sincronizar';
  const instante = new Date(fecha);
  if (Number.isNaN(instante.getTime())) return 'Sin sincronizar';

  const partes = new Intl.DateTimeFormat('es-AR', {
    timeZone: 'America/Argentina/Buenos_Aires',
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit', hourCycle: 'h23'
  }).formatToParts(instante);
  const valores: Record<string, string> = {};
  for (const parte of partes) valores[parte.type] = parte.value;
  return `${valores.day}-${valores.month}-${valores.year} ${valores.hour}:${valores.minute}:${valores.second}`;
}

function mostrarFrecuencia(valor: string): string {
  if (valor === 'MENSUAL') return 'Mensual';
  if (valor === 'SEMANAL') return 'Semanal';
  if (valor === 'QUINCENAL') return 'Quincenal';
  return valor;
}

function mostrarDia(valor: string): string {
  if (valor === 'MIERCOLES') return 'Miércoles';
  if (valor === 'SABADO') return 'Sábado';
  const texto = valor.toLocaleLowerCase('es');
  return texto.charAt(0).toLocaleUpperCase('es') + texto.substring(1);
}

function mostrarDiaMensual(tipo: string, dia: number | null): string {
  if (tipo === 'ULTIMO_DIA_HABIL') return 'Último día hábil';
  if (dia === null) return 'Sin definir';
  return `Día ${dia}`;
}

interface DatoProps {
  titulo: string;
  valor: string | number;
}

function Dato({ titulo, valor }: DatoProps) {
  return (
    <Box sx={{ minWidth: 0 }}>
      <Typography variant="body2" sx={{ color: colores.textoSecundario, mb: 0.5 }}>
        {titulo}
      </Typography>
      <Typography variant="body1" sx={{ overflowWrap: 'anywhere' }}>
        {valor}
      </Typography>
    </Box>
  );
}

interface PanelProps {
  titulo: string;
  children: React.ReactNode;
  accion?: React.ReactNode;
}

function Panel({ titulo, children, accion }: PanelProps) {
  return (
    <Paper variant="outlined" sx={{ p: 3, borderRadius: 1, minWidth: 0 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 2, mb: '12px' }}>
        <Typography component="h2" variant="h6" sx={{ fontWeight: 500 }}>{titulo}</Typography>
        {accion}
      </Box>
      {children}
    </Paper>
  );
}

interface FilaTallerProps {
  etiqueta: string;
  valor: number | string;
}

/** Una métrica por fila y un divisor, conforme a la tarjeta Talleres de Figma. */
function FilaTaller({ etiqueta, valor }: FilaTallerProps) {
  return (
    <Box sx={{
      minHeight: 55, px: 1.5, py: 1,
      display: 'flex', justifyContent: 'space-between', alignItems: 'center',
      gap: 2, borderBottom: '1px solid', borderColor: 'divider'
    }}>
      <Typography variant="body2" sx={{ color: colores.textoPrimario, fontSize: 16 }}>
        {etiqueta}
      </Typography>
      <Typography variant="body2" sx={{ color: colores.textoPrimario, fontSize: 16, flexShrink: 0 }}>
        {valor}
      </Typography>
    </Box>
  );
}

function DetalleProyectoPage({
  proyecto, rol, onVolverAlListado, onIrADocentes,
  onIrAPlanificacion, onIrASeguimiento, onIrATalleres, onSesionExpirada, solapaInicial = 'resumen'
}: DetalleProyectoPageProps) {
  const [detalle, setDetalle] = useState<DetalleProyecto | null>(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');
  const [advertenciaTalleres, setAdvertenciaTalleres] = useState('');
  const [talleresResumen, setTalleresResumen] = useState<TallerResumen[] | null>(null);
  const [metadatosTalleres, setMetadatosTalleres] = useState<TalleresMetadatos | null>(null);
  const [versionConsulta, setVersionConsulta] = useState(0);
  const [solapa, setSolapa] = useState<SolapaProyecto>(solapaInicial);

  useEffect(function consultarDetalleAlAbrir() {
    let activo = true;

    async function cargar() {
      try {
        const datos = await obtenerDetalleProyecto(proyecto.idProyecto);
        if (activo) setDetalle(datos);
      } catch (excepcion) {
        if (!activo) return;
        if (excepcion instanceof SesionExpiradaError) {
          onSesionExpirada();
          return;
        }
        if (excepcion instanceof Error) {
          setError(excepcion.message);
        } else {
          setError('No se pudo consultar el detalle del proyecto.');
        }
      } finally {
        if (activo) setCargando(false);
      }
    }

    void cargar();
    return function cancelarConsulta() { activo = false; };
  }, [proyecto.idProyecto, versionConsulta, onSesionExpirada]);

  // Al consultar un proyecto, los perfiles internos sincronizan antes de cargar el resumen.
  // Si falla el origen remoto, se intenta igualmente mostrar la copia local.
  useEffect(function consultarTalleresDelProyecto() {
    if (proyecto.estado === 'PENDIENTE_PLANIFICACION') return;
    let activo = true;
    async function ejecutar() {
      let advertencia = '';
      if (rol !== 'EXTERNO') {
        try {
          await sincronizarTalleres(proyecto.idProyecto);
          const actualizado = await obtenerDetalleProyecto(proyecto.idProyecto);
          if (activo) setDetalle(actualizado);
        } catch (excepcion) {
          if (excepcion instanceof SesionExpiradaError) {
            onSesionExpirada();
            return;
          }
          advertencia = 'No se pudieron actualizar los talleres desde la plataforma educativa.';
        }
      }
      try {
        const lista = await listarTalleres(proyecto.idProyecto);
        if (activo) setTalleresResumen(lista);
        const metadatos = await obtenerMetadatosTalleres(proyecto.idProyecto);
        if (activo) setMetadatosTalleres(metadatos);
      } catch (excepcion) {
        if (excepcion instanceof SesionExpiradaError) {
          onSesionExpirada();
          return;
        }
        advertencia = 'No se pudo consultar el resumen de talleres del proyecto.';
      }
      if (activo) setAdvertenciaTalleres(advertencia);
    }
    void ejecutar();
    return function cancelar() { activo = false; };
  }, [proyecto.idProyecto, proyecto.estado, rol, onSesionExpirada]);

  function reintentar() {
    setError('');
    setCargando(true);
    setVersionConsulta(versionConsulta + 1);
  }

  function abrirConfiguracionEsquema() {
    setSolapa('programa');
  }

  function abrirTalleres() {
    onIrATalleres();
  }

  function cambiarSolapa(valor: SolapaProyecto) {
    if (valor === 'talleres') {
      onIrATalleres();
      return;
    }
    setSolapa(valor);
  }

  const datosProyecto = detalle?.proyecto ?? proyecto;
  const puedeEditar = rol === 'ADMINISTRADOR';
  let talleresEnPlazo = 0;
  let talleresAtrasados = 0;
  let proximosAtraso = 0;
  let talleresNoIniciados = 0;
  if (talleresResumen !== null) {
    for (const taller of talleresResumen) {
      // Al igual que en la Pantalla 8, las bajas pendientes no cuentan como vigentes.
      if (taller.pendienteBaja) continue;
      if (taller.estado === 'EN_PLAZO') talleresEnPlazo++;
      if (taller.estado === 'ATRASADO') talleresAtrasados++;
      if (taller.estado === 'PROXIMO_ATRASO') proximosAtraso++;
      if (taller.avanceReal === 0) talleresNoIniciados++;
    }
  }

  return (
    <Box component="section" sx={{ width: '100%', boxSizing: 'border-box', padding: '12px 24px' }}>
      <Breadcrumbs separator={<ChevronRightIcon sx={{ fontSize: 16 }} />} sx={{ mb: 2 }}>
        <Link component="button" type="button" underline="hover" onClick={onVolverAlListado}
          sx={{ color: '#667085', fontSize: 14 }}>Inicio</Link>
        <Link component="button" type="button" underline="hover" onClick={onVolverAlListado}
          sx={{ color: '#667085', fontSize: 14 }}>Proyectos</Link>
        <Typography title={datosProyecto.nombreCompleto}
          sx={{ fontSize: 14, color: colores.verdeOscuro, maxWidth: 450,
            whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
          {datosProyecto.nombreCompleto}
        </Typography>
      </Breadcrumbs>

      <Box sx={{ display: 'flex', alignItems: 'center', flexWrap: 'wrap', gap: 2, mb: 2 }}>
        <Typography component="h1" variant="h5">{datosProyecto.nombreCompleto}</Typography>
        <Chip label={obtenerNombreEstado(datosProyecto.estado)}
          sx={{ backgroundColor: datosProyecto.estado === 'PLANIFICADO' ? colores.chipVerdeClaro
            : datosProyecto.estado === 'PENDIENTE_PLANIFICACION' ? colores.chipAmarillo : colores.chipRosa,
            color: colores.textoPrimario, borderRadius: '6px', height: 32, fontSize: 14, fontWeight: 500 }} />
      </Box>

      <Paper sx={{
        height: 'calc(100dvh - 195px)',
        minHeight: 320,
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden'
      }}>
        <ProyectoTabs valor={solapa} onCambiar={cambiarSolapa} />

        <Box sx={{ p: { xs: 2, md: 3 }, flex: 1, minHeight: 0, overflowY: 'auto' }}>
          {cargando && <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}><CircularProgress /></Box>}
          {error !== '' && <Alert severity="error" action={<Button onClick={reintentar}>Reintentar</Button>}>{error}</Alert>}
          {!cargando && error === '' && detalle !== null && solapa === 'resumen' && (
            <Box sx={{
              display: 'grid',
              gridTemplateColumns: { xs: 'minmax(0, 1fr)', lg: 'minmax(0, 1.4fr) minmax(0, 1fr)' },
              gap: 3,
              alignItems: 'stretch'
            }}>
              {/* Las dos tarjetas de la primera fila comparten altura sin valores fijos. */}
              <Box sx={{ minWidth: 0, order: 1,
                '& > .MuiPaper-root': { height: '100%', boxSizing: 'border-box' } }}>
                <Paper variant="outlined" sx={{ borderRadius: 1, minWidth: 0, overflow: 'hidden' }}>
                  <Box sx={{ p: 3 }}>
                    <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                      gap: 2, flexWrap: 'wrap', mb: '12px' }}>
                      <Typography component="h2" variant="h6" sx={{ fontWeight: 500 }}>
                        Datos generales y docentes
                      </Typography>
                      {rol !== 'EXTERNO' && (
                        <Button variant="text" color="primary" onClick={onIrADocentes}
                          sx={{ whiteSpace: 'nowrap' }}>
                          VER DOCENTES
                        </Button>
                      )}
                    </Box>
                    <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' },
                      columnGap: 3, rowGap: 3 }}>
                      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, minmax(0, 1fr))' },
                        gap: 3, minWidth: 0 }}>
                        <Dato titulo="Nombre del proyecto" valor={datosProyecto.nombre} />
                        <Dato titulo="Año" valor={datosProyecto.anio} />
                        <Dato titulo="Empresa financiadora" valor={datosProyecto.nombreEmpresa} />
                        <Dato titulo="Última actualización" valor={mostrarFecha(detalle.ultimaActualizacion)} />
                      </Box>
                      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, minmax(0, 1fr))' },
                        gap: 3, minWidth: 0 }}>
                        <Dato titulo="Cantidad de docentes" valor={detalle.cantidadDocentes} />
                        <Dato titulo="Cantidad de alumnos" valor={detalle.cantidadAlumnos} />
                        <Dato titulo="Cantidad de escuelas" valor={detalle.cantidadEscuelas} />
                        <Dato titulo="Cantidad de países" valor={detalle.cantidadPaises} />
                      </Box>
                    </Box>
                  </Box>
                  <Box sx={{ px: 3, py: 2, borderTop: '1px solid', borderColor: 'divider' }}>
                    <Typography variant="body2" sx={{ color: colores.textoSecundario,
                      overflowWrap: 'anywhere' }}>
                      Descripción del proyecto: {datosProyecto.descripcion?.trim() || 'Sin descripción registrada.'}
                    </Typography>
                  </Box>
                </Paper>
              </Box>
              <Box sx={{ minWidth: 0, order: { xs: 3, lg: 2 },
                '& > .MuiPaper-root': { height: '100%', boxSizing: 'border-box' } }}>
                <Panel titulo="Planificación"
                  accion={puedeEditar ? <Button variant="text" onClick={onIrAPlanificacion}>Modificar</Button> : undefined}>
                  {detalle.planificacion === null ? (
                    <Typography color="text.secondary">Planificación pendiente.</Typography>
                  ) : (
                    <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, minmax(0, 1fr))' }, gap: 3 }}>
                      {/* Fila 1: fechas planificadas. */}
                      <Dato titulo="Fecha de comienzo" valor={mostrarFecha(detalle.planificacion.fechaInicio)} />
                      <Dato titulo="Fecha de finalización" valor={mostrarFecha(detalle.planificacion.fechaFinalizacion)} />

                      {/* Fila 2: duración y cantidad de clases. */}
                      <Dato titulo="Cantidad de meses" valor={detalle.planificacion.duracionMeses} />
                      <Dato titulo="Cantidad de clases" valor={detalle.planificacion.cantidadClases} />

                      {/* Fila 3: frecuencia de clases. */}
                      <Dato titulo="Frecuencia de clases" valor={mostrarFrecuencia(detalle.planificacion.frecuenciaClases)} />
                    </Box>
                  )}
                </Panel>
              </Box>
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3,
                minWidth: 0, order: { xs: 2, lg: 3 } }}>
                <Panel titulo="Parámetros y notificaciones"
                  accion={puedeEditar ? <Button variant="text" onClick={onIrASeguimiento}>Modificar</Button> : undefined}>
                  {detalle.seguimiento === null ? (
                    <Typography color="text.secondary">Configuración de seguimiento pendiente.</Typography>
                  ) : (
                    <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 3 }}>
                      <Dato titulo="Umbral de próximo atraso" valor={`${detalle.seguimiento.umbralProximoAtrasoSemanas} semanas`} />
                      <Dato titulo="Umbral de atraso" valor={`${detalle.seguimiento.umbralAtrasoSemanas} semanas`} />
                      <Divider sx={{ gridColumn: '1 / -1' }} />
                      <Dato titulo="Responsable de seguimiento" valor={detalle.seguimiento.responsableSeguimiento} />
                      <Dato titulo="Responsable de proyecto" valor={detalle.seguimiento.responsableProyecto} />
                      <Dato titulo="Día de envío del reporte semanal" valor={mostrarDia(detalle.seguimiento.diaInformeSemanal)} />
                      <Dato titulo="Día de envío del informe mensual" valor={mostrarDiaMensual(
                        detalle.seguimiento.tipoDiaInformeMensual, detalle.seguimiento.diaInformeMensual)} />
                    </Box>
                  )}
                </Panel>

                <Panel titulo="Programa del proyecto"
                  accion={puedeEditar ? (
                    <Button variant="text" onClick={abrirConfiguracionEsquema}>
                      Ver programa
                    </Button>
                  ) : undefined}>
                  <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 3 }}>
                    <Dato titulo="Clases del programa" valor="Ninguna" />
                    <Dato titulo="Estado" valor="Disponible en la pestaña Programa" />
                  </Box>
                  <Typography variant="body2" sx={{ color: colores.textoSecundario, mt: 2 }}>
                    El programa contiene las clases y preguntas comunes a todos los talleres.
                  </Typography>
                </Panel>
              </Box>
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3,
                minWidth: 0, order: 4 }}>
                <Panel titulo="Talleres"
                  accion={<Button variant="text" onClick={abrirTalleres}>Ver talleres</Button>}>
                  {advertenciaTalleres !== '' && <Alert severity="warning" sx={{ mb: 2 }}>
                    {advertenciaTalleres}
                  </Alert>}
                  {talleresResumen === null ? (
                    <Typography color="text.secondary" variant="body2">
                      Sin datos de talleres disponibles para el proyecto.
                    </Typography>
                  ) : (
                    <Box>
                      <FilaTaller etiqueta="Total de talleres en la plataforma"
                        valor={metadatosTalleres?.cantidadTalleresPlataforma ?? '—'} />
                      <FilaTaller etiqueta="Talleres en plazo" valor={talleresEnPlazo} />
                      <FilaTaller etiqueta="Talleres próximos a atraso" valor={proximosAtraso} />
                      <FilaTaller etiqueta="Talleres atrasados" valor={talleresAtrasados} />
                      <FilaTaller etiqueta="Talleres no iniciados" valor={talleresNoIniciados} />
                      <Box sx={{ display: 'flex', gap: 2, alignItems: 'center', flexWrap: 'wrap',
                        px: 1.5, pt: 1.5, pb: 0.5 }}>
                        <Typography sx={{ fontSize: 14, color: colores.textoSecundario }}>
                          Última sincronización
                        </Typography>
                        <Typography sx={{ fontSize: 14, color: colores.textoSecundario }}>
                          {mostrarFechaSincronizacion(metadatosTalleres?.ultimaSincronizacion)}
                        </Typography>
                      </Box>
                    </Box>
                  )}
                </Panel>
              </Box>
            </Box>
          )}
          {!cargando && error === '' && detalle !== null && solapa === 'programa' && (
            <ProgramaProyecto idProyecto={proyecto.idProyecto} puedeActualizar={rol !== 'EXTERNO'}
              onSesionExpirada={onSesionExpirada} />
          )}
          {!cargando && error === '' && detalle !== null && solapa !== 'resumen' && solapa !== 'programa' && (
              <Alert severity="info">
                {solapa === 'talleres' ? 'La consulta de talleres requiere la integración con la plataforma educativa.'
                  : solapa === 'evidencias' ? 'Las evidencias globales del proyecto se implementarán en la Etapa 3.'
                  : solapa === 'encuestas' ? 'Las encuestas globales del proyecto se implementarán en la Etapa 3.'
                  : 'El resumen de avance global del proyecto se implementará en la Etapa 3.'}
              </Alert>
            )}
        </Box>
      </Paper>
    </Box>
  );
}

export default DetalleProyectoPage;
