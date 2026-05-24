import sqlite3
import data.redis_manager as rm
from noir_core import noir_security
from path_manager import PathManager
from datetime import datetime, timedelta

def get_db() -> sqlite3.Connection:
    conn = sqlite3.connect(PathManager.get_db_path())
    conn.row_factory = sqlite3.Row
    return conn

def init_db() -> None:
    """Crea la tabla de dispositivos autorizados si no existe."""
    with get_db() as conn:
        conn.execute("""
            CREATE TABLE IF NOT EXISTS authorized_devices (
                device_id TEXT PRIMARY KEY,
                device_name TEXT NOT NULL,
                username TEXT NULLABLE,
                is_active INTEGER DEFAULT 1,
                banned INTEGER DEFAULT 0,
                created_at TEXT,
                expires_at TEXT
            )
        """)
        conn.commit()

def approve_device(device_id: str, device_name: str, username: str) -> bool:
    """
    Mueve el dispositivo de Redis a SQLite de forma permanente.
    Calcula la fecha de expiración solicitada.
    """

    if not rm.get_pending_request(device_id):
        return False

    try:
        created_at = datetime.now()

        with get_db() as conn:

            print("[BD] Buscando dispositivos")
            cursor = conn.execute(
                "SELECT device_id FROM authorized_devices WHERE device_id = ?", 
                (device_id,)
            )
            existing_device = cursor.fetchone()

            if existing_device:
                print("[BD] Actualizando dispositivos")
                conn.execute("""
                    UPDATE authorized_devices 
                    SET username = ?, 
                        is_active = 1
                    WHERE device_id = ?
                """, (username, device_id))
                print(f"🔄 [DB] Dispositivo {device_id} reactivado.")
            else:
                print("[BD] Creando Dispositivo Nuevo")
                conn.execute("""
                    INSERT INTO authorized_devices 
                    (device_id, device_name, username, is_active, banned, created_at)
                    VALUES (?, ?, ?, 1, 0, ?)
                """, (device_id, device_name, username, created_at))
                print(f"✨ [DB] Nuevo dispositivo {device_id} registrado.")
            conn.commit()
        
        rm.delete_pending_request(device_id)
        return True
        
    except Exception as e:
        print(f"[DB Error] Fallo al aprobar dispositivo: {e}")
        return False

def get_token(device_id: str, device_name: str, username: str) -> dict:
    """
    Valida identidad contra SQLite y genera el token dinámicamente con Rust.
    Actualiza la fecha de expiración en la DB para registro histórico.
    """
    try:
        with get_db() as conn:
            query = """
                SELECT is_active, banned 
                FROM authorized_devices 
                WHERE device_id = ? AND device_name = ? AND username = ?
            """
            row = conn.execute(query, (device_id, device_name, username)).fetchone()

            if not row:
                return {
                    "success": False, 
                    "diagnostic": "Credenciales inválidas o dispositivo no aprobado."
                }

            if row["banned"] == 1:
                return {"success": False, "diagnostic": "Dispositivo baneado permanentemente."}

            if row["is_active"] == 0:
                return {"success": False, "diagnostic": "El acceso para este dispositivo está desactivado."}

            token, expires_at = noir_security.generate_approval_token(
                PathManager.get_config_dir(),
                device_id, 
                device_name, 
                username
            )

            conn.execute("""
                UPDATE authorized_devices 
                SET expires_at = ? 
                WHERE device_id = ?
            """, (expires_at, device_id))
            conn.commit()

            return {
                "success": True, 
                "token": token,
                "expires_at": expires_at
            }

    except Exception as e:
        print(f"❌ [Error get_token]: {e}")
        return {"success": False, "diagnostic": "Error interno al generar la sesión."}

def get_device_status(device_id: str) -> dict | None:
    """
    Consulta el estado administrativo de un dispositivo específico.
    No valida el token, solo el permiso del dispositivo en el sistema.
    """
    try:
        with get_db() as conn:
            conn.row_factory = sqlite3.Row 
            
            row = conn.execute("""
                SELECT is_active, banned 
                FROM authorized_devices 
                WHERE device_id = ?
            """, (device_id,)).fetchone()
            
            if row:
                return {
                    "is_active": bool(row["is_active"]),
                    "banned": bool(row["banned"])
                }
            return None

    except Exception as e:
        print(f"❌ Error al consultar estado del dispositivo {device_id}: {e}")
        return None

def get_all_authorized_devices() -> list[dict]:
    """Recupera todos los dispositivos autorizados desde SQLite."""
    with get_db() as conn:
        rows = conn.execute("""
            SELECT device_id, device_name, username, created_at, is_active, banned, expires_at 
            FROM authorized_devices 
            ORDER BY created_at DESC
        """).fetchall()
        return [dict(row) for row in rows]
    
def toggle_device_status(device_id: str) -> tuple[bool, str]:
    """Activa o desactiva un dispositivo autorizado."""
    with get_db() as conn:
        row = conn.execute("""
            SELECT is_active FROM authorized_devices WHERE device_id = ?
        """, (device_id,)).fetchone()
        
        if not row:
            return False, "Dispositivo no encontrado"
        
        new_status = 0 if row["is_active"] == 1 else 1
        conn.execute("""
            UPDATE authorized_devices SET is_active = ? WHERE device_id = ?
        """, (new_status, device_id))
        conn.commit()
        
        return True, "Estado del dispositivo actualizado"

def toggle_device_ban(device_id: str) -> tuple[bool, str]:
    """Banea o desbanea un dispositivo autorizado."""
    with get_db() as conn:
        row = conn.execute("""
            SELECT banned FROM authorized_devices WHERE device_id = ?
        """, (device_id,)).fetchone()
        
        if not row:
            return False, "Dispositivo no encontrado"
        
        new_status = 0 if row["banned"] == 1 else 1
        conn.execute("""
            UPDATE authorized_devices SET banned = ? WHERE device_id = ?
        """, (new_status, device_id))
        conn.commit()
        
        return True, "Estado de ban del dispositivo actualizado"
    
def device_is_banned(device_id: str) -> bool:
    """Verifica si un dispositivo está baneado."""
    with get_db() as conn:
        row = conn.execute("""
            SELECT banned FROM authorized_devices WHERE device_id = ?
        """, (device_id,)).fetchone()
        
        if row and row["banned"] == 1:
            return True
    return False
