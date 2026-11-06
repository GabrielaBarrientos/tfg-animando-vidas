
import { obtenerTokenCsrf } from './csrfApi';
import { verificarSesionEnRespuesta } from './erroresApi';

export interface Proyecto {
  idProyecto: number;
  nombre: string;
  nombreCompleto: string;
  anio: number;
  descripcion: string | null;
  idEmpresa: number;
  nombreEmpresa: string;
  estado: string;
}

export async function obtenerProyectos(): Promise<Proyecto[]> {

  const respuesta = await fetch('/api/proyectos', {
    method: 'GET',
    credentials: 'include'
  });

  await verificarSesionEnRespuesta(respuesta);

  if (!respuesta.ok) {
    throw new Error('No se pudo obtener el listado de proyectos');
  }

  const proyectos: Proyecto[] = await respuesta.json();

  return proyectos;
}

export class ConflictoCrearProyectoError extends Error {
  constructor() {
    super('No se pudo registrar el proyecto. Revisa los datos ingresados.');
    this.name = 'ConflictoCrearProyectoError';
  }
}

export interface CrearProyectoRequest {
  nombre: string;
  anio: number;
  descripcion: string | null;
  idEmpresa: number;
}

export async function crearProyecto(datos: CrearProyectoRequest): Promise<Proyecto> {

  // Obtener el token CSRF correspondiente a la sesión.
  const csrf = await obtenerTokenCsrf();

  // Preparar las cabeceras de la solicitud.
  const cabeceras = new Headers();

  cabeceras.set('Content-Type', 'application/json');
  cabeceras.set(csrf.headerName, csrf.token);

  // Enviar la solicitud de creación.
  const respuesta = await fetch('/api/proyectos', {
    method: 'POST',
    credentials: 'include',
    headers: cabeceras,
    body: JSON.stringify(datos)
  });

  await verificarSesionEnRespuesta(respuesta);

  if (!respuesta.ok) {
    if (respuesta.status === 409) {
      throw new ConflictoCrearProyectoError();
    }
    let detalle = 'No se pudo crear el proyecto';
    try {
      const error: { detail?: string; mensaje?: string } = await respuesta.json();
      detalle = error.detail ?? error.mensaje ?? detalle;
    } catch {
      // La respuesta de error puede estar vacía.
    }
    throw new Error(detalle);
  }

  const proyecto: Proyecto = await respuesta.json();

  return proyecto;
}
