import { obtenerTokenCsrf } from './csrfApi';
import { verificarSesionEnRespuesta } from './erroresApi';

export interface Docente {
  idDocente: number;
  idProyecto: number;
  nombres: string;
  apellidos: string;
  email: string;
  pais: string;
  provincia: string;
  centroEducativo: string;
  tieneTalleresAsociados: boolean;
}

export interface DatosDocente {
  nombres: string;
  apellidos: string;
  email: string;
  pais: string;
  provincia: string;
  centroEducativo: string;
}

export async function listarDocentes(idProyecto: number): Promise<Docente[]> {
  const respuesta = await fetch(`/api/proyectos/${idProyecto}/docentes`, {
    credentials: 'include'
  });
  await verificarSesionEnRespuesta(respuesta);
  if (!respuesta.ok) {
    throw new Error('No se pudieron consultar los docentes del proyecto.');
  }
  const docentes: Docente[] = await respuesta.json();
  return docentes;
}

export async function guardarDocente(idProyecto: number, idDocente: number | null,
  datos: DatosDocente): Promise<Docente> {
  const csrf = await obtenerTokenCsrf();
  const cabeceras = new Headers();
  cabeceras.set('Content-Type', 'application/json');
  cabeceras.set(csrf.headerName, csrf.token);

  let url = `/api/proyectos/${idProyecto}/docentes`;
  let metodo = 'POST';
  if (idDocente !== null) {
    url += `/${idDocente}`;
    metodo = 'PUT';
  }

  const respuesta = await fetch(url, {
    method: metodo,
    headers: cabeceras,
    credentials: 'include',
    body: JSON.stringify(datos)
  });
  await verificarSesionEnRespuesta(respuesta);
  if (respuesta.status === 409) {
    throw new Error('Ya existe un docente con este correo electrónico en el proyecto.');
  }
  if (respuesta.status === 400) {
    throw new Error('Revisá los datos ingresados para el docente.');
  }
  if (!respuesta.ok) {
    throw new Error('No se pudo guardar el docente.');
  }
  const docente: Docente = await respuesta.json();
  return docente;
}

export async function eliminarDocente(idProyecto: number, idDocente: number): Promise<void> {
  const csrf = await obtenerTokenCsrf();
  const cabeceras = new Headers();
  cabeceras.set(csrf.headerName, csrf.token);
  const respuesta = await fetch(`/api/proyectos/${idProyecto}/docentes/${idDocente}`, {
    method: 'DELETE', credentials: 'include', headers: cabeceras
  });
  await verificarSesionEnRespuesta(respuesta);
  if (respuesta.status === 409) {
    throw new Error('No se puede eliminar el docente porque tiene talleres asociados.');
  }
  if (!respuesta.ok) {
    throw new Error('No se pudo eliminar el docente.');
  }
}
