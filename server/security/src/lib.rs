use pyo3::prelude::*;
use pyo3::exceptions::{PyValueError, PyRuntimeError};
use jsonwebtoken::{encode, decode, Header, Validation, EncodingKey, DecodingKey};
use serde::{Serialize, Deserialize};
use std::path::{Path, PathBuf};

use std::sync::mpsc::channel;
use std::time::Duration as StdDuration;
use chrono::{Utc, Duration as ChronoDuration}; 

use notify::{Watcher, RecursiveMode, Config as NotifyConfig};

use rand::RngCore;
use base64::engine::general_purpose::URL_SAFE_NO_PAD;
use base64::Engine;

mod file_manager;
mod config;
use crate::file_manager::FileManager;
use crate::config::{Config, ServerConfig, AuthConfig, QrConfig};

#[derive(Debug, Serialize, Deserialize)]
struct Claims {
    sub: String,      // device_id
    device_name: String,
    username: String,
    iss: String,      // Nombre de la instancia
    iat: usize,       // Issued at
    exp: usize,       // Expiration
}

// ====================== FUNCIONES INTERNAS ======================

fn get_config_path<P: AsRef<Path>>(config_dir_path: P) -> PathBuf {
    Path::new(config_dir_path.as_ref())
        .join("config.json")
}

fn load_config<P: AsRef<Path>>(config_dir_path: P) -> Result<Config, String> {
    let path = get_config_path(config_dir_path);
    
    let path_str = path.to_str()
        .ok_or_else(|| "La ruta generada contiene caracteres Unicode inválidos".to_string())?;
    
    FileManager::read_json::<Config>(path_str)
        .map_err(|e| format!("Error al cargar configuración en {:?}: {}", path, e))
}

fn generate_jwt_secret() -> String {
    let mut bytes = [0u8; 64];
    rand::thread_rng().fill_bytes(&mut bytes);
    URL_SAFE_NO_PAD.encode(bytes)
}

fn json_filename_error_log(py: Python<'_>, err: PyErr) {
    eprintln!("❌ Rust [Watcher]: El callback de Python arrojó un error: {:?}", err);
    err.print(py);
}

// ====================== FUNCIONES PARA PYTHON ======================
#[pyfunction]
fn generate_approval_token(
    config_dir_path: String,
    device_id: String,
    device_name: String,
    username: String
) -> PyResult<(String, String)> {
    let cfg = load_config(&config_dir_path).map_err(PyRuntimeError::new_err)?;

    let iat = Utc::now();
    let exp_date = iat + ChronoDuration::days(cfg.auth.jwt_expire_days as i64);

    let my_claims = Claims {
        sub: device_id.clone(), // Clonamos para los log s si es necesario
        device_name: device_name.clone(),
        username: username.clone(),
        iss: cfg.server.name,
        iat: iat.timestamp() as usize,
        exp: exp_date.timestamp() as usize,
    };
    
    let token = encode(
        &Header::default(),
        &my_claims,
        &EncodingKey::from_secret(cfg.auth.jwt_secret.as_bytes()),
    ).map_err(|e| PyRuntimeError::new_err(format!("Error al firmar: {}", e)))?;

    let exp_str = exp_date.format("%Y-%m-%d %H:%M:%S").to_string();

    println!("🔐 Rust: Token generado para {}", username);
    println!("📅 Expira el: {}", exp_str);
    
    Ok((token, exp_str))
}

#[pyfunction]
fn validate_token(
    config_dir_path: String,
    token: String,
    device_id: String,
    username: String
) -> PyResult<bool> {
    let cfg = load_config(&config_dir_path).map_err(PyRuntimeError::new_err)?;
    let validation = Validation::default();
    
    match decode::<Claims>(
        &token,
        &DecodingKey::from_secret(cfg.auth.jwt_secret.as_bytes()),
        &validation,
    ) {
        Ok(data) => {
            if data.claims.username != username {
                println!("⚠️ Rust: Username mismatch.");
                return Ok(false);
            }

            if data.claims.sub != device_id {
                println!("⚠️ Rust: Device ID mismatch.");
                return Ok(false);
            }

            println!("✅ Rust: Validacion Exitosa. ");
            Ok(true)
        }
        Err(e) => {
            println!("❌ Rust: Error de decodificación: {}", e);
            Ok(false)
        }
    }
}

#[pyfunction]
fn init_security(config_dir_path: String) -> PyResult<bool> {
    let path = get_config_path(&config_dir_path);
    let path_str = path.to_str()
        .ok_or_else(|| PyValueError::new_err("Ruta de directorio de configuración inválida"))?;
        
    if !FileManager::exists(path_str) {
        println!("⚠️ Rust: No se encontró config.json en: {:?}. Se requiere inicialización.", path);
        return Ok(false);
    }
    
    println!("🔐 Rust: Seguridad verificada con éxito.");
    Ok(true)
}

#[pyfunction]
fn create_default_config(
    config_dir_path: String,
    domain: String,
    port: u16,
    server_name: String,
    jwt_expire_days: u32,
    qr_expiration_minutes: u32
) -> PyResult<bool> {
    let path = get_config_path(&config_dir_path);
    let path_str = path.to_str()
        .ok_or_else(|| PyValueError::new_err("Ruta de directorio de configuración inválida"))?;

    if FileManager::exists(path_str) {
        return Err(PyRuntimeError::new_err("El archivo de configuración ya existe de forma física. Abortando sobreescritura."));
    }

    println!("⚙️ Rust: Generando llave criptográfica segura de 64 bytes...");
    let jwt_secret = generate_jwt_secret();

    let config = Config {
        server: ServerConfig {
            domain,
            port,
            name: server_name,
        },
        auth: AuthConfig {
            jwt_secret,
            jwt_expire_days,
        },
        qr: QrConfig {
            expiration_minutes: qr_expiration_minutes,
        },
    };

    if let Some(parent) = path.parent() {
        std::fs::create_dir_all(parent)
            .map_err(|e| PyRuntimeError::new_err(format!("No se pudo asegurar el directorio de configuración: {}", e)))?;
    }

    FileManager::write_json(path_str, &config)
        .map_err(|e| PyRuntimeError::new_err(format!("Error de persistencia del JSON: {}", e)))?;

    println!("🎉 Rust: Archivo config.json creado exitosamente en: {:?}", path);
    Ok(true)
}

#[pyfunction]
fn get_server_domain(config_dir_path: String) -> PyResult<String> {
    let cfg = load_config(&config_dir_path).map_err(PyRuntimeError::new_err)?;
    Ok(cfg.server.domain)
}

#[pyfunction]
fn get_server_port(config_dir_path: String) -> PyResult<u16> {
    let cfg = load_config(&config_dir_path).map_err(PyRuntimeError::new_err)?;
    Ok(cfg.server.port)
}

#[pyfunction]
fn start_audio_watcher(
    py: Python<'_>, // 1. Solicitamos el token del hilo de Python actual en los argumentos
    upload_dir_path: String, 
    callback: PyObject
) -> PyResult<()> {
    let path_to_watch = Path::new(&upload_dir_path);

    if !path_to_watch.exists() {
        return Err(PyValueError::new_err(format!(
            "El directorio de audios especificado no existe: {}", upload_dir_path
        )));
    }

    println!("📡 Rust: Watcher de audios iniciado en: {:?}", path_to_watch);

    let (tx, rx) = channel();
    let config = NotifyConfig::default().with_poll_interval(StdDuration::from_millis(500));

    let mut watcher = notify::RecommendedWatcher::new(tx, config)
        .map_err(|e| PyRuntimeError::new_err(format!("No se pudo crear el Watcher: {}", e)))?;

    watcher.watch(path_to_watch, RecursiveMode::NonRecursive)
        .map_err(|e| PyRuntimeError::new_err(format!("Error al activar observación: {}", e)))?;

    // 2. 🚀 LIBERAMOS EL GIL AQUÍ MIENTRAS ESCUCHAMOS EL EMBUDO DE EVENTOS
    py.allow_threads(|| {
        for res in rx {
            match res {
                Ok(event) => {
                    if event.kind.is_create() || event.kind.is_modify() {
                        
                        // 3. READQUIRIMOS el GIL de forma segura única y exclusivamente
                        // para ejecutar la función de callback de Python
                        Python::with_gil(|py_gil| {
                            println!("🔔 Rust [Watcher]: ¡Detectado nuevo archivo! Avisando al backend...");
                            
                            let _ = callback.call0(py_gil).map_err(|e| {
                                json_filename_error_log(py_gil, e); // Manejo preventivo
                            });
                        });
                    }
                }
                Err(e) => println!("⚠️ Rust [Watcher]: Error en el evento de escaneo: {:?}", e),
            }
        }
    });

    Ok(())
}

// ====================== MÓDULO PYTHON ======================

#[pymodule]
fn noir_core(m: &Bound<'_, PyModule>) -> PyResult<()> {
    let py = m.py();

    let security_mod = PyModule::new_bound(py, "noir_security")?;
    security_mod.add_function(wrap_pyfunction!(init_security, &security_mod)?)?;
    security_mod.add_function(wrap_pyfunction!(create_default_config, &security_mod)?)?;
    security_mod.add_function(wrap_pyfunction!(generate_approval_token, &security_mod)?)?;
    security_mod.add_function(wrap_pyfunction!(validate_token, &security_mod)?)?;
    security_mod.add_function(wrap_pyfunction!(get_server_domain, &security_mod)?)?;
    security_mod.add_function(wrap_pyfunction!(get_server_port, &security_mod)?)?;
    m.add_submodule(&security_mod)?;

    let utils_mod = PyModule::new_bound(py, "noir_utils")?;
    utils_mod.add_function(wrap_pyfunction!(start_audio_watcher, &utils_mod)?)?;
    m.add_submodule(&utils_mod)?;

    Ok(())
}