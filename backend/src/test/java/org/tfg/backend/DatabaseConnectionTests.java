
package org.tfg.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class DatabaseConnectionTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void verificarConexionPostgreSQL() {
        Integer resultado = jdbcTemplate.queryForObject(
            "SELECT 1", Integer.class
        );

        assertEquals(1, resultado);
    }
}
