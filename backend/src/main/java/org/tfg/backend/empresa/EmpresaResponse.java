
package org.tfg.backend.empresa;

public class EmpresaResponse {

    private Long idEmpresa;
    private String nombre;

    public EmpresaResponse(Empresa empresa) {
        this.idEmpresa = empresa.getIdEmpresa();
        this.nombre = empresa.getNombre();
    }

    public Long getIdEmpresa() {
        return idEmpresa;
    }

    public String getNombre() {
        return nombre;
    }
}
