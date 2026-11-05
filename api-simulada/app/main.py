import json
from datetime import date
from pathlib import Path

from fastapi import FastAPI, HTTPException, Query
from enum import Enum

from pydantic import BaseModel, Field, model_validator

app = FastAPI(title="API simulada - Animando Vidas")
DATOS = Path(__file__).parent / "talleres.json"
DETALLES = Path(__file__).parent / "detalle_talleres.json"


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


class EstadoClase(str, Enum):
    REALIZADA = "Realizada"
    PENDIENTE = "Pendiente"


class ClaseDetallePlataforma(BaseModel):
    numeroClase: int = Field(ge=1)
    nombreClase: str = Field(min_length=1)
    nombreModulo: str = Field(min_length=1)
    estadoClase: EstadoClase
    fechaRealizacion: date | None = None
    cantidadParticipantes: int | None = Field(default=None, ge=0)
    cantidadEvidencias: int | None = Field(default=None, ge=0)

    @model_validator(mode="after")
    def validar_estado(self):
        if self.estadoClase == EstadoClase.REALIZADA:
            if (self.fechaRealizacion is None or self.cantidadParticipantes is None
                    or self.cantidadEvidencias is None):
                raise ValueError("Una clase realizada debe informar fecha, participantes y evidencias")
        else:
            if (self.fechaRealizacion is not None or self.cantidadParticipantes is not None
                    or self.cantidadEvidencias is not None):
                raise ValueError("Una clase pendiente no debe informar fecha, participantes ni evidencias")
        return self


class DetalleTallerPlataforma(BaseModel):
    id_taller: int = Field(ge=1)
    clases: list[ClaseDetallePlataforma]

    @model_validator(mode="after")
    def validar_numeros_unicos(self):
        numeros = set()
        for clase in self.clases:
            if clase.numeroClase in numeros:
                raise ValueError("Los números de clase no pueden repetirse")
            numeros.add(clase.numeroClase)
        return self


@app.get("/taller", response_model=DetalleTallerPlataforma)
def obtener_detalle_taller(id_taller_plataforma: int = Query(ge=1)):
    """Devuelve las clases de un único taller desde los datos simulados."""
    try:
        datos = json.loads(DETALLES.read_text(encoding="utf-8"))
        if not isinstance(datos, list):
            raise ValueError("Los detalles simulados deben contener una lista")
        for dato in datos:
            if not isinstance(dato, dict):
                raise ValueError("Cada detalle de taller debe ser un objeto")
            if dato.get("id_taller") == id_taller_plataforma:
                return DetalleTallerPlataforma.model_validate(dato)
        raise HTTPException(status_code=404, detail="Taller no encontrado en plataforma")
    except (OSError, ValueError) as error:
        raise HTTPException(status_code=500,
                            detail=f"No se pueden leer los detalles simulados: {error}") from error
