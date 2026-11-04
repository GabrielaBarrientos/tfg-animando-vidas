
import { useCallback, useEffect, useState } from 'react';

import AppLayout from './components/AppLayout';
import ProyectosPage from './pages/ProyectosPage';
import CrearProyectoPage from './pages/CrearProyectoPage';
import PlanificacionProyectoPage from './pages/PlanificacionProyectoPage';
import SeguimientoProyectoPage from './pages/SeguimientoProyectoPage';
import DatosGeneralesProyectoPage from './pages/DatosGeneralesProyectoPage';
import DocentesProyectoPage from './pages/DocentesProyectoPage';
import DetalleProyectoPage from './pages/DetalleProyectoPage';
import TalleresProyectoPage from './pages/TalleresProyectoPage';
import type { SolapaProyecto } from './components/ProyectoTabs';

import { Alert, Box, Button, Paper, Snackbar, Typography } from '@mui/material';

import {
  obtenerUsuarioActual,
  type UsuarioActual
} from './services/authApi';

import type { Proyecto } from './services/proyectosApi';

// Pantallas disponibles en esta etapa.
type Pantalla = 'listado' | 'alta' | 'datos-generales' | 'planificacion' | 'seguimiento' | 'docentes' | 'detalle' | 'talleres' | 'detalle-taller';

function App() {

  // Usuario autenticado mediante Google.
  const [usuario, setUsuario] = useState<UsuarioActual | null>(null);

  // Indica si se está verificando la sesión.
  const [cargandoUsuario, setCargandoUsuario] = useState(true);

  // Pantalla que se está mostrando.
  const [pantalla, setPantalla] = useState<Pantalla>('listado');
  const [tallerSeleccionado, setTallerSeleccionado] = useState<string | null>(null);
  const [solapaDetalle, setSolapaDetalle] = useState<SolapaProyecto>('resumen');

  // El Stepper solo pertenece al flujo de alta de un proyecto.
  // Las pantallas abiertas desde Detalle se muestran sin Stepper.
  const [enFlujoAlta, setEnFlujoAlta] = useState(true);

  // Proyecto que se está configurando.
  const [proyectoSeleccionado, setProyectoSeleccionado] =
    useState<Proyecto | null>(null);

  // Permite actualizar el listado después de crear un proyecto.
  const [versionListado, setVersionListado] = useState(0);

  // Indica que la sesión finalizó durante el uso de la aplicación.
  const [sesionExpirada, setSesionExpirada] = useState(false);
  const [advertenciaSinDocentes, setAdvertenciaSinDocentes] = useState(false);

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
    setEnFlujoAlta(true);
    setProyectoSeleccionado(null);
    setPantalla('alta');
  }

  // Abrir la planificación de un proyecto.
  function mostrarPlanificacionProyecto(proyecto: Proyecto) {
    setEnFlujoAlta(true);
    setProyectoSeleccionado(proyecto);
    setPantalla('planificacion');
  }


  function mostrarDatosGeneralesProyecto() {
    setPantalla('datos-generales');
  }

  function mostrarSeguimientoProyecto() {
    setPantalla('seguimiento');
  }

  function mostrarDocentesProyecto() {
    setPantalla('docentes');
  }

  function mostrarDetalleProyecto(proyecto: Proyecto) {
    setEnFlujoAlta(false);
    setProyectoSeleccionado(proyecto);
    setSolapaDetalle('resumen');
    setPantalla('detalle');
  }

  function mostrarTalleresProyecto() {
    setPantalla('talleres');
  }

  function volverAlDetalle() {
    setSolapaDetalle('resumen');
    setPantalla('detalle');
  }

  function seleccionarSolapaDesdeTalleres(solapa: SolapaProyecto) {
    if (solapa === 'talleres') return;
    setSolapaDetalle(solapa);
    setPantalla('detalle');
  }

  function mostrarDocentesDesdeDetalle() {
    setEnFlujoAlta(false);
    setPantalla('docentes');
  }

  function mostrarPlanificacionDesdeDetalle() {
    setEnFlujoAlta(false);
    setPantalla('planificacion');
  }

  function mostrarSeguimientoDesdeDetalle() {
    setEnFlujoAlta(false);
    setPantalla('seguimiento');
  }

  function finalizarGestionDocentes(sinDocentes: boolean) {
    volverAlListado();
    setAdvertenciaSinDocentes(sinDocentes);
  }

  function cerrarAdvertenciaSinDocentes() {
    setAdvertenciaSinDocentes(false);
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
          onIrADocentes={mostrarDocentesProyecto}
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
          enFlujoAlta={enFlujoAlta}
          onVolverAlDetalle={volverAlDetalle}
          onVolver={volverAlListado}
          onContinuar={mostrarSeguimientoProyecto}
          onIrADocentes={mostrarDocentesProyecto}
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
          enFlujoAlta={enFlujoAlta}
          onVolverAlDetalle={volverAlDetalle}
          onVolver={volverAPlanificacion}
          onVerDatosGenerales={mostrarDatosGeneralesProyecto}
          onVolverAlListado={volverAlListado}
          onConfiguracionGuardada={manejarConfiguracionGuardada}
          onIrADocentes={mostrarDocentesProyecto}
          onSesionExpirada={manejarSesionExpirada}
        />
      </AppLayout>
    );
  }

  if (pantalla === 'docentes' && proyectoSeleccionado !== null
      && (usuario.rol === 'ADMINISTRADOR' || usuario.rol === 'OPERATIVO')) {
    return (
      <AppLayout usuario={usuario} onIrAProyectos={volverAlListado}>
        <DocentesProyectoPage
          proyecto={proyectoSeleccionado}
          enFlujoAlta={enFlujoAlta}
          onVolverAlDetalle={volverAlDetalle}
          puedeGestionar={usuario.rol === 'ADMINISTRADOR'}
          onIrADatosGenerales={mostrarDatosGeneralesProyecto}
          onIrAPlanificacion={volverAPlanificacion}
          onIrASeguimiento={mostrarSeguimientoProyecto}
          onFinalizar={finalizarGestionDocentes}
          onVolverAlListado={volverAlListado}
          onSesionExpirada={manejarSesionExpirada}
        />
      </AppLayout>
    );
  }

  // Pantalla 7: consulta de detalle del proyecto para todos los perfiles autorizados.
  if (pantalla === 'detalle' && proyectoSeleccionado !== null) {
    return (
      <AppLayout usuario={usuario} onIrAProyectos={volverAlListado}>
        <DetalleProyectoPage
          proyecto={proyectoSeleccionado}
          rol={usuario.rol}
          onVolverAlListado={volverAlListado}
          onIrADocentes={mostrarDocentesDesdeDetalle}
          onIrAPlanificacion={mostrarPlanificacionDesdeDetalle}
          onIrASeguimiento={mostrarSeguimientoDesdeDetalle}
          onIrATalleres={mostrarTalleresProyecto}
          solapaInicial={solapaDetalle}
          onSesionExpirada={manejarSesionExpirada}
        />
      </AppLayout>
    );
  }

  if (pantalla === 'talleres' && proyectoSeleccionado !== null) {
    return (
      <AppLayout usuario={usuario} onIrAProyectos={volverAlListado}>
        <TalleresProyectoPage
          proyecto={proyectoSeleccionado}
          puedeActualizar={usuario.rol !== 'EXTERNO'}
          onVolverAlListado={volverAlListado}
          onVolverAlDetalle={volverAlDetalle}
          onSeleccionarSolapa={seleccionarSolapaDesdeTalleres}
          onVerTaller={function verDetalleTaller(idTaller) {
            setTallerSeleccionado(idTaller);
            setPantalla('detalle-taller');
          }}
          onSesionExpirada={manejarSesionExpirada}
        />
      </AppLayout>
    );
  }

  if (pantalla === 'detalle-taller' && tallerSeleccionado !== null) {
    return (
      <AppLayout usuario={usuario} onIrAProyectos={volverAlListado}>
        <Box sx={{ p: 3 }}>
          <Button variant="text" onClick={mostrarTalleresProyecto}>Volver a talleres</Button>
          <Typography variant="h5" component="h1" sx={{ my: 2 }}>Detalle del taller</Typography>
          <Alert severity="info">
            Taller {tallerSeleccionado}. La consulta detallada de clases, evidencias y encuestas
            corresponde a la Pantalla 12, prevista para un incremento posterior.
          </Alert>
        </Box>
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
        onVerDetalle={mostrarDetalleProyecto}
        onSesionExpirada={manejarSesionExpirada}
      />
      <Snackbar
        open={advertenciaSinDocentes}
        anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
        autoHideDuration={6000}
        onClose={cerrarAdvertenciaSinDocentes}
      >
        <Alert severity="warning" onClose={cerrarAdvertenciaSinDocentes}>
          El proyecto no tiene docentes
        </Alert>
      </Snackbar>
    </AppLayout>
  );

}

export default App;
