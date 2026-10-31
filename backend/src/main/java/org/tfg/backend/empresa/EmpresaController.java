
package org.tfg.backend.empresa;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/empresas")
public class EmpresaController {

    private final EmpresaService empresaService;

    public EmpresaController(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    @GetMapping
    public List<EmpresaResponse> listarEmpresas() {

        List<Empresa> empresas = empresaService.listarEmpresas();

        List<EmpresaResponse> respuesta = new ArrayList<>();

        for (Empresa empresa : empresas) {
            EmpresaResponse empresaResponse = new EmpresaResponse(empresa);

            respuesta.add(empresaResponse);
        }

        return respuesta;
    }
}
