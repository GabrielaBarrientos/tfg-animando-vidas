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
PROGRAMAS = Path(__file__).parent / "programas.json"


class EstadoClase(str, Enum):
    REALIZADA = "Realizada"
    PENDIENTE = "Pendiente"


class PreguntaPrograma(BaseModel):
    idPreguntaPlataforma: int = Field(ge=1)
    numeroPregunta: int = Field(ge=1)
    formato: str
    tipo: str
    enunciado: str = Field(min_length=1)

    @model_validator(mode="after")
    def validar(self):
        if self.formato != "texto":
            raise ValueError(f"Formato no admitido en pregunta {self.idPreguntaPlataforma}: {self.formato}")
        if self.tipo not in ("intro", "clase"):
            raise ValueError("Tipo de pregunta desconocido")
        if not self.enunciado.strip():
            raise ValueError("El enunciado no puede estar vacío")
        return self


class ClasePrograma(BaseModel):
    idClasePlataforma: int = Field(ge=1)
    numeroClase: int = Field(ge=1)
    nombreClase: str = Field(min_length=1)
    nombreModulo: str = Field(min_length=1)
    pregunta: list[PreguntaPrograma]

    @model_validator(mode="after")
    def validar(self):
        if not self.nombreClase.strip() or not self.nombreModulo.strip():
            raise ValueError("Nombre de clase y módulo obligatorios")
        ids = set()
        ordenes = set()
        for pregunta in self.pregunta:
            if pregunta.idPreguntaPlataforma in ids or pregunta.numeroPregunta in ordenes:
                raise ValueError("Preguntas o números de pregunta repetidos")
            ids.add(pregunta.idPreguntaPlataforma)
            ordenes.add(pregunta.numeroPregunta)
        if ordenes != set(range(1, len(self.pregunta) + 1)):
            raise ValueError("Los números de pregunta deben ser consecutivos")
        return self


class ProgramaPlataforma(BaseModel):
    clase: list[ClasePrograma] = Field(min_length=1)

    @model_validator(mode="after")
    def validar(self):
        ids = set()
        numeros = set()
        for clase in self.clase:
            if clase.idClasePlataforma in ids or clase.numeroClase in numeros:
                raise ValueError("Clases o números de clase duplicados")
            ids.add(clase.idClasePlataforma)
            numeros.add(clase.numeroClase)
        return self


class ClasePlataforma(BaseModel):
    idClasePlataforma: int = Field(ge=1)
    numeroClase: int = Field(ge=1)
    fechaRealizacion: date | None = None
    cantidadParticipantes: int = Field(ge=0)
    cantidadEvidencias: int = Field(ge=0)
    estado: EstadoClase

    @model_validator(mode="after")
    def validar(self):
        if self.estado == EstadoClase.REALIZADA and self.fechaRealizacion is None:
            raise ValueError("Una clase realizada requiere fecha")
        if self.estado == EstadoClase.PENDIENTE and (self.fechaRealizacion is not None or
                self.cantidadParticipantes != 0 or self.cantidadEvidencias != 0):
            raise ValueError("Una clase pendiente debe tener fecha nula y contadores cero")
        return self


class TallerPlataforma(BaseModel):
    model_config = ConfigDict(extra="forbid")
    idTallerPlataforma: str = Field(min_length=1, max_length=150)
    nombreProyecto: str = Field(min_length=1)
    anioProyecto: int = Field(ge=1)
    nombreEmpresa: str = Field(min_length=1)
    usuarioPlataforma: str = Field(min_length=1)
    clases: list[ClasePlataforma]

    @model_validator(mode="after")
    def validar(self):
        ids = set()
        numeros = set()
        for clase in self.clases:
            if clase.idClasePlataforma in ids or clase.numeroClase in numeros:
                raise ValueError("Clase duplicada en el taller")
            ids.add(clase.idClasePlataforma)
            numeros.add(clase.numeroClase)
        return self


@app.get("/health")
def health():
    return {"status": "ok", "service": "api-simulada"}


@app.get("/programas", response_model=ProgramaPlataforma)
def obtener_programa(nombreProyecto: str = Query(min_length=1),
                     empresa: str = Query(min_length=1), anio: int = Query(ge=1)):
    try:
        datos = json.loads(PROGRAMAS.read_text(encoding="utf-8"))
        if not isinstance(datos, list):
            raise ValueError("La fuente de programas debe contener una lista")
        for dato in datos:
            if (dato.get("nombreProyecto") == nombreProyecto and
                    dato.get("empresa") == empresa and dato.get("anio") == anio):
                return ProgramaPlataforma.model_validate({"clase": dato["clase"]})
        raise HTTPException(status_code=404, detail="El programa no existe en la plataforma educativa")
    except (OSError, ValueError, KeyError) as error:
        raise HTTPException(status_code=502, detail=f"Programa inválido: {error}") from error


@app.get("/talleres", response_model=list[TallerPlataforma])
def obtener_talleres(nombreProyecto: str = Query(min_length=1),
                    anioProyecto: int = Query(ge=1), nombreEmpresa: str = Query(min_length=1)):
    try:
        datos = json.loads(DATOS.read_text(encoding="utf-8"))
        if not isinstance(datos, list):
            raise ValueError("La fuente de talleres debe contener una lista")
        resultado = []
        ids_talleres = set()
        referencia_clases = None
        for dato in datos:
            if (dato.get("nombreProyecto") == nombreProyecto and
                    dato.get("anioProyecto") == anioProyecto and
                    dato.get("nombreEmpresa") == nombreEmpresa):
                taller = TallerPlataforma.model_validate(dato)
                if taller.idTallerPlataforma in ids_talleres:
                    raise ValueError("Identificador de taller repetido")
                ids_talleres.add(taller.idTallerPlataforma)
                definiciones = {(c.idClasePlataforma, c.numeroClase) for c in taller.clases}
                if referencia_clases is None:
                    referencia_clases = definiciones
                elif referencia_clases != definiciones:
                    raise ValueError("Los talleres no comparten las mismas clases del programa")
                resultado.append(taller)
        return resultado
    except (OSError, ValueError) as error:
        raise HTTPException(status_code=502, detail=f"Talleres inválidos: {error}") from error


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


# Respuestas por clase: definiciones de preguntas provienen de GET /programas.
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
