import { obtenerTokenCsrf } from './csrfApi';
import { verificarSesionEnRespuesta } from './erroresApi';

export type DiaInformeSemanal =
  | 'LUNES' | 'MARTES' | 'MIERCOLES' | 'JUEVES'
  | 'VIERNES' | 'SABADO' | 'DOMINGO';

export type TipoDiaInformeMensual = 'DIA_DEL_MES' | 'ULTIMO_DIA_HABIL';

export interface ResponsableElegible {
  idUsuario: number;
  nombres: string;
  apellidos: string;
  email: string;
  rol: 'ADMINISTRADOR' | 'OPERATIVO';
}

export interface GuardarSeguimientoRequest {
  umbralProximoAtrasoSemanas: number;
  umbralAtrasoSemanas: number;
  idResponsableSeguimiento: number;
  diaInformeSemanal: DiaInformeSemanal;
  idResponsableProyecto: number;
  tipoDiaInformeMensual: TipoDiaInformeMensual;
  diaInformeMensual: number | null;
}

export interface ConfiguracionSeguimiento extends GuardarSeguimientoRequest {
  configurada: true;
  idConfiguracionSeguimiento: number;
  idProyecto: number;
  estadoProyecto: string;
}

interface ConfiguracionSeguimientoPendiente {
  configurada: false;
  idConfiguracionSeguimiento: null;
  idProyecto: number;
  umbralProximoAtrasoSemanas: null;
  umbralAtrasoSemanas: null;
  idResponsableSeguimiento: null;
  diaInformeSemanal: null;
  idResponsableProyecto: null;
  tipoDiaInformeMensual: null;
  diaInformeMensual: null;
  estadoProyecto: string;
}

type ConsultaConfiguracionSeguimientoResponse =
  ConfiguracionSeguimiento | ConfiguracionSeguimientoPendiente;

export async function obtenerResponsablesElegibles(idProyecto: number): Promise<ResponsableElegible[]> {
  const respuesta = await fetch(`/api/proyectos/${idProyecto}/responsables-elegibles`, {
    method: 'GET',
    credentials: 'include'
  });

  await verificarSesionEnRespuesta(respuesta);

  if (!respuesta.ok) {
    throw new Error('No se pudieron consultar los responsables disponibles.');
  }

  const usuarios: ResponsableElegible[] = await respuesta.json();
  return usuarios;
}

export async function obtenerConfiguracionSeguimiento(idProyecto: number): Promise<ConfiguracionSeguimiento | null> {
  const respuesta = await fetch(`/api/proyectos/${idProyecto}/seguimiento`, {
    method: 'GET',
    credentials: 'include'
  });

  await verificarSesionEnRespuesta(respuesta);

  // Un 404 significa que el proyecto no existe; no representa seguimiento pendiente.
  if (!respuesta.ok) {
    throw new Error('No se pudo consultar la configuración del seguimiento.');
  }

  const configuracion: ConsultaConfiguracionSeguimientoResponse = await respuesta.json();
  // La pantalla conserva sus valores iniciales cuando aún no hay configuración persistida.
  if (!configuracion.configurada) {
    return null;
  }

  return configuracion;
}

export async function guardarConfiguracionSeguimiento(
  idProyecto: number,
  datos: GuardarSeguimientoRequest
): Promise<ConfiguracionSeguimiento> {
  const csrf = await obtenerTokenCsrf();
  const cabeceras = new Headers();
  cabeceras.set('Content-Type', 'application/json');
  cabeceras.set(csrf.headerName, csrf.token);

  const respuesta = await fetch(`/api/proyectos/${idProyecto}/seguimiento`, {
    method: 'PUT',
    credentials: 'include',
    headers: cabeceras,
    body: JSON.stringify(datos)
  });

  await verificarSesionEnRespuesta(respuesta);

  if (!respuesta.ok) {
    if (respuesta.status === 400) {
      throw new Error('Los datos de seguimiento no son válidos. Revisá los campos y responsables.');
    }
    throw new Error('No se pudo guardar la configuración del seguimiento.');
  }

  const configuracion: ConfiguracionSeguimiento = await respuesta.json();
  return configuracion;
}
