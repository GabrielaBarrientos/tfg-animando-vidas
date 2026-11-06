import json
from datetime import date
from pathlib import Path

from fastapi import FastAPI, HTTPException, Query, Response
from fastapi.responses import FileResponse
from enum import Enum

from pydantic import BaseModel, ConfigDict, Field, model_validator

app = FastAPI(title="API simulada - Animando Vidas")
DATOS = Path(__file__).parent / "talleres.json"
DETALLES = Path(__file__).parent / "detalle_talleres.json"
EVIDENCIAS = Path(__file__).parent / "evidencias.json"
ENCUESTAS = Path(__file__).parent / "encuestas.json"
ARCHIVOS = Path(__file__).parent / "archivos"


class ClasePlataforma(BaseModel):
    idClasePlataforma: int = Field(ge=1)
    numeroClase: int = Field(ge=1)
    fechaRealizacion: date | None = None
    cantidadParticipantes: int | None = Field(default=None, ge=0)
    cantidadEvidencias: int = Field(default=0, ge=0)
    preguntas: list["PreguntaProyectoPlataforma"]

    @model_validator(mode="after")
    def validar(self):
        if self.fechaRealizacion is None and (self.cantidadEvidencias != 0 or self.cantidadParticipantes is not None):
            raise ValueError("Una clase sin realizar no puede tener participantes ni evidencias")
        if self.fechaRealizacion is not None and self.cantidadParticipantes is None:
            raise ValueError("Una clase realizada debe informar participantes")
        return self


class PreguntaProyectoPlataforma(BaseModel):
    idPreguntaPlataforma: int = Field(ge=1)
    enunciado: str = Field(min_length=1, max_length=10000)
    tipo: str
    orden: int = Field(ge=1)

    @model_validator(mode="after")
    def validar_tipo(self):
        if self.tipo not in ("introducción", "clase"):
            raise ValueError("Tipo de pregunta desconocido")
        if not self.enunciado.strip():
            raise ValueError("El enunciado no puede estar vacío")
        return self


class TallerPlataforma(BaseModel):
    model_config = ConfigDict(extra="forbid")
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
        identificadores_clases = set()
        catalogo_referencia = None
        for dato in datos:
            if not isinstance(dato, dict):
                raise ValueError("Cada taller debe ser un objeto")
            if (dato.get("nombreProyecto") == nombreProyecto
                    and dato.get("anioProyecto") == anioProyecto
                    and dato.get("nombreEmpresa") == nombreEmpresa):
                taller = TallerPlataforma.model_validate(dato)
                numeros_clase = set()
                identificadores_preguntas = set()
                preguntas_por_clase = {}
                for clase in taller.clases:
                    if clase.idClasePlataforma in identificadores_clases:
                        raise ValueError("Identificador externo de clase repetido en el proyecto")
                    if clase.numeroClase in numeros_clase:
                        raise ValueError("Número de clase repetido en un taller")
                    identificadores_clases.add(clase.idClasePlataforma)
                    numeros_clase.add(clase.numeroClase)
                    preguntas_clase = {}
                    ordenes = set()
                    for pregunta in clase.preguntas:
                        if pregunta.idPreguntaPlataforma in identificadores_preguntas:
                            raise ValueError("Cada pregunta debe tener su propio identificador por clase")
                        if pregunta.orden in ordenes:
                            raise ValueError("Dos preguntas de una clase comparten orden")
                        identificadores_preguntas.add(pregunta.idPreguntaPlataforma)
                        ordenes.add(pregunta.orden)
                        preguntas_clase[pregunta.idPreguntaPlataforma] = (
                            pregunta.enunciado, pregunta.tipo, pregunta.orden
                        )
                    if ordenes != set(range(1, len(clase.preguntas) + 1)):
                        raise ValueError("El orden de cada clase debe ser 1, 2, 3, ...")
                    preguntas_por_clase[clase.numeroClase] = preguntas_clase
                if catalogo_referencia is None:
                    catalogo_referencia = preguntas_por_clase
                elif preguntas_por_clase != catalogo_referencia:
                    raise ValueError("Cada clase debe compartir las mismas preguntas entre talleres")
                coincidencias.append(taller)
        return coincidencias
    except (OSError, ValueError) as error:
        raise HTTPException(status_code=500, detail=f"No se pueden leer los datos simulados: {error}") from error


class EstadoClase(str, Enum):
    REALIZADA = "Realizada"
    PENDIENTE = "Pendiente"


class ClaseDetallePlataforma(BaseModel):
    idClasePlataforma: int = Field(ge=1)
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
        identificadores = set()
        for clase in self.clases:
            if clase.idClasePlataforma in identificadores:
                raise ValueError("Los identificadores de clase no pueden repetirse")
            identificadores.add(clase.idClasePlataforma)
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


# Evidencias: la API educativa real obtiene estos metadatos de su BD;
# la simulada los obtiene del JSON local. La ruta se usa SOLO internamente.
class ArchivoPlataforma(BaseModel):
    id_archivo_plataforma: int = Field(ge=1)
    id_clase_plataforma: int = Field(ge=1)
    nombre_fisico: str = Field(min_length=1)
    mime_type: str = Field(min_length=1)
    tamaño: int = Field(ge=0)


class RespuestaEvidencias(BaseModel):
    id_taller: int = Field(ge=1)
    evidencias: list[ArchivoPlataforma]


def leer_registros_evidencias() -> list[dict]:
    try:
        datos = json.loads(EVIDENCIAS.read_text(encoding="utf-8"))
        if not isinstance(datos, list):
            raise ValueError("El archivo debe contener un array de registros")
        identificadores = set()
        for dato in datos:
            if not isinstance(dato, dict):
                raise ValueError("Registro de evidencia inválido")
            ArchivoPlataforma.model_validate(dato)
            if dato["id_archivo_plataforma"] in identificadores:
                raise ValueError("Identificador de archivo duplicado")
            identificadores.add(dato["id_archivo_plataforma"])
            if not isinstance(dato.get("ruta"), str) or not dato["ruta"].strip():
                raise ValueError("Una evidencia no tiene ruta física")
            if not isinstance(dato.get("id_taller_plataforma"), int):
                raise ValueError("Una evidencia no identifica el taller")
        return datos
    except (OSError, ValueError, KeyError) as error:
        raise HTTPException(status_code=500, detail=f"Datos simulados de evidencias inválidos: {error}") from error


@app.get("/evidencias", response_model=RespuestaEvidencias)
def obtener_evidencias(id_taller_plataforma: int = Query(ge=1)):
    registros = leer_registros_evidencias()
    archivos = []
    for registro in registros:
        if registro["id_taller_plataforma"] == id_taller_plataforma:
            archivos.append(ArchivoPlataforma.model_validate(registro))
    return RespuestaEvidencias(id_taller=id_taller_plataforma, evidencias=archivos)


@app.get("/evidencias/{id_archivo_plataforma}/archivo")
def descargar_archivo_plataforma(id_archivo_plataforma: int):
    if id_archivo_plataforma < 1:
        raise HTTPException(status_code=404, detail="Archivo inexistente")
    registros = leer_registros_evidencias()
    for registro in registros:
        if registro["id_archivo_plataforma"] == id_archivo_plataforma:
            raiz = ARCHIVOS.resolve()
            candidato = (raiz / registro["ruta"] / registro["nombre_fisico"]).resolve()
            if not candidato.is_relative_to(raiz):
                raise HTTPException(status_code=404, detail="Ruta fuera del almacenamiento autorizado")
            if not candidato.is_file():
                raise HTTPException(status_code=404, detail="El archivo físico no existe")
            if candidato.stat().st_size != registro["tamaño"]:
                raise HTTPException(status_code=500, detail="Tamaño de archivo inconsistente")
            return FileResponse(
                candidato,
                media_type=registro["mime_type"],
                filename=registro["nombre_fisico"],
                content_disposition_type="inline",
            )
    raise HTTPException(status_code=404, detail="Archivo inexistente")


# Respuestas de encuestas por clase: las preguntas se obtienen de GET /proyecto.
class RespuestaEncuestaPlataforma(BaseModel):
    idTallerPlataforma: int = Field(ge=1)
    idClasePlataforma: int = Field(ge=1)
    idPreguntaPlataforma: int = Field(ge=1)
    respuesta: str = Field(min_length=1, max_length=20000)


class EncuestasPlataforma(BaseModel):
    idTallerPlataforma: int = Field(ge=1)
    respuestas: list[RespuestaEncuestaPlataforma]


@app.get("/encuestas", response_model=EncuestasPlataforma)
def consultar_encuestas(id_taller_plataforma: int = Query(ge=1)):
    try:
        datos = json.loads(ENCUESTAS.read_text(encoding="utf-8"))
        if not isinstance(datos, list):
            raise ValueError("Las encuestas simuladas deben ser una lista")
        respuestas = []
        combinaciones = set()
        for dato in datos:
            if not isinstance(dato, dict):
                raise ValueError("Respuesta de encuesta inválida")
            if dato.get("idTallerPlataforma") == id_taller_plataforma:
                respuesta = RespuestaEncuestaPlataforma.model_validate(dato)
                clave = (respuesta.idClasePlataforma, respuesta.idPreguntaPlataforma)
                if clave in combinaciones:
                    raise ValueError("La misma pregunta está respondida dos veces en una clase")
                combinaciones.add(clave)
                respuestas.append(respuesta)
        return EncuestasPlataforma(idTallerPlataforma=id_taller_plataforma, respuestas=respuestas)
    except (OSError, ValueError) as excepcion:
        raise HTTPException(status_code=500, detail=f"Encuestas simuladas inválidas: {excepcion}") from excepcion
