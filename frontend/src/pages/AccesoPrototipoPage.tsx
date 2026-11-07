import { Alert, Box, Button, CircularProgress, Typography } from '@mui/material';

import GoogleLogo from '../components/GoogleLogo';
import { colores } from '../theme';
import logotipoAnimandoVidas from '../assets/logo-animando-vidas.png';

export type EstadoAcceso = 'verificando' | 'sin-sesion' | 'sesion-expirada';

interface AccesoPrototipoPageProps {
  estado: EstadoAcceso;
}

// OAuth2 / OIDC se ejecuta en Spring Security y en Google, nunca en un formulario local.
const urlInicioGoogle = 'http://localhost:8080/oauth2/authorization/google';

function AccesoPrototipoPage({ estado }: AccesoPrototipoPageProps) {
  // El codigo de rechazo es provisto por el backend al finalizar OAuth2.
  // No se muestra información de la cuenta ni se modifica la autorización.
  const errorAcceso = new URLSearchParams(window.location.search).get('acceso');

  return (
    <Box
      component="main"
      sx={{
        minHeight: '100vh',
        display: 'flex',
        backgroundColor: colores.blanco
      }}
    >
      <Box
        sx={{
          flex: { xs: '1 1 auto', md: '0 0 39%' },
          minWidth: 0,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          px: { xs: 2, sm: 3 },
          pt: { xs: 5, sm: 8, md: '100px' },
          pb: 5,
          textAlign: 'center'
        }}
      >
        <Box
          component="img"
          src={logotipoAnimandoVidas}
          alt="Animando Vidas: niños junto al planeta"
          sx={{
            display: 'block',
            width: '100%',
            maxWidth: 386,
            height: { xs: 215, sm: 262 },
            objectFit: 'contain'
          }}
        />

        <Typography
          component="h1"
          sx={{
            color: colores.verdeOscuro,
            fontSize: 28,
            fontWeight: 600,
            lineHeight: 1.3
          }}
        >
          Animando Vidas
        </Typography>

        <Typography
          sx={{
            mt: 5,
            color: '#202124',
            fontSize: { xs: 20, sm: 24 },
            fontWeight: 400,
            lineHeight: { xs: 1.6, sm: '40px' },
            maxWidth: 420
          }}
        >
          Seguimiento de Talleres de Dibujo, Historietas y Animación.
        </Typography>

        {estado === 'sin-sesion' && errorAcceso === 'no-autorizado' && (
          <Alert severity="error" role="alert" sx={{ mt: 3, width: '100%', maxWidth: 420, textAlign: 'left' }}>
            Esta cuenta de Google no está autorizada para acceder al prototipo.
            Consultá al administrador para solicitar el acceso.
          </Alert>
        )}

        {estado === 'sin-sesion' && errorAcceso === 'error-google' && (
          <Alert severity="error" role="alert" sx={{ mt: 3, width: '100%', maxWidth: 420, textAlign: 'left' }}>
            No se pudo completar el inicio de sesión con Google. Intentá nuevamente.
          </Alert>
        )}

        {estado === 'sin-sesion' && errorAcceso === 'sesion-cerrada' && (
          <Alert severity="success" role="status" sx={{ mt: 3, width: '100%', maxWidth: 420, textAlign: 'left' }}>
            Cerraste sesión correctamente.
          </Alert>
        )}

        {estado === 'sesion-expirada' && (
          <Alert severity="warning" sx={{ mt: 3, width: '100%', maxWidth: 390, textAlign: 'left' }}>
            Tu sesión finalizó. Iniciá sesión nuevamente para continuar.
          </Alert>
        )}

        {estado === 'verificando' ? (
          <Box
            role="status"
            aria-label="Verificando sesión"
            sx={{ mt: { xs: 6, sm: 9 }, display: 'flex', alignItems: 'center', gap: 2 }}
          >
            <CircularProgress size={24} />
            <Typography color="text.secondary">Verificando sesión...</Typography>
          </Box>
        ) : (
          <Button
            component="a"
            href={urlInicioGoogle}
            startIcon={<GoogleLogo />}
            sx={{
              mt: { xs: 6, sm: estado === 'sesion-expirada' ? 4 : 11 },
              color: '#5F6368',
              textTransform: 'none',
              fontSize: 24,
              fontWeight: 400,
              lineHeight: 1.5,
              gap: 1,
              px: 2,
              py: 1,
              minHeight: 52,
              maxWidth: '100%',
              '&:hover': { backgroundColor: 'rgba(37,121,145,0.08)' },
              '&:focus-visible': {
                outline: `3px solid ${colores.verdeOscuro}`,
                outlineOffset: 3
              }
            }}
          >
            Acceder con Google
          </Button>
        )}
      </Box>

      <Box
        aria-hidden="true"
        sx={{
          display: { xs: 'none', md: 'block' },
          flex: '1 1 61%',
          bgcolor: colores.verdeClaro
        }}
      />
    </Box>
  );
}

export default AccesoPrototipoPage;
