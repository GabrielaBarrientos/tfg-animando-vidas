
export interface Empresa {
  idEmpresa: number;
  nombre: string;
}

export async function obtenerEmpresas(): Promise<Empresa[]> {

  const respuesta = await fetch('/api/empresas', {
    method: 'GET',
    credentials: 'include'
  });

  if (!respuesta.ok) {
    throw new Error('No se pudo obtener el listado de empresas');
  }

  const empresas: Empresa[] = await respuesta.json();

  return empresas;
}
