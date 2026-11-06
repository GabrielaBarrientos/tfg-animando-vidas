import { obtenerTokenCsrf } from './csrfApi';
import { verificarSesionEnRespuesta } from './erroresApi';

export type CanalComunicacion = 'WHATSAPP' | 'LLAMADA' | 'CORREO' | 'OTRO';
export type MotivoComunicacion = 'ATRASO_EN_EL_AVANCE' | 'EVIDENCIAS_PENDIENTES' | 'CONSULTA' | 'OTRO';
export type AccionComunicacion = 'RECORDATORIO' | 'RECLAMO' | 'SEGUIMIENTO' | 'CONSULTA';
export type ResultadoComunicacion = 'ESPERANDO_RESPUESTA' | 'RESPUESTA_SIN_COMPROMISO'
  | 'COMPROMISO_DE_REGULARIZACION' | 'NO_SE_PUDO_CONTACTAR';

export interface ComunicacionTaller {
  idComunicacion: number;
  fechaContacto: string;
  canal: CanalComunicacion;
  canalOtro: string | null;
  motivo: MotivoComunicacion;
  motivoOtro: string | null;
  accionRealizada: AccionComunicacion;
  observaciones: string | null;
  resultado: ResultadoComunicacion;
  fechaRespuestaDocente: string | null;
  respuestaDocente: string | null;
  fechaCompromiso: string | null;
  idUsuarioRegistro: number;
  registradoPor: string;
  fechaCreacion: string;
  fechaUltimaModificacion: string;
}

export interface CrearComunicacionRequest {
  fechaContacto: string;
  canal: CanalComunicacion;
  canalOtro: string | null;
  motivo: MotivoComunicacion;
  motivoOtro: string | null;
  accionRealizada: AccionComunicacion;
  resultado: ResultadoComunicacion;
  fechaCompromiso: string | null;
  observaciones: string | null;
}

export interface ActualizarSeguimientoRequest {
  resultado: ResultadoComunicacion;
  fechaRespuestaDocente: string | null;
  respuestaDocente: string | null;
  fechaCompromiso: string | null;
}

export const canales: { valor: CanalComunicacion; etiqueta: string }[] = [
  { valor: 'WHATSAPP', etiqueta: 'WhatsApp' },
  { valor: 'LLAMADA', etiqueta: 'Llamada' },
  { valor: 'CORREO', etiqueta: 'Correo' },
  { valor: 'OTRO', etiqueta: 'Otro' }
];
export const motivos: { valor: MotivoComunicacion; etiqueta: string }[] = [
  { valor: 'ATRASO_EN_EL_AVANCE', etiqueta: 'Atraso en el avance' },
  { valor: 'EVIDENCIAS_PENDIENTES', etiqueta: 'Evidencias pendientes' },
  { valor: 'CONSULTA', etiqueta: 'Consulta' },
  { valor: 'OTRO', etiqueta: 'Otro' }
];
export const acciones: { valor: AccionComunicacion; etiqueta: string }[] = [
  { valor: 'RECORDATORIO', etiqueta: 'Recordatorio' },
  { valor: 'RECLAMO', etiqueta: 'Reclamo' },
  { valor: 'SEGUIMIENTO', etiqueta: 'Seguimiento' },
  { valor: 'CONSULTA', etiqueta: 'Consulta' }
];
export const resultados: { valor: ResultadoComunicacion; etiqueta: string }[] = [
  { valor: 'ESPERANDO_RESPUESTA', etiqueta: 'Esperando respuesta' },
  { valor: 'RESPUESTA_SIN_COMPROMISO', etiqueta: 'Respuesta sin compromiso' },
  { valor: 'COMPROMISO_DE_REGULARIZACION', etiqueta: 'Compromiso de regularización' },
  { valor: 'NO_SE_PUDO_CONTACTAR', etiqueta: 'No se pudo contactar' }
];

export function etiquetaDe<T extends string>(opciones: { valor: T; etiqueta: string }[], valor: T): string {
  for (const opcion of opciones) {
    if (opcion.valor === valor) return opcion.etiqueta;
  }
  return valor;
}

export function mostrarFecha(fecha: string | null): string {
  if (!fecha) return '—';
  const partes = fecha.split('-');
  if (partes.length !== 3) return fecha;
  return `${partes[2]}-${partes[1]}-${partes[0]}`;
}

export function mostrarFechaHora(fecha: string): string {
  const instante = new Date(fecha);
  if (Number.isNaN(instante.getTime())) return '—';
  return new Intl.DateTimeFormat('es-AR', { timeZone: 'America/Argentina/Buenos_Aires',
    day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
    hourCycle: 'h23' }).format(instante);
}

export function hoyLocal(): string {
  const ahora = new Date();
  const local = new Date(ahora.getTime() - ahora.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 10);
}

function ruta(idProyecto: number, idTaller: string): string {
  return `/api/proyectos/${idProyecto}/talleres/${encodeURIComponent(idTaller)}/comunicaciones`;
}

async function interpretarRespuesta<T>(respuesta: Response, mensajeGeneral: string): Promise<T> {
  await verificarSesionEnRespuesta(respuesta);
  if (respuesta.status === 403) throw new Error('No tenés permisos para gestionar las comunicaciones del taller.');
  if (!respuesta.ok) {
    let detalle = '';
    try {
      const datos: { detail?: string; message?: string } = await respuesta.json();
      detalle = datos.detail || datos.message || '';
    } catch {
      // La respuesta puede no incluir JSON.
    }
    if (detalle === '' || detalle === 'Bad Request' || detalle === 'Bad request') {
      throw new Error(mensajeGeneral);
    }
    throw new Error(detalle);
  }
  return respuesta.json() as Promise<T>;
}

export async function listarComunicaciones(idProyecto: number, idTaller: string): Promise<ComunicacionTaller[]> {
  const respuesta = await fetch(ruta(idProyecto, idTaller), { credentials: 'include' });
  return interpretarRespuesta<ComunicacionTaller[]>(respuesta, 'No se pudo consultar el historial de comunicaciones.');
}

export async function crearComunicacion(idProyecto: number, idTaller: string,
  datos: CrearComunicacionRequest): Promise<ComunicacionTaller> {
  const csrf = await obtenerTokenCsrf();
  const respuesta = await fetch(ruta(idProyecto, idTaller), {
    method: 'POST', credentials: 'include',
    headers: { 'Content-Type': 'application/json', [csrf.headerName]: csrf.token },
    body: JSON.stringify(datos)
  });
  return interpretarRespuesta<ComunicacionTaller>(respuesta, 'No se pudo registrar la comunicación.');
}

export async function actualizarSeguimiento(idProyecto: number, idTaller: string, idComunicacion: number,
  datos: ActualizarSeguimientoRequest): Promise<ComunicacionTaller> {
  const csrf = await obtenerTokenCsrf();
  const respuesta = await fetch(`${ruta(idProyecto, idTaller)}/${idComunicacion}`, {
    method: 'PATCH', credentials: 'include',
    headers: { 'Content-Type': 'application/json', [csrf.headerName]: csrf.token },
    body: JSON.stringify(datos)
  });
  return interpretarRespuesta<ComunicacionTaller>(respuesta, 'No se pudo actualizar el seguimiento.');
}
