import { Box, IconButton, Paper, Tooltip, Typography } from '@mui/material';
import BookmarkBorderOutlinedIcon from '@mui/icons-material/BookmarkBorderOutlined';
import BookmarkIcon from '@mui/icons-material/Bookmark';
import FavoriteBorderIcon from '@mui/icons-material/FavoriteBorder';
import FavoriteIcon from '@mui/icons-material/Favorite';
import DownloadOutlinedIcon from '@mui/icons-material/DownloadOutlined';
import type { EvidenciaTaller } from '../services/evidenciasApi';
import { urlDescargaEvidencia } from '../services/evidenciasApi';
import MiniaturaEvidencia from './MiniaturaEvidencia';

interface TarjetaEvidenciaProps {
  archivo: EvidenciaTaller;
  idProyecto: number;
  idTaller: string;
  puedeEditar: boolean;
  bloqueado: boolean;
  onAbrir: (archivo: EvidenciaTaller) => void;
  onMarcar: (id: number, util: boolean, destacada: boolean) => Promise<void>;
}

export default function TarjetaEvidencia({ archivo, idProyecto, idTaller, puedeEditar, bloqueado,
  onAbrir, onMarcar }: TarjetaEvidenciaProps) {
  function abrir(): void {
    onAbrir(archivo);
  }

  function marcarUtil(): void {
    void onMarcar(archivo.idEvidencia, !archivo.esUtil, archivo.esDestacada);
  }

  function marcarDestacada(): void {
    void onMarcar(archivo.idEvidencia, archivo.esUtil, !archivo.esDestacada);
  }

  return (
    <Paper variant="outlined" sx={{ minWidth: 0, borderRadius: 1, overflow: 'hidden' }}>
      <Box component="button" type="button" onClick={abrir}
        aria-label={`Visualizar ${archivo.nombreFisico}`}
        sx={{ display: 'block', p: 0, border: 0, cursor: 'pointer', width: '100%',
          bgcolor: '#F2F4F7', aspectRatio: '16/9', overflow: 'hidden' }}>
        <MiniaturaEvidencia archivo={archivo} idProyecto={idProyecto} idTaller={idTaller} />
      </Box>
      <Box sx={{ p: 1, display: 'flex', alignItems: 'center', gap: 0.5, flexWrap: 'wrap' }}>
        <Typography title={archivo.nombreFisico} variant="body2"
          sx={{ flex: 1, minWidth: 0, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          {archivo.nombreFisico}
        </Typography>
        <Tooltip title={archivo.esUtil ? 'Evidencia útil' : 'Marcar como útil'}>
          <span>
            <IconButton size="small" aria-label="Marcar evidencia útil"
              disabled={!puedeEditar || bloqueado} onClick={marcarUtil}
              sx={{ color: archivo.esUtil ? '#D8A600' : '#98A2B3',
                '&.Mui-disabled': { color: archivo.esUtil ? '#D8A600' : '#98A2B3' } }}>
              {archivo.esUtil ? <BookmarkIcon /> : <BookmarkBorderOutlinedIcon />}
            </IconButton>
          </span>
        </Tooltip>
        <Tooltip title={archivo.esDestacada ? 'Evidencia destacada' : 'Marcar como destacada'}>
          <span>
            <IconButton size="small" aria-label="Marcar evidencia destacada"
              disabled={!puedeEditar || bloqueado} onClick={marcarDestacada}
              sx={{ color: archivo.esDestacada ? '#ED4D6E' : '#98A2B3',
                '&.Mui-disabled': { color: archivo.esDestacada ? '#ED4D6E' : '#98A2B3' } }}>
              {archivo.esDestacada ? <FavoriteIcon /> : <FavoriteBorderIcon />}
            </IconButton>
          </span>
        </Tooltip>
        {puedeEditar && (
          <Tooltip title="Descargar archivo individual">
            <IconButton size="small" component="a" download
              href={urlDescargaEvidencia(idProyecto, idTaller, archivo.idEvidencia)}
              aria-label={`Descargar ${archivo.nombreFisico}`}>
              <DownloadOutlinedIcon />
            </IconButton>
          </Tooltip>
        )}
      </Box>
    </Paper>
  );
}
