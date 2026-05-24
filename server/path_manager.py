import os

class PathManager:
    _MANAGER_DIR = os.path.dirname(os.path.abspath(__file__))
    
    BASE_DIR = os.path.abspath(_MANAGER_DIR)
    
    CONFIG_DIR = os.path.join(BASE_DIR, "configs_data")
    UPLOAD_DIR = os.path.join(BASE_DIR, "received_audios")
    JSON_DIR = os.path.join(BASE_DIR, "json_outputs")

    DB_PATH = os.path.join(CONFIG_DIR, "noir_auth.db")

    @classmethod
    def initialize_directories(cls):
        """
        Se encarga de crear físicamente toda la topología de carpetas 
        del sistema antes de que los servicios intenten escribir en ellas.
        """
        print("📁 [PathManager] Asegurando entorno de directorios absolutos...")
        os.makedirs(cls.CONFIG_DIR, exist_ok=True)
        os.makedirs(cls.UPLOAD_DIR, exist_ok=True)
        os.makedirs(cls.JSON_DIR, exist_ok=True)
        print(f"📌 [PathManager] Directorio base anclado en: {cls.BASE_DIR}")

    @classmethod
    def get_config_dir(cls) -> str:
        return cls.CONFIG_DIR

    @classmethod
    def get_upload_dir(cls) -> str:
        return cls.UPLOAD_DIR

    @classmethod
    def get_json_dir(cls) -> str:
        return cls.JSON_DIR
    
    @classmethod
    def get_db_path(cls) -> str:
        return cls.DB_PATH