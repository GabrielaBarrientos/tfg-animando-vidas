/** Sugerencia local; la validación definitiva de formato y unicidad ocurre en Spring Boot. */
function primeraPalabra(texto: string): string {
  const normalizado = texto.trim().toLocaleLowerCase('es').normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '');
  const primera = normalizado.split(/\s+/)[0];
  return primera === undefined ? '' : primera.replace(/[^a-z]/g, '');
}

export function generarUsuarioPlataforma(nombres: string, apellidos: string): string {
  const nombre = primeraPalabra(nombres);
  const apellido = primeraPalabra(apellidos);
  if (nombre === '' || apellido === '') return '';
  let prefijo = nombre.charAt(0);
  let posicion = 1;
  while (prefijo.length + 1 + apellido.length < 8) {
    if (posicion >= nombre.length) posicion = 0;
    prefijo += nombre.charAt(posicion);
    posicion++;
  }
  return `${prefijo}.${apellido}`;
}

export function formatoUsuarioValido(usuario: string): boolean {
  return usuario.length >= 8 && usuario.length <= 150 && /^[a-z]+(\.[a-z]+)+$/.test(usuario);
}
