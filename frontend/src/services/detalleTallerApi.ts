import { obtenerTokenCsrf } from './csrfApi';
import { verificarSesionEnRespuesta } from './erroresApi';
import type { EstadoEvidencias, EstadoTaller } from './talleresApi';

export type EstadoClase = 'REALIZADA' | 'PENDIENTE';

export interface ClaseDetalle {
  numeroClase: number;
  nombreClase: string | null;
  nombreModulo: string | null;
  fechaPrevista: string;
  fechaRealizacion: string | null;
  cantidadParticipantes: number | null;
  cantidadEvidencias: number;
  estado: EstadoClase;
}

export interface DetalleTaller {
  idTaller: string;
  nombreDocente: string;
  usuarioPlataforma: string;
  pais: string;
  provincia: string;
  centroEducativo: string;
  cantidadAlumnos: number;
  totalClases: number;
  avanceEsperado: number;
  avanceReal: number;
  diferencia: number;
  estado: EstadoTaller;
  evidencias: EstadoEvidencias;
  pendienteBaja: boolean;
  ultimaSincronizacion: string | null;
  clases: ClaseDetalle[];
}

/** Comparte las consultas simultáneas originadas por React.StrictMode, sin guardar resultados. */
const consultasEnCurso = new Map<string, Promise<DetalleTaller>>();

export function obtenerDetalleTaller(idProyecto: number, idTaller: string): Promise<DetalleTaller> {
  const clave = `${idProyecto}:${idTaller}`;
  const consultaActual = consultasEnCurso.get(clave);
  if (consultaActual !== undefined) return consultaActual;

  const consulta = consultarDetalleDesdePostgresql(idProyecto, idTaller, clave);
  consultasEnCurso.set(clave, consulta);
  return consulta;
}

/** Solo PostgreSQL: nunca realiza una sincronización remota. */
async function consultarDetalleDesdePostgresql(idProyecto: number, idTaller: string,
  clave: string): Promise<DetalleTaller> {
  try {
    return await ejecutarConsulta(idProyecto, idTaller);
  } finally {
    consultasEnCurso.delete(clave);
  }
}

async function ejecutarConsulta(idProyecto: number, idTaller: string): Promise<DetalleTaller> {
  const respuesta = await fetch(
    `/api/proyectos/${idProyecto}/talleres/${encodeURIComponent(idTaller)}`,
    { credentials: 'include' }
  );
  await verificarSesionEnRespuesta(respuesta);
  if (respuesta.status === 403) throw new Error('No tenés permiso para consultar este taller.');
  if (respuesta.status === 404) throw new Error('El taller ya no existe en este proyecto.');
  if (respuesta.status === 409) throw new Error('El proyecto requiere planificación y seguimiento configurados.');
  if (!respuesta.ok) throw new Error('No se pudo consultar el detalle del taller.');
  const detalle: DetalleTaller = await respuesta.json();
  return detalle;
}


/** Solicita exclusivamente las clases del taller indicado, sin sincronizar el proyecto completo. */
export async function sincronizarClasesTaller(idProyecto: number, idTaller: string): Promise<number> {
  const csrf = await obtenerTokenCsrf();
  const cabeceras = new Headers();
  cabeceras.set(csrf.headerName, csrf.token);
  const respuesta = await fetch(
    `/api/proyectos/${idProyecto}/talleres/${encodeURIComponent(idTaller)}/clases/sincronizar`,
    { method: 'POST', credentials: 'include', headers: cabeceras }
  );
  await verificarSesionEnRespuesta(respuesta);
  if (respuesta.status === 403) throw new Error('No tenés permiso para actualizar las clases.');
  if (respuesta.status === 404) throw new Error('El taller ya no existe en este proyecto.');
  if (respuesta.status === 409) throw new Error('No se pueden actualizar las clases de este taller.');
  if (!respuesta.ok) throw new Error('No se pudo conectar con la plataforma educativa.');
  const datos: { cantidadClases: number } = await respuesta.json();
  return datos.cantidadClases;
}
