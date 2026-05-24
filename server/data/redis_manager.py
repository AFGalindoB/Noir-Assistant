import redis
import json
import uuid
from datetime import datetime
import data.database as db

redis_client = redis.Redis(
    host='localhost', 
    port=6379, 
    db=0, 
    decode_responses=True,
    socket_connect_timeout=2.0,
    socket_timeout=2.0,
    retry_on_timeout=False
)

def check_redis_connection() -> bool:
    """Verifica si Redis está encendido. Si no, mata el proceso con un error claro."""
    try:
        if redis_client.ping():
            print("[Redis Manager] Redis ONLINE.")
            return True
    except (redis.exceptions.ConnectionError, redis.exceptions.TimeoutError):
        print("\n" + "="*50)
        print("[ERROR CRÍTICO] No se pudo conectar con Redis.")
        print("Causa: El contenedor de Redis está apagado o fuera de alcance.")
        print("="*50 + "\n")
        return False

def create_qr_session(ttl: int = 300) -> str:
    """
    Al generar un QR, creamos un SID único y lo almacenamos en Redis con un TTL.
     - El QR contiene el SID, que es lo que la App escaneará para reclamar la sesión.
     - El TTL asegura que el QR expire después de un tiempo si no se reclama.
    """
    sid = str(uuid.uuid4())
    redis_client.setex(f"qr_sid:{sid}", ttl, "1")
    return sid

def claim_qr_session(sid: str, device_id: str, device_name: str) -> tuple[int, str]:
    """
    Ahora retorna un (Código de Estado, Mensaje).
    Códigos internos: 
    200: Nuevo registro/Pendiente
    202: Ya autorizado
    403: Baneado
    410: QR Inválido
    """
    qr_key = f"qr_sid:{sid}"

    if not redis_client.exists(qr_key):
        print("[Redis Manager] QR Inválido, expirado o ya reclamado")
        return 410, "QR Inválido, expirado o ya reclamado"

    redis_client.delete(qr_key)

    if db.device_is_banned(device_id):
        print(f"[Redis Manager] Intento de acceso desde dispositivo baneado: {device_id}")
        return 403, "Dispositivo baneado. Contacta al administrador."

    device_status = db.get_device_status(device_id)
    if device_status and device_status["is_active"]:
        print(f"[Redis Manager] Dispositivo {device_id} ya está activo. Saltando a 202.")
        return 202, "Este dispositivo ya está autorizado. Procede a reclamar tu token."

    if redis_client.exists(f"pending:{device_id}"):
        print("[Redis Manager] Solicitud ya existente, esperando aprobación")
        return 200, "Solicitud ya existente, esperando aprobación"

    pending_data = {
        "device_id": device_id,
        "device_name": device_name,
        "status": "pending_approval",
        "timestamp": datetime.now().isoformat()
    }
    
    redis_client.setex(f"pending:{device_id}", 86400, json.dumps(pending_data))
    
    print("[Redis Manager] Esperando aprobación manual")
    return 200, "Solicitud recibida. Espera la aprobación en el panel."

def get_pending_request(device_id: str) -> dict | None:
    """Obtiene la data de una solicitud pendiente."""
    raw = redis_client.get(f"pending:{device_id}")
    return json.loads(raw) if raw else None

def get_all_pending() -> list[dict]:
    """Recupera todas las peticiones para el Admin Panel."""
    keys = redis_client.keys("pending:*")
    pending_list = []
    for key in keys:
        data = redis_client.get(key)
        if data:
            pending_list.append(json.loads(data))
    return pending_list

def delete_pending_request(device_id: str) -> tuple[bool, str]:
    """Elimina una solicitud pendiente de Redis (usado al aprobar o rechazar)."""
    if redis_client.exists(f"pending:{device_id}"):
        redis_client.delete(f"pending:{device_id}")
        return True, "[Redis Manager] Solicitud eliminada con exito"
    return False, "[Redis Manager] La solicitud no existe o ya expiró."
