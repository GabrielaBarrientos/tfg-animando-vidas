
import {
  useEffect,
  useState,
  type ChangeEvent,
  type FormEvent,
  type SyntheticEvent
} from 'react';

import ChevronRightIcon from '@mui/icons-material/ChevronRight';

import {
  obtenerPlanificacionProyecto,
  guardarPlanificacionProyecto,
  type FrecuenciaClases
} from '../services/planificacionApi';

import type { Proyecto } from '../services/proyectosApi';
import { SesionExpiradaError } from '../services/erroresApi';

import { colores } from '../theme';

import {
  Alert,
  Box,
  Breadcrumbs,
  Button,
  Chip,
  CircularProgress,
  Link,
  MenuItem,
  Paper,
  Snackbar,
  Step,
  StepLabel,
  Stepper,
  TextField,
  Typography
} from '@mui/material';

interface PlanificacionProyectoPageProps {
  proyecto: Proyecto;
  onVolver: () => void;
  onSesionExpirada: () => void;
}

// Cálculo para mostrar la duración antes de guardar.
// El backend realiza el cálculo definitivo.
function calcularDuracionMeses(
  fechaInicio: string,
  fechaFinalizacion: string
): number | null {

  if (
    fechaInicio === '' ||
    fechaFinalizacion === '' ||
    fechaFinalizacion <= fechaInicio
  ) {
    return null;
  }

  const anioInicio = Number(fechaInicio.substring(0, 4));
  const mesInicio = Number(fechaInicio.substring(5, 7));

  const anioFinal = Number(fechaFinalizacion.substring(0, 4));
  const mesFinal = Number(fechaFinalizacion.substring(5, 7));

  return (anioFinal - anioInicio) * 12 + (mesFinal - mesInicio) + 1;
}

function calcularMaximoClases(
  fechaInicio: string,
  fechaFinalizacion: string,
  frecuencia: FrecuenciaClases | ''
): number | null {

  if (
    fechaInicio === '' ||
    fechaFinalizacion === '' ||
    fechaFinalizacion <= fechaInicio ||
    frecuencia === ''
  ) {
    return null;
  }

  // Trabajar en UTC para evitar diferencias de zona horaria.
  const inicio = new Date(fechaInicio + 'T00:00:00Z');
  const fin = new Date(fechaFinalizacion + 'T00:00:00Z');

  const milisegundosDia = 24 * 60 * 60 * 1000;

  // Ambas fechas son inclusivas.
  const diasTotales =
    Math.round(
      (fin.getTime() - inicio.getTime()) / milisegundosDia
    ) + 1;

  if (frecuencia === 'SEMANAL') {
    return Math.ceil(diasTotales / 7);
  }

  if (frecuencia === 'QUINCENAL') {
    return Math.ceil(diasTotales / 14);
  }

  // Frecuencia mensual: considerar las fechas exactas.
  const mesesTranscurridos =
    (fin.getUTCFullYear() - inicio.getUTCFullYear()) * 12 +
    (fin.getUTCMonth() - inicio.getUTCMonth());

  // Determinar el último día del mes de finalización.
  const ultimoDiaMes = new Date(
    Date.UTC(
      fin.getUTCFullYear(),
      fin.getUTCMonth() + 1,
      0
    )
  ).getUTCDate();

  // Mantener el día de inicio, ajustándolo cuando sea necesario.
  const diaClase = Math.min(
    inicio.getUTCDate(),
    ultimoDiaMes
  );

  const ultimaFechaPosible = new Date(
    Date.UTC(
      fin.getUTCFullYear(),
      fin.getUTCMonth(),
      diaClase
    )
  );

  let maximoClases = mesesTranscurridos;

  if (ultimaFechaPosible <= fin) {
    maximoClases++;
  }

  return maximoClases;
}

  function PlanificacionProyectoPage({
    proyecto,
    onVolver,
    onSesionExpirada
  }: PlanificacionProyectoPageProps) {

  // Datos del formulario.
  const [fechaInicio, setFechaInicio] = useState('');
  const [fechaFinalizacion, setFechaFinalizacion] = useState('');
  const [cantidadClases, setCantidadClases] = useState('');
  const [frecuenciaClases, setFrecuenciaClases] =
    useState<FrecuenciaClases | ''>('');

  // Estado de la pantalla.
  const [cargando, setCargando] = useState(true);
  const [guardando, setGuardando] = useState(false);
  const [errorCarga, setErrorCarga] = useState(false);
  const [error, setError] = useState('');
  const [mensajeExito, setMensajeExito] = useState('');

  // Indica si el usuario intentó guardar el formulario.
  const [intentoGuardar, setIntentoGuardar] = useState(false);

  // Permite volver a consultar la planificación si falla la carga.
  const [intentoCarga, setIntentoCarga] = useState(0);

  const maximoClases = calcularMaximoClases(
    fechaInicio,
    fechaFinalizacion,
    frecuenciaClases
  );

  const numeroClases = Number(cantidadClases);

  const excedeCapacidad =
    maximoClases !== null &&
    cantidadClases !== '' &&
    Number.isInteger(numeroClases) &&
    numeroClases > maximoClases;

  const fechaFinalizacionInvalida =
  fechaInicio !== '' &&
  fechaFinalizacion !== '' &&
  fechaFinalizacion <= fechaInicio;

  const cantidadInvalida =
    cantidadClases !== '' &&
    (
      !Number.isInteger(Number(cantidadClases)) ||
      Number(cantidadClases) <= 0
    );

  // Recuperar los datos al ingresar a la pantalla.
 useEffect(function cargarDatosIniciales() {

    let activo = true;

    async function cargarPlanificacion() {
      try {
        const resultado = await obtenerPlanificacionProyecto(
          proyecto.idProyecto
        );

        if (!activo) {
          return;
        }

        // null indica que el proyecto todavía no tiene planificación.
        if (resultado !== null) {
          setFechaInicio(resultado.fechaInicio);
          setFechaFinalizacion(resultado.fechaFinalizacion);
          setCantidadClases(String(resultado.cantidadClases));
          setFrecuenciaClases(resultado.frecuenciaClases);
        }

      } catch (excepcion) {

        if (!activo) {
          return;
        }

        if (excepcion instanceof SesionExpiradaError) {
          onSesionExpirada();
          return;
        }

        setErrorCarga(true);

      } finally {
        if (activo) {
          setCargando(false);
        }
      }
    }

    void cargarPlanificacion();

    return function cancelarCarga() {
      activo = false;
    };

  }, [proyecto.idProyecto, intentoCarga, onSesionExpirada]);

  function reintentarCarga() {
    setCargando(true);
    setErrorCarga(false);
    setError('');

    setIntentoCarga(intentoCarga + 1);
  }

  function cambiarFechaInicio(
    evento: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) {
    setFechaInicio(evento.target.value);
    setMensajeExito('');
  }

  function cambiarFechaFinalizacion(
    evento: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) {
    setFechaFinalizacion(evento.target.value);
    setMensajeExito('');
  }

  function cambiarCantidadClases(
    evento: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) {
    setCantidadClases(evento.target.value);
    setMensajeExito('');
  }

  function cambiarFrecuenciaClases(
    evento: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) {
    setFrecuenciaClases(evento.target.value as FrecuenciaClases);
    setMensajeExito('');
  }


  async function guardarPlanificacion(
    evento: FormEvent<HTMLFormElement>
  ) {
    evento.preventDefault();

    if (guardando || cargando || errorCarga) {
      return;
    }

    setIntentoGuardar(true);
    setError('');
    setMensajeExito('');

    // Validar las fechas.
    if (
      fechaInicio === '' ||
      fechaFinalizacion === '' ||
      fechaFinalizacionInvalida
    ) {
      return;
    }

    // Validar la cantidad de clases.
    const numeroClases = Number(cantidadClases);

    if (
      cantidadClases === '' ||
      !Number.isInteger(numeroClases) ||
      numeroClases <= 0
    ) {
      return;
    }

    // Validar la frecuencia.
    if (frecuenciaClases === '') {
      return;
    }

    // Verificar que las clases puedan realizarse
    // dentro del período planificado.
    if (excedeCapacidad) {
      return;
    }

    setGuardando(true);

    try {
      await guardarPlanificacionProyecto(
        proyecto.idProyecto,
        {
          fechaInicio: fechaInicio,
          fechaFinalizacion: fechaFinalizacion,
          cantidadClases: numeroClases,
          frecuenciaClases: frecuenciaClases
        }
      );

      setIntentoGuardar(false);

      setMensajeExito(
        'La planificación se guardó correctamente.'
      );

    } catch (excepcion) {

      if (excepcion instanceof SesionExpiradaError) {
        onSesionExpirada();
        return;
      }

      setError(
        'No se pudo guardar la planificación. ' +
        'Intentá nuevamente.'
      );

    } finally {
      setGuardando(false);
    }
  }


  function cerrarNotificacion(_evento: SyntheticEvent | Event | null, motivo?: string) {
    if (motivo === 'clickaway') {
      return;
    }

    setError('');
    setMensajeExito('');
  }

  const duracionMeses = calcularDuracionMeses(
    fechaInicio,
    fechaFinalizacion
  );

  return (
    <Box
      component="section"
      sx={{
        width: '100%',
        boxSizing: 'border-box',
        px: { xs: 2, md: 4 },
        py: 2
      }}
    >
      {/* Navegación */}
      <Breadcrumbs
        separator={<ChevronRightIcon sx={{ fontSize: 16 }} />}
        sx={{ mb: 2 }}
      >
        <Link
          component="button"
          type="button"
          underline="hover"
          onClick={onVolver}
          sx={{ fontSize: 14, color: '#667085' }}
        >
          Inicio
        </Link>

        <Link
          component="button"
          type="button"
          underline="hover"
          onClick={onVolver}
          sx={{ fontSize: 14, color: '#667085' }}
        >
          Proyectos
        </Link>

        <Typography
          title={proyecto.nombreCompleto}
          sx={{
            fontSize: 14,
            color: '#667085',
            maxWidth: { xs: 180, md: 450 },
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap'
          }}
        >
          {proyecto.nombreCompleto}
        </Typography>

        <Typography
          sx={{
            fontSize: 14,
            color: colores.verdeOscuro,
            fontWeight: 500
          }}
        >
          Planificación
        </Typography>

      </Breadcrumbs>

      {/* Identificación del proyecto */}
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: 2,
          mb: 1.5
        }}
      >
        <Typography component="h1" variant="h5">
          {proyecto.nombreCompleto}
        </Typography>

        {proyecto.estado === 'PENDIENTE_PLANIFICACION' && (
          <Chip
            label="Pendiente"
            sx={{
              backgroundColor: colores.chipAmarillo,
              color: colores.textoPrimario,
              borderRadius: '6px',
              height: 32,
              fontSize: 14,
              fontWeight: 500
            }}
          />
        )}
      </Box>

      {/* Etapas de configuración */}
      <Paper
        variant="outlined"
        sx={{
          mb: 2,
          height: 48,
          px: 3,
          display: 'flex',
          alignItems: 'center',
          borderRadius: 2,
          overflowX: 'auto',
          boxSizing: 'border-box'
        }}
      >
        <Stepper
          activeStep={1}
          sx={{ width: '100%', minWidth: 720 }}
        >
          <Step>
            <StepLabel>Datos generales</StepLabel>
          </Step>
          <Step>
            <StepLabel>Planificación</StepLabel>
          </Step>
          <Step>
            <StepLabel>Seguimiento</StepLabel>
          </Step>
          <Step>
            <StepLabel>Docentes</StepLabel>
          </Step>
        </Stepper>
      </Paper>

      {/* Formulario de planificación */}
      <Paper
        elevation={0}
        sx={{
          px: 4,
          py: 2,
          minHeight: 580,
          borderRadius: 1
        }}
      >
        <Box sx={{ mb: 3 }}>
          <Typography component="h2" variant="h6">
            Planificación del proyecto
          </Typography>

          <Typography
            variant="body1"
            sx={{ color: colores.textoSecundario }}
          >
            Definí la planificación del proyecto: cuándo se
            desarrolla y con qué frecuencia se espera que
            se dicten las clases.
          </Typography>
        </Box>


        {cargando ? (
          <Box
            sx={{
              display: 'flex',
              justifyContent: 'center',
              py: 5
            }}
          >
            <CircularProgress />
          </Box>) : errorCarga ? (
          <Alert
            severity="error"
            variant="outlined"
            role="alert"
            sx={{ mt: 2 }}
            action={
              <Button
                color="inherit"
                size="small"
                onClick={reintentarCarga}
              >
                Reintentar
              </Button>
            }
          >
            No se pudo cargar la planificación del proyecto.
            Verificá la conexión e intentá nuevamente.
          </Alert>) : (
          <Box
            component="form"
            noValidate
            onSubmit={guardarPlanificacion}
          >
            <Box
              sx={{
                display: 'grid',
                gridTemplateColumns: {
                  xs: '1fr',
                  md: '300px 300px'
                },
                columnGap: { xs: 2, md: 10 },
                rowGap: 2,
                alignItems: 'start'
              }}
            >
              <Box>
                <Typography component="label" htmlFor="fechaInicio" sx={{ display: 'block', mb: 1 }}>
                  Fecha de inicio *
                </Typography>
                <TextField
                  id="fechaInicio"
                  type="date"
                  size="small"
                  fullWidth
                  required
                  value={fechaInicio}
                  onChange={cambiarFechaInicio}
                  error={intentoGuardar && fechaInicio === ''}
                  helperText={
                    intentoGuardar && fechaInicio === ''
                      ? 'La fecha de inicio es obligatoria.'
                      : ''
                  }
                />
              </Box>

              <Box>
                <Typography component="label" htmlFor="fechaFinalizacion" sx={{ display: 'block', mb: 1 }}>
                  Fecha de finalización *
                </Typography>
                <TextField
                  id="fechaFinalizacion"
                  type="date"
                  size="small"
                  fullWidth
                  required
                  value={fechaFinalizacion}
                  onChange={cambiarFechaFinalizacion}
                  error={fechaFinalizacionInvalida || (intentoGuardar && fechaFinalizacion === '')}
                  helperText={
                    fechaFinalizacionInvalida
                      ? 'La fecha de finalización debe ser posterior a la de inicio.'
                      : intentoGuardar && fechaFinalizacion === ''
                        ? 'La fecha de finalización es obligatoria.'
                        : ''
                  }
                />
              </Box>

              <Box>
                <Typography component="label" htmlFor="duracionMeses" sx={{ display: 'block', mb: 1 }}>
                  Duración en meses
                </Typography>
                <TextField
                  id="duracionMeses"
                  size="small"
                  fullWidth
                  value={duracionMeses === null ? '' : duracionMeses}
                  slotProps={{ input: { readOnly: true } }}
                  helperText="Calculada automáticamente en meses calendario."
                />
              </Box>


              <Box>
                <Typography component="label" htmlFor="cantidadClases" sx={{ display: 'block', mb: 1 }}>
                  Cantidad de clases *
                </Typography>
                <TextField
                  id="cantidadClases"
                  type="number"
                  size="small"
                  fullWidth
                  required
                  value={cantidadClases}
                  onChange={cambiarCantidadClases}
                  slotProps={{htmlInput: { min: 1, step: 1 }}}
                  error={excedeCapacidad || cantidadInvalida || (intentoGuardar && cantidadClases === '')}
                  helperText={excedeCapacidad
                      ? `La cantidad máxima de clases para este período es ${maximoClases}.`
                      : cantidadInvalida ||
                        (intentoGuardar && cantidadClases === '')
                        ? 'Ingresá un número entero mayor que cero.'
                        : maximoClases !== null
                          ? `Máximo de clases posibles: ${maximoClases}.`
                          : ''}
                />
              </Box>

              <Box>
                <Typography component="label" htmlFor="frecuenciaClases" sx={{ display: 'block', mb: 1 }}>
                  Frecuencia de clases *
                </Typography>
                <TextField
                  id="frecuenciaClases"
                  select
                  size="small"
                  fullWidth
                  required
                  value={frecuenciaClases}
                  onChange={cambiarFrecuenciaClases}
                  error={intentoGuardar && frecuenciaClases === ''}
                  helperText={intentoGuardar && frecuenciaClases === ''
                      ? 'Seleccioná una frecuencia de clases.'
                      : ''}
                >
                  <MenuItem value="" disabled>
                    Seleccionar frecuencia
                  </MenuItem>
                  <MenuItem value="SEMANAL">Semanal</MenuItem>
                  <MenuItem value="QUINCENAL">Quincenal</MenuItem>
                  <MenuItem value="MENSUAL">Mensual</MenuItem>
                </TextField>
              </Box>

            </Box>

            <Box
              sx={{
                display: 'flex',
                justifyContent: 'flex-end',
                gap: 2,
                mt: 4
              }}
            >
              <Button
                type="button"
                variant="text"
                disabled={guardando}
                onClick={onVolver}
                sx={{ minWidth: 107, height: 36 }}
              >
                Volver
              </Button>

              <Button
                type="submit"
                variant="contained"
                disabled={guardando || errorCarga}
                sx={{ minWidth: 200, height: 36 }}
              >
                {guardando ? 'Guardando...' : 'Guardar planificación'}
              </Button>
            </Box>
          </Box>
        )}
      </Paper>


      <Snackbar
        key={error !== '' ? `error-${error}` : `exito-${mensajeExito}`}
        open={error !== '' || mensajeExito !== ''}
        autoHideDuration={error !== '' ? null : 5000}
        onClose={cerrarNotificacion}
        anchorOrigin={{
          vertical: 'top',
          horizontal: 'right'
        }}
        sx={{
          top: { xs: 64, sm: 64 },
          right: { xs: 16, sm: 24 }
        }}
      >
        <Alert
          severity={error !== '' ? 'error' : 'success'}
          variant="filled"
          role={error !== '' ? 'alert' : 'status'}
          onClose={cerrarNotificacion}
          sx={{
            backgroundColor:
              error !== '' ? 'error.main' : colores.verdeToastExito,
            color: '#FFFFFF',
            minWidth: { xs: 0, sm: 320 },
            maxWidth: 420,
            boxShadow: 3
          }}
        >
          {error !== '' ? error : mensajeExito}
        </Alert>
      </Snackbar>
    </Box>
  );
}

export default PlanificacionProyectoPage;
