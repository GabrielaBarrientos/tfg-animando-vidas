
import { useEffect, useState } from 'react';
import { obtenerProyectos, type Proyecto } from '../services/proyectosApi';
import './ProyectosPage.css';

function obtenerNombreEstado(estado: string): string {
  switch (estado) {
    case 'PENDIENTE_PLANIFICACION':
      return 'Pendiente de planificación';

    case 'PLANIFICADO':
      return 'Planificado';

    case 'EN_CURSO':
      return 'En curso';

    case 'FINALIZADO':
      return 'Finalizado';

    default:
      return estado;
  }
}


interface ProyectosPageProps {puedeCrearProyecto: boolean; onNuevoProyecto: () => void;
}


function ProyectosPage({puedeCrearProyecto, onNuevoProyecto}: ProyectosPageProps) {
  const [proyectos, setProyectos] = useState<Proyecto[]>([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let componenteActivo = true;

    async function cargarProyectos() {
      try {
        const proyectosObtenidos = await obtenerProyectos();

        if (componenteActivo) {
          setProyectos(proyectosObtenidos);
        }
      } catch {
        if (componenteActivo) {
          setError('No se pudo obtener el listado de proyectos.');
        }
      } finally {
        if (componenteActivo) {
          setCargando(false);
        }
      }
    }

    cargarProyectos();

    return () => {
      componenteActivo = false;
    };
  }, []);

  return (
    <main className="proyectos-pagina">
      <h1>Proyectos</h1>
      <p>Listado de proyectos educativos de Animando Vidas.</p>

        {puedeCrearProyecto && (
            <button
            type="button"
            onClick={onNuevoProyecto}
        >
            Nuevo proyecto
        </button>
        )}

      {cargando && <p>Cargando proyectos...</p>}

      {error && <p role="alert">{error}</p>}

      {!cargando && !error && proyectos.length === 0 && (
        <p>No hay proyectos disponibles para consultar.</p>
      )}

      {!cargando && !error && proyectos.length > 0 && (
        <table className="proyectos-tabla">
          <thead>
            <tr>
              <th scope="col">Proyecto</th>
              <th scope="col">Estado</th>
            </tr>
          </thead>

          <tbody>
            {proyectos.map((proyecto) => (
              <tr key={proyecto.idProyecto}>
                <td>{proyecto.nombreCompleto}</td>
                <td>{obtenerNombreEstado(proyecto.estado)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </main>
  );
}

export default ProyectosPage;
