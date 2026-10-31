
import { obtenerTokenCsrf } from './csrfApi';

export interface Proyecto {
  idProyecto: number;
  nombre: string;
  nombreCompleto: string;
  anio: number;
  descripcion: string | null;
  idEmpresa: number;
  estado: string;
}

export async function obtenerProyectos(): Promise<Proyecto[]> {

  const respuesta = await fetch('/api/proyectos', {
    method: 'GET',
    credentials: 'include'
  });

  if (!respuesta.ok) {
    throw new Error('No se pudo obtener el listado de proyectos');
  }

  const proyectos: Proyecto[] = await respuesta.json();

  return proyectos;
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

  if (!respuesta.ok) {
    throw new Error('No se pudo crear el proyecto');
  }

  const proyecto: Proyecto = await respuesta.json();

  return proyecto;
}
