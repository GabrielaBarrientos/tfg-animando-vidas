"""Contrato de preguntas por clase y respuestas identificadas por los tres ID externos."""
import copy
import json

from fastapi.testclient import TestClient
from app import main

cliente = TestClient(main.app)
PARAMETROS = {
    "nombreProyecto": "Una noche en el prado", "anioProyecto": 2026,
    "nombreEmpresa": "Fundación Prosegur",
}


def cargar_ejemplo():
    ruta = main.Path(__file__).parent.parent / "ejemplo-talleres.json"
    return json.loads(ruta.read_text(encoding="utf-8"))


def usar_datos(monkeypatch, tmp_path, datos):
    archivo = tmp_path / "talleres.json"
    archivo.write_text(json.dumps(datos, ensure_ascii=False), encoding="utf-8")
    monkeypatch.setattr(main, "DATOS", archivo)


def test_cada_clase_tiene_sus_propias_preguntas(monkeypatch, tmp_path):
    usar_datos(monkeypatch, tmp_path, cargar_ejemplo())
    respuesta = cliente.get("/proyecto", params=PARAMETROS)
    assert respuesta.status_code == 200
    taller = respuesta.json()[0]
    assert taller["idTaller"] == "8"
    assert "preguntas" not in taller
    assert len(taller["clases"]) == 10
    assert [c["numeroClase"] for c in taller["clases"]] == list(range(1, 11))
    assert taller["clases"][0]["idClasePlataforma"] == 7
    assert len(taller["clases"][0]["preguntas"]) == 6
    assert set(taller["clases"][0]["preguntas"][0]) == {
        "idPreguntaPlataforma", "enunciado", "tipo", "orden"}
    assert taller["clases"][0]["preguntas"][0]["tipo"] == "introducción"
    assert taller["clases"][0]["preguntas"][1]["tipo"] == "clase"
    assert [p["orden"] for p in taller["clases"][0]["preguntas"]] == list(range(1, 7))
    assert taller["clases"][9]["preguntas"]
    assert taller["clases"][9]["fechaRealizacion"] is None


def test_mismo_enunciado_identificador_distinto_segun_clase(monkeypatch, tmp_path):
    usar_datos(monkeypatch, tmp_path, cargar_ejemplo())
    taller = cliente.get("/proyecto", params=PARAMETROS).json()[0]
    primera = taller["clases"][0]["preguntas"][5]
    segunda = taller["clases"][1]["preguntas"][0]
    assert primera["enunciado"] == segunda["enunciado"]
    assert primera["idPreguntaPlataforma"] != segunda["idPreguntaPlataforma"]


def test_catalogo_igual_en_dos_talleres_con_clases_externas_distintas(monkeypatch, tmp_path):
    datos = cargar_ejemplo()
    otro = copy.deepcopy(datos[0])
    otro["idTaller"] = "9"
    otro["usuarioPlataforma"] = "otra.docente"
    for clase in otro["clases"]:
        clase["idClasePlataforma"] += 1000
    datos.append(otro)
    usar_datos(monkeypatch, tmp_path, datos)
    respuesta = cliente.get("/proyecto", params=PARAMETROS)
    assert respuesta.status_code == 200
    assert len(respuesta.json()) == 2
    assert (respuesta.json()[0]["clases"][0]["preguntas"]
            == respuesta.json()[1]["clases"][0]["preguntas"])


def test_rechaza_catalogo_diferente_entre_talleres(monkeypatch, tmp_path):
    datos = cargar_ejemplo()
    otro = copy.deepcopy(datos[0])
    otro["idTaller"] = "9"
    otro["usuarioPlataforma"] = "otra.docente"
    for clase in otro["clases"]:
        clase["idClasePlataforma"] += 1000
    otro["clases"][0]["preguntas"][0]["enunciado"] = "Cambiado"
    datos.append(otro)
    usar_datos(monkeypatch, tmp_path, datos)
    assert cliente.get("/proyecto", params=PARAMETROS).status_code == 500


def test_rechaza_pregunta_repetida_en_clases_diferentes(monkeypatch, tmp_path):
    datos = cargar_ejemplo()
    datos[0]["clases"][1]["preguntas"][0]["idPreguntaPlataforma"] = 106
    usar_datos(monkeypatch, tmp_path, datos)
    assert cliente.get("/proyecto", params=PARAMETROS).status_code == 500


def test_rechaza_orden_incorrecto(monkeypatch, tmp_path):
    datos = cargar_ejemplo()
    datos[0]["clases"][0]["preguntas"][2]["orden"] = 8
    usar_datos(monkeypatch, tmp_path, datos)
    assert cliente.get("/proyecto", params=PARAMETROS).status_code == 500


def test_rechaza_tipo_invalido(monkeypatch, tmp_path):
    datos = cargar_ejemplo()
    datos[0]["clases"][0]["preguntas"][0]["tipo"] = "TEXTO"
    usar_datos(monkeypatch, tmp_path, datos)
    assert cliente.get("/proyecto", params=PARAMETROS).status_code == 500


def test_get_encuestas_identifica_taller_clase_y_pregunta():
    respuesta = cliente.get("/encuestas", params={"id_taller_plataforma": 8})
    assert respuesta.status_code == 200
    resultado = respuesta.json()
    assert resultado["idTallerPlataforma"] == 8
    assert len(resultado["respuestas"]) == 12
    assert resultado["respuestas"][0]["idTallerPlataforma"] == 8
    assert resultado["respuestas"][0]["idClasePlataforma"] == 7
    assert resultado["respuestas"][0]["idPreguntaPlataforma"] == 101
    ids = {(r["idClasePlataforma"], r["idPreguntaPlataforma"]) for r in resultado["respuestas"]}
    assert (8, 107) in ids and (9, 109) in ids


def test_encuestas_sin_registros_devuelve_lista_vacia():
    respuesta = cliente.get("/encuestas", params={"id_taller_plataforma": 999})
    assert respuesta.status_code == 200
    assert respuesta.json() == {"idTallerPlataforma": 999, "respuestas": []}


def test_get_encuestas_rechaza_duplicados(monkeypatch, tmp_path):
    datos = [
        {"idTallerPlataforma": 8, "idClasePlataforma": 7, "idPreguntaPlataforma": 101, "respuesta": "Sí"},
        {"idTallerPlataforma": 8, "idClasePlataforma": 7, "idPreguntaPlataforma": 101, "respuesta": "No"}
    ]
    archivo = tmp_path / "encuestas.json"
    archivo.write_text(json.dumps(datos), encoding="utf-8")
    monkeypatch.setattr(main, "ENCUESTAS", archivo)
    respuesta = cliente.get("/encuestas", params={"id_taller_plataforma": 8})
    assert respuesta.status_code == 500


def test_rechaza_clase_sin_array_preguntas(monkeypatch, tmp_path):
    datos = cargar_ejemplo()
    del datos[0]["clases"][0]["preguntas"]
    usar_datos(monkeypatch, tmp_path, datos)
    assert cliente.get("/proyecto", params=PARAMETROS).status_code == 500
