"""Contrato: cada clase de GET /talleres lleva estado; no se infiere silenciosamente."""
import copy
import json
from pathlib import Path

from fastapi.testclient import TestClient
from app import main

CLIENTE = TestClient(main.app)
ARCHIVO_EJEMPLO = Path(__file__).resolve().parent.parent / "ejemplo-talleres.json"
PARAMETROS = {
    "nombreProyecto": "Una noche en el prado",
    "nombreEmpresa": "Fundación Prosegur",
    "anioProyecto": 2026,
}


def preparar_talleres(monkeypatch, tmp_path, talleres):
    ruta = tmp_path / "talleres.json"
    ruta.write_text(json.dumps(talleres, ensure_ascii=False), encoding="utf-8")
    monkeypatch.setattr(main, "DATOS", ruta)


def test_ejemplo_talleres_incluye_estado_en_todas_las_clases(monkeypatch, tmp_path):
    talleres = json.loads(ARCHIVO_EJEMPLO.read_text(encoding="utf-8"))
    assert talleres
    for taller in talleres:
        for clase in taller["clases"]:
            assert clase["estado"] in ("Realizada", "Pendiente")
    preparar_talleres(monkeypatch, tmp_path, talleres)
    respuesta = CLIENTE.get("/talleres", params=PARAMETROS)
    assert respuesta.status_code == 200, respuesta.text
    for taller in respuesta.json():
        for clase in taller["clases"]:
            assert "estado" in clase


def test_falta_estado_en_una_clase_devuelve_502(monkeypatch, tmp_path):
    talleres = json.loads(ARCHIVO_EJEMPLO.read_text(encoding="utf-8"))
    talleres = copy.deepcopy(talleres)
    del talleres[0]["clases"][0]["estado"]
    preparar_talleres(monkeypatch, tmp_path, talleres)
    respuesta = CLIENTE.get("/talleres", params=PARAMETROS)
    assert respuesta.status_code == 502


def test_clase_pendiente_con_fecha_devuelve_502(monkeypatch, tmp_path):
    talleres = json.loads(ARCHIVO_EJEMPLO.read_text(encoding="utf-8"))
    talleres = copy.deepcopy(talleres)
    pendiente = next(c for c in talleres[0]["clases"] if c["estado"] == "Pendiente")
    pendiente["fechaRealizacion"] = "2026-11-06"
    preparar_talleres(monkeypatch, tmp_path, talleres)
    respuesta = CLIENTE.get("/talleres", params=PARAMETROS)
    assert respuesta.status_code == 502
