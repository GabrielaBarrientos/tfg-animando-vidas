"""Regresión: programa separado de la ejecución de talleres."""
import copy
import json
from pathlib import Path
from fastapi.testclient import TestClient
from app import main

cliente = TestClient(main.app)
RUTA = Path(__file__).parent.parent
PROGRAMA = json.loads((RUTA / "app/programas.json").read_text(encoding="utf-8"))
TALLERES = json.loads((RUTA / "ejemplo-talleres.json").read_text(encoding="utf-8"))
PARAM_PROGRAMA = {"nombreProyecto":"Una noche en el prado", "empresa":"Fundación Prosegur", "anio":2026}
PARAM_TALLERES = {"nombreProyecto":"Una noche en el prado", "nombreEmpresa":"Fundación Prosegur", "anioProyecto":2026}

def sustituir(monkeypatch, tmp_path, atributo, datos):
    archivo = tmp_path / f"{atributo}.json"
    archivo.write_text(json.dumps(datos, ensure_ascii=False), encoding="utf-8")
    monkeypatch.setattr(main, atributo, archivo)

def test_programa_devuelve_clases_preguntas_intro_y_clase(monkeypatch, tmp_path):
    sustituir(monkeypatch, tmp_path, "PROGRAMAS", PROGRAMA)
    r = cliente.get('/programas', params=PARAM_PROGRAMA)
    assert r.status_code == 200, r.text
    clases = r.json()['clase']
    assert len(clases) == 10
    assert [c['numeroClase'] for c in clases] == list(range(1, 11))
    assert 'nombreModulo' in clases[0]
    assert clases[0]['pregunta'][0]['tipo'] == 'intro'
    assert clases[0]['pregunta'][1]['tipo'] == 'clase'
    assert clases[0]['pregunta'][0]['formato'] == 'texto'
    assert clases[0]['pregunta'][0]['numeroPregunta'] == 1

def test_programa_no_encontrado(monkeypatch, tmp_path):
    sustituir(monkeypatch, tmp_path, "PROGRAMAS", PROGRAMA)
    r = cliente.get('/programas', params={**PARAM_PROGRAMA, 'nombreProyecto':'No existe'})
    assert r.status_code == 404

def test_programa_rechaza_formato_distinto_texto(monkeypatch, tmp_path):
    datos = copy.deepcopy(PROGRAMA)
    datos[0]['clase'][0]['pregunta'][0]['formato'] = 'numero'
    sustituir(monkeypatch, tmp_path, "PROGRAMAS", datos)
    r = cliente.get('/programas', params=PARAM_PROGRAMA)
    assert r.status_code == 502
    assert 'formato' in r.json()['detail'].lower()

def test_programa_rechaza_numero_pregunta_duplicado(monkeypatch, tmp_path):
    datos = copy.deepcopy(PROGRAMA)
    datos[0]['clase'][0]['pregunta'][1]['numeroPregunta'] = 1
    sustituir(monkeypatch, tmp_path, "PROGRAMAS", datos)
    assert cliente.get('/programas', params=PARAM_PROGRAMA).status_code == 502

def test_programa_rechaza_clases_repetidas(monkeypatch, tmp_path):
    datos = copy.deepcopy(PROGRAMA)
    datos[0]['clase'][1]['idClasePlataforma'] = datos[0]['clase'][0]['idClasePlataforma']
    sustituir(monkeypatch, tmp_path, "PROGRAMAS", datos)
    assert cliente.get('/programas', params=PARAM_PROGRAMA).status_code == 502

def test_talleres_incluyen_clases_realizadas_y_pendientes(monkeypatch, tmp_path):
    sustituir(monkeypatch, tmp_path, "DATOS", TALLERES)
    r = cliente.get('/talleres', params=PARAM_TALLERES)
    assert r.status_code == 200, r.text
    taller = r.json()[0]
    assert taller['idTallerPlataforma'] == '8'
    assert 'idTaller' not in taller
    assert len(taller['clases']) == 10
    assert taller['clases'][0]['estado'] == 'Realizada'
    assert taller['clases'][-1]['estado'] == 'Pendiente'
    assert taller['clases'][-1]['fechaRealizacion'] is None
    assert taller['clases'][-1]['cantidadParticipantes'] == 0
    assert taller['clases'][-1]['cantidadEvidencias'] == 0
    assert 'preguntas' not in taller['clases'][0]

def test_identificador_de_clase_compartido_entre_talleres(monkeypatch, tmp_path):
    datos = copy.deepcopy(TALLERES)
    segundo = copy.deepcopy(datos[0]); segundo['idTallerPlataforma'] = '9'; segundo['usuarioPlataforma'] = 'ana.gomez'
    datos.append(segundo)
    sustituir(monkeypatch, tmp_path, "DATOS", datos)
    r = cliente.get('/talleres', params=PARAM_TALLERES)
    assert r.status_code == 200, r.text
    assert r.json()[0]['clases'][0]['idClasePlataforma'] == r.json()[1]['clases'][0]['idClasePlataforma']

def test_taller_pendiente_con_participantes_falla(monkeypatch, tmp_path):
    datos = copy.deepcopy(TALLERES)
    datos[0]['clases'][-1]['cantidadParticipantes'] = 4
    sustituir(monkeypatch, tmp_path, "DATOS", datos)
    assert cliente.get('/talleres', params=PARAM_TALLERES).status_code == 502

def test_clases_incompletas_entre_talleres_falla(monkeypatch, tmp_path):
    datos = copy.deepcopy(TALLERES)
    otro = copy.deepcopy(datos[0]); otro['idTallerPlataforma'] = '9'; otro['usuarioPlataforma']='otro.docente'
    otro['clases'].pop()
    datos.append(otro)
    sustituir(monkeypatch, tmp_path, "DATOS", datos)
    assert cliente.get('/talleres', params=PARAM_TALLERES).status_code == 502

def test_endpoint_anterior_se_retiro():
    assert cliente.get('/programa', params=PARAM_PROGRAMA).status_code == 404
    assert cliente.get('/proyecto', params=PARAM_TALLERES).status_code == 404
    assert cliente.get('/taller', params={'id_taller_plataforma':8}).status_code == 404
