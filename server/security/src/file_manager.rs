use anyhow::{Context, Result};
use serde::Serialize;
use std::fs;
use std::path::Path;

/// Módulo encargado de todas las operaciones de archivos
pub struct FileManager;

impl FileManager {
    pub fn write_json<T: Serialize>(path: &str, data: &T) -> Result<()> {
        // Crear directorios padres si no existen
        if let Some(parent) = Path::new(path).parent() {
            fs::create_dir_all(parent)
                .context(format!("No se pudo crear el directorio para {}", path))?;
        }

        let json = serde_json::to_string_pretty(data)
            .context("Error al convertir datos a JSON")?;

        fs::write(path, json)
            .context(format!("Error al escribir el archivo {}", path))?;

        println!("📁 Archivo guardado: {}", path);
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
}