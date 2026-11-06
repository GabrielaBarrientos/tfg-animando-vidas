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

/** Solo consulta PostgreSQL: nunca inicia la sincronización remota. */
export function listarTalleres(idProyecto: number): Promise<TallerResumen[]> {
  const consultaActual = consultasEnCurso.get(idProyecto);
  if (consultaActual !== undefined) return consultaActual;

  const consulta = ejecutarConsulta(idProyecto);
  consultasEnCurso.set(idProyecto, consulta);
  void consulta.then(
    function finalizarCorrectamente() {
      if (consultasEnCurso.get(idProyecto) === consulta) consultasEnCurso.delete(idProyecto);
    },
    function finalizarConError() {
      if (consultasEnCurso.get(idProyecto) === consulta) consultasEnCurso.delete(idProyecto);
    }
  );
  return consulta;
}

async function ejecutarConsulta(idProyecto: number): Promise<TallerResumen[]> {
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

/**
 * No guarda resultados: comparte únicamente las operaciones simultáneas.
 * StrictMode puede montar dos veces un efecto de desarrollo; ambas ejecuciones
 * deben esperar la misma sincronización, no enviar dos POST a la plataforma.
 */
const sincronizacionesEnCurso = new Map<number, Promise<number>>();
const consultasEnCurso = new Map<number, Promise<TallerResumen[]>>();
const metadatosEnCurso = new Map<number, Promise<TalleresMetadatos>>();

/** Esperar, si existe, la sincronización iniciada desde el detalle del proyecto. */
export async function esperarSincronizacionEnCurso(idProyecto: number): Promise<void> {
  const sincronizacion = sincronizacionesEnCurso.get(idProyecto);
  if (sincronizacion !== undefined) await sincronizacion;
}

export function sincronizarTalleres(idProyecto: number): Promise<number> {
  const sincronizacionActual = sincronizacionesEnCurso.get(idProyecto);
  if (sincronizacionActual !== undefined) return sincronizacionActual;

  const sincronizacion = ejecutarSincronizacion(idProyecto);
  sincronizacionesEnCurso.set(idProyecto, sincronizacion);
  void sincronizacion.then(
    function finalizarCorrectamente() {
      if (sincronizacionesEnCurso.get(idProyecto) === sincronizacion) {
        sincronizacionesEnCurso.delete(idProyecto);
      }
    },
    function finalizarConError() {
      if (sincronizacionesEnCurso.get(idProyecto) === sincronizacion) {
        sincronizacionesEnCurso.delete(idProyecto);
      }
    }
  );
  return sincronizacion;
}

async function ejecutarSincronizacion(idProyecto: number): Promise<number> {
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


/** Resumen persistido en el proyecto: no depende de los filtros de la tabla. */
export interface TalleresMetadatos {
  estadoProyecto: string;
  ultimaSincronizacion: string | null;
  cantidadTalleresPlataforma: number;
}

export function obtenerMetadatosTalleres(idProyecto: number): Promise<TalleresMetadatos> {
  const consultaActual = metadatosEnCurso.get(idProyecto);
  if (consultaActual !== undefined) return consultaActual;

  const consulta = ejecutarConsultaMetadatos(idProyecto);
  metadatosEnCurso.set(idProyecto, consulta);
  void consulta.then(
    function finalizarCorrectamente() {
      if (metadatosEnCurso.get(idProyecto) === consulta) metadatosEnCurso.delete(idProyecto);
    },
    function finalizarConError() {
      if (metadatosEnCurso.get(idProyecto) === consulta) metadatosEnCurso.delete(idProyecto);
    }
  );
  return consulta;
}

async function ejecutarConsultaMetadatos(idProyecto: number): Promise<TalleresMetadatos> {
  const respuesta = await fetch(`/api/proyectos/${idProyecto}/talleres/metadatos`, {
    credentials: 'include'
  });
  await verificarSesionEnRespuesta(respuesta);
  if (respuesta.status === 403) throw new Error('No tenés permiso para consultar el proyecto.');
  if (!respuesta.ok) throw new Error('No se pudieron consultar los datos de sincronización.');
  const metadatos: TalleresMetadatos = await respuesta.json();
  return metadatos;
}
