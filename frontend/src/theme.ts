
import { createTheme } from '@mui/material/styles';

// Paleta oficial de Animando Vidas.
export const colores = {
  // Identidad visual
  gris: '#F3F4F6',
  blanco: '#FFFFFF',
  violeta: '#634B66',
  rosa: '#ED4D6E',
  amarillo: '#FFCD02',
  verdeOscuro: '#257991',
  verdeClaro: '#7FD9D8',

  // Mensajes e iconos
  verdeToastExito: '#2DAA6E',
  verdeIcono: '#25C196',
  grisIcono: '#6E7174',
  rojoIcono: '#EF3323',

  // Textos
  textoPrimario: 'rgba(0, 0, 0, 0.87)',
  textoSecundario: 'rgba(0, 0, 0, 0.50)',

  // Fondos de chips
  chipAmarillo: 'rgba(255, 205, 2, 0.50)',
  chipRosa: 'rgba(237, 77, 110, 0.25)',
  chipVerdeClaro: '#7FD9D8'
};

const theme = createTheme({
  palette: {
    mode: 'light',

    primary: {
      main: colores.verdeOscuro,
      contrastText: colores.blanco
    },

    secondary: {
      main: colores.violeta
    },

    success: {
      main: colores.verdeToastExito
    },

    warning: {
      main: colores.amarillo
    },

    error: {
      main: colores.rojoIcono
    },

    background: {
      default: colores.gris,
      paper: colores.blanco
    },

    text: {
      primary: colores.textoPrimario,
      secondary: colores.textoSecundario
    }
  },

  typography: {
    fontFamily: '"Roboto", "Arial", sans-serif',

    h5: {
      fontSize: '24px',
      fontWeight: 400
    },

    h6: {
      fontSize: '20px',
      fontWeight: 400
    },

    body1: {
      fontSize: '16px'
    },

    body2: {
      fontSize: '14px'
    }
  },

  shape: {
    borderRadius: 4
  },

  components: {
    // Botones principales y secundarios:
    // utilizar color="primary" con distintas variantes.
    MuiButton: {
      styleOverrides: {
        root: {
          textTransform: 'uppercase',
          fontWeight: 500
        }
      }
    },

    // Enlaces en rosa.
    MuiLink: {
      styleOverrides: {
        root: {
          color: colores.rosa
        }
      }
    },

    // Pasos activos y completados del Stepper.
    MuiStepIcon: {
      styleOverrides: {
        root: {
          '&.Mui-active, &.Mui-completed': {
            color: colores.rosa
          }
        }
      }
    },

    // Barras de progreso en verde claro.
    MuiLinearProgress: {
      styleOverrides: {
        bar: {
          backgroundColor: colores.verdeClaro
        }
      }
    }
  }
});

export default theme;
