
import { useState, type ReactNode } from 'react';

import {
  Alert,
  AppBar,
  Box,
  Button,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  Toolbar,
  Tooltip,
  Typography
} from '@mui/material';

import MenuOpenIcon from '@mui/icons-material/MenuOpen';
import MenuIcon from '@mui/icons-material/Menu';
import TableRowsIcon from '@mui/icons-material/TableRows';
import BarChartIcon from '@mui/icons-material/BarChart';
import SettingsIcon from '@mui/icons-material/Settings';
import AccountCircleIcon from '@mui/icons-material/AccountCircle';
import LogoutIcon from '@mui/icons-material/Logout';

import { colores } from '../theme';
import type { UsuarioActual } from '../services/authApi';

interface AppLayoutProps {
  usuario: UsuarioActual;
  onIrAProyectos: () => void;
  onCerrarSesion: () => Promise<void>;
  children: ReactNode;
}

const anchoMenuExpandido = 220;
const anchoMenuColapsado = 72;
const altoBarraSuperior = 48;

function AppLayout({
  usuario,
  onIrAProyectos,
  onCerrarSesion,
  children
}: AppLayoutProps) {

  // Estado del menú lateral.
  const [menuExpandido, setMenuExpandido] = useState(true);
  const [cerrandoSesion, setCerrandoSesion] = useState(false);
  const [errorCierreSesion, setErrorCierreSesion] = useState('');

  // Ancho actual del menú según su estado.
  const anchoMenu = menuExpandido
    ? anchoMenuExpandido
    : anchoMenuColapsado;

  function alternarMenu() {
    setMenuExpandido(!menuExpandido);
  }

  async function solicitarCierreSesion() {
    if (cerrandoSesion) return;

    setErrorCierreSesion('');
    setCerrandoSesion(true);

    try {
      await onCerrarSesion();
    } catch {
      setErrorCierreSesion('No se pudo cerrar la sesión. Intentá nuevamente.');
      setCerrandoSesion(false);
    }
  }

  return (
    <Box
      sx={{
        minHeight: '100vh',
        backgroundColor: colores.gris
      }}
    >

      {/* Barra superior */}
      <AppBar
        position="fixed"
        elevation={0}
        sx={{
          height: altoBarraSuperior,
          backgroundColor: colores.blanco,
          borderBottom: '1px solid #E0E0E0',
          zIndex: 1201
        }}
      >
        <Toolbar
          sx={{
            minHeight: '48px !important',
            gap: 2
          }}
        >
          <Box
            sx={{
              width: 32,
              height: 32,
              borderRadius: '50%',
              backgroundColor: colores.gris
            }}
          />

          <Typography
            sx={{
              color: colores.verdeOscuro,
              fontSize: 16,
              fontWeight: 500
            }}
          >
            Animando Vidas
          </Typography>
        </Toolbar>
      </AppBar>

      {/* Menú lateral colapsable */}
      <Drawer
        variant="permanent"
        sx={{
          width: anchoMenu,
          flexShrink: 0,
          transition: 'width 200ms ease',

          '& .MuiDrawer-paper': {
            width: anchoMenu,
            top: altoBarraSuperior,
            height: `calc(100vh - ${altoBarraSuperior}px)`,
            boxSizing: 'border-box',
            overflowX: 'hidden',
            backgroundColor: colores.verdeOscuro,
            color: colores.blanco,
            borderRight: 'none',
            transition: 'width 200ms ease'
          }
        }}
      >
        <Box
          sx={{
            display: 'flex',
            flexDirection: 'column',
            height: '100%'
          }}
        >

          {/* Botón para expandir o contraer */}
          <Box
            sx={{
              display: 'flex',
              justifyContent: menuExpandido
                ? 'flex-start'
                : 'center',
              px: menuExpandido ? 3 : 2,
              pt: 4,
              pb: 2
            }}
          >
            <Tooltip
              title={menuExpandido ? 'Contraer menú' : 'Expandir menú'}
              placement="right"
            >
              <IconButton
                onClick={alternarMenu}
                aria-label={
                  menuExpandido ? 'Contraer menú' : 'Expandir menú'
                }
                aria-expanded={menuExpandido}
                sx={{
                  color: colores.blanco,
                  width: 40,
                  height: 40
                }}
              >
                {menuExpandido ? <MenuOpenIcon /> : <MenuIcon />}
              </IconButton>
            </Tooltip>
          </Box>

          {/* Opciones del menú */}
          <List sx={{ px: 2 }}>

            {/* Proyectos */}
            <Tooltip
              title={menuExpandido ? '' : 'Proyectos'}
              placement="right"
            >
              <ListItemButton
                selected
                onClick={onIrAProyectos}
                sx={{
                  minHeight: 48,
                  borderRadius: 1,
                  mb: 3,
                  px: menuExpandido ? 2 : 1,
                  justifyContent: menuExpandido
                    ? 'flex-start'
                    : 'center',

                  '&.Mui-selected': {
                    backgroundColor: colores.verdeClaro,
                    color: colores.violeta
                  },

                  '&.Mui-selected:hover': {
                    backgroundColor: colores.verdeClaro
                  }
                }}
              >
                <ListItemIcon
                  sx={{
                    minWidth: menuExpandido ? 36 : 0,
                    color: 'inherit',
                    justifyContent: 'center'
                  }}
                >
                  <TableRowsIcon />
                </ListItemIcon>

                {menuExpandido && (
                  <Typography
                    sx={{
                      fontSize: 14,
                      fontWeight: 500,
                      whiteSpace: 'nowrap'
                    }}
                  >
                    Proyectos
                  </Typography>
                )}
              </ListItemButton>
            </Tooltip>

            {/* Dashboard: navegación pendiente */}
            <Tooltip
              title={menuExpandido ? '' : 'Dashboard'}
              placement="right"
            >
              <Box
                sx={{
                  minHeight: 56,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: menuExpandido
                    ? 'flex-start'
                    : 'center',
                  gap: 2,
                  px: menuExpandido ? 2 : 1
                }}
              >
                <BarChartIcon />

                {menuExpandido && (
                  <Typography
                    sx={{
                      fontSize: 14,
                      whiteSpace: 'nowrap'
                    }}
                  >
                    Dashboard
                  </Typography>
                )}
              </Box>
            </Tooltip>

            {/* Configuración: navegación pendiente */}
            <Tooltip
              title={menuExpandido ? '' : 'Configuración'}
              placement="right"
            >
              <Box
                sx={{
                  minHeight: 56,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: menuExpandido
                    ? 'flex-start'
                    : 'center',
                  gap: 2,
                  px: menuExpandido ? 2 : 1
                }}
              >
                <SettingsIcon />

                {menuExpandido && (
                  <Typography
                    sx={{
                      fontSize: 14,
                      whiteSpace: 'nowrap'
                    }}
                  >
                    Configuración
                  </Typography>
                )}
              </Box>
            </Tooltip>

          </List>

          {/* Identificación del usuario */}
          <Box
            sx={{
              mt: 'auto',
              px: menuExpandido ? 3 : 2,
              pb: 4
            }}
          >
            {menuExpandido ? (
              <Typography
                variant="body2"
                sx={{
                  color: colores.blanco,
                  overflowWrap: 'anywhere'
                }}
              >
                {usuario.name}
              </Typography>
            ) : (
              <Tooltip
                title={usuario.name}
                placement="right"
              >
                <Box
                  sx={{
                    display: 'flex',
                    justifyContent: 'center',
                    color: colores.blanco
                  }}
                >
                  <AccountCircleIcon />
                </Box>
              </Tooltip>
            )}

            <Tooltip title={menuExpandido ? '' : 'Cerrar sesión'} placement="right">
              <Box sx={{ mt: 2 }}>
                <Button
                  type="button"
                  onClick={() => { void solicitarCierreSesion(); }}
                  disabled={cerrandoSesion}
                  aria-label="Cerrar sesión"
                  startIcon={menuExpandido ? <LogoutIcon /> : undefined}
                  sx={{
                    width: '100%',
                    minWidth: 40,
                    minHeight: 40,
                    justifyContent: menuExpandido ? 'flex-start' : 'center',
                    color: colores.blanco,
                    textTransform: 'none',
                    fontSize: 14,
                    px: menuExpandido ? 1 : 0,
                    '&:hover': { backgroundColor: 'rgba(255,255,255,0.12)' },
                    '&.Mui-disabled': { color: 'rgba(255,255,255,0.5)' }
                  }}
                >
                  {menuExpandido ? (cerrandoSesion ? 'Cerrando sesión...' : 'Cerrar sesión') : <LogoutIcon />}
                </Button>
              </Box>
            </Tooltip>
            {errorCierreSesion !== '' && (
              <Alert severity="error" role="alert" sx={{ mt: 1, fontSize: 12 }}>
                {errorCierreSesion}
              </Alert>
            )}
          </Box>

        </Box>
      </Drawer>

      {/* Área principal: se ajusta al ancho del menú */}
      <Box
        component="main"
        sx={{
          marginLeft: `${anchoMenu}px`,
          width: `calc(100% - ${anchoMenu}px)`,
          paddingTop: `${altoBarraSuperior}px`,
          minHeight: '100vh',
          boxSizing: 'border-box',
          transition: 'margin-left 200ms ease, width 200ms ease'
        }}
      >
        {children}
      </Box>

    </Box>
  );
}

export default AppLayout;
