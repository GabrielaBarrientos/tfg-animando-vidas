
import { useCallback, useEffect, useState } from 'react';

import AppLayout from './components/AppLayout';
import ProyectosPage from './pages/ProyectosPage';
import CrearProyectoPage from './pages/CrearProyectoPage';
import PlanificacionProyectoPage from './pages/PlanificacionProyectoPage';
import SeguimientoProyectoPage from './pages/SeguimientoProyectoPage';
import DatosGeneralesProyectoPage from './pages/DatosGeneralesProyectoPage';

import { Box, Button, Paper, Typography } from '@mui/material';

import {
  obtenerUsuarioActual,
  type UsuarioActual
} from './services/authApi';

import type { Proyecto } from './services/proyectosApi';

// Pantallas disponibles en esta etapa.
type Pantalla = 'listado' | 'alta' | 'datos-generales' | 'planificacion' | 'seguimiento';

function App() {

  // Usuario autenticado mediante Google.
  const [usuario, setUsuario] = useState<UsuarioActual | null>(null);

  // Indica si se está verificando la sesión.
  const [cargandoUsuario, setCargandoUsuario] = useState(true);

  // Pantalla que se está mostrando.
  const [pantalla, setPantalla] = useState<Pantalla>('listado');

  // Proyecto que se está configurando.
  const [proyectoSeleccionado, setProyectoSeleccionado] =
    useState<Proyecto | null>(null);

  // Permite actualizar el listado después de crear un proyecto.
  const [versionListado, setVersionListado] = useState(0);

  // Indica que la sesión finalizó durante el uso de la aplicación.
  const [sesionExpirada, setSesionExpirada] = useState(false);

    // Finalizar la sesión del frontend cuando el backend informa que el usuario ya no está autenticado.
  const manejarSesionExpirada = useCallback(
    function manejarSesionExpirada() {
      setSesionExpirada(true);
      setUsuario(null);
    },
    []
  );

  // Verificar la sesión al iniciar la aplicación.
  useEffect(function verificarSesion() {

    async function cargarUsuario() {
      try {
        const usuarioActual = await obtenerUsuarioActual();
        setUsuario(usuarioActual);

      } catch {
        setUsuario(null);

      } finally {
        setCargandoUsuario(false);
      }
    }

    void cargarUsuario();

  }, []);

  // Navegar al formulario de alta.
  function mostrarAltaProyecto() {
    setProyectoSeleccionado(null);
    setPantalla('alta');
  }

  // Abrir la planificación de un proyecto.
  function mostrarPlanificacionProyecto(proyecto: Proyecto) {
    setProyectoSeleccionado(proyecto);
    setPantalla('planificacion');
  }


  function mostrarDatosGeneralesProyecto() {
    setPantalla('datos-generales');
  }

  function mostrarSeguimientoProyecto() {
    setPantalla('seguimiento');
  }

  function volverAPlanificacion() {
    setPantalla('planificacion');
  }

  function manejarConfiguracionGuardada(estadoProyecto: string) {
    // Mantener el estado persistido actualizado en todas las pantallas.
    // Cambiar de paso en el Stepper no modifica el estado del proyecto.
    setProyectoSeleccionado(function actualizarEstado(proyectoActual) {
      if (proyectoActual === null) {
        return null;
      }

      return { ...proyectoActual, estado: estadoProyecto };
    });

    // La próxima consulta del listado recuperará los datos actualizados.
    setVersionListado(function incrementarVersion(valorActual) {
      return valorActual + 1;
    });
  }

  // Regresar al listado de proyectos.
  function volverAlListado() {
    setProyectoSeleccionado(null);
    setPantalla('listado');
  }

  // Continuar con la planificación después del alta.
  function manejarProyectoCreado(proyecto: Proyecto) {

    // Actualizar el listado cuando se vuelva a él.
    setVersionListado(versionListado + 1);

    mostrarPlanificacionProyecto(proyecto);
  }

  // Mientras se verifica la sesión.
  if (cargandoUsuario) {
    return <p>Verificando usuario...</p>;
  }

  // Mostrar una pantalla específica si la sesión expiró.
  if (sesionExpirada) {
    return (
      <Box
        sx={{
          minHeight: '100vh',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          backgroundColor: 'background.default',
          p: 2
        }}
      >
        <Paper
          elevation={1}
          sx={{
            p: 4,
            width: '100%',
            maxWidth: 440
          }}
        >
          <Typography
            component="h1"
            variant="h5"
            sx={{ mb: 2 }}
          >
            Sesión finalizada
          </Typography>

          <Typography
            variant="body1"
            color="text.secondary"
            sx={{ mb: 3 }}
          >
            Tu sesión finalizó. Iniciá sesión nuevamente
            para continuar trabajando.
          </Typography>

          <Button
            component="a"
            href="http://localhost:8080/oauth2/authorization/google"
            variant="contained"
            fullWidth
          >
            Iniciar sesión con Google
          </Button>
        </Paper>
      </Box>
    );
  }

  // Usuario no autenticado o no autorizado.
  if (usuario === null) {
    return (
      <main>
        <h1>Acceso a Animando Vidas</h1>

        <p>No se pudo verificar una sesión autorizada.</p>

        <a href="http://localhost:8080/oauth2/authorization/google">
          Iniciar sesión con Google
        </a>
      </main>
    );
  }

  // Pantalla 3: Alta de proyecto.
  if (pantalla === 'alta' && usuario.rol === 'ADMINISTRADOR') {
    return (
      <AppLayout
        usuario={usuario}
        onIrAProyectos={volverAlListado}
      >
        <CrearProyectoPage
          onCancelar={volverAlListado}
          onProyectoCreado={manejarProyectoCreado}
          onSesionExpirada={manejarSesionExpirada}
        />
      </AppLayout>
    );
  }

  // Consulta de los datos generales de un proyecto existente (solo lectura).
  if (
    pantalla === 'datos-generales' &&
    usuario.rol === 'ADMINISTRADOR' &&
    proyectoSeleccionado !== null
  ) {
    return (
      <AppLayout usuario={usuario} onIrAProyectos={volverAlListado}>
        <DatosGeneralesProyectoPage
          proyecto={proyectoSeleccionado}
          onIrAPlanificacion={volverAPlanificacion}
          onIrASeguimiento={mostrarSeguimientoProyecto}
          onVolverAlListado={volverAlListado}
          onSesionExpirada={manejarSesionExpirada}
        />
      </AppLayout>
    );
  }

  // Pantalla 4: Planificación del proyecto.
  if (
    pantalla === 'planificacion' &&
    usuario.rol === 'ADMINISTRADOR' &&
    proyectoSeleccionado !== null
  ) {
    return (
      <AppLayout
        usuario={usuario}
        onIrAProyectos={volverAlListado}
      >
        <PlanificacionProyectoPage
          proyecto={proyectoSeleccionado}
          onVolver={volverAlListado}
          onContinuar={mostrarSeguimientoProyecto}
          onVerDatosGenerales={mostrarDatosGeneralesProyecto}
          onSesionExpirada={manejarSesionExpirada}
        />
      </AppLayout>
    );
  }

  if (pantalla === 'seguimiento' && usuario.rol === 'ADMINISTRADOR'
      && proyectoSeleccionado !== null) {
    return (
      <AppLayout usuario={usuario} onIrAProyectos={volverAlListado}>
        <SeguimientoProyectoPage
          proyecto={proyectoSeleccionado}
          onVolver={volverAPlanificacion}
          onVerDatosGenerales={mostrarDatosGeneralesProyecto}
          onVolverAlListado={volverAlListado}
          onConfiguracionGuardada={manejarConfiguracionGuardada}
          onSesionExpirada={manejarSesionExpirada}
        />
      </AppLayout>
    );
  }

  // Pantalla 2: Listado de proyectos.
  return (
    <AppLayout
      usuario={usuario}
      onIrAProyectos={volverAlListado}
    >
      <ProyectosPage
        key={versionListado}
        puedeCrearProyecto={usuario.rol === 'ADMINISTRADOR'}
        onNuevoProyecto={mostrarAltaProyecto}
        onPlanificarProyecto={mostrarPlanificacionProyecto}
        onSesionExpirada={manejarSesionExpirada}
      />
    </AppLayout>
  );

}

export default App;
