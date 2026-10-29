
from fastapi import FastAPI

app = FastAPI(
    title="API simulada - Animando Vidas"
)


@app.get("/health")
def health():
    return {
        "status": "ok",
        "service": "api-simulada"
    }
