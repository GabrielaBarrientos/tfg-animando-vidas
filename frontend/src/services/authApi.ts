
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

  if (!respuesta.ok) {
    throw new Error('No se pudo obtener el usuario autenticado');
  }

  const usuario: UsuarioActual = await respuesta.json();

  return usuario;
}
