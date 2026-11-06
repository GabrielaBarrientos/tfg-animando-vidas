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
           empresa="Fundación Prosegur", usuario="lara.kon", id_clase=7):
    return {
        "idTaller": id_taller,
        "nombreProyecto": proyecto,
        "anioProyecto": anio,
        "nombreEmpresa": empresa,
        "usuarioPlataforma": usuario,
        "clases": [
            {
                "idClasePlataforma": id_clase,
                "numeroClase": 1,
                "fechaRealizacion": "2026-09-05",
                "cantidadParticipantes": 12,
                "cantidadEvidencias": 1,
                "preguntas": [],
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
    assert recibido["clases"][0]["idClasePlataforma"] == 7


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
        taller(id_taller="t2", usuario="clara.kon", id_clase=8),
    ])
    respuesta = cliente.get("/proyecto", params=PARAMETROS)
    assert respuesta.status_code == 200
    assert len(respuesta.json()) == 2


def test_rechaza_clase_sin_identificador_plataforma(tmp_path, monkeypatch):
    dato = taller()
    del dato["clases"][0]["idClasePlataforma"]
    guardar_datos(tmp_path, monkeypatch, [dato])
    assert cliente.get("/proyecto", params=PARAMETROS).status_code == 500


def test_rechaza_identificador_clase_no_positivo(tmp_path, monkeypatch):
    guardar_datos(tmp_path, monkeypatch, [taller(id_clase=0)])
    assert cliente.get("/proyecto", params=PARAMETROS).status_code == 500


def test_rechaza_identificadores_externos_repetidos_entre_talleres(tmp_path, monkeypatch):
    guardar_datos(tmp_path, monkeypatch, [
        taller(id_taller="t1", usuario="lara.kon", id_clase=7),
        taller(id_taller="t2", usuario="clara.kon", id_clase=7),
    ])
    assert cliente.get("/proyecto", params=PARAMETROS).status_code == 500


def test_ids_de_clases_de_proyecto_coinciden_con_detalle_taller():
    from app import main
    # Usar el ejemplo versionado: talleres.json puede contener datos locales diferentes.
    ejemplo = main.DATOS.parent.parent / "ejemplo-talleres.json"
    proyecto = json.loads(ejemplo.read_text(encoding="utf-8"))
    detalle = json.loads(main.DETALLES.read_text(encoding="utf-8"))
    for taller_proyecto in proyecto:
        correspondiente = None
        for taller_detalle in detalle:
            if int(taller_proyecto["idTaller"]) == taller_detalle["id_taller"]:
                correspondiente = taller_detalle
                break
        if correspondiente is None:
            continue
        por_numero = {}
        for clase in correspondiente["clases"]:
            por_numero[clase["numeroClase"]] = clase["idClasePlataforma"]
        for clase in taller_proyecto["clases"]:
            assert clase["idClasePlataforma"] == por_numero[clase["numeroClase"]]


def test_respuesta_real_del_ejemplo_contiene_ids_de_clases(tmp_path, monkeypatch):
    from app import main
    ejemplo = main.DATOS.parent.parent / "ejemplo-talleres.json"
    datos = json.loads(ejemplo.read_text(encoding="utf-8"))
    guardar_datos(tmp_path, monkeypatch, datos)
    respuesta = cliente.get("/proyecto", params=PARAMETROS)
    assert respuesta.status_code == 200
    talleres = respuesta.json()
    assert len(talleres) == 1
    assert talleres[0]["idTaller"] == "8"
    assert [clase["idClasePlataforma"] for clase in talleres[0]["clases"]] == list(range(7, 17))


def test_rechaza_numero_clase_duplicado(tmp_path, monkeypatch):
    dato = taller()
    segunda = dict(dato["clases"][0])
    segunda["idClasePlataforma"] = 8
    dato["clases"].append(segunda)
    guardar_datos(tmp_path, monkeypatch, [dato])
    assert cliente.get("/proyecto", params=PARAMETROS).status_code == 500
