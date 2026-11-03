import { verificarSesionEnRespuesta } from './erroresApi';
import type { Proyecto } from './proyectosApi';

export interface PlanificacionResumen {
  fechaInicio: string;
  fechaFinalizacion: string;
  cantidadClases: number;
  frecuenciaClases: string;
  duracionMeses: number;
}

export interface SeguimientoResumen {
  umbralProximoAtrasoSemanas: number;
  umbralAtrasoSemanas: number;
  responsableSeguimiento: string;
  diaInformeSemanal: string;
  responsableProyecto: string;
  tipoDiaInformeMensual: string;
  diaInformeMensual: number | null;
}

export interface DetalleProyecto {
  proyecto: Proyecto;
  ultimaActualizacion: string;
  planificacion: PlanificacionResumen | null;
  seguimiento: SeguimientoResumen | null;
  cantidadDocentes: number;
}

export async function obtenerDetalleProyecto(idProyecto: number): Promise<DetalleProyecto> {
  const respuesta = await fetch(`/api/proyectos/${idProyecto}/detalle`, {
    method: 'GET',
    credentials: 'include'
  });

  await verificarSesionEnRespuesta(respuesta);

  if (respuesta.status === 403) {
    throw new Error('No tenés permisos para consultar este proyecto.');
  }
  if (respuesta.status === 404) {
    throw new Error('El proyecto no está disponible.');
  }
  if (!respuesta.ok) {
    throw new Error('No se pudo consultar el detalle del proyecto.');
  }

  const detalle: DetalleProyecto = await respuesta.json();
  return detalle;
}
