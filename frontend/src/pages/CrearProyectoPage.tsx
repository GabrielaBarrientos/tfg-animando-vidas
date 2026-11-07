
import { useEffect, useState, type FormEvent } from 'react';

import { SesionExpiradaError } from '../services/erroresApi';

import {
  Alert,
  Box,
  Breadcrumbs,
  Button,
  CircularProgress,
  FormControl,
  FormControlLabel,
  FormHelperText,
  FormLabel,
  Link,
  MenuItem,
  Paper,
  Radio,
  RadioGroup,
  Step,
  StepLabel,
  Stepper,
  TextField,
  Typography
} from '@mui/material';

import ChevronRightIcon from '@mui/icons-material/ChevronRight';

import {
  obtenerEmpresas,
  type Empresa
} from '../services/empresasApi';

import {
  ConflictoCrearProyectoError,
  crearProyecto,
  type Proyecto
} from '../services/proyectosApi';

import { colores } from '../theme';

interface CrearProyectoPageProps {
  onCancelar: () => void;
  onProyectoCreado: (proyecto: Proyecto) => void;
   onSesionExpirada: () => void;
}

// Etapas del proceso de configuración del proyecto.
const etapas = [
  'Datos generales',
  'Planificación',
  'Seguimiento',
  'Docentes'
];

function CrearProyectoPage({
  onCancelar,
  onProyectoCreado,
  onSesionExpirada
}: CrearProyectoPageProps) {

  const anioActual = new Date().getFullYear();

  // Datos del formulario.
  const [nombre, setNombre] = useState('');
  const [anio, setAnio] = useState(anioActual);
  const [descripcion, setDescripcion] = useState('');
  const [idEmpresa, setIdEmpresa] = useState(0);

  // Estados de la pantalla.
  const [empresas, setEmpresas] = useState<Empresa[]>([]);
  const [cargandoEmpresas, setCargandoEmpresas] = useState(true);
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState('');

  // Cargar las empresas financiadoras.
  useEffect(() => {
    let activo = true;

    async function cargarEmpresas() {
      try {
        const resultado = await obtenerEmpresas();

        if (activo) {
          setEmpresas(resultado);
        }
      } catch (excepcion) {

        if (!activo) {
          return;
        }

        if (excepcion instanceof SesionExpiradaError) {
          onSesionExpirada();
          return;
        }

        setError(
          'No se pudieron cargar las empresas financiadoras.'
        );
      } finally {
        if (activo) {
          setCargandoEmpresas(false);
        }
      }
    }

    void cargarEmpresas();

    return () => {
      activo = false;
    };
  }, [onSesionExpirada]);

  // Validar y registrar el proyecto.
  async function guardarProyecto(
    evento: FormEvent<HTMLFormElement>
  ) {
    evento.preventDefault();
    setError('');

    if (guardando) {
      return;
    }

    if (nombre.trim() === '') {
      setError('El nombre del proyecto es obligatorio.');
      return;
    }

    if (
      !Number.isInteger(anio) ||
      anio < anioActual ||
      anio > 32767
    ) {
      setError(
        'El año debe ser igual o posterior al actual y no superar 32767.'
      );
      return;
    }

    if (idEmpresa === 0) {
      setError(
        'Debés seleccionar una empresa financiadora.'
      );
      return;
    }

    setGuardando(true);

    try {
      const proyectoCreado = await crearProyecto({
        nombre: nombre.trim(),
        anio: anio,
        descripcion: descripcion.trim() || null,
        idEmpresa: idEmpresa
      });

      onProyectoCreado(proyectoCreado);

    } catch (excepcion) {

      if (excepcion instanceof SesionExpiradaError) {
        onSesionExpirada();
        return;
      }

      if (excepcion instanceof ConflictoCrearProyectoError) {
        setError(excepcion.message);
      } else {
        setError('No se pudo registrar el proyecto.');
      }

    } finally {
      setGuardando(false);
    }
  }

  return (
    <Box
      component="section"
      sx={{
        width: '100%',
        boxSizing: 'border-box',
        px: { xs: 2, md: 4 },
        pt: 2,
        pb: 0,
        minHeight: 'calc(100dvh - 48px)',
        height: 'calc(100dvh - 48px)',
        display: 'flex',
        flexDirection: 'column'
      }}
    >

      {/* Breadcrumb */}
      <Breadcrumbs
        separator={
          <ChevronRightIcon sx={{ fontSize: 16 }} />
        }
        sx={{ mb: 2 }}
      >
        <Link
          component="button"
          type="button"
          underline="hover"
          onClick={onCancelar}
          sx={{
            fontSize: 14,
            color: '#667085'
          }}
        >
          Inicio
        </Link>

        <Link
          component="button"
          type="button"
          underline="hover"
          onClick={onCancelar}
          sx={{
            fontSize: 14,
            color: '#667085'
          }}
        >
          Proyectos
        </Link>

        <Typography
          sx={{
            fontSize: 14,
            color: colores.verdeOscuro
          }}
        >
          Nuevo proyecto
        </Typography>
      </Breadcrumbs>

      {/* Título */}
      <Typography
        component="h1"
        variant="h5"
        sx={{ mb: 1.5 }}
      >
        Nuevo proyecto
      </Typography>

      {/* Etapas del proceso */}
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
          activeStep={0}
          sx={{
              width: '100%',
              minWidth: 720,

              '& .MuiStepIcon-root': {
                fontSize: 20
              },

              '& .MuiStepIcon-root.Mui-active': {
                color: colores.rosa
              },

              '& .MuiStepLabel-label': {
                fontSize: 13
              },

              '& .MuiStepConnector-line': {
                borderColor: '#DEDEDE'
              }
          }}
        >
          {etapas.map((etapa) => (
            <Step key={etapa}>
              <StepLabel
                sx={{
                  '& .MuiStepLabel-label': {
                    fontSize: 14
                  }
                }}
              >
                {etapa}
              </StepLabel>
            </Step>
          ))}
        </Stepper>
      </Paper>

      {/* Tarjeta del formulario */}
      <Paper
        elevation={0}
        sx={{
          px: 4,
          py: 2,
          flexGrow: 1,
          borderRadius: 1
        }}
      >

        {/* Encabezado de la tarjeta */}
        <Box sx={{ mb: 3 }}>
          <Typography
            component="h2"
            variant="h6"
          >
            Datos generales del proyecto
          </Typography>

          <Typography
            variant="body1"
            sx={{
              color: colores.textoSecundario
          }}
          >
            Completá los datos generales del proyecto
          </Typography>
        </Box>

        {/* Formulario */}
        <Box
          component="form"
          onSubmit={guardarProyecto}
        >

          {/* Distribución en dos columnas */}
          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: {
                xs: '1fr',
                md: 'minmax(0, 3fr) minmax(210px, 1fr)'
              },
              columnGap: { md: 8 },
              rowGap: 3
            }}
          >

            {/* Columna izquierda */}
            <Box
              sx={{
                display: 'flex',
                flexDirection: 'column',
                gap: 2
              }}
            >

              {/* Nombre */}
              <Box>
                <Box
                  component="label"
                  htmlFor="nombre"
                  sx={{
                    display: 'block',
                    fontSize: 16,
                    mb: 1,
                    color: colores.textoPrimario
                  }}
                >
                  Nombre del proyecto *
                </Box>

                <TextField
                  id="nombre"
                  size="small"
                  fullWidth
                  required
                  placeholder="Nombre del proyecto"
                  value={nombre}
                  onChange={(evento) => {
                    setNombre(evento.target.value);
                  }}
                  slotProps={{
                    htmlInput: {
                      maxLength: 200
                    }
                  }}
                />
              </Box>

              {/* Empresa financiadora */}
              <Box>
                <Box
                  component="label"
                  htmlFor="empresa"
                  sx={{
                    display: 'block',
                    fontSize: 16,
                    mb: 1,
                    color: colores.textoPrimario
                  }}
                >
                  Empresa financiadora *
                </Box>


                <TextField
                  id="empresa"
                  select
                  size="small"
                  fullWidth
                  required
                  value={idEmpresa}
                  disabled={cargandoEmpresas}
                  onChange={(evento) => {
                    setIdEmpresa(Number(evento.target.value));
                  }}
                  sx={{
                    '& .MuiSelect-select': {
                      color: idEmpresa === 0
                        ? colores.textoSecundario
                        : colores.textoPrimario
                    }
                  }}
                >
                  <MenuItem value={0}>
                    Seleccionar empresa
                  </MenuItem>

                  {empresas.map((empresa) => (
                    <MenuItem
                      key={empresa.idEmpresa}
                      value={empresa.idEmpresa}
                    >
                      {empresa.nombre}
                    </MenuItem>
                  ))}
                </TextField>

              </Box>

              {/* Descripción */}
              <Box>
                <Box
                  component="label"
                  htmlFor="descripcion"
                  sx={{
                    display: 'block',
                    fontSize: 16,
                    mb: 1,
                    color: colores.textoPrimario
                  }}
                >
                  Descripción del proyecto
                </Box>

                <TextField
                  id="descripcion"
                  fullWidth
                  multiline
                  rows={2}
                  label="Escribí un texto"
                  value={descripcion}
                  onChange={(evento) => {
                    setDescripcion(evento.target.value);
                  }}
                />
              </Box>

              {/* Información sobre inmutabilidad */}
              <Typography
                variant="caption"
                sx={{
                  color: colores.textoSecundario
                }}
              >
                Los datos generales no podrán modificarse
                después de registrar el proyecto.
              </Typography>

            </Box>

            {/* Columna derecha */}
            <Box
              sx={{
                display: 'flex',
                flexDirection: 'column',
                gap: 4,
                pt: { xs: 0, md: 4 }
              }}
            >

              {/* Año */}
              <TextField
                id="anio"
                size="small"
                fullWidth
                required
                type="number"
                label="Año"
                value={anio}
                onChange={(evento) => {
                  setAnio(
                    Number(evento.target.value)
                  );
                }}
                slotProps={{
                  htmlInput: {
                    min: anioActual,
                    max: 32767,
                    step: 1
                  }
                }}
              />

              {/* Tipo de proyecto */}
              <FormControl>
                <FormLabel
                  sx={{
                    color: colores.textoPrimario,
                    '&.Mui-focused': {
                      color: colores.textoPrimario
                    }
                  }}
                >
                  Tipo de proyecto *
                </FormLabel>

                <RadioGroup
                  value="plataforma"
                  aria-label="Tipo de proyecto"
                >
                  <FormControlLabel
                    value="plataforma"
                    control={<Radio />}
                    label="Talleres por plataforma"
                  />

                  <FormControlLabel
                    value="otros"
                    control={<Radio />}
                    label="Otros"
                    disabled
                  />
                </RadioGroup>

                <FormHelperText>
                  Talleres de Dibujo, Historieta y Animación.
                </FormHelperText>
              </FormControl>

            </Box>

          </Box>

          {/* Mensaje de error */}
          {error !== '' && (
            <Alert
              severity="error"
              sx={{ mt: 3 }}
            >
              {error}
            </Alert>
          )}

          {/* Botones */}
          <Box
            sx={{
              display: 'flex',
              justifyContent: 'flex-end',
              flexWrap: 'wrap',
              gap: 2,
              mt: 2
            }}
          >
            <Button
              type="button"
              variant="text"
              onClick={onCancelar}
              disabled={guardando}
              sx={{
                minWidth: 107,
                height: 36,
                color: colores.verdeOscuro
              }}
            >
              Cancelar
            </Button>

            <Button
              type="submit"
              variant="contained"
              disabled={
                guardando ||
                cargandoEmpresas ||
                empresas.length === 0
              }
              sx={{
                minWidth: 200,
                height: 36
              }}
            >
              {guardando ? (
                <>
                  <CircularProgress
                    size={16}
                    color="inherit"
                    sx={{ mr: 1 }}
                  />
                  Guardando...
                </>
              ) : (
                'Crear proyecto'
              )}
            </Button>
          </Box>

        </Box>
      </Paper>
    </Box>
  );
}

export default CrearProyectoPage;
