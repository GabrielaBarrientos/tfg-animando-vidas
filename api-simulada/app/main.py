import json
from datetime import date
from pathlib import Path

from fastapi import FastAPI, HTTPException, Query
from pydantic import BaseModel, Field, model_validator

app = FastAPI(title="API simulada - Animando Vidas")
DATOS = Path(__file__).parent / "talleres.json"


class ClasePlataforma(BaseModel):
    numeroClase: int = Field(ge=1)
    fechaRealizacion: date | None = None
    cantidadParticipantes: int | None = Field(default=None, ge=0)
    cantidadEvidencias: int = Field(default=0, ge=0)

    @model_validator(mode="after")
    def validar(self):
        if self.fechaRealizacion is None and (self.cantidadEvidencias != 0 or self.cantidadParticipantes is not None):
            raise ValueError("Una clase sin realizar no puede tener participantes ni evidencias")
        if self.fechaRealizacion is not None and self.cantidadParticipantes is None:
            raise ValueError("Una clase realizada debe informar participantes")
        return self


class TallerPlataforma(BaseModel):
    idTaller: str = Field(min_length=1, max_length=150)
    nombreProyecto: str = Field(min_length=1)
    anioProyecto: int = Field(ge=1)
    nombreEmpresa: str = Field(min_length=1)
    usuarioPlataforma: str = Field(min_length=8)
    clases: list[ClasePlataforma]


@app.get("/health")
def health():
    return {"status": "ok", "service": "api-simulada"}


@app.get("/proyecto", response_model=list[TallerPlataforma])
def obtener_talleres(
    nombreProyecto: str = Query(min_length=1),
    anioProyecto: int = Query(ge=1),
    nombreEmpresa: str = Query(min_length=1),
):
    """Selecciona talleres por datos del proyecto, sin un ID interno ni entidad Proyecto."""
    try:
        datos = json.loads(DATOS.read_text(encoding="utf-8"))
        if not isinstance(datos, list):
            raise ValueError("Los datos simulados deben contener una lista de talleres")

        coincidencias = []
        for dato in datos:
            if not isinstance(dato, dict):
                raise ValueError("Cada taller debe ser un objeto")
            if (dato.get("nombreProyecto") == nombreProyecto
                    and dato.get("anioProyecto") == anioProyecto
                    and dato.get("nombreEmpresa") == nombreEmpresa):
                coincidencias.append(TallerPlataforma.model_validate(dato))
        return coincidencias
    except (OSError, ValueError) as error:
        raise HTTPException(status_code=500, detail=f"No se pueden leer los datos simulados: {error}") from error
