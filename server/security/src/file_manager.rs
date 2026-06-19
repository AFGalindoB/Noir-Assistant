use serde::Serialize;
use std::fs::{self, OpenOptions};
use std::io::Write;
use std::path::Path;
use anyhow::{Context, Result, bail};

/// Módulo encargado de todas las operaciones de archivos
pub struct FileManager;

impl FileManager {
    pub fn write_json<T: Serialize>(path: &str, data: &T) -> Result<()> {
        if let Some(parent) = Path::new(path).parent() {
            fs::create_dir_all(parent)
                .context(format!("No se pudo crear el directorio para {}", path))?;
        }

        let json = serde_json::to_string_pretty(data)
            .context("Error al convertir datos a JSON")?;

        fs::write(path, json)
            .context(format!("Error al escribir el archivo {}", path))?;

        println!("📁 Archivo JSON guardado: {}", path);
        Ok(())
    }

    pub fn read_json<T: for<'de> serde::Deserialize<'de>>(path: &str) -> Result<T> {
        let content = fs::read_to_string(path)
            .context(format!("No se pudo leer el archivo: {}", path))?;

        let data: T = serde_json::from_str(&content)
            .context(format!("Error al parsear el JSON: {}", path))?;

        Ok(data)
    }

    pub fn exists(path: &str) -> bool {
        Path::new(path).exists()
    }

    pub fn read_string(path: &str) -> Result<String> {
        fs::read_to_string(path)
            .context(format!("No se pudo leer el archivo: {}", path))
    }

    pub fn upsert_string(path: &str, content: &str) -> Result<()> {
        let path_obj = Path::new(path);

        if let Some(parent) = path_obj.parent() {
            if !parent.exists() {
                bail!(
                    "Fallo de entorno: El directorio padre esperado '{:?}' no existe de forma física. Abortando operación.", 
                    parent
                );
            }
        } else {
            bail!("La ruta proporcionada '{}' no tiene un directorio contenedor válido.", path);
        }

        let mut file = OpenOptions::new()
            .write(true)
            .create(true)
            .truncate(true)
            .open(path)
            .context(format!("No se pudo abrir o crear el archivo en modo upsert: {}", path))?;

        file.write_all(content.as_bytes())
            .context(format!("Error de bajo nivel al escribir contenido en: {}", path))?;

        file.flush()
            .context(format!("Error al sincronizar los buffers en el disco: {}", path))?;

        println!("[Rust: FileManager] Upsert de archivo completado con éxito en: {}", path);
        Ok(())
    }
}