package org.tfg.backend.docente;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Sugerencia de usuario: primer nombre, punto y primer apellido; mínimo 8 caracteres. */
@Component
public class GeneradorUsuarioPlataforma {
    private static final Pattern FORMATO = Pattern.compile("^[a-z]+(\\.[a-z]+)+$");

    public String generar(String nombres, String apellidos) {
        String nombre = primeraPalabra(nombres);
        String apellido = primeraPalabra(apellidos);
        if (nombre.isEmpty() || apellido.isEmpty()) {
            return "";
        }
        StringBuilder prefijo = new StringBuilder();
        prefijo.append(nombre.charAt(0));
        int posicion = 1;
        while (prefijo.length() + 1 + apellido.length() < 8) {
            if (posicion >= nombre.length()) posicion = 0;
            prefijo.append(nombre.charAt(posicion));
            posicion++;
        }
        return prefijo + "." + apellido;
    }

    public String primeraPalabra(String texto) {
        if (texto == null || texto.isBlank()) return "";
        String normalizado = normalizar(texto);
        String[] partes = normalizado.split("\\s+");
        if (partes.length == 0) return "";
        return partes[0].replaceAll("[^a-z]", "");
    }

    private String normalizar(String texto) {
        return Normalizer.normalize(texto.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }

    public boolean formatoValido(String usuario) {
        return usuario != null && usuario.length() >= 8 && usuario.length() <= 150
                && FORMATO.matcher(usuario).matches();
    }
}
