import { useEffect, useState } from 'react';
import {
  Alert, Accordion, AccordionDetails, AccordionSummary, Box, Button,
  CircularProgress, Paper, Snackbar, Tooltip, Typography
} from '@mui/material';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import { colores } from '../theme';
import { consultarPrograma, actualizarPrograma, type ProgramaProyecto as ProgramaDatos } from '../services/programaApi';
import { SesionExpiradaError } from '../services/erroresApi';

interface ProgramaProyectoProps {
  idProyecto: number;
  puedeActualizar: boolean;
  onSesionExpirada: () => void;
}

function fechaLocal(fecha: string | null): string {
  if (!fecha) return 'Sin sincronizar';
  const instante = new Date(fecha);
  if (Number.isNaN(instante.getTime())) return 'Sin sincronizar';
  return new Intl.DateTimeFormat('es-AR', {
    timeZone: 'America/Argentina/Buenos_Aires', year: 'numeric', month: '2-digit',
    day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit', hourCycle: 'h23'
  }).format(instante);
}

export default function ProgramaProyecto({ idProyecto, puedeActualizar, onSesionExpirada }: ProgramaProyectoProps) {
  const [programa, setPrograma] = useState<ProgramaDatos | null>(null);
  const [abiertas, setAbiertas] = useState<Record<number, boolean>>({});
  const [cargando, setCargando] = useState(true);
  const [actualizando, setActualizando] = useState(false);
  const [error, setError] = useState('');
  const [exito, setExito] = useState(false);

  useEffect(function cargarPrograma() {
    let activo = true;
    async function ejecutar() {
      try {
        const datos = await consultarPrograma(idProyecto);
        if (!activo) return;
        setPrograma(datos);
        const iniciales: Record<number, boolean> = {};
        if (datos.clases.length > 0) iniciales[datos.clases[0].idPrograma] = true;
        setAbiertas(iniciales);
      } catch (excepcion) {
        if (!activo) return;
        if (excepcion instanceof SesionExpiradaError) onSesionExpirada();
        else setError(excepcion instanceof Error ? excepcion.message : 'Error al cargar el programa.');
      } finally {
        if (activo) setCargando(false);
      }
    }
    void ejecutar();
    return function cancelar() { activo = false; };
  }, [idProyecto, onSesionExpirada]);

  async function actualizar(): Promise<void> {
    if (actualizando) return;
    setActualizando(true);
    setError('');
    try {
      const datos = await actualizarPrograma(idProyecto);
      setPrograma(datos);
      setExito(true);
    } catch (excepcion) {
      if (excepcion instanceof SesionExpiradaError) onSesionExpirada();
      else setError(excepcion instanceof Error ? excepcion.message : 'No se pudo sincronizar el programa.');
    } finally {
      setActualizando(false);
    }
  }

  function estanTodasAbiertas(): boolean {
    if (programa === null || programa.clases.length === 0) return false;
    for (const clase of programa.clases) {
      if (!abiertas[clase.idPrograma]) return false;
    }
    return true;
  }

  function expandirOContraerTodas(): void {
    if (programa === null) return;
    const abrir = !estanTodasAbiertas();
    const siguiente: Record<number, boolean> = {};
    for (const clase of programa.clases) siguiente[clase.idPrograma] = abrir;
    setAbiertas(siguiente);
  }

  function alternar(idPrograma: number): void {
    setAbiertas(function actualizarAbiertas(actual) {
      return { ...actual, [idPrograma]: !actual[idPrograma] };
    });
  }

  function cerrarExito(): void { setExito(false); }

  return (
    <Paper elevation={1} sx={{ borderRadius: 1, minWidth: 0, overflow: 'hidden' }}>
      <Box sx={{ px: 2, py: 2.25, display: 'flex', alignItems: 'center', gap: 2, flexWrap: 'wrap' }}>
        <Box sx={{ flex: '1 1 320px', minWidth: 0 }}>
          <Typography variant="h6" component="h2" sx={{ fontSize: 20 }}>Programa del proyecto</Typography>
          <Typography variant="body2" color="text.secondary">
            Clases y preguntas comunes a todos los talleres del proyecto
          </Typography>
        </Box>
        <Box sx={{ display: 'flex', flexDirection: 'column', mr: 1, flexShrink: 0 }}>
          <Typography variant="caption" color="text.secondary">Última sincronización</Typography>
          <Typography variant="body2">{fechaLocal(programa?.ultimaSincronizacion ?? null)}</Typography>
        </Box>
        {puedeActualizar && (
          <Button variant="outlined" onClick={actualizar} disabled={actualizando || cargando}>
            {actualizando ? 'ACTUALIZANDO...' : 'ACTUALIZAR PROGRAMA'}
          </Button>
        )}
        <Tooltip title="La configuración del esquema de análisis se implementará en la Etapa 3">
          <span><Button variant="outlined" disabled>CREAR ESQUEMA</Button></span>
        </Tooltip>
        <Button variant="text" onClick={expandirOContraerTodas} disabled={!programa || programa.clases.length === 0}
          sx={{ color: '#257991', '&:not(.Mui-disabled)': { color: '#257991' } }}>
          {estanTodasAbiertas() ? 'COLAPSAR TODAS' : 'VER PREGUNTAS'}
        </Button>
      </Box>
      {error !== '' && <Alert severity="error" sx={{ mx: 2, mb: 2 }}>{error}</Alert>}
      {cargando ? <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}><CircularProgress /></Box> : (
        <>
          <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', px: 2, py: 1,
            borderBottom: '1px solid #DCE3E9' }}>
            <Typography variant="body2" sx={{ fontWeight: 500 }}>Clase</Typography>
            <Typography variant="body2" sx={{ fontWeight: 500 }}>Módulo</Typography>
          </Box>
          {programa !== null && programa.clases.length === 0 && (
            <Typography sx={{ p: 3 }} color="text.secondary">No se encontraron clases del programa.</Typography>
          )}
          {programa !== null && programa.clases.map(function mostrarClase(clase) {
            return (
              <Accordion key={clase.idPrograma} expanded={Boolean(abiertas[clase.idPrograma])}
                onChange={function cambiar() { alternar(clase.idPrograma); }}
                disableGutters elevation={0} sx={{ borderBottom: '1px solid #DCE3E9',
                  '&:before': { display: 'none' } }}>
                <AccordionSummary expandIcon={<ExpandMoreIcon />} sx={{ minHeight: 48,
                  '& .MuiAccordionSummary-content': { my: 1 } }}>
                  <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', width: '100%', gap: 2 }}>
                    <Typography sx={{ fontSize: 16 }}>{clase.nombreClase}</Typography>
                    <Typography sx={{ fontSize: 16, color: colores.textoSecundario }}>
                      {clase.nombreModulo}
                    </Typography>
                  </Box>
                </AccordionSummary>
                <AccordionDetails sx={{ px: 2, py: 1 }}>
                  <Box sx={{ bgcolor: '#F4F4F4', border: '1px dashed #DCE3E9', borderRadius: 1, p: 1 }}>
                    {clase.preguntas.length === 0 && (
                      <Typography variant="body2" color="text.secondary">Esta clase no tiene preguntas.</Typography>
                    )}
                    {clase.preguntas.map(function mostrarPregunta(pregunta) {
                      return <Typography key={pregunta.idPreguntaProyecto} variant="body2"
                        sx={{ fontSize: 13, color: colores.textoSecundario, mb: 0.5 }}>
                        {pregunta.enunciado}
                      </Typography>;
                    })}
                  </Box>
                </AccordionDetails>
              </Accordion>
            );
          })}
        </>
      )}
      <Snackbar open={exito} autoHideDuration={5000} onClose={cerrarExito}
        anchorOrigin={{ vertical: 'top', horizontal: 'right' }}>
        <Alert onClose={cerrarExito} severity="success" variant="filled"
          sx={{ backgroundColor: colores.verdeToastExito, color: '#FFFFFF', boxShadow: 3 }}>
          Programa sincronizado correctamente.
        </Alert>
      </Snackbar>
    </Paper>
  );
}
