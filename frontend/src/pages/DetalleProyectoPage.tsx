import { useEffect, useState } from 'react';

import {
  Alert, Box, Breadcrumbs, Button, Chip, CircularProgress,
  Divider, Link, Paper, Typography
} from '@mui/material';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';

import { obtenerDetalleProyecto, type DetalleProyecto } from '../services/detalleProyectoApi';
import { SesionExpiradaError } from '../services/erroresApi';
import { listarTalleres, sincronizarTalleres, type TallerResumen } from '../services/talleresApi';
import type { Proyecto } from '../services/proyectosApi';
import { colores } from '../theme';
import ProyectoTabs, { type SolapaProyecto } from '../components/ProyectoTabs';


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

function DetalleProyectoPage({
  proyecto, rol, onVolverAlListado, onIrADocentes,
  onIrAPlanificacion, onIrASeguimiento, onIrATalleres, onSesionExpirada, solapaInicial = 'resumen'
}: DetalleProyectoPageProps) {
  const [detalle, setDetalle] = useState<DetalleProyecto | null>(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');
  const [advertenciaTalleres, setAdvertenciaTalleres] = useState('');
  const [talleresResumen, setTalleresResumen] = useState<TallerResumen[] | null>(null);
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
    setSolapa('preguntas');
  }

  function abrirTalleres() {
    onIrATalleres();
  }

  function cambiarSolapa(valor: SolapaProyecto) {
    if (valor === 'docentes' && rol !== 'EXTERNO') {
      onIrADocentes();
      return;
    }
    if (valor === 'talleres') {
      onIrATalleres();
      return;
    }
    setSolapa(valor);
  }

  const datosProyecto = detalle?.proyecto ?? proyecto;
  const puedeEditar = rol === 'ADMINISTRADOR';
  let talleresAtrasados = 0;
  let proximosAtraso = 0;
  let evidenciasPendientes = 0;
  if (talleresResumen !== null) {
    for (const taller of talleresResumen) {
      if (taller.estado === 'ATRASADO') talleresAtrasados++;
      if (taller.estado === 'PROXIMO_ATRASO') proximosAtraso++;
      if (taller.evidencias === 'PENDIENTES') evidenciasPendientes++;
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
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', lg: '1.4fr 0.70fr' }, gap: 3 }}>
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
                <Panel titulo="Datos generales">
                  <Box sx={{
                    display: 'grid',
                    gridTemplateColumns: {
                      xs: '1fr',
                      sm: 'repeat(2, minmax(0, 1fr))',
                      lg: '1.45fr 0.9fr 0.9fr 0.9fr'
                    },
                    gap: 3
                  }}>
                    {/* Fila 1: datos del proyecto y participantes. */}
                    <Dato titulo="Nombre del proyecto" valor={datosProyecto.nombre} />
                    <Dato titulo="Año" valor={datosProyecto.anio} />
                    <Dato titulo="Cantidad de docentes" valor={detalle.cantidadDocentes} />
                    <Dato titulo="Cantidad de alumnos" valor="Sin datos" />

                    {/* Fila 2: empresa, actualización e instituciones. */}
                    <Dato titulo="Empresa" valor={datosProyecto.nombreEmpresa} />
                    <Dato titulo="Última actualización" valor={mostrarFecha(detalle.ultimaActualizacion)} />
                    <Dato titulo="Cantidad de escuelas" valor="Sin datos" />
                    <Dato titulo="Cantidad de países" valor="Sin datos" />

                    {/* Fila 3: descripción, a todo el ancho. */}
                    <Box sx={{ gridColumn: '1 / -1' }}>
                      <Dato titulo="Descripción" valor={datosProyecto.descripcion || 'Sin descripción'} />
                    </Box>
                  </Box>
                </Panel>

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

                <Panel titulo="Preguntas de encuestas"
                  accion={puedeEditar ? (
                    <Button variant="text" onClick={abrirConfiguracionEsquema}>
                      Configurar esquema
                    </Button>
                  ) : undefined}>
                  <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 3 }}>
                    <Dato titulo="Preguntas obtenidas desde la plataforma" valor="Ninguna" />
                    <Dato titulo="Estado" valor="Sincronización pendiente" />
                  </Box>
                  <Typography variant="body2" sx={{ color: colores.textoSecundario, mt: 2 }}>
                    La sincronización y configuración del análisis de preguntas se implementarán en incrementos posteriores.
                  </Typography>
                </Panel>
              </Box>

              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
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
                    <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 2 }}>
                      <Dato titulo="Talleres" valor={talleresResumen.length} />
                      <Dato titulo="Atrasados" valor={talleresAtrasados} />
                      <Dato titulo="Próximos a atraso" valor={proximosAtraso} />
                      <Dato titulo="Evidencias pendientes" valor={evidenciasPendientes} />
                    </Box>
                  )}
                </Panel>
              </Box>
            </Box>
          )}
          {!cargando && error === '' && detalle !== null && solapa === 'docentes' && rol === 'EXTERNO' && (
            <Alert severity="info">La consulta individual de docentes no está habilitada para usuarios externos.</Alert>
          )}
          {!cargando && error === '' && detalle !== null && solapa !== 'resumen' &&
            solapa !== 'docentes' && (
              <Alert severity="info">
                {solapa === 'preguntas' ? 'La sincronización de preguntas y su configuración estarán disponibles en un incremento posterior.'
                  : solapa === 'talleres' ? 'La consulta de talleres requiere la integración con la plataforma educativa.'
                  : solapa === 'evidencias' ? 'Las evidencias se consultarán cuando se sincronicen los talleres.'
                  : solapa === 'encuestas' ? 'Las encuestas estarán disponibles cuando se implementen los talleres.'
                  : 'El resumen de avance estará disponible cuando existan datos de talleres sincronizados.'}
              </Alert>
            )}
        </Box>
      </Paper>
    </Box>
  );
}

export default DetalleProyectoPage;
