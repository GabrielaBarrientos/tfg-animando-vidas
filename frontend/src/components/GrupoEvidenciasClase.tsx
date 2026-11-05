import type { ReactElement } from 'react';
import { Box, Button, Chip, Paper, Tooltip, Typography } from '@mui/material';
import DownloadOutlinedIcon from '@mui/icons-material/DownloadOutlined';
import ImageNotSupportedOutlinedIcon from '@mui/icons-material/ImageNotSupportedOutlined';
import type { ClaseDetalle } from '../services/detalleTallerApi';
import type { EvidenciaTaller } from '../services/evidenciasApi';
import { urlZipClase } from '../services/evidenciasApi';
import TarjetaEvidencia from './TarjetaEvidencia';
import { colores } from '../theme';

interface GrupoEvidenciasClaseProps {
  clase: ClaseDetalle;
  archivos: EvidenciaTaller[];
  idProyecto: number;
  idTaller: string;
  puedeEditar: boolean;
  bloqueado: boolean;
  onAbrir: (archivo: EvidenciaTaller) => void;
  onMarcar: (id: number, util: boolean, destacada: boolean) => Promise<void>;
}

export default function GrupoEvidenciasClase({ clase, archivos, idProyecto, idTaller,
  puedeEditar, bloqueado, onAbrir, onMarcar }: GrupoEvidenciasClaseProps) {
  let seleccionadas = 0;
  const tarjetas: ReactElement[] = [];

  for (const archivo of archivos) {
    if (archivo.esUtil || archivo.esDestacada) {
      seleccionadas++;
    }
    tarjetas.push(
      <TarjetaEvidencia key={archivo.idEvidencia} archivo={archivo} idProyecto={idProyecto}
        idTaller={idTaller} puedeEditar={puedeEditar} bloqueado={bloqueado}
        onAbrir={onAbrir} onMarcar={onMarcar} />
    );
  }

  const zipDisponible = seleccionadas > 0 && clase.idClaseTaller !== null;
  const enlaceZip = clase.idClaseTaller === null
    ? undefined : urlZipClase(idProyecto, idTaller, clase.idClaseTaller);

  return (
    <Box component="section" sx={{ mb: 3 }}>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        gap: 2, mb: 1.5, flexWrap: 'wrap' }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, flexWrap: 'wrap', minWidth: 0 }}>
          <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
            {clase.nombreClase || `Clase ${clase.numeroClase}`}
          </Typography>
          <Chip
            label={clase.estado === 'REALIZADA' ? 'Realizada' : 'Pendiente'}
            sx={{
              backgroundColor: 'rgba(255, 205, 2, 0.50)',
              color: colores.textoPrimario,
              borderRadius: '6px',
              height: 32,
              fontSize: 14,
              fontWeight: 500
            }}
          />
        </Box>
        {puedeEditar && (
          <Tooltip title={zipDisponible ? '' : 'Marcá al menos una evidencia como útil o destacada'}>
            <span>
              <Button variant="outlined" size="small" startIcon={<DownloadOutlinedIcon />}
                component="a" href={enlaceZip} disabled={!zipDisponible}>
                DESCARGAR ZIP ({seleccionadas})
              </Button>
            </span>
          </Tooltip>
        )}
      </Box>
      {archivos.length === 0 ? (
        <Paper variant="outlined" sx={{ bgcolor: '#F2F4F7', borderStyle: 'dashed', p: 3,
          display: 'flex', alignItems: 'center', gap: 1.5 }}>
          <ImageNotSupportedOutlinedIcon sx={{ color: '#98A2B3' }} />
          <Typography variant="body2" color="text.secondary">Sin evidencias cargadas</Typography>
        </Paper>
      ) : (
        <Box sx={{ display: 'grid', gridTemplateColumns: {
          xs: '1fr', sm: 'repeat(2, minmax(0, 1fr))', lg: 'repeat(3, minmax(0, 1fr))'
        }, gap: 2 }}>
          {tarjetas}
        </Box>
      )}
    </Box>
  );
}
