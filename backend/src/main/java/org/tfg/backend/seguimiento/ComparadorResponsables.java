package org.tfg.backend.seguimiento;

import java.util.Comparator;

public class ComparadorResponsables implements Comparator<ResponsableElegibleResponse> {

    @Override
    public int compare(ResponsableElegibleResponse primero, ResponsableElegibleResponse segundo) {
        int comparacionApellidos = primero.apellidos().compareToIgnoreCase(segundo.apellidos());
        if (comparacionApellidos != 0) {
            return comparacionApellidos;
        }
        return primero.nombres().compareToIgnoreCase(segundo.nombres());
    }
}
