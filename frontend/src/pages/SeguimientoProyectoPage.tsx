import {
  useEffect,
  useState,
  type ChangeEvent,
  type FormEvent,
  type SyntheticEvent
} from 'react';

import ChevronRightIcon from '@mui/icons-material/ChevronRight';

import {
  Alert,
  Box,
  Breadcrumbs,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  FormLabel,
  InputAdornment,
  Link,
  MenuItem,
  Paper,
  Snackbar,
  Step,
  StepButton,
  StepLabel,
  Stepper,
  TextField,
  Typography
} from '@mui/material';

import {
  obtenerResponsablesElegibles,
  obtenerConfiguracionSeguimiento,
  guardarConfiguracionSeguimiento,
  type DiaInformeSemanal,
  type TipoDiaInformeMensual,
  type ResponsableElegible
} from '../services/seguimientoApi';

import type { Proyecto } from '../services/proyectosApi';
import { SesionExpiradaError } from '../services/erroresApi';
import { colores } from '../theme';

interface SeguimientoProyectoPageProps {
  proyecto: Proyecto;
  onVolver: () => void;
  onVerDatosGenerales: () => void;
  onVolverAlListado: () => void;
  onConfiguracionGuardada: (estadoProyecto: string) => void;
  onIrADocentes: () => void;
  onSesionExpirada: () => void;
}

const diasSemana: { valor: DiaInformeSemanal; nombre: string }[] = [
  { valor: 'LUNES', nombre: 'Lunes' },
  { valor: 'MARTES', nombre: 'Martes' },
  { valor: 'MIERCOLES', nombre: 'Miércoles' },
  { valor: 'JUEVES', nombre: 'Jueves' },
  { valor: 'VIERNES', nombre: 'Viernes' },
  { valor: 'SABADO', nombre: 'Sábado' },
  { valor: 'DOMINGO', nombre: 'Domingo' }
];

function SeguimientoProyectoPage({
  proyecto,
  onVolver,
  onVerDatosGenerales,
  onVolverAlListado,
  onConfiguracionGuardada,
  onIrADocentes,
  onSesionExpirada
}: SeguimientoProyectoPageProps) {

  const [umbralProximo, setUmbralProximo] = useState('2');
  const [umbralAtraso, setUmbralAtraso] = useState('3');
  const [responsableSemanal, setResponsableSemanal] = useState('');
  const [diaSemanal, setDiaSemanal] = useState<DiaInformeSemanal>('LUNES');
  const [responsableMensual, setResponsableMensual] = useState('');
  const [tipoDiaMensual, setTipoDiaMensual] =
    useState<TipoDiaInformeMensual>('ULTIMO_DIA_HABIL');
  const [diaMensual, setDiaMensual] = useState('');

  const [responsables, setResponsables] = useState<ResponsableElegible[]>([]);
  const [cargando, setCargando] = useState(true);
  const [errorCarga, setErrorCarga] = useState(false);
  const [intentoCarga, setIntentoCarga] = useState(0);
  const [guardando, setGuardando] = useState(false);
  const [intentoGuardar, setIntentoGuardar] = useState(false);
  const [error, setError] = useState('');
  const [mensajeExito, setMensajeExito] = useState('');
  const [estadoPlanificado, setEstadoPlanificado] = useState(proyecto.estado === 'PLANIFICADO');
  const [cambiosSinGuardar, setCambiosSinGuardar] = useState(false);
  const [destinoPendiente, setDestinoPendiente] =
    useState<'planificacion' | 'datos-generales' | 'proyectos' | 'docentes' | null>(null);

  useEffect(function cargarConfiguracionInicial() {
    let activo = true;

    async function cargarDatos() {
      try {
        const resultados = await Promise.all([
          obtenerResponsablesElegibles(proyecto.idProyecto),
          obtenerConfiguracionSeguimiento(proyecto.idProyecto)
        ]);

        if (!activo) {
          return;
        }

        const usuarios = resultados[0];
        const configuracion = resultados[1];
        setResponsables(usuarios);

        if (configuracion !== null) {
          setUmbralProximo(String(configuracion.umbralProximoAtrasoSemanas));
          setUmbralAtraso(String(configuracion.umbralAtrasoSemanas));
          setResponsableSemanal(String(configuracion.idResponsableSeguimiento));
          setDiaSemanal(configuracion.diaInformeSemanal);
          setResponsableMensual(String(configuracion.idResponsableProyecto));
          setTipoDiaMensual(configuracion.tipoDiaInformeMensual);
          setDiaMensual(configuracion.diaInformeMensual === null
            ? '' : String(configuracion.diaInformeMensual));
          setEstadoPlanificado(configuracion.estadoProyecto === 'PLANIFICADO');
          setCambiosSinGuardar(false);
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

    void cargarDatos();

    return function cancelarCarga() {
      activo = false;
    };
  }, [proyecto.idProyecto, intentoCarga, onSesionExpirada]);

  function reintentarCarga() {
    setErrorCarga(false);
    setCargando(true);
    setIntentoCarga(intentoCarga + 1);
  }

  function ejecutarNavegacion(destino: 'planificacion' | 'datos-generales' | 'proyectos' | 'docentes') {
    if (destino === 'docentes') {
      onIrADocentes();
    } else if (destino === 'planificacion') {
      onVolver();
    } else if (destino === 'datos-generales') {
      onVerDatosGenerales();
    } else {
      onVolverAlListado();
    }
  }

  function solicitarNavegacion(destino: 'planificacion' | 'datos-generales' | 'proyectos' | 'docentes') {
    if (guardando) {
      return;
    }

    if (cambiosSinGuardar) {
      setDestinoPendiente(destino);
      return;
    }

    ejecutarNavegacion(destino);
  }

  function volverAPlanificacion() {
    solicitarNavegacion('planificacion');
  }

  function verDatosGenerales() {
    solicitarNavegacion('datos-generales');
  }

  function irAProyectos() {
    solicitarNavegacion('proyectos');
  }

  function irADocentes() {
    solicitarNavegacion('docentes');
  }

  function cancelarSalida() {
    setDestinoPendiente(null);
  }

  function descartarCambiosYSalir() {
    const destino = destinoPendiente;
    setDestinoPendiente(null);

    if (destino !== null) {
      ejecutarNavegacion(destino);
    }
  }

  function cambiarUmbralProximo(evento: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) {
    setUmbralProximo(evento.target.value);
    setCambiosSinGuardar(true);
  }

  function cambiarUmbralAtraso(evento: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) {
    setUmbralAtraso(evento.target.value);
    setCambiosSinGuardar(true);
  }

  function cambiarResponsableSemanal(evento: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) {
    setResponsableSemanal(evento.target.value);
    setCambiosSinGuardar(true);
  }

  function cambiarDiaSemanal(evento: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) {
    setDiaSemanal(evento.target.value as DiaInformeSemanal);
    setCambiosSinGuardar(true);
  }

  function cambiarResponsableMensual(evento: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) {
    setResponsableMensual(evento.target.value);
    setCambiosSinGuardar(true);
  }

  function cambiarTipoDiaMensual(evento: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) {
    const valor = evento.target.value as TipoDiaInformeMensual;
    setTipoDiaMensual(valor);
    setCambiosSinGuardar(true);
    if (valor === 'ULTIMO_DIA_HABIL') {
      setDiaMensual('');
    }
  }

  function cambiarDiaMensual(evento: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) {
    setDiaMensual(evento.target.value);
    setCambiosSinGuardar(true);
  }

  const proximoNumero = Number(umbralProximo);
  const atrasoNumero = Number(umbralAtraso);
  const proximoValido = umbralProximo !== '' && Number.isInteger(proximoNumero) && proximoNumero > 0;
  const atrasoValido = umbralAtraso !== '' && Number.isInteger(atrasoNumero) && atrasoNumero > proximoNumero;
  const diaMensualNumero = Number(diaMensual);
  const diaMensualValido = tipoDiaMensual === 'ULTIMO_DIA_HABIL'
    || (diaMensual !== '' && Number.isInteger(diaMensualNumero)
      && diaMensualNumero >= 1 && diaMensualNumero <= 31);
  const responsableSemanalValido = responsableSemanal !== ''
    && responsables.some(function buscarSemanal(usuario) {
      return usuario.idUsuario === Number(responsableSemanal);
    });
  const responsableMensualValido = responsableMensual !== ''
    && responsables.some(function buscarMensual(usuario) {
      return usuario.idUsuario === Number(responsableMensual);
    });
  const responsablesValidos = responsableSemanalValido && responsableMensualValido;

  // El paso se considera completo únicamente cuando su configuración válida
  // quedó guardada y no hay modificaciones pendientes.
  const seguimientoCompleto = estadoPlanificado
    && !cargando
    && !errorCarga
    && !cambiosSinGuardar
    && proximoValido
    && atrasoValido
    && diaMensualValido
    && responsablesValidos;

  async function guardarSeguimiento(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();

    if (guardando || cargando || errorCarga) {
      return;
    }

    setIntentoGuardar(true);
    setError('');
    setMensajeExito('');

    if (!proximoValido || !atrasoValido || !diaMensualValido || !responsablesValidos) {
      return;
    }

    setGuardando(true);

    try {
      const resultado = await guardarConfiguracionSeguimiento(proyecto.idProyecto, {
        umbralProximoAtrasoSemanas: proximoNumero,
        umbralAtrasoSemanas: atrasoNumero,
        idResponsableSeguimiento: Number(responsableSemanal),
        diaInformeSemanal: diaSemanal,
        idResponsableProyecto: Number(responsableMensual),
        tipoDiaInformeMensual: tipoDiaMensual,
        diaInformeMensual: tipoDiaMensual === 'ULTIMO_DIA_HABIL' ? null : diaMensualNumero
      });

      setEstadoPlanificado(resultado.estadoProyecto === 'PLANIFICADO');
      setCambiosSinGuardar(false);
      setMensajeExito('Configuración guardada correctamente.');
      onConfiguracionGuardada(resultado.estadoProyecto);
    } catch (excepcion) {
      if (excepcion instanceof SesionExpiradaError) {
        onSesionExpirada();
        return;
      }

      if (excepcion instanceof Error) {
        setError(excepcion.message);
      } else {
        setError('No se pudo guardar la configuración.');
      }
    } finally {
      setGuardando(false);
    }
  }

  function cerrarNotificacion(_evento: SyntheticEvent | Event, motivo?: string) {
    if (motivo === 'clickaway') {
      return;
    }
    setError('');
    setMensajeExito('');
  }

  return (
    <Box
      component="section"
      sx={{ px: { xs: 2, md: 4 }, py: 2, width: '100%', boxSizing: 'border-box' }}
    >
      <Breadcrumbs separator={<ChevronRightIcon sx={{ fontSize: 16 }} />} sx={{ mb: 1.5 }}>
        <Link component="button" type="button" underline="hover" color="inherit" onClick={irAProyectos}>
          Inicio
        </Link>
        <Link component="button" type="button" underline="hover" color="inherit" onClick={irAProyectos}>
          Proyectos
        </Link>
        <Typography variant="body2" color="text.secondary">{proyecto.nombreCompleto}</Typography>
        <Typography variant="body2" sx={{ color: colores.verdeOscuro }}>Seguimiento</Typography>
      </Breadcrumbs>

      <Box sx={{ display: 'flex', alignItems: 'center', flexWrap: 'wrap', gap: 2, mb: 2 }}>
        <Typography component="h1" variant="h5">{proyecto.nombreCompleto}</Typography>
        <Chip
          label={estadoPlanificado ? 'Planificado' : 'Pendiente'}
          sx={{
            backgroundColor: estadoPlanificado ? colores.chipVerdeClaro : colores.chipAmarillo,
            color: colores.textoPrimario,
            borderRadius: '6px',
            height: 32,
            fontSize: 14,
            fontWeight: 500
          }}
        />
      </Box>

      <Paper
        variant="outlined"
        sx={{
          mb: 2,
          minHeight: 48,
          px: { xs: 1, sm: 2, md: 3 },
          py: { xs: 1.5, sm: 1 },
          display: 'flex',
          alignItems: 'center',
          borderRadius: 2,
          boxSizing: 'border-box'
        }}
      >
        <Stepper
          activeStep={2}
          sx={{
            width: '100%',
            minWidth: 0,
            '& .MuiStep-root': { minWidth: 0, px: { xs: 0.25, md: 1 } },
            '& .MuiStepLabel-root': {
              flexDirection: { xs: 'column', md: 'row' }
            },
            '& .MuiStepLabel-label': {
              fontSize: { xs: 10, sm: 12, md: 14 },
              lineHeight: 1.3,
              whiteSpace: 'normal',
              textAlign: { xs: 'center', md: 'left' }
            },
            '& .MuiStepButton-root': {
              width: '100%',
              minWidth: 0,
              minHeight: 32,
              boxSizing: 'border-box',
              margin: 0,
              padding: { xs: 0.25, sm: 0.75 }
            }
          }}
        >
          <Step completed>
            <StepButton onClick={verDatosGenerales} disabled={cargando || guardando}>
              Datos generales
            </StepButton>
          </Step>
          <Step>
            <StepButton onClick={volverAPlanificacion} disabled={cargando || guardando}>
              Planificación
            </StepButton>
          </Step>
          <Step completed={seguimientoCompleto}>
            <StepLabel
              sx={{
                '& .MuiStepIcon-root.Mui-active': {
                  color: seguimientoCompleto ? colores.rosa : 'text.disabled'
                }
              }}
            >
              Seguimiento
            </StepLabel>
          </Step>
          <Step>
            <StepButton onClick={irADocentes} disabled={cargando || guardando || !estadoPlanificado}>
              Docentes
            </StepButton>
          </Step>
        </Stepper>
      </Paper>

      <Paper sx={{ px: { xs: 2, md: 4 }, py: 2, borderRadius: 1 }} elevation={0}>
        <Typography component="h2" variant="h6">
          Configuración del seguimiento y notificaciones
        </Typography>
        <Typography variant="body1" sx={{ color: colores.textoSecundario, mb: 3 }}>
          Definí los criterios y personas involucradas en el seguimiento del proyecto.
        </Typography>

        {cargando ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
            <CircularProgress />
          </Box>
        ) : errorCarga ? (
          <Alert severity="error" action={<Button color="inherit" onClick={reintentarCarga}>Reintentar</Button>}>
            No se pudieron cargar los parámetros del seguimiento.
          </Alert>
        ) : (
          <Box component="form" noValidate onSubmit={guardarSeguimiento}>
            <Box
              sx={{
                display: 'grid',
                gridTemplateColumns: { xs: 'minmax(0, 1fr)', lg: 'repeat(3, minmax(0, 1fr))' },
                columnGap: '40px',
                rowGap: '40px',
                alignItems: 'start',
                mb: 3
              }}
            >
              <Box sx={{ display: 'flex', flexDirection: 'column', gap: '40px', minWidth: 0 }}>
                <Typography component="h3" variant="subtitle1" sx={{ fontWeight: 600 }}>
                  Parámetros de seguimiento
                </Typography>
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                  <FormLabel
                    htmlFor="umbral-proximo-atraso"
                    required
                    sx={{ color: 'text.primary', '&.Mui-focused': { color: 'text.primary' } }}
                  >
                    Umbral de próximo atraso
                  </FormLabel>
                  <Typography variant="body2" sx={{ color: colores.textoSecundario }}>
                    Anticipación con la que el sistema identificará un taller próximo a presentar atraso.
                  </Typography>
                  <TextField
                    id="umbral-proximo-atraso"
                    fullWidth type="number" size="small" required
                    value={umbralProximo} onChange={cambiarUmbralProximo}
                    slotProps={{
                      htmlInput: { min: 1, step: 1 },
                      input: { endAdornment: <InputAdornment position="end">Semanas</InputAdornment> }
                    }}
                    error={intentoGuardar && !proximoValido}
                    helperText={intentoGuardar && !proximoValido
                      ? 'Ingresá un entero positivo.' : undefined}
                  />
                </Box>
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                  <FormLabel
                    htmlFor="umbral-atraso"
                    required
                    sx={{ color: 'text.primary', '&.Mui-focused': { color: 'text.primary' } }}
                  >
                    Umbral de atraso
                  </FormLabel>
                  <Typography variant="body2" sx={{ color: colores.textoSecundario }}>
                    Diferencia entre el avance esperado y el avance real a partir de la cual el taller se considerará atrasado.
                  </Typography>
                  <TextField
                    id="umbral-atraso"
                    fullWidth type="number" size="small" required
                    value={umbralAtraso} onChange={cambiarUmbralAtraso}
                    slotProps={{
                      htmlInput: { min: 1, step: 1 },
                      input: { endAdornment: <InputAdornment position="end">Semanas</InputAdornment> }
                    }}
                    error={intentoGuardar && !atrasoValido}
                    helperText={intentoGuardar && !atrasoValido
                      ? 'Debe ser un entero mayor que el umbral de próximo atraso.'
                      : undefined}
                  />
                </Box>
              </Box>

              <Box sx={{ display: 'flex', flexDirection: 'column', gap: '40px', minWidth: 0 }}>
                <Typography component="h3" variant="subtitle1" sx={{ fontWeight: 600 }}>
                  Seguimiento semanal
                </Typography>
                <TextField
                  fullWidth select label="Responsable de seguimiento" size="small" required
                  value={responsableSemanal} onChange={cambiarResponsableSemanal}
                  error={intentoGuardar && !responsableSemanalValido}
                  helperText="Usuario interno que recibiría el avance de sus proyectos asignados."
                >
                  <MenuItem value="" disabled>Seleccionar usuario</MenuItem>
                  {responsables.map(function mostrarResponsable(usuario) {
                    return (
                      <MenuItem key={usuario.idUsuario} value={String(usuario.idUsuario)}>
                        {usuario.apellidos}, {usuario.nombres} ({usuario.email})
                      </MenuItem>
                    );
                  })}
                </TextField>
                <TextField fullWidth select label="Día de informe semanal" size="small" required
                  value={diaSemanal} onChange={cambiarDiaSemanal}>
                  {diasSemana.map(function mostrarDia(dia) {
                    return <MenuItem key={dia.valor} value={dia.valor}>{dia.nombre}</MenuItem>;
                  })}
                </TextField>
                <Typography variant="body2" sx={{ color: colores.textoSecundario }}>
                  El responsable recibirá un correo con el reporte del estado de los talleres
                  indicando aquellos que requieren seguimiento y un enlace para consultar el proyecto.
                </Typography>
              </Box>

              <Box sx={{ display: 'flex', flexDirection: 'column', gap: '40px', minWidth: 0 }}>
                <Typography component="h3" variant="subtitle1" sx={{ fontWeight: 600 }}>
                  Avance mensual
                </Typography>
                <TextField
                  fullWidth select label="Responsable del proyecto" size="small" required
                  value={responsableMensual} onChange={cambiarResponsableMensual}
                  error={intentoGuardar && !responsableMensualValido}
                  helperText="Usuario interno que recibiría el informe mensual del proyecto."
                >
                  <MenuItem value="" disabled>Seleccionar usuario</MenuItem>
                  {responsables.map(function mostrarResponsable(usuario) {
                    return (
                      <MenuItem key={usuario.idUsuario} value={String(usuario.idUsuario)}>
                        {usuario.apellidos}, {usuario.nombres} ({usuario.email})
                      </MenuItem>
                    );
                  })}
                </TextField>
                <TextField fullWidth select label="Fecha del informe mensual" size="small" required
                  value={tipoDiaMensual} onChange={cambiarTipoDiaMensual}>
                  <MenuItem value="ULTIMO_DIA_HABIL">Último día hábil del mes</MenuItem>
                  <MenuItem value="DIA_DEL_MES">Día específico del mes</MenuItem>
                </TextField>
                {tipoDiaMensual === 'DIA_DEL_MES' && (
                  <TextField
                    fullWidth label="Día del mes" type="number" size="small" required
                    value={diaMensual} onChange={cambiarDiaMensual}
                    slotProps={{ htmlInput: { min: 1, max: 31, step: 1 } }}
                    error={intentoGuardar && !diaMensualValido}
                    helperText={intentoGuardar && !diaMensualValido
                      ? 'Ingresá un día entre 1 y 31.'
                      : 'La fecha efectiva en meses más cortos queda fuera del alcance de este incremento.'}
                  />
                )}
                <Typography variant="body2" sx={{ color: colores.textoSecundario }}>
                  El responsable recibirá un correo con el informe de avance consolidado del
                  proyecto y un enlace para consultar el detalle.
                </Typography>
              </Box>
            </Box>

            {responsables.length === 0 && (
              <Alert severity="warning" sx={{ mb: 3 }}>
                No hay usuarios internos habilitados elegibles. Registrá un administrador o
                asigná usuarios operativos al proyecto antes de guardar.
              </Alert>
            )}

            <Box sx={{ display: 'flex', gap: 2, justifyContent: 'flex-end', flexWrap: 'wrap' }}>
              <Button variant="outlined" type="button" disabled={guardando} onClick={irAProyectos}>
                Ir a proyectos
              </Button>
              <Button variant="contained" type="submit" disabled={guardando || responsables.length === 0}>
                {guardando ? 'Guardando...' : 'Guardar configuración'}
              </Button>
            </Box>
          </Box>
        )}
      </Paper>

      <Dialog
        open={destinoPendiente !== null}
        onClose={cancelarSalida}
        aria-labelledby="confirmar-salida-seguimiento"
      >
        <DialogTitle id="confirmar-salida-seguimiento">
          ¿Descartar cambios sin guardar?
        </DialogTitle>
        <DialogContent>
          <DialogContentText>
            Los cambios realizados en el seguimiento se perderán si salís sin guardarlos.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={cancelarSalida}>Cancelar</Button>
          <Button color="error" onClick={descartarCambiosYSalir}>
            Descartar y salir
          </Button>
        </DialogActions>
      </Dialog>

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

export default SeguimientoProyectoPage;
