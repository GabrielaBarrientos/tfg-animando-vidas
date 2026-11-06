import { useState } from 'react';
import type { ChangeEvent, FormEvent } from 'react';
import {
  Alert, Box, Button, Dialog, DialogActions, DialogContent, DialogTitle,
  FormControl, FormControlLabel, FormLabel, MenuItem, Radio, RadioGroup, TextField
} from '@mui/material';
import {
  acciones, canales, hoyLocal, motivos, resultados,
  type AccionComunicacion, type CanalComunicacion, type CrearComunicacionRequest,
  type MotivoComunicacion, type ResultadoComunicacion
} from '../services/comunicacionesApi';
import { SesionExpiradaError } from '../services/erroresApi';

interface Props {
  onCancelar: () => void;
  onGuardar: (datos: CrearComunicacionRequest) => Promise<void>;
  onSesionExpirada: () => void;
}

export default function ModalNuevaComunicacion({ onCancelar, onGuardar, onSesionExpirada }: Props) {
  const hoy = hoyLocal();
  const [fechaContacto, setFechaContacto] = useState(hoy);
  const [canal, setCanal] = useState<CanalComunicacion>('WHATSAPP');
  const [canalOtro, setCanalOtro] = useState('');
  const [motivo, setMotivo] = useState<MotivoComunicacion>('ATRASO_EN_EL_AVANCE');
  const [motivoOtro, setMotivoOtro] = useState('');
  const [accion, setAccion] = useState<AccionComunicacion>('RECORDATORIO');
  const [resultado, setResultado] = useState<ResultadoComunicacion>('ESPERANDO_RESPUESTA');
  const [fechaCompromiso, setFechaCompromiso] = useState('');
  const [observaciones, setObservaciones] = useState('');
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState('');

  function cambiarCanal(evento: ChangeEvent<HTMLInputElement>): void {
    const valor = evento.target.value as CanalComunicacion;
    setCanal(valor);
    if (valor !== 'OTRO') setCanalOtro('');
  }

  function cambiarMotivo(evento: ChangeEvent<HTMLInputElement>): void {
    const valor = evento.target.value as MotivoComunicacion;
    setMotivo(valor);
    if (valor !== 'OTRO') setMotivoOtro('');
  }

  function cambiarResultado(evento: ChangeEvent<HTMLInputElement>): void {
    const valor = evento.target.value as ResultadoComunicacion;
    setResultado(valor);
    if (valor !== 'COMPROMISO_DE_REGULARIZACION') setFechaCompromiso('');
  }

  async function guardar(evento: FormEvent<HTMLFormElement>): Promise<void> {
    evento.preventDefault();
    if (guardando) return;
    setError('');
    if (!fechaContacto || fechaContacto > hoy) {
      setError('Ingresá una fecha de contacto válida, que no sea futura.');
      return;
    }
    if (canal === 'OTRO' && canalOtro.trim() === '') {
      setError('Especificá el canal (máximo 20 caracteres).');
      return;
    }
    if (motivo === 'OTRO' && motivoOtro.trim() === '') {
      setError('Especificá el motivo (máximo 20 caracteres).');
      return;
    }
    if (fechaCompromiso && fechaCompromiso < fechaContacto) {
      setError('La fecha de compromiso no puede ser anterior al contacto.');
      return;
    }
    setGuardando(true);
    try {
      await onGuardar({ fechaContacto, canal, canalOtro: canal === 'OTRO' ? canalOtro.trim() : null,
        motivo, motivoOtro: motivo === 'OTRO' ? motivoOtro.trim() : null,
        accionRealizada: accion, resultado, fechaCompromiso: fechaCompromiso || null,
        observaciones: observaciones.trim() || null });
    } catch (excepcion) {
      if (excepcion instanceof SesionExpiradaError) onSesionExpirada();
      else setError(excepcion instanceof Error ? excepcion.message : 'No se pudo registrar.');
    } finally {
      setGuardando(false);
    }
  }

  return (
    <Dialog open onClose={guardando ? undefined : onCancelar} maxWidth="sm" fullWidth
      scroll="paper" slotProps={{ paper: { sx: { maxHeight: 'calc(100dvh - 24px)' } } }}
      aria-labelledby="titulo-nueva-comunicacion">
      <Box component="form" onSubmit={guardar}
        sx={{ display: 'flex', flexDirection: 'column', flex: '1 1 auto', minHeight: 0, overflow: 'hidden' }}>
        <DialogTitle id="titulo-nueva-comunicacion" sx={{ flexShrink: 0 }}>Registrar comunicación</DialogTitle>
        <DialogContent dividers sx={{
          display: 'flex', flexDirection: 'column', gap: 1.5, pt: 2,
          minHeight: 0, overflowY: 'auto',
          // Los campos de una línea (incluidos los combos y los campos Otro) miden 45px.
          // Observaciones conserva su altura multilínea.
          '& .MuiTextField-root .MuiOutlinedInput-root:not(.MuiInputBase-multiline)': {
            height: '45px'
          },
          '& .MuiTextField-root .MuiOutlinedInput-root:not(.MuiInputBase-multiline) .MuiOutlinedInput-input': {
            padding: '11px 14px'
          },
          '& .MuiTextField-root .MuiSelect-select': { paddingRight: '32px' },
          '& .MuiTextField-root:has(.MuiOutlinedInput-root:not(.MuiInputBase-multiline)) .MuiInputLabel-outlined:not(.MuiInputLabel-shrink)': {
            transform: 'translate(14px, 11px) scale(1)'
          }
        }}>
          {error !== '' && <Alert severity="error">{error}</Alert>}
          <TextField label="Fecha de comunicación" type="date" required fullWidth
            slotProps={{ inputLabel: { shrink: true }, htmlInput: { max: hoy } }}
            value={fechaContacto} onChange={evento => setFechaContacto(evento.target.value)} />
          <FormControl required>
            <FormLabel>Canal</FormLabel>
            <RadioGroup row value={canal} onChange={cambiarCanal}>
              {canales.map(opcion => <FormControlLabel key={opcion.valor} value={opcion.valor}
                control={<Radio size="small" />} label={opcion.etiqueta} />)}
            </RadioGroup>
          </FormControl>
          {canal === 'OTRO' && <TextField label="Especificar canal" fullWidth required
            value={canalOtro} onChange={evento => setCanalOtro(evento.target.value)}
            slotProps={{ htmlInput: { maxLength: 20 } }} helperText={`${canalOtro.length}/20 caracteres`} />}
          <TextField select label="Motivo" required fullWidth value={motivo} onChange={cambiarMotivo}>
            {motivos.map(opcion => <MenuItem key={opcion.valor} value={opcion.valor}>{opcion.etiqueta}</MenuItem>)}
          </TextField>
          {motivo === 'OTRO' && <TextField label="Especificar motivo" required fullWidth
            value={motivoOtro} onChange={evento => setMotivoOtro(evento.target.value)}
            slotProps={{ htmlInput: { maxLength: 20 } }} helperText={`${motivoOtro.length}/20 caracteres`} />}
          <TextField select label="Acción realizada" required fullWidth value={accion}
            onChange={evento => setAccion(evento.target.value as AccionComunicacion)}>
            {acciones.map(opcion => <MenuItem key={opcion.valor} value={opcion.valor}>{opcion.etiqueta}</MenuItem>)}
          </TextField>
          <TextField select label="Resultado" required fullWidth value={resultado} onChange={cambiarResultado}>
            {resultados.map(opcion => <MenuItem key={opcion.valor} value={opcion.valor}>{opcion.etiqueta}</MenuItem>)}
          </TextField>
          <TextField label="Fecha de compromiso (opcional)" type="date" fullWidth
            disabled={resultado !== 'COMPROMISO_DE_REGULARIZACION'} value={fechaCompromiso}
            onChange={evento => setFechaCompromiso(evento.target.value)}
            slotProps={{ inputLabel: { shrink: true }, htmlInput: { min: fechaContacto } }} />
          <TextField label="Observaciones" fullWidth multiline minRows={3}
            placeholder="Detalles del contacto, comentarios del docente o acuerdos..."
            value={observaciones} onChange={evento => setObservaciones(evento.target.value)}
            slotProps={{ htmlInput: { maxLength: 5000 } }} />
        </DialogContent>
        <DialogActions sx={{ p: 2, flexShrink: 0 }}>
          <Button onClick={onCancelar} disabled={guardando} variant="outlined">Cancelar</Button>
          <Button type="submit" disabled={guardando} variant="contained">
            {guardando ? 'Guardando...' : 'Guardar'}
          </Button>
        </DialogActions>
      </Box>
    </Dialog>
  );
}
