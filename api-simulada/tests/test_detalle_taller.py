import json

from fastapi.testclient import TestClient

from app import main

cliente = TestClient(main.app)


def detalle_valido():
    return {
        "id_taller": 8,
        "clases": [
            {
                "idClasePlataforma": 7,
                "numeroClase": 1,
                "nombreClase": "ENCUENTRO 1 - EL ARCA DE NOE",
                "nombreModulo": "INTRODUCCIÓN AL DIBUJO",
                "estadoClase": "Realizada",
                "fechaRealizacion": "2026-05-15",
                "cantidadParticipantes": 9,
                "cantidadEvidencias": 0,
            },
            {
                "idClasePlataforma": 8,
                "numeroClase": 2,
                "nombreClase": "ENCUENTRO 2 - APRENDER COMO RUBENS",
                "nombreModulo": "INTRODUCCIÓN AL DIBUJO",
                "estadoClase": "Pendiente",
                "fechaRealizacion": None,
                "cantidadParticipantes": None,
                "cantidadEvidencias": None,
            },
        ],
    }


def guardar_detalle(tmp_path, monkeypatch, datos):
    archivo = tmp_path / "detalle_talleres.json"
    archivo.write_text(json.dumps(datos, ensure_ascii=False), encoding="utf-8")
    monkeypatch.setattr(main, "DETALLES", archivo)


def test_obtiene_detalle_con_clases_realizadas_y_pendientes(tmp_path, monkeypatch):
    guardar_detalle(tmp_path, monkeypatch, [detalle_valido()])
    respuesta = cliente.get("/taller", params={"id_taller_plataforma": 8})
    assert respuesta.status_code == 200
    assert respuesta.json()["id_taller"] == 8
    assert len(respuesta.json()["clases"]) == 2
    assert respuesta.json()["clases"][0]["estadoClase"] == "Realizada"
    assert respuesta.json()["clases"][1]["cantidadEvidencias"] is None


def test_no_existe_el_taller(tmp_path, monkeypatch):
    guardar_detalle(tmp_path, monkeypatch, [detalle_valido()])
    assert cliente.get("/taller", params={"id_taller_plataforma": 9}).status_code == 404


def test_rechaza_identificadores_invalidos():
    assert cliente.get("/taller").status_code == 422
    assert cliente.get("/taller", params={"id_taller_plataforma": "av-2026-001"}).status_code == 422
    assert cliente.get("/taller", params={"id_taller_plataforma": 0}).status_code == 422


def test_rechaza_estado_pendiente_con_fecha(tmp_path, monkeypatch):
    dato = detalle_valido()
    dato["clases"][1]["fechaRealizacion"] = "2026-06-01"
    guardar_detalle(tmp_path, monkeypatch, [dato])
    assert cliente.get("/taller", params={"id_taller_plataforma": 8}).status_code == 500


def test_rechaza_clases_repetidas(tmp_path, monkeypatch):
    dato = detalle_valido()
    dato["clases"][1]["numeroClase"] = 1
    guardar_detalle(tmp_path, monkeypatch, [dato])
    assert cliente.get("/taller", params={"id_taller_plataforma": 8}).status_code == 500


def test_rechaza_realizada_sin_participantes(tmp_path, monkeypatch):
    dato = detalle_valido()
    dato["clases"][0]["cantidadParticipantes"] = None
    guardar_detalle(tmp_path, monkeypatch, [dato])
    assert cliente.get("/taller", params={"id_taller_plataforma": 8}).status_code == 500
