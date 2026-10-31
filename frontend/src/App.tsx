
import { useEffect, useState } from 'react';

import AppLayout from './components/AppLayout';
import ProyectosPage from './pages/ProyectosPage';
import CrearProyectoPage from './pages/CrearProyectoPage';

import {
  obtenerUsuarioActual,
  type UsuarioActual
} from './services/authApi';

function App() {
  // Usuario autenticado mediante Google.
  const [usuario, setUsuario] = useState<UsuarioActual | null>(null);

  // Indica si se está verificando la sesión.
  const [cargandoUsuario, setCargandoUsuario] = useState(true);

  // Pantalla que se está mostrando.
  const [pantalla, setPantalla] = useState<'listado' | 'alta'>('listado');

  // Permite actualizar el listado después de crear un proyecto.
  const [versionListado, setVersionListado] = useState(0);

  // Verificar la sesión al iniciar la aplicación.
  useEffect(() => {
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
    setPantalla('alta');
  }

  // Regresar al listado de proyectos.
  function volverAlListado() {
    setPantalla('listado');
  }

  // Regresar al listado y actualizarlo después del alta.
  function manejarProyectoCreado() {
    setVersionListado(versionActual => versionActual + 1);
    setPantalla('listado');
  }

  // Mientras se verifica la sesión.
  if (cargandoUsuario) {
    return <p>Verificando usuario...</p>;
  }

  // Usuario no autenticado o no autorizado.
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

  // Pantalla 3: Alta de proyecto (solo administradores).
  if (pantalla === 'alta' && usuario.rol === 'ADMINISTRADOR') {
    return (
      <AppLayout
        usuario={usuario}
        onIrAProyectos={volverAlListado}
      >
        <CrearProyectoPage
          onCancelar={volverAlListado}
          onProyectoCreado={manejarProyectoCreado}
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
      />
    </AppLayout>
  );
}

export default App;