
import { useEffect, useState, type FormEvent } from 'react';
import { obtenerEmpresas, type Empresa } from '../services/empresasApi';
import { crearProyecto, type Proyecto } from '../services/proyectosApi';

import './CrearProyectoPage.css';

interface CrearProyectoPageProps {
  onCancelar: () => void;
  onProyectoCreado: (proyecto: Proyecto) => void;
}

function CrearProyectoPage({
  onCancelar,
  onProyectoCreado
}: CrearProyectoPageProps) {

  const anioActual = new Date().getFullYear();

  const [nombre, setNombre] = useState('');
  const [anio, setAnio] = useState(anioActual);
  const [descripcion, setDescripcion] = useState('');
  const [idEmpresa, setIdEmpresa] = useState(0);

  const [empresas, setEmpresas] = useState<Empresa[]>([]);
  const [cargandoEmpresas, setCargandoEmpresas] = useState(true);
  const [guardando, setGuardando] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    let activo = true;

    async function cargarEmpresas() {
      try {
        const resultado = await obtenerEmpresas();

        if (activo) {
          setEmpresas(resultado);
        }
      } catch {
        if (activo) {
          setError('No se pudieron cargar las empresas financiadoras.');
        }
      } finally {
        if (activo) {
          setCargandoEmpresas(false);
        }
      }
    }

    cargarEmpresas();

    return () => {
      activo = false;
    };
  }, []);

  function obtenerNombreEmpresa(): string {

    for (const empresa of empresas) {
      if (empresa.idEmpresa === idEmpresa) {
        return empresa.nombre;
      }
    }

    return '';
  }

  function obtenerNombreCompleto(): string {

    const nombreEmpresa = obtenerNombreEmpresa();

    if (nombre.trim() === '' || nombreEmpresa === '') {
      return '';
    }

    return nombre.trim() + ' - ' + nombreEmpresa + ' - ' + anio;
  }

  async function guardarProyecto(evento: FormEvent<HTMLFormElement>) {

    evento.preventDefault();
    setError('');

    if (nombre.trim() === '') {
      setError('El nombre del proyecto es obligatorio.');
      return;
    }

    if (anio < anioActual) {
      setError('El año debe ser igual o posterior al actual.');
      return;
    }

    if (idEmpresa === 0) {
      setError('Debés seleccionar una empresa financiadora.');
      return;
    }

    if (guardando) {
      return;
    }

    setGuardando(true);

    try {
      const proyectoCreado = await crearProyecto({
        nombre: nombre.trim(),
        anio: anio,
        descripcion: descripcion.trim() || null,
        idEmpresa: idEmpresa
      });

      onProyectoCreado(proyectoCreado);

    } catch {
      setError('No se pudo registrar el proyecto.');

    } finally {
      setGuardando(false);
    }
  }

  return (
    <main className="crear-proyecto-pagina">

      <h1>Nuevo proyecto</h1>

      <p>
        Completá los datos generales del proyecto educativo.
      </p>

      <p className="crear-proyecto-aviso">
        Los datos generales no podrán modificarse después
        de registrar el proyecto.
      </p>

      <form onSubmit={guardarProyecto}>

        <div className="crear-proyecto-campo">
          <label htmlFor="nombre">
            Nombre del proyecto (Animando Vidas) *
          </label>

          <input
            id="nombre"
            type="text"
            maxLength={200}
            required
            value={nombre}
            onChange={(evento) => setNombre(evento.target.value)}
          />
        </div>

        <div className="crear-proyecto-campo">
          <label htmlFor="empresa">
            Empresa financiadora *
          </label>

          <select
            id="empresa"
            required
            value={idEmpresa}
            disabled={cargandoEmpresas}
            onChange={(evento) =>
              setIdEmpresa(Number(evento.target.value))
            }
          >
            <option value={0}>
              Seleccionar empresa
            </option>

            {empresas.map((empresa) => (
              <option
                key={empresa.idEmpresa}
                value={empresa.idEmpresa}
              >
                {empresa.nombre}
              </option>
            ))}
          </select>
        </div>

        <div className="crear-proyecto-campo">
          <label htmlFor="anio">
            Año del proyecto *
          </label>

          <input
            id="anio"
            type="number"
            min={anioActual}
            max={32767}
            required
            value={anio}
            onChange={(evento) =>
              setAnio(Number(evento.target.value))
            }
          />
        </div>

        <div className="crear-proyecto-campo">
          <label htmlFor="descripcion">
            Descripción del proyecto
          </label>

          <textarea
            id="descripcion"
            rows={4}
            value={descripcion}
            onChange={(evento) =>
              setDescripcion(evento.target.value)
            }
          />
        </div>

        <div className="crear-proyecto-vista-previa">
          <strong>Nombre completo del proyecto</strong>

          <p>
            {obtenerNombreCompleto() ||
              'Completá el nombre y seleccioná una empresa.'}
          </p>
        </div>

        {error && (
          <p role="alert" className="crear-proyecto-error">
            {error}
          </p>
        )}

        <div className="crear-proyecto-acciones">
          <button
            type="button"
            onClick={onCancelar}
            disabled={guardando}
          >
            Cancelar
          </button>

          <button
            type="submit"
            disabled={guardando || cargandoEmpresas || empresas.length === 0}
          >
            {guardando ? 'Guardando...' : 'Registrar proyecto'}
          </button>
        </div>

      </form>
    </main>
  );
}

export default CrearProyectoPage;
