"""Pruebas del ajuste de datos de GET /talleres (V11)."""
import importlib.util
import json
from pathlib import Path

from fastapi.testclient import TestClient
from app import main

RAIZ = Path(__file__).resolve().parents[2]
MODULO = RAIZ / "NORMALIZAR-CLASES-TALLERES.py"
ESPECIFICACION = importlib.util.spec_from_file_location("normalizar_talleres", MODULO)
normalizador = importlib.util.module_from_spec(ESPECIFICACION)
ESPECIFICACION.loader.exec_module(normalizador)
PARAMETROS = {
    "nombreProyecto": "Una noche en el prado",
    "nombreEmpresa": "Fundación Prosegur",
    "anioProyecto": 2026,
}


def taller_con_clases():
    return [{
        "idTallerPlataforma": "8",
        "nombreProyecto": PARAMETROS["nombreProyecto"],
        "nombreEmpresa": PARAMETROS["nombreEmpresa"],
        "anioProyecto": PARAMETROS["anioProyecto"],
        "usuarioPlataforma": "ji.opazo",
        "clases": [
            {
                "idClasePlataforma": 7, "numeroClase": 1,
                "fechaRealizacion": "2026-05-15",
                "cantidadParticipantes": 9, "cantidadEvidencias": 0,
                "preguntas": [{"idPreguntaPlataforma": 101, "enunciado": "¿Te gustó?"}],
            },
            {
                "idClasePlataforma": 8, "numeroClase": 2,
                "fechaRealizacion": None,
                "cantidadParticipantes": 0, "cantidadEvidencias": 0,
                "pregunta": [{"idPreguntaPlataforma": 102}],
            },
        ],
    }]


def test_normalizacion_agrega_estado_elimina_preguntas_y_respeta_datos():
    datos = taller_con_clases()
    original = json.dumps(datos, ensure_ascii=False)
    corregido, agregados, eliminados = normalizador.normalizar_contenido(original, "talleres.json")
    assert (agregados, eliminados) == (2, 2)
    resultado = json.loads(corregido)
    assert resultado[0]["idTallerPlataforma"] == "8"
    assert resultado[0]["clases"][0] == {
        "idClasePlataforma": 7, "numeroClase": 1,
        "fechaRealizacion": "2026-05-15", "cantidadParticipantes": 9,
        "cantidadEvidencias": 0, "estado": "Realizada",
    }
    assert resultado[0]["clases"][1]["estado"] == "Pendiente"
    for clase in resultado[0]["clases"]:
        assert "pregunta" not in clase and "preguntas" not in clase
    assert normalizador.normalizar_contenido(corregido, "talleres.json") == (None, 0, 0)


def test_normalizacion_funciona_con_get_talleres(tmp_path, monkeypatch):
    archivo = tmp_path / "talleres.json"
    corregido, _, _ = normalizador.normalizar_contenido(
        json.dumps(taller_con_clases(), ensure_ascii=False), "talleres.json"
    )
    archivo.write_text(corregido, encoding="utf-8")
    monkeypatch.setattr(main, "DATOS", archivo)
    respuesta = TestClient(main.app).get("/talleres", params=PARAMETROS)
    assert respuesta.status_code == 200, respuesta.text
    clases = respuesta.json()[0]["clases"]
    assert [clase["estado"] for clase in clases] == ["Realizada", "Pendiente"]
    assert all("preguntas" not in clase and "pregunta" not in clase for clase in clases)


def test_no_modifica_archivos_si_una_clase_es_inconsistente(tmp_path):
    archivo_operativo = tmp_path / "api-simulada" / "app" / "talleres.json"
    archivo_operativo.parent.mkdir(parents=True)
    datos = taller_con_clases()
    datos[0]["clases"][1]["cantidadParticipantes"] = 3
    original = json.dumps(datos, ensure_ascii=False)
    archivo_operativo.write_text(original, encoding="utf-8")
    try:
        normalizador.procesar_archivos(tmp_path)
        assert False, "Debió rechazar datos inconsistentes"
    except ValueError as error:
        assert "pendiente" in str(error).lower()
    assert archivo_operativo.read_text(encoding="utf-8") == original
    assert not list(archivo_operativo.parent.glob("*.bak"))
