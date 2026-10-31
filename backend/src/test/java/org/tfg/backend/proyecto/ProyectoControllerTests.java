
package org.tfg.backend.proyecto;

import java.time.Year;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import org.tfg.backend.empresa.Empresa;
import org.tfg.backend.empresa.EmpresaRepository;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProyectoControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmpresaRepository empresaRepository;

    private Empresa crearEmpresaPrueba() {
        String nombre = "Empresa " + UUID.randomUUID();

        Empresa empresa = new Empresa(nombre);

        return empresaRepository.saveAndFlush(empresa);
    }

    private String crearJson(
            String nombre,
            short anio,
            Long idEmpresa) {

        return "{"
                + "\"nombre\":\"" + nombre + "\","
                + "\"anio\":" + anio + ","
                + "\"descripcion\":\"Talleres educativos\","
                + "\"idEmpresa\":" + idEmpresa
                + "}";
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void administradorPuedeCrearProyecto() throws Exception {

        Empresa empresa = crearEmpresaPrueba();
        short anio = (short) Year.now().getValue();

        String cuerpo = crearJson(
                "Una noche en el prado",
                anio,
                empresa.getIdEmpresa()
        );

        String nombreCompleto = "Una noche en el prado - " + empresa.getNombre() + " - " + anio;

        mockMvc.perform(post("/api/proyectos")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idProyecto").isNumber())
                .andExpect(jsonPath("$.nombre")
                        .value("Una noche en el prado"))
                .andExpect(jsonPath("$.nombreCompleto")
                        .value(nombreCompleto))
                .andExpect(jsonPath("$.estado")
                        .value("PENDIENTE_PLANIFICACION"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void rechazaProyectoSinNombre() throws Exception {

        Empresa empresa = crearEmpresaPrueba();
        short anio = (short) Year.now().getValue();

        String cuerpo = crearJson(
                " ",
                anio,
                empresa.getIdEmpresa()
        );

        mockMvc.perform(post("/api/proyectos")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void rechazaProyectoConAnioAnterior() throws Exception {

        Empresa empresa = crearEmpresaPrueba();
        short anio = (short) (Year.now().getValue() - 1);

        String cuerpo = crearJson(
                "Proyecto anterior",
                anio,
                empresa.getIdEmpresa()
        );

        mockMvc.perform(post("/api/proyectos")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void rechazaEmpresaInexistente() throws Exception {

        short anio = (short) Year.now().getValue();

        String cuerpo = crearJson(
                "Proyecto educativo",
                anio,
                -1L
        );

        mockMvc.perform(post("/api/proyectos")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "OPERATIVO")
    void operativoNoPuedeCrearProyecto() throws Exception {

        short anio = (short) Year.now().getValue();

        String cuerpo = crearJson(
                "Proyecto no autorizado",
                anio,
                1L
        );

        mockMvc.perform(post("/api/proyectos")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EXTERNO")
    void externoNoPuedeCrearProyecto() throws Exception {

        short anio = (short) Year.now().getValue();

        String cuerpo = crearJson(
                "Proyecto no autorizado",
                anio,
                1L
        );

        mockMvc.perform(post("/api/proyectos")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isForbidden());
    }
}
