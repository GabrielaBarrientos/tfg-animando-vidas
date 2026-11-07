import { obtenerTokenCsrf } from './csrfApi';
import { verificarSesionEnRespuesta } from './erroresApi';

export interface UsuarioActual {
  subject: string;
  name: string;
  email: string;
  rol: 'ADMINISTRADOR' | 'OPERATIVO' | 'EXTERNO';
}

export async function obtenerUsuarioActual(): Promise<UsuarioActual> {

  const respuesta = await fetch('/api/auth/me', {
    method: 'GET',
    credentials: 'include'
  });

  await verificarSesionEnRespuesta(respuesta);

  if (!respuesta.ok) {
    throw new Error('No se pudo obtener el usuario autenticado');
  }

  const usuario: UsuarioActual = await respuesta.json();

  return usuario;
}

// Cierra la sesion del backend, incluida la cookie de sesion.
// El token CSRF es obligatorio para el POST de Spring Security.
export async function cerrarSesionEnServidor(): Promise<void> {
  const csrf = await obtenerTokenCsrf();

  const respuesta = await fetch('/api/auth/logout', {
    method: 'POST',
    credentials: 'include',
    headers: {
      [csrf.headerName]: csrf.token
    }
  });

  if (!respuesta.ok) {
    await verificarSesionEnRespuesta(respuesta);
    throw new Error('No se pudo cerrar la sesión. Intentá nuevamente.');
  }
}
