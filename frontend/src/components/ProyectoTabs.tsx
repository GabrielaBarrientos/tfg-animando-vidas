import type { SyntheticEvent } from 'react';
import { Tab, Tabs } from '@mui/material';

export type SolapaProyecto = 'resumen' | 'preguntas' | 'talleres'
  | 'evidencias' | 'encuestas' | 'avance';

interface ProyectoTabsProps {
  valor: SolapaProyecto;
  onCambiar: (solapa: SolapaProyecto) => void;
}

/** Navegación común a las pantallas 7 y 8. Las pestañas no se desplazan con el contenido. */
export default function ProyectoTabs({ valor, onCambiar }: ProyectoTabsProps) {
  function cambiar(_evento: SyntheticEvent, seleccion: SolapaProyecto) {
    onCambiar(seleccion);
  }

  return (
    <Tabs value={valor} onChange={cambiar} variant="scrollable" scrollButtons="auto"
      aria-label="Secciones del proyecto"
      sx={{ borderBottom: 1, borderColor: 'divider', flexShrink: 0 }}>
      <Tab value="resumen" label="Resumen" />
      <Tab value="preguntas" label="Preguntas" />
      <Tab value="talleres" label="Talleres" />
      <Tab value="evidencias" label="Evidencias" />
      <Tab value="encuestas" label="Encuestas" />
      <Tab value="avance" label="Avance" />
    </Tabs>
  );
}
