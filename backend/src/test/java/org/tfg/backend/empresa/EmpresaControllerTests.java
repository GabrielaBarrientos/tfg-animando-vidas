
package org.tfg.backend.empresa;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EmpresaControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    void administradorPuedeConsultarEmpresasOrdenadas() throws Exception {

        String identificador = UUID.randomUUID().toString();

        String nombreA = "Empresa A " + identificador;
        String nombreZ = "Empresa Z " + identificador;

        empresaRepository.saveAndFlush(new Empresa(nombreZ));
        empresaRepository.saveAndFlush(new Empresa(nombreA));

        MvcResult resultado = mockMvc.perform(
                        get("/api/empresas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andReturn();

        String respuesta = resultado.getResponse()
                .getContentAsString();

        int posicionA = respuesta.indexOf(nombreA);
        int posicionZ = respuesta.indexOf(nombreZ);

        assertTrue(posicionA >= 0, "Debe aparecer la empresa A");

        assertTrue(posicionZ >= 0, "Debe aparecer la empresa Z");

        assertTrue(posicionA < posicionZ, "Las empresas deben estar ordenadas por nombre");
    }

    @Test
    @WithMockUser(roles = "OPERATIVO")
    void operativoNoPuedeConsultarEmpresas() throws Exception {

        mockMvc.perform(get("/api/empresas"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EXTERNO")
    void externoNoPuedeConsultarEmpresas() throws Exception {

        mockMvc.perform(get("/api/empresas"))
                .andExpect(status().isForbidden());
    }
}
