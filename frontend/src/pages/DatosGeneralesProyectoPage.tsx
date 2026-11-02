import { useEffect, useState } from 'react';

import ChevronRightIcon from '@mui/icons-material/ChevronRight';

import {
  Alert,
  Box,
  Breadcrumbs,
  Chip,
  Link,
  Paper,
  Step,
  StepButton,
  StepLabel,
  Stepper,
  TextField,
  Typography
} from '@mui/material';

import { obtenerPlanificacionProyecto } from '../services/planificacionApi';
import { SesionExpiradaError } from '../services/erroresApi';
import type { Proyecto } from '../services/proyectosApi';
import { colores } from '../theme';

interface DatosGeneralesProyectoPageProps {
  proyecto: Proyecto;
  onIrAPlanificacion: () => void;
  onIrASeguimiento: () => void;
  onIrADocentes: () => void;
  onVolverAlListado: () => void;
  onSesionExpirada: () => void;
}

function DatosGeneralesProyectoPage({
  proyecto,
  onIrAPlanificacion,
  onIrASeguimiento,
  onIrADocentes,
  onVolverAlListado,
  onSesionExpirada
}: DatosGeneralesProyectoPageProps) {

  // La consulta permite habilitar Seguimiento cuando la planificación
  // ya fue guardada, aunque el proyecto todavía figure como Pendiente.
  const [planificacionGuardada, setPlanificacionGuardada] = useState(false);
  const [cargandoPlanificacion, setCargandoPlanificacion] = useState(true);
  const [errorPlanificacion, setErrorPlanificacion] = useState(false);

  useEffect(function consultarPlanificacion() {
    let activo = true;

    async function cargarPlanificacion() {
      try {
        const planificacion = await obtenerPlanificacionProyecto(proyecto.idProyecto);

        if (activo) {
          setPlanificacionGuardada(planificacion !== null);
        }
      } catch (excepcion) {
        if (!activo) {
          return;
        }

        if (excepcion instanceof SesionExpiradaError) {
          onSesionExpirada();
          return;
        }

        setErrorPlanificacion(true);
      } finally {
        if (activo) {
          setCargandoPlanificacion(false);
        }
      }
    }

    void cargarPlanificacion();

    return function cancelarConsulta() {
      activo = false;
    };
  }, [proyecto.idProyecto, onSesionExpirada]);

  return (
    <Box
      component="section"
      sx={{ width: '100%', boxSizing: 'border-box', px: { xs: 2, md: 4 }, py: 2 }}
    >
      <Breadcrumbs
        separator={<ChevronRightIcon sx={{ fontSize: 16 }} />}
        sx={{ mb: 2 }}
      >
        <Link
          component="button"
          type="button"
          underline="hover"
          onClick={onVolverAlListado}
          sx={{ fontSize: 14, color: '#667085' }}
        >
          Inicio
        </Link>
        <Link
          component="button"
          type="button"
          underline="hover"
          onClick={onVolverAlListado}
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
        <Typography sx={{ fontSize: 14, color: colores.verdeOscuro, fontWeight: 500 }}>
          Datos generales
        </Typography>
      </Breadcrumbs>

      <Box sx={{ display: 'flex', alignItems: 'center', flexWrap: 'wrap', gap: 2, mb: 1.5 }}>
        <Typography component="h1" variant="h5">
          {proyecto.nombreCompleto}
        </Typography>
        <Chip
          label={proyecto.estado === 'PLANIFICADO' ? 'Planificado' : 'Pendiente'}
          sx={{
            backgroundColor: proyecto.estado === 'PLANIFICADO'
              ? colores.chipVerdeClaro : colores.chipAmarillo,
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
          activeStep={0}
          sx={{
            width: '100%',
            minWidth: 0,
            '& .MuiStep-root': { minWidth: 0, px: { xs: 0.25, md: 1 } },
            '& .MuiStepLabel-root': { flexDirection: { xs: 'column', md: 'row' } },
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
            <StepLabel>Datos generales</StepLabel>
          </Step>
          <Step>
            <StepButton onClick={onIrAPlanificacion}>Planificación</StepButton>
          </Step>
          <Step completed={proyecto.estado === 'PLANIFICADO'}>
            <StepButton
              onClick={onIrASeguimiento}
              disabled={cargandoPlanificacion || errorPlanificacion || !planificacionGuardada}
            >
              Seguimiento
            </StepButton>
          </Step>
          <Step>
            <StepButton onClick={onIrADocentes} disabled={proyecto.estado !== 'PLANIFICADO'}>
              Docentes
            </StepButton>
          </Step>
        </Stepper>
      </Paper>

      <Paper
        elevation={0}
        sx={{ px: { xs: 2, md: 4 }, py: 2, borderRadius: 1, minHeight: 400 }}
      >
        <Typography component="h2" variant="h6" sx={{ mb: 1 }}>
          Datos generales del proyecto
        </Typography>
        <Typography variant="body1" sx={{ color: colores.textoSecundario, mb: 3 }}>
          Consultá la información registrada para el proyecto.
        </Typography>

        {errorPlanificacion && (
          <Alert severity="warning" sx={{ mb: 2 }}>
            No se pudo verificar si la planificación está guardada.
            Podés acceder a ella desde el paso Planificación.
          </Alert>
        )}

        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: { xs: 'minmax(0, 1fr)', lg: '400px 400px' },
            columnGap: 3,
            rowGap: 2
          }}
        >
          <Box sx={{ width: 400, maxWidth: '100%' }}>
            <Typography component="label" htmlFor="nombreProyectoConsulta" sx={{ display: 'block', mb: 1 }}>
              Nombre del proyecto
            </Typography>
            <TextField
              id="nombreProyectoConsulta"
              size="small"
              fullWidth
              value={proyecto.nombre}
              slotProps={{ input: { readOnly: true } }}
            />
          </Box>

          <Box sx={{ width: 400, maxWidth: '100%' }}>
            <Typography component="label" htmlFor="anioProyectoConsulta" sx={{ display: 'block', mb: 1 }}>
              Año
            </Typography>
            <TextField
              id="anioProyectoConsulta"
              size="small"
              fullWidth
              value={proyecto.anio}
              slotProps={{ input: { readOnly: true } }}
            />
          </Box>

          <Box sx={{ width: 400, maxWidth: '100%' }}>
            <Typography component="label" htmlFor="empresaProyectoConsulta" sx={{ display: 'block', mb: 1 }}>
              Empresa financiadora
            </Typography>
            <TextField
              id="empresaProyectoConsulta"
              size="small"
              fullWidth
              value={proyecto.nombreEmpresa}
              slotProps={{ input: { readOnly: true } }}
            />
          </Box>

          <Box sx={{ width: 400, maxWidth: '100%' }}>
            <Typography component="label" htmlFor="descripcionProyectoConsulta" sx={{ display: 'block', mb: 1 }}>
              Descripción del proyecto
            </Typography>
            <TextField
              id="descripcionProyectoConsulta"
              size="small"
              fullWidth
              multiline
              minRows={3}
              value={proyecto.descripcion ?? ''}
              slotProps={{ input: { readOnly: true } }}
            />
          </Box>
        </Box>
      </Paper>
    </Box>
  );
}

export default DatosGeneralesProyectoPage;
