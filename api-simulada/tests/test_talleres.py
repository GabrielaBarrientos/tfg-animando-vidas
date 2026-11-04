import json

from fastapi.testclient import TestClient
from app.main import app

cliente = TestClient(app)
PARAMETROS = {
    "nombreProyecto": "Una noche en el prado",
    "anioProyecto": 2026,
    "nombreEmpresa": "Fundación Prosegur",
}


def taller(id_taller="av-001", proyecto="Una noche en el prado", anio=2026,
           empresa="Fundación Prosegur", usuario="lara.kon"):
    return {
        "idTaller": id_taller,
        "nombreProyecto": proyecto,
        "anioProyecto": anio,
        "nombreEmpresa": empresa,
        "usuarioPlataforma": usuario,
        "clases": [
            {
                "numeroClase": 1,
                "fechaRealizacion": "2026-09-05",
                "cantidadParticipantes": 12,
                "cantidadEvidencias": 1,
            },
        ],
    }


def guardar_datos(tmp_path, monkeypatch, datos):
    from app import main

    archivo = tmp_path / "talleres.json"
    archivo.write_text(json.dumps(datos, ensure_ascii=False), encoding="utf-8")
    monkeypatch.setattr(main, "DATOS", archivo)


def test_proyecto_sin_datos_devuelve_lista_vacia(tmp_path, monkeypatch):
    guardar_datos(tmp_path, monkeypatch, [])
    respuesta = cliente.get("/proyecto", params=PARAMETROS)
    assert respuesta.status_code == 200
    assert respuesta.json() == []


def test_parametros_de_consulta_son_obligatorios():
    assert cliente.get("/proyecto").status_code == 422
    assert cliente.get("/proyecto", params={**PARAMETROS, "anioProyecto": 0}).status_code == 422


def test_filtra_por_nombre_anio_y_empresa(tmp_path, monkeypatch):
    guardar_datos(tmp_path, monkeypatch, [
        taller(id_taller="correcto"),
        taller(id_taller="otro-nombre", proyecto="Otro proyecto"),
        taller(id_taller="otro-anio", anio=2025),
        taller(id_taller="otra-empresa", empresa="Otra empresa"),
    ])
    respuesta = cliente.get("/proyecto", params=PARAMETROS)
    assert respuesta.status_code == 200
    assert [t["idTaller"] for t in respuesta.json()] == ["correcto"]


def test_contrato_incluye_usuario_plataforma_y_clases(tmp_path, monkeypatch):
    guardar_datos(tmp_path, monkeypatch, [taller()])
    respuesta = cliente.get("/proyecto", params=PARAMETROS)
    assert respuesta.status_code == 200
    recibido = respuesta.json()[0]
    assert recibido["nombreProyecto"] == PARAMETROS["nombreProyecto"]
    assert recibido["anioProyecto"] == 2026
    assert recibido["nombreEmpresa"] == PARAMETROS["nombreEmpresa"]
    assert recibido["usuarioPlataforma"] == "lara.kon"
    assert "usuarioDocente" not in recibido
    assert recibido["clases"][0]["cantidadParticipantes"] == 12


def test_fechas_invalidas_se_rechazan(tmp_path, monkeypatch):
    dato = taller()
    dato["clases"][0]["fechaRealizacion"] = "2026-99-99"
    guardar_datos(tmp_path, monkeypatch, [dato])
    assert cliente.get("/proyecto", params=PARAMETROS).status_code == 500


def test_rechaza_participantes_sin_fecha(tmp_path, monkeypatch):
    dato = taller()
    dato["clases"][0]["fechaRealizacion"] = None
    guardar_datos(tmp_path, monkeypatch, [dato])
    assert cliente.get("/proyecto", params=PARAMETROS).status_code == 500


def test_formato_anterior_por_id_proyecto_no_es_valido(tmp_path, monkeypatch):
    guardar_datos(tmp_path, monkeypatch, {"1": [taller()]})
    assert cliente.get("/proyecto", params=PARAMETROS).status_code == 500


def test_acepta_varios_docentes_del_mismo_proyecto(tmp_path, monkeypatch):
    guardar_datos(tmp_path, monkeypatch, [
        taller(id_taller="t1", usuario="lara.kon"),
        taller(id_taller="t2", usuario="clara.kon"),
    ])
    respuesta = cliente.get("/proyecto", params=PARAMETROS)
    assert respuesta.status_code == 200
    assert len(respuesta.json()) == 2
