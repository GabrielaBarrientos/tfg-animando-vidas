
import { useEffect, useState } from 'react';

import ProyectosPage from './pages/ProyectosPage';
import CrearProyectoPage from './pages/CrearProyectoPage';

import {
  obtenerUsuarioActual,
  type UsuarioActual
} from './services/authApi';

type Pantalla = 'listado' | 'alta';

function App() {

  const [usuario, setUsuario] =
    useState<UsuarioActual | null>(null);

  const [cargandoUsuario, setCargandoUsuario] =
    useState(true);

  const [pantalla, setPantalla] =
    useState<Pantalla>('listado');

  const [versionListado, setVersionListado] =
    useState(0);

  useEffect(() => {

    let activo = true;

    async function cargarUsuario() {

      try {
        const resultado = await obtenerUsuarioActual();

        if (activo) {
          setUsuario(resultado);
        }

      } catch {
        if (activo) {
          setUsuario(null);
        }

      } finally {
        if (activo) {
          setCargandoUsuario(false);
        }
      }
    }

    cargarUsuario();

    return () => {
      activo = false;
    };

  }, []);

  function mostrarAltaProyecto() {
    setPantalla('alta');
  }

  function volverAlListado() {
    setPantalla('listado');
  }

  function manejarProyectoCreado() {

    // Forzar la actualización del listado.
    setVersionListado(versionListado + 1);

    // Regresar a la pantalla de proyectos.
    setPantalla('listado');
  }

  if (cargandoUsuario) {
    return <p>Verificando usuario...</p>;
  }

  if (usuario === null) {
    return (
      <main>
        <h1>Acceso a Animando Vidas</h1>

        <p>
          No se pudo verificar una sesión autorizada.
        </p>

        <a href="http://localhost:8080/oauth2/authorization/google">
          Iniciar sesión con Google
        </a>
      </main>
    );
  }

  if (pantalla === 'alta' &&
      usuario.rol === 'ADMINISTRADOR') {

    return (
      <CrearProyectoPage
        onCancelar={volverAlListado}
        onProyectoCreado={manejarProyectoCreado}
      />
    );
  }

  return (
    <ProyectosPage
      key={versionListado}
      puedeCrearProyecto={usuario.rol === 'ADMINISTRADOR'}
      onNuevoProyecto={mostrarAltaProyecto}
    />
  );
}

export default App;
