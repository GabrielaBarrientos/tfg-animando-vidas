import { useState } from 'react';
import { Box, CircularProgress, Typography } from '@mui/material';
import ImageNotSupportedOutlinedIcon from '@mui/icons-material/ImageNotSupportedOutlined';
import type { EvidenciaTaller } from '../services/evidenciasApi';
import { urlVisualizacionEvidencia } from '../services/evidenciasApi';

interface MiniaturaEvidenciaProps {
  archivo: EvidenciaTaller;
  idProyecto: number;
  idTaller: string;
}

type EstadoMiniatura = 'cargando' | 'lista' | 'error';

/** La miniatura solicita el binario solo cuando el navegador la necesita. */
export default function MiniaturaEvidencia({ archivo, idProyecto, idTaller }: MiniaturaEvidenciaProps) {
  const [estado, setEstado] = useState<EstadoMiniatura>('cargando');
  const url = urlVisualizacionEvidencia(idProyecto, idTaller, archivo.idEvidencia);
  const esVideo = archivo.mimeType.startsWith('video/');

  function confirmarCarga(): void {
    setEstado('lista');
  }

  function registrarError(): void {
    setEstado('error');
  }

  return (
    <Box sx={{ position: 'relative', height: '100%', width: '100%', bgcolor: '#F2F4F7' }}>
      {estado === 'cargando' && (
        <Box role="status" aria-label="Cargando miniatura"
          sx={{ position: 'absolute', inset: 0, display: 'flex', alignItems: 'center',
            justifyContent: 'center', gap: 1 }}>
          <CircularProgress size={24} />
        </Box>
      )}
      {estado === 'error' && (
        <Box role="status" sx={{ position: 'absolute', inset: 0, display: 'flex',
          alignItems: 'center', justifyContent: 'center', flexDirection: 'column', gap: 0.5,
          color: '#667085', p: 1 }}>
          <ImageNotSupportedOutlinedIcon />
          <Typography variant="caption" color="text.secondary" sx={{ textAlign: 'center' }}>
            No se pudo cargar la vista previa
          </Typography>
        </Box>
      )}
      {esVideo ? (
        <Box component="video" src={url} muted preload="metadata" playsInline
          onLoadedMetadata={confirmarCarga} onError={registrarError}
          sx={{ display: estado === 'error' ? 'none' : 'block', width: '100%', height: '100%',
            objectFit: 'contain', opacity: estado === 'lista' ? 1 : 0 }} />
      ) : (
        <Box component="img" src={url} loading="lazy" decoding="async"
          alt={archivo.nombreFisico} onLoad={confirmarCarga} onError={registrarError}
          sx={{ display: estado === 'error' ? 'none' : 'block', width: '100%', height: '100%',
            objectFit: 'contain', opacity: estado === 'lista' ? 1 : 0 }} />
      )}
    </Box>
  );
}
