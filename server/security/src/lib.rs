use pyo3::prelude::*;
use pyo3::exceptions::{PyValueError, PyRuntimeError};
use jsonwebtoken::{encode, decode, Header, Validation, EncodingKey, DecodingKey};
use serde::{Serialize, Deserialize};
use std::path::{Path, PathBuf};
use anyhow::{Context, Result as AnyhowResult};

use std::sync::mpsc::channel;
use std::time::Duration as StdDuration;
use chrono::{Utc, Duration as ChronoDuration}; 
use bcrypt::{hash, verify, DEFAULT_COST};

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

fn get_secret_path<P: AsRef<Path>>(config_dir_path: P) -> PathBuf {
    Path::new(config_dir_path.as_ref()).join(".secret")
}

fn load_config<P: AsRef<Path>>(config_dir_path: P) -> Result<Config, String> {
    let path = get_config_path(config_dir_path);
    
    let path_str = path.to_str()
        .ok_or_else(|| "La ruta generada contiene caracteres Unicode inválidos".to_string())?;
    
    FileManager::read_json::<Config>(path_str)
        .map_err(|e| format!("Error al cargar configuración en {:?}: {}", path, e))
}

fn generate_and_save_jwt_secret(absolute_path: &str) -> AnyhowResult<String> {
    println!("⚙️ Rust: Iniciando generación de entropía para llave criptográfica...");
    let mut bytes = [0u8; 64];
    rand::thread_rng().fill_bytes(&mut bytes);
    let raw_jwt_secret = URL_SAFE_NO_PAD.encode(bytes);

    // Invocamos el nuevo método de abstracción de archivos
    FileManager::upsert_string(absolute_path, &raw_jwt_secret)
        .context("Fallo crítico en la capa de persistencia al procesar el upsert del secreto")?;
    
    Ok(".secret".to_string())
}

fn load_jwt_secret(config_dir_path: &str, relative_secret_path: &str) -> AnyhowResult<String> {
    let absolute_path = Path::new(config_dir_path).join(relative_secret_path);
    
    let path_str = absolute_path.to_str()
        .context("La ruta del secreto contiene caracteres Unicode inválidos")?;

    if !FileManager::exists(path_str) {
        println!("⚠️ Rust [Runtime]: Se intentó leer el secreto pero el archivo '{}' no existe.", relative_secret_path);
        println!("🔄 Rust [Runtime]: Regenerando llave criptográfica ausente en caliente...");
        
        generate_and_save_jwt_secret(path_str)
            .context("Fallo crítico al intentar autorecuperar el secreto en tiempo de ejecución")?;
            
        println!("✅ Rust [Runtime]: Llave regenerada con éxito.");
    }

    let secret_content = FileManager::read_string(path_str)
        .context("No se pudo leer el archivo físico del secreto")?;

    Ok(secret_content)
}

fn json_filename_error_log(py: Python<'_>, err: PyErr) {
    eprintln!("❌ Rust [Watcher]: El callback de Python arrojó un error: {:?}", err);
    err.print(py);
}

// ====================== FUNCIONES PARA PYTHON ======================

#[pyfunction]
fn create_default_config(
    config_dir_path: String,
    domain: String,
    port: u16,
    server_name: String,
    jwt_expire_days: u32,
    qr_expiration_minutes: u32,
    admin_password: String
) -> PyResult<bool> {
    let config_path = get_config_path(&config_dir_path);
    let secret_path = get_secret_path(&config_dir_path);
    
    let config_path_str = config_path.to_str()
        .ok_or_else(|| PyValueError::new_err("Ruta de archivo de configuración inválida"))?;
    let secret_path_str = secret_path.to_str()
        .ok_or_else(|| PyValueError::new_err("Ruta de archivo secreto inválida"))?;

    if FileManager::exists(config_path_str) {
        return Err(PyRuntimeError::new_err("El archivo de configuración ya existe de forma física. Abortando sobreescritura."));
    }

    let jwt_secret_relative_path = generate_and_save_jwt_secret(secret_path_str)
        .map_err(|e| PyRuntimeError::new_err(format!("Error en infraestructura de llaves: {}", e)))?;

    println!("Rust: Hasheando contraseña de administrador...");
    let hashed_password = hash(admin_password, DEFAULT_COST)
        .map_err(|e| PyRuntimeError::new_err(format!("Error al procesar la contraseña: {}", e)))?;

    let config = Config {
        server: ServerConfig {
            domain,
            port,
            name: server_name,
        },
        auth: AuthConfig {
            jwt_secret_path: jwt_secret_relative_path,
            jwt_expire_days,
            admin_password_hash: hashed_password,
        },
        qr: QrConfig {
            expiration_minutes: qr_expiration_minutes,
        },
    };

    FileManager::write_json(config_path_str, &config)
        .map_err(|e| PyRuntimeError::new_err(format!("Error de persistencia del JSON: {}", e)))?;

    println!("🎉 Rust: Entorno de configuración inicializado con éxito.");
    Ok(true)
}

#[pyfunction]
fn generate_approval_token(
    config_dir_path: String,
    device_id: String,
    device_name: String,
    username: String
) -> PyResult<(String, String)> {
    let cfg = load_config(&config_dir_path).map_err(PyRuntimeError::new_err)?;

    let jwt_secret_content = load_jwt_secret(&config_dir_path, &cfg.auth.jwt_secret_path)
        .map_err(|e| PyRuntimeError::new_err(format!("Fallo de seguridad al cargar la llave: {}", e)))?;

    let iat = Utc::now();
    let exp_date = iat + ChronoDuration::days(cfg.auth.jwt_expire_days as i64);

    let my_claims = Claims {
        sub: device_id.clone(),
        device_name: device_name.clone(),
        username: username.clone(),
        iss: cfg.server.name,
        iat: iat.timestamp() as usize,
        exp: exp_date.timestamp() as usize,
    };

    let token = encode(
        &Header::default(),
        &my_claims,
        &EncodingKey::from_secret(jwt_secret_content.as_bytes()),
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

    let jwt_secret_content = load_jwt_secret(&config_dir_path, &cfg.auth.jwt_secret_path)
        .map_err(|e| PyRuntimeError::new_err(format!("Fallo de seguridad al cargar la llave: {}", e)))?;
    
    let validation = Validation::default();
    
    match decode::<Claims>(
        &token,
        &DecodingKey::from_secret(jwt_secret_content.as_bytes()),
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
    let config_path = get_config_path(&config_dir_path);
    let config_path_str = config_path.to_str()
        .ok_or_else(|| PyValueError::new_err("Ruta de directorio de configuración inválida"))?;
        
    if !FileManager::exists(config_path_str) {
        println!("⚠️ Rust: No se encontró config.json en: {:?}. Se requiere inicialización completa.", config_path);
        return Ok(false);
    }
    
    let cfg = load_config(&config_dir_path).map_err(PyRuntimeError::new_err)?;
    
    let secret_path = get_secret_path(&config_dir_path);
    let secret_path_str = secret_path.to_str()
        .ok_or_else(|| PyValueError::new_err("Ruta del archivo secreto inválida durante la verificación"))?;

    if !FileManager::exists(secret_path_str) {
        println!("Rust [Integridad]: 'config.json' presente pero '{}' no fue encontrado.", cfg.auth.jwt_secret_path);
        println!("Rust [Autorrecuperación]: Reconstruyendo llave criptográfica ausente...");
        
        generate_and_save_jwt_secret(secret_path_str)
            .map_err(|e| PyRuntimeError::new_err(format!("Fallo crítico en la autorrecuperación del secreto: {}", e)))?;
            
        println!("Rust [Integridad]: Llave criptográfica regenerada y sincronizada exitosamente.");
        println!("Rust: Es posible que los dispositivos ya autorizados necesiten nuevas creedenciales.")
    }
    
    println!("Rust: Seguridad e integridad verificadas con éxito.");
    Ok(true)
}

#[pyfunction]
fn verify_admin_password(config_dir_path: String, password_to_check: String) -> PyResult<bool> {
    let cfg = load_config(&config_dir_path).map_err(PyRuntimeError::new_err)?;
    
    let is_valid = verify(password_to_check, &cfg.auth.admin_password_hash)
        .map_err(|e| PyRuntimeError::new_err(format!("Error al verificar hash: {}", e)))?;
        
    Ok(is_valid)
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
    security_mod.add_function(wrap_pyfunction!(verify_admin_password, &security_mod)?)?;
    m.add_submodule(&security_mod)?;

    let utils_mod = PyModule::new_bound(py, "noir_utils")?;
    utils_mod.add_function(wrap_pyfunction!(start_audio_watcher, &utils_mod)?)?;
    m.add_submodule(&utils_mod)?;

    Ok(())
}