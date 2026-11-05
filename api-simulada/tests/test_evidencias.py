import json
from pathlib import Path
from fastapi.testclient import TestClient
from app import main

cliente = TestClient(main.app)


def test_listado_muestra_solo_metadatos_y_sin_ruta():
    respuesta = cliente.get("/evidencias", params={"id_taller_plataforma": 8})
    assert respuesta.status_code == 200
    datos = respuesta.json()
    assert datos["id_taller"] == 8
    assert len(datos["evidencias"]) > 0
    primero = datos["evidencias"][0]
    assert primero["id_archivo_plataforma"] == 1
    assert primero["id_clase_plataforma"] == 8
    assert primero["nombre_fisico"] == "evidencia j.opazo.jpg"
    assert primero["mime_type"] == "image/jpeg"
    assert primero["tamaño"] == 145382
    assert "ruta" not in primero
    assert "fecha_subida" not in primero


def test_descarga_binaria_y_soporte_de_rangos():
    respuesta = cliente.get("/evidencias/1/archivo")
    assert respuesta.status_code == 200
    assert respuesta.headers["content-type"].startswith("image/jpeg")
    assert len(respuesta.content) == 145382
    assert respuesta.content[:2] == b"\xff\xd8"
    parcial = cliente.get("/evidencias/1/archivo", headers={"Range": "bytes=0-9"})
    assert parcial.status_code == 206
    assert parcial.content == respuesta.content[:10]


def test_no_expone_archivos_fuera_del_raiz(tmp_path, monkeypatch):
    archivo = tmp_path / "registros.json"
    archivo.write_text(json.dumps([{
        "id_taller_plataforma": 8, "id_archivo_plataforma": 99,
        "id_clase_plataforma": 8, "nombre_fisico": "passwd",
        "ruta": "../../../../etc", "mime_type": "image/jpeg", "tamaño": 1
    }]), encoding="utf-8")
    monkeypatch.setattr(main, "EVIDENCIAS", archivo)
    respuesta = cliente.get("/evidencias/99/archivo")
    assert respuesta.status_code == 404


def test_errores_y_taller_sin_evidencias():
    assert cliente.get("/evidencias").status_code == 422
    assert cliente.get("/evidencias", params={"id_taller_plataforma": 9}).json()["evidencias"] == []
    assert cliente.get("/evidencias/999/archivo").status_code == 404
