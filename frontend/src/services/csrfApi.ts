import { verificarSesionEnRespuesta } from './erroresApi';

export interface CsrfToken {
  token: string;
  headerName: string;
  parameterName: string;
}

export async function obtenerTokenCsrf(): Promise<CsrfToken> {

  const respuesta = await fetch('/api/csrf', {
    method: 'GET',
    credentials: 'include'
  });

  await verificarSesionEnRespuesta(respuesta);

  if (!respuesta.ok) {
    throw new Error('No se pudo obtener el token CSRF');
  }

  const datos: CsrfToken = await respuesta.json();

  return datos;
}
