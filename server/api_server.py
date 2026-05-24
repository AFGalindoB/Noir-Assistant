from fastapi import FastAPI, Request, HTTPException, Depends, Response, UploadFile, File, Form, BackgroundTasks, Query
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from fastapi.responses import HTMLResponse
from pydantic import BaseModel
import json
from pathlib import Path
import data.database as db
import data.redis_manager as rm
from noir_core import noir_security
from path_manager import PathManager
import audio_processor

REQUIRED_KEYS = {"header", "audio_filename", "titulo", "descripcion", "fecha"}

app = FastAPI(title="Noir Assistant API")
security = HTTPBearer()

class RequestAuth(BaseModel):
    sid: str
    type: str
    device_id: str
    device_name: str

class ClaimTokenRequest(BaseModel):
    username: str
    type: str
    device_id: str
    device_name: str

@app.get("/health")
async def health():
    return {"status": "healthy", "version": "2.0.0"}

@app.post("/auth/request_autorization")
async def request_authorization(data: RequestAuth, response: Response):

    if data.type != "authorization":
        raise HTTPException(status_code=400, detail="QR type inválido")

    status_code, message = rm.claim_qr_session(
        sid=data.sid, 
        device_id=data.device_id,
        device_name=data.device_name
    )

    response.status_code = status_code
    
    status_text = "pending"
    if status_code == 202:
        status_text = "already_authorized"
    elif status_code == 403:
        raise HTTPException(status_code=403, detail=message)
    elif status_code == 410:
        raise HTTPException(status_code=410, detail=message)
    
    return {
        "status": status_text, 
        "message": message
    }

@app.post("/auth/claim_token")
async def claim_token(data: ClaimTokenRequest):
    """
    Endpoint para reclamar el token. 
    Valida identidad en SQLite y genera la firma en Rust.
    """
    
    if data.type != "claim_token":
        raise HTTPException(status_code=400, detail="Tipo de solicitud (QR type) inválido")

    result = db.get_token(
        device_id=data.device_id, 
        device_name=data.device_name, 
        username=data.username
    )
    
    if result["success"]:
        return {
            "status": "success",
            "token": result["token"],
            "expires_at": result.get("expires_at"),
            "message": "Dispositivo autorizado exitosamente."
        }
    
    raise HTTPException(
        status_code=401, 
        detail=result.get("diagnostic", "Acceso denegado o sesión expirada.")
    )

@app.get("/auth/validate_credentials")
async def validate_credentials(
    device_id: str,
    username: str,
    credentials: HTTPAuthorizationCredentials = Depends(security)
):
    """Verifica si la sesión sigue viva usando el helper."""
    await verify_device_access(device_id, credentials.credentials, username)
    
    return {
        "status": "valid",
        "username": username,
        "message": "Acceso concedido"
    }

@app.post("/ia/process_audio")
async def process_audio(
    background_tasks: BackgroundTasks,
    device_id: str = Form(...),
    username: str = Form(...),
    token: str = Form(...),
    audio_file: UploadFile = File(...)
):
    """
    Valida credenciales y delega el guardado al AudioProcessor.
    """
    
    await verify_device_access(device_id, token, username)

    
    if not audio_file.filename.lower().endswith(".m4a"):
        raise HTTPException(status_code=400, detail="Formato no soportado")

    content = await audio_file.read()

    background_tasks.add_task(
        audio_processor.save_audio, 
        content, 
        audio_file.filename
    )

    return {
        "status": "accepted",
        "message": "Audio recibido y enviado a procesamiento.",
        "filename": audio_file.filename
    }

@app.get("/ia/processed_audios")
async def get_processed_audios(
    device_id: str = Query(..., description="ID único del dispositivo móvil"),
    username: str = Query(..., description="Nombre de usuario que solicita los JSONs"),
    auth: HTTPAuthorizationCredentials = Depends(security)
):
    """
    Endpoint seguro que valida las credenciales del dispositivo 
    antes de verificar la existencia de transcripciones procesadas.
    """

    await verify_device_access(device_id, auth.credentials, username)

    json_dir = Path(PathManager.get_json_dir())
    
    processed_audios_list = []

    if json_dir.exists() and json_dir.is_dir():
        for file_path in json_dir.glob("*.json"):
            should_delete = True
            
            try:
                with open(file_path, "r", encoding="utf-8") as f:
                    json_content = json.load(f)
                
                if isinstance(json_content, dict) and set(json_content.keys()) == REQUIRED_KEYS:
                    processed_audios_list.append(json_content)
                else:
                    print(f"⚠️ [JSON Validation] Archivo {file_path.name} no cumple con la estructura. Se descartará y eliminará.")
            
            except (json.JSONDecodeError, TypeError, ValueError):
                print(f"⚠️ [JSON Corrupto] El archivo {file_path.name} está mal formado. Se eliminará.")
            except IOError as e:
                print(f"❌ [IO Error] No se pudo acceder al archivo {file_path.name}: {e}")
                should_delete = False

            if should_delete:
                try:
                    file_path.unlink(missing_ok=True)
                except Exception as e:
                    print(f"❌ [OS Error] No se pudo eliminar el archivo físico {file_path.name}: {e}")

    return {
        "status": "success",
        "count": len(processed_audios_list),
        "audios": processed_audios_list
    }

async def verify_device_access(device_id: str, token: str, username: str):
    """
    Helper centralizado para validar la integridad del token con Rust 
    y el estado del dispositivo en SQLite.
    """
    
    if not noir_security.validate_token(PathManager.get_config_dir(), token, device_id, username):
        raise HTTPException(
            status_code=401, 
            detail="Credenciales Inválidas."
        )

    device_status = db.get_device_status(device_id)
    
    if not device_status:
        raise HTTPException(status_code=404, detail="Dispositivo no registrado")
    
    if device_status["banned"]:
        raise HTTPException(status_code=403, detail="Dispositivo baneado")
        
    if not device_status["is_active"]:
        raise HTTPException(status_code=403, detail="Acceso desactivado")
    
    return device_status

@app.exception_handler(404)
async def custom_404_handler(request: Request, __):
    return HTMLResponse(
        content="<html><body><h1>404 Not Found</h1></body></html>", 
        status_code=404
    )
