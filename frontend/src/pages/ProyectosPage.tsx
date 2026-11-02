
import { useEffect, useState } from 'react';

import { SesionExpiradaError } from '../services/erroresApi';

import {
  Alert,
  Box,
  Breadcrumbs,
  Button,
  Chip,
  CircularProgress,
  IconButton,
  InputAdornment,
  MenuItem,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  TextField,
  Typography
} from '@mui/material';

import ChevronRightIcon from '@mui/icons-material/ChevronRight';
import CloseIcon from '@mui/icons-material/Close';

import {
  obtenerProyectos,
  type Proyecto
} from '../services/proyectosApi';

import { colores } from '../theme';

interface ProyectosPageProps {
  puedeCrearProyecto: boolean;
  puedeConsultarDocentes: boolean;
  onNuevoProyecto: () => void;
  onPlanificarProyecto(proyecto: Proyecto): void;
  onConsultarDocentes(proyecto: Proyecto): void;
  onSesionExpirada: () => void;
}

function obtenerNombreEstado(estado: string): string {
  switch (estado) {
    case 'PENDIENTE_PLANIFICACION':
      return 'Pendiente';

    case 'PLANIFICADO':
      return 'Planificado';

    case 'EN_CURSO':
      return 'En curso';

    case 'FINALIZADO':
      return 'Finalizado';

    default:
      return estado;
  }
}

interface AccesoPlanificacionProps {
  proyecto: Proyecto;
  onPlanificarProyecto(proyecto: Proyecto): void;
}

function AccesoPlanificacion({
  proyecto,
  onPlanificarProyecto
}: AccesoPlanificacionProps) {

  function abrirPlanificacion() {
    onPlanificarProyecto(proyecto);
  }

  return (
    <IconButton
      size="small"
      title="Abrir configuración del proyecto"
      aria-label={'Abrir configuración de ' + proyecto.nombre}
      onClick={abrirPlanificacion}
    >
      <ChevronRightIcon
        sx={{ color: colores.textoPrimario }}
      />
    </IconButton>
  );
}

function ProyectosPage({
  puedeCrearProyecto,
  puedeConsultarDocentes,
  onNuevoProyecto,
  onPlanificarProyecto,
  onConsultarDocentes,
  onSesionExpirada
}: ProyectosPageProps) {

  // Datos recuperados del backend.
  const [proyectos, setProyectos] = useState<Proyecto[]>([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');

  // Filtros del listado.
  const [busquedaNombre, setBusquedaNombre] = useState('');
  const [empresaSeleccionada, setEmpresaSeleccionada] = useState('');
  const [anioSeleccionado, setAnioSeleccionado] = useState('');

  // Paginación.
  const [pagina, setPagina] = useState(0);
  const [filasPorPagina, setFilasPorPagina] = useState(10);

  // Consultar los proyectos cuando se abre la pantalla.
  useEffect(function cargarProyectosAlAbrir() {

    let componenteActivo = true;

    async function cargarProyectos() {

      try {
        const proyectosObtenidos = await obtenerProyectos();

        if (componenteActivo) {
          setProyectos(proyectosObtenidos);
        }

      } catch (excepcion) {

        if (!componenteActivo) {
          return;
        }

        if (excepcion instanceof SesionExpiradaError) {
          onSesionExpirada();
          return;
        }

        setError('No se pudo obtener el listado de proyectos.');

      } finally {

        if (componenteActivo) {
          setCargando(false);
        }
      }
    }

    void cargarProyectos();

    function cancelarCarga() {
      componenteActivo = false;
    }

    return cancelarCarga;

  }, [onSesionExpirada]);


  // Obtener las empresas de los proyectos visibles.
  // No consultamos /api/empresas porque ese endpoint
  // está reservado para administradores.
  const empresasDisponibles: {
    idEmpresa: number;
    nombreEmpresa: string;
  }[] = [];

  const empresasEncontradas = new Set<number>();

  for (const proyecto of proyectos) {
    if (!empresasEncontradas.has(proyecto.idEmpresa)) {
      empresasEncontradas.add(proyecto.idEmpresa);

      empresasDisponibles.push({
        idEmpresa: proyecto.idEmpresa,
        nombreEmpresa: proyecto.nombreEmpresa
      });
    }
  }

  empresasDisponibles.sort((a, b) =>
    a.nombreEmpresa.localeCompare(b.nombreEmpresa)
  );

  // Obtener los años existentes en los proyectos.
  const aniosDisponibles: number[] = [];

  for (const proyecto of proyectos) {
    if (!aniosDisponibles.includes(proyecto.anio)) {
      aniosDisponibles.push(proyecto.anio);
    }
  }

  aniosDisponibles.sort((a, b) => b - a);

  // Aplicar los tres filtros.
  const proyectosFiltrados: Proyecto[] = [];

  for (const proyecto of proyectos) {
    const coincideNombre = proyecto.nombre
      .toLocaleLowerCase('es')
      .includes(busquedaNombre.trim().toLocaleLowerCase('es'));

    const coincideEmpresa =
      empresaSeleccionada === '' ||
      proyecto.idEmpresa === Number(empresaSeleccionada);

    const coincideAnio =
      anioSeleccionado === '' ||
      proyecto.anio === Number(anioSeleccionado);

    if (coincideNombre && coincideEmpresa && coincideAnio) {
      proyectosFiltrados.push(proyecto);
    }
  }

  // Obtener solamente las filas de la página actual.
  const inicio = pagina * filasPorPagina;
  const fin = inicio + filasPorPagina;

  const proyectosPagina = proyectosFiltrados.slice(inicio, fin);

  function limpiarBusqueda() {
    setBusquedaNombre('');
    setPagina(0);
  }

  function limpiarFiltros() {
    setBusquedaNombre('');
    setEmpresaSeleccionada('');
    setAnioSeleccionado('');
    setPagina(0);
  }

  return (
    <Box
      component="section"
      sx={{
        px: 4,
        py: 2,
        width: '100%'
      }}
    >

      {/* Navegación */}
      <Breadcrumbs
        separator={<ChevronRightIcon sx={{ fontSize: 16 }} />}
        sx={{ mb: 1.5 }}
      >
        <Typography
          variant="body2"
          sx={{ color: '#667085' }}
        >
          Inicio
        </Typography>

        <Typography
          variant="body2"
          sx={{ color: colores.verdeOscuro }}
        >
          Proyectos
        </Typography>
      </Breadcrumbs>

      {/* Contenedor principal */}
      <Paper
        elevation={1}
        sx={{
          minHeight: 'calc(100vh - 204px)',
          borderRadius: 1,
          overflow: 'hidden'
        }}
      >

        {/* Encabezado */}
        <Box sx={{
              px: 4,
              pt: 2,
              pb: 0
            }}>
          <Typography component="h1" variant="h5">
            Proyectos
          </Typography>

          <Typography
            variant="body1"
            sx={{
              color: colores.textoSecundario
          }}
          >
            Talleres por Plataforma
          </Typography>
        </Box>

        {/* Filtros y acciones */}
        <Box
          sx={{
            px: 4,
            py: 2,
            display: 'flex',
            flexWrap: 'wrap',
            alignItems: 'center',
            gap: 2
          }}
        >

          {/* Buscar por nombre */}
          <TextField
            label="Buscar"
            size="small"
            placeholder="Nombre del proyecto"
            value={busquedaNombre}
            onChange={(evento) => {
              setBusquedaNombre(evento.target.value);
              setPagina(0);
            }}
            sx={{
              width: 500,
              maxWidth: '100%'
            }}
            slotProps={{
              input: {
                endAdornment: busquedaNombre !== '' ? (
                  <InputAdornment position="end">
                    <IconButton
                      size="small"
                      aria-label="Limpiar búsqueda"
                      onClick={limpiarBusqueda}
                    >
                      <CloseIcon />
                    </IconButton>
                  </InputAdornment>
                ) : undefined
              }
            }}
          />

          {/* Filtrar por empresa */}
          <TextField
            select
            label="Empresa financiadora"
            size="small"
            value={empresaSeleccionada}
            onChange={(evento) => {
              setEmpresaSeleccionada(evento.target.value);
              setPagina(0);
            }}
            sx={{
              width: 320,
              maxWidth: '100%'
            }}
          >
            <MenuItem value="">
              Todas las empresas
            </MenuItem>

            {empresasDisponibles.map((empresa) => (
              <MenuItem
                key={empresa.idEmpresa}
                value={String(empresa.idEmpresa)}
              >
                {empresa.nombreEmpresa}
              </MenuItem>
            ))}
          </TextField>

          {/* Filtrar por año */}
          <TextField
            select
            label="Año"
            size="small"
            value={anioSeleccionado}
            onChange={(evento) => {
              setAnioSeleccionado(evento.target.value);
              setPagina(0);
            }}
            sx={{ width: 100 }}
          >
            <MenuItem value="">
              Todos
            </MenuItem>

            {aniosDisponibles.map((anio) => (
              <MenuItem
                key={anio}
                value={String(anio)}
              >
                {anio}
              </MenuItem>
            ))}
          </TextField>

          {/* Restablecer todos los filtros del listado */}
          <Button
            variant="outlined"
            onClick={limpiarFiltros}
            disabled={busquedaNombre === '' && empresaSeleccionada === '' && anioSeleccionado === ''}
            sx={{ ml: { md: 'auto' }, height: 42 }}
          >
            Limpiar filtros
          </Button>

          {/* Alta: solo administradores */}
          {puedeCrearProyecto && (
            <Button
              variant="contained"
              onClick={onNuevoProyecto}
              sx={{
                minWidth: 179,
                height: 42
              }}
            >
              Nuevo proyecto
            </Button>
          )}
        </Box>

        {/* Mensajes de carga y error */}
        {cargando && (
          <Box
            sx={{
              display: 'flex',
              justifyContent: 'center',
              py: 6
            }}
          >
            <CircularProgress />
          </Box>
        )}

        {error !== '' && (
          <Box sx={{ px: 2, py: 2 }}>
            <Alert severity="error">
              {error}
            </Alert>
          </Box>
        )}

        {/* Tabla */}
        {!cargando && error === '' && (
          <>
            <TableContainer>
              <Table
                aria-label="Listado de proyectos"
                sx={{
                  minWidth: 850,

                  '& .MuiTableCell-root:first-of-type': {
                    pl: 4
                  },

                  '& .MuiTableCell-root:last-of-type': {
                    pr: 4
                  }
                }}
              >
                <TableHead>
                  <TableRow>
                    <TableCell sx={{ width: '39%', fontWeight: 600 }}>
                      Nombre
                    </TableCell>

                    <TableCell sx={{ width: '26%', fontWeight: 600 }}>
                      Empresa
                    </TableCell>

                    <TableCell sx={{ width: '14%', fontWeight: 600 }}>
                      Año
                    </TableCell>

                    <TableCell sx={{ width: '17%', fontWeight: 600 }}>
                      Estado
                    </TableCell>

                    <TableCell sx={{ width: '4%' }} />
                  </TableRow>
                </TableHead>

                <TableBody>
                  {proyectosPagina.map((proyecto) => (
                    <TableRow
                      key={proyecto.idProyecto}
                      hover
                      sx={{
                        height: 52,
                        '& td': {
                          py: 1
                        }
                      }}
                    >
                      <TableCell>
                        {proyecto.nombre}
                      </TableCell>

                      <TableCell>
                        {proyecto.nombreEmpresa}
                      </TableCell>

                      <TableCell>
                        {proyecto.anio}
                      </TableCell>

                      <TableCell>
                        <Chip
                          label={obtenerNombreEstado(proyecto.estado)}
                          sx={{
                            backgroundColor: proyecto.estado === 'PLANIFICADO'
                              ? colores.chipVerdeClaro : colores.chipAmarillo,
                            color: colores.textoPrimario,
                            borderRadius: '6px',
                            height: 32,
                            fontSize: 14,
                            fontWeight: 500
                          }}
                        />
                      </TableCell>

                      <TableCell align="right">
                        {puedeCrearProyecto &&
                        (proyecto.estado === 'PENDIENTE_PLANIFICACION' ||
                        proyecto.estado === 'PLANIFICADO') ? (
                          <AccesoPlanificacion
                            proyecto={proyecto}
                            onPlanificarProyecto={onPlanificarProyecto}
                          />
                        ) : puedeConsultarDocentes && (proyecto.estado === 'PLANIFICADO' || proyecto.estado === 'EN_CURSO' || proyecto.estado === 'FINALIZADO') ? (
                          <IconButton
                            size="small"
                            title="Consultar docentes del proyecto"
                            aria-label={'Consultar docentes de ' + proyecto.nombre}
                            onClick={function abrirDocentes() { onConsultarDocentes(proyecto); }}
                          >
                            <ChevronRightIcon sx={{ color: colores.textoPrimario }} />
                          </IconButton>
                        ) : (
                          <ChevronRightIcon
                            aria-hidden="true"
                            sx={{ color: colores.textoPrimario }}
                          />
                        )}
                      </TableCell>

                    </TableRow>
                  ))}

                  {proyectosFiltrados.length === 0 && (
                    <TableRow>
                      <TableCell colSpan={5} align="center">
                        <Typography
                          color="text.secondary"
                          sx={{ py: 4 }}
                        >
                          {proyectos.length === 0
                            ? 'No hay proyectos disponibles para consultar.'
                            : 'No se encontraron proyectos con los filtros seleccionados.'}
                        </Typography>
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>

            {/* Paginación */}
            <TablePagination
              component="div"
              count={proyectosFiltrados.length}
              page={pagina}
              rowsPerPage={filasPorPagina}
              rowsPerPageOptions={[5, 10, 25]}
              labelRowsPerPage="Proyectos por página"
              labelDisplayedRows={({ from, to, count }) =>
                `${from}–${to} de ${count}`
              }
              onPageChange={(_evento, nuevaPagina) => {
                setPagina(nuevaPagina);
              }}
              onRowsPerPageChange={(evento) => {
                setFilasPorPagina(Number(evento.target.value));
                setPagina(0);
              }}
            />
          </>
        )}

      </Paper>
    </Box>
  );
}

export default ProyectosPage;
