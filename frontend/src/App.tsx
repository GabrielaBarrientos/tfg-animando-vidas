
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
import DetalleTallerPage from './pages/DetalleTallerPage';
import AccesoPrototipoPage from './pages/AccesoPrototipoPage';
import type { SolapaProyecto } from './components/ProyectoTabs';

import { Alert, Snackbar } from '@mui/material';

import {
  obtenerUsuarioActual,
  cerrarSesionEnServidor,
  type UsuarioActual
} from './services/authApi';

import { SesionExpiradaError } from './services/erroresApi';

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

  // El logout se confirma en el servidor antes de volver a la pantalla de acceso.
  async function manejarCerrarSesion(): Promise<void> {
    try {
      await cerrarSesionEnServidor();
    } catch (error) {
      // Si la sesión ya expiró, no hay sesión local que invalidar.
      if (!(error instanceof SesionExpiradaError)) {
        throw error;
      }
    }

    setUsuario(null);
    setSesionExpirada(false);
    window.location.replace('/?acceso=sesion-cerrada');
  }

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

  function mostrarDetalleTaller(idTaller: string): void {
    setTallerSeleccionado(idTaller);
    setPantalla('detalle-taller');
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

  // Acceso al prototipo: iniciar Google OAuth2 desde una única pantalla.
  // La selección de cuentas (Pantalla 1.2 de Figma) la presenta Google.
  if (cargandoUsuario) {
    return <AccesoPrototipoPage estado="verificando" />;
  }

  if (sesionExpirada) {
    return <AccesoPrototipoPage estado="sesion-expirada" />;
  }

  if (usuario === null) {
    return <AccesoPrototipoPage estado="sin-sesion" />;
  }

  // Pantalla 3: Alta de proyecto.
  if (pantalla === 'alta' && usuario.rol === 'ADMINISTRADOR') {
    return (
      <AppLayout
        usuario={usuario}
        onIrAProyectos={volverAlListado}
        onCerrarSesion={manejarCerrarSesion}
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
      <AppLayout usuario={usuario} onIrAProyectos={volverAlListado} onCerrarSesion={manejarCerrarSesion}>
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
        onCerrarSesion={manejarCerrarSesion}
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
      <AppLayout usuario={usuario} onIrAProyectos={volverAlListado} onCerrarSesion={manejarCerrarSesion}>
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
      <AppLayout usuario={usuario} onIrAProyectos={volverAlListado} onCerrarSesion={manejarCerrarSesion}>
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
      <AppLayout usuario={usuario} onIrAProyectos={volverAlListado} onCerrarSesion={manejarCerrarSesion}>
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
      <AppLayout usuario={usuario} onIrAProyectos={volverAlListado} onCerrarSesion={manejarCerrarSesion}>
        <TalleresProyectoPage
          proyecto={proyectoSeleccionado}
          puedeActualizar={usuario.rol !== 'EXTERNO'}
          onVolverAlListado={volverAlListado}
          onVolverAlDetalle={volverAlDetalle}
          onSeleccionarSolapa={seleccionarSolapaDesdeTalleres}
          onVerTaller={mostrarDetalleTaller}
          onSesionExpirada={manejarSesionExpirada}
        />
      </AppLayout>
    );
  }

  if (pantalla === 'detalle-taller' && tallerSeleccionado !== null && proyectoSeleccionado !== null) {
    return (
      <AppLayout usuario={usuario} onIrAProyectos={volverAlListado} onCerrarSesion={manejarCerrarSesion}>
        <DetalleTallerPage
          proyecto={proyectoSeleccionado}
          idTaller={tallerSeleccionado}
          puedeActualizar={usuario.rol !== 'EXTERNO'}
          onVolverAProyectos={volverAlListado}
          onVolverAProyecto={volverAlDetalle}
          onVolverATalleres={mostrarTalleresProyecto}
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
      onCerrarSesion={manejarCerrarSesion}
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
