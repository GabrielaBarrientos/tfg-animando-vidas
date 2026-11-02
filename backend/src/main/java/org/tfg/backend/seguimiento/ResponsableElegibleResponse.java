package org.tfg.backend.seguimiento;

import org.tfg.backend.usuario.RolUsuario;

public record ResponsableElegibleResponse(
        Long idUsuario,
        String nombres,
        String apellidos,
        String email,
        RolUsuario rol) {
}
