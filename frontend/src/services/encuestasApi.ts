import { obtenerTokenCsrf } from './csrfApi';
import { verificarSesionEnRespuesta } from './erroresApi';

export interface RespuestaEncuesta {
  idPreguntaProyecto: number;
  pregunta: string;
  tipo: 'clase';
  respuesta: string;
}

export interface ClaseEncuesta {
  idClaseTaller: number;
  numeroClase: number;
  nombreClase: string | null;
  estado: 'REALIZADA' | 'PENDIENTE';
  respuestas: RespuestaEncuesta[];
}

export interface EncuestasTallerResponse {
  ultimaSincronizacion: string | null;
  clases: ClaseEncuesta[];
}

function ruta(idProyecto: number, idTaller: string): string {
  return `/api/proyectos/${idProyecto}/talleres/${encodeURIComponent(idTaller)}/encuestas`;
}

export async function obtenerEncuestas(idProyecto: number, idTaller: string): Promise<EncuestasTallerResponse> {
  const respuesta = await fetch(ruta(idProyecto, idTaller), { credentials: 'include' });
  await verificarSesionEnRespuesta(respuesta);
  if (respuesta.status === 403) {
    throw new Error('No tenés permiso para consultar las encuestas del taller.');
  }
  if (!respuesta.ok) {
    throw new Error('No se pudieron consultar las encuestas del taller.');
  }
  const datos: EncuestasTallerResponse = await respuesta.json();
  return datos;
}

export async function sincronizarRespuestas(idProyecto: number, idTaller: string): Promise<number> {
  const csrf = await obtenerTokenCsrf();
  const headers = new Headers();
  headers.set(csrf.headerName, csrf.token);
  const respuesta = await fetch(`${ruta(idProyecto, idTaller)}/sincronizar`, {
    credentials: 'include', method: 'POST', headers
  });
  await verificarSesionEnRespuesta(respuesta);
  if (respuesta.status === 403) {
    throw new Error('No tenés permiso para actualizar las respuestas.');
  }
  if (respuesta.status === 409) {
    throw new Error('Actualizá primero los talleres y sus clases para sincronizar las preguntas y respuestas.');
  }
  if (!respuesta.ok) {
    throw new Error('No se pudieron actualizar las respuestas desde la plataforma educativa.');
  }
  const datos: { cantidadRespuestas: number } = await respuesta.json();
  return datos.cantidadRespuestas;
}
