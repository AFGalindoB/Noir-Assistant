import os
from datetime import datetime
import gc
from faster_whisper import WhisperModel
import json
import re
from path_manager import PathManager

WHISPER_MODEL_SIZE = "medium"
WHISPER_COMPUTE_TYPE = "int8" 

def save_audio(file_content: bytes, filename: str):
    """
    Se encarga de la persistencia física del audio.
    """
    try:
        file_path = os.path.join(PathManager.get_upload_dir(), filename)

        with open(file_path, "wb") as f:
            f.write(file_content)

        print(f"📦 [AudioProcessor] Archivo guardado exitosamente: {filename}")
        return True
    
    except Exception as e:
        print(f"❌ [AudioProcessor] Error al guardar archivo: {e}")
        return False

def run_stt_transcription(audio_path: str) -> str:
    """
    Carga el modelo STT en memoria, transcribe el audio y libera 
    la RAM inmediatamente al terminar.
    """
    print("🧠 [STT] Cargando Faster-Whisper en memoria RAM...")
    
    whisper_model = WhisperModel(
        WHISPER_MODEL_SIZE, 
        device="cpu", 
        compute_type=WHISPER_COMPUTE_TYPE
    )
    
    print("🎙️ [STT] Transcribiendo archivo de audio...")
    segments, _ = whisper_model.transcribe(audio_path)
    texto = " ".join([seg.text for seg in segments])
    
    print(f"📝 [STT] Transcripción completada.")
    
    del whisper_model
    
    gc.collect()
    print("♻️ [STT] Modelo Faster-Whisper descargado de la RAM con éxito.")
    
    return texto

def extract_readable_date(filename: str) -> str:
    """
    Extrae el número de caracteres del timestamp del nombre del archivo 
    y lo convierte a una cadena legible 'YYYY-MM-DD HH:MM:SS'.
    Ejemplo: 'audio_1779082922438.m4a' -> 'Audio Nota - 2026-05-20 00:14:37'
    """
    try:
        
        match = re.search(r"\d+", filename)
        if match:
            timestamp_ms = int(match.group(0))
            timestamp_seconds = timestamp_ms / 1000.0
            
            dt = datetime.fromtimestamp(timestamp_seconds)
            return f"Nota de voz - {dt.strftime('%Y-%m-%d %H:%M:%S')}"
            
    except Exception as e:
        print(f"⚠️ [TimeUtils] No se pudo parsear el timestamp: {e}")
    
    return f"Nota de voz - {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}"

def transcribe_and_process(audio_path: str) -> str:
    """
    Orquestador del pipeline en exclusión mutua de memoria.
    """

    texto_transcrito = run_stt_transcription(audio_path)

    filename = os.path.basename(audio_path)
    
    if not texto_transcrito.strip():
        print("⚠️ [Pipeline] Audio sin contenido o inteligible")
        return "{}"
        
    print(f"💬 Texto extraído: {texto_transcrito}\n")
    
    titulo_legible = extract_readable_date(filename)
    
    final_output = {
        "header": "Noir Assistant - Transcripción de Audio",
        "audio_filename": filename,
        "titulo": titulo_legible,
        "descripcion": texto_transcrito.strip(),
        "fecha": ""
    }
    
    json_filename = os.path.splitext(filename)[0] + ".json"
    json_path = os.path.join(PathManager.get_json_dir(), json_filename)
    
    try:
        with open(json_path, "w", encoding="utf-8") as f:
            json.dump(final_output, f, ensure_ascii=False, indent=2)
        print(f"💾 [Pipeline] JSON guardado con éxito en: {json_path}")
    except Exception as e:
        print(f"❌ [Pipeline] Error al guardar el archivo JSON: {e}")
        
    return json.dumps(final_output, ensure_ascii=False)

if __name__ == "__main__":
    test_audio = "received_audios/test.m4a"
    resultado = transcribe_and_process(test_audio)
    print(f"Resultado final del pipeline:\n{resultado}")