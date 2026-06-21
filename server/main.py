import threading
import uvicorn
import tkinter as tk
from tkinter import TclError
from noir_core import noir_security, noir_utils
import data.database as db
import data.redis_manager as rm
from path_manager import PathManager
from api_server import app
from admin_panel import AdminApp
import audio_processor
import os
import re
import getpass

class NoirSystem:
    def __init__(self):
        self.root = None
        self.server_thread = None
        self.watcher_thread = None
        self.host = "0.0.0.0"
        self.TIMEOUT_MS = 300000

    def validate_password_format(self, password: str) -> bool:
        """Verifica longitud mínima de 8, un número y un carácter especial."""
        if len(password) < 8:
            print("❌ La contraseña debe tener al menos 8 caracteres.")
            return False
        if not re.search(r"\d", password):
            print("❌ La contraseña debe contener al menos un número.")
            return False
        if not re.search(r"[!@#$%^&*(),.?\":{}|<>]", password):
            print("❌ La contraseña debe contener al menos un carácter especial.")
            return False
        return True

    def prompt_config_inputs(self) -> tuple:
        """
        Función independiente encargada puramente de la interacción con el usuario
        para recolectar los datos de configuración inicial.
        """

        print("\n✨ [Security] Asistente de configuración de Noir Assistant...")
        domain = input("Ingrese el dominio para el servidor: ")
        
        while True:
            try:
                port = int(input("Ingrese el puerto para el servidor: "))
                break
            except ValueError:
                print("⚠️ Por favor, ingrese un número de puerto válido.")

        server_name = input("Ingrese un nombre para el servidor: ")
        
        while True:
            try:
                jwt_expire_days = int(input("Ingrese los días de expiración para JWT: "))
                qr_expiration_minutes = int(input("Ingrese los minutos de expiración para QR: "))
                break
            except ValueError:
                print("⚠️ Por favor, ingrese valores numéricos válidos.")

        while True:
            print("\n--- Configuración de credenciales de Administrador ---")
            password = getpass.getpass("Establezca la contraseña maestra de administrador: ")
            
            if self.validate_password_format(password):
                confirm_password = getpass.getpass("Confirme la contraseña: ")
                if password == confirm_password:
                    break
                print("❌ Las contraseñas no coinciden. Intente de nuevo.")
                
        return domain, port, server_name, jwt_expire_days, qr_expiration_minutes, password

    def check_services(self):
        """Verifica la integridad de los servicios externos y seguridad."""
        print("[System] Verificando servicios...")

        PathManager.initialize_directories()

        config_path = PathManager.get_config_dir()

        entorno_existe = noir_utils.is_configured(config_path)

        if entorno_existe:
            try:
                print("[Security] Entorno detectado.")
                password = getpass.getpass("Introduzca la contraseña maestra para iniciar el sistema: ")
                
                noir_security.init_security(config_path, password)
                print("✅ [Security] Entorno verificado y cargado en RAM.")
            except FileNotFoundError:
                print("\n⚠️ [Anomalía] El archivo config.json desapareció inesperadamente antes de la carga.")

                domain, port, s_name, jwt_days, qr_min, password = self.prompt_config_inputs()
                
                try:
                    noir_security.create_default_config(config_path, domain, port, s_name, jwt_days, qr_min, password)
                    noir_security.init_security(config_path, password)
                    print("Configuración de seguridad regenerada y cargada en RAM con éxito.")
                except Exception as e:
                    print(f"[FATAL] Error al recrear el entorno volátil: {e}")
                    return False
                    
            except ValueError as e:
                print(f"❌ [Security] Acceso Denegado: {e}")
                return False
            except Exception as e:
                print(f"❌ [FATAL] Error inesperado en el Rust Core: {e}")
                return False
        else:
            domain, port, s_name, jwt_days, qr_min, password = self.prompt_config_inputs()
            try:
                noir_security.create_default_config(config_path, domain, port, s_name, jwt_days, qr_min, password)
                noir_security.init_security(config_path, password)
                print("✅ Configuración de seguridad inicializada y cargada en RAM exitosamente.")
            except Exception as e:
                print(f"❌ [FATAL] Error en la inicialización base: {e}")
                return False

        # Redis
        print("[System] Verificando conexión a Redis...")
        if not rm.check_redis_connection():
            print("❌ [System] Redis no está disponible.")
            return False

        # SQL
        print("[System] Verificando persistencia SQLite...")
        db.init_db()
        
        return True

    def run_watcher_service(self):
        """El hilo del centinela llama a la librería de utilidades."""
        try:
            upload_path = PathManager.get_upload_dir()
            noir_utils.start_audio_watcher(upload_path, self.on_audio_received_callback)
        except Exception as e:
            print(f"⚠️ [Watcher Error] Error en el hilo de utilidades de Rust: {e}")

    def run_api_server(self):
        """Lógica para ejecutar Uvicorn."""
        try:
            port = noir_security.get_server_port()
            print(f"🚀 [Server] Motor API activo en http://{self.host}:{port}")
            uvicorn.run(app, host=self.host, port=port, log_level="info")
        except Exception as e:
            print(f"⚠️ [Server Error] Error en el hilo del servidor: {e}")

    def on_audio_received_callback(self):
        """
        Esta es la función que disparará Rust de inmediato.
        Python retoma el control, escanea la carpeta y procesa el menor timestamp.
        """
        print("📥 [Python Core] Alerta recibida de Rust. Iniciando escaneo de prioridades...")
        
        upload_dir = PathManager.get_upload_dir()
        archivos = os.listdir(upload_dir)
        
        if not archivos:
            print("🔍 [Python Core] Falsa alarma o carpeta vacía.")
            return

        def extraer_timestamp(nombre_archivo):
            numeros = ''.join(c for c in nombre_archivo if c.isdigit())
            return int(numeros) if numeros else float('inf')

        archivos_ordenados = sorted(archivos, key=extraer_timestamp)
        primer_audio = archivos_ordenados[0]
        
        print(f"🎯 [Python Core] Audio prioritario detectado: {primer_audio}")
        
        ruta_absoluta_audio = os.path.join(upload_dir, primer_audio)
        try:
            audio_processor.transcribe_and_process(ruta_absoluta_audio)
            
            if os.path.exists(ruta_absoluta_audio):
                os.remove(ruta_absoluta_audio)
                print(f"🗑️ [Python Core] Archivo de origen eliminado para liberar espacio: {primer_audio}")
                
        except Exception as e:
            print(f"❌ [Python Core] Error procesando la cola de audio: {e}")

    def start(self):
        """Punto de entrada principal del sistema."""
        
        if not self.check_services():
            print("[System] Abortando por fallo en servicios.")
            return

        self.watcher_thread = threading.Thread(target=self.run_watcher_service, daemon=True)
        self.watcher_thread.start()

        self.server_thread = threading.Thread(target=self.run_api_server, daemon=True)
        self.server_thread.start()

        opciones = {"Abrir Panel de Administración (GUI)":1, "Salir la Aplicación":2}
        lista_opciones = list(opciones.keys())

        while True:
            print("\n" + "="*40)
            for i, op in enumerate(lista_opciones, start=1):
                print(f"[{i}] {op}")
            print("="*40)

            try:
                opcion = int(input("Ingrese el índice de una opción: "))
            except ValueError:
                print("⚠️ Por favor, ingrese un número válido.")
                continue

            if 0 < opcion <= len(lista_opciones):
                if opcion == 1:
                    print("\n🔒 Acceso Restringido")
                    password_input = getpass.getpass("Introduce la contraseña de administrador: ")
            
                    try:
                        noir_security.verify_admin_password(password_input)
                        print("🔓 Acceso concedido. Lanzando Panel de Administración...")

                        try:
                            self.root = tk.Tk()
                            self.root.title("Noir Assistant - Centro de Control")
                            
                            app_ui = AdminApp(self.root)

                            def auto_close_panel():
                                print("\n⏰ [UI] El tiempo de sesión del panel ha expirado. Cerrando automáticamente...")
                                if hasattr(self, 'root') and self.root:
                                    self.root.destroy()

                            self.root.after(self.TIMEOUT_MS, auto_close_panel)

                            try:
                                self.root.mainloop() 
                                print("\n[UI] Panel de administración cerrado. Volviendo a la CLI...")
                            except KeyboardInterrupt:
                                print("\n[System] Interrupción detectada desde la interfaz.")
                                self.shutdown()
                                break
                        except TclError as e:
                            print("\n❌ [ERROR DE INTERFAZ] No se pudo inicializar el panel gráfico.")
                            print(f"   Detalle: {e}")
                            print("   Sugerencia: Asegúrate de estar en un entorno con servidor gráfico activo (X11/Wayland) o reenvío de GUI (SSH -X).")
                            print("   El servidor CLI y el backend siguen ejecutándose con normalidad.\n")
                    except ValueError:
                        print("[Error] Contraseña incorrecta. Acceso denegado.")
                    except Exception as e:
                        print(f"[FATAL] Error inesperado en el Rust Core: {e}")
                        return False
                        
                elif opcion == 2:
                    print("\n[System] Iniciando el proceso de apagado...")
                    self.shutdown()
                    break
            else:
                print("⚠️ Opción no válida. Por favor, intente de nuevo.")

    def shutdown(self):
        """Limpieza al cerrar."""
        print("\n[System] Apagando Noir Assistant...")
        print("[System] Bye.")

if __name__ == "__main__":
    noir = NoirSystem()
    noir.start()