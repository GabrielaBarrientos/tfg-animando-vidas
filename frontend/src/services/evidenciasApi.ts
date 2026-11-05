import { obtenerTokenCsrf } from './csrfApi';
import { verificarSesionEnRespuesta } from './erroresApi';

export interface EvidenciaTaller {
  idEvidencia: number;
  idClaseTaller: number;
  numeroClase: number;
  nombreClase: string | null;
  nombreFisico: string;
  mimeType: string;
  tamanioBytes: number;
  esUtil: boolean;
  esDestacada: boolean;
}

export interface EvidenciasTallerResponse {
  ultimaSincronizacion: string | null;
  evidencias: EvidenciaTaller[];
}

function rutaBase(idProyecto: number, idTaller: string): string {
  return `/api/proyectos/${idProyecto}/talleres/${encodeURIComponent(idTaller)}`;
}

export function urlVisualizacionEvidencia(idProyecto: number, idTaller: string, idEvidencia: number): string {
  return `${rutaBase(idProyecto, idTaller)}/evidencias/${idEvidencia}/visualizar`;
}

export function urlDescargaEvidencia(idProyecto: number, idTaller: string, idEvidencia: number): string {
  return `${rutaBase(idProyecto, idTaller)}/evidencias/${idEvidencia}/descargar`;
}

export function urlZipClase(idProyecto: number, idTaller: string, idClaseTaller: number): string {
  return `${rutaBase(idProyecto, idTaller)}/clases/${idClaseTaller}/evidencias/zip`;
}

export async function consultarEvidencias(idProyecto: number, idTaller: string): Promise<EvidenciasTallerResponse> {
  const respuesta = await fetch(`${rutaBase(idProyecto, idTaller)}/evidencias`, { credentials: 'include' });
  await verificarSesionEnRespuesta(respuesta);
  if (respuesta.status === 403) throw new Error('No tenés acceso a las evidencias de este taller.');
  if (!respuesta.ok) throw new Error('No se pudieron consultar las evidencias.');
  const datos: EvidenciasTallerResponse = await respuesta.json();
  return datos;
}

export async function sincronizarEvidencias(idProyecto: number, idTaller: string): Promise<number> {
  const csrf = await obtenerTokenCsrf();
  const cabeceras = new Headers();
  cabeceras.set(csrf.headerName, csrf.token);
  const respuesta = await fetch(`${rutaBase(idProyecto, idTaller)}/evidencias/sincronizar`, {
    credentials: 'include', method: 'POST', headers: cabeceras
  });
  await verificarSesionEnRespuesta(respuesta);
  if (respuesta.status === 403) throw new Error('No tenés permiso para actualizar las evidencias.');
  if (respuesta.status === 409) throw new Error('Actualizá primero las clases del taller y volvé a intentar.');
  if (!respuesta.ok) throw new Error('No fue posible sincronizar las evidencias con la plataforma.');
  const datos: { cantidadEvidencias: number } = await respuesta.json();
  return datos.cantidadEvidencias;
}

export async function actualizarMarcadoresEvidencia(idProyecto: number, idTaller: string,
  idEvidencia: number, esUtil: boolean, esDestacada: boolean): Promise<EvidenciaTaller> {
  const csrf = await obtenerTokenCsrf();
  const cabeceras = new Headers();
  cabeceras.set('Content-Type', 'application/json');
  cabeceras.set(csrf.headerName, csrf.token);
  const respuesta = await fetch(`${rutaBase(idProyecto, idTaller)}/evidencias/${idEvidencia}/marcadores`, {
    credentials: 'include', method: 'PATCH', headers: cabeceras,
    body: JSON.stringify({ esUtil, esDestacada })
  });
  await verificarSesionEnRespuesta(respuesta);
  if (respuesta.status === 403) throw new Error('No tenés permiso para valorar evidencias.');
  if (!respuesta.ok) throw new Error('No se pudo guardar la valoración de la evidencia.');
  const datos: EvidenciaTaller = await respuesta.json();
  return datos;
}
