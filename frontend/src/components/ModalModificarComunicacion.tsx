import { useState } from 'react';
import type { ChangeEvent, FormEvent } from 'react';
import ChatOutlinedIcon from '@mui/icons-material/ChatOutlined';
import {
  Alert, Box, Button, Dialog, DialogActions, DialogContent, DialogTitle,
  Divider, MenuItem, TextField, Typography
} from '@mui/material';
import {
  acciones, canales, etiquetaDe, hoyLocal, motivos, mostrarFecha, resultados,
  type ActualizarSeguimientoRequest, type ComunicacionTaller,
  type ResultadoComunicacion
} from '../services/comunicacionesApi';
import { SesionExpiradaError } from '../services/erroresApi';

interface Props {
  comunicacion: ComunicacionTaller;
  onCancelar: () => void;
  onGuardar: (datos: ActualizarSeguimientoRequest) => Promise<void>;
  onSesionExpirada: () => void;
}

function Dato({ etiqueta, valor }: { etiqueta: string; valor: string }) {
  return (
    <Box>
      <Typography variant="caption" color="text.secondary">{etiqueta}</Typography>
      <Typography variant="body2" sx={{ fontWeight: 600 }}>{valor}</Typography>
    </Box>
  );
}

export default function ModalModificarComunicacion({ comunicacion, onCancelar, onGuardar,
  onSesionExpirada }: Props) {
  const [resultado, setResultado] = useState<ResultadoComunicacion>(comunicacion.resultado);
  const [fechaRespuesta, setFechaRespuesta] = useState(comunicacion.fechaRespuestaDocente || '');
  const [respuesta, setRespuesta] = useState(comunicacion.respuestaDocente || '');
  const [fechaCompromiso, setFechaCompromiso] = useState(comunicacion.fechaCompromiso || '');
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState('');
  const hoy = hoyLocal();

  function cambiarResultado(evento: ChangeEvent<HTMLInputElement>): void {
    const siguiente = evento.target.value as ResultadoComunicacion;
    setResultado(siguiente);
    if (siguiente !== 'COMPROMISO_DE_REGULARIZACION') setFechaCompromiso('');
  }

  async function guardar(evento: FormEvent<HTMLFormElement>): Promise<void> {
    evento.preventDefault();
    if (guardando) return;
    setError('');
    const tieneRespuesta = respuesta.trim() !== '';
    if (tieneRespuesta !== (fechaRespuesta !== '')) {
      setError('Completá conjuntamente la respuesta del docente y su fecha.');
      return;
    }
    if (fechaRespuesta && (fechaRespuesta < comunicacion.fechaContacto || fechaRespuesta > hoy)) {
      setError('La fecha de respuesta debe estar entre el contacto y la fecha actual.');
      return;
    }
    if ((resultado === 'ESPERANDO_RESPUESTA' || resultado === 'NO_SE_PUDO_CONTACTAR') && tieneRespuesta) {
      setError('El resultado seleccionado no admite una respuesta del docente.');
      return;
    }
    if (fechaCompromiso && (fechaCompromiso < comunicacion.fechaContacto
      || (fechaRespuesta !== '' && fechaCompromiso < fechaRespuesta))) {
      setError('La fecha de compromiso no puede ser anterior al contacto ni a la respuesta.');
      return;
    }
    setGuardando(true);
    try {
      await onGuardar({ resultado, fechaRespuestaDocente: fechaRespuesta || null,
        respuestaDocente: respuesta.trim() || null, fechaCompromiso: fechaCompromiso || null });
    } catch (excepcion) {
      if (excepcion instanceof SesionExpiradaError) onSesionExpirada();
      else setError(excepcion instanceof Error ? excepcion.message : 'No se pudo guardar el seguimiento.');
    } finally {
      setGuardando(false);
    }
  }

  return (
    <Dialog open maxWidth="sm" fullWidth onClose={guardando ? undefined : onCancelar}
      aria-labelledby="titulo-modificar-comunicacion">
      <Box component="form" onSubmit={guardar}>
        <DialogTitle id="titulo-modificar-comunicacion" sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <ChatOutlinedIcon fontSize="small" /> Modificar comunicación
        </DialogTitle>
        <DialogContent dividers sx={{ display: 'flex', flexDirection: 'column', gap: 2.5 }}>
          {error !== '' && <Alert severity="error">{error}</Alert>}
          <Typography variant="overline" color="text.secondary">Contacto original</Typography>
          <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 2 }}>
            <Dato etiqueta="Fecha del contacto" valor={mostrarFecha(comunicacion.fechaContacto)} />
            <Dato etiqueta="Canal" valor={comunicacion.canal === 'OTRO' ? comunicacion.canalOtro || 'Otro'
              : etiquetaDe(canales, comunicacion.canal)} />
            <Dato etiqueta="Motivo" valor={comunicacion.motivo === 'OTRO'
              ? comunicacion.motivoOtro || 'Otro' : etiquetaDe(motivos, comunicacion.motivo)} />
            <Dato etiqueta="Registrado por" valor={comunicacion.registradoPor} />
            <Dato etiqueta="Acción realizada" valor={etiquetaDe(acciones, comunicacion.accionRealizada)} />
          </Box>
          <Divider />
          <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>Actualizar seguimiento</Typography>
          <TextField select label="Resultado" required fullWidth value={resultado} onChange={cambiarResultado}>
            {resultados.map(opcion => <MenuItem key={opcion.valor} value={opcion.valor}>{opcion.etiqueta}</MenuItem>)}
          </TextField>
          <TextField label="Fecha de respuesta del docente" type="date" fullWidth
            value={fechaRespuesta} onChange={evento => setFechaRespuesta(evento.target.value)}
            slotProps={{ inputLabel: { shrink: true }, htmlInput: { min: comunicacion.fechaContacto, max: hoy } }} />
          <TextField label="Respuesta del docente" fullWidth multiline minRows={3}
            value={respuesta} onChange={evento => setRespuesta(evento.target.value)}
            slotProps={{ htmlInput: { maxLength: 5000 } }} />
          <TextField label="Fecha de compromiso (opcional)" type="date" fullWidth
            disabled={resultado !== 'COMPROMISO_DE_REGULARIZACION'} value={fechaCompromiso}
            onChange={evento => setFechaCompromiso(evento.target.value)}
            slotProps={{ inputLabel: { shrink: true },
              htmlInput: { min: fechaRespuesta || comunicacion.fechaContacto } }} />
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={onCancelar} disabled={guardando} variant="outlined">Cancelar</Button>
          <Button type="submit" variant="contained" disabled={guardando}>
            {guardando ? 'Guardando...' : 'Guardar cambios'}
          </Button>
        </DialogActions>
      </Box>
    </Dialog>
  );
}
