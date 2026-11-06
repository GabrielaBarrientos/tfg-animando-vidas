import { obtenerTokenCsrf } from './csrfApi';
import { verificarSesionEnRespuesta } from './erroresApi';

export interface PreguntaPrograma {
  idPreguntaProyecto: number;
  idPreguntaPlataforma: number;
  numeroPregunta: number;
  tipo: 'intro' | 'clase';
  enunciado: string;
}
export interface ClasePrograma {
  idPrograma: number;
  idClasePlataforma: number;
  numeroClase: number;
  nombreClase: string;
  nombreModulo: string;
  preguntas: PreguntaPrograma[];
}
export interface ProgramaProyecto {
  sincronizado: boolean;
  ultimaSincronizacion: string | null;
  clases: ClasePrograma[];
}

async function convertirRespuesta(respuesta: Response): Promise<ProgramaProyecto> {
  await verificarSesionEnRespuesta(respuesta);
  if (!respuesta.ok) {
    let detalle = 'No se pudo consultar o sincronizar el programa.';
    try {
      const error: { detail?: string; mensaje?: string } = await respuesta.json();
      detalle = error.detail ?? error.mensaje ?? detalle;
    } catch {
      // El servidor puede devolver una respuesta sin cuerpo.
    }
    throw new Error(detalle);
  }
  const programa: ProgramaProyecto = await respuesta.json();
  return programa;
}

export async function consultarPrograma(idProyecto: number): Promise<ProgramaProyecto> {
  const respuesta = await fetch(`/api/proyectos/${idProyecto}/programa`, {
    credentials: 'include'
  });
  return convertirRespuesta(respuesta);
}

export async function actualizarPrograma(idProyecto: number): Promise<ProgramaProyecto> {
  const csrf = await obtenerTokenCsrf();
  const cabeceras = new Headers();
  cabeceras.set(csrf.headerName, csrf.token);
  const respuesta = await fetch(`/api/proyectos/${idProyecto}/programa/sincronizar`, {
    method: 'POST', headers: cabeceras, credentials: 'include'
  });
  return convertirRespuesta(respuesta);
}
