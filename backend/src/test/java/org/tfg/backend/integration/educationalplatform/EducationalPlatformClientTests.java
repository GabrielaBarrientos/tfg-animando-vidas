package org.tfg.backend.integration.educationalplatform;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class EducationalPlatformClientTests {
    @Test
    void consultaTalleresEnNuevoEndpoint() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer mock = MockRestServiceServer.bindTo(builder).build();
        EducationalPlatformClient client = new EducationalPlatformClient(builder, "http://localhost:8000");
        mock.expect(requestTo("http://localhost:8000/talleres?nombreProyecto=Un%20prado&anioProyecto=2026&nombreEmpresa=Empresa"))
            .andExpect(method(HttpMethod.GET)).andRespond(withSuccess("""
                [{"idTallerPlataforma":"8","nombreProyecto":"Un prado","anioProyecto":2026,
                "nombreEmpresa":"Empresa","usuarioPlataforma":"ji.opazo","clases":[
                  {"idClasePlataforma":7,"numeroClase":1,"fechaRealizacion":null,
                    "cantidadParticipantes":0,"cantidadEvidencias":0,"estado":"Pendiente"}]}]
                """, MediaType.APPLICATION_JSON));
        var remotos = client.obtenerTalleres("Un prado", (short) 2026, "Empresa");
        assertEquals("8", remotos.get(0).idTallerPlataforma());
        assertEquals("Pendiente", remotos.get(0).clases().get(0).estado());
        mock.verify();
    }

    @Test
    void consultaProgramaIndependiente() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer mock = MockRestServiceServer.bindTo(builder).build();
        EducationalPlatformClient client = new EducationalPlatformClient(builder, "http://localhost:8000");
        mock.expect(requestTo("http://localhost:8000/programas?nombreProyecto=Un%20prado&empresa=Empresa&anio=2026"))
            .andExpect(method(HttpMethod.GET)).andRespond(withSuccess("""
                {"clase":[{"idClasePlataforma":7,"numeroClase":1,"nombreClase":"Encuentro 1",
                    "nombreModulo":"Dibujo","pregunta":[{"idPreguntaPlataforma":101,"numeroPregunta":1,
                        "formato":"texto","tipo":"intro","enunciado":"Pregunta de introducción"}]}]}
                """, MediaType.APPLICATION_JSON));
        var remoto = client.obtenerPrograma("Un prado", "Empresa", (short) 2026);
        assertEquals(7L, remoto.clase().get(0).idClasePlataforma());
        assertEquals("intro", remoto.clase().get(0).pregunta().get(0).tipo());
        mock.verify();
    }
}
