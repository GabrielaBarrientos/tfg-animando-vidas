import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import DeleteOutlinedIcon from '@mui/icons-material/DeleteOutlined';
import {
  Alert, Box, Breadcrumbs, Button, Chip, CircularProgress, Dialog,
  DialogActions, DialogContent, DialogContentText, DialogTitle,
  IconButton, Link, MenuItem, Paper, Snackbar, Step, StepButton,
  StepLabel, Stepper, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, TextField, Typography
} from '@mui/material';

import { eliminarDocente, guardarDocente, listarDocentes,
  type DatosDocente, type Docente } from '../services/docentesApi';
import { SesionExpiradaError } from '../services/erroresApi';
import type { Proyecto } from '../services/proyectosApi';
import { colores } from '../theme';
import { generarUsuarioPlataforma, formatoUsuarioValido } from '../services/generarUsuarioPlataforma';

interface DocentesProyectoPageProps {
  proyecto: Proyecto;
  puedeGestionar: boolean;
  enFlujoAlta: boolean;
  onVolverAlDetalle: () => void;
  onIrADatosGenerales: () => void;
  onIrAPlanificacion: () => void;
  onIrASeguimiento: () => void;
  onFinalizar: (sinDocentes: boolean) => void;
  onVolverAlListado: () => void;
  onSesionExpirada: () => void;
}

const formularioVacio: DatosDocente = {
  nombres: '', apellidos: '', email: '', telefono: '', pais: '', provincia: '', centroEducativo: '',
  cargo: '', grado: '', materia: '', cargaHoraria: 0, cantidadAlumnos: 0, usuarioPlataforma: ''
};

function validarFormulario(datos: DatosDocente): string {
  if (datos.nombres.trim() === '' || datos.apellidos.trim() === ''
      || datos.email.trim() === '' || datos.telefono.trim() === '' || datos.pais.trim() === ''
      || datos.provincia.trim() === '' || datos.centroEducativo.trim() === ''
      || datos.cargo.trim() === '' || datos.grado.trim() === '' || datos.materia.trim() === '') {
    return 'Todos los campos son obligatorios.';
  }
  if (datos.nombres.length > 200 || datos.apellidos.length > 200
      || datos.email.length > 320 || datos.pais.length > 100
      || datos.provincia.length > 150 || datos.centroEducativo.length > 200
      || datos.telefono.length > 30 || datos.cargo.length > 150
      || datos.grado.length > 100 || datos.materia.length > 150) {
    return 'Uno de los campos excede la longitud máxima permitida.';
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(datos.email.trim())) {
    return 'Ingresá un correo electrónico válido.';
  }
  if (!Number.isInteger(datos.cargaHoraria) || datos.cargaHoraria <= 0
      || !Number.isInteger(datos.cantidadAlumnos) || datos.cantidadAlumnos <= 0) {
    return 'Carga horaria y alcance alumnos deben ser enteros mayores que cero.';
  }
  if (!formatoUsuarioValido(datos.usuarioPlataforma)) {
    return 'El usuario debe tener mínimo 8 caracteres, solo letras minúsculas y al menos un punto.';
  }
  return '';
}

function DocentesProyectoPage({
  proyecto, puedeGestionar, enFlujoAlta, onVolverAlDetalle,
  onIrADatosGenerales, onIrAPlanificacion, onIrASeguimiento,
  onFinalizar, onVolverAlListado, onSesionExpirada
}: DocentesProyectoPageProps) {
  const [docentes, setDocentes] = useState<Docente[]>([]);
  const [cargando, setCargando] = useState(true);
  const [errorCarga, setErrorCarga] = useState('');
  const [version, setVersion] = useState(0);
  const [busqueda, setBusqueda] = useState('');
  const [filtroPais, setFiltroPais] = useState('');
  const [dialogoFormulario, setDialogoFormulario] = useState(false);
  const [idEditando, setIdEditando] = useState<number | null>(null);
  const [formulario, setFormulario] = useState<DatosDocente>(formularioVacio);
  const [usuarioEditado, setUsuarioEditado] = useState(false);
  const [errorFormulario, setErrorFormulario] = useState('');
  const [docenteAEliminar, setDocenteAEliminar] = useState<Docente | null>(null);
  const [procesando, setProcesando] = useState(false);
  const [notificacion, setNotificacion] = useState('');
  const [tipoNotificacion, setTipoNotificacion] = useState<'success' | 'error'>('success');

  useEffect(function cargarDocentes() {
    let activo = true;
    async function consultar() {
      try {
        const resultado = await listarDocentes(proyecto.idProyecto);
        if (activo) {
          setDocentes(resultado);
          setErrorCarga('');
        }
      } catch (excepcion) {
        if (!activo) {
          return;
        }
        if (excepcion instanceof SesionExpiradaError) {
          onSesionExpirada();
          return;
        }
        setErrorCarga('No se pudieron cargar los docentes del proyecto.');
      } finally {
        if (activo) {
          setCargando(false);
        }
      }
    }
    void consultar();
    return function cancelarConsulta() {
      activo = false;
    };
  }, [proyecto.idProyecto, version, onSesionExpirada]);

  function reintentar() {
    setCargando(true);
    setVersion(version + 1);
  }

  function avisar(mensaje: string, tipo: 'success' | 'error') {
    setNotificacion(mensaje);
    setTipoNotificacion(tipo);
  }

  function cerrarNotificacion() {
    setNotificacion('');
  }

  function cambiarCampo(campo: keyof DatosDocente, valor: string) {
    setFormulario(function actualizar(actuales) {
      const siguientes = { ...actuales, [campo]: valor };
      if (campo === 'usuarioPlataforma') {
        return siguientes;
      }
      if (!usuarioEditado && (campo === 'nombres' || campo === 'apellidos')) {
        siguientes.usuarioPlataforma = generarUsuarioPlataforma(siguientes.nombres, siguientes.apellidos);
      }
      return siguientes;
    });
    if (campo === 'usuarioPlataforma') setUsuarioEditado(true);
    setErrorFormulario('');
  }

  function cambiarEntero(campo: 'cargaHoraria' | 'cantidadAlumnos', valor: string) {
    setFormulario(function actualizar(actuales) {
      return { ...actuales, [campo]: valor === '' ? 0 : Number(valor) };
    });
    setErrorFormulario('');
  }

  function regenerarUsuario() {
    setFormulario(function actualizar(actuales) {
      return { ...actuales, usuarioPlataforma: generarUsuarioPlataforma(actuales.nombres, actuales.apellidos) };
    });
    setUsuarioEditado(false);
  }

  function abrirAlta() {
    setIdEditando(null);
    setFormulario({ ...formularioVacio });
    setUsuarioEditado(false);
    setErrorFormulario('');
    setDialogoFormulario(true);
  }

  function abrirEdicion(docente: Docente) {
    setIdEditando(docente.idDocente);
    setFormulario({
      nombres: docente.nombres,
      apellidos: docente.apellidos,
      email: docente.email,
      telefono: docente.telefono,
      cargo: docente.cargo,
      grado: docente.grado,
      materia: docente.materia,
      cargaHoraria: docente.cargaHoraria,
      cantidadAlumnos: docente.cantidadAlumnos,
      usuarioPlataforma: docente.usuarioPlataforma,
      pais: docente.pais,
      provincia: docente.provincia,
      centroEducativo: docente.centroEducativo
    });
    setUsuarioEditado(true);
    setErrorFormulario('');
    setDialogoFormulario(true);
  }

  function cerrarFormulario() {
    if (!procesando) {
      setDialogoFormulario(false);
    }
  }

  async function enviarFormulario(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    const errorValidacion = validarFormulario(formulario);
    if (errorValidacion !== '') {
      setErrorFormulario(errorValidacion);
      return;
    }
    setProcesando(true);
    try {
      await guardarDocente(proyecto.idProyecto, idEditando, formulario);
      setDialogoFormulario(false);
      avisar(idEditando === null ? 'Docente registrado correctamente.'
        : 'Docente actualizado correctamente.', 'success');
      setVersion(function incrementar(valor) { return valor + 1; });
    } catch (excepcion) {
      if (excepcion instanceof SesionExpiradaError) {
        onSesionExpirada();
        return;
      }
      if (excepcion instanceof Error) {
        setErrorFormulario(excepcion.message);
      } else {
        setErrorFormulario('No se pudo guardar el docente.');
      }
    } finally {
      setProcesando(false);
    }
  }

  function solicitarEliminacion(docente: Docente) {
    if (docente.tieneTalleresAsociados) {
      avisar('No se puede eliminar el docente porque tiene talleres asociados.', 'error');
      return;
    }
    setDocenteAEliminar(docente);
  }

  function cancelarEliminacion() {
    if (!procesando) {
      setDocenteAEliminar(null);
    }
  }

  async function confirmarEliminacion() {
    if (docenteAEliminar === null) {
      return;
    }
    setProcesando(true);
    try {
      await eliminarDocente(proyecto.idProyecto, docenteAEliminar.idDocente);
      setDocenteAEliminar(null);
      avisar('Docente eliminado correctamente.', 'success');
      setVersion(function incrementar(valor) { return valor + 1; });
    } catch (excepcion) {
      if (excepcion instanceof SesionExpiradaError) {
        onSesionExpirada();
        return;
      }
      setDocenteAEliminar(null);
      if (excepcion instanceof Error) {
        avisar(excepcion.message, 'error');
      } else {
        avisar('No se pudo eliminar el docente.', 'error');
      }
      setVersion(function incrementar(valor) { return valor + 1; });
    } finally {
      setProcesando(false);
    }
  }

  function finalizar() {
    if (!cargando && errorCarga === '' && !procesando) {
      onFinalizar(docentes.length === 0);
    }
  }

  function cambiarBusqueda(evento: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) {
    setBusqueda(evento.target.value);
  }

  function cambiarPais(evento: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) {
    setFiltroPais(evento.target.value);
  }

  function limpiarFiltros() {
    setBusqueda('');
    setFiltroPais('');
  }

  const paises: string[] = [];
  const docentesFiltrados: Docente[] = [];
  for (const docente of docentes) {
    if (!paises.includes(docente.pais)) {
      paises.push(docente.pais);
    }
    const texto = (docente.nombres + ' ' + docente.apellidos + ' '
      + docente.centroEducativo + ' ' + docente.usuarioPlataforma + ' ' + docente.email).toLocaleLowerCase('es');
    if (texto.includes(busqueda.trim().toLocaleLowerCase('es'))
        && (filtroPais === '' || docente.pais === filtroPais)) {
      docentesFiltrados.push(docente);
    }
  }
  paises.sort(function comparar(a, b) { return a.localeCompare(b, 'es'); });

  return (
    <Box component="section" sx={{ width: '100%', boxSizing: 'border-box', px: { xs: 2, md: 4 }, py: 2 }}>
      <Breadcrumbs separator={<ChevronRightIcon sx={{ fontSize: 16 }} />} sx={{ mb: 2 }}>
        <Link component="button" type="button" underline="hover" onClick={onVolverAlListado}
          sx={{ color: '#667085', '&:hover': { color: '#667085' } }}>Inicio</Link>
        <Link component="button" type="button" underline="hover" onClick={onVolverAlListado}
          sx={{ color: '#667085', '&:hover': { color: '#667085' } }}>Proyectos</Link>
        {enFlujoAlta ? (
          <Typography title={proyecto.nombreCompleto} sx={{ color: colores.textoSecundario,
            maxWidth: 400, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {proyecto.nombreCompleto}
          </Typography>
        ) : (
          <Link component="button" type="button" underline="hover" onClick={onVolverAlDetalle}
            title={proyecto.nombreCompleto}
            sx={{ color: '#667085', maxWidth: 400, overflow: 'hidden',
              textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {proyecto.nombreCompleto}
          </Link>
        )}
        <Typography sx={{ color: colores.verdeOscuro }}>Docentes</Typography>
      </Breadcrumbs>

      <Box sx={{ display: 'flex', alignItems: 'center', flexWrap: 'wrap', gap: 2, mb: 1.5 }}>
        <Typography component="h1" variant="h5">{proyecto.nombreCompleto}</Typography>
        <Chip label={proyecto.estado === 'PLANIFICADO' ? 'Planificado' : 'Pendiente'}
          sx={{ backgroundColor: proyecto.estado === 'PLANIFICADO'
            ? colores.chipVerdeClaro : colores.chipAmarillo,
            color: colores.textoPrimario, borderRadius: '6px', height: 32,
            fontSize: 14, fontWeight: 500 }} />
      </Box>

      {puedeGestionar && enFlujoAlta && (
        <Paper variant="outlined" sx={{ mb: 2, minHeight: 48, px: { xs: 1, sm: 2, md: 3 },
          py: { xs: 1.5, sm: 1 }, display: 'flex', alignItems: 'center', borderRadius: 2 }}>
          <Stepper activeStep={3} sx={{ width: '100%', minWidth: 0,
            '& .MuiStep-root': { minWidth: 0, px: { xs: 0.25, md: 1 } },
            '& .MuiStepLabel-label': { fontSize: { xs: 10, sm: 12, md: 14 }, whiteSpace: 'normal' },
            '& .MuiStepButton-root': { minHeight: 32, width: '100%', minWidth: 0 } }}>
            <Step completed><StepButton onClick={onIrADatosGenerales}>Datos generales</StepButton></Step>
            <Step completed><StepButton onClick={onIrAPlanificacion}>Planificación</StepButton></Step>
            <Step completed><StepButton onClick={onIrASeguimiento}>Seguimiento</StepButton></Step>
            <Step><StepLabel>Docentes</StepLabel></Step>
          </Stepper>
        </Paper>
      )}

      <Paper elevation={0} sx={{ px: { xs: 2, md: 4 }, py: 2, borderRadius: 1,
        minHeight: 'calc(100vh - 270px)' }}>
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 2,
          flexWrap: 'wrap', mb: 2 }}>
          <Box>
            <Typography component="h2" variant="h6">Gestión de docentes</Typography>
            <Typography variant="body1" sx={{ color: colores.textoSecundario }}>
              {puedeGestionar
                ? 'Registrá los docentes que participan en el proyecto. '
                  + 'Esta información se utilizará posteriormente para vincular los talleres de la plataforma.'
                : 'Consultá los docentes del proyecto asignado.'}
            </Typography>
          </Box>
        </Box>

        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2, mb: 2 }}>
          <TextField size="small" label="Buscar docente" value={busqueda} onChange={cambiarBusqueda}
            sx={{ width: 500, maxWidth: '100%' }} />
          <TextField size="small" select label="País" value={filtroPais} onChange={cambiarPais}
            sx={{ width: 320, maxWidth: '100%' }}>
            <MenuItem value="">Todos</MenuItem>
            {paises.map(function mostrarPais(pais) {
              return <MenuItem key={pais} value={pais}>{pais}</MenuItem>;
            })}
          </TextField>
          <Button variant="outlined" onClick={limpiarFiltros}
            disabled={busqueda === '' && filtroPais === ''}
            sx={{ ml: { md: 'auto' } }}>
            Limpiar filtros
          </Button>
          {puedeGestionar && (
            <Button variant="contained" onClick={abrirAlta} disabled={cargando || errorCarga !== ''}>
              Nuevo docente
            </Button>
          )}
        </Box>

        {cargando && <Box sx={{ py: 5, textAlign: 'center' }}><CircularProgress /></Box>}
        {errorCarga !== '' && (
          <Alert severity="error" sx={{ mb: 2 }} action={<Button color="inherit" onClick={reintentar}>Reintentar</Button>}>
            {errorCarga}
          </Alert>
        )}
        {!cargando && errorCarga === '' && (
          <TableContainer sx={{ width: '100%', overflowX: 'auto' }}>
            <Table size="small" aria-label="Docentes del proyecto" sx={{ minWidth: 2100 }}>
              <TableHead>
                <TableRow>
                  <TableCell>País</TableCell>
                  <TableCell>Centro educativo</TableCell>
                  <TableCell>Provincia/Región/Departamento</TableCell>
                  <TableCell>Nombres y apellidos</TableCell>
                  <TableCell>Cargo</TableCell>
                  <TableCell>Celular</TableCell>
                  <TableCell>Correo electrónico</TableCell>
                  <TableCell>Grado</TableCell>
                  <TableCell align="right">Alcance alumnos</TableCell>
                  <TableCell>Materia</TableCell>
                  <TableCell align="right">Carga horaria</TableCell>
                  <TableCell>Usuario de plataforma</TableCell>
                  {puedeGestionar && <TableCell align="right">Acciones</TableCell>}
                </TableRow>
              </TableHead>
              <TableBody>
                {docentesFiltrados.map(function mostrarDocente(docente) {
                  return (
                    <TableRow key={docente.idDocente} hover>
                      <TableCell>{docente.pais}</TableCell>
                      <TableCell>{docente.centroEducativo}</TableCell>
                      <TableCell>{docente.provincia}</TableCell>
                      <TableCell>{docente.nombres} {docente.apellidos}</TableCell>
                      <TableCell>{docente.cargo}</TableCell>
                      <TableCell>{docente.telefono}</TableCell>
                      <TableCell>{docente.email}</TableCell>
                      <TableCell>{docente.grado}</TableCell>
                      <TableCell align="right">{docente.cantidadAlumnos}</TableCell>
                      <TableCell>{docente.materia}</TableCell>
                      <TableCell align="right">{docente.cargaHoraria}</TableCell>
                      <TableCell>{docente.usuarioPlataforma}</TableCell>
                      {puedeGestionar && (
                        <TableCell align="right" sx={{ whiteSpace: 'nowrap' }}>
                          <IconButton aria-label={'Modificar docente ' + docente.apellidos}
                            onClick={function editar() { abrirEdicion(docente); }}>
                            <EditOutlinedIcon />
                          </IconButton>
                          <IconButton aria-label={'Eliminar docente ' + docente.apellidos}
                            onClick={function eliminar() { solicitarEliminacion(docente); }}>
                            <DeleteOutlinedIcon />
                          </IconButton>
                        </TableCell>
                      )}
                    </TableRow>
                  );
                })}
                {docentesFiltrados.length === 0 && (
                  <TableRow><TableCell colSpan={puedeGestionar ? 13 : 12}>
                    {docentes.length === 0 ? 'El proyecto no tiene docentes registrados.'
                      : 'No se encontraron docentes con los filtros seleccionados.'}
                  </TableCell></TableRow>
                )}
              </TableBody>
            </Table>
          </TableContainer>
        )}
        <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 2, mt: 3 }}>
          <Button variant="text" onClick={enFlujoAlta ? onVolverAlListado : onVolverAlDetalle}>
            {enFlujoAlta ? 'Volver a proyectos' : 'VOLVER'}
          </Button>
          {puedeGestionar && enFlujoAlta && (
            <Button variant="contained" onClick={finalizar} disabled={cargando || errorCarga !== '' || procesando}>
              Finalizar
            </Button>
          )}
        </Box>
      </Paper>

      <Dialog open={dialogoFormulario} onClose={cerrarFormulario} fullWidth maxWidth="md">
        <Box component="form" onSubmit={enviarFormulario} noValidate>
          <DialogTitle>{idEditando === null ? 'Registrar docente' : 'Modificar docente'}</DialogTitle>
          <DialogContent sx={{ display: 'grid',
            gridTemplateColumns: { xs: 'minmax(0, 1fr)', md: 'repeat(2, minmax(0, 1fr))' },
            gap: 4, '&&': { pt: 2 } }}>
            <TextField label="Nombres" required value={formulario.nombres} fullWidth size="small"
              onChange={function cambiar(e) { cambiarCampo('nombres', e.target.value); }} />
            <TextField label="Apellidos" required value={formulario.apellidos} fullWidth size="small"
              onChange={function cambiar(e) { cambiarCampo('apellidos', e.target.value); }} />
            <TextField label="Correo electrónico" required value={formulario.email} fullWidth size="small"
              onChange={function cambiar(e) { cambiarCampo('email', e.target.value); }} />
            <TextField label="País" required value={formulario.pais} fullWidth size="small"
              onChange={function cambiar(e) { cambiarCampo('pais', e.target.value); }} />
            <TextField label="Provincia/Región/Departamento" placeholder="Provincia/Región/Departamento"
              required value={formulario.provincia} fullWidth size="small"
              onChange={function cambiar(e) { cambiarCampo('provincia', e.target.value); }} />
            <TextField label="Centro educativo" required value={formulario.centroEducativo} fullWidth size="small"
              onChange={function cambiar(e) { cambiarCampo('centroEducativo', e.target.value); }} />
            <TextField label="Celular" required value={formulario.telefono} fullWidth size="small"
              onChange={function cambiar(e) { cambiarCampo('telefono', e.target.value); }} />
            <TextField label="Cargo" required value={formulario.cargo} fullWidth size="small"
              onChange={function cambiar(e) { cambiarCampo('cargo', e.target.value); }} />
            <TextField label="Grado" required value={formulario.grado} fullWidth size="small"
              onChange={function cambiar(e) { cambiarCampo('grado', e.target.value); }} />
            <TextField label="Materia" required value={formulario.materia} fullWidth size="small"
              onChange={function cambiar(e) { cambiarCampo('materia', e.target.value); }} />
            <TextField label="Carga horaria (horas cátedra)" required type="number" value={formulario.cargaHoraria || ''}
              fullWidth size="small" slotProps={{ htmlInput: { min: 1, step: 1 } }}
              onChange={function cambiar(e) { cambiarEntero('cargaHoraria', e.target.value); }} />
            <TextField label="Alcance alumnos" required type="number" value={formulario.cantidadAlumnos || ''}
              fullWidth size="small" slotProps={{ htmlInput: { min: 1, step: 1 } }}
              onChange={function cambiar(e) { cambiarEntero('cantidadAlumnos', e.target.value); }} />
            <Box sx={{ display: 'flex', gridColumn: '1 / -1', alignItems: 'start', gap: 1 }}>
              <TextField label="Usuario de plataforma" required value={formulario.usuarioPlataforma} fullWidth size="small"
                helperText="Mínimo 8 caracteres; letras y puntos, sin números. Siempre editable."
                onChange={function cambiar(e) { cambiarCampo('usuarioPlataforma', e.target.value); }} />
              <Button variant="outlined" type="button" onClick={regenerarUsuario}>Generar</Button>
            </Box>
            {errorFormulario !== '' && <Alert severity="error" sx={{ gridColumn: '1 / -1' }}>{errorFormulario}</Alert>}
          </DialogContent>
          <DialogActions sx={{ p: 3 }}>
            <Button onClick={cerrarFormulario} disabled={procesando}>Cancelar</Button>
            <Button variant="contained" type="submit" disabled={procesando}>
              {procesando ? 'Guardando...' : 'Guardar'}
            </Button>
          </DialogActions>
        </Box>
      </Dialog>

      <Dialog open={docenteAEliminar !== null} onClose={cancelarEliminacion}>
        <DialogTitle>Eliminar docente</DialogTitle>
        <DialogContent><DialogContentText>
          ¿Confirmás la eliminación del docente {docenteAEliminar?.nombres} {docenteAEliminar?.apellidos}?
        </DialogContentText></DialogContent>
        <DialogActions>
          <Button onClick={cancelarEliminacion} disabled={procesando}>Cancelar</Button>
          <Button color="error" variant="contained" onClick={confirmarEliminacion} disabled={procesando}>
            Eliminar
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar open={notificacion !== ''} anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
        autoHideDuration={tipoNotificacion === 'success' ? 5000 : null}
        onClose={cerrarNotificacion}>
        <Alert severity={tipoNotificacion} variant="filled" onClose={cerrarNotificacion}
          sx={{ color: '#FFFFFF',
            backgroundColor: tipoNotificacion === 'success' ? colores.verdeToastExito : undefined,
            '& .MuiAlert-icon': { color: '#FFFFFF' } }}>
          {notificacion}
        </Alert>
      </Snackbar>
    </Box>
  );
}

export default DocentesProyectoPage;
