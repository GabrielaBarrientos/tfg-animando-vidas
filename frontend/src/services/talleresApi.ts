import { obtenerTokenCsrf } from './csrfApi';
import { verificarSesionEnRespuesta } from './erroresApi';

export type EstadoTaller = 'NO_INICIADO' | 'EN_PLAZO' | 'PROXIMO_ATRASO' | 'ATRASADO';
export type EstadoEvidencias = 'SIN_CLASES_REALIZADAS' | 'COMPLETAS' | 'PENDIENTES';

export interface TallerResumen {
  idTaller: string;
  pais: string;
  centroEducativo: string;
  docente: string;
  cantidadAlumnos: number;
  avanceEsperado: number;
  avanceReal: number;
  diferencia: number;
  estado: EstadoTaller;
  evidencias: EstadoEvidencias;
  pendienteBaja: boolean;
}

export async function listarTalleres(idProyecto: number): Promise<TallerResumen[]> {
  const respuesta = await fetch(`/api/proyectos/${idProyecto}/talleres`, {
    credentials: 'include'
  });
  await verificarSesionEnRespuesta(respuesta);
  if (respuesta.status === 403) throw new Error('No tenés permiso para consultar los talleres.');
  if (respuesta.status === 409) throw new Error('El proyecto requiere planificación y configuración de seguimiento.');
  if (!respuesta.ok) throw new Error('No se pudo consultar el listado de talleres.');
  const talleres: TallerResumen[] = await respuesta.json();
  return talleres;
}

export async function sincronizarTalleres(idProyecto: number): Promise<number> {
  const csrf = await obtenerTokenCsrf();
  const cabeceras = new Headers();
  cabeceras.set(csrf.headerName, csrf.token);
  const respuesta = await fetch(`/api/proyectos/${idProyecto}/talleres/sincronizar`, {
    method: 'POST', credentials: 'include', headers: cabeceras
  });
  await verificarSesionEnRespuesta(respuesta);
  if (respuesta.status === 403) throw new Error('No tenés permiso para actualizar los talleres.');
  if (respuesta.status === 409) throw new Error('Comprobá que los docentes de la plataforma estén registrados en el proyecto.');
  if (respuesta.status === 502) throw new Error('No se pudo conectar con la plataforma educativa.');
  if (!respuesta.ok) throw new Error('No se pudieron actualizar los talleres.');
  const resultado: { cantidadTalleres: number } = await respuesta.json();
  return resultado.cantidadTalleres;
}

/** Solo puede eliminarse una copia marcada ausente por una sincronización exitosa. */
export async function eliminarCopiaLocalTaller(idProyecto: number, idTaller: string): Promise<void> {
  const csrf = await obtenerTokenCsrf();
  const cabeceras = new Headers();
  cabeceras.set(csrf.headerName, csrf.token);
  const respuesta = await fetch(
    `/api/proyectos/${idProyecto}/talleres/${encodeURIComponent(idTaller)}/copia-local`,
    { method: 'DELETE', credentials: 'include', headers: cabeceras }
  );
  await verificarSesionEnRespuesta(respuesta);
  if (respuesta.status === 409) throw new Error('La copia local ya no está pendiente de baja. Actualizá los talleres.');
  if (!respuesta.ok) throw new Error('No se pudo eliminar la copia local del taller.');
}

/** Resumen persistido en el proyecto: no depende de los filtros de la tabla. */
export interface TalleresMetadatos {
  estadoProyecto: string;
  ultimaSincronizacion: string | null;
  cantidadTalleresPlataforma: number;
}

export async function obtenerMetadatosTalleres(idProyecto: number): Promise<TalleresMetadatos> {
  const respuesta = await fetch(`/api/proyectos/${idProyecto}/talleres/metadatos`, {
    credentials: 'include'
  });
  await verificarSesionEnRespuesta(respuesta);
  if (respuesta.status === 403) throw new Error('No tenés permiso para consultar el proyecto.');
  if (!respuesta.ok) throw new Error('No se pudieron consultar los datos de sincronización.');
  const metadatos: TalleresMetadatos = await respuesta.json();
  return metadatos;
}
