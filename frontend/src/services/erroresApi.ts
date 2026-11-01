
export class SesionExpiradaError extends Error {

  constructor() {
    super('Tu sesión finalizó. Iniciá sesión nuevamente para continuar.');

    this.name = 'SesionExpiradaError';
  }
}


export async function verificarSesionEnRespuesta(
  respuesta: Response
): Promise<void> {

  // Una respuesta 401 indica que no hay sesión autenticada.
  if (respuesta.status === 401) {
    throw new SesionExpiradaError();
  }

  // Solo necesitamos comprobar la sesión ante un 403.
  if (respuesta.status !== 403) {
    return;
  }

  // Un 403 puede corresponder a permisos o a CSRF.
  // Consultamos la sesión para distinguir estos casos.
  let respuestaSesion: Response;

  try {
    respuestaSesion = await fetch('/api/auth/me', {
      method: 'GET',
      credentials: 'include'
    });

  } catch {
    // Si falla la conexión, no podemos asegurar
    // que la sesión haya expirado.
    return;
  }

  if (respuestaSesion.status === 401) {
    throw new SesionExpiradaError();
  }
}
