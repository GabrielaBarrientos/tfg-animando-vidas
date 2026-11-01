
import { obtenerTokenCsrf } from './csrfApi';
import { verificarSesionEnRespuesta } from './erroresApi';

// Frecuencias permitidas por el backend.
export type FrecuenciaClases =
  | 'SEMANAL'
  | 'QUINCENAL'
  | 'MENSUAL';

// Datos enviados al guardar una planificación.
export interface GuardarPlanificacionRequest {
  fechaInicio: string;
  fechaFinalizacion: string;
  cantidadClases: number;
  frecuenciaClases: FrecuenciaClases;
}

// Datos devueltos por el backend.
export interface PlanificacionProyecto {
  idPlanificacionProyecto: number;
  idProyecto: number;
  fechaInicio: string;
  fechaFinalizacion: string;
  cantidadClases: number;
  frecuenciaClases: FrecuenciaClases;
  duracionMeses: number;
}

// Consultar la planificación de un proyecto.
export async function obtenerPlanificacionProyecto(idProyecto: number): Promise<PlanificacionProyecto | null> {

  const respuesta = await fetch(`/api/proyectos/${idProyecto}/planificacion`,
    {
      method: 'GET',
      credentials: 'include'
    }
  );

  await verificarSesionEnRespuesta(respuesta);

  // Un proyecto nuevo puede no tener planificación.
  if (respuesta.status === 404) {
    return null;
  }

  if (!respuesta.ok) {
    throw new Error('No se pudo obtener la planificación del proyecto'
    );
  }

  const planificacion: PlanificacionProyecto = await respuesta.json();

  return planificacion;
}

// Registrar o actualizar la planificación.
export async function guardarPlanificacionProyecto(
  idProyecto: number,
  datos: GuardarPlanificacionRequest
): Promise<PlanificacionProyecto> {

  const csrf = await obtenerTokenCsrf();

  const cabeceras = new Headers();

  cabeceras.set('Content-Type', 'application/json');
  cabeceras.set(csrf.headerName, csrf.token);

  const respuesta = await fetch(`/api/proyectos/${idProyecto}/planificacion`,
    {
      method: 'PUT',
      credentials: 'include',
      headers: cabeceras,
      body: JSON.stringify(datos)
    }
  );

  await verificarSesionEnRespuesta(respuesta);

  if (!respuesta.ok) {
    throw new Error('No se pudo guardar la planificación del proyecto'
    );
  }

  const planificacion: PlanificacionProyecto = await respuesta.json();

  return planificacion;
}
