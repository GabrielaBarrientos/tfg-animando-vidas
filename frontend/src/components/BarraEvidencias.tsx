import type { ChangeEvent, ReactElement } from 'react';
import { Box, Button, MenuItem, TextField, Tooltip, Typography } from '@mui/material';
import type { ClaseDetalle } from '../services/detalleTallerApi';

interface BarraEvidenciasProps {
  clases: ClaseDetalle[];
  filtro: string;
  ultimaSincronizacion: string | null;
  puedeEditar: boolean;
  actualizando: boolean;
  onCambiarFiltro: (valor: string) => void;
  onActualizar: () => void;
}

function mostrarFechaHora(valor: string | null): string {
  if (valor === null || valor === '') {
    return 'Sin sincronizar';
  }
  const fecha = new Date(valor);
  if (Number.isNaN(fecha.getTime())) {
    return 'Sin sincronizar';
  }
  return new Intl.DateTimeFormat('es-AR', {
    timeZone: 'America/Argentina/Buenos_Aires',
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit', hourCycle: 'h23'
  }).format(fecha);
}

export default function BarraEvidencias({ clases, filtro, ultimaSincronizacion, puedeEditar,
  actualizando, onCambiarFiltro, onActualizar }: BarraEvidenciasProps) {
  function cambiarFiltro(evento: ChangeEvent<HTMLInputElement>): void {
    onCambiarFiltro(evento.target.value);
  }

  function limpiarFiltro(): void {
    onCambiarFiltro('todas');
  }

  const opciones: ReactElement[] = [
    <MenuItem key="todas" value="todas">Todas las clases</MenuItem>
  ];
  for (const clase of clases) {
    opciones.push(
      <MenuItem key={clase.numeroClase} value={String(clase.numeroClase)}>
        {clase.nombreClase || `Clase ${clase.numeroClase}`}
      </MenuItem>
    );
  }

  return (
    <Box sx={{ display: 'flex', gap: 2, alignItems: 'center', flexWrap: 'wrap', mb: 2 }}>
      <TextField select size="small" label="Clase" value={filtro} onChange={cambiarFiltro}
        sx={{ width: '500px', maxWidth: '100%', flexShrink: 0 }}>
        {opciones}
      </TextField>
      <Button variant="text" color="secondary" onClick={limpiarFiltro}
        disabled={filtro === 'todas'}
        sx={{ flexShrink: 0, '&:not(.Mui-disabled)': { color: '#257991' } }}>
        LIMPIAR FILTRO
      </Button>
      <Box sx={{ display: 'flex', gap: 2, alignItems: 'center', justifyContent: 'flex-end',
        flexWrap: 'wrap', ml: 'auto' }}>
        <Box sx={{ display: 'flex', flexDirection: 'column', flexShrink: 0 }}>
          <Typography variant="caption" color="text.secondary" sx={{ whiteSpace: 'nowrap' }}>
            Última sincronización:
          </Typography>
          <Typography variant="body2" sx={{ whiteSpace: 'nowrap' }}>
            {mostrarFechaHora(ultimaSincronizacion)}
          </Typography>
        </Box>
        {puedeEditar && (
          <Button variant="outlined" disabled={actualizando} onClick={onActualizar}>
            {actualizando ? 'Actualizando...' : 'ACTUALIZAR EVIDENCIAS'}
          </Button>
        )}
        <Tooltip title="Disponible en el incremento de comunicaciones">
          <span><Button variant="outlined" disabled>REGISTRAR COMUNICACIÓN</Button></span>
        </Tooltip>
      </Box>
    </Box>
  );
}
