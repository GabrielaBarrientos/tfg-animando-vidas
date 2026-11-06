
package org.tfg.backend.integration.educationalplatform;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class EducationalPlatformClientTests {

    @Test
    void obtieneTalleresPorNombreAnioYEmpresaSinIdLocal() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        EducationalPlatformClient client = new EducationalPlatformClient(builder, "http://127.0.0.1:8000");

        mockServer.expect(requestTo(
                "http://127.0.0.1:8000/proyecto?nombreProyecto=Una%20noche%20en%20el%20prado"
                        + "&anioProyecto=2026&nombreEmpresa=Fundaci%C3%B3n%20Prosegur"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        """
                        [{
                          "idTaller": "av-001",
                          "nombreProyecto": "Una noche en el prado",
                          "anioProyecto": 2026,
                          "nombreEmpresa": "Fundación Prosegur",
                          "usuarioPlataforma": "lara.kon",
                          "clases": [{
                            "idClasePlataforma": 7,
                            "numeroClase": 1,
                            "fechaRealizacion": "2026-07-12",
                            "cantidadParticipantes": 20,
                            "cantidadEvidencias": 2,
                            "preguntas": [{
                              "idPreguntaPlataforma": 101,
                              "enunciado": "¿Cómo resultó la clase?",
                              "tipo": "clase",
                              "orden": 1
                            }]
                          }]
                        }]
                        """,
                        MediaType.APPLICATION_JSON));

        var talleres = client.obtenerTalleres(
                "Una noche en el prado", (short) 2026, "Fundación Prosegur");

        assertEquals(1, talleres.size());
        assertEquals("av-001", talleres.get(0).idTaller());
        assertEquals("lara.kon", talleres.get(0).usuarioPlataforma());
        assertEquals(2026, talleres.get(0).anioProyecto().intValue());
        assertEquals(20, talleres.get(0).clases().get(0).cantidadParticipantes());
        assertEquals(7L, talleres.get(0).clases().get(0).idClasePlataforma());
        assertEquals(1, talleres.get(0).clases().get(0).preguntas().size());
        assertEquals(101L, talleres.get(0).clases().get(0).preguntas().get(0).idPreguntaPlataforma());
        assertEquals("¿Cómo resultó la clase?", talleres.get(0).clases().get(0).preguntas().get(0).enunciado());
        assertEquals("clase", talleres.get(0).clases().get(0).preguntas().get(0).tipo());
        assertEquals(1, talleres.get(0).clases().get(0).preguntas().get(0).orden());
        mockServer.verify();
    }

    @Test
    void devuelveListaVaciaCuandoLaPlataformaNoTieneTalleres() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(builder).build();
        EducationalPlatformClient client = new EducationalPlatformClient(builder, "http://127.0.0.1:8000");

        mockServer.expect(requestTo(
                "http://127.0.0.1:8000/proyecto?nombreProyecto=Proyecto%20sin%20talleres"
                        + "&anioProyecto=2026&nombreEmpresa=Empresa"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        var talleres = client.obtenerTalleres("Proyecto sin talleres", (short) 2026, "Empresa");
        assertTrue(talleres.isEmpty());
        mockServer.verify();
    }

    @Test
    void obtieneClasesDeUnTallerPorIdDePlataforma() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        EducationalPlatformClient cliente = new EducationalPlatformClient(builder, "http://127.0.0.1:8000");
        servidor.expect(requestTo("http://127.0.0.1:8000/taller?id_taller_plataforma=8"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "id_taller": 8,
                          "clases": [{
                            "numeroClase": 10,
                            "nombreClase": "ENCUENTRO 12 - GALERIA DE VIRTUDES",
                            "nombreModulo": "INTRODUCCIÓN A LA ANIMACIÓN",
                            "estadoClase": "Pendiente",
                            "fechaRealizacion": null,
                            "cantidadParticipantes": null,
                            "cantidadEvidencias": null
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        PlatformDetalleTallerResponse detalle = cliente.obtenerDetalleTaller(8);
        assertEquals(8, detalle.idTaller().intValue());
        assertEquals(10, detalle.clases().get(0).numeroClase().intValue());
        assertEquals("Pendiente", detalle.clases().get(0).estadoClase());
        org.junit.jupiter.api.Assertions.assertNull(detalle.clases().get(0).cantidadEvidencias());
        servidor.verify();
    }

    @Test
    void shouldRetrievePlatformHealth() {

        RestClient.Builder builder = RestClient.builder();

        MockRestServiceServer mockServer =
                MockRestServiceServer.bindTo(builder).build();

        EducationalPlatformClient client =
                new EducationalPlatformClient(
                        builder,
                        "http://127.0.0.1:8000"
                );

        mockServer.expect(
                requestTo("http://127.0.0.1:8000/health")
        )
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(
                """
                {
                    "status": "ok",
                    "service": "api-simulada"
                }
                """,
                MediaType.APPLICATION_JSON
        ));

        var response = client.checkHealth();

        assertNotNull(response);
        assertEquals("ok", response.status());
        assertEquals("api-simulada", response.service());

        mockServer.verify();
    }

    @Test
    void obtieneRespuestasConLosTresIdentificadoresExternos() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer servidor = MockRestServiceServer.bindTo(builder).build();
        EducationalPlatformClient cliente = new EducationalPlatformClient(builder, "http://127.0.0.1:8000");
        servidor.expect(requestTo("http://127.0.0.1:8000/encuestas?id_taller_plataforma=8"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "idTallerPlataforma": 8,
                          "respuestas": [{
                            "idTallerPlataforma": 8,
                            "idClasePlataforma": 7,
                            "idPreguntaPlataforma": 102,
                            "respuesta": "Todos comprendieron observando el video."
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        PlatformEncuestasResponse datos = cliente.obtenerEncuestas(8);
        assertEquals(8, datos.idTallerPlataforma().intValue());
        assertEquals(8, datos.respuestas().get(0).idTallerPlataforma().intValue());
        assertEquals(7L, datos.respuestas().get(0).idClasePlataforma().longValue());
        assertEquals(102L, datos.respuestas().get(0).idPreguntaPlataforma().longValue());
        servidor.verify();
    }
}
