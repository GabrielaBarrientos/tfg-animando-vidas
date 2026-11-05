import { Box, Dialog, DialogContent, DialogTitle, IconButton, Typography } from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import type { EvidenciaTaller } from '../services/evidenciasApi';
import { urlVisualizacionEvidencia } from '../services/evidenciasApi';

interface VisorEvidenciaProps {
  archivo: EvidenciaTaller | null;
  idProyecto: number;
  idTaller: string;
  onCerrar: () => void;
}

export default function VisorEvidencia({ archivo, idProyecto, idTaller, onCerrar }: VisorEvidenciaProps) {
  const url = archivo === null ? '' : urlVisualizacionEvidencia(
    idProyecto, idTaller, archivo.idEvidencia);

  return (
    <Dialog open={archivo !== null} onClose={onCerrar} maxWidth="lg" fullWidth>
      <DialogTitle sx={{ display: 'flex', justifyContent: 'space-between', gap: 2 }}>
        <Typography sx={{ overflowWrap: 'anywhere' }}>{archivo?.nombreFisico || ''}</Typography>
        <IconButton onClick={onCerrar} aria-label="Cerrar visor">
          <CloseIcon />
        </IconButton>
      </DialogTitle>
      <DialogContent sx={{ p: 2, bgcolor: '#F2F4F7', textAlign: 'center' }}>
        {archivo !== null && (archivo.mimeType.startsWith('video/') ? (
          <Box component="video" key={archivo.idEvidencia} src={url}
            controls controlsList="nodownload" autoPlay
            sx={{ maxHeight: '70vh', maxWidth: '100%' }} />
        ) : (
          <Box component="img" key={archivo.idEvidencia} src={url}
            alt={archivo.nombreFisico} sx={{ maxHeight: '70vh', maxWidth: '100%' }} />
        ))}
      </DialogContent>
    </Dialog>
  );
}
